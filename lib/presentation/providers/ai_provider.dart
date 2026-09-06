// lib/presentation/providers/ai_provider.dart
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/song_model.dart';
import '../../data/services/firebase_music_service.dart';
import 'auth_provider.dart';

final selectedMoodProvider = StateProvider<String?>((ref) => null);

final aiRecommendationsProvider =
    FutureProvider.autoDispose<List<SongModel>>((ref) async {
  final mood = ref.watch(selectedMoodProvider);
  final user = ref.watch(currentUserProvider);
  if (user == null) return [];

  return FirebaseMusicService.getAIRecommendations(
    userId: user.uid,
    mood: mood,
  );
});

final aiAnalysisProvider = Provider<Map<String, dynamic>>((ref) {
  // Smart rule-based analysis without external AI
  final mood = ref.watch(selectedMoodProvider);
  final hour = DateTime.now().hour;

  String timeContext;
  if (hour >= 5 && hour < 12) {
    timeContext = 'morning';
  } else if (hour >= 12 && hour < 17) {
    timeContext = 'afternoon';
  } else if (hour >= 17 && hour < 21) {
    timeContext = 'evening';
  } else {
    timeContext = 'night';
  }

  final String suggestion = _getTimeSuggestion(timeContext, mood);

  return {
    'timeContext': timeContext,
    'mood': mood,
    'suggestion': suggestion,
  };
});

String _getTimeSuggestion(String timeContext, String? mood) {
  if (mood != null) {
    if (mood.contains('Happy')) return 'Upbeat tracks to match your energy!';
    if (mood.contains('Sad')) return 'Soulful music to accompany your feelings';
    if (mood.contains('Energetic')) return 'High-energy beats to keep you going!';
    if (mood.contains('Chill')) return 'Relaxing vibes for your chill session';
    if (mood.contains('Romantic')) return 'Sweet melodies for your romantic mood';
    if (mood.contains('Focus')) return 'Ambient music to boost your focus';
    if (mood.contains('Party')) return 'Party anthems to get the crowd moving!';
    if (mood.contains('Workout')) return 'Powerful tracks to fuel your workout!';
  }
  switch (timeContext) {
    case 'morning':
      return 'Good morning! Start your day with fresh beats';
    case 'afternoon':
      return 'Keep the energy up this afternoon';
    case 'evening':
      return 'Wind down with some evening vibes';
    case 'night':
      return 'Perfect late-night listening session';
    default:
      return 'Personalized picks just for you';
  }
}
