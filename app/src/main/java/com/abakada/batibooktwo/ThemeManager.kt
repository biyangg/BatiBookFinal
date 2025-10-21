package com.abakada.batibooktwo

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"
    
    fun applyTheme(theme: String) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    fun getCurrentTheme(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return sharedPrefs.getString("theme", THEME_SYSTEM) ?: THEME_SYSTEM
    }
    
    fun saveTheme(context: Context, theme: String) {
        val sharedPrefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("theme", theme).apply()
    }
    
    fun isDarkMode(context: Context): Boolean {
        val currentTheme = getCurrentTheme(context)
        return when (currentTheme) {
            THEME_DARK -> true
            THEME_LIGHT -> false
            THEME_SYSTEM -> {
                val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }
            else -> false
        }
    }
}

