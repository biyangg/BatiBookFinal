# API Integration Guide - BatiBook App

## Overview
This document explains all the APIs integrated in the BatiBook app, including setup instructions, how they work, and API key configuration.

---

## APIs Added

### 1. Firebase Authentication API ✅

**Purpose:** User authentication and account management

**Integration Status:** ✅ Fully Integrated

**Location:**
- Service: `app/src/main/java/com/abakada/batibooktwo/FirebaseAuthService.kt`
- Configuration: Already set up in `google-services.json`

**Setup Process:**
1. Firebase project is already configured (google-services.json exists)
2. Firebase Auth dependency is added in `build.gradle.kts`
3. Enable Authentication in Firebase Console:
   - Go to https://console.firebase.google.com/
   - Select your project (batibook-project)
   - Navigate to **Authentication** → **Get Started**
   - Enable **Email/Password** sign-in method
   - Enable **Google** sign-in if needed (optional)

**How It Works:**
- Firebase Auth manages user accounts securely in the cloud
- User credentials are stored securely by Firebase
- Provides real-time authentication state changes
- Supports email/password, Google Sign-In, and more
- Automatically handles token refresh and session management

**API Key Location:**
- API keys are automatically managed by Firebase
- Configuration is in `google-services.json`
- No manual API key setup needed

**Features:**
- User registration with email/password
- User login with email/password
- Password reset functionality
- User profile management
- Authentication state monitoring
- Secure token-based authentication

**Usage Example:**
```kotlin
val authService = FirebaseAuthService.getInstance()

// Register user
authService.registerUser(email, password, object : FirebaseAuthService.AuthCallback {
    override fun onSuccess(user: FirebaseUser?) {
        // User registered successfully
    }
    override fun onError(error: String) {
        // Handle error
    }
})

// Sign in user
authService.signInUser(email, password, object : FirebaseAuthService.AuthCallback {
    override fun onSuccess(user: FirebaseUser?) {
        // User signed in successfully
    }
    override fun onError(error: String) {
        // Handle error
    }
})
```

---

### 2. Android Text-to-Speech API ✅

**Purpose:** Automatically narrate story content for children

**Integration Status:** ✅ Fully Integrated

**Location:**
- Service: `app/src/main/java/com/abakada/batibooktwo/TextToSpeechService.kt`

**Setup Process:**
1. No API key required - uses Android's built-in TTS engine
2. TTS engine is available on all Android devices
3. Users may need to install TTS data if not pre-installed (Android handles this)
4. Supports multiple languages and voices

**How It Works:**
- Android TTS engine converts text strings into spoken audio
- Supports multiple languages (configured in LanguageManager)
- Can pause, resume, and stop narration
- Provides callbacks for speech events (start, done, error)
- Automatically handles voice selection based on language

**API Key Location:**
- **No API key needed** - uses Android system TTS engine

**Features:**
- Automatic story narration
- Multiple language support (English, Filipino)
- Play/pause/stop controls
- Speed and pitch adjustment
- Progress tracking during narration
- Error handling for missing TTS data

**Usage Example:**
```kotlin
val ttsService = TextToSpeechService.getInstance(context)

// Initialize TTS
ttsService.initialize { success ->
    if (success) {
        // TTS ready
        ttsService.speak("Once upon a time...")
    }
}

// Set callbacks
ttsService.onSpeechStart = { /* Speech started */ }
ttsService.onSpeechDone = { /* Speech completed */ }
ttsService.onSpeechError = { /* Error occurred */ }

// Adjust settings
ttsService.setLanguage("en") // or "fil" for Filipino
ttsService.setSpeechRate(1.0f) // 1.0 = normal, 0.5 = slow, 2.0 = fast
ttsService.setSpeechPitch(1.0f) // 1.0 = normal

// Clean up
ttsService.shutdown()
```

**Permissions:**
- No special permissions required
- TTS engine is part of Android system

---

### 3. Progress Tracking API (Firebase Firestore) ✅

**Purpose:** Store and sync children's reading progress across devices

**Integration Status:** ✅ Fully Integrated

**Location:**
- Service: `app/src/main/java/com/abakada/batibooktwo/ProgressTrackingService.kt`
- Database: Firebase Firestore (cloud)

