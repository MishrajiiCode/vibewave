// lib/presentation/providers/player_provider.dart
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/song_model.dart';
import '../../data/services/audio_player_service.dart';

final audioPlayerServiceProvider = Provider<AudioPlayerService>((ref) {
  final service = AudioPlayerService();
  ref.onDispose(() => service.dispose());
  return service;
});

final playerStateProvider = StreamProvider<PlayerState>((ref) {
  final service = ref.watch(audioPlayerServiceProvider);
  return service.stateStream;
});

final currentSongProvider = Provider<SongModel?>((ref) {
  final state = ref.watch(playerStateProvider);
  return state.whenOrNull(data: (s) => s.currentSong);
});

final isPlayingProvider = Provider<bool>((ref) {
  final state = ref.watch(playerStateProvider);
  return state.whenOrNull(data: (s) => s.isPlaying) ?? false;
});

final isLoadingProvider = Provider<bool>((ref) {
  final state = ref.watch(playerStateProvider);
  return state.whenOrNull(data: (s) => s.isLoading) ?? false;
});

// Show mini player when a song is loaded
final showMiniPlayerProvider = Provider<bool>((ref) {
  final song = ref.watch(currentSongProvider);
  return song != null;
});
