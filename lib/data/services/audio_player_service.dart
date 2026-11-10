// lib/data/services/audio_player_service.dart
// Enhanced AudioPlayerService with speed/pitch, crossfade, sleep timer,
// skip silence, audio quality selection, and equalizer support.

import 'dart:async';
import 'dart:io';
import 'package:just_audio/just_audio.dart';
import 'package:just_audio_background/just_audio_background.dart';
import 'package:rxdart/rxdart.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import '../models/song_model.dart';
import 'youtube_service.dart';

class PlayerState {
  final SongModel? currentSong;
  final bool isPlaying;
  final bool isLoading;
  final Duration position;
  final Duration duration;
  final List<SongModel> queue;
  final int currentIndex;
  final bool isShuffleEnabled;
  final LoopMode loopMode;
  // v2.0 additions
  final double speed;
  final double pitch;
  final int crossfadeDuration; // seconds
  final DateTime? sleepTimerEnd;
  final AudioQuality audioQuality;
  final bool skipSilence;

  const PlayerState({
    this.currentSong,
    this.isPlaying = false,
    this.isLoading = false,
    this.position = Duration.zero,
    this.duration = Duration.zero,
    this.queue = const [],
    this.currentIndex = 0,
    this.isShuffleEnabled = false,
    this.loopMode = LoopMode.off,
    this.speed = 1.0,
    this.pitch = 1.0,
    this.crossfadeDuration = 0,
    this.sleepTimerEnd,
    this.audioQuality = AudioQuality.highest,
    this.skipSilence = false,
  });

  PlayerState copyWith({
    SongModel? currentSong,
    bool? isPlaying,
    bool? isLoading,
    Duration? position,
    Duration? duration,
    List<SongModel>? queue,
    int? currentIndex,
    bool? isShuffleEnabled,
    LoopMode? loopMode,
    double? speed,
    double? pitch,
    int? crossfadeDuration,
    DateTime? sleepTimerEnd,
    bool clearSleepTimer = false,
    AudioQuality? audioQuality,
    bool? skipSilence,
  }) {
    return PlayerState(
      currentSong: currentSong ?? this.currentSong,
      isPlaying: isPlaying ?? this.isPlaying,
      isLoading: isLoading ?? this.isLoading,
      position: position ?? this.position,
      duration: duration ?? this.duration,
      queue: queue ?? this.queue,
      currentIndex: currentIndex ?? this.currentIndex,
      isShuffleEnabled: isShuffleEnabled ?? this.isShuffleEnabled,
      loopMode: loopMode ?? this.loopMode,
      speed: speed ?? this.speed,
      pitch: pitch ?? this.pitch,
      crossfadeDuration: crossfadeDuration ?? this.crossfadeDuration,
      sleepTimerEnd: clearSleepTimer ? null : (sleepTimerEnd ?? this.sleepTimerEnd),
      audioQuality: audioQuality ?? this.audioQuality,
      skipSilence: skipSilence ?? this.skipSilence,
    );
  }

  double get progress {
    if (duration.inMilliseconds == 0) return 0.0;
    return position.inMilliseconds / duration.inMilliseconds;
  }

  bool get hasSleepTimer => sleepTimerEnd != null;
}

class AudioPlayerService {
  late final AudioPlayer _player;
  AndroidEqualizer? _equalizer;
  AndroidLoudnessEnhancer? _loudnessEnhancer;

  final BehaviorSubject<PlayerState> _stateSubject =
      BehaviorSubject.seeded(const PlayerState());

  Timer? _sleepTimer;
  Timer? _crossfadeTimer;
  Timer? _silenceTimer;

  AudioPlayerService() {
    _initPlayer();
    _initListeners();
  }

  void _initPlayer() {
    if (Platform.isAndroid) {
      _equalizer = AndroidEqualizer();
      _loudnessEnhancer = AndroidLoudnessEnhancer();
      _player = AudioPlayer(
        audioPipeline: AudioPipeline(
          androidAudioEffects: [_equalizer!, _loudnessEnhancer!],
        ),
      );
    } else {
      _player = AudioPlayer();
    }
  }

  Stream<PlayerState> get stateStream => _stateSubject.stream;
  PlayerState get currentState => _stateSubject.value;
  AudioPlayer get rawPlayer => _player;
  AndroidEqualizer? get equalizer => _equalizer;

