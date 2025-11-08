# Firestore Security Rules for BatiBook

## Important: Update Your Firestore Security Rules

To fix the "Permission_denied:Missing or insufficient permissions" error, you need to update your Firestore security rules in Firebase Console.

## Steps to Update Security Rules:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **"BatiBook"** project
3. Go to **Firestore Database** → **Rules** tab
4. Replace the existing rules with the following:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function to check if user owns the document
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    // Users collection - users can only read/write their own data
    match /users/{userId} {
      allow read, write: if isOwner(userId);
    }
    
    // Progress collection (reading progress) - users can only read/write their own progress
    match /progress/{userId} {
      allow read, write: if isOwner(userId);
    }
    
    // Reading progress collection - users can only read/write their own progress
    match /reading_progress/{documentId} {
      allow read: if isAuthenticated() && request.auth.uid == resource.data.userId;
      allow create: if isAuthenticated() && request.auth.uid == request.resource.data.userId;
      allow update: if isAuthenticated() && request.auth.uid == resource.data.userId && 
                       request.auth.uid == request.resource.data.userId;
      allow delete: if isAuthenticated() && request.auth.uid == resource.data.userId;
    }
    
    // Library collection - users can only read/write their own library
    match /library/{documentId} {
      allow read: if isAuthenticated() && request.auth.uid == resource.data.userId;
      allow create: if isAuthenticated() && request.auth.uid == request.resource.data.userId;
      allow update: if isAuthenticated() && request.auth.uid == resource.data.userId && 
                       request.auth.uid == request.resource.data.userId;
      allow delete: if isAuthenticated() && request.auth.uid == resource.data.userId;
    }
  }
}
```

## For Development (Less Secure - Use Only for Testing):

If you want to test quickly, you can use these less secure rules (NOT recommended for production):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow read/write access to authenticated users only
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Important Notes:

1. **Always use authenticated rules in production** - The second set of rules is only for quick testing
2. **Test your rules** - After updating rules, test them in Firebase Console → Firestore → Rules → Rules Playground
3. **Monitor access** - Check Firebase Console → Firestore → Usage to monitor access patterns
4. **Backup rules** - Save a copy of your rules before making changes

## Testing the Rules:

After updating the rules:
1. Sign in to your app
2. Try to add a book to favorites
3. Try to download a book
4. Check if the library loads without permission errors

If you still get permission errors:
1. Check Firebase Console → Authentication → Users to ensure you're authenticated
2. Check Firestore → Data to see if documents are being created with correct userId
3. Verify the document structure matches the rules (userId field must exist)

