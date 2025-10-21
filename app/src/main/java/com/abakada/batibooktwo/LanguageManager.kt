package com.abakada.batibooktwo

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.*

object LanguageManager {
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_FILIPINO = "fil"
    
    fun setLanguage(context: Context, languageCode: String) {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
        
        // Save language preference
        val sharedPrefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("language", languageCode).apply()
    }
    
    fun getCurrentLanguage(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return sharedPrefs.getString("language", LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }
    
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_ENGLISH -> "English"
            LANGUAGE_FILIPINO -> "Filipino"
            else -> "English"
        }
    }
    
    fun getAvailableLanguages(): List<Pair<String, String>> {
        return listOf(
            Pair(LANGUAGE_ENGLISH, "English"),
            Pair(LANGUAGE_FILIPINO, "Filipino")
        )
    }
}
