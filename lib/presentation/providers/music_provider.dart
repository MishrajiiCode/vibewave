// lib/presentation/providers/music_provider.dart
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/song_model.dart';
import '../../data/models/category_model.dart';
import '../../data/services/firebase_music_service.dart';

final allSongsProvider = StreamProvider<List<SongModel>>((ref) {
  return FirebaseMusicService.getSongsStream();
});

final featuredSongsProvider = StreamProvider<List<SongModel>>((ref) {
  return FirebaseMusicService.getFeaturedSongsStream();
});

final trendingSongsProvider = StreamProvider<List<SongModel>>((ref) {
  return FirebaseMusicService.getTrendingSongsStream();
});

final newReleasesProvider = StreamProvider<List<SongModel>>((ref) {
  return FirebaseMusicService.getNewReleasesStream();
});

final categoriesProvider = StreamProvider<List<CategoryModel>>((ref) {
  return FirebaseMusicService.getCategoriesStream();
});

final selectedCategoryProvider = StateProvider<String?>((ref) => null);

final songsByCategoryProvider =
    StreamProvider.family<List<SongModel>, String>((ref, category) {
  return FirebaseMusicService.getSongsByCategoryStream(category);
});

final searchQueryProvider = StateProvider<String>((ref) => '');

final searchResultsProvider = FutureProvider<List<SongModel>>((ref) async {
  final query = ref.watch(searchQueryProvider);
  if (query.isEmpty) return [];
  return FirebaseMusicService.searchSongs(query);
});

final likedSongIdsProvider = StateProvider<Set<String>>((ref) => {});