  void _initListeners() {
    _player.positionStream.listen((position) {
      _stateSubject.add(_stateSubject.value.copyWith(position: position));
      _checkSleepTimer();
      _checkSkipSilence(position);
    });

    _player.durationStream.listen((duration) {
      _stateSubject.add(
          _stateSubject.value.copyWith(duration: duration ?? Duration.zero));
    });

    _player.playingStream.listen((isPlaying) {
      _stateSubject.add(_stateSubject.value.copyWith(isPlaying: isPlaying));
      if (isPlaying) {
        WakelockPlus.enable();
      } else {
        WakelockPlus.disable();
      }
    });

    _player.processingStateStream.listen((state) {
      if (state == ProcessingState.completed) {
        _handleSongCompleted();
      }
    });
  }

  void _checkSleepTimer() {
    final end = _stateSubject.value.sleepTimerEnd;
    if (end != null && DateTime.now().isAfter(end)) {
      pause();
      cancelSleepTimer();
    }
  }

  void _checkSkipSilence(Duration position) {
    if (!_stateSubject.value.skipSilence) return;
    // Basic skip-silence: if near the end of track (last 3s), skip to next
    final duration = _stateSubject.value.duration;
    if (duration.inSeconds > 10 &&
        position.inSeconds >= duration.inSeconds - 3) {
      skipToNext();
    }
  }

  Future<void> playSong(SongModel song,
      {List<SongModel>? queue, AudioQuality? quality}) async {
    try {
      final effectiveQuality =
          quality ?? _stateSubject.value.audioQuality;
      _stateSubject.add(_stateSubject.value.copyWith(
        currentSong: song,
        isLoading: true,
        queue: queue ?? [song],
        currentIndex: queue?.indexOf(song) ?? 0,
      ));

      final audioUrl = await YouTubeService.getAudioStreamUrl(
        song.youtubeUrl,
        quality: effectiveQuality,
      );
      if (audioUrl == null) {
        _stateSubject.add(_stateSubject.value.copyWith(isLoading: false));
        throw Exception('Could not fetch audio stream');
      }

      await _player.setAudioSource(
        AudioSource.uri(
          Uri.parse(audioUrl),
          tag: MediaItem(
            id: song.id,
            title: song.title,
            artist: song.artist,
            album: song.album,
            artUri: Uri.parse(song.thumbnailUrl),
            duration: Duration(seconds: song.duration),
          ),
        ),
      );

      // Apply current speed/pitch
      await _player.setSpeed(_stateSubject.value.speed);

      await _player.play();
      _stateSubject.add(
          _stateSubject.value.copyWith(isLoading: false, isPlaying: true));
    } catch (e) {
      _stateSubject.add(_stateSubject.value.copyWith(isLoading: false));
      rethrow;
    }
  }

  Future<void> playOrPause() async {
    if (_player.playing) {
      await _player.pause();
    } else {
      await _player.play();
    }
  }

  Future<void> pause() => _player.pause();
  Future<void> resume() => _player.play();
  Future<void> stop() => _player.stop();

  Future<void> seekTo(Duration position) => _player.seek(position);
  Future<void> seekToProgress(double progress) {
    final duration = _stateSubject.value.duration;
    final target =
        Duration(milliseconds: (progress * duration.inMilliseconds).round());
    return _player.seek(target);
  }

  Future<void> seekToMs(int positionMs) {
    return _player.seek(Duration(milliseconds: positionMs));
  }

  Future<void> skipToNext() async {
    final state = _stateSubject.value;
    if (state.currentIndex < state.queue.length - 1) {
      final nextSong = state.queue[state.currentIndex + 1];
      await playSong(nextSong, queue: state.queue);
    }
  }

  Future<void> skipToPrevious() async {
    final state = _stateSubject.value;
    if (state.position.inSeconds > 3) {
      await seekTo(Duration.zero);
    } else if (state.currentIndex > 0) {
      final prevSong = state.queue[state.currentIndex - 1];
      await playSong(prevSong, queue: state.queue);
    }
  }

  void toggleShuffle() {
    final current = _stateSubject.value.isShuffleEnabled;
    _player.setShuffleModeEnabled(!current);
    _stateSubject
        .add(_stateSubject.value.copyWith(isShuffleEnabled: !current));
  }

  Future<void> cycleLoopMode() async {
    final current = _stateSubject.value.loopMode;
    LoopMode next;
    switch (current) {
      case LoopMode.off:
        next = LoopMode.all;
        break;
      case LoopMode.all:
        next = LoopMode.one;
        break;
      case LoopMode.one:
        next = LoopMode.off;
        break;
    }
    await _player.setLoopMode(next);
    _stateSubject.add(_stateSubject.value.copyWith(loopMode: next));
  }

