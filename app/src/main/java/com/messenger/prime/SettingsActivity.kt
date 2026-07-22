package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

    // Launcher для открытия PhotoViewActivity и получения результата (если фото изменили или удалили)
    private val photoViewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data?.getBooleanExtra("DELETED", false) == true) {
                // Фото удалено
                currentAvatarUri = null
                val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                val currentUser = sharedPrefs.getString("current_user", "") ?: ""
                sharedPrefs.edit().remove("${currentUser}_avatar").apply()
                applyAvatarState(null)
            } else {
                val newUri = data?.getStringExtra("NEW_URI")
                if (newUri != null) {
                    // Фото обновлено
                    currentAvatarUri = newUri
                    val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                    val currentUser = sharedPrefs.getString("current_user", "") ?: ""
                    sharedPrefs.edit().putString("${currentUser}_avatar", newUri).apply()
                    applyAvatarState(newUri)
                }
            }
        }
    }

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
        
        // Активируем "выделение" для работы бегущей строки (marquee)
        binding.tvUserNameFloating.isSelected = true
        binding.tvUserNameStatic.isSelected = true

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

        binding.ivProfilePhoto.setOnClickListener { showPhotoActionDialog() }
    }

    private fun applyAvatarState(avatarUri: String?) {
        if (avatarUri != null) {
            val uri = Uri.parse(avatarUri)
            binding.ivProfilePhoto.setImageURI(uri)
            binding.layoutWithPhoto.visibility = View.VISIBLE
            binding.layoutNoPhoto.visibility = View.GONE
        } else {
            binding.layoutWithPhoto.visibility = View.GONE
            binding.layoutNoPhoto.visibility = View.VISIBLE
        }
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
            if (currentAvatarUri != null) {
                val intent = Intent(this, PhotoViewActivity::class.java)
                intent.putExtra("EXTRA_URI", currentAvatarUri)
                
                // Передаем координаты для анимации
                val rect = Rect()
                binding.ivProfilePhoto.getGlobalVisibleRect(rect)
                intent.putExtra("EXTRA_RECT", rect)
                
                photoViewLauncher.launch(intent)
                overridePendingTransition(0, 0)
            }
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

            var hasError = false
            if (newLogin.isEmpty()) {
                dialogBinding.inputLayoutLogin.error = "Логин не может быть пустым"
                dialogBinding.inputLayoutLogin.shake()
                hasError = true
            } else {
                dialogBinding.inputLayoutLogin.error = null
            }

            if (newPass.length < 8) {
                dialogBinding.inputLayoutPassword.error = "Минимум 8 символов"
                dialogBinding.inputLayoutPassword.shake()
                hasError = true
            } else {
                dialogBinding.inputLayoutPassword.error = null
            }

            if (hasError) return@setOnClickListener

            // Проверка на отсутствие изменений
            if (newLogin == currentUser && newPass == currentPass) {
                PrimeNotification.show(this@SettingsActivity, "Изменений не обнаружено")
                return@setOnClickListener
            }

            // Проверка: занят ли новый логин другим пользователем
            if (newLogin != currentUser && sharedPrefs.contains(newLogin)) {
                dialogBinding.inputLayoutLogin.error = "Этот логин уже занят"
                dialogBinding.inputLayoutLogin.shake()
                return@setOnClickListener
            }

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
}
