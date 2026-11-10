// lib/data/models/listen_together_model.dart
import 'package:cloud_firestore/cloud_firestore.dart';

class ListenTogetherRoom {
  final String roomId;
  final String hostId;
  final String? songId;
  final String songTitle;
  final String songArtist;
  final String songThumbnail;
  final String youtubeUrl;
  final bool isPlaying;
  final int positionMs;
  final DateTime lastUpdatedAt;
  final List<String> participants;

  const ListenTogetherRoom({
    required this.roomId,
    required this.hostId,
    this.songId,
    this.songTitle = '',
    this.songArtist = '',
    this.songThumbnail = '',
    this.youtubeUrl = '',
    this.isPlaying = false,
    this.positionMs = 0,
    required this.lastUpdatedAt,
    this.participants = const [],
  });

  factory ListenTogetherRoom.fromMap(Map<String, dynamic> data, String id) {
    return ListenTogetherRoom(
      roomId: id,
      hostId: data['hostId'] as String? ?? '',
      songId: data['songId'] as String?,
      songTitle: data['songTitle'] as String? ?? '',
      songArtist: data['songArtist'] as String? ?? '',
      songThumbnail: data['songThumbnail'] as String? ?? '',
      youtubeUrl: data['youtubeUrl'] as String? ?? '',
      isPlaying: data['isPlaying'] as bool? ?? false,
      positionMs: data['positionMs'] as int? ?? 0,
      lastUpdatedAt: (data['lastUpdatedAt'] as Timestamp?)?.toDate() ?? DateTime.now(),
      participants: List<String>.from(data['participants'] as List? ?? []),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'hostId': hostId,
      'songId': songId,
      'songTitle': songTitle,
      'songArtist': songArtist,
      'songThumbnail': songThumbnail,
      'youtubeUrl': youtubeUrl,
      'isPlaying': isPlaying,
      'positionMs': positionMs,
      'lastUpdatedAt': Timestamp.fromDate(lastUpdatedAt),
      'participants': participants,
    };
  }

  ListenTogetherRoom copyWith({
    String? roomId,
    String? hostId,
    String? songId,
    String? songTitle,
    String? songArtist,
    String? songThumbnail,
    String? youtubeUrl,
    bool? isPlaying,
    int? positionMs,
    DateTime? lastUpdatedAt,
    List<String>? participants,
  }) {
    return ListenTogetherRoom(
      roomId: roomId ?? this.roomId,
      hostId: hostId ?? this.hostId,
      songId: songId ?? this.songId,
      songTitle: songTitle ?? this.songTitle,
      songArtist: songArtist ?? this.songArtist,
      songThumbnail: songThumbnail ?? this.songThumbnail,
      youtubeUrl: youtubeUrl ?? this.youtubeUrl,
      isPlaying: isPlaying ?? this.isPlaying,
      positionMs: positionMs ?? this.positionMs,
      lastUpdatedAt: lastUpdatedAt ?? this.lastUpdatedAt,
      participants: participants ?? this.participants,
    );
  }
}

class RoomMessage {
  final String id;
  final String userId;
  final String displayName;
  final String text;
  final DateTime sentAt;

  const RoomMessage({
    required this.id,
    required this.userId,
    required this.displayName,
    required this.text,
    required this.sentAt,
  });

  factory RoomMessage.fromMap(Map<String, dynamic> data, String id) {
    return RoomMessage(
      id: id,
      userId: data['userId'] as String? ?? '',
      displayName: data['displayName'] as String? ?? 'Guest',
      text: data['text'] as String? ?? '',
      sentAt: (data['sentAt'] as Timestamp?)?.toDate() ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'userId': userId,
      'displayName': displayName,
      'text': text,
      'sentAt': Timestamp.fromDate(sentAt),
    };
  }
}
