package com.abakada.batibooktwo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Library Service - Firebase Firestore Integration
 * 
 * This service manages user's library (favorites and downloaded books) using Firebase Firestore.
 * It stores book data in the cloud and syncs across devices.
 * 
 * INTEGRATION PROCESS:
 * 1. Firebase Firestore is already configured (google-services.json exists)
 * 2. Firestore dependency is already added in build.gradle.kts
 * 3. Ensure Firestore is enabled in Firebase Console
 * 
 * DATA STRUCTURE:
 * Collection: "user_library"
 * Document ID: {userId}_{bookId}
 * Fields:
 *   - userId: String
 *   - bookId: String
 *   - bookTitle: String
 *   - bookAuthor: String
 *   - bookImageRes: Int (as String)
 *   - bookTitleRes: Int (as String)
 *   - isFavorite: Boolean
 *   - isDownloaded: Boolean
 *   - addedDate: Timestamp
 *   - downloadDate: Timestamp (if downloaded)
 * 
 * HOW IT WORKS:
 * - Saves books to Firestore when user adds to favorites or downloads
 * - Retrieves user's library from Firestore
 * - Supports filtering by favorites or downloads
 * - Automatically syncs across devices
 */
class LibraryService private constructor() {
    
    companion object {
        private const val TAG = "LibraryService"
        private const val COLLECTION_NAME = "user_library"
        
        @Volatile
        private var INSTANCE: LibraryService? = null
        
        fun getInstance(): LibraryService {
            return INSTANCE ?: synchronized(this) {
                val instance = LibraryService()
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Data model for library book
     */
    data class LibraryBook(
        val userId: String,
        val bookId: String,
        val bookTitle: String,
        val bookAuthor: String,
        val bookImageRes: String, // Stored as String (resource name)
        val bookTitleRes: String, // Stored as String
        val isFavorite: Boolean,
        val isDownloaded: Boolean,
        val addedDate: com.google.firebase.Timestamp,
        val downloadDate: com.google.firebase.Timestamp?
    )
    
    /**
     * Add book to favorites
     * 
     * @param bookId Unique book identifier
     * @param bookTitle Book title
     * @param bookAuthor Book author
     * @param bookImageRes Image resource ID
     * @param bookTitleRes Title resource ID
     * @param callback Callback to handle result
     */
    fun addToFavorites(
        bookId: String,
        bookTitle: String,
        bookAuthor: String = "Unknown Author",
        bookImageRes: Int,
        bookTitleRes: Int,
        callback: LibraryCallback? = null
    ) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Log.w(TAG, "User not authenticated, cannot add to favorites")
            callback?.onError("User not authenticated. Please sign in first.")
            return
        }
        
        val userId = user.uid
        val documentId = "${userId}_${bookId}"
        
        val bookData = hashMapOf(
            "userId" to userId,
            "bookId" to bookId,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "bookImageRes" to bookImageRes.toString(),
            "bookTitleRes" to bookTitleRes.toString(),
            "isFavorite" to true,
            "isDownloaded" to false, // Keep existing downloaded status
            "addedDate" to com.google.firebase.Timestamp.now(),
            "downloadDate" to null
        )
        
        // Check if book already exists to preserve download status
        firestore.collection(COLLECTION_NAME)
            .document(documentId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                try {
                    if (documentSnapshot.exists() && documentSnapshot.data != null) {
                        // Update existing book - preserve download status
                        val existingData = documentSnapshot.data!!
                        bookData["isDownloaded"] = (existingData["isDownloaded"] as? Boolean) ?: false
                        bookData["downloadDate"] = existingData["downloadDate"] as? com.google.firebase.Timestamp
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading existing data", e)
                }
                
                // Save/update book
                firestore.collection(COLLECTION_NAME)
                    .document(documentId)
                    .set(bookData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "Book added to favorites: $documentId")
                        callback?.onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error adding book to favorites", e)
                        callback?.onError(e.message ?: "Failed to add to favorites")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking existing book, saving anyway", e)
                // If check fails, try to save anyway
                firestore.collection(COLLECTION_NAME)
                    .document(documentId)
                    .set(bookData, SetOptions.merge())
                    .addOnSuccessListener {
                        callback?.onSuccess()
                    }
                    .addOnFailureListener { error ->
                        callback?.onError(error.message ?: "Failed to add to favorites")
                    }
            }
    }
    
    /**
     * Download book (add to library)
     * 
     * @param bookId Unique book identifier
     * @param bookTitle Book title
     * @param bookAuthor Book author
     * @param bookImageRes Image resource ID
     * @param bookTitleRes Title resource ID
     * @param callback Callback to handle result
     */
    fun downloadBook(
        bookId: String,
        bookTitle: String,
        bookAuthor: String = "Unknown Author",
        bookImageRes: Int,
        bookTitleRes: Int,
        callback: LibraryCallback? = null
    ) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Log.w(TAG, "User not authenticated, cannot download book")
            callback?.onError("User not authenticated. Please sign in first.")
            return
        }
        
        val userId = user.uid
        val documentId = "${userId}_${bookId}"
        
        val bookData = hashMapOf(
            "userId" to userId,
            "bookId" to bookId,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "bookImageRes" to bookImageRes.toString(),
            "bookTitleRes" to bookTitleRes.toString(),
            "isDownloaded" to true,
            "isFavorite" to false, // Keep existing favorite status
            "downloadDate" to com.google.firebase.Timestamp.now(),
            "addedDate" to com.google.firebase.Timestamp.now()
        )
        
        // Check if book already exists to preserve favorite status
        firestore.collection(COLLECTION_NAME)
            .document(documentId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                try {
                    if (documentSnapshot.exists() && documentSnapshot.data != null) {
                        // Update existing book - preserve favorite status
                        val existingData = documentSnapshot.data!!
                        bookData["isFavorite"] = (existingData["isFavorite"] as? Boolean) ?: false
                        bookData["addedDate"] = (existingData["addedDate"] as? com.google.firebase.Timestamp)
                            ?: com.google.firebase.Timestamp.now()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading existing data", e)
                }
                
                // Save/update book
                firestore.collection(COLLECTION_NAME)
                    .document(documentId)
                    .set(bookData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "Book downloaded: $documentId")
                        callback?.onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error downloading book", e)
                        callback?.onError(e.message ?: "Failed to download book")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking existing book, saving anyway", e)
                // If check fails, try to save anyway
                firestore.collection(COLLECTION_NAME)
                    .document(documentId)
                    .set(bookData, SetOptions.merge())
                    .addOnSuccessListener {
                        callback?.onSuccess()
                    }
                    .addOnFailureListener { error ->
                        callback?.onError(error.message ?: "Failed to download book")
                    }
            }
    }
    
    /**
     * Get all library books for current user
     * 
     * @param filter Filter type: "all", "favorites", "downloads"
     * @param callback Callback with list of books
     */
    fun getLibraryBooks(
        filter: String = "all",
        callback: LibraryListCallback
    ) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback.onError("User not authenticated")
            return
        }
        
