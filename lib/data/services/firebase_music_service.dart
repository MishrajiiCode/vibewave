// lib/data/services/firebase_music_service.dart
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../models/song_model.dart';
import '../models/category_model.dart';
import '../models/user_model.dart';
import '../models/playlist_model.dart';

class FirebaseMusicService {
  static final FirebaseFirestore _db = FirebaseFirestore.instance;
  static final FirebaseAuth _auth = FirebaseAuth.instance;

  // =================== SONGS ===================

  static Stream<List<SongModel>> getSongsStream() {
    return _db
        .collection('songs')
        .orderBy('createdAt', descending: true)
        .snapshots()
        .map((snap) => snap.docs.map(SongModel.fromFirestore).toList());
  }

  static Stream<List<SongModel>> getFeaturedSongsStream() {
    return _db
        .collection('songs')
        .where('isFeatured', isEqualTo: true)
        .orderBy('playCount', descending: true)
        .limit(10)
        .snapshots()
        .map((snap) => snap.docs.map(SongModel.fromFirestore).toList());
  }

  static Stream<List<SongModel>> getSongsByCategoryStream(String category) {
    return _db
        .collection('songs')
        .where('category', isEqualTo: category)
        .orderBy('playCount', descending: true)
        .snapshots()
        .map((snap) => snap.docs.map(SongModel.fromFirestore).toList());
  }

  static Future<List<SongModel>> searchSongs(String query) async {
    if (query.isEmpty) return [];
    final q = query.toLowerCase();
    final snapshot = await _db.collection('songs').get();
    return snapshot.docs
        .map(SongModel.fromFirestore)
        .where((song) =>
            song.title.toLowerCase().contains(q) ||
            song.artist.toLowerCase().contains(q) ||
            song.album.toLowerCase().contains(q) ||
            song.tags.any((tag) => tag.toLowerCase().contains(q)))
        .toList();
  }

  static Future<String> addSong(SongModel song) async {
    final docRef = await _db.collection('songs').add(song.toFirestore());
    return docRef.id;
  }

  static Future<void> updateSong(SongModel song) async {
    await _db.collection('songs').doc(song.id).update(song.toFirestore());
  }

  static Future<void> deleteSong(String songId) async {
    await _db.collection('songs').doc(songId).delete();
  }

  static Future<void> incrementPlayCount(String songId) async {
    await _db.collection('songs').doc(songId).update({
      'playCount': FieldValue.increment(1),
    });
  }

  static Stream<List<SongModel>> getTrendingSongsStream() {
    return _db
        .collection('songs')
        .orderBy('playCount', descending: true)
        .limit(20)
        .snapshots()
        .map((snap) => snap.docs.map(SongModel.fromFirestore).toList());
  }

  static Stream<List<SongModel>> getNewReleasesStream() {
    return _db
        .collection('songs')
        .orderBy('createdAt', descending: true)
        .limit(10)
        .snapshots()
        .map((snap) => snap.docs.map(SongModel.fromFirestore).toList());
  }

  // =================== CATEGORIES ===================

  static Stream<List<CategoryModel>> getCategoriesStream() {
    return _db
        .collection('categories')
        .orderBy('order')
        .snapshots()
        .map((snap) => snap.docs.map(CategoryModel.fromFirestore).toList());
  }

  static Future<void> addCategory(CategoryModel category) async {
    await _db.collection('categories').add(category.toFirestore());
  }

  static Future<void> updateCategory(CategoryModel category) async {
    await _db
        .collection('categories')
        .doc(category.id)
        .update(category.toFirestore());
  }

  static Future<void> deleteCategory(String categoryId) async {
    await _db.collection('categories').doc(categoryId).delete();
  }

  static Future<void> initializeDefaultCategories() async {
    final snap = await _db.collection('categories').limit(1).get();
    if (snap.docs.isEmpty) {
      final batch = _db.batch();
      for (final cat in CategoryModel.defaults) {
        final ref = _db.collection('categories').doc(cat.id);
        batch.set(ref, cat.toFirestore());
      }
      await batch.commit();
    }
  }

  // =================== USER ===================

  static Future<UserModel?> getUser(String userId) async {
    final doc = await _db.collection('users').doc(userId).get();
    if (!doc.exists) return null;
    return UserModel.fromFirestore(doc);
  }

  static Future<void> createOrUpdateUser(UserModel user) async {
    await _db
        .collection('users')
        .doc(user.id)
        .set(user.toFirestore(), SetOptions(merge: true));
  }

  static Future<void> toggleLikeSong(String userId, String songId, bool liked) async {
    await _db.collection('users').doc(userId).update({
      'likedSongs': liked
          ? FieldValue.arrayUnion([songId])
          : FieldValue.arrayRemove([songId]),
    });
    await _db.collection('songs').doc(songId).update({
      'likeCount': liked ? FieldValue.increment(1) : FieldValue.increment(-1),
    });
  }

