package com.abakada.batibooktwo

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Initialize UI elements
        val backButton = findViewById<Button>(R.id.back_button)
        val darkModeSwitch = findViewById<Switch>(R.id.dark_mode_switch)
        val notificationsSwitch = findViewById<Switch>(R.id.notifications_switch)
        val languageText = findViewById<TextView>(R.id.language_text)
        val languageButton = findViewById<Button>(R.id.language_button)
        val aboutButton = findViewById<Button>(R.id.about_button)
        val helpButton = findViewById<Button>(R.id.help_button)
        val privacyButton = findViewById<Button>(R.id.privacy_button)
        
        // Load saved preferences
        val sharedPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val isDarkMode = ThemeManager.isDarkMode(this)
        val currentLanguage = LanguageManager.getCurrentLanguage(this)
        
        // Set initial states
        darkModeSwitch.isChecked = isDarkMode
        languageText.text = LanguageManager.getLanguageDisplayName(currentLanguage)
        
        // Back button functionality
        backButton.setOnClickListener {
            finish()
        }
        
        // Dark mode switch functionality
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val theme = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.saveTheme(this, theme)
            ThemeManager.applyTheme(theme)
            
            recreate()
        }
        
        // Notifications switch functionality
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("notifications", isChecked).apply()
        }
        
        // Language button functionality
        languageButton.setOnClickListener {
            showLanguageDialog()
        }
        
        // About button functionality
        aboutButton.setOnClickListener {
            val intent = android.content.Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
        
        // Help button functionality
        helpButton.setOnClickListener {
            // TODO: Implement help functionality
        }
        
        // Privacy button functionality
        privacyButton.setOnClickListener {
            // TODO: Implement privacy policy functionality
        }
    }
    
    private fun showLanguageDialog() {
        val languages = LanguageManager.getAvailableLanguages()
        val languageNames = languages.map { it.second }.toTypedArray()
        val currentLanguage = LanguageManager.getCurrentLanguage(this)
        val currentIndex = languages.indexOfFirst { it.first == currentLanguage }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                val selectedLanguage = languages[which].first
                LanguageManager.setLanguage(this, selectedLanguage)
                
                // Update the current language display
                val languageText = findViewById<TextView>(R.id.language_text)
                languageText.text = languages[which].second
                
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
