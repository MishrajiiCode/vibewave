// lib/firebase_options.dart
// Generated for streamhub-yt-app / VibeWave

import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, kIsWeb, TargetPlatform;

class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    if (kIsWeb) {
      return web;
    }
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return android;
      case TargetPlatform.iOS:
        return ios;
      default:
        throw UnsupportedError(
          'DefaultFirebaseOptions are not supported for this platform.',
        );
    }
  }

  static const FirebaseOptions android = FirebaseOptions(
    apiKey: 'AIzaSyBSyy8cIsYdMPjT-AdqxYtwAN7Qx97A_T0',
    appId: '1:721351757303:android:55068e358631d13b79b2f2',
    messagingSenderId: '721351757303',
    projectId: 'streamhub-yt-app',
    storageBucket: 'streamhub-yt-app.firebasestorage.app',
  );

  static const FirebaseOptions ios = FirebaseOptions(
    apiKey: 'AIzaSyDor-ltK0a-0lo80JmRsONjH3HQ4g79vO4',
    appId: '1:721351757303:ios:ec357d967a96ca4179b2f2',
    messagingSenderId: '721351757303',
    projectId: 'streamhub-yt-app',
    storageBucket: 'streamhub-yt-app.firebasestorage.app',
    iosBundleId: 'com.vibewave.music',
  );

  static const FirebaseOptions web = FirebaseOptions(
    apiKey: 'AIzaSyBKaPpC_crBWoPbPAakNO0zlqkZsRio8qs',
    appId: '1:721351757303:web:316fafed8ef11b8c79b2f2',
    messagingSenderId: '721351757303',
    projectId: 'streamhub-yt-app',
    authDomain: 'streamhub-yt-app.firebaseapp.com',
    storageBucket: 'streamhub-yt-app.firebasestorage.app',
  );
}
