package com.abakada.batibooktwo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * User Service - Firebase Firestore Integration for User Data
 * 
 * This service manages user profile data in Firestore.
 * Stores user information when they sign up and retrieves it when needed.
 * 
 * DATA STRUCTURE:
 * Collection: "users"
 * Document ID: {userId} (Firebase Auth UID)
 * Fields:
 *   - userId: String
 *   - email: String
 *   - displayName: String
 *   - userType: String ("user" or "admin")
 *   - createdAt: Timestamp
 *   - lastLogin: Timestamp
 *   - profileImageUrl: String (optional)
 * 
 * HOW IT WORKS:
 * - Saves user data to Firestore on signup
 * - Updates last login timestamp on sign in
 * - Retrieves user profile data
 * - Supports admin and regular user types
 */
class UserService private constructor() {
    
    companion object {
        private const val TAG = "UserService"
        private const val COLLECTION_NAME = "users"
        
        @Volatile
        private var INSTANCE: UserService? = null
        
        fun getInstance(): UserService {
            return INSTANCE ?: synchronized(this) {
                val instance = UserService()
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Data model for user profile
     */
    data class UserProfile(
        val userId: String,
        val email: String,
        val displayName: String,
        val userType: String, // "user" or "admin"
        val createdAt: com.google.firebase.Timestamp,
        val lastLogin: com.google.firebase.Timestamp,
        val profileImageUrl: String? = null
    )
    
    /**
     * Create or update user profile in Firestore
     * 
     * @param userId User ID from Firebase Auth
     * @param email User email
     * @param displayName User display name
     * @param userType User type ("user" or "admin")
     * @param callback Callback to handle result
     */
    fun createUserProfile(
        userId: String,
        email: String,
        displayName: String,
        userType: String = "user",
        callback: UserCallback? = null
    ) {
        val userData = hashMapOf(
            "userId" to userId,
            "email" to email,
            "displayName" to displayName,
            "userType" to userType,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "lastLogin" to com.google.firebase.Timestamp.now()
        )
        
        firestore.collection(COLLECTION_NAME)
            .document(userId)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "User profile created/updated: $userId")
                callback?.onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating user profile", e)
                callback?.onError(e.message ?: "Failed to create user profile")
            }
    }
    
    /**
     * Update last login timestamp
     * 
     * @param userId User ID
     */
    fun updateLastLogin(userId: String) {
        firestore.collection(COLLECTION_NAME)
            .document(userId)
            .update("lastLogin", com.google.firebase.Timestamp.now())
            .addOnSuccessListener {
                Log.d(TAG, "Last login updated: $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating last login", e)
            }
    }
    
    /**
     * Get user profile from Firestore
     * 
     * @param userId User ID
     * @param callback Callback with user profile
     */
    fun getUserProfile(userId: String, callback: UserProfileCallback) {
        firestore.collection(COLLECTION_NAME)
            .document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                try {
                    if (documentSnapshot.exists() && documentSnapshot.data != null) {
                        val data = documentSnapshot.data!!
                        val profile = UserProfile(
                            userId = (data["userId"] as? String) ?: userId,
                            email = (data["email"] as? String) ?: "",
                            displayName = (data["displayName"] as? String) ?: "",
                            userType = (data["userType"] as? String) ?: "user",
                            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)
                                ?: com.google.firebase.Timestamp.now(),
                            lastLogin = (data["lastLogin"] as? com.google.firebase.Timestamp)
                                ?: com.google.firebase.Timestamp.now(),
                            profileImageUrl = data["profileImageUrl"] as? String
                        )
                        callback.onSuccess(profile)
                    } else {
                        callback.onError("User profile not found")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing user profile", e)
                    callback.onError("Error parsing user data: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user profile", e)
                callback.onError(e.message ?: "Failed to fetch user profile")
            }
    }
    
    /**
     * Update user display name
     * 
     * @param userId User ID
     * @param displayName New display name
     * @param callback Callback to handle result
     */
    fun updateDisplayName(userId: String, displayName: String, callback: UserCallback? = null) {
        firestore.collection(COLLECTION_NAME)
            .document(userId)
            .update("displayName", displayName)
            .addOnSuccessListener {
                Log.d(TAG, "Display name updated: $userId")
                callback?.onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating display name", e)
                callback?.onError(e.message ?: "Failed to update display name")
            }
    }
    
    /**
     * Get current user profile
     * 
     * @param callback Callback with user profile
     */
    fun getCurrentUserProfile(callback: UserProfileCallback) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback.onError("User not authenticated")
            return
        }
        
        getUserProfile(user.uid, callback)
    }
    
    /**
     * Callback interfaces
     */
    interface UserCallback {
        fun onSuccess()
        fun onError(error: String)
    }
    
    interface UserProfileCallback {
        fun onSuccess(profile: UserProfile)
        fun onError(error: String)
    }
}

