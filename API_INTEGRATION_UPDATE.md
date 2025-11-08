# Firebase Firestore API Integration Update

## New APIs Added

### 1. Library Service (Firestore) ✅

**Status:** ✅ Fully Integrated

**Purpose:** Manage user's library (favorites and downloaded books) using Firebase Firestore

**Location:**
- Service: `app/src/main/java/com/abakada/batibooktwo/LibraryService.kt`

**Integration:**
- No API key needed - automatically configured via Firebase
- Ensure Firestore is enabled in Firebase Console

**Features:**
- Add books to favorites
- Download books to library
- Get all library books
- Filter by favorites or downloads
- Remove from favorites
- Check book status
- Automatic cloud synchronization

**Data Structure:**
```
Collection: "user_library"
Document ID: {userId}_{bookId}
Fields:
  - userId: String
  - bookId: String
  - bookTitle: String
  - bookAuthor: String
  - bookImageRes: String (resource ID as string)
  - bookTitleRes: String (resource ID as string)
  - isFavorite: Boolean
  - isDownloaded: Boolean
  - addedDate: Timestamp
  - downloadDate: Timestamp
```

**Usage:**
```kotlin
val libraryService = LibraryService.getInstance()

// Add to favorites
libraryService.addToFavorites(
    bookId = "book_123",
    bookTitle = "Dog and Cat",
    bookAuthor = "Unknown Author",
    bookImageRes = R.drawable.book1,
    bookTitleRes = R.string.dog_and_cat
)

// Download book
libraryService.downloadBook(
    bookId = "book_123",
    bookTitle = "Dog and Cat",
    bookAuthor = "Unknown Author",
    bookImageRes = R.drawable.book1,
    bookTitleRes = R.string.dog_and_cat
)

// Get library books
libraryService.getLibraryBooks("all") { books ->
    // Display books
}
```

---

### 2. User Service (Firestore) ✅

**Status:** ✅ Fully Integrated

**Purpose:** Store user profile data in Firestore

**Location:**
- Service: `app/src/main/java/com/abakada/batibooktwo/UserService.kt`

**Integration:**
- No API key needed - automatically configured via Firebase
- Automatically saves user data on signup

**Features:**
- Create user profile on signup
- Update last login timestamp
- Get user profile
- Update display name
- Store user type (user/admin)

**Data Structure:**
```
Collection: "users"
Document ID: {userId} (Firebase Auth UID)
Fields:
  - userId: String
  - email: String
  - displayName: String
  - userType: String ("user" or "admin")
  - createdAt: Timestamp
  - lastLogin: Timestamp
  - profileImageUrl: String (optional)
```

**Usage:**
```kotlin
val userService = UserService.getInstance()

// Create user profile (automatically called on signup)
userService.createUserProfile(
    userId = user.uid,
    email = "user@example.com",
    displayName = "John Doe",
    userType = "user"
)

// Update last login (automatically called on sign in)
userService.updateLastLogin(user.uid)

// Get user profile
userService.getCurrentUserProfile { profile ->
    // Use profile data
}
```

---

## Implementation Details

### Home Screen Integration
- **Add to Favorites Button**: Saves book to Firestore when clicked
- **Download Button**: Saves book to Firestore with download status
- **Authentication Check**: Prompts user to sign in if not authenticated

### Library Fragment Integration
- **Loads books from Firestore**: Displays user's library from cloud
- **Filter by favorites**: Shows only favorite books
- **Filter by downloads**: Shows only downloaded books
- **Filter by all**: Shows all books in library
- **Auto-refresh**: Refreshes when fragment resumes
- **Authentication Check**: Shows sign-in message if not authenticated

### Login/Signup Integration
- **User Profile Creation**: Automatically creates user profile in Firestore on signup
- **User Type Storage**: Saves user type (user/admin) to Firestore
- **Last Login Update**: Updates last login timestamp on sign in

---

## Firestore Security Rules

For production, configure Firestore security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User library rules
    match /user_library/{document} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
    }
    
    // User profile rules
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Reading progress rules
    match /reading_progress/{document} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
    }
  }
}
```

---

## Setup Checklist

- [x] Firebase Firestore enabled in Console
- [x] LibraryService created and integrated
- [x] UserService created and integrated
- [x] Home screen favorites/download integrated
- [x] Library fragment loads from Firestore
- [x] Login/signup saves user data to Firestore
- [ ] Configure Firestore security rules (for production)
- [ ] Test favorites functionality
- [ ] Test download functionality
- [ ] Test library loading

---

## Files Created/Modified

### New Files:
- `LibraryService.kt` - Firestore library management
- `UserService.kt` - Firestore user profile management

### Modified Files:
- `MainActivity.kt` - Integrated LibraryService for favorites/downloads
- `Library.kt` - Loads books from Firestore
- `Profile.kt` - Saves user data to Firestore on signup

---

## How It Works

### Adding to Favorites:
1. User clicks "Add to Favorites" on a book
2. LibraryService saves book to Firestore with `isFavorite: true`
3. Book appears in Library fragment under "Favorites" tab
4. Data syncs across devices automatically

### Downloading Books:
1. User clicks "Download" on a book
2. LibraryService saves book to Firestore with `isDownloaded: true`
3. Book appears in Library fragment under "Downloads" tab
4. Data syncs across devices automatically

### Library Display:
1. Library fragment loads books from Firestore
2. Filters books based on selected tab (All/Favorites/Downloads)
3. Displays books in grid layout
4. Refreshes automatically when fragment resumes

### User Profile:
1. User signs up → UserService creates profile in Firestore
2. User signs in → UserService updates last login timestamp
3. Profile data is stored securely in Firestore
4. User type (user/admin) is saved for future use

---

## Notes

- All Firestore operations require user authentication
- Books are organized by user ID to ensure privacy
- Library syncs automatically across devices
- Progress tracking and library are separate collections
- User profiles are stored separately from library data

