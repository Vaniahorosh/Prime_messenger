package com.messenger.prime

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class PrimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val theme = sharedPrefs.getString("app_theme", "system")
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
