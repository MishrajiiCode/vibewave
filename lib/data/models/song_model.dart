// lib/data/models/song_model.dart
import 'package:cloud_firestore/cloud_firestore.dart';

class SongModel {
  final String id;
  final String title;
  final String artist;
  final String album;
  final String youtubeUrl;
  final String videoId;
  final String thumbnailUrl;
  final String category;
  final List<String> tags;
  final String language;
  final int year;
  final int duration; // seconds
  final bool isFeatured;
  final String lyrics;
  final int playCount;
  final int likeCount;
  final DateTime createdAt;
  final DateTime updatedAt;

  const SongModel({
    required this.id,
    required this.title,
    required this.artist,
    this.album = '',
    required this.youtubeUrl,
    required this.videoId,
    required this.thumbnailUrl,
    required this.category,
    this.tags = const [],
    this.language = 'English',
    this.year = 2024,
    this.duration = 0,
    this.isFeatured = false,
    this.lyrics = '',
    this.playCount = 0,
    this.likeCount = 0,
    required this.createdAt,
    required this.updatedAt,
  });

  factory SongModel.fromFirestore(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>;
    return SongModel(
      id: doc.id,
      title: data['title'] ?? '',
      artist: data['artist'] ?? '',
      album: data['album'] ?? '',
      youtubeUrl: data['youtubeUrl'] ?? '',
      videoId: data['videoId'] ?? '',
      thumbnailUrl: data['thumbnailUrl'] ?? '',
      category: data['category'] ?? '',
      tags: List<String>.from(data['tags'] ?? []),
      language: data['language'] ?? 'English',
      year: data['year'] ?? 2024,
      duration: data['duration'] ?? 0,
      isFeatured: data['isFeatured'] ?? false,
      lyrics: data['lyrics'] ?? '',
      playCount: data['playCount'] ?? 0,
      likeCount: data['likeCount'] ?? 0,
      createdAt: (data['createdAt'] as Timestamp?)?.toDate() ?? DateTime.now(),
      updatedAt: (data['updatedAt'] as Timestamp?)?.toDate() ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'title': title,
      'artist': artist,
      'album': album,
      'youtubeUrl': youtubeUrl,
      'videoId': videoId,
      'thumbnailUrl': thumbnailUrl,
      'category': category,
      'tags': tags,
      'language': language,
      'year': year,
      'duration': duration,
      'isFeatured': isFeatured,
      'lyrics': lyrics,
      'playCount': playCount,
      'likeCount': likeCount,
      'createdAt': Timestamp.fromDate(createdAt),
      'updatedAt': Timestamp.fromDate(updatedAt),
    };
  }

  SongModel copyWith({
    String? id,
    String? title,
    String? artist,
    String? album,
    String? youtubeUrl,
    String? videoId,
    String? thumbnailUrl,
    String? category,
    List<String>? tags,
    String? language,
    int? year,
    int? duration,
    bool? isFeatured,
    String? lyrics,
    int? playCount,
    int? likeCount,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return SongModel(
      id: id ?? this.id,
      title: title ?? this.title,
      artist: artist ?? this.artist,
      album: album ?? this.album,
      youtubeUrl: youtubeUrl ?? this.youtubeUrl,
      videoId: videoId ?? this.videoId,
      thumbnailUrl: thumbnailUrl ?? this.thumbnailUrl,
      category: category ?? this.category,
      tags: tags ?? this.tags,
      language: language ?? this.language,
      year: year ?? this.year,
      duration: duration ?? this.duration,
      isFeatured: isFeatured ?? this.isFeatured,
      lyrics: lyrics ?? this.lyrics,
      playCount: playCount ?? this.playCount,
      likeCount: likeCount ?? this.likeCount,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  String get formattedDuration {
    final minutes = duration ~/ 60;
    final seconds = duration % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  static String extractVideoId(String url) {
    final regExp = RegExp(
      r'(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})',
    );
    final match = regExp.firstMatch(url);
    return match?.group(1) ?? '';
  }

  static String getThumbnailUrl(String videoId) {
    return 'https://img.youtube.com/vi/$videoId/maxresdefault.jpg';
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is SongModel && id == other.id;

  @override
  int get hashCode => id.hashCode;
}
