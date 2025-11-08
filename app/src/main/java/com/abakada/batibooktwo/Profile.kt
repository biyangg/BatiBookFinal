package com.abakada.batibooktwo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import android.widget.EditText

class Profile : Fragment() {

    private var isSignInMode = true
    private lateinit var authService: FirebaseAuthService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        authService = FirebaseAuthService.getInstance()
        
        // Check if user is authenticated
        if (authService.isUserAuthenticated()) {
            // User is logged in - show profile
            return inflater.inflate(R.layout.fragment_profile, container, false)
        } else {
            // User is not logged in - show login/signup
            return inflater.inflate(R.layout.login_layout, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check which layout is shown
        if (authService.isUserAuthenticated()) {
            setupProfileView(view)
        } else {
            setupLoginView(view)
        }
    }

    private fun setupLoginView(view: View) {
        val btnSignIn = view.findViewById<Button>(R.id.btn_sign_in)
        val btnSignUp = view.findViewById<Button>(R.id.btn_sign_up)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit)
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val etName = view.findViewById<EditText>(R.id.et_name)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tv_forgot_password)
        val tvError = view.findViewById<TextView>(R.id.tv_error)

        // Toggle between Sign In and Sign Up
        btnSignIn.setOnClickListener {
            isSignInMode = true
            btnSignIn.setTextColor(resources.getColor(android.R.color.white, null))
            btnSignIn.background = resources.getDrawable(R.drawable.button_primary, null)
            btnSignUp.setTextColor(resources.getColor(android.R.color.black, null))
            btnSignUp.background = null
            btnSubmit.text = "Sign In"
            etName.visibility = View.GONE
            tvForgotPassword.visibility = View.VISIBLE
        }

        btnSignUp.setOnClickListener {
            isSignInMode = false
            btnSignUp.setTextColor(resources.getColor(android.R.color.white, null))
            btnSignUp.background = resources.getDrawable(R.drawable.button_primary, null)
            btnSignIn.setTextColor(resources.getColor(android.R.color.black, null))
            btnSignIn.background = null
            btnSubmit.text = "Sign Up"
            etName.visibility = View.VISIBLE
            tvForgotPassword.visibility = View.GONE
        }

