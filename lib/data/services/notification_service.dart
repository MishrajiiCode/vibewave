// lib/data/services/notification_service.dart
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:permission_handler/permission_handler.dart';

class NotificationService {
  static final FlutterLocalNotificationsPlugin _local =
      FlutterLocalNotificationsPlugin();
  static final FirebaseMessaging _fcm = FirebaseMessaging.instance;

  static Future<void> initialize() async {
    // Android initialization
    const android = AndroidInitializationSettings('@mipmap/ic_launcher');
    const settings = InitializationSettings(android: android);
    await _local.initialize(
      settings,
      onDidReceiveNotificationResponse: _onNotificationTapped,
    );

    // Create notification channel
    const channel = AndroidNotificationChannel(
      'vibewave_updates',
      'VibeWave Updates',
      description: 'Notifications for new music and app updates',
      importance: Importance.high,
    );
    await _local
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(channel);

    // FCM background handler
    FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);

    // FCM foreground handler
    FirebaseMessaging.onMessage.listen((message) {
      _showLocalNotification(message);
    });
  }

  static Future<bool> requestPermission() async {
    final settings = await _fcm.requestPermission(
      alert: true,
      badge: true,
      sound: true,
      provisional: false,
    );

    final status = await Permission.notification.request();
    return status.isGranted &&
        settings.authorizationStatus == AuthorizationStatus.authorized;
  }

  static Future<String?> getFcmToken() async {
    return await _fcm.getToken();
  }

  static void _showLocalNotification(RemoteMessage message) {
    final notification = message.notification;
    if (notification == null) return;

    _local.show(
      notification.hashCode,
      notification.title,
      notification.body,
      NotificationDetails(
        android: AndroidNotificationDetails(
          'vibewave_updates',
          'VibeWave Updates',
          importance: Importance.high,
          priority: Priority.high,
          styleInformation: BigTextStyleInformation(notification.body ?? ''),
        ),
      ),
    );
  }

  static void _onNotificationTapped(NotificationResponse response) {
    // Handle notification tap - navigate accordingly
    print('Notification tapped: ${response.payload}');
  }
}

@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  print('Background FCM message: ${message.messageId}');
}