        val userId = user.uid
        
        // Build query based on filter
        val query = when (filter) {
            "favorites" -> firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isFavorite", true)
            
            "downloads" -> firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDownloaded", true)
            
            else -> firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
        }
        
        // For "all" filter, we'll get all books and filter in memory to avoid index requirement
        if (filter == "all") {
            firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    try {
                        val books = mutableListOf<LibraryBook>()
                        for (document in querySnapshot.documents) {
                            val data = document.data
                            if (data != null) {
                                val book = LibraryBook(
                                    userId = (data["userId"] as? String) ?: "",
                                    bookId = (data["bookId"] as? String) ?: "",
                                    bookTitle = (data["bookTitle"] as? String) ?: "",
                                    bookAuthor = (data["bookAuthor"] as? String) ?: "Unknown Author",
                                    bookImageRes = (data["bookImageRes"] as? String) ?: "0",
                                    bookTitleRes = (data["bookTitleRes"] as? String) ?: "0",
                                    isFavorite = (data["isFavorite"] as? Boolean) ?: false,
                                    isDownloaded = (data["isDownloaded"] as? Boolean) ?: false,
                                    addedDate = (data["addedDate"] as? com.google.firebase.Timestamp)
                                        ?: com.google.firebase.Timestamp.now(),
                                    downloadDate = data["downloadDate"] as? com.google.firebase.Timestamp
                                )
                                books.add(book)
                            }
                        }
                        // Sort by addedDate descending
                        books.sortByDescending { it.addedDate.seconds }
                        callback.onSuccess(books)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing library books", e)
                        callback.onError("Error parsing library data: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching library books", e)
                    callback.onError(e.message ?: "Failed to fetch library books")
                }
        } else {
            // For filtered queries, fetch all books and filter in memory to avoid index requirement
            firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    try {
                        val books = mutableListOf<LibraryBook>()
                        for (document in querySnapshot.documents) {
                            val data = document.data
                            if (data != null) {
                                val isFavorite = (data["isFavorite"] as? Boolean) ?: false
                                val isDownloaded = (data["isDownloaded"] as? Boolean) ?: false
                                
                                // Apply filter in memory
                                val shouldInclude = when (filter) {
                                    "favorites" -> isFavorite
                                    "downloads" -> isDownloaded
                                    else -> true
                                }
                                
                                if (shouldInclude) {
                                    val book = LibraryBook(
                                        userId = (data["userId"] as? String) ?: "",
                                        bookId = (data["bookId"] as? String) ?: "",
                                        bookTitle = (data["bookTitle"] as? String) ?: "",
                                        bookAuthor = (data["bookAuthor"] as? String) ?: "Unknown Author",
                                        bookImageRes = (data["bookImageRes"] as? String) ?: "0",
                                        bookTitleRes = (data["bookTitleRes"] as? String) ?: "0",
                                        isFavorite = isFavorite,
                                        isDownloaded = isDownloaded,
                                        addedDate = (data["addedDate"] as? com.google.firebase.Timestamp)
                                            ?: com.google.firebase.Timestamp.now(),
                                        downloadDate = data["downloadDate"] as? com.google.firebase.Timestamp
                                    )
                                    books.add(book)
                                }
                            }
                        }
                        // Sort by addedDate descending
                        books.sortByDescending { it.addedDate.seconds }
                        callback.onSuccess(books)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing library books", e)
                        callback.onError("Error parsing library data: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching library books", e)
                    callback.onError(e.message ?: "Failed to fetch library books")
                }
        }
    }
    
    /**
     * Remove book from library
     * 
     * @param bookId Book identifier
     * @param callback Callback to handle result
     */
    fun removeFromLibrary(bookId: String, callback: LibraryCallback? = null) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback?.onError("User not authenticated")
            return
        }
        
        val userId = user.uid
        val documentId = "${userId}_${bookId}"
        
        firestore.collection(COLLECTION_NAME)
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Book removed from library: $documentId")
                callback?.onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error removing book from library", e)
                callback?.onError(e.message ?: "Failed to remove book")
            }
    }
    
    /**
     * Remove from favorites (but keep if downloaded)
     * 
     * @param bookId Book identifier
     * @param callback Callback to handle result
     */
    fun removeFromFavorites(bookId: String, callback: LibraryCallback? = null) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback?.onError("User not authenticated")
            return
        }
        
        val userId = user.uid
        val documentId = "${userId}_${bookId}"
        
        firestore.collection(COLLECTION_NAME)
            .document(documentId)
            .update("isFavorite", false)
            .addOnSuccessListener {
                Log.d(TAG, "Book removed from favorites: $documentId")
                callback?.onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error removing from favorites", e)
                callback?.onError(e.message ?: "Failed to remove from favorites")
            }
    }
    
    /**
     * Check if book is in library
     * 
     * @param bookId Book identifier
     * @param callback Callback with book status
     */
    fun checkBookStatus(bookId: String, callback: BookStatusCallback) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            callback.onSuccess(false, false)
            return
        }
        
        val userId = user.uid
        val documentId = "${userId}_${bookId}"
        
        firestore.collection(COLLECTION_NAME)
            .document(documentId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                try {
                    if (documentSnapshot.exists() && documentSnapshot.data != null) {
                        val data = documentSnapshot.data!!
                        val isFavorite = (data["isFavorite"] as? Boolean) ?: false
                        val isDownloaded = (data["isDownloaded"] as? Boolean) ?: false
                        callback.onSuccess(isFavorite, isDownloaded)
                    } else {
                        callback.onSuccess(false, false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking book status", e)
                    callback.onSuccess(false, false)
                }
            }
            .addOnFailureListener {
                callback.onSuccess(false, false)
            }
    }
    
    /**
     * Callback interfaces
     */
    interface LibraryCallback {
        fun onSuccess()
        fun onError(error: String)
    }
    
    interface LibraryListCallback {
        fun onSuccess(books: List<LibraryBook>)
        fun onError(error: String)
    }
    
    interface BookStatusCallback {
        fun onSuccess(isFavorite: Boolean, isDownloaded: Boolean)
    }
}

