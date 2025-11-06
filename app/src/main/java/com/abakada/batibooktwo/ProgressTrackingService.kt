package com.abakada.batibooktwo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.*

/**
 * Progress Tracking API Service using Firebase Firestore
 * 
 * This service tracks and syncs children's reading progress across devices.
 * It stores reading data in Firebase Firestore for cloud synchronization.
 * 
 * INTEGRATION PROCESS:
 * 1. Firebase Firestore is already configured (google-services.json exists)
 * 2. Firestore dependency is added in build.gradle.kts
 * 3. Enable Firestore in Firebase Console:
 *    - Go to https://console.firebase.google.com/
 *    - Select your project (batibook-project)
 *    - Go to Firestore Database → Create Database
 *    - Choose "Start in test mode" (for development)
 *    - Set location (e.g., us-central1)
 * 
 * HOW IT WORKS:
 * - Stores reading progress in Firestore database
 * - Syncs data across devices automatically
 * - Tracks multiple metrics: pages read, time spent, completion status
 * - Organizes data by user ID and story ID
 * - Provides real-time updates when data changes
 * 
 * DATA STRUCTURE:
 * Collection: "reading_progress"
 * Document ID: {userId}_{storyId}
 * Fields:
 *   - userId: String
 *   - storyId: String
 *   - storyTitle: String
 *   - currentPage: Int
 *   - totalPages: Int
 *   - progressPercentage: Double (0.0-100.0)
 *   - timeSpent: Long (milliseconds)
 *   - lastReadDate: Timestamp
 *   - completed: Boolean
 *   - createdAt: Timestamp
 *   - updatedAt: Timestamp
 * 
 * API KEY LOCATION:
 * - API keys are automatically managed by Firebase
 * - Configuration is in google-services.json
 * - Firestore rules control access (configure in Firebase Console)
 */
class ProgressTrackingService private constructor() {
    
    companion object {
        private const val TAG = "ProgressTrackingService"
        private const val COLLECTION_NAME = "reading_progress"
        
        @Volatile
        private var INSTANCE: ProgressTrackingService? = null
        
        fun getInstance(): ProgressTrackingService {
            return INSTANCE ?: synchronized(this) {
                val instance = ProgressTrackingService()
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Save or update reading progress
     * 
     * @param storyId Unique identifier for the story
     * @param storyTitle Title of the story
     * @param currentPage Current page number (0-indexed)
     * @param totalPages Total number of pages
     * @param timeSpent Time spent reading in milliseconds
     * @param callback Callback to handle result
     * 
     * Process:
     * 1. Gets current user ID
     * 2. Calculates progress percentage
     * 3. Creates/updates document in Firestore
     * 4. Sets completion status if at last page
     * 5. Updates timestamp
     */
    fun saveProgress(
        storyId: String,
        storyTitle: String,
        currentPage: Int,
        totalPages: Int,
        timeSpent: Long = 0,
        callback: ProgressCallback? = null
    ) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Log.w(TAG, "User not authenticated, cannot save progress")
            callback?.onError("User not authenticated")
            return
        }
        
        val userId = user.uid
        val documentId = "${userId}_${storyId}"
        
        // Calculate progress percentage
        val progressPercentage = if (totalPages > 0) {
            ((currentPage + 1).toDouble() / totalPages.toDouble()) * 100.0
        } else {
            0.0
        }
        
        // Check if completed
        val completed = currentPage >= totalPages - 1
        
        // Create progress data
        val progressData = hashMapOf(
            "userId" to userId,
            "storyId" to storyId,
            "storyTitle" to storyTitle,
            "currentPage" to currentPage,
            "totalPages" to totalPages,
            "progressPercentage" to progressPercentage,
            "timeSpent" to timeSpent,
            "lastReadDate" to com.google.firebase.Timestamp.now(),
            "completed" to completed,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        // Check if document exists to preserve createdAt
        firestore.collection(COLLECTION_NAME)
            .document(documentId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    // New document - add createdAt
                    progressData["createdAt"] = com.google.firebase.Timestamp.now()
                }
                
                // Save/update document
                firestore.collection(COLLECTION_NAME)
                    .document(documentId)
                    .set(progressData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "Progress saved: $documentId")
                        callback?.onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving progress", e)
                        callback?.onError(e.message ?: "Failed to save progress")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking document", e)
                // Try to save anyway
                progressData["createdAt"] = com.google.firebase.Timestamp.now()
                firestore.collection(COLLECTION_NAME)
                    .document(documentId)
                    .set(progressData, SetOptions.merge())
                    .addOnSuccessListener {
                        callback?.onSuccess()
                    }
                    .addOnFailureListener { error ->
                        callback?.onError(error.message ?: "Failed to save progress")
                    }
            }
    }
    
    /**
     * Get reading progress for a specific story
     * 
     * @param storyId Story identifier
     * @param callback Callback with progress data
     */
    fun getProgress(storyId: String, callback: ProgressFetchCallback) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback.onError("User not authenticated")
            return
        }
        
