package com.abakada.batibooktwo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.edit

class Profile : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements
        val darkModeSwitch = view.findViewById<Switch>(R.id.dark_mode_switch)
        val notificationsSwitch = view.findViewById<Switch>(R.id.notifications_switch)
        val currentLanguageText = view.findViewById<TextView>(R.id.current_language)
        val languageSettingLayout = view.findViewById<View>(R.id.layout_language_setting)
        val aboutLayout = view.findViewById<View>(R.id.layout_about)
        val logoutLayout = view.findViewById<View>(R.id.layout_logout)

        // Load saved preferences
        val sharedPrefs = requireContext().getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
        
        // Set current language
        val currentLanguage = LanguageManager.getCurrentLanguage(requireContext())
        val languageDisplayName = LanguageManager.getLanguageDisplayName(currentLanguage)
        currentLanguageText.text = languageDisplayName

        // Set dark mode switch state based on current theme
        val isDarkMode = ThemeManager.isDarkMode(requireContext())
        darkModeSwitch.isChecked = isDarkMode

        // Set notifications switch state
        val notificationsEnabled = sharedPrefs.getBoolean("notifications", true)
        notificationsSwitch.isChecked = notificationsEnabled

        // Dark mode switch listener
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val theme = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.saveTheme(requireContext(), theme)
            ThemeManager.applyTheme(theme)
            
            // Restart activity to apply theme changes
            requireActivity().recreate()
        }

        // Notifications switch listener
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit { putBoolean("notifications", isChecked) }
        }

        // Language setting click listener
        languageSettingLayout.setOnClickListener {
            showLanguageSelectionDialog()
        }

        // About BatiBook click listener
        aboutLayout.setOnClickListener {
            val intent = Intent(requireContext(), AboutActivity::class.java)
            startActivity(intent)
        }


        // Logout click listener
        logoutLayout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLanguageSelectionDialog() {
        val languages = LanguageManager.getAvailableLanguages()
        val languageNames = languages.map { it.second }.toTypedArray()
        val currentLanguage = LanguageManager.getCurrentLanguage(requireContext())
        val currentIndex = languages.indexOfFirst { it.first == currentLanguage }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Language")
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                val selectedLanguage = languages[which].first
                LanguageManager.setLanguage(requireContext(), selectedLanguage)
                
                // Update the current language display
                val currentLanguageText = view?.findViewById<TextView>(R.id.current_language)
                currentLanguageText?.text = languages[which].second
                
                dialog.dismiss()
                
                // Restart activity to apply language changes
                requireActivity().recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                // Clear user data
                val sharedPrefs = requireContext().getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
                sharedPrefs.edit().clear().apply()
                
                // TODO: Navigate to login screen or restart app
                // For now, just show a message
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Profile().apply {
                arguments = Bundle().apply {
                    putString("param1", param1)
                    putString("param2", param2)
                }
            }
    }
}