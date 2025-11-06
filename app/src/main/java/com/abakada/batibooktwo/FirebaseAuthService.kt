package com.abakada.batibooktwo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Firebase Authentication API Service
 * 
 * This service handles user authentication using Firebase Authentication.
 * Firebase Auth provides secure user authentication with email/password, 
 * Google Sign-In, and other authentication methods.
 * 
 * INTEGRATION PROCESS:
 * 1. Firebase project is already set up (google-services.json exists)
 * 2. Firebase Auth dependency is added in build.gradle.kts
 * 3. Enable Authentication in Firebase Console:
 *    - Go to https://console.firebase.google.com/
 *    - Select your project (batibook-project)
 *    - Go to Authentication → Get Started
 *    - Enable "Email/Password" sign-in method
 *    - Enable "Google" sign-in if needed (optional)
 * 
 * HOW IT WORKS:
 * - Firebase Auth manages user accounts securely in the cloud
 * - User credentials are stored securely by Firebase
 * - Provides real-time authentication state changes
 * - Supports email/password, Google Sign-In, and more
 * - Automatically handles token refresh and session management
 * 
 * API KEY LOCATION:
 * - API keys are automatically managed by Firebase
 * - Configuration is in google-services.json
 * - No manual API key setup needed
 * 
 * FEATURES:
 * - User registration with email/password
 * - User login with email/password
 * - Password reset functionality
 * - User profile management
 * - Authentication state monitoring
 * - Secure token-based authentication
 */
class FirebaseAuthService private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: FirebaseAuthService? = null
        
        fun getInstance(): FirebaseAuthService {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseAuthService()
                INSTANCE = instance
                instance
            }
        }
    }
    
    // Get Firebase Auth instance
    // Firebase automatically initializes from google-services.json
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Get current authenticated user
     * @return FirebaseUser if logged in, null otherwise
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
    
    /**
     * Check if user is currently authenticated
     * @return true if user is logged in, false otherwise
     */
    fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }
    
    /**
     * Register a new user with email and password
     * 
     * @param email User's email address
     * @param password User's password (must be at least 6 characters)
     * @param callback Callback to handle registration result
     * 
     * Process:
     * 1. Firebase validates email format
     * 2. Validates password strength (min 6 chars)
     * 3. Creates new user account in Firebase
     * 4. Sends verification email (optional)
     * 5. Returns user ID on success
     */
    fun registerUser(
        email: String,
        password: String,
        callback: AuthCallback
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    callback.onSuccess(user)
                } else {
                    val error = task.exception?.message ?: "Registration failed"
                    callback.onError(error)
                }
            }
    }
    
    /**
     * Sign in existing user with email and password
     * 
     * @param email User's email address
     * @param password User's password
     * @param callback Callback to handle sign-in result
     * 
     * Process:
     * 1. Firebase validates credentials
     * 2. Authenticates user with Firebase servers
     * 3. Returns user object on success
     * 4. Handles invalid credentials or network errors
     */
    fun signInUser(
        email: String,
        password: String,
        callback: AuthCallback
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    callback.onSuccess(user)
                } else {
                    val error = task.exception?.message ?: "Sign in failed"
                    callback.onError(error)
                }
            }
    }
    
    /**
     * Sign out current user
     * 
     * Process:
     * 1. Clears local authentication session
     * 2. Invalidates authentication tokens
     * 3. User must sign in again to access protected features
     */
    fun signOut() {
        firebaseAuth.signOut()
    }
    
    /**
     * Send password reset email
     * 
     * @param email User's email address
     * @param callback Callback to handle result
     * 
     * Process:
     * 1. Firebase sends password reset email
     * 2. User clicks link in email
     * 3. User can set new password
     */
    fun resetPassword(email: String, callback: AuthCallback) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess(null)
                } else {
                    val error = task.exception?.message ?: "Password reset failed"
                    callback.onError(error)
                }
            }
    }
    
    /**
     * Update user display name
     * 
     * @param displayName New display name
     * @param callback Callback to handle result
     */
    fun updateDisplayName(displayName: String, callback: AuthCallback) {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            
            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback.onSuccess(firebaseAuth.currentUser)
                    } else {
                        val error = task.exception?.message ?: "Failed to update profile"
                        callback.onError(error)
                    }
                }
        } else {
            callback.onError("No user signed in")
        }
    }
    
    /**
     * Delete current user account
     * 
     * @param callback Callback to handle result
     * 
     * WARNING: This permanently deletes the user account
     */
    fun deleteAccount(callback: AuthCallback) {
        val user = firebaseAuth.currentUser
        if (user != null) {
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback.onSuccess(null)
                    } else {
                        val error = task.exception?.message ?: "Failed to delete account"
                        callback.onError(error)
                    }
                }
        } else {
            callback.onError("No user signed in")
        }
    }
    
    /**
     * Add authentication state listener
     * 
     * @param listener Listener to be notified of auth state changes
     * @return FirebaseAuth.AuthStateListener that can be removed later
     * 
     * Use this to monitor authentication state in real-time
     */
    fun addAuthStateListener(listener: (FirebaseUser?) -> Unit): FirebaseAuth.AuthStateListener {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            listener(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        return authStateListener
    }
    
    /**
     * Remove authentication state listener
     */
    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        firebaseAuth.removeAuthStateListener(listener)
    }
    
    /**
     * Callback interface for authentication operations
     */
    interface AuthCallback {
        fun onSuccess(user: FirebaseUser?)
        fun onError(error: String)
    }
}