**Setup Process:**
1. Firebase Firestore dependency is added in `build.gradle.kts`
2. Enable Firestore in Firebase Console:
   - Go to https://console.firebase.google.com/
   - Select your project (batibook-project)
   - Navigate to **Firestore Database** → **Create Database**
   - Choose **"Start in test mode"** (for development)
   - Set location (e.g., us-central1)
   - Click **Enable**

**How It Works:**
- Stores reading progress in Firestore database
- Syncs data across devices automatically
- Tracks multiple metrics: pages read, time spent, completion status
- Organizes data by user ID and story ID
- Provides real-time updates when data changes

**Data Structure:**
```
Collection: "reading_progress"
Document ID: {userId}_{storyId}
Fields:
  - userId: String
  - storyId: String
  - storyTitle: String
  - currentPage: Int
  - totalPages: Int
  - progressPercentage: Double (0.0-100.0)
  - timeSpent: Long (milliseconds)
  - lastReadDate: Timestamp
  - completed: Boolean
  - createdAt: Timestamp
  - updatedAt: Timestamp
```

**API Key Location:**
- API keys are automatically managed by Firebase
- Configuration is in `google-services.json`
- Firestore rules control access (configure in Firebase Console)

**Features:**
- Save reading progress
- Retrieve progress for specific story
- Get all progress for user
- Delete progress
- Automatic cloud synchronization
- Real-time updates

**Usage Example:**
```kotlin
val progressService = ProgressTrackingService.getInstance()

// Save progress
progressService.saveProgress(
    storyId = "story_001",
    storyTitle = "Dog and Cat",
    currentPage = 5,
    totalPages = 10,
    timeSpent = 300000, // 5 minutes in milliseconds
    object : ProgressTrackingService.ProgressCallback {
        override fun onSuccess() {
            // Progress saved successfully
        }
        override fun onError(error: String) {
            // Handle error
        }
    }
)

// Get progress
progressService.getProgress("story_001", 
    object : ProgressTrackingService.ProgressFetchCallback {
        override fun onSuccess(progress: ReadingProgress?) {
            progress?.let {
                // Use progress data
                val currentPage = it.currentPage
                val percentage = it.progressPercentage
            }
        }
        override fun onError(error: String) {
            // Handle error
        }
    }
)
```

**Firestore Security Rules (for production):**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /reading_progress/{document} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
    }
  }
}
```

---

### 4. Quiz Generation API ✅

**Purpose:** Generate simple comprehension quizzes based on story content

**Integration Status:** ✅ Fully Integrated

**Location:**
- Service: `app/src/main/java/com/abakada/batibooktwo/QuizGenerationService.kt`

**Setup Process:**

**Option 1 - OpenAI API (Recommended):**
1. Get OpenAI API key from https://platform.openai.com/api-keys
2. Create account or sign in
3. Navigate to API Keys section
4. Create new secret key
5. Copy the key and replace `OPENAI_API_KEY` constant in `QuizGenerationService.kt` (line 39)

**Option 2 - Simple Local Generation (Fallback):**
1. No API key needed
2. Generates basic quizzes using keyword extraction
3. Works offline automatically if OpenAI is not configured

**How It Works:**
- Takes story text as input
- Analyzes content to identify key concepts
- Generates multiple-choice questions
- Creates answer options with correct and incorrect answers
- Returns quiz questions in structured format

**API Key Location:**
- **OpenAI API Key:** `QuizGenerationService.kt` (line 39)
- Replace: `"YOUR_OPENAI_API_KEY_HERE"` with your actual key
- Get your key at: https://platform.openai.com/api-keys

**Features:**
- Multiple choice questions
- Automatic answer generation
- Difficulty level selection (easy, medium, hard)
- Question count customization
- Fallback to simple generation if API unavailable
- Works offline with fallback mode

**Usage Example:**
```kotlin
val quizService = QuizGenerationService.getInstance(context)

// Generate quiz with OpenAI (recommended)
quizService.generateQuizWithOpenAI(
    storyText = "Once upon a time, there was a dog and a cat...",
    questionCount = 5,
    difficulty = "medium",
    object : QuizGenerationService.QuizCallback {
        override fun onSuccess(questions: List<QuizQuestion>) {
            // Display quiz questions
            questions.forEach { question ->
                println("Q: ${question.question}")
                question.options.forEachIndexed { index, option ->
                    println("  ${index + 1}. $option")
                }
                println("Correct: ${question.correctAnswer + 1}")
            }
        }
        override fun onError(error: String) {
            // Handle error
        }
    }
)

