package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.messenger.prime.databinding.ActivitySettingsAccountBinding
import com.messenger.prime.shapes.M3EShapes
import java.io.File
import java.util.concurrent.Executors

class SettingsAccountActivity : BaseSettingsDetailActivity() {

    private lateinit var b: ActivitySettingsAccountBinding
    private var currentNameInDB = ""
    private var currentLoginInDB = ""
    private var currentPassInDB = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsAccountBinding.inflate(layoutInflater)
        setContentView(b.root)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        currentLoginInDB = currentUser
        currentPassInDB = sharedPrefs.getString(currentUser, "") ?: ""
        val defaultMe = getString(R.string.settings_default_me)
        currentNameInDB = sharedPrefs.getString("my_name", null)
            ?: sharedPrefs.getString("my_local_name", null)
            ?: sharedPrefs.getString("current_user_name", null)
            ?: sharedPrefs.getString("${currentUser}_name", defaultMe) ?: defaultMe

        setupDetailHeroChrome(
            rootView = b.root,
            heroStage = b.layoutAccountHeroStage,
            heroOverlay = b.layoutAccountHeroOverlay,
            scrollView = b.scrollAccountSettings,
            title = getString(R.string.settings_account_title),
            subtitle = currentNameInDB,
            heroHeightFraction = 0.30f,
            heroMinDp = 216,
            heroMaxDp = 268,
            imeHeroHeightFraction = 0.17f,
            imeHeroMinDp = 144,
            imeHeroMaxDp = 180,
            animatedCards = listOf(b.cardAccountForm)
        )

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)
        SettingsSupport.applyM3EBadgeIcon(b.ivIconAccountCard, accentColor, M3EShapes.circle, isDark)
        b.tvTitleAccountCard.setTextColor(accentColor)
        b.btnSaveAccount.backgroundTintList = ColorStateList.valueOf(accentColor)

        b.etSettingsName.setText(currentNameInDB)
        b.etSettingsLogin.setText(currentLoginInDB)
        b.etSettingsPassword.setText(currentPassInDB)

        bindHeroAccountPreview(currentNameInDB)
        setupEditingListeners()
        ColorAccentManager.tintViewTree(b.root, accentColor)
    }

    private fun bindHeroAccountPreview(displayName: String) {
        val safeName = displayName.ifBlank { currentNameInDB.ifBlank { getString(R.string.settings_default_me) } }
        b.tvHeroAccountName.text = safeName

        val avatarUri = AvatarManager.getMyAvatarUri(this)
        val avatarSizePx = (68 * resources.displayMetrics.density).toInt()
        val radiusPx = getAvatarCornerRadiusPx(this, avatarSizePx)

        if (!avatarUri.isNullOrEmpty()) {
            b.ivHeroAvatar.visibility = View.VISIBLE
            b.tvHeroAvatarInitials.visibility = View.GONE
            loadAvatarIntoView(this, avatarUri, b.ivHeroAvatar, safeName)
            b.ivHeroAvatar.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radiusPx.toFloat())
                }
            }
            b.ivHeroAvatar.clipToOutline = true
        } else {
            b.ivHeroAvatar.visibility = View.GONE
            b.tvHeroAvatarInitials.visibility = View.VISIBLE
            b.tvHeroAvatarInitials.text = safeName.take(1).uppercase()
            b.tvHeroAvatarInitials.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusPx.toFloat()
                setColor(ColorAccentManager.getAvatarColor(safeName))
            }
        }
    }

    private fun setupEditingListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAccountChanges()
                b.inputLayoutName.error = null
                b.inputLayoutLogin.error = null
                b.inputLayoutPassword.error = null
                bindHeroAccountPreview(b.etSettingsName.text?.toString()?.trim().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        b.etSettingsName.addTextChangedListener(textWatcher)
        b.etSettingsLogin.addTextChangedListener(textWatcher)
        b.etSettingsPassword.addTextChangedListener(textWatcher)

        b.etSettingsName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                b.etSettingsLogin.requestFocus()
                true
            } else false
        }

        b.etSettingsLogin.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                b.etSettingsPassword.requestFocus()
                true
            } else false
        }

        b.etSettingsPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                b.btnSaveAccount.performClick()
                true
            } else false
        }

        b.btnSaveAccount.setOnClickListener {
            val newName = b.etSettingsName.text?.toString()?.trim().orEmpty()
            val newLogin = b.etSettingsLogin.text?.toString()?.trim().orEmpty()
            val newPass = b.etSettingsPassword.text?.toString().orEmpty()
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)

            val errorName = ValidationUtils.validateName(newName)
            val errorLogin = ValidationUtils.validateLogin(newLogin, sharedPrefs, currentLoginInDB)
            val errorPass = ValidationUtils.validatePassword(newPass)

            if (errorName != null || errorLogin != null || errorPass != null) {
                if (errorName != null) {
                    b.inputLayoutName.error = errorName
                    b.inputLayoutName.shake()
                }
                if (errorLogin != null) {
                    b.inputLayoutLogin.error = errorLogin
                    b.inputLayoutLogin.shake()
                }
                if (errorPass != null) {
                    b.inputLayoutPassword.error = errorPass
                    b.inputLayoutPassword.shake()
                }
                return@setOnClickListener
            }

            val oldName = currentNameInDB
            val oldLogin = currentLoginInDB
            val oldPass = currentPassInDB

            if (newLogin != currentLoginInDB) {
                val oldAvatar = sharedPrefs.getString("${currentLoginInDB}_avatar", null)
                    ?: sharedPrefs.getString("${currentLoginInDB}_avatarUri", null)

                sharedPrefs.edit().apply {
                    remove(currentLoginInDB)
                    remove("${currentLoginInDB}_name")
                    remove("${currentLoginInDB}_avatar")
                    remove("${currentLoginInDB}_avatarUri")
                    putString(newLogin, newPass)
                    putString("${newLogin}_name", newName)
                    if (oldAvatar != null) {
                        putString("${newLogin}_avatar", oldAvatar)
                        putString("${newLogin}_avatarUri", oldAvatar)
                    }
                    apply()
                }

                val oldGif = File(filesDir, "avatar_$currentLoginInDB.gif")
                val oldJpg = File(filesDir, "avatar_$currentLoginInDB.jpg")
                if (oldGif.exists()) {
                    try { oldGif.renameTo(File(filesDir, "avatar_$newLogin.gif")) } catch (_: Exception) {}
                }
                if (oldJpg.exists()) {
                    try { oldJpg.renameTo(File(filesDir, "avatar_$newLogin.jpg")) } catch (_: Exception) {}
                }
            } else {
                sharedPrefs.edit().apply {
                    putString("${currentLoginInDB}_name", newName)
                    putString(currentLoginInDB, newPass)
                    apply()
                }
            }

            AvatarManager.syncActiveUserProfile(this, newLogin, newName)
            currentNameInDB = newName
            currentLoginInDB = newLogin
            currentPassInDB = newPass
            updateDetailSubtitle(b.root, newName)
            Executors.newSingleThreadExecutor().execute {
                SettingsSupport.sendProfileUpdateOverBluetooth(applicationContext)
            }
            sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))

            PrimeNotification.show(this, getString(R.string.settings_account_updated_short)) {
                sharedPrefs.edit().apply {
                    if (newLogin != oldLogin) {
                        val currentAvatar = sharedPrefs.getString("${newLogin}_avatar", null)
                        remove(newLogin)
                        remove("${newLogin}_name")
                        remove("${newLogin}_avatar")
                        putString(oldLogin, oldPass)
                        putString("${oldLogin}_name", oldName)
                        if (currentAvatar != null) putString("${oldLogin}_avatar", currentAvatar)
                    } else {
                        putString("${oldLogin}_name", oldName)
                        putString(oldLogin, oldPass)
                    }
                    apply()
                }
                AvatarManager.syncActiveUserProfile(this, oldLogin, oldName)
                currentNameInDB = oldName
                currentLoginInDB = oldLogin
                currentPassInDB = oldPass
                b.etSettingsName.setText(oldName)
                b.etSettingsLogin.setText(oldLogin)
                b.etSettingsPassword.setText(oldPass)
                updateDetailSubtitle(b.root, oldName)
                checkAccountChanges()
                sendBroadcast(Intent("com.messenger.prime.AVATAR_CHANGED").setPackage(packageName))
            }

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(b.etSettingsName.windowToken, 0)
            b.etSettingsName.clearFocus()
            b.etSettingsLogin.clearFocus()
            b.etSettingsPassword.clearFocus()
            checkAccountChanges()
        }

        checkAccountChanges()
    }

    private fun checkAccountChanges() {
        val newName = b.etSettingsName.text?.toString()?.trim().orEmpty()
        val newLogin = b.etSettingsLogin.text?.toString()?.trim().orEmpty()
        val newPass = b.etSettingsPassword.text?.toString().orEmpty()
        val nameChanged = newName != currentNameInDB
        val loginChanged = newLogin != currentLoginInDB
        val passChanged = newPass != currentPassInDB

        if (nameChanged) {
            b.inputLayoutName.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel)
            b.inputLayoutName.setStartIconOnClickListener { b.etSettingsName.setText(currentNameInDB) }
        } else {
            b.inputLayoutName.startIconDrawable = null
            b.inputLayoutName.setStartIconOnClickListener(null)
        }

        if (loginChanged) {
            b.inputLayoutLogin.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel)
            b.inputLayoutLogin.setStartIconOnClickListener { b.etSettingsLogin.setText(currentLoginInDB) }
        } else {
            b.inputLayoutLogin.startIconDrawable = null
            b.inputLayoutLogin.setStartIconOnClickListener(null)
        }

        if (passChanged) {
            b.inputLayoutPassword.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_cancel)
            b.inputLayoutPassword.setStartIconOnClickListener { b.etSettingsPassword.setText(currentPassInDB) }
        } else {
            b.inputLayoutPassword.startIconDrawable = null
            b.inputLayoutPassword.setStartIconOnClickListener(null)
        }

        val show = (nameChanged || loginChanged || passChanged) && newLogin.isNotEmpty() && newName.isNotEmpty()
        b.btnSaveAccount.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