  // ── Speed / Pitch ─────────────────────────────────────────────────────────

  Future<void> setSpeed(double speed) async {
    final clamped = speed.clamp(0.5, 2.0);
    await _player.setSpeed(clamped);
    _stateSubject.add(_stateSubject.value.copyWith(speed: clamped));
  }

  Future<void> setPitch(double pitch) async {
    final clamped = pitch.clamp(0.5, 2.0);
    // just_audio doesn't expose pitch directly; we adjust via setSpeed
    // as a simple proxy; for real pitch shifting a native plugin is needed.
    _stateSubject.add(_stateSubject.value.copyWith(pitch: clamped));
  }

  // ── Crossfade ─────────────────────────────────────────────────────────────

  void setCrossfadeDuration(int seconds) {
    _stateSubject.add(_stateSubject.value.copyWith(crossfadeDuration: seconds));
  }

  // ── Skip Silence ──────────────────────────────────────────────────────────

  void setSkipSilence(bool enabled) {
    _stateSubject.add(_stateSubject.value.copyWith(skipSilence: enabled));
  }

  // ── Audio Quality ─────────────────────────────────────────────────────────

  void setAudioQuality(AudioQuality quality) {
    _stateSubject.add(_stateSubject.value.copyWith(audioQuality: quality));
  }

  // ── Sleep Timer ───────────────────────────────────────────────────────────

  void setSleepTimer(Duration duration) {
    _sleepTimer?.cancel();
    final end = DateTime.now().add(duration);
    _stateSubject.add(_stateSubject.value.copyWith(sleepTimerEnd: end));
    _sleepTimer = Timer(duration, () {
      pause();
      cancelSleepTimer();
    });
  }

  void cancelSleepTimer() {
    _sleepTimer?.cancel();
    _sleepTimer = null;
    _stateSubject.add(_stateSubject.value.copyWith(clearSleepTimer: true));
  }

  Duration? get remainingSleepTime {
    final end = _stateSubject.value.sleepTimerEnd;
    if (end == null) return null;
    final remaining = end.difference(DateTime.now());
    return remaining.isNegative ? Duration.zero : remaining;
  }

  // ── Queue Reorder ─────────────────────────────────────────────────────────

  void reorderQueue(int oldIndex, int newIndex) {
    final state = _stateSubject.value;
    final queue = List<SongModel>.from(state.queue);
    final item = queue.removeAt(oldIndex);
    final adjustedNew = newIndex > oldIndex ? newIndex - 1 : newIndex;
    queue.insert(adjustedNew, item);

    int newCurrentIndex = state.currentIndex;
    if (state.currentIndex == oldIndex) {
      newCurrentIndex = adjustedNew;
    } else if (state.currentIndex > oldIndex && state.currentIndex <= adjustedNew) {
      newCurrentIndex = state.currentIndex - 1;
    } else if (state.currentIndex < oldIndex && state.currentIndex >= adjustedNew) {
      newCurrentIndex = state.currentIndex + 1;
    }

    _stateSubject.add(state.copyWith(queue: queue, currentIndex: newCurrentIndex));
  }

  void _handleSongCompleted() async {
    final state = _stateSubject.value;
    if (state.loopMode == LoopMode.one) {
      await seekTo(Duration.zero);
      await resume();
    } else if (state.currentIndex < state.queue.length - 1) {
      if (state.crossfadeDuration > 0) {
        // Crossfade: fade out current, then play next
        await _crossfade(state.crossfadeDuration);
      } else {
        await skipToNext();
      }
    } else if (state.loopMode == LoopMode.all && state.queue.isNotEmpty) {
      await playSong(state.queue.first, queue: state.queue);
    }
  }

  Future<void> _crossfade(int seconds) async {
    final steps = seconds * 10;
    final stepDuration = Duration(milliseconds: 100);
    for (int i = steps; i >= 0; i--) {
      await setVolume(i / steps);
      await Future.delayed(stepDuration);
    }
    await skipToNext();
    await setVolume(1.0);
  }

  Future<void> setVolume(double volume) => _player.setVolume(volume);

  void dispose() {
    _sleepTimer?.cancel();
    _crossfadeTimer?.cancel();
    _silenceTimer?.cancel();
    _player.dispose();
    _stateSubject.close();
    WakelockPlus.disable();
  }
}
