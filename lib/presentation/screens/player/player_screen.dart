// lib/presentation/screens/player/player_screen.dart
// v2.0 - SimpMusic-inspired overhaul with waveform visualizer, synced lyrics,
// sleep timer indicator, speed/pitch sheet, equalizer, queue reorder,
// and Listen Together button.

import 'dart:math';
import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:just_audio/just_audio.dart' hide PlayerState;
import 'package:palette_generator/palette_generator.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_strings.dart';
import '../../providers/player_provider.dart';
import '../../providers/music_provider.dart';
import '../../providers/settings_provider.dart';
import '../../../data/services/audio_player_service.dart';
import '../../../data/models/song_model.dart';
import '../../widgets/song_tile.dart';
import 'equalizer_sheet.dart';

class PlayerScreen extends ConsumerStatefulWidget {
  const PlayerScreen({super.key});

  @override
  ConsumerState<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends ConsumerState<PlayerScreen>
    with TickerProviderStateMixin {
  late AnimationController _vinylController;
  late AnimationController _glowController;
  late AnimationController _waveController;
  Color _dominantColor = AppColors.primary;
  Color _accentColor = AppColors.secondary;
  String? _lastThumbUrl;
  bool _showLyrics = false;

  // Waveform bars heights (10 bars, animated)
  final List<double> _waveHeights = List.filled(20, 0.3);
  final Random _rng = Random();

  @override
  void initState() {
    super.initState();
    _vinylController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 8),
    );
    _glowController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat(reverse: true);
    _waveController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
    )..addStatusListener((status) {
        if (status == AnimationStatus.completed) {
          _animateWave();
        }
      });
  }

  void _animateWave() {
    if (!mounted) return;
    final isPlaying = ref.read(isPlayingProvider);
    if (!isPlaying) return;
    setState(() {
      for (int i = 0; i < _waveHeights.length; i++) {
        _waveHeights[i] = 0.1 + _rng.nextDouble() * 0.9;
      }
    });
    _waveController.forward(from: 0);
  }

  Future<void> _extractColors(String imageUrl) async {
    if (_lastThumbUrl == imageUrl) return;
    _lastThumbUrl = imageUrl;
    try {
      final generator = await PaletteGenerator.fromImageProvider(
        CachedNetworkImageProvider(imageUrl),
        maximumColorCount: 6,
      );
      if (mounted) {
        setState(() {
          _dominantColor =
              generator.dominantColor?.color ?? AppColors.primary;
          _accentColor =
              generator.vibrantColor?.color ?? AppColors.secondary;
        });
      }
    } catch (_) {}
  }

  @override
  void dispose() {
    _vinylController.dispose();
    _glowController.dispose();
    _waveController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final playerState = ref.watch(playerStateProvider);
    final audioService = ref.read(audioPlayerServiceProvider);

    return playerState.when(
      data: (state) {
        final song = state.currentSong;
        if (song == null) {
          return Scaffold(
            backgroundColor: AppColors.background,
            body: const Center(child: Text('No song playing')),
          );
        }
        if (song.thumbnailUrl.isNotEmpty) {
          _extractColors(song.thumbnailUrl);
        }
        if (state.isPlaying && !_vinylController.isAnimating) {
          _vinylController.repeat();
          if (!_waveController.isAnimating) _animateWave();
        } else if (!state.isPlaying && _vinylController.isAnimating) {
          _vinylController.stop();
          _waveController.stop();
        }
        return _buildPlayer(context, state, song, audioService);
      },
      loading: () => const Scaffold(
        backgroundColor: AppColors.background,
        body: Center(child: CircularProgressIndicator()),
      ),
      error: (_, __) => const Scaffold(
        backgroundColor: AppColors.background,
        body: Center(child: Text('Error')),
      ),
    );
  }

  Widget _buildPlayer(BuildContext context, PlayerState state, SongModel song,
      AudioPlayerService audioService) {
    return Scaffold(
      body: Stack(
        fit: StackFit.expand,
        children: [
          // Background blurred album art
          CachedNetworkImage(
            imageUrl: song.thumbnailUrl,
            fit: BoxFit.cover,
            placeholder: (_, __) => Container(color: AppColors.background),
            errorWidget: (_, __, ___) =>
                Container(color: AppColors.background),
          ),
          BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 50, sigmaY: 50),
            child: Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    _dominantColor.withOpacity(0.4),
                    Colors.black.withOpacity(0.85),
                    Colors.black.withOpacity(0.95),
                  ],
                ),
              ),
            ),
          ),
          // Content
          SafeArea(
            child: Column(
              children: [
                _buildTopBar(context, song, state, audioService),
                const SizedBox(height: 12),
                if (!_showLyrics) ...[
                  _buildVinylRecord(song, state),
                  const SizedBox(height: 12),
                  _buildWaveformVisualizer(state),
                  const SizedBox(height: 20),
                ] else ...[
                  _buildLyricsView(song, state),
                  const SizedBox(height: 20),
                ],
                _buildSongInfo(song, state, audioService),
                const SizedBox(height: 16),
                _buildProgressBar(state, audioService),
                const SizedBox(height: 16),
                _buildControls(state, audioService),
                const SizedBox(height: 16),
                _buildBottomActions(song, state, audioService),
                const SizedBox(height: 8),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTopBar(BuildContext context, SongModel song, PlayerState state,
      AudioPlayerService audioService) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.keyboard_arrow_down_rounded,
                color: Colors.white, size: 32),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(
            child: Column(
              children: [
                const Text(
                  'NOW PLAYING',
                  style: TextStyle(
                    color: AppColors.textTertiary,
                    fontSize: 11,
                    letterSpacing: 2,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                Text(
                  song.album.isNotEmpty ? song.album : 'Single',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          // Sleep timer indicator
          if (state.hasSleepTimer)
            Padding(
              padding: const EdgeInsets.only(right: 4),
              child: GestureDetector(
                onTap: () => _showSleepTimerStatus(context, audioService),
                child: Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.orange.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                        color: Colors.orange.withOpacity(0.4), width: 1),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.bedtime_rounded,
                          color: Colors.orange, size: 14),
                      const SizedBox(width: 4),
                      Text(
                        _formatSleepRemaining(audioService),
                        style: const TextStyle(
                            color: Colors.orange, fontSize: 11),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          IconButton(
            icon: const Icon(Icons.more_vert_rounded,
                color: Colors.white, size: 24),
            onPressed: () => _showOptionsSheet(context, song),
          ),
        ],
      ),
    );
  }

  String _formatSleepRemaining(AudioPlayerService service) {
    final remaining = service.remainingSleepTime;
    if (remaining == null) return '';
    final m = remaining.inMinutes.remainder(60).toString().padLeft(2, '0');
    final s = remaining.inSeconds.remainder(60).toString().padLeft(2, '0');
    return '$m:$s';
  }

  Widget _buildWaveformVisualizer(PlayerState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32),
      child: SizedBox(
        height: 40,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: List.generate(_waveHeights.length, (i) {
            return AnimatedContainer(
              duration: const Duration(milliseconds: 300),
              width: 4,
              height: state.isPlaying ? (_waveHeights[i] * 36).clamp(4, 36) : 4,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.bottomCenter,
                  end: Alignment.topCenter,
                  colors: [
                    _dominantColor,
                    _accentColor,
                  ],
                ),
                borderRadius: BorderRadius.circular(2),
              ),
            );
          }),
        ),
      ),
    );
  }

  Widget _buildVinylRecord(SongModel song, PlayerState state) {
    return AnimatedBuilder(
      animation: _glowController,
      builder: (context, child) {
        return Container(
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: _dominantColor
                    .withOpacity(0.3 + _glowController.value * 0.3),
                blurRadius: 40 + _glowController.value * 20,
                spreadRadius: 5 + _glowController.value * 5,
              ),
            ],
          ),
          child: child,
        );
      },
      child: RotationTransition(
        turns: _vinylController,
        child: Stack(
          alignment: Alignment.center,
          children: [
            Container(
              width: 240,
              height: 240,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: const Color(0xFF1A1A1A),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.5),
                    blurRadius: 30,
                    spreadRadius: 5,
                  ),
                ],
              ),
            ),
            for (double size in [220, 180, 140, 100])
              Container(
                width: size,
                height: size,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: Colors.white.withOpacity(0.03),
                    width: 2,
                  ),
                ),
              ),
            ClipOval(
              child: CachedNetworkImage(
                imageUrl: song.thumbnailUrl,
                width: 140,
                height: 140,
                fit: BoxFit.cover,
                placeholder: (_, __) => Container(
                  color: AppColors.surfaceVariant,
                  child: const Icon(Icons.music_note,
                      color: AppColors.textTertiary, size: 50),
                ),
                errorWidget: (_, __, ___) => Container(
                  color: AppColors.surfaceVariant,
                  child: const Icon(Icons.music_note,
                      color: AppColors.textTertiary, size: 50),
                ),
              ),
            ),
            Container(
              width: 18,
              height: 18,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.white,
                boxShadow: [
                  BoxShadow(
                    color: _dominantColor.withOpacity(0.5),
                    blurRadius: 8,
                    spreadRadius: 2,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    ).animate().scale(
          begin: const Offset(0.8, 0.8),
          end: const Offset(1, 1),
          duration: 600.ms,
          curve: Curves.elasticOut,
        );
  }

  Widget _buildLyricsView(SongModel song, PlayerState state) {
    final lines = song.lyrics.split('\n').where((l) => l.trim().isNotEmpty).toList();
    // Simple synced lyrics: highlight line based on progress
    final progress = state.progress;
    final highlightIndex = (progress * lines.length).floor().clamp(0, lines.length - 1);

    return Expanded(
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 16),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.black.withOpacity(0.4),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: AppColors.glassBorder),
        ),
        child: lines.isEmpty
            ? const Center(
                child: Text(
                  'No lyrics available.',
                  style: TextStyle(
                      color: AppColors.textTertiary, fontSize: 15),
                  textAlign: TextAlign.center,
                ),
              )
            : ListView.builder(
                itemCount: lines.length,
                itemBuilder: (_, i) {
                  final isHighlighted = i == highlightIndex;
                  return AnimatedContainer(
                    duration: const Duration(milliseconds: 300),
                    padding: const EdgeInsets.symmetric(vertical: 6),
                    child: Text(
                      lines[i],
                      style: TextStyle(
                        color: isHighlighted
                            ? Colors.white
                            : Colors.white.withOpacity(0.4),
                        fontSize: isHighlighted ? 17 : 15,
                        fontWeight: isHighlighted
                            ? FontWeight.w700
                            : FontWeight.w400,
                        height: 1.5,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  );
                },
              ),
      ),
    );
  }

  Widget _buildSongInfo(SongModel song, PlayerState state,
      AudioPlayerService audioService) {
    final liked = ref.watch(likedSongIdsProvider).contains(song.id);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  song.title,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                    letterSpacing: -0.5,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Text(
                  song.artist,
                  style: const TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 14,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            icon: Icon(
              liked ? Icons.favorite_rounded : Icons.favorite_border_rounded,
              color: liked ? AppColors.secondary : AppColors.textSecondary,
              size: 26,
            ),
            onPressed: () {
              final set = Set<String>.from(ref.read(likedSongIdsProvider));
              if (liked) {
                set.remove(song.id);
              } else {
                set.add(song.id);
              }
              ref.read(likedSongIdsProvider.notifier).state = set;
            },
          ),
        ],
      ),
    );
  }

  Widget _buildProgressBar(PlayerState state, AudioPlayerService audioService) {
    final posStr = _formatDuration(state.position);
    final durStr = _formatDuration(state.duration);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          SliderTheme(
            data: SliderThemeData(
              thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
              overlayShape: SliderComponentShape.noOverlay,
              activeTrackColor: Colors.white,
              inactiveTrackColor: Colors.white24,
              thumbColor: Colors.white,
              trackHeight: 3,
            ),
            child: Slider(
              value: state.progress.clamp(0.0, 1.0),
              onChanged: (v) => audioService.seekToProgress(v),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(posStr,
                    style: const TextStyle(
                        color: AppColors.textSecondary, fontSize: 12)),
                // Speed indicator
                if (state.speed != 1.0)
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: AppColors.primary.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      '${state.speed.toStringAsFixed(1)}x',
                      style: const TextStyle(
                          color: AppColors.primary, fontSize: 11),
                    ),
                  ),
                Text(durStr,
                    style: const TextStyle(
                        color: AppColors.textSecondary, fontSize: 12)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildControls(PlayerState state, AudioPlayerService audioService) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          IconButton(
            icon: Icon(
              Icons.shuffle_rounded,
              color: state.isShuffleEnabled
                  ? AppColors.primary
                  : AppColors.textSecondary,
              size: 22,
            ),
            onPressed: audioService.toggleShuffle,
          ),
          IconButton(
            icon: const Icon(Icons.skip_previous_rounded,
                color: Colors.white, size: 34),
            onPressed: audioService.skipToPrevious,
          ),
          GestureDetector(
            onTap: audioService.playOrPause,
            child: AnimatedBuilder(
              animation: _glowController,
              builder: (context, child) => Container(
                width: 68,
                height: 68,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [_dominantColor, _accentColor],
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: _dominantColor.withOpacity(
                          0.4 + _glowController.value * 0.2),
                      blurRadius: 20 + _glowController.value * 10,
                      spreadRadius: 2,
                    ),
                  ],
                ),
                child: child,
              ),
              child: state.isLoading
                  ? const Padding(
                      padding: EdgeInsets.all(20),
                      child: CircularProgressIndicator(
                          color: Colors.white, strokeWidth: 2),
                    )
                  : Icon(
                      state.isPlaying
                          ? Icons.pause_rounded
                          : Icons.play_arrow_rounded,
                      color: Colors.white,
                      size: 34,
                    ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.skip_next_rounded,
                color: Colors.white, size: 34),
            onPressed: audioService.skipToNext,
          ),
          IconButton(
            icon: Icon(
              state.loopMode == LoopMode.one
                  ? Icons.repeat_one_rounded
                  : Icons.repeat_rounded,
              color: state.loopMode != LoopMode.off
                  ? AppColors.primary
                  : AppColors.textSecondary,
              size: 22,
            ),
            onPressed: audioService.cycleLoopMode,
          ),
        ],
      ),
    );
  }

  Widget _buildBottomActions(SongModel song, PlayerState state,
      AudioPlayerService audioService) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          _ActionButton(
            icon: Icons.queue_music_rounded,
            label: 'Queue',
            onTap: () => _showQueue(state, audioService),
          ),
          _ActionButton(
            icon: _showLyrics
                ? Icons.music_note_rounded
                : Icons.lyrics_outlined,
            label: 'Lyrics',
            isActive: _showLyrics,
            onTap: () => setState(() => _showLyrics = !_showLyrics),
          ),
          _ActionButton(
            icon: Icons.equalizer_rounded,
            label: 'EQ',
            onTap: () => _showEqualizerSheet(context),
          ),
          _ActionButton(
            icon: Icons.speed_rounded,
            label: 'Speed',
            onTap: () => _showSpeedSheet(context, state, audioService),
          ),
          _ActionButton(
            icon: Icons.people_alt_rounded,
            label: 'Together',
            onTap: () => context.push('/listen-together'),
          ),
        ],
      ),
    );
  }

  void _showEqualizerSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (_) => const EqualizerSheet(),
    );
  }

  void _showSpeedSheet(BuildContext context, PlayerState state,
      AudioPlayerService audioService) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => _SpeedPitchSheet(
        initialSpeed: state.speed,
        initialPitch: state.pitch,
        audioService: audioService,
      ),
    );
  }

  void _showSleepTimerStatus(BuildContext context, AudioPlayerService service) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.bedtime_rounded, color: Colors.orange, size: 40),
            const SizedBox(height: 12),
            const Text(
              'Sleep Timer Active',
              style: TextStyle(
                color: Colors.white,
                fontSize: 18,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Music will stop in ${_formatSleepRemaining(service)}',
              style: const TextStyle(
                  color: AppColors.textSecondary, fontSize: 14),
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () {
                  service.cancelSleepTimer();
                  Navigator.pop(context);
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.red.withOpacity(0.2),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12)),
                ),
                child: const Text('Cancel Timer',
                    style: TextStyle(color: Colors.red)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showOptionsSheet(BuildContext context, SongModel song) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.textTertiary,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 20),
            ListTile(
              leading: const Icon(Icons.playlist_add_rounded,
                  color: AppColors.primary),
              title: const Text('Add to Playlist',
                  style: TextStyle(color: Colors.white)),
              onTap: () => Navigator.pop(context),
            ),
            ListTile(
              leading: const Icon(Icons.settings_rounded,
                  color: AppColors.textSecondary),
              title: const Text('Settings',
                  style: TextStyle(color: Colors.white)),
              onTap: () {
                Navigator.pop(context);
                context.push('/settings');
              },
            ),
            ListTile(
              leading: const Icon(Icons.info_outline_rounded,
                  color: AppColors.textSecondary),
              title: const Text('Song Info',
                  style: TextStyle(color: Colors.white)),
              subtitle: Text(
                '${song.category} • ${song.year}',
                style: const TextStyle(color: AppColors.textTertiary),
              ),
              onTap: () => Navigator.pop(context),
            ),
          ],
        ),
      ),
    );
  }

  void _showQueue(PlayerState state, AudioPlayerService audioService) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surface,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => DraggableScrollableSheet(
        initialChildSize: 0.6,
        maxChildSize: 0.9,
        minChildSize: 0.4,
        expand: false,
        builder: (_, controller) => Column(
          children: [
            Container(
              margin: const EdgeInsets.only(top: 12),
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.textTertiary,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text('Queue',
                  style: TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.w700)),
            ),
            Expanded(
              child: ReorderableListView.builder(
                scrollController: controller,
                itemCount: state.queue.length,
                onReorder: (oldIndex, newIndex) {
                  audioService.reorderQueue(oldIndex, newIndex);
                },
                itemBuilder: (_, i) {
                  final qSong = state.queue[i];
                  final isCurrent = i == state.currentIndex;
                  return ListTile(
                    key: ValueKey(qSong.id + i.toString()),
                    leading: ClipRRect(
                      borderRadius: BorderRadius.circular(8),
                      child: CachedNetworkImage(
                        imageUrl: qSong.thumbnailUrl,
                        width: 44,
                        height: 44,
                        fit: BoxFit.cover,
                        errorWidget: (_, __, ___) => Container(
                          width: 44,
                          height: 44,
                          color: AppColors.surfaceVariant,
                          child: const Icon(Icons.music_note,
                              color: AppColors.textTertiary, size: 20),
                        ),
                      ),
                    ),
                    title: Text(
                      qSong.title,
                      style: TextStyle(
                        color: isCurrent ? AppColors.primary : Colors.white,
                        fontSize: 14,
                        fontWeight: isCurrent
                            ? FontWeight.w700
                            : FontWeight.w400,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    subtitle: Text(
                      qSong.artist,
                      style: const TextStyle(
                          color: AppColors.textTertiary, fontSize: 12),
                    ),
                    trailing: const Icon(Icons.drag_handle_rounded,
                        color: AppColors.textTertiary),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatDuration(Duration d) {
    final m = d.inMinutes.remainder(60).toString().padLeft(2, '0');
    final s = d.inSeconds.remainder(60).toString().padLeft(2, '0');
    return '$m:$s';
  }
}

// ── Speed / Pitch bottom sheet ────────────────────────────────────────────────
class _SpeedPitchSheet extends StatefulWidget {
  final double initialSpeed;
  final double initialPitch;
  final AudioPlayerService audioService;

  const _SpeedPitchSheet({
    required this.initialSpeed,
    required this.initialPitch,
    required this.audioService,
  });

  @override
  State<_SpeedPitchSheet> createState() => _SpeedPitchSheetState();
}

class _SpeedPitchSheetState extends State<_SpeedPitchSheet> {
  late double _speed;
  late double _pitch;

  @override
  void initState() {
    super.initState();
    _speed = widget.initialSpeed;
    _pitch = widget.initialPitch;
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 36,
            height: 4,
            decoration: BoxDecoration(
              color: AppColors.textTertiary,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 20),
          const Text(
            'Speed & Pitch',
            style: TextStyle(
              color: Colors.white,
              fontSize: 20,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 24),
          // Speed
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Playback Speed',
                  style: TextStyle(color: Colors.white, fontSize: 15)),
              Text('${_speed.toStringAsFixed(2)}x',
                  style: const TextStyle(
                      color: AppColors.primary, fontSize: 14)),
            ],
          ),
          Slider(
            value: _speed,
            min: 0.5,
            max: 2.0,
            divisions: 15,
            activeColor: AppColors.primary,
            inactiveColor: AppColors.surfaceVariant,
            onChanged: (v) async {
              setState(() => _speed = v);
              await widget.audioService.setSpeed(v);
            },
          ),
          // Preset speeds
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [0.5, 0.75, 1.0, 1.25, 1.5, 2.0].map((s) {
              final isSelected = (_speed - s).abs() < 0.01;
              return GestureDetector(
                onTap: () async {
                  setState(() => _speed = s);
                  await widget.audioService.setSpeed(s);
                },
                child: Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: isSelected
                        ? AppColors.primary.withOpacity(0.2)
                        : AppColors.surfaceVariant,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: isSelected
                          ? AppColors.primary
                          : Colors.transparent,
                      width: 1.5,
                    ),
                  ),
                  child: Text(
                    '${s}x',
                    style: TextStyle(
                      color: isSelected
                          ? AppColors.primary
                          : AppColors.textSecondary,
                      fontSize: 12,
                      fontWeight: isSelected
                          ? FontWeight.w600
                          : FontWeight.w400,
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 20),
          // Pitch
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Pitch',
                  style: TextStyle(color: Colors.white, fontSize: 15)),
              Text('${_pitch.toStringAsFixed(2)}',
                  style: const TextStyle(
                      color: AppColors.secondary, fontSize: 14)),
            ],
          ),
          Slider(
            value: _pitch,
            min: 0.5,
            max: 2.0,
            divisions: 15,
            activeColor: AppColors.secondary,
            inactiveColor: AppColors.surfaceVariant,
            onChanged: (v) async {
              setState(() => _pitch = v);
              await widget.audioService.setPitch(v);
            },
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: TextButton(
              onPressed: () async {
                setState(() {
                  _speed = 1.0;
                  _pitch = 1.0;
                });
                await widget.audioService.setSpeed(1.0);
                await widget.audioService.setPitch(1.0);
              },
              child: const Text('Reset to Default',
                  style: TextStyle(color: AppColors.textSecondary)),
            ),
          ),
          const SizedBox(height: 8),
        ],
      ),
    );
  }
}

class _ActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool isActive;

  const _ActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
    this.isActive = false,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          Icon(
            icon,
            color: isActive ? AppColors.primary : AppColors.textSecondary,
            size: 22,
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              color: isActive ? AppColors.primary : AppColors.textSecondary,
              fontSize: 10,
            ),
          ),
        ],
      ),
    );
  }
}
