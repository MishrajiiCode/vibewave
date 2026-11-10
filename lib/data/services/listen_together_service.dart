// lib/data/services/listen_together_service.dart
// Listen Together Service using Firestore
// A "room" is a Firestore document in collection "listen_together_rooms"

import 'dart:math';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../models/listen_together_model.dart';
import '../models/song_model.dart';

class ListenTogetherService {
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  final FirebaseAuth _auth = FirebaseAuth.instance;

  static const String _collection = 'listen_together_rooms';
  static const String _messagesSubcollection = 'messages';

  String? _currentRoomId;
  String? get currentRoomId => _currentRoomId;

  String get _currentUserId {
    final user = _auth.currentUser;
    return user?.uid ?? 'anonymous_${DateTime.now().millisecondsSinceEpoch}';
  }

  String get _displayName {
    final user = _auth.currentUser;
    return user?.displayName ?? user?.email?.split('@').first ?? 'Guest';
  }

  /// Generate a 6-character alphanumeric room code
  String _generateRoomCode() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    final random = Random.secure();
    return List.generate(6, (_) => chars[random.nextInt(chars.length)]).join();
  }

  /// Create a new Listen Together room. Returns the room ID.
  Future<String> createRoom(SongModel song) async {
    final roomId = _generateRoomCode();
    final userId = _currentUserId;

    final room = ListenTogetherRoom(
      roomId: roomId,
      hostId: userId,
      songId: song.id,
      songTitle: song.title,
      songArtist: song.artist,
      songThumbnail: song.thumbnailUrl,
      youtubeUrl: song.youtubeUrl,
      isPlaying: false,
      positionMs: 0,
      lastUpdatedAt: DateTime.now(),
      participants: [userId],
    );

    await _firestore.collection(_collection).doc(roomId).set(room.toMap());
    _currentRoomId = roomId;
    return roomId;
  }

  /// Join an existing room. Returns the room if found, null otherwise.
  Future<ListenTogetherRoom?> joinRoom(String roomId) async {
    final doc = await _firestore.collection(_collection).doc(roomId.toUpperCase()).get();
    if (!doc.exists) return null;

    final room = ListenTogetherRoom.fromMap(doc.data()!, doc.id);
    _currentRoomId = roomId.toUpperCase();

    // Add self to participants
    await _firestore.collection(_collection).doc(_currentRoomId).update({
      'participants': FieldValue.arrayUnion([_currentUserId]),
    });

    return room;
  }

  /// Leave the current room
  Future<void> leaveRoom() async {
    if (_currentRoomId == null) return;

    final userId = _currentUserId;

    // Remove self from participants
    await _firestore.collection(_collection).doc(_currentRoomId).update({
      'participants': FieldValue.arrayRemove([userId]),
    });

    // If host, delete the room
    final doc = await _firestore.collection(_collection).doc(_currentRoomId).get();
    if (doc.exists) {
      final data = doc.data()!;
      if (data['hostId'] == userId) {
        await _firestore.collection(_collection).doc(_currentRoomId).delete();
      }
    }

    _currentRoomId = null;
  }

  /// Send a playback update (host only)
  Future<void> sendPlaybackUpdate({
    required bool isPlaying,
    required int positionMs,
    SongModel? song,
  }) async {
    if (_currentRoomId == null) return;

    final update = <String, dynamic>{
      'isPlaying': isPlaying,
      'positionMs': positionMs,
      'lastUpdatedAt': Timestamp.fromDate(DateTime.now()),
    };

    if (song != null) {
      update['songId'] = song.id;
      update['songTitle'] = song.title;
      update['songArtist'] = song.artist;
      update['songThumbnail'] = song.thumbnailUrl;
      update['youtubeUrl'] = song.youtubeUrl;
    }

    await _firestore.collection(_collection).doc(_currentRoomId).update(update);
  }

  /// Stream real-time room updates
  Stream<ListenTogetherRoom?> listenToRoomUpdates() {
    if (_currentRoomId == null) return Stream.value(null);

    return _firestore
        .collection(_collection)
        .doc(_currentRoomId)
        .snapshots()
        .map((doc) {
      if (!doc.exists) return null;
      return ListenTogetherRoom.fromMap(doc.data()!, doc.id);
    });
  }

  /// Send a chat message
  Future<void> sendMessage(String text) async {
    if (_currentRoomId == null || text.trim().isEmpty) return;

    final message = RoomMessage(
      id: '',
      userId: _currentUserId,
      displayName: _displayName,
      text: text.trim(),
      sentAt: DateTime.now(),
    );

    await _firestore
        .collection(_collection)
        .doc(_currentRoomId)
        .collection(_messagesSubcollection)
        .add(message.toMap());
  }

  /// Stream chat messages
  Stream<List<RoomMessage>> streamMessages() {
    if (_currentRoomId == null) return Stream.value([]);

    return _firestore
        .collection(_collection)
        .doc(_currentRoomId)
        .collection(_messagesSubcollection)
        .orderBy('sentAt', descending: false)
        .snapshots()
        .map((snapshot) => snapshot.docs
            .map((doc) => RoomMessage.fromMap(doc.data(), doc.id))
            .toList());
  }

  /// Check if a room exists
  Future<bool> roomExists(String roomId) async {
    final doc = await _firestore
        .collection(_collection)
        .doc(roomId.toUpperCase())
        .get();
    return doc.exists;
  }

  void dispose() {
    _currentRoomId = null;
  }
}

