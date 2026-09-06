// lib/data/services/audio_player_service.dart
import 'package:just_audio/just_audio.dart';
import 'package:just_audio_background/just_audio_background.dart';
import 'package:rxdart/rxdart.dart';
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
    );
  }

  double get progress {
    if (duration.inMilliseconds == 0) return 0.0;
    return position.inMilliseconds / duration.inMilliseconds;
  }
}

class AudioPlayerService {
  final AudioPlayer _player = AudioPlayer();
  final BehaviorSubject<PlayerState> _stateSubject =
      BehaviorSubject.seeded(const PlayerState());

  AudioPlayerService() {
    _initListeners();
  }

  Stream<PlayerState> get stateStream => _stateSubject.stream;
  PlayerState get currentState => _stateSubject.value;

  void _initListeners() {
    // Position updates
    _player.positionStream.listen((position) {
      _stateSubject.add(_stateSubject.value.copyWith(position: position));
    });

    // Duration updates
    _player.durationStream.listen((duration) {
      _stateSubject.add(
          _stateSubject.value.copyWith(duration: duration ?? Duration.zero));
    });

    // Playing state
    _player.playingStream.listen((isPlaying) {
      _stateSubject.add(_stateSubject.value.copyWith(isPlaying: isPlaying));
    });

    // Process state (loading etc.)
    _player.processingStateStream.listen((state) {
      if (state == ProcessingState.completed) {
        _handleSongCompleted();
      }
    });
  }

  Future<void> playSong(SongModel song, {List<SongModel>? queue}) async {
    try {
      _stateSubject.add(_stateSubject.value.copyWith(
        currentSong: song,
        isLoading: true,
        queue: queue ?? [song],
        currentIndex: queue?.indexOf(song) ?? 0,
      ));

      final audioUrl = await YouTubeService.getAudioStreamUrl(song.youtubeUrl);
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

      await _player.play();
      _stateSubject
          .add(_stateSubject.value.copyWith(isLoading: false, isPlaying: true));
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
    final target = Duration(
        milliseconds: (progress * duration.inMilliseconds).round());
    return _player.seek(target);
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

  void _handleSongCompleted() async {
    final state = _stateSubject.value;
    if (state.loopMode == LoopMode.one) {
      await seekTo(Duration.zero);
      await resume();
    } else if (state.currentIndex < state.queue.length - 1) {
      await skipToNext();
    } else if (state.loopMode == LoopMode.all && state.queue.isNotEmpty) {
      await playSong(state.queue.first, queue: state.queue);
    }
  }

  Future<void> setVolume(double volume) => _player.setVolume(volume);

  void dispose() {
    _player.dispose();
    _stateSubject.close();
  }
}
