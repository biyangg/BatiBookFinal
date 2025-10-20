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
import android.widget.Toast
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
        val savedLanguage = sharedPrefs.getString("language", "English") ?: "English"
        currentLanguageText.text = savedLanguage

        // Set dark mode switch state
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        darkModeSwitch.isChecked = isDarkMode

        // Set notifications switch state
        val notificationsEnabled = sharedPrefs.getBoolean("notifications", true)
        notificationsSwitch.isChecked = notificationsEnabled

        // Dark mode switch listener
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit { putBoolean("dark_mode", isChecked) }
            Toast.makeText(requireContext(), 
                if (isChecked) "Dark mode enabled" else "Dark mode disabled", 
                Toast.LENGTH_SHORT).show()
        }

        // Notifications switch listener
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit { putBoolean("notifications", isChecked) }
            Toast.makeText(requireContext(), 
                if (isChecked) "Notifications enabled" else "Notifications disabled", 
                Toast.LENGTH_SHORT).show()
        }

        // Language setting click listener
        languageSettingLayout.setOnClickListener {
            Toast.makeText(requireContext(), "Language settings opened", Toast.LENGTH_SHORT).show()
            // TODO: Open language selection dialog
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

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                // Clear user data
                val sharedPrefs = requireContext().getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
                sharedPrefs.edit().clear().apply()
                
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                
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