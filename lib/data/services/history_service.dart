// lib/data/services/history_service.dart
// History service using SharedPreferences for local storage of recently played songs.

import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/song_model.dart';

class HistoryService {
  static const String _key = 'vibewave_song_history';
  static const int _maxHistorySize = 50;

  static final HistoryService _instance = HistoryService._internal();
  factory HistoryService() => _instance;
  HistoryService._internal();

  List<SongModel> _history = [];

  /// Initialize history from local preferences.
  Future<void> init() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final rawList = prefs.getStringList(_key) ?? [];
      _history = rawList
          .map((jsonStr) {
            try {
              final map = jsonDecode(jsonStr) as Map<String, dynamic>;
              final id = map['id'] as String? ?? '';
              return SongModel.fromMap(map, id);
            } catch (_) {
              return null;
            }
          })
          .whereType<SongModel>()
          .toList();
    } catch (_) {}
  }

  /// Add a song to the listening history (most recent first, no duplicates).
  Future<void> addToHistory(SongModel song) async {
    _history.removeWhere((s) => s.id == song.id);
    _history.insert(0, song);
    if (_history.length > _maxHistorySize) {
      _history = _history.sublist(0, _maxHistorySize);
    }
    try {
      final prefs = await SharedPreferences.getInstance();
      final list = _history.map((s) => jsonEncode(s.toMap())).toList();
      await prefs.setStringList(_key, list);
    } catch (_) {}
  }

  /// Get recently played songs, most recent first.
  List<SongModel> getHistory() {
    return List.unmodifiable(_history);
  }

  /// Clear all history.
  Future<void> clearHistory() async {
    _history.clear();
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_key);
    } catch (_) {}
  }

  void dispose() {}
}
