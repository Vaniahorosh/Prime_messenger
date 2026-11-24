package com.messenger.prime

import android.content.res.ColorStateList
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.core.graphics.ColorUtils
import com.messenger.prime.browser.BrowserPreferences
import com.messenger.prime.databinding.ActivitySettingsBrowserBinding
import com.messenger.prime.shapes.M3EDrawable
import com.messenger.prime.shapes.M3EShapes

class SettingsBrowserActivity : BaseSettingsDetailActivity() {

    private lateinit var b: ActivitySettingsBrowserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBrowserBinding.inflate(layoutInflater)
        setContentView(b.root)

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)

        setupDetailHeroChrome(
            rootView = b.root,
            heroStage = b.layoutBrowserHeroStage,
            heroOverlay = b.layoutBrowserHeroOverlay,
            scrollView = b.scrollBrowserSettings,
            title = "Браузер",
            subtitle = null,
            heroHeightFraction = 0.29f,
            heroMinDp = 212,
            heroMaxDp = 264,
            animatedCards = listOf(b.cardBrowserSettings, b.cardBrowserPrivacy, b.cardBrowserClearCache)
        )

        b.flBrowserHeroBadge.background = M3EDrawable(
            shape = M3EShapes.verySunny,
            color = ColorUtils.setAlphaComponent(accentColor, if (isDark) 68 else 36)
        )
        b.ivBrowserHeroIcon.imageTintList = ColorStateList.valueOf(accentColor)

        SettingsSupport.applyM3EBadgeIcon(b.ivIconBrowserCard, accentColor, M3EShapes.verySunny, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivIconBrowserPrivacy, accentColor, M3EShapes.cookie6Sided, isDark)
        b.tvTitleBrowserCard.setTextColor(accentColor)
        b.tvTitleBrowserPrivacy.setTextColor(accentColor)
        b.tvSearchEngineValue.setTextColor(accentColor)
        b.cardBrowserClearCache.addBounceTouchEffect()

        val searchEngines = arrayOf("Google", "Яндекс", "DuckDuckGo", "Bing")

        val refreshHeroStatus = {
            val enabled = BrowserPreferences.isInternalBrowserEnabled(this)
            val engine = BrowserPreferences.getSearchEngine(this)
            val jsOn = BrowserPreferences.isJavaScriptEnabled(this)
            val cookiesOn = BrowserPreferences.areCookiesEnabled(this)

            b.tvSearchEngineValue.text = engine
            b.tvHeroBrowserStatus.text = if (enabled) "Встроенный браузер" else "Внешний браузер"
            b.tvHeroBrowserSummary.text = if (enabled) {
                val jsLabel = if (jsOn) "JS вкл" else "JS выкл"
                val cookiesLabel = if (cookiesOn) "Куки вкл" else "Куки выкл"
                "Поиск: $engine • $jsLabel • $cookiesLabel"
            } else {
                "Ссылки открываются в системном браузере"
            }
            updateDetailSubtitle(b.root, if (enabled) engine else "Внешний")
        }

        b.switchUseInternalBrowser.isChecked = BrowserPreferences.isInternalBrowserEnabled(this)
        b.switchUseInternalBrowser.setOnCheckedChangeListener { _, isChecked ->
            BrowserPreferences.setInternalBrowserEnabled(this, isChecked)
            refreshHeroStatus()
        }

        b.rowSearchEngine.setOnClickListener { v ->
            val current = searchEngines.indexOf(BrowserPreferences.getSearchEngine(this)).coerceAtLeast(0)
            val menu = PrimePopupMenu.create(this, v)
            searchEngines.forEachIndexed { index, title ->
                menu.addItem(title, isSelected = (index == current)) {
                    BrowserPreferences.setSearchEngine(this, title)
                    refreshHeroStatus()
                }
            }
            menu.show()
        }

        b.switchBrowserJs.isChecked = BrowserPreferences.isJavaScriptEnabled(this)
        b.switchBrowserJs.setOnCheckedChangeListener { _, isChecked ->
            BrowserPreferences.setJavaScriptEnabled(this, isChecked)
            refreshHeroStatus()
        }

        b.switchBrowserCookies.isChecked = BrowserPreferences.areCookiesEnabled(this)
        b.switchBrowserCookies.setOnCheckedChangeListener { _, isChecked ->
            BrowserPreferences.setCookiesEnabled(this, isChecked)
            refreshHeroStatus()
        }

        b.switchBrowserDesktopMode.isChecked = BrowserPreferences.isDesktopModeEnabled(this)
        b.switchBrowserDesktopMode.setOnCheckedChangeListener { _, isChecked ->
            BrowserPreferences.setDesktopModeEnabled(this, isChecked)
        }

        b.layoutClearBrowserCache.setOnClickListener {
            try {
                CookieManager.getInstance().removeAllCookies(null)
                WebStorage.getInstance().deleteAllData()
                PrimeNotification.show(this, "Кэш и данные браузера очищены")
            } catch (_: Exception) {
                PrimeNotification.show(this, "Ошибка при очистке кэша")
            }
        }

        refreshHeroStatus()
        ColorAccentManager.tintViewTree(b.root, accentColor)
    }
}
