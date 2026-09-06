// lib/presentation/screens/player/player_screen.dart
import 'dart:math';
import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import 'package:palette_generator/palette_generator.dart';
import '../../../core/constants/app_colors.dart';
import '../../providers/player_provider.dart';
import '../../providers/music_provider.dart';
import '../../../data/services/audio_player_service.dart';
import '../../../data/models/song_model.dart';

class PlayerScreen extends ConsumerStatefulWidget {
  const PlayerScreen({super.key});

  @override
  ConsumerState<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends ConsumerState<PlayerScreen>
    with TickerProviderStateMixin {
  late AnimationController _vinylController;
  late AnimationController _glowController;
  Color _dominantColor = AppColors.primary;
  Color _accentColor = AppColors.secondary;
  String? _lastThumbUrl;
  bool _showLyrics = false;

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
        // Extract colors from thumbnail
        if (song.thumbnailUrl.isNotEmpty) {
          _extractColors(song.thumbnailUrl);
        }
        // Vinyl spin control
        if (state.isPlaying && !_vinylController.isAnimating) {
          _vinylController.repeat();
        } else if (!state.isPlaying && _vinylController.isAnimating) {
          _vinylController.stop();
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
            errorWidget: (_, __, ___) => Container(color: AppColors.background),
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
                const SizedBox(height: 20),
                if (!_showLyrics) ...[
                  _buildVinylRecord(song, state),
                  const SizedBox(height: 32),
                ] else ...[
                  _buildLyricsView(song),
                ],
                _buildSongInfo(song, state, audioService),
                const SizedBox(height: 20),
                _buildProgressBar(state, audioService),
                const SizedBox(height: 20),
                _buildControls(state, audioService),
                const SizedBox(height: 20),
                _buildBottomActions(song, state, audioService),
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
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
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
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
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

  Widget _buildVinylRecord(SongModel song, PlayerState state) {
    return AnimatedBuilder(
      animation: _glowController,
      builder: (context, child) {
        return Container(
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: _dominantColor.withOpacity(0.3 + _glowController.value * 0.3),
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
            // Vinyl disc background
            Container(
              width: 260,
              height: 260,
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
            // Vinyl rings
            for (double size in [240, 200, 160, 120])
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
            // Album art circle
            ClipOval(
              child: CachedNetworkImage(
                imageUrl: song.thumbnailUrl,
                width: 160,
                height: 160,
                fit: BoxFit.cover,
                placeholder: (_, __) => Container(
                  color: AppColors.surfaceVariant,
                  child: const Icon(Icons.music_note, color: AppColors.textTertiary, size: 60),
                ),
                errorWidget: (_, __, ___) => Container(
                  color: AppColors.surfaceVariant,
                  child: const Icon(Icons.music_note, color: AppColors.textTertiary, size: 60),
                ),
              ),
            ),
            // Center dot
            Container(
              width: 20,
              height: 20,
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

  Widget _buildLyricsView(SongModel song) {
    return Expanded(
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 20),
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: Colors.black.withOpacity(0.4),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: AppColors.glassBorder),
        ),
        child: SingleChildScrollView(
          child: Text(
            song.lyrics.isNotEmpty
                ? song.lyrics
                : 'No lyrics available for this song.',
            style: const TextStyle(
              color: Colors.white,
              fontSize: 16,
              height: 1.8,
              fontWeight: FontWeight.w400,
            ),
            textAlign: TextAlign.center,
          ),
        ),
      ),
    );
  }

  Widget _buildSongInfo(SongModel song, PlayerState state, AudioPlayerService audioService) {
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
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                    letterSpacing: -0.5,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Text(
                  song.artist,
                  style: const TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 15,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            icon: Icon(
              liked ? Icons.favorite_rounded : Icons.favorite_border_rounded,
              color: liked ? AppColors.secondary : AppColors.textSecondary,
              size: 28,
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
              thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 7),
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
              size: 24,
            ),
            onPressed: audioService.toggleShuffle,
          ),
          IconButton(
            icon: const Icon(Icons.skip_previous_rounded,
                color: Colors.white, size: 36),
            onPressed: audioService.skipToPrevious,
          ),
          // Play/Pause button
          GestureDetector(
            onTap: audioService.playOrPause,
            child: AnimatedBuilder(
              animation: _glowController,
              builder: (context, child) => Container(
                width: 70,
                height: 70,
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
                      size: 36,
                    ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.skip_next_rounded,
                color: Colors.white, size: 36),
            onPressed: audioService.skipToNext,
          ),
          IconButton(
            icon: Icon(
              state.loopMode == LoopMode.off
                  ? Icons.repeat_rounded
                  : state.loopMode == LoopMode.one
                      ? Icons.repeat_one_rounded
                      : Icons.repeat_rounded,
              color: state.loopMode != LoopMode.off
                  ? AppColors.primary
                  : AppColors.textSecondary,
              size: 24,
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
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          _ActionButton(
            icon: Icons.queue_music_rounded,
            label: 'Queue',
            onTap: () => _showQueue(state),
          ),
          _ActionButton(
            icon: _showLyrics ? Icons.music_note_rounded : Icons.lyrics_outlined,
            label: 'Lyrics',
            isActive: _showLyrics,
            onTap: () => setState(() => _showLyrics = !_showLyrics),
          ),
          _ActionButton(
            icon: Icons.share_outlined,
            label: 'Share',
            onTap: () {},
          ),
        ],
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
              leading: const Icon(Icons.playlist_add_rounded, color: AppColors.primary),
              title: const Text('Add to Playlist', style: TextStyle(color: Colors.white)),
              onTap: () => Navigator.pop(context),
            ),
            ListTile(
              leading: const Icon(Icons.info_outline_rounded, color: AppColors.textSecondary),
              title: const Text('Song Info', style: TextStyle(color: Colors.white)),
              subtitle: Text('${song.category} • ${song.year}',
                  style: const TextStyle(color: AppColors.textTertiary)),
              onTap: () => Navigator.pop(context),
            ),
          ],
        ),
      ),
    );
  }

  void _showQueue(PlayerState state) {
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
                      color: Colors.white, fontSize: 18, fontWeight: FontWeight.w700)),
            ),
            Expanded(
              child: ListView.builder(
                controller: controller,
                itemCount: state.queue.length,
                itemBuilder: (_, i) {
                  final song = state.queue[i];
                  return SongTile(
                    song: song,
                    queue: state.queue,
                    showMenu: false,
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
            size: 24,
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              color: isActive ? AppColors.primary : AppColors.textSecondary,
              fontSize: 11,
            ),
          ),
        ],
      ),
    );
  }
}
