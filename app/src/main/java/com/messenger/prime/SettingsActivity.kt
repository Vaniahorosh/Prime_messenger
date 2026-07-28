package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.messenger.prime.databinding.ActivitySettingsBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private var currentAvatarUri: String? = null
    private var activeNameDialogBinding: com.messenger.prime.databinding.DialogEditNameBinding? = null
    private var activePhotoDialogBinding: com.messenger.prime.databinding.DialogPhotoActionsBinding? = null

    // Текущие данные пользователя в базе (для отслеживания изменений)
    private var currentLoginInDB: String = ""
    private var currentPassInDB: String = ""

    // Launcher для открытия PhotoViewActivity и получения результата (если фото изменили или удалили)
    private val photoViewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data?.getBooleanExtra("DELETED", false) == true) {
                handlePhotoDeletionWithUndo(currentAvatarUri)
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

    // Launcher для фоторедактора
    private val photoEditorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val editedUriString = result.data?.getStringExtra("EDITED_IMAGE_URI")
            if (editedUriString != null) {
                val editedUri = Uri.parse(editedUriString)
                // Для файлов из кэша (Uri.fromFile) НЕЛЬЗЯ вызывать takePersistableUriPermission
                currentAvatarUri = editedUri.toString()
                
                val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                val currentUser = sharedPrefs.getString("current_user", "") ?: ""
                sharedPrefs.edit().putString("${currentUser}_avatar", currentAvatarUri).apply()
                
                applyAvatarState(currentAvatarUri)
                PrimeNotification.show(this, "Фото обновлено")
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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        val slidrConfig = SlidrConfig.Builder().position(SlidrPosition.LEFT).build()
        Slidr.attach(this, slidrConfig)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""

        currentLoginInDB = currentUser
        currentPassInDB = sharedPrefs.getString(currentUser, "") ?: ""

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
            binding.layoutSettingsTitle.getDrawingRect(rect)
            binding.nestedScrollView.offsetDescendantRectToMyCoords(binding.layoutSettingsTitle, rect)
            binding.nestedScrollView.smoothScrollTo(0, rect.top - (16 * resources.displayMetrics.density).toInt())
        }
        binding.btnExtraSettings.setOnClickListener(extraSettingsAction)
        binding.btnExtraSettingsNP.setOnClickListener(extraSettingsAction)

        setupInlineAccountEditing()

        val openNameDialogListener = View.OnClickListener { showNameEditDialog() }
        binding.floatingTitleContainer.setOnClickListener(openNameDialogListener)
        binding.tvUserNameStatic.setOnClickListener(openNameDialogListener)

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
            handlePhotoDeletionWithUndo(currentAvatarUri)
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

    private fun showNameEditDialog() {
        val dialogBinding = com.messenger.prime.databinding.DialogEditNameBinding.inflate(layoutInflater)
        activeNameDialogBinding = dialogBinding
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

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val currentName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"

        dialogBinding.etNewName.setText(currentName)
        
        // Кнопка сохранить изначально выключена (т.к. изменений нет)
        dialogBinding.btnSave.isEnabled = false
        dialogBinding.btnSave.alpha = 0.5f

        dialogBinding.etNewName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newName = s.toString().trim()
                val isChanged = newName != currentName && newName.isNotEmpty()
                dialogBinding.btnSave.isEnabled = isChanged
                dialogBinding.btnSave.alpha = if (isChanged) 1.0f else 0.5f
                dialogBinding.inputLayoutName.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        dialogBinding.etNewName.setOnEditorActionListener { textView: android.widget.TextView?, actionId: Int, event: android.view.KeyEvent? ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                if (dialogBinding.btnSave.isEnabled) {
                    dialogBinding.btnSave.performClick()
                }
                true
            } else false
        }

        dialogBinding.btnBack.setOnClickListener { hideNameEditDialog(dialogBinding) }
        dialogBinding.btnSave.setOnClickListener {
            val newName = dialogBinding.etNewName.text.toString().trim()
            val error = ValidationUtils.getValidationError(newName, false)
            if (error != null) {
                dialogBinding.inputLayoutName.error = error
                dialogBinding.cardContainer.shake()
                return@setOnClickListener
            }
            val oldName = currentName // Запоминаем старое имя для отмены
            
            sharedPrefs.edit().putString("${currentUser}_name", newName).apply()
            
            binding.tvUserNameFloating.text = newName
            binding.tvUserNameStatic.text = newName
            
            PrimeNotification.show(this, "Имя обновлено") {
                // Логика отката имени (Undo)
                sharedPrefs.edit().putString("${currentUser}_name", oldName).apply()
                binding.tvUserNameFloating.text = oldName
                binding.tvUserNameStatic.text = oldName
            }
            hideNameEditDialog(dialogBinding)
        }
        
        dialogBinding.dialogRoot.setOnClickListener { hideNameEditDialog(dialogBinding) }
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
                            if (event.rawX - startX > screenWidth / 4) hideNameEditDialog(dialogBinding)
                            else {
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

    private fun hideNameEditDialog(dialogBinding: com.messenger.prime.databinding.DialogEditNameBinding) {
        dialogBinding.dialogRoot.animate().alpha(0f).setDuration(300).start()
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        dialogBinding.cardContainer.animate()
            .translationX(screenWidth).alpha(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.dialogContainer.visibility = View.GONE
                binding.dialogContainer.removeAllViews()
                activeNameDialogBinding = null
            }
            .start()
    }

    private fun setupInlineAccountEditing() {
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAccountChanges()
                binding.inputLayoutLogin.error = null
                binding.inputLayoutPassword.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        binding.etSettingsLogin.addTextChangedListener(textWatcher)
        binding.etSettingsPassword.addTextChangedListener(textWatcher)

        binding.etSettingsLogin.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                binding.etSettingsPassword.requestFocus()
                true
            } else false
        }

        binding.etSettingsPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                if (binding.btnSaveAccount.visibility == View.VISIBLE) {
                    binding.btnSaveAccount.performClick()
                }
                true
            } else false
        }


        // Логика отмены изменений
        // Оставляем пустым, будем настраивать динамически в checkAccountChanges

        binding.btnSaveAccount.setOnClickListener {
            val newLogin = binding.etSettingsLogin.text.toString().trim()
            val newPass = binding.etSettingsPassword.text.toString()
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)

            var hasError = false
            if (newLogin.isEmpty()) {
                binding.inputLayoutLogin.error = "Логин не может быть пустым"
                binding.inputLayoutLogin.shake()
                hasError = true
            } else {
                val loginError = ValidationUtils.getValidationError(newLogin, true)
                if (loginError != null) {
                    binding.inputLayoutLogin.error = loginError
                    binding.inputLayoutLogin.shake()
                    hasError = true
                } else {
                    binding.inputLayoutLogin.error = null
                }
            }

            if (newPass.length < 8) {
                binding.inputLayoutPassword.error = "Минимум 8 символов"
                binding.inputLayoutPassword.shake()
                hasError = true
            } else {
                binding.inputLayoutPassword.error = null
            }

            if (hasError) return@setOnClickListener

            // Проверка: занят ли новый логин другим пользователем
            if (newLogin != currentLoginInDB && sharedPrefs.contains(newLogin)) {
                binding.inputLayoutLogin.error = "Этот логин уже занят"
                binding.inputLayoutLogin.shake()
                return@setOnClickListener
            }

            // Сохранение
            sharedPrefs.edit().apply {
                if (newLogin != currentLoginInDB) {
                    val name = sharedPrefs.getString("${currentLoginInDB}_name", "Пользователь")
                    val avatar = sharedPrefs.getString("${currentLoginInDB}_avatar", null)
                    putString("current_user", newLogin)
                    putString(newLogin, newPass)
                    putString("${newLogin}_name", name)
                    if (avatar != null) putString("${newLogin}_avatar", avatar)
                    remove(currentLoginInDB)
                    remove("${currentLoginInDB}_name")
                    remove("${currentLoginInDB}_avatar")
                } else {
                    putString(currentLoginInDB, newPass)
                }
                apply()
            }
            
            // Запоминаем старые данные для возможности отмены
            val oldLogin = currentLoginInDB
            val oldPass = currentPassInDB
            
            // Обновляем текущие данные в памяти
            currentLoginInDB = newLogin
            currentPassInDB = newPass
            
            PrimeNotification.show(this, "Данные обновлены") {
                // Логика отката изменений (Undo)
                sharedPrefs.edit().apply {
                    if (newLogin != oldLogin) {
                        val name = sharedPrefs.getString("${newLogin}_name", "Пользователь")
                        val avatar = sharedPrefs.getString("${newLogin}_avatar", null)
                        
                        putString("current_user", oldLogin)
                        putString(oldLogin, oldPass)
                        putString("${oldLogin}_name", name)
                        if (avatar != null) putString("${oldLogin}_avatar", avatar)
                        
                        remove(newLogin)
                        remove("${newLogin}_name")
                        remove("${newLogin}_avatar")
                    } else {
                        putString(oldLogin, oldPass)
                    }
                    apply()
                }
                
                // Возвращаем старые данные в память и UI
                currentLoginInDB = oldLogin
                currentPassInDB = oldPass
                binding.etSettingsLogin.setText(oldLogin)
                binding.etSettingsPassword.setText(oldPass)
                checkAccountChanges()
            }
            
            // Прячем клавиатуру и сбрасываем фокус
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSettingsPassword.windowToken, 0)
            binding.etSettingsLogin.clearFocus()
            binding.etSettingsPassword.clearFocus()

            // Скрываем кнопку и крестики
            checkAccountChanges()
        }

        // Первичная проверка (чтобы скрыть иконки при запуске)
        checkAccountChanges()
    }

    private fun checkAccountChanges() {
        val newLogin = binding.etSettingsLogin.text.toString().trim()
        val newPass = binding.etSettingsPassword.text.toString()

        val loginChanged = newLogin != currentLoginInDB
        val passChanged = newPass != currentPassInDB

        // Управляем крестиком СЛЕВА (startIcon), глазик СПРАВА остается всегда
        if (loginChanged) {
            binding.inputLayoutLogin.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel)
            binding.inputLayoutLogin.setStartIconOnClickListener {
                binding.etSettingsLogin.setText(currentLoginInDB)
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        } else {
            binding.inputLayoutLogin.startIconDrawable = null
            binding.inputLayoutLogin.setStartIconOnClickListener(null)
        }
            
        if (passChanged) {
            binding.inputLayoutPassword.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel)
            binding.inputLayoutPassword.setStartIconOnClickListener {
                binding.etSettingsPassword.setText(currentPassInDB)
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        } else {
            binding.inputLayoutPassword.startIconDrawable = null
            binding.inputLayoutPassword.setStartIconOnClickListener(null)
        }

        val hasChanges = (loginChanged || passChanged) && newLogin.isNotEmpty()
        animateSaveButton(hasChanges)
    }

    private fun animateSaveButton(show: Boolean) {
        if (show && binding.btnSaveAccount.visibility == View.VISIBLE) return
        if (!show && binding.btnSaveAccount.visibility == View.GONE) return

        TransitionManager.beginDelayedTransition(
            binding.layoutAccountData,
            TransitionSet().apply {
                addTransition(Fade())
                addTransition(ChangeBounds())
                duration = 300
                interpolator = DecelerateInterpolator()
            }
        )
        binding.btnSaveAccount.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.rawX.toInt()
            val y = event.rawY.toInt()

            var hitProtected = false
            val rect = android.graphics.Rect()

            // Проверка для диалога имени
            activeNameDialogBinding?.let { db ->
                val protectedViews = listOf(db.btnBack, db.etNewName, db.btnSave)
                for (view in protectedViews) {
                    view.getGlobalVisibleRect(rect)
                    if (rect.contains(x, y)) { hitProtected = true; break }
                }
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
        return super.dispatchTouchEvent(event)
    }

    private fun handlePhotoDeletionWithUndo(uriToDelete: String?) {
        if (uriToDelete == null) return
        
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        
        sharedPrefs.edit().remove("${currentUser}_avatar").apply()
        currentAvatarUri = null
        applyAvatarState(null)
        
        PrimeNotification.show(this, "Фото удалено") {
            sharedPrefs.edit().putString("${currentUser}_avatar", uriToDelete).apply()
            currentAvatarUri = uriToDelete
            applyAvatarState(uriToDelete)
        }
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
