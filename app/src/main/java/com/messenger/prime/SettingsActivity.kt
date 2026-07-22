package com.messenger.prime

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.messenger.prime.databinding.ActivitySettingsBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private var currentAvatarUri: String? = null
    private var activeDialogBinding: com.messenger.prime.databinding.DialogEditAccountBinding? = null
    private var activePhotoDialogBinding: com.messenger.prime.databinding.DialogPhotoActionsBinding? = null
    
    private var isFullScreenMode = false
    private var fsStartX = 0f
    private var fsStartY = 0f
    private var isFsDismissing = false

    // Переменные для зума и перетаскивания полноэкранного фото
    private var photoScale = 1f
    private var photoTranslateX = 0f
    private var photoTranslateY = 0f
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var photoGestureDetector: android.view.GestureDetector

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            currentAvatarUri = it.toString()
            
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val currentUser = sharedPrefs.getString("current_user", "") ?: ""
            sharedPrefs.edit().putString("${currentUser}_avatar", currentAvatarUri).apply()
            
            applyAvatarState(currentAvatarUri)
            PrimeNotification.show(this, "Фото обновлено")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        val slidrConfig = SlidrConfig.Builder().position(SlidrPosition.LEFT).build()
        Slidr.attach(this, slidrConfig)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""

        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь")
        val savedPassword = sharedPrefs.getString(currentUser, "")
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)
        currentAvatarUri = savedAvatarUri

        binding.tvUserNameFloating.text = savedName
        binding.tvUserNameStatic.text = savedName
        binding.etSettingsLogin.setText(currentUser)
        binding.etSettingsPassword.setText(savedPassword)

        applyAvatarState(savedAvatarUri)

        // Слушатели кнопок
        val backAction = View.OnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        binding.btnBackWP.setOnClickListener(backAction)
        binding.btnBackNP.setOnClickListener(backAction)
        binding.btnCloseFullPhoto.setOnClickListener { closeFullScreenMode() }

        val logoutAction = View.OnClickListener { showLogoutDialog() }
        binding.btnLogout.setOnClickListener(logoutAction)
        binding.btnLogoutNP.setOnClickListener(logoutAction)

        val photoAction = View.OnClickListener { pickImage.launch("image/*") }
        binding.btnChangePhoto.setOnClickListener(photoAction)
        binding.btnChangePhotoNP.setOnClickListener(photoAction)

        val extraSettingsAction = View.OnClickListener {
            val rect = android.graphics.Rect()
            binding.tvSettingsTitle.getDrawingRect(rect)
            binding.nestedScrollView.offsetDescendantRectToMyCoords(binding.tvSettingsTitle, rect)
            binding.nestedScrollView.smoothScrollTo(0, rect.top - (16 * resources.displayMetrics.density).toInt())
        }
        binding.btnExtraSettings.setOnClickListener(extraSettingsAction)
        binding.btnExtraSettingsNP.setOnClickListener(extraSettingsAction)

        val openDialogListener = View.OnClickListener { showAccountEditDialog() }
        binding.etSettingsLogin.setOnClickListener(openDialogListener)
        binding.etSettingsPassword.setOnClickListener(openDialogListener)

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

        initPhotoGestures()
        binding.ivProfilePhoto.setOnClickListener { showPhotoActionDialog() }
        
        binding.ivFullscreenPhoto.setOnTouchListener { _, event ->
            if (!isFullScreenMode) return@setOnTouchListener false
            scaleDetector.onTouchEvent(event)
            photoGestureDetector.onTouchEvent(event)
            if (scaleDetector.isInProgress) return@setOnTouchListener true

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    fsStartX = event.rawX
                    fsStartY = event.rawY
                    isFsDismissing = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (photoScale == 1f) {
                        val dx = event.rawX - fsStartX
                        val dy = event.rawY - fsStartY
                        if (!isFsDismissing && (kotlin.math.abs(dx) > 20 || kotlin.math.abs(dy) > 20)) isFsDismissing = true
                        if (isFsDismissing) {
                            photoTranslateX = dx
                            photoTranslateY = dy
                            updateFullscreenPhotoTransform()
                            val distance = kotlin.math.sqrt(dx*dx + dy*dy)
                            val alpha = (1f - (distance / 600f)).coerceIn(0.2f, 1f)
                            binding.viewDimmer.alpha = alpha
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isFsDismissing) {
                        val distance = kotlin.math.sqrt(photoTranslateX * photoTranslateX + photoTranslateY * photoTranslateY)
                        if (distance > 150f) closeFullScreenMode()
                        else {
                            resetFullscreenPhotoTransform()
                            binding.viewDimmer.animate().alpha(1f).setDuration(300).start()
                        }
                        isFsDismissing = false
                    }
                }
            }
            true
        }
    }

    private fun applyAvatarState(avatarUri: String?) {
        if (avatarUri != null) {
            val uri = Uri.parse(avatarUri)
            binding.ivProfilePhoto.setImageURI(uri)
            binding.ivFullscreenPhoto.setImageURI(uri)
            binding.layoutWithPhoto.visibility = View.VISIBLE
            binding.layoutNoPhoto.visibility = View.GONE
        } else {
            binding.layoutWithPhoto.visibility = View.GONE
            binding.layoutNoPhoto.visibility = View.VISIBLE
        }
    }

    private fun initPhotoGestures() {
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                photoScale *= detector.scaleFactor
                photoScale = photoScale.coerceIn(1f, 5f)
                updateFullscreenPhotoTransform()
                return true
            }
        })

        photoGestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (photoScale > 1f) {
                    photoTranslateX -= distanceX
                    photoTranslateY -= distanceY
                    updateFullscreenPhotoTransform()
                    return true
                }
                return false
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (photoScale > 1f) {
                    photoScale = 1f
                    photoTranslateX = 0f
                    photoTranslateY = 0f
                } else {
                    photoScale = 2.5f
                    photoTranslateX = 0f
                    photoTranslateY = 0f
                }
                updateFullscreenPhotoTransform()
                return true
            }
        })
    }

    private fun updateFullscreenPhotoTransform() {
        binding.ivFullscreenPhoto.scaleX = photoScale
        binding.ivFullscreenPhoto.scaleY = photoScale
        binding.ivFullscreenPhoto.translationX = photoTranslateX
        binding.ivFullscreenPhoto.translationY = photoTranslateY
    }

    private fun resetFullscreenPhotoTransform() {
        photoScale = 1f
        photoTranslateX = 0f
        photoTranslateY = 0f
        binding.ivFullscreenPhoto.animate()
            .scaleX(1f).scaleY(1f)
            .translationX(0f).translationY(0f)
            .setDuration(300)
            .start()
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
        dialogBinding.cardContainer.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        dialogBinding.btnOpenPhoto.setOnClickListener {
            hidePhotoActionDialog(dialogBinding)
            if (currentAvatarUri != null) openFullScreenMode()
        }

        dialogBinding.btnChangePhoto.setOnClickListener {
            hidePhotoActionDialog(dialogBinding)
            pickImage.launch("image/*")
        }

        dialogBinding.btnDeletePhoto.setOnClickListener {
            hidePhotoActionDialog(dialogBinding)
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val currentUser = sharedPrefs.getString("current_user", "") ?: ""
            sharedPrefs.edit().remove("${currentUser}_avatar").apply()
            currentAvatarUri = null
            applyAvatarState(null)
            PrimeNotification.show(this, "Фото удалено")
        }

        dialogBinding.btnClose.setOnClickListener { hidePhotoActionDialog(dialogBinding) }
        dialogBinding.dialogRoot.setOnClickListener { hidePhotoActionDialog(dialogBinding) }
        
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        dialogBinding.cardContainer.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var isDragging = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { startX = event.rawX; isDragging = false; return false }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - startX
                        if (deltaX > 30f && !isDragging) isDragging = true
                        if (isDragging) {
                            v.translationX = deltaX.coerceAtLeast(0f)
                            dialogBinding.dialogRoot.alpha = 1f - (deltaX / screenWidth).coerceIn(0f, 0.5f)
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            if (event.rawX - startX > screenWidth / 4) hidePhotoActionDialog(dialogBinding)
                            else {
                                v.animate().translationX(0f).setDuration(200).start()
                                dialogBinding.dialogRoot.animate().alpha(1f).setDuration(200).start()
                            }
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    private fun hidePhotoActionDialog(dialogBinding: com.messenger.prime.databinding.DialogPhotoActionsBinding) {
        dialogBinding.dialogRoot.animate().alpha(0f).setDuration(300).start()
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        dialogBinding.cardContainer.animate()
            .translationX(screenWidth).alpha(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.dialogContainer.visibility = View.GONE
                binding.dialogContainer.removeAllViews()
                activePhotoDialogBinding = null
            }
            .start()
    }

    private fun showAccountEditDialog() {
        val dialogBinding = com.messenger.prime.databinding.DialogEditAccountBinding.inflate(layoutInflater)
        activeDialogBinding = dialogBinding
        binding.dialogContainer.removeAllViews()
        binding.dialogContainer.addView(dialogBinding.root)
        binding.dialogContainer.visibility = View.VISIBLE

        dialogBinding.cardContainer.scaleX = 0.8f
        dialogBinding.cardContainer.scaleY = 0.8f
        dialogBinding.cardContainer.alpha = 0f
        dialogBinding.dialogRoot.alpha = 0f

        dialogBinding.dialogRoot.animate().alpha(1f).setDuration(300).start()
        dialogBinding.cardContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val currentPass = sharedPrefs.getString(currentUser, "") ?: ""

        dialogBinding.etNewLogin.setText(currentUser)
        dialogBinding.etNewPassword.setText(currentPass)

        dialogBinding.btnBack.setOnClickListener { hideAccountEditDialog(dialogBinding) }
        dialogBinding.btnSave.setOnClickListener {
            val newLogin = dialogBinding.etNewLogin.text.toString().trim()
            val newPass = dialogBinding.etNewPassword.text.toString()
            sharedPrefs.edit().apply {
                if (newLogin != currentUser) {
                    val name = sharedPrefs.getString("${currentUser}_name", "Пользователь")
                    val avatar = sharedPrefs.getString("${currentUser}_avatar", null)
                    putString("current_user", newLogin)
                    putString(newLogin, newPass)
                    putString("${newLogin}_name", name)
                    if (avatar != null) putString("${newLogin}_avatar", avatar)
                    remove(currentUser)
                    remove("${currentUser}_name")
                    remove("${currentUser}_avatar")
                } else {
                    putString(currentUser, newPass)
                }
                apply()
            }
            binding.etSettingsLogin.setText(newLogin)
            binding.etSettingsPassword.setText(newPass)
            PrimeNotification.show(this, "Данные обновлены")
            hideAccountEditDialog(dialogBinding)
        }
        
        dialogBinding.dialogRoot.setOnClickListener { hideAccountEditDialog(dialogBinding) }
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        dialogBinding.cardContainer.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var isDragging = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        isDragging = false
                        return false 
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - startX
                        if (deltaX > 30f && !isDragging) {
                            isDragging = true
                        }
                        if (isDragging) {
                            v.translationX = deltaX.coerceAtLeast(0f)
                            dialogBinding.dialogRoot.alpha = 1f - (deltaX / screenWidth).coerceIn(0f, 0.5f)
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            val deltaX = event.rawX - startX
                            if (deltaX > screenWidth / 4) {
                                hideAccountEditDialog(dialogBinding)
                            } else {
                                v.animate().translationX(0f).setDuration(200).start()
                                dialogBinding.dialogRoot.animate().alpha(1f).setDuration(200).start()
                            }
                            isDragging = false
                            return true
                        }
                    }
                }
                return false
            }
        })
        dialogBinding.cardContainer.setOnClickListener { }
    }

    private fun hideAccountEditDialog(dialogBinding: com.messenger.prime.databinding.DialogEditAccountBinding) {
        dialogBinding.dialogRoot.animate().alpha(0f).setDuration(300).start()
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        dialogBinding.cardContainer.animate()
            .translationX(screenWidth).alpha(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.dialogContainer.visibility = View.GONE
                binding.dialogContainer.removeAllViews()
                activeDialogBinding = null
            }
            .start()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            activeDialogBinding?.let { dialogBinding ->
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                val protectedViews = listOf(dialogBinding.btnBack, dialogBinding.etNewLogin, dialogBinding.etNewPassword, dialogBinding.btnSave)
                var hitProtected = false
                val rect = android.graphics.Rect()
                for (view in protectedViews) {
                    view.getGlobalVisibleRect(rect)
                    if (rect.contains(x, y)) { hitProtected = true; break }
                }
                if (!hitProtected) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    currentFocus?.let { focusedView ->
                        imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
                        focusedView.clearFocus()
                    }
                    activePhotoDialogBinding?.let { hidePhotoActionDialog(it) }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun showLogoutDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
            .setTitle("Выход")
            .setMessage("Сделать выход из аккаунта?")
            .setPositiveButton("Да") { _, _ ->
                getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_logged_in", false).apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("Нет", null).show()
    }

    private fun openFullScreenMode() {
        if (currentAvatarUri == null || isFullScreenMode) return
        isFullScreenMode = true
        
        photoScale = 1f
        photoTranslateX = 0f
        photoTranslateY = 0f
        updateFullscreenPhotoTransform()

        binding.ivFullscreenPhoto.visibility = View.VISIBLE
        binding.btnCloseFullPhoto.visibility = View.VISIBLE
        binding.viewDimmer.visibility = View.VISIBLE
        binding.viewDimmer.alpha = 1f
        binding.root.setBackgroundColor(Color.BLACK)
    }

    private fun closeFullScreenMode() {
        if (!isFullScreenMode) return
        isFullScreenMode = false

        binding.ivFullscreenPhoto.visibility = View.GONE
        binding.btnCloseFullPhoto.visibility = View.GONE
        binding.viewDimmer.visibility = View.GONE
        binding.root.setBackgroundColor(Color.parseColor("#F7F8FA"))
        resetFullscreenPhotoTransform()
    }
}