        val userId = user.uid
        val documentId = "${userId}_${storyId}"
        
        firestore.collection(COLLECTION_NAME)
            .document(documentId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val data = snapshot.data
                    val progress = ReadingProgress(
                        userId = data?.get("userId") as? String ?: "",
                        storyId = data?.get("storyId") as? String ?: "",
                        storyTitle = data?.get("storyTitle") as? String ?: "",
                        currentPage = (data?.get("currentPage") as? Number)?.toInt() ?: 0,
                        totalPages = (data?.get("totalPages") as? Number)?.toInt() ?: 0,
                        progressPercentage = (data?.get("progressPercentage") as? Number)?.toDouble() ?: 0.0,
                        timeSpent = (data?.get("timeSpent") as? Number)?.toLong() ?: 0,
                        completed = data?.get("completed") as? Boolean ?: false,
                        lastReadDate = data?.get("lastReadDate") as? com.google.firebase.Timestamp
                    )
                    callback.onSuccess(progress)
                } else {
                    callback.onSuccess(null) // No progress found
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching progress", e)
                callback.onError(e.message ?: "Failed to fetch progress")
            }
    }
    
    /**
     * Get all reading progress for current user
     * 
     * @param callback Callback with list of progress data
     */
    fun getAllProgress(callback: ProgressListCallback) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback.onError("User not authenticated")
            return
        }
        
        val userId = user.uid
        
        firestore.collection(COLLECTION_NAME)
            .whereEqualTo("userId", userId)
            .orderBy("lastReadDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val progressList = mutableListOf<ReadingProgress>()
                for (document in snapshot.documents) {
                    val data = document.data
                    if (data != null) {
                        val progress = ReadingProgress(
                            userId = data["userId"] as? String ?: "",
                            storyId = data["storyId"] as? String ?: "",
                            storyTitle = data["storyTitle"] as? String ?: "",
                            currentPage = (data["currentPage"] as? Number)?.toInt() ?: 0,
                            totalPages = (data["totalPages"] as? Number)?.toInt() ?: 0,
                            progressPercentage = (data["progressPercentage"] as? Number)?.toDouble() ?: 0.0,
                            timeSpent = (data["timeSpent"] as? Number)?.toLong() ?: 0,
                            completed = data["completed"] as? Boolean ?: false,
                            lastReadDate = data["lastReadDate"] as? com.google.firebase.Timestamp
                        )
                        progressList.add(progress)
                    }
                }
                callback.onSuccess(progressList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching all progress", e)
                callback.onError(e.message ?: "Failed to fetch progress")
            }
    }
    
    /**
     * Delete reading progress for a story
     * 
     * @param storyId Story identifier
     * @param callback Callback to handle result
     */
    fun deleteProgress(storyId: String, callback: ProgressCallback? = null) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback?.onError("User not authenticated")
            return
        }
        
        val userId = user.uid
        val documentId = "${userId}_${storyId}"
        
        firestore.collection(COLLECTION_NAME)
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Progress deleted: $documentId")
                callback?.onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting progress", e)
                callback?.onError(e.message ?: "Failed to delete progress")
            }
    }
    
    /**
     * Data model for reading progress
     */
    data class ReadingProgress(
        val userId: String,
        val storyId: String,
        val storyTitle: String,
        val currentPage: Int,
        val totalPages: Int,
        val progressPercentage: Double,
        val timeSpent: Long,
        val completed: Boolean,
        val lastReadDate: com.google.firebase.Timestamp?
    )
    
    /**
     * Callback interfaces
     */
    interface ProgressCallback {
        fun onSuccess()
        fun onError(error: String)
    }
    
    interface ProgressFetchCallback {
        fun onSuccess(progress: ReadingProgress?)
        fun onError(error: String)
    }
    
    interface ProgressListCallback {
        fun onSuccess(progressList: List<ReadingProgress>)
        fun onError(error: String)
    }
}