  static Future<void> addToRecentlyPlayed(String userId, String songId) async {
    final userRef = _db.collection('users').doc(userId);
    final doc = await userRef.get();
    if (!doc.exists) return;

    final recent = List<String>.from(
        (doc.data() as Map<String, dynamic>)['recentlyPlayed'] ?? []);
    recent.remove(songId);
    recent.insert(0, songId);
    if (recent.length > 50) recent.removeLast();

    await userRef.update({'recentlyPlayed': recent});
  }

  static Future<void> updateFcmToken(String userId, String token) async {
    await _db.collection('users').doc(userId).update({'fcmToken': token});
  }

  // =================== PLAYLISTS ===================

  static Stream<List<PlaylistModel>> getUserPlaylistsStream(String userId) {
    return _db
        .collection('playlists')
        .where('userId', isEqualTo: userId)
        .orderBy('createdAt', descending: true)
        .snapshots()
        .map((snap) => snap.docs.map(PlaylistModel.fromFirestore).toList());
  }

  static Future<String> createPlaylist(PlaylistModel playlist) async {
    final ref = await _db.collection('playlists').add(playlist.toFirestore());
    return ref.id;
  }

  static Future<void> updatePlaylist(PlaylistModel playlist) async {
    await _db
        .collection('playlists')
        .doc(playlist.id)
        .update(playlist.toFirestore());
  }

  static Future<void> deletePlaylist(String playlistId) async {
    await _db.collection('playlists').doc(playlistId).delete();
  }

  // =================== AUTH ===================

  static User? get currentUser => _auth.currentUser;

  static Future<UserCredential> signInAdmin(
      String email, String password) async {
    return await _auth.signInWithEmailAndPassword(
        email: email, password: password);
  }

  static Future<void> signOut() async => await _auth.signOut();

  static Future<UserCredential> signInAnonymously() async {
    return await _auth.signInAnonymously();
  }

  static Stream<User?> get authStateStream => _auth.authStateChanges();

  // =================== AI RECOMMENDATIONS ===================

  /// Smart Firestore-based recommendations (no external AI API)
  static Future<List<SongModel>> getAIRecommendations({
    required String userId,
    String? mood,
    String? preferredCategory,
  }) async {
    try {
      final user = await getUser(userId);
      if (user == null) return await _getFallbackRecommendations(mood);

      // Get user's top genres based on recently played
      final recentIds = user.recentlyPlayed.take(20).toList();
      if (recentIds.isEmpty) return await _getFallbackRecommendations(mood);

      // Fetch recently played songs to analyze
      final recentSongs = <SongModel>[];
      for (final id in recentIds.take(10)) {
        final doc = await _db.collection('songs').doc(id).get();
        if (doc.exists) recentSongs.add(SongModel.fromFirestore(doc));
      }

      // Count category frequencies
      final Map<String, int> categoryCounts = {};
      for (final song in recentSongs) {
        categoryCounts[song.category] = (categoryCounts[song.category] ?? 0) + 1;
      }

      // Sort categories by frequency
      final sortedCategories = categoryCounts.entries.toList()
        ..sort((a, b) => b.value.compareTo(a.value));

      // Get top category
      final topCategory = preferredCategory ??
          (sortedCategories.isNotEmpty ? sortedCategories.first.key : null);

      Query<Map<String, dynamic>> query = _db.collection('songs');

      if (topCategory != null) {
        query = query.where('category', isEqualTo: topCategory);
      }

      // Mood-based filtering using tags
      if (mood != null && mood.isNotEmpty) {
        final moodTag = _moodToTag(mood);
        if (moodTag.isNotEmpty) {
          query = query.where('tags', arrayContains: moodTag);
        }
      }

      final snap = await query
          .orderBy('playCount', descending: true)
          .limit(20)
          .get();

      var songs = snap.docs.map(SongModel.fromFirestore).toList();

      // Filter out recently played (already heard)
      songs = songs.where((s) => !recentIds.contains(s.id)).toList();

      if (songs.isEmpty) return await _getFallbackRecommendations(mood);

      // Shuffle for variety
      songs.shuffle();
      return songs.take(10).toList();
    } catch (e) {
      print('AI Recommendations error: $e');
      return await _getFallbackRecommendations(mood);
    }
  }

  static String _moodToTag(String mood) {
    final cleaned = mood.toLowerCase();
    if (cleaned.contains('happy')) return 'happy';
    if (cleaned.contains('sad')) return 'sad';
    if (cleaned.contains('energetic')) return 'energetic';
    if (cleaned.contains('chill')) return 'chill';
    if (cleaned.contains('romantic')) return 'romantic';
    if (cleaned.contains('focus')) return 'focus';
    if (cleaned.contains('party')) return 'party';
    if (cleaned.contains('workout')) return 'workout';
    return '';
  }

  static Future<List<SongModel>> _getFallbackRecommendations(String? mood) async {
    Query<Map<String, dynamic>> query = _db.collection('songs');
    if (mood != null) {
      final tag = _moodToTag(mood);
      if (tag.isNotEmpty) {
        query = query.where('tags', arrayContains: tag);
      }
    }
    final snap = await query.orderBy('playCount', descending: true).limit(10).get();
    return snap.docs.map(SongModel.fromFirestore).toList();
  }
}
