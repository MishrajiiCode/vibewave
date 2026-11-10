// lib/data/services/history_service.dart
// History service using Hive for local storage of recently played songs.

import 'package:hive_flutter/hive_flutter.dart';
import '../models/song_model.dart';

class HistoryService {
  static const String _boxName = 'song_history';
  static const int _maxHistorySize = 50;

  Box<Map>? _box;

  Future<void> init() async {
    await Hive.initFlutter();
    _box = await Hive.openBox<Map>(_boxName);
  }

  /// Add a song to the listening history (most recent first, no duplicates).
  Future<void> addToHistory(SongModel song) async {
    final box = _box;
    if (box == null) return;

    // Remove existing entry for this song to avoid duplicates
    final existingKey = box.keys.firstWhere(
      (k) => (box.get(k)?['id'] as String?) == song.id,
      orElse: () => null,
    );
    if (existingKey != null) {
      await box.delete(existingKey);
    }

    // Build a serialisable map (avoid DateTime serialisation issues)
    final entry = <String, dynamic>{
      'id': song.id,
      'title': song.title,
      'artist': song.artist,
      'album': song.album,
      'youtubeUrl': song.youtubeUrl,
      'videoId': song.videoId,
      'thumbnailUrl': song.thumbnailUrl,
      'category': song.category,
      'tags': song.tags,
      'language': song.language,
      'year': song.year,
      'duration': song.duration,
      'isFeatured': song.isFeatured,
      'lyrics': song.lyrics,
      'playCount': song.playCount,
      'likeCount': song.likeCount,
      'createdAt': song.createdAt.millisecondsSinceEpoch,
      'updatedAt': song.updatedAt.millisecondsSinceEpoch,
      'playedAt': DateTime.now().millisecondsSinceEpoch,
    };

    // Use timestamp as key so entries are ordered naturally
    await box.put('${DateTime.now().millisecondsSinceEpoch}_${song.id}', entry);

    // Trim to max size
    if (box.length > _maxHistorySize) {
      final sortedKeys = box.keys.toList()..sort();
      final toDelete = sortedKeys.take(box.length - _maxHistorySize);
      for (final key in toDelete) {
        await box.delete(key);
      }
    }
  }

  /// Get recently played songs, most recent first.
  List<SongModel> getHistory() {
    final box = _box;
    if (box == null) return [];

    final entries = box.keys.map((k) {
      final raw = box.get(k);
      return raw == null ? null : _mapToSong(raw.cast<String, dynamic>());
    }).whereType<SongModel>().toList();

    // Sort by playedAt descending
    entries.sort((a, b) {
      final aTime = box.get(entries.indexOf(a).toString())?['playedAt'] as int? ?? 0;
      final bTime = box.get(entries.indexOf(b).toString())?['playedAt'] as int? ?? 0;
      return bTime.compareTo(aTime);
    });

    // Simpler: just reverse the natural key order (keys are timestamp-based)
    return entries.reversed.toList();
  }

  SongModel _mapToSong(Map<String, dynamic> data) {
    return SongModel(
      id: data['id'] as String? ?? '',
      title: data['title'] as String? ?? '',
      artist: data['artist'] as String? ?? '',
      album: data['album'] as String? ?? '',
      youtubeUrl: data['youtubeUrl'] as String? ?? '',
      videoId: data['videoId'] as String? ?? '',
      thumbnailUrl: data['thumbnailUrl'] as String? ?? '',
      category: data['category'] as String? ?? '',
      tags: (data['tags'] as List?)?.cast<String>() ?? [],
      language: data['language'] as String? ?? 'English',
      year: data['year'] as int? ?? 2024,
      duration: data['duration'] as int? ?? 0,
      isFeatured: data['isFeatured'] as bool? ?? false,
      lyrics: data['lyrics'] as String? ?? '',
      playCount: data['playCount'] as int? ?? 0,
      likeCount: data['likeCount'] as int? ?? 0,
      createdAt: DateTime.fromMillisecondsSinceEpoch(
        data['createdAt'] as int? ?? 0,
      ),
      updatedAt: DateTime.fromMillisecondsSinceEpoch(
        data['updatedAt'] as int? ?? 0,
      ),
    );
  }

  /// Clear all history.
  Future<void> clearHistory() async {
    await _box?.clear();
  }

  Future<void> dispose() async {
    await _box?.close();
  }
}
