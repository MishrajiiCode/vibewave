// lib/presentation/providers/settings_provider.dart
// Settings providers – audio quality, crossfade, skip silence, speed.
// Values are persisted to SharedPreferences.

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../data/services/youtube_service.dart';

// ── Keys ──────────────────────────────────────────────────────────────────────
const _kAudioQuality = 'settings_audio_quality';
const _kCrossfade = 'settings_crossfade';
const _kSkipSilence = 'settings_skip_silence';
const _kPlaybackSpeed = 'settings_playback_speed';

// ── SharedPreferences provider ────────────────────────────────────────────────
final sharedPreferencesProvider =
    FutureProvider<SharedPreferences>((ref) => SharedPreferences.getInstance());

// ── Audio Quality ─────────────────────────────────────────────────────────────
final audioQualityProvider =
    StateProvider<AudioQuality>((ref) {
  return AudioQuality.highest;
});

// Persist quality change
void persistAudioQuality(SharedPreferences prefs, AudioQuality quality) {
  prefs.setString(_kAudioQuality, quality.name);
}

AudioQuality loadAudioQuality(SharedPreferences prefs) {
  final stored = prefs.getString(_kAudioQuality);
  return AudioQuality.values.firstWhere(
    (q) => q.name == stored,
    orElse: () => AudioQuality.highest,
  );
}

// ── Crossfade Duration ────────────────────────────────────────────────────────
final crossfadeDurationProvider = StateProvider<int>((ref) => 0);

// ── Skip Silence ──────────────────────────────────────────────────────────────
final skipSilenceProvider = StateProvider<bool>((ref) => false);

// ── Playback Speed ────────────────────────────────────────────────────────────
final playbackSpeedProvider = StateProvider<double>((ref) => 1.0);

// ── Persistence helpers ───────────────────────────────────────────────────────
Future<void> saveCrossfade(SharedPreferences prefs, int seconds) =>
    prefs.setInt(_kCrossfade, seconds);

Future<void> saveSkipSilence(SharedPreferences prefs, bool value) =>
    prefs.setBool(_kSkipSilence, value);

Future<void> savePlaybackSpeed(SharedPreferences prefs, double speed) =>
    prefs.setDouble(_kPlaybackSpeed, speed);

Future<void> loadAndApplySettings(WidgetRef ref, SharedPreferences prefs) async {
  final quality = loadAudioQuality(prefs);
  ref.read(audioQualityProvider.notifier).state = quality;
  ref.read(crossfadeDurationProvider.notifier).state =
      prefs.getInt(_kCrossfade) ?? 0;
  ref.read(skipSilenceProvider.notifier).state =
      prefs.getBool(_kSkipSilence) ?? false;
  ref.read(playbackSpeedProvider.notifier).state =
      prefs.getDouble(_kPlaybackSpeed) ?? 1.0;
}
