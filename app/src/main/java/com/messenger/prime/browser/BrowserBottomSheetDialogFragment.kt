package com.messenger.prime.browser

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.messenger.prime.PrimePopupMenu
import com.messenger.prime.setupBlur
import com.messenger.prime.PrimePredictiveBack
import com.messenger.prime.R
import com.messenger.prime.SettingsActivity

class BrowserBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var initialUrl: String = "https://www.google.com"

    private lateinit var layoutBrowserHeader: LinearLayout
    private lateinit var tvPageTitle: TextView
    private lateinit var ivSecurityStatus: ImageView
    private lateinit var webView: ObservableWebView
    private lateinit var progressBar: ProgressBar

    private var isLoadingPage: Boolean = false

    private lateinit var layoutBottomBarContainer: View
    private lateinit var layoutControlsRow: LinearLayout
    private lateinit var layoutSiteInfoRow: LinearLayout
    private lateinit var btnRestorePanel: View

    private var isBottomBarCollapsed: Boolean = false
    private var isPanelHiddenCompletely: Boolean = false
    private val inactivityHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val expandRunnable = Runnable { expandBottomBar() }

    private lateinit var btnBackClose: ImageView
    private lateinit var btnForward: ImageView
    private lateinit var cardAddressBar: LinearLayout
    private lateinit var ivAddressSearchIcon: ImageView
    private lateinit var etAddressInput: EditText
    private lateinit var btnGoOrRefresh: ImageView
    private lateinit var btnMenu: ImageView

    private lateinit var layoutFindInPage: LinearLayout
    private lateinit var etFindQuery: EditText
    private lateinit var tvFindMatches: TextView
    private lateinit var btnFindPrev: ImageView
    private lateinit var btnFindNext: ImageView
    private lateinit var btnFindClose: ImageView

    private var isDesktopUserAgent = false
    private lateinit var defaultMobileUserAgent: String
    private val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    companion object {
        private const val ARG_URL = "arg_url"

        @JvmStatic
        fun newInstance(url: String): BrowserBottomSheetDialogFragment {
            val fragment = BrowserBottomSheetDialogFragment()
            val args = Bundle()
            args.putString(ARG_URL, url)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Prime_BottomSheetDialog)
        arguments?.getString(ARG_URL)?.let {
            initialUrl = it
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                sheet.requestLayout()
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.isFitToContents = false
                behavior.expandedOffset = 0
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = true
                Log.d("BrowserDebug", "BottomSheet shown, height set to MATCH_PARENT")
            }
        }

        val onBackPressedCallback = object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    remove()
                    dialog.dismiss()
                }
            }
        }
        dialog.onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("BrowserDebug", "onCreateView called")
        return try {
            inflater.inflate(R.layout.dialog_browser_bottom_sheet, container, false)
        } catch (e: Exception) {
            Log.e("BrowserDebug", "Error inflating dialog_browser_bottom_sheet layout", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            bindViews(view)
            setupWebView()
            setupListeners()

            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                val bottomInset = systemBars.bottom.coerceAtLeast(0)
                val density = resources.displayMetrics.density
                val baseMargin = (12 * density).toInt()

                val layoutParams = layoutBottomBarContainer.layoutParams as? ViewGroup.MarginLayoutParams
                if (layoutParams != null) {
                    layoutParams.bottomMargin = baseMargin + bottomInset
                    layoutBottomBarContainer.layoutParams = layoutParams
                }

                val restoreParams = btnRestorePanel.layoutParams as? ViewGroup.MarginLayoutParams
                if (restoreParams != null) {
                    restoreParams.bottomMargin = (16 * density).toInt() + bottomInset
                    btnRestorePanel.layoutParams = restoreParams
                }

                val islandHeight = layoutBottomBarContainer.height.takeIf { it > 0 } ?: (90 * density).toInt()
                webView.setPadding(
                    webView.paddingLeft,
                    webView.paddingTop,
                    webView.paddingRight,
                    islandHeight + bottomInset + baseMargin
                )
                webView.clipToPadding = false

                insets
            }

            view.post {
                setupBlurView(view)
            }

            val startUrl = BrowserPreferences.buildSearchOrUrl(requireContext(), initialUrl)
            Log.d("BrowserDebug", "onViewCreated loading URL: $startUrl")
            webView.loadUrl(startUrl)
        } catch (e: Exception) {
            Log.e("BrowserDebug", "Error initializing views in onViewCreated", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()
        PrimePredictiveBack.isGestureLocked = true

        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return

        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        view?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        bottomSheet.clipToOutline = true
        view?.clipToOutline = true

        bottomSheet.requestLayout()
        view?.requestLayout()

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.maxHeight = ViewGroup.LayoutParams.MATCH_PARENT
        behavior.isFitToContents = false
        behavior.expandedOffset = 0
        behavior.peekHeight = resources.displayMetrics.heightPixels
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        behavior.isHideable = true
        behavior.isDraggable = true
    }

    override fun onResume() {
        super.onResume()
        PrimePredictiveBack.isGestureLocked = true
        if (::webView.isInitialized) {
            webView.onResume()
        }
    }

    override fun onPause() {
        if (::webView.isInitialized) {
            webView.onPause()
        }
        super.onPause()
    }

    override fun onStop() {
        PrimePredictiveBack.isGestureLocked = false
        super.onStop()
    }

    override fun onDismiss(dialog: DialogInterface) {
        PrimePredictiveBack.isGestureLocked = false
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        inactivityHandler.removeCallbacks(expandRunnable)
        PrimePredictiveBack.isGestureLocked = false
        try {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.webChromeClient = null
            webView.destroy()
        } catch (e: Exception) {
            Log.e("BrowserDebug", "Error destroying webView", e)
        }
        super.onDestroyView()
    }

    private fun bindViews(root: View) {
        layoutBrowserHeader = root.findViewById(R.id.layoutBrowserHeader)
        tvPageTitle = root.findViewById(R.id.tvPageTitle)
        ivSecurityStatus = root.findViewById(R.id.ivSecurityStatus)
        webView = root.findViewById(R.id.webView)
        progressBar = root.findViewById(R.id.progressBar)

        layoutBottomBarContainer = root.findViewById(R.id.layoutBottomBarContainer)
        layoutControlsRow = root.findViewById(R.id.layoutControlsRow)
        layoutSiteInfoRow = root.findViewById(R.id.layoutSiteInfoRow)
        btnRestorePanel = root.findViewById(R.id.btnRestorePanel)

        btnBackClose = root.findViewById(R.id.btnBackClose)
        btnForward = root.findViewById(R.id.btnForward)
        cardAddressBar = root.findViewById(R.id.cardAddressBar)
        ivAddressSearchIcon = root.findViewById(R.id.ivAddressSearchIcon)
        etAddressInput = root.findViewById(R.id.etAddressInput)
        btnGoOrRefresh = root.findViewById(R.id.btnGoOrRefresh)
        btnMenu = root.findViewById(R.id.btnMenu)

        layoutFindInPage = root.findViewById(R.id.layoutFindInPage)
        etFindQuery = root.findViewById(R.id.etFindQuery)
        tvFindMatches = root.findViewById(R.id.tvFindMatches)
        btnFindPrev = root.findViewById(R.id.btnFindPrev)
        btnFindNext = root.findViewById(R.id.btnFindNext)
        btnFindClose = root.findViewById(R.id.btnFindClose)
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebView() {
        val context = requireContext()
        val settings = webView.settings

        settings.javaScriptEnabled = BrowserPreferences.isJavaScriptEnabled(context)
        settings.domStorageEnabled = true
        @Suppress("DEPRECATION")
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.mediaPlaybackRequiresUserGesture = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(false)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        defaultMobileUserAgent = settings.userAgentString.orEmpty()
        isDesktopUserAgent = BrowserPreferences.isDesktopModeEnabled(context)
        settings.userAgentString = if (isDesktopUserAgent) desktopUserAgent else defaultMobileUserAgent

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(BrowserPreferences.areCookiesEnabled(context))
        cookieManager.setAcceptThirdPartyCookies(webView, BrowserPreferences.areCookiesEnabled(context))

        webView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!v.canScrollVertically(-1) && event.y > 40) {
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                    } else {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        webView.setDownloadListener { downloadUrl, _, _, _, _ ->
            Log.d("BrowserDebug", "Download triggered for URL: $downloadUrl")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Не удалось открыть файл для скачивания", Toast.LENGTH_SHORT).show()
            }
        }

        webView.setOnObservableScrollChangeListener { _, scrollY, _, oldScrollY ->
            if (!isLoadingPage) {
                val scrollProgress = webView.getScrollPercentage()
                updateProgressBar(scrollProgress, animate = false)
            }

            if (isPanelHiddenCompletely) return@setOnObservableScrollChangeListener

            val dy = scrollY - oldScrollY
            if (scrollY <= 15) {
                expandBottomBar()
            } else if (dy > 12) {
                collapseBottomBar()
            } else if (dy < -12) {
                expandBottomBar()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                Log.d("BrowserDebug", "onProgressChanged: $newProgress%")
                if (newProgress < 100) {
                    isLoadingPage = true
                    updateProgressBar(newProgress, animate = true)
                } else {
                    isLoadingPage = false
                    val scrollProgress = webView.getScrollPercentage()
                    updateProgressBar(scrollProgress, animate = true)
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                Log.d("BrowserDebug", "onReceivedTitle: $title")
                updateNavigationState(view?.url)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("BrowserDebug", "JS Console [${consoleMessage?.messageLevel()}]: ${consoleMessage?.message()} (source: ${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                return true
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                val result = view?.hitTestResult
                val data = result?.extra
                if (!data.isNullOrEmpty()) {
                    view.loadUrl(data)
                    return true
                }
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                if (transport != null) {
                    transport.webView = view
                    resultMsg.sendToTarget()
                    return true
                }
                return false
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                Log.d("BrowserDebug", "shouldOverrideUrlLoading: $url")
                return handleUrlLoading(url)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                Log.d("BrowserDebug", "shouldOverrideUrlLoading (deprecated): $url")
                return handleUrlLoading(url)
            }

            private fun handleUrlLoading(url: String): Boolean {
                val lowerUrl = url.lowercase()
                if (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://") || lowerUrl.startsWith("about:") || lowerUrl.startsWith("file://")) {
                    return false
                }
                if (lowerUrl.startsWith("intent://") || lowerUrl.startsWith("intent:")) {
                    return try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (intent != null) {
                            val packageManager = context.packageManager
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                return true
                            }
                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                            if (!fallbackUrl.isNullOrEmpty()) {
                                webView.loadUrl(fallbackUrl)
                                return true
                            }
                        }
                        true
                    } catch (e: Exception) {
                        Log.e("BrowserDebug", "Error handling intent scheme: $url", e)
                        true
                    }
                }
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    Log.e("BrowserDebug", "Error opening external app for url: $url", e)
                    true
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                Log.e("BrowserDebug", "onReceivedSslError: ${error?.primaryError}")
                handler?.cancel()
                tvPageTitle.text = "Ошибка SSL-сертификата"
                ivSecurityStatus.setImageResource(R.drawable.ic_globe)
                ivSecurityStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.prime_text_secondary))
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Log.e("BrowserDebug", "onReceivedError: ${error?.description} (code ${error?.errorCode}) for URL: ${request?.url}")
                if (request?.isForMainFrame == true) {
                    tvPageTitle.text = "Ошибка загрузки страницы"
                }
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                Log.e("BrowserDebug", "onReceivedHttpError: status ${errorResponse?.statusCode} (${errorResponse?.reasonPhrase}) for URL: ${request?.url}")
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Log.d("BrowserDebug", "onPageStarted: $url")
                isLoadingPage = true
                updateProgressBar(0, animate = false)
                webView.post {
                    Log.d("BrowserDebug", "WebView size on page start: width=${webView.width}, height=${webView.height}, measuredWidth=${webView.measuredWidth}, measuredHeight=${webView.measuredHeight}")
                }
                updateNavigationState(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d("BrowserDebug", "onPageFinished: $url")
                isLoadingPage = false
                val scrollProgress = webView.getScrollPercentage()
                updateProgressBar(scrollProgress, animate = true)
                updateNavigationState(url)
            }
        }

        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (isDoneCounting) {
                if (numberOfMatches == 0) {
                    tvFindMatches.text = "0/0"
                } else {
                    tvFindMatches.text = "${activeMatchOrdinal + 1}/$numberOfMatches"
                }
            }
        }
    }

    private fun setupListeners() {
        layoutBrowserHeader.setOnClickListener {
            dismiss()
        }

        btnRestorePanel.setOnClickListener {
            restorePanelCompletely()
        }

        setupDraggableRestoreButton()

        layoutSiteInfoRow.setOnClickListener {
            if (isBottomBarCollapsed) {
                expandBottomBar()
            }
        }

        btnBackClose.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                dismiss()
            }
        }

        btnForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        btnGoOrRefresh.setOnClickListener {
            if (etAddressInput.hasFocus()) {
                performSearchFromInput()
            } else {
                webView.reload()
            }
        }

        etAddressInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                expandBottomBar()
                etAddressInput.selectAll()
                btnGoOrRefresh.setImageResource(R.drawable.ic_send)
                ivAddressSearchIcon.setImageResource(R.drawable.ic_search)
            } else {
                btnGoOrRefresh.setImageResource(R.drawable.ic_refresh)
                ivAddressSearchIcon.setImageResource(R.drawable.ic_globe)
            }
        }

        etAddressInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearchFromInput()
                true
            } else {
                false
            }
        }

        btnMenu.setOnClickListener { v ->
            showBrowserOptionsMenu(v)
        }

        // In-page search listeners
        etFindQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                if (query.isNotEmpty()) {
                    webView.findAllAsync(query)
                } else {
                    webView.clearMatches()
                    tvFindMatches.text = "0/0"
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnFindPrev.setOnClickListener { webView.findNext(false) }
        btnFindNext.setOnClickListener { webView.findNext(true) }
        btnFindClose.setOnClickListener {
            layoutFindInPage.visibility = View.GONE
            webView.clearMatches()
            hideKeyboard(etFindQuery)
        }
    }

    private fun performSearchFromInput() {
        val query = etAddressInput.text.toString().trim()
        if (query.isNotEmpty()) {
            val destinationUrl = BrowserPreferences.buildSearchOrUrl(requireContext(), query)
            webView.loadUrl(destinationUrl)
        }
        etAddressInput.clearFocus()
        hideKeyboard(etAddressInput)
    }

    private fun updateProgressBar(progress: Int, animate: Boolean = true) {
        if (::progressBar.isInitialized) {
            val targetProgress = progress.coerceIn(0, 100)
            if (animate) {
                progressBar.setProgress(targetProgress, true)
            } else {
                progressBar.progress = targetProgress
            }
        }
    }

    private fun collapseBottomBar() {
        if (isBottomBarCollapsed || ::etAddressInput.isInitialized && etAddressInput.hasFocus() || ::layoutFindInPage.isInitialized && layoutFindInPage.visibility == View.VISIBLE) return
        isBottomBarCollapsed = true

        inactivityHandler.removeCallbacks(expandRunnable)
        inactivityHandler.postDelayed(expandRunnable, 3500L)

        val container = layoutBottomBarContainer as? ViewGroup ?: return
        androidx.transition.TransitionManager.beginDelayedTransition(
            container,
            androidx.transition.AutoTransition().apply {
                duration = 250
            }
        )
        layoutControlsRow.visibility = View.GONE
    }

    private fun expandBottomBar() {
        inactivityHandler.removeCallbacks(expandRunnable)
        if (!isBottomBarCollapsed) return
        isBottomBarCollapsed = false

        val container = layoutBottomBarContainer as? ViewGroup ?: return
        androidx.transition.TransitionManager.beginDelayedTransition(
            container,
            androidx.transition.AutoTransition().apply {
                duration = 250
            }
        )
        layoutControlsRow.visibility = View.VISIBLE
    }

    private fun setupBlurView(view: View) {
        try {
            val dialog = dialog as? BottomSheetDialog ?: return
            val targetRootView = dialog.findViewById<ViewGroup>(com.google.android.material.R.id.design_bottom_sheet)
                ?: (view.rootView as? ViewGroup)
                ?: (view as? ViewGroup)

            if (targetRootView != null) {
                val overlayColor = ContextCompat.getColor(requireContext(), R.color.prime_blur_overlay)
                
                (layoutBottomBarContainer as? eightbitlab.com.blurview.BlurView)?.setupBlur(
                    rootView = targetRootView,
                    blurRadius = 18f,
                    tintColor = overlayColor
                )

                (btnRestorePanel as? eightbitlab.com.blurview.BlurView)?.setupBlur(
                    rootView = targetRootView,
                    blurRadius = 18f,
                    tintColor = overlayColor
                )
            }
        } catch (e: Exception) {
            Log.e("BrowserDebug", "Error setting up BlurView", e)
        }
    }

    private fun hidePanelCompletely() {
        if (isPanelHiddenCompletely) return
        isPanelHiddenCompletely = true
        inactivityHandler.removeCallbacks(expandRunnable)

        val rootContainer = view as? ViewGroup ?: return
        androidx.transition.TransitionManager.beginDelayedTransition(
            rootContainer,
            androidx.transition.AutoTransition().apply {
                duration = 250
            }
        )
        layoutBottomBarContainer.visibility = View.GONE
        btnRestorePanel.visibility = View.VISIBLE
    }

    private fun restorePanelCompletely() {
        if (!isPanelHiddenCompletely) return
        isPanelHiddenCompletely = false
        isBottomBarCollapsed = false

        val rootContainer = view as? ViewGroup ?: return
        androidx.transition.TransitionManager.beginDelayedTransition(
            rootContainer,
            androidx.transition.AutoTransition().apply {
                duration = 250
            }
        )
        btnRestorePanel.visibility = View.GONE
        btnRestorePanel.translationX = 0f
        btnRestorePanel.translationY = 0f
        layoutBottomBarContainer.visibility = View.VISIBLE
        layoutControlsRow.visibility = View.VISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggableRestoreButton() {
        val touchSlop = android.view.ViewConfiguration.get(requireContext()).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var initialX = 0f
        var initialY = 0f
        var isDragging = false

        btnRestorePanel.setOnTouchListener { v, event ->
            val parent = v.parent as? ViewGroup ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    initialX = v.x
                    initialY = v.y
                    isDragging = false
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!isDragging && Math.hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val newX = (initialX + dx).coerceIn(0f, (parent.width - v.width).toFloat())
                        val newY = (initialY + dy).coerceIn(0f, (parent.height - v.height).toFloat())
                        v.x = newX
                        v.y = newY
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    if (!isDragging) {
                        v.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }
    }

    private fun updateNavigationState(currentUrl: String? = webView.url) {
        val canGoBack = webView.canGoBack()
        val canGoForward = webView.canGoForward()

        btnBackClose.setImageResource(R.drawable.ic_arrow_back)
        if (canGoBack) {
            btnBackClose.alpha = 1.0f
        } else {
            btnBackClose.alpha = 0.5f
        }

        if (canGoForward) {
            btnForward.isEnabled = true
            btnForward.alpha = 1.0f
        } else {
            btnForward.isEnabled = false
            btnForward.alpha = 0.4f
        }

        if (!etAddressInput.hasFocus() && !currentUrl.isNullOrEmpty()) {
            etAddressInput.setText(currentUrl)
        }

        val host = if (!currentUrl.isNullOrEmpty()) Uri.parse(currentUrl).host ?: currentUrl else ""
        val isHttps = currentUrl?.startsWith("https://", ignoreCase = true) == true

        if (isHttps) {
            ivSecurityStatus.setImageResource(R.drawable.ic_lock_closed)
            ivSecurityStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.prime_brand))
            val title = webView.title
            if (!title.isNullOrEmpty() && title != currentUrl) {
                tvPageTitle.text = "$title ($host)"
            } else {
                tvPageTitle.text = if (host.isNotEmpty()) "$host — Защищенное соединение" else "Загрузка..."
            }
        } else {
            ivSecurityStatus.setImageResource(R.drawable.ic_globe)
            ivSecurityStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.prime_text_secondary))
            val title = webView.title
            if (!title.isNullOrEmpty() && title != currentUrl) {
                tvPageTitle.text = "$title ($host)"
            } else {
                tvPageTitle.text = if (host.isNotEmpty()) "$host — Незащищенное соединение" else "Загрузка..."
            }
        }
    }

    private fun showBrowserOptionsMenu(anchorView: View) {
        val desktopTitle = if (isDesktopUserAgent) "Мобильная версия" else "Версия для ПК"
        PrimePopupMenu.create(requireContext(), anchorView)
            .addItem(1, "Поиск на странице", R.drawable.ic_search) {
                layoutFindInPage.visibility = View.VISIBLE
                etFindQuery.requestFocus()
                showKeyboard(etFindQuery)
            }
            .addItem(2, "Обновить страницу", R.drawable.ic_refresh) {
                webView.reload()
            }
            .addItem(3, desktopTitle, R.drawable.ic_desktop) {
                toggleDesktopMode()
            }
            .addItem(4, "Скопировать ссылку", R.drawable.ic_copy) {
                copyCurrentUrl()
            }
            .addItem(5, "Поделиться", R.drawable.ic_share) {
                shareCurrentUrl()
            }
            .addItem(6, "Открыть в браузере", R.drawable.ic_globe) {
                openExternalBrowser()
            }
            .addItem(7, "Настройки браузера", R.drawable.ic_settings) {
                openBrowserSettings()
            }
            .addItem(8, "Очистить кэш сайта", R.drawable.ic_trash) {
                clearSiteData()
            }
            .addItem(9, "Скрыть панель полностью", R.drawable.ic_hide_panel) {
                hidePanelCompletely()
            }
            .show()
    }

    private fun toggleDesktopMode() {
        isDesktopUserAgent = !isDesktopUserAgent
        BrowserPreferences.setDesktopModeEnabled(requireContext(), isDesktopUserAgent)
        if (isDesktopUserAgent) {
            webView.settings.userAgentString = desktopUserAgent
            Toast.makeText(requireContext(), "Включена версия для ПК", Toast.LENGTH_SHORT).show()
        } else {
            webView.settings.userAgentString = defaultMobileUserAgent
            Toast.makeText(requireContext(), "Включена мобильная версия", Toast.LENGTH_SHORT).show()
        }
        webView.reload()
    }

    private fun copyCurrentUrl() {
        val url = webView.url ?: etAddressInput.text.toString()
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("URL", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Ссылка скопирована", Toast.LENGTH_SHORT).show()
    }

    private fun shareCurrentUrl() {
        val url = webView.url ?: etAddressInput.text.toString()
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Поделиться ссылкой")
            startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Не удалось поделиться", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternalBrowser() {
        val url = webView.url ?: etAddressInput.text.toString()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBrowserSettings() {
        try {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun clearSiteData() {
        webView.clearCache(true)
        webView.clearHistory()
        Toast.makeText(requireContext(), "Кэш браузера очищен", Toast.LENGTH_SHORT).show()
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}