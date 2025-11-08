# Firebase Setup Guide for BatiBook

## Current Status
Your Firebase project is named **"BatiBook"**, but the `google-services.json` file references **"batibook-project"**.

## Important: Update google-services.json

Since you created a new Firebase project named "BatiBook", you need to download the correct `google-services.json` file:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **"BatiBook"** project
3. Click on the gear icon ⚙️ next to "Project Overview"
4. Select **"Project settings"**
5. Scroll down to **"Your apps"** section
6. If you don't see an Android app, click **"Add app"** → Select Android
7. Enter package name: `com.abakada.batibooktwo`
8. Click **"Register app"**
9. Download the `google-services.json` file
10. Replace the existing `google-services.json` in `app/` directory with the new one

## Firebase Services Setup

### 1. Authentication (Email/Password)
✅ Already enabled (as per your instructions)

**Steps you completed:**
- Enabled Email/Password authentication in Firebase Console
- Authentication → Sign-in method → Email/Password → Enabled

### 2. Firestore Database
✅ Already created (as per your instructions)

**Steps you completed:**
- Created Firestore database
- Set to default location
- Started in test mode (for development)

**Important:** For production, update Firestore security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - users can only read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Progress collection - users can only read/write their own progress
    match /progress/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Reading progress collection - users can only read/write their own progress
    match /reading_progress/{document} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    // Library collection - users can only read/write their own library
    match /library/{document} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
  }
}
```

## Testing the Setup

1. **Test Authentication:**
   - Open the app
   - Go to Profile tab
   - Try to sign up with a new email
   - Check Firebase Console → Authentication → Users to see if user was created

2. **Test Firestore:**
   - After signing up, check Firestore Console → `users` collection
   - You should see a document with the user's UID containing user profile data

3. **Test Progress Tracking:**
   - Read a story
   - Check Firestore Console → `reading_progress` collection
   - You should see progress data being saved

## Troubleshooting

### Login not working?
- Check if `google-services.json` is correctly placed in `app/` directory
- Verify package name matches: `com.abakada.batibooktwo`
- Check Firebase Console → Authentication → Users to see if users are being created
- Check Logcat for Firebase error messages

### Firestore errors?
- Verify Firestore is enabled in Firebase Console
- Check Firestore security rules (should allow authenticated users)
- Check Logcat for specific Firestore error messages
- Ensure user is authenticated before trying to save data

### Text-to-Speech not working?
- TTS uses Android's built-in engine (no Firebase needed)
- Check if TTS data is installed on the device
- Some devices may need to download TTS language packs
- Check Logcat for TTS initialization errors

## Next Steps

1. ✅ Download correct `google-services.json` for "BatiBook" project
2. ✅ Replace the file in `app/` directory
3. ✅ Rebuild the app
4. ✅ Test authentication (sign up/login)
5. ✅ Test story reading and progress tracking
6. ✅ Test quiz generation
7. ⚠️ Update Firestore security rules for production (when ready)

## Support

If you encounter any issues:
1. Check Logcat for error messages
2. Verify Firebase Console settings
3. Ensure `google-services.json` is correct
4. Make sure all Firebase services are enabled in Console