// Generate simple quiz (fallback)
quizService.generateSimpleQuiz(
    storyText = "Story content...",
    questionCount = 5,
    object : QuizGenerationService.QuizCallback {
        override fun onSuccess(questions: List<QuizQuestion>) {
            // Use questions
        }
        override fun onError(error: String) {
            // Handle error
        }
    }
)
```

**OpenAI API Pricing:**
- Pay-as-you-go pricing
- GPT-3.5-turbo: ~$0.002 per 1K tokens
- Typical quiz generation: ~500-1000 tokens
- Cost per quiz: ~$0.001-0.002

---

## Dependencies Added

All dependencies are managed in `gradle/libs.versions.toml` and `app/build.gradle.kts`:

```kotlin
// Firebase
implementation(platform(libs.firebase.bom))
implementation(libs.google.firebase.analytics)
implementation(libs.google.firebase.auth)      // ✅ New
implementation(libs.google.firebase.firestore) // ✅ New

// HTTP & JSON
implementation(libs.volley)                    // For Quiz API
implementation(libs.gson)                      // JSON parsing

// UI
implementation(libs.androidx.recyclerview)     // For lists
```

---

## API Summary

| API | Status | Key Required | Location |
|-----|--------|--------------|----------|
| Firebase Authentication | ✅ Integrated | No (auto) | google-services.json |
| Text-to-Speech | ✅ Integrated | No | Android System |
| Progress Tracking | ✅ Integrated | No (auto) | google-services.json |
| Quiz Generation | ✅ Integrated | Optional (OpenAI) | QuizGenerationService.kt |

---

## Configuration Checklist

- [x] Firebase project configured (google-services.json exists)
- [ ] Enable Firebase Authentication in Console
- [ ] Enable Firebase Firestore in Console
- [ ] Configure Firestore security rules
- [ ] (Optional) Add OpenAI API key for quiz generation
- [ ] Test authentication flow
- [ ] Test TTS functionality
- [ ] Test progress tracking
- [ ] Test quiz generation

---

## Troubleshooting

### Firebase Authentication
- **Error: "User not authenticated"**
  - Ensure user is signed in before accessing protected features
  - Check Firebase Console: Authentication → Users

### Text-to-Speech
- **No speech output**
  - Check TTS is initialized: `ttsService.initialize { }`
  - Verify TTS data is installed on device
  - Check device volume is not muted

### Progress Tracking
- **Progress not saving**
  - Check user is authenticated
  - Verify Firestore is enabled in Firebase Console
  - Check Firestore security rules
  - Verify internet connection

### Quiz Generation
- **API errors**
  - Check OpenAI API key is set correctly
  - Verify internet connection
  - Check API key has sufficient credits
  - Fallback to simple generation if API unavailable

---

## Security Notes

1. **Firebase Configuration:**
   - `google-services.json` contains Firebase config
   - Keep this file secure and don't commit sensitive keys
   - Use environment variables for production

2. **OpenAI API Key:**
   - Store securely, don't hardcode in production
   - Consider using Android Keystore or backend proxy
   - Monitor API usage and costs

3. **Firestore Rules:**
   - Configure security rules in Firebase Console
   - Ensure users can only access their own data
   - Test rules before production deployment

---

## Support

- **Firebase:** https://firebase.google.com/docs
- **OpenAI:** https://platform.openai.com/docs
- **Android TTS:** https://developer.android.com/reference/android/speech/tts/TextToSpeech

---

## Files Created/Modified

### New Files:
- `FirebaseAuthService.kt` - Firebase Authentication service
- `TextToSpeechService.kt` - Android TTS service
- `ProgressTrackingService.kt` - Firestore progress tracking
- `QuizGenerationService.kt` - Quiz generation service

### Modified Files:
- `build.gradle.kts` - Added Firebase Auth and Firestore
- `libs.versions.toml` - Added dependency versions
- `SearchActivity.kt` - Simplified (removed Google Books)


