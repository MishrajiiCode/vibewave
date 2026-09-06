<div align="center">
  <h1>?? VibeWave</h1>
  <p><strong>Premium Music Streaming App built with Flutter</strong></p>
  <p>
    <img src="https://img.shields.io/badge/Flutter-3.24-blue?logo=flutter" />
    <img src="https://img.shields.io/badge/Firebase-Firestore-orange?logo=firebase" />
    <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" />
    <img src="https://img.shields.io/badge/License-MIT-yellow" />
  </p>
</div>

---

## ? Features

| Feature | Description |
|---|---|
| ?? **YouTube Streaming** | Paste any YouTube URL to stream music audio |
| ?? **Firebase Backend** | Firestore for music library, Firebase Auth for admin |
| ?? **AI Recommendations** | Smart Firestore-based recommendations by mood & history |
| ?? **Notification Controls** | Full media controls in notification panel (like Spotify) |
| ?? **Glassmorphic UI** | Premium dark UI with blur effects and smooth animations |
| ?? **Modern Player** | Animated vinyl record, dynamic album art colors, lyrics |
| ??? **Categories** | Browse music by genre/category |
| ??? **Admin Panel** | Add/Edit/Delete songs & categories with Firebase sync |
| ?? **Mini Player** | Persistent bottom mini player across all screens |
| ?? **Library** | Like songs and build your personal library |

## ?? Screenshots

> Add your screenshots here after building the app

## ?? Disclaimer

> This app uses `youtube_explode_dart` to extract audio streams from YouTube URLs. This may violate YouTube's Terms of Service. This project is **for educational purposes only**. Use at your own risk. Always respect copyright laws and content creators.

---

## ?? Getting Started

### Prerequisites

- [Flutter SDK](https://flutter.dev/docs/get-started/install) (=3.3.0)
- [Android Studio](https://developer.android.com/studio) or [VS Code](https://code.visualstudio.com/)
- [Firebase Account](https://console.firebase.google.com/) (free tier is enough)
- [Git](https://git-scm.com/)

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/vibewave.git
cd vibewave
```

### 2. Set Up Firebase

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** ? Name it `vibewave` ? Continue
3. Enable **Google Analytics** (optional) ? Create project

#### Enable Firebase Services:
- **Firestore Database**: Build ? Firestore Database ? Create database ? Start in test mode
- **Authentication**: Build ? Authentication ? Sign-in method ? Enable **Anonymous** and **Email/Password**
- **Cloud Messaging**: Build ? Cloud Messaging (auto-enabled)

#### Download Config Files:
- Android: Project Settings ? Your apps ? Add Android app ? Package: `com.vibewave.music` ? Download `google-services.json` ? Place in `android/app/`

#### Install FlutterFire CLI & Configure:
```bash
dart pub global activate flutterfire_cli
flutterfire configure --project=YOUR_FIREBASE_PROJECT_ID
```
This auto-generates `lib/firebase_options.dart` with your real config.

### 3. Set Up Admin Account

In Firebase Console ? Authentication ? Users ? Add user:
- Email: `admin@vibewave.com` (or your preferred email)
- Password: your secure password

In `lib/presentation/providers/auth_provider.dart`, update the admin email:
```dart
const adminEmails = ['admin@vibewave.com']; // your admin email
```

### 4. Install Dependencies & Download Fonts

```bash
# Install Flutter packages
flutter pub get

# Download Poppins font files and place in assets/fonts/
# Get from: https://fonts.google.com/specimen/Poppins
# Required files:
# - Poppins-Regular.ttf
# - Poppins-Medium.ttf
# - Poppins-SemiBold.ttf
# - Poppins-Bold.ttf
```

### 5. Run the App

```bash
# Check connected devices
flutter devices

# Run in debug mode
flutter run

# Build release APK
flutter build apk --release
```

---

## ?? Project Structure

```
vibewave/
+-- lib/
¦   +-- main.dart                     # App entry point
¦   +-- firebase_options.dart          # Firebase config (generated)
¦   +-- core/
¦   ¦   +-- constants/                # Colors, strings
¦   ¦   +-- theme/                    # Dark glassmorphic theme
¦   ¦   +-- router/                   # GoRouter navigation
¦   +-- data/
¦   ¦   +-- models/                   # Song, Category, User models
¦   ¦   +-- services/                 # Firebase, YouTube, Audio, Notification
¦   +-- presentation/
¦       +-- screens/                  # All app screens
¦       +-- widgets/                  # Reusable widgets
¦       +-- providers/                # Riverpod state management
+-- android/                          # Android platform files
+-- .github/workflows/                # GitHub Actions CI/CD
```

---

## ?? How to Add Music (Admin)

1. Open the app ? Tap the **?? profile icon** in the top-right
2. Sign in with your admin email & password
3. In the Admin Dashboard, tap **+ Add Song**
4. Paste a YouTube URL ? Tap the **? magic wand** to auto-fetch title, artist & thumbnail
5. Select category, add tags (e.g., `happy, energetic`), toggle Featured if needed
6. Tap **Save** — the song appears instantly in the app!

---

## ?? AI Recommendations System

The built-in AI works without any external API:

1. **Listening History Analysis**: Tracks recently played songs per user
2. **Genre Frequency Counting**: Identifies your top genres
3. **Mood Filtering**: Tags songs with moods (`happy`, `sad`, `energetic`, etc.)
4. **Time Context**: Morning ? Fresh beats, Night ? Chill vibes
5. **Personalized Queue**: Filters already-heard songs for fresh recommendations

**To make AI recommendations better**: When adding songs in admin, use **tags** like `happy`, `chill`, `energetic`, `romantic`, `focus`, `party`, `workout`, `sad`

---

## ?? Firestore Security Rules

After development, update your Firestore rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Songs - readable by all, writable only by admins
    match /songs/{songId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.token.email in ['admin@vibewave.com'];
    }
    // Categories - readable by all
    match /categories/{catId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.token.email in ['admin@vibewave.com'];
    }
    // Users - only accessible by the user themselves
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    // Playlists
    match /playlists/{playlistId} {
      allow read: if resource.data.isPublic == true || (request.auth != null && request.auth.uid == resource.data.userId);
      allow write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
  }
}
```

---

## ?? Dependencies

| Package | Purpose |
|---|---|
| `firebase_core`, `cloud_firestore` | Firebase backend |
| `firebase_auth` | Admin authentication |
| `firebase_messaging` | Push notifications |
| `just_audio` | Audio playback engine |
| `just_audio_background` | Background playback & notification controls |
| `youtube_explode_dart` | YouTube audio stream extraction |
| `flutter_riverpod` | State management |
| `go_router` | Navigation |
| `cached_network_image` | Image caching |
| `flutter_animate` | Smooth animations |
| `palette_generator` | Dynamic color from album art |
| `shimmer` | Loading skeleton UI |

---

## ?? Contributing

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## ?? License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

---

<div align="center">
  <p>Made with ?? using Flutter & Firebase</p>
  <p>? Star this repo if you find it useful!</p>
</div>
