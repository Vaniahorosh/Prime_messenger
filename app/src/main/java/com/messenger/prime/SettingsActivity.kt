package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.messenger.prime.databinding.ActivitySettingsBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private var isHeaderExpanded = false
    private var isHeaderMoving = false
    private var isAnimating = false
    private var pullStartY = 0f
    private val PULL_THRESHOLD = 250f
    private var currentAnimator: ValueAnimator? = null
    private var isVibrated = false
    private var isClosing = false

    private var currentAvatarUri: String? = null
    private var activePhotoDialogBinding: com.messenger.prime.databinding.DialogPhotoActionsBinding? = null

    private var currentNameInDB: String = ""
    private var currentLoginInDB: String = ""
    private var currentPassInDB: String = ""

    private val avatarUriState = mutableStateOf<String?>(null)
    private var profileImageView: ImageView? = null

    private val photoViewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data?.getBooleanExtra("DELETED", false) == true) {
                handlePhotoDeletionWithUndo(currentAvatarUri)
            } else {
                val newUri = data?.getStringExtra("NEW_URI")
                if (newUri != null) {
                    currentAvatarUri = newUri
                    val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                    val currentUser = sharedPrefs.getString("current_user", "") ?: ""
                    sharedPrefs.edit().putString("${currentUser}_avatar", newUri).apply()
                    applyAvatarState(newUri)
                }
            }
        }
    }

    private val photoEditorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val editedUriString = result.data?.getStringExtra("EDITED_IMAGE_URI")
            if (editedUriString != null) {
                currentAvatarUri = editedUriString
                val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                val currentUser = sharedPrefs.getString("current_user", "") ?: ""
                sharedPrefs.edit().putString("${currentUser}_avatar", currentAvatarUri).apply()
                applyAvatarState(currentAvatarUri)
                PrimeNotification.show(this, "Фото готово")
            }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val intent = Intent(this, PhotoEditorActivity::class.java)
            intent.putExtra("EXTRA_IMAGE_URI", it.toString())
            photoEditorLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge(isDarkIcons = true)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        currentLoginInDB = currentUser
        currentPassInDB = sharedPrefs.getString(currentUser, "") ?: ""
        
        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"
        currentNameInDB = savedName
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)
        currentAvatarUri = savedAvatarUri
        avatarUriState.value = savedAvatarUri

        binding.tvUserNameStatic.text = savedName
        binding.tvUserNameWP.text = savedName
        binding.tvUserNameStatic.isSelected = true
        binding.tvUserNameWP.isSelected = true
        binding.etSettingsName.setText(savedName)
        binding.etSettingsLogin.setText(currentUser)
        binding.etSettingsPassword.setText(currentPassInDB)
        binding.tvAccountHeaderSummary.text = savedName

        val isExpanded = sharedPrefs.getBoolean("settings_account_expanded", false)
        binding.layoutAccountCollapsible.visibility = if (isExpanded) View.VISIBLE else View.GONE
        binding.ivAccountArrow.rotation = if (isExpanded) -90f else 90f

        setupComposePhoto()
        setupListeners()
        applyAvatarState(savedAvatarUri)

        val slidrConfig = SlidrConfig.Builder().position(SlidrPosition.LEFT).build()
        Slidr.attach(this, slidrConfig)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        })
    }

    private fun setupComposePhoto() {
        binding.composePhotoCard.setContent {
            val avatarUri = avatarUriState.value
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setOnClickListener {
                                if (!isHeaderExpanded) showPhotoActionDialog()
                                else openFullPhoto()
                            }
                            profileImageView = this
                        }
                    },
                    update = { view ->
                        if (avatarUri != null) view.setImageURI(Uri.parse(avatarUri))
                        else view.setImageResource(R.drawable.ic_person)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun setupListeners() {
        val backAction = View.OnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        binding.btnBackWP.setOnClickListener(backAction)
        binding.btnBackNP.setOnClickListener(backAction)
        binding.btnLogout.setOnClickListener { showLogoutDialog() }
        binding.btnLogoutNP.setOnClickListener { showLogoutDialog() }
        binding.btnChangePhoto.setOnClickListener { pickImage.launch("image/*") }
        binding.btnChangePhotoNP.setOnClickListener { pickImage.launch("image/*") }

        val extraAction = View.OnClickListener {
            binding.nestedScrollView.smoothScrollTo(0, 1000)
        }
        binding.btnExtraSettings.setOnClickListener(extraAction)
        binding.btnExtraSettingsNP.setOnClickListener(extraAction)

        setupInlineAccountEditing()
        setupAccountCollapsible()

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        binding.switchAnimations.isChecked = sharedPrefs.getBoolean("settings_animations", true)
        binding.switchBlocked.isChecked = sharedPrefs.getBoolean("settings_show_blocked", false)
        binding.switchSearch.isChecked = sharedPrefs.getBoolean("settings_hide_search", false)

        binding.switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_animations", isChecked).apply()
        }
        binding.switchBlocked.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_show_blocked", isChecked).apply()
        }
        binding.switchSearch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_hide_search", isChecked).apply()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.headerStaticBlock.setPadding(0, systemBarsInsets.top, 0, 0)
            binding.nestedScrollView.setPadding(0, 0, 0, systemBarsInsets.bottom)
            windowInsets
        }

        setupHeaderExpansion()
    }

    override fun onResume() {
        super.onResume()
        isClosing = false
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)
        
        currentAvatarUri = savedAvatarUri
        avatarUriState.value = savedAvatarUri
        binding.tvUserNameStatic.text = savedName
        binding.tvUserNameWP.text = savedName
        binding.etSettingsName.setText(savedName)
        binding.tvAccountHeaderSummary.text = savedName
        currentNameInDB = savedName
        binding.tvUserNameStatic.isSelected = true
        binding.tvUserNameWP.isSelected = true
        applyAvatarState(savedAvatarUri)
    }

    private fun applyAvatarState(avatarUri: String?) {
        if (avatarUri != null) {
            binding.layoutWithPhoto.visibility = View.VISIBLE
            binding.layoutNoPhoto.visibility = View.GONE
            binding.headerStaticBlock.minimumHeight = (320 * resources.displayMetrics.density).toInt()
        } else {
            binding.layoutWithPhoto.visibility = View.GONE
            binding.layoutNoPhoto.visibility = View.VISIBLE
            binding.headerStaticBlock.minimumHeight = 0
        }
    }

    private fun setupHeaderExpansion() {
        binding.nestedScrollView.setOnTouchListener { v, event ->
            if (isAnimating) return@setOnTouchListener true
            if (binding.nestedScrollView.scrollY > 0 && !isHeaderExpanded) {
                pullStartY = -1f
                return@setOnTouchListener false
            }
            if (currentAvatarUri == null) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pullStartY = event.y
                    isVibrated = false
                    isHeaderMoving = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (pullStartY == -1f) {
                        pullStartY = event.y
                        return@setOnTouchListener false
                    }
                    val dy = event.y - pullStartY
                    if (!isHeaderExpanded) {
                        if (dy > 20f && binding.nestedScrollView.scrollY == 0) {
                            if (!isHeaderMoving) {
                                isHeaderMoving = true
                                v.parent.requestDisallowInterceptTouchEvent(true)
                            }
                            val progress = (dy / PULL_THRESHOLD).coerceIn(0f, 1.2f)
                            updateHeaderAnimation(progress)
                            if (progress >= 1f && !isVibrated) {
                                binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                isVibrated = true
                            }
                            return@setOnTouchListener true
                        }
                    } else {
                        if (dy < -20f) {
                            if (!isHeaderMoving) {
                                isHeaderMoving = true
                                v.parent.requestDisallowInterceptTouchEvent(true)
                            }
                            val progress = (1f - (Math.abs(dy) / PULL_THRESHOLD)).coerceIn(0f, 1f)
                            updateHeaderAnimation(progress)
                            if (progress <= 0f) {
                                isHeaderExpanded = false
                                isHeaderMoving = false
                                v.parent.requestDisallowInterceptTouchEvent(false)
                                return@setOnTouchListener false 
                            }
                            return@setOnTouchListener true
                        } else if (dy > 150f && !isClosing) {
                            openFullPhoto()
                            pullStartY = event.y
                            return@setOnTouchListener true
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (pullStartY != -1f && isHeaderMoving) {
                        val dy = event.y - pullStartY
                        if (!isHeaderExpanded) {
                            if (dy > PULL_THRESHOLD / 2) animateHeaderState(true)
                            else animateHeaderState(false)
                        } else {
                            if (dy < -PULL_THRESHOLD / 3) animateHeaderState(false)
                            else animateHeaderState(true)
                        }
                    }
                    pullStartY = -1f
                    isHeaderMoving = false
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    private fun updateHeaderAnimation(progress: Float) {
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val leftColWidth = binding.layoutLeftColumn.width.toFloat()
        val cardWidth = binding.photoCard.width.toFloat()
        val cardHeight = binding.photoCard.height.toFloat()
        val headerHeight = binding.headerStaticBlock.height.toFloat()

        if (leftColWidth == 0f || cardWidth == 0f || headerHeight == 0f) return

        val statusBarHeight = ViewCompat.getRootWindowInsets(binding.root)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top?.toFloat() ?: 0f
        val innerPadding = binding.layoutWithPhoto.paddingTop.toFloat()
        val totalOffsetUp = statusBarHeight + innerPadding

        val targetScaleX = screenWidth / cardWidth
        val targetScaleY = headerHeight / cardHeight 
        
        val currentScaleX = 1f + (targetScaleX - 1f) * progress
        val currentScaleY = 1f + (targetScaleY - 1f) * progress
        
        binding.photoCard.scaleX = currentScaleX
        binding.photoCard.scaleY = currentScaleY
        
        // ВАЖНО: Применяем обратное скалирование ТОЛЬКО к контейнеру с текстом, а не ко всему содержимому.
        // Изображение (composePhotoCard) остается вне этого контейнера в XML, поэтому оно будет расширяться.
        binding.layoutPhotoInternal.scaleX = 1f / currentScaleX
        binding.layoutPhotoInternal.scaleY = 1f / currentScaleY
        binding.layoutPhotoInternal.pivotX = 0f
        binding.layoutPhotoInternal.pivotY = cardHeight
        
        val extraPadding = (16 * progress * density)
        binding.tvUserNameWP.translationX = extraPadding
        binding.tvUserNameWP.translationY = -extraPadding
        binding.tvStatusWP.translationX = extraPadding
        binding.tvStatusWP.translationY = -extraPadding

        // Анимация расширения фона текста (градиента)
        val gradView = binding.viewPhotoInfoGradient
        // Поскольку теперь градиент находится вне layoutPhotoInternal, он масштабируется ВМЕСТЕ с карточкой.
        // Нам нужно только компенсировать его высоту, чтобы она оставалась постоянной (90dp), а ширина росла.
        gradView.pivotX = 0f
        gradView.pivotY = gradView.height.toFloat()
        gradView.scaleY = 1f / currentScaleY
        gradView.scaleX = 1f // scaleX наследуется от photoCard, поэтому здесь 1.0 относительно родителя

        val otherAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
        val otherTranslationX = -leftColWidth * progress

        binding.btnBackWP.alpha = (1f - progress * 0.6f).coerceIn(0.4f, 1f)
        binding.tvBackLabelWP.alpha = otherAlpha
        binding.tvBackLabelWP.translationX = otherTranslationX
        binding.layoutLeftColumn.alpha = otherAlpha
        binding.layoutLeftColumn.translationX = otherTranslationX

        binding.photoCard.pivotX = 0f
        binding.photoCard.pivotY = 0f
        binding.photoCard.translationX = -(leftColWidth * progress)
        binding.photoCard.translationY = -(totalOffsetUp * progress)

        binding.photoCard.radius = (24 * (1f - progress)).coerceAtLeast(0f) * density
        binding.photoCard.cardElevation = (8 * (1f - progress)).coerceAtLeast(0f) * density
        
        val pushDown = (screenWidth - headerHeight).coerceAtLeast(0f) * progress
        binding.layoutAccountData.translationY = pushDown
        binding.layoutSwitches.translationY = pushDown
        binding.tvAppVersion.translationY = pushDown
    }

    private fun animateHeaderState(expand: Boolean) {
        currentAnimator?.cancel()
        isAnimating = true
        val currentProgress = (binding.photoCard.scaleX - 1f) / ((resources.displayMetrics.widthPixels.toFloat() / binding.photoCard.width) - 1f)
        val startVal = if (currentProgress.isNaN()) 0f else currentProgress.coerceIn(0f, 1f)
        val animator = ValueAnimator.ofFloat(startVal, if (expand) 1f else 0f)
        currentAnimator = animator
        animator.addUpdateListener { anim -> updateHeaderAnimation(anim.animatedValue as Float) }
        animator.duration = 300
        animator.interpolator = DecelerateInterpolator()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isHeaderExpanded = expand
                isAnimating = false
                currentAnimator = null
            }
        })
        animator.start()
    }

    private fun openFullPhoto() {
        if (currentAvatarUri == null || isClosing) return
        isClosing = true
        val intent = Intent(this, PhotoViewActivity::class.java)
        intent.putExtra("EXTRA_URI", currentAvatarUri)
        val rect = Rect()
        profileImageView?.getGlobalVisibleRect(rect)
        intent.putExtra("EXTRA_RECT", rect)
        photoViewLauncher.launch(intent)
        overridePendingTransition(0, 0)
    }

    private fun showPhotoActionDialog() {
        val dialogBinding = com.messenger.prime.databinding.DialogPhotoActionsBinding.inflate(layoutInflater)
        activePhotoDialogBinding = dialogBinding
        binding.dialogContainer.removeAllViews()
        binding.dialogContainer.addView(dialogBinding.root)
        binding.dialogContainer.visibility = View.VISIBLE
        dialogBinding.cardContainer.scaleX = 0.8f
        dialogBinding.cardContainer.scaleY = 0.8f
        dialogBinding.cardContainer.alpha = 0f
        dialogBinding.dialogRoot.alpha = 0f
        dialogBinding.dialogRoot.animate().alpha(1f).setDuration(300).start()
        dialogBinding.cardContainer.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(400).setInterpolator(DecelerateInterpolator()).start()

        dialogBinding.btnOpenPhoto.setOnClickListener { hidePhotoActionDialog(dialogBinding); openFullPhoto() }
        dialogBinding.btnChangePhoto.setOnClickListener { hidePhotoActionDialog(dialogBinding); pickImage.launch("image/*") }
        dialogBinding.btnDeletePhoto.setOnClickListener { hidePhotoActionDialog(dialogBinding) ; handlePhotoDeletionWithUndo(currentAvatarUri) }
        dialogBinding.btnClose.setOnClickListener { hidePhotoActionDialog(dialogBinding) }
        dialogBinding.dialogRoot.setOnClickListener { hidePhotoActionDialog(dialogBinding) }
    }

    private fun hidePhotoActionDialog(dialogBinding: com.messenger.prime.databinding.DialogPhotoActionsBinding) {
        dialogBinding.dialogRoot.animate().alpha(0f).setDuration(300).start()
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        dialogBinding.cardContainer.animate().translationX(screenWidth).alpha(0f).setDuration(350).setInterpolator(DecelerateInterpolator()).withEndAction {
            binding.dialogContainer.visibility = View.GONE
            binding.dialogContainer.removeAllViews()
            activePhotoDialogBinding = null
        }.start()
    }

    private fun setupAccountCollapsible() {
        binding.layoutAccountHeader.setOnClickListener {
            val isExpanded = binding.layoutAccountCollapsible.visibility == View.VISIBLE
            val newVisibility = if (isExpanded) View.GONE else View.VISIBLE
            
            // Используем родительский контейнер для плавной анимации всех элементов и ускоряем её
            val transition = AutoTransition().apply {
                duration = 200 // Быстрая анимация
            }
            TransitionManager.beginDelayedTransition(binding.layoutAccountData.parent as ViewGroup, transition)
            
            binding.layoutAccountCollapsible.visibility = newVisibility
            binding.ivAccountArrow.animate().rotation(if (isExpanded) 90f else -90f).setDuration(200).start()
            
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("settings_account_expanded", !isExpanded).apply()
        }
    }

    private fun setupInlineAccountEditing() {
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAccountChanges()
                binding.inputLayoutName.error = null
                binding.inputLayoutLogin.error = null
                binding.inputLayoutPassword.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        binding.etSettingsName.addTextChangedListener(textWatcher)
        binding.etSettingsLogin.addTextChangedListener(textWatcher)
        binding.etSettingsPassword.addTextChangedListener(textWatcher)

        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.postDelayed({
                    val parent = v.parent as? View ?: return@postDelayed
                    val targetY = binding.layoutAccountData.top + parent.top - (60 * resources.displayMetrics.density).toInt()
                    binding.nestedScrollView.smoothScrollTo(0, targetY.coerceAtLeast(0))
                }, 100)
            }
        }
        binding.etSettingsName.onFocusChangeListener = focusListener
        binding.etSettingsLogin.onFocusChangeListener = focusListener
        binding.etSettingsPassword.onFocusChangeListener = focusListener

        binding.btnSaveAccount.setOnClickListener {
            val newName = binding.etSettingsName.text.toString().trim()
            val newLogin = binding.etSettingsLogin.text.toString().trim()
            val newPass = binding.etSettingsPassword.text.toString()
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            
            val errorName = if (newName.isEmpty()) "Имя не может быть пустым" else ValidationUtils.getValidationError(newName, false)
            
            var errorLogin = when {
                newLogin.isEmpty() -> "Логин не может быть пустым"
                newLogin != currentLoginInDB && sharedPrefs.contains(newLogin) -> "Этот логин уже занят"
                else -> ValidationUtils.getValidationError(newLogin, true)
            }
            
            var errorPass = if (newPass.length < 8) "Минимум 8 символов" else null
            
            if (errorName != null || errorLogin != null || errorPass != null) {
                if (errorName != null) {
                    binding.inputLayoutName.error = errorName
                    binding.inputLayoutName.shake()
                }
                if (errorLogin != null) {
                    binding.inputLayoutLogin.error = errorLogin
                    binding.inputLayoutLogin.shake()
                }
                if (errorPass != null) {
                    binding.inputLayoutPassword.error = errorPass
                    binding.inputLayoutPassword.shake()
                }
                return@setOnClickListener
            }
            
            val oldName = currentNameInDB
            val oldLogin = currentLoginInDB
            val oldPass = currentPassInDB

            sharedPrefs.edit().apply {
                if (newLogin != currentLoginInDB) {
                    val avatar = sharedPrefs.getString("${currentLoginInDB}_avatar", null)
                    putString("current_user", newLogin)
                    putString(newLogin, newPass)
                    putString("${newLogin}_name", newName)
                    if (avatar != null) putString("${newLogin}_avatar", avatar)
                    remove(currentLoginInDB)
                    remove("${currentLoginInDB}_name")
                    remove("${currentLoginInDB}_avatar")
                } else {
                    putString("${currentLoginInDB}_name", newName)
                    putString(currentLoginInDB, newPass)
                }
                apply()
            }
            currentNameInDB = newName
            currentLoginInDB = newLogin
            currentPassInDB = newPass
            
            binding.tvUserNameStatic.text = newName
            binding.tvUserNameWP.text = newName
            binding.tvAccountHeaderSummary.text = newName
            
            PrimeNotification.show(this, "Данные обновлены") {
                sharedPrefs.edit().apply {
                    if (newLogin != oldLogin) {
                        val currentAvatar = sharedPrefs.getString("${newLogin}_avatar", null)
                        putString("current_user", oldLogin)
                        putString(oldLogin, oldPass)
                        putString("${oldLogin}_name", oldName)
                        if (currentAvatar != null) putString("${oldLogin}_avatar", currentAvatar)
                        remove(newLogin)
                        remove("${newLogin}_name")
                        remove("${newLogin}_avatar")
                    } else {
                        putString("${oldLogin}_name", oldName)
                        putString(oldLogin, oldPass)
                    }
                    apply()
                }
                currentNameInDB = oldName
                currentLoginInDB = oldLogin
                currentPassInDB = oldPass

                binding.etSettingsName.setText(oldName)
                binding.etSettingsLogin.setText(oldLogin)
                binding.etSettingsPassword.setText(oldPass)

                binding.tvUserNameStatic.text = oldName
                binding.tvUserNameWP.text = oldName
                binding.tvAccountHeaderSummary.text = oldName
                checkAccountChanges()
            }
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSettingsName.windowToken, 0)
            binding.etSettingsName.clearFocus()
            binding.etSettingsLogin.clearFocus()
            binding.etSettingsPassword.clearFocus()
            checkAccountChanges()
        }
        checkAccountChanges()
    }

    private fun checkAccountChanges() {
        val newName = binding.etSettingsName.text.toString().trim()
        val newLogin = binding.etSettingsLogin.text.toString().trim()
        val newPass = binding.etSettingsPassword.text.toString()
        
        val nameChanged = newName != currentNameInDB
        val loginChanged = newLogin != currentLoginInDB
        val passChanged = newPass != currentPassInDB
        
        if (nameChanged) {
            binding.inputLayoutName.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel)
            binding.inputLayoutName.setStartIconOnClickListener { binding.etSettingsName.setText(currentNameInDB) }
        } else {
            binding.inputLayoutName.startIconDrawable = null
            binding.inputLayoutName.setStartIconOnClickListener(null)
        }
        
        if (loginChanged) {
            binding.inputLayoutLogin.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel)
            binding.inputLayoutLogin.setStartIconOnClickListener { binding.etSettingsLogin.setText(currentLoginInDB) }
        } else {
            binding.inputLayoutLogin.startIconDrawable = null
            binding.inputLayoutLogin.setStartIconOnClickListener(null)
        }
        
        if (passChanged) {
            binding.inputLayoutPassword.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel)
            binding.inputLayoutPassword.setStartIconOnClickListener { binding.etSettingsPassword.setText(currentPassInDB) }
        } else {
            binding.inputLayoutPassword.startIconDrawable = null
            binding.inputLayoutPassword.setStartIconOnClickListener(null)
        }
        
        animateSaveButton((nameChanged || loginChanged || passChanged) && newLogin.isNotEmpty() && newName.isNotEmpty())
    }

    private fun animateSaveButton(show: Boolean) {
        if (show && binding.btnSaveAccount.visibility == View.VISIBLE) return
        if (!show && binding.btnSaveAccount.visibility == View.GONE) return
        binding.btnSaveAccount.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            currentFocus?.let { focusedView -> imm.hideSoftInputFromWindow(focusedView.windowToken, 0); focusedView.clearFocus() }
            activePhotoDialogBinding?.let { hidePhotoActionDialog(it) }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun handlePhotoDeletionWithUndo(uriToDelete: String?) {
        if (uriToDelete == null) return
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        sharedPrefs.edit().remove("${currentUser}_avatar").apply()
        currentAvatarUri = null; applyAvatarState(null)
        PrimeNotification.show(this, "Фото удалено") { sharedPrefs.edit().putString("${currentUser}_avatar", uriToDelete).apply(); currentAvatarUri = uriToDelete; applyAvatarState(uriToDelete) }
    }

    private fun showLogoutDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog).setTitle("Выход").setMessage("Сделать выход из аккаунта?").setPositiveButton("Да") { _, _ -> getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE).edit().putBoolean("is_logged_in", false).apply(); startActivity(Intent(this, LoginActivity::class.java)); finishAffinity() }.setNegativeButton("Нет", null).show()
    }
}
