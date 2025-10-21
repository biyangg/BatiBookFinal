package com.abakada.batibooktwo

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
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
            // Toast notification for back navigation - SettingsActivity
            val context = getApplicationContext()
            val txt = "Settings saved"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
        
        // Dark mode switch functionality
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val theme = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.saveTheme(this, theme)
            ThemeManager.applyTheme(theme)
            
            // Toast notification for dark mode toggle - SettingsActivity
            val context = getApplicationContext()
            val txt = if (isChecked) "Dark mode enabled" else "Dark mode disabled"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            
            recreate()
        }
        
        // Notifications switch functionality
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("notifications", isChecked).apply()
            
            // Toast notification for notifications toggle - SettingsActivity
            val context = getApplicationContext()
            val txt = if (isChecked) "Notifications enabled" else "Notifications disabled"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
        
        // Language button functionality
        languageButton.setOnClickListener {
            showLanguageDialog()
        }
        
        // About button functionality
        aboutButton.setOnClickListener {
            val intent = android.content.Intent(this, AboutActivity::class.java)
            startActivity(intent)
            // Toast notification for about navigation - SettingsActivity
            val context = getApplicationContext()
            val txt = "Opening About BatiBook"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
        
        // Help button functionality
        helpButton.setOnClickListener {
            // Toast notification for help functionality - SettingsActivity
            val context = getApplicationContext()
            val txt = "Opening Help Center"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            // TODO: Implement help functionality
        }
        
        // Privacy button functionality
        privacyButton.setOnClickListener {
            // Toast notification for privacy functionality - SettingsActivity
            val context = getApplicationContext()
            val txt = "Opening Privacy Policy"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
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
                
                // Toast notification for language change - SettingsActivity
                val context = getApplicationContext()
                val txt = "Language changed to ${languages[which].second}"
                val time = Toast.LENGTH_SHORT
                val toast = Toast.makeText(context, txt, time)
                toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
                toast.show()
                
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