        // Submit button
        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                tvError.text = "Please fill in all fields"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (password.length < 6) {
                tvError.text = "Password must be at least 6 characters"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE

            if (isSignInMode) {
                // Sign In
                authService.signInUser(email, password, object : FirebaseAuthService.AuthCallback {
                    override fun onSuccess(user: com.google.firebase.auth.FirebaseUser?) {
                        // Update last login in Firestore
                        user?.let {
                            val userService = UserService.getInstance()
                            userService.updateLastLogin(it.uid)
                        }
                        
                        Toast.makeText(requireContext(), "Signed in successfully!", Toast.LENGTH_SHORT).show()
                        // Update display name if available
                        user?.displayName?.let { name ->
                            view.findViewById<TextView>(R.id.profile_name)?.text = name
                        }
                        // Refresh fragment to show profile
                        parentFragmentManager.beginTransaction().detach(this@Profile).attach(this@Profile).commit()
                        // Update navigation visibility
                        (activity as? MainActivity)?.updateBottomNavigationVisibility()
                    }
                    override fun onError(error: String) {
                        tvError.text = error
                        tvError.visibility = View.VISIBLE
                    }
                })
            } else {
                // Sign Up
                val name = etName.text.toString().trim()
                if (TextUtils.isEmpty(name)) {
                    tvError.text = "Please enter your name"
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                authService.registerUser(email, password, object : FirebaseAuthService.AuthCallback {
                    override fun onSuccess(user: com.google.firebase.auth.FirebaseUser?) {
                        // Update display name and save to Firestore
                        user?.let {
                            val userService = UserService.getInstance()
                            
                            // Save user profile to Firestore (default userType is "user")
                            userService.createUserProfile(
                                userId = it.uid,
                                email = email,
                                displayName = name,
                                userType = "user", // Default to "user"
                                object : UserService.UserCallback {
                                    override fun onSuccess() {
                                        // Update display name in Firebase Auth
                                        authService.updateDisplayName(name, object : FirebaseAuthService.AuthCallback {
                                            override fun onSuccess(user: com.google.firebase.auth.FirebaseUser?) {
                                                Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()
                                                // Refresh fragment to show profile
                                                parentFragmentManager.beginTransaction().detach(this@Profile).attach(this@Profile).commit()
                                            }
                                            override fun onError(error: String) {
                                                Toast.makeText(requireContext(), "Account created but failed to update name: $error", Toast.LENGTH_SHORT).show()
                                                parentFragmentManager.beginTransaction().detach(this@Profile).attach(this@Profile).commit()
                                            }
                                        })
                                    }
                                    override fun onError(error: String) {
                                        Toast.makeText(requireContext(), "Account created but failed to save profile: $error", Toast.LENGTH_SHORT).show()
                                        parentFragmentManager.beginTransaction().detach(this@Profile).attach(this@Profile).commit()
                                    }
                                }
                            )
                        }
                    }
                    override fun onError(error: String) {
                        tvError.text = error
                        tvError.visibility = View.VISIBLE
                    }
                })
            }
        }

        // Forgot password
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(requireContext(), "Please enter your email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authService.resetPassword(email, object : FirebaseAuthService.AuthCallback {
                override fun onSuccess(user: com.google.firebase.auth.FirebaseUser?) {
                    Toast.makeText(requireContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show()
                }
                override fun onError(error: String) {
                    Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setupProfileView(view: View) {
        // Initialize UI elements
        val darkModeSwitch = view.findViewById<Switch>(R.id.dark_mode_switch)
        val notificationsSwitch = view.findViewById<Switch>(R.id.notifications_switch)
        val currentLanguageText = view.findViewById<TextView>(R.id.current_language)
        val profileName = view.findViewById<TextView>(R.id.profile_name)
        val profileEmail = view.findViewById<TextView>(R.id.profile_email)
        val languageSettingLayout = view.findViewById<View>(R.id.layout_language_setting)
        val aboutLayout = view.findViewById<View>(R.id.layout_about)
        val logoutLayout = view.findViewById<View>(R.id.layout_logout)

        // Load user info
        val user = authService.getCurrentUser()
        user?.let {
            profileName.text = it.displayName ?: "BatiBook Reader"
            profileEmail.text = it.email ?: "No email"
        }

        // Load saved preferences
        val sharedPrefs = requireContext().getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
        
        // Set current language
        val currentLanguage = LanguageManager.getCurrentLanguage(requireContext())
        val languageDisplayName = LanguageManager.getLanguageDisplayName(currentLanguage)
        currentLanguageText?.text = languageDisplayName

        // Set dark mode switch state based on current theme
        val isDarkMode = ThemeManager.isDarkMode(requireContext())
        darkModeSwitch?.isChecked = isDarkMode

        // Set notifications switch state
        val notificationsEnabled = sharedPrefs.getBoolean("notifications", true)
        notificationsSwitch?.isChecked = notificationsEnabled

        // Dark mode switch listener
        darkModeSwitch?.setOnCheckedChangeListener { _, isChecked ->
            val theme = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.saveTheme(requireContext(), theme)
            ThemeManager.applyTheme(theme)
            
            // Restart activity to apply theme changes
            requireActivity().recreate()
        }

        // Notifications switch listener
        notificationsSwitch?.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit { putBoolean("notifications", isChecked) }
        }

        // Language setting click listener
        languageSettingLayout?.setOnClickListener {
            showLanguageSelectionDialog()
        }

        // About BatiBook click listener
        aboutLayout?.setOnClickListener {
            val intent = Intent(requireContext(), AboutActivity::class.java)
            startActivity(intent)
        }

        // Logout click listener
        logoutLayout?.setOnClickListener {
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
                authService.signOut()
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                // Refresh fragment to show login
                parentFragmentManager.beginTransaction().detach(this).attach(this).commit()
                // Update navigation visibility
                (activity as? MainActivity)?.updateBottomNavigationVisibility()
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