// lib/data/models/playlist_model.dart
import 'package:cloud_firestore/cloud_firestore.dart';

class PlaylistModel {
  final String id;
  final String userId;
  final String name;
  final List<String> songIds;
  final bool isPublic;
  final String coverUrl;
  final DateTime createdAt;

  const PlaylistModel({
    required this.id,
    required this.userId,
    required this.name,
    this.songIds = const [],
    this.isPublic = false,
    this.coverUrl = '',
    required this.createdAt,
  });

  factory PlaylistModel.fromFirestore(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>;
    return PlaylistModel(
      id: doc.id,
      userId: data['userId'] ?? '',
      name: data['name'] ?? '',
      songIds: List<String>.from(data['songIds'] ?? []),
      isPublic: data['isPublic'] ?? false,
      coverUrl: data['coverUrl'] ?? '',
      createdAt: (data['createdAt'] as Timestamp?)?.toDate() ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'userId': userId,
      'name': name,
      'songIds': songIds,
      'isPublic': isPublic,
      'coverUrl': coverUrl,
      'createdAt': Timestamp.fromDate(createdAt),
    };
  }
}
