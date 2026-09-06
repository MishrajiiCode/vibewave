// lib/data/models/user_model.dart
import 'package:cloud_firestore/cloud_firestore.dart';

class UserModel {
  final String id;
  final String email;
  final String displayName;
  final List<String> likedSongs;
  final List<String> recentlyPlayed;
  final List<String> favoriteGenres;
  final String fcmToken;
  final DateTime createdAt;

  const UserModel({
    required this.id,
    required this.email,
    this.displayName = '',
    this.likedSongs = const [],
    this.recentlyPlayed = const [],
    this.favoriteGenres = const [],
    this.fcmToken = '',
    required this.createdAt,
  });

  factory UserModel.fromFirestore(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>;
    return UserModel(
      id: doc.id,
      email: data['email'] ?? '',
      displayName: data['displayName'] ?? '',
      likedSongs: List<String>.from(data['likedSongs'] ?? []),
      recentlyPlayed: List<String>.from(data['recentlyPlayed'] ?? []),
      favoriteGenres: List<String>.from(
          (data['preferences'] as Map<String, dynamic>?)?['favoriteGenres'] ?? []),
      fcmToken: data['fcmToken'] ?? '',
      createdAt: (data['createdAt'] as Timestamp?)?.toDate() ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'email': email,
      'displayName': displayName,
      'likedSongs': likedSongs,
      'recentlyPlayed': recentlyPlayed,
      'preferences': {
        'favoriteGenres': favoriteGenres,
      },
      'fcmToken': fcmToken,
      'createdAt': Timestamp.fromDate(createdAt),
    };
  }

  UserModel copyWith({
    List<String>? likedSongs,
    List<String>? recentlyPlayed,
    List<String>? favoriteGenres,
    String? fcmToken,
    String? displayName,
  }) {
    return UserModel(
      id: id,
      email: email,
      displayName: displayName ?? this.displayName,
      likedSongs: likedSongs ?? this.likedSongs,
      recentlyPlayed: recentlyPlayed ?? this.recentlyPlayed,
      favoriteGenres: favoriteGenres ?? this.favoriteGenres,
      fcmToken: fcmToken ?? this.fcmToken,
      createdAt: createdAt,
    );
  }
}
