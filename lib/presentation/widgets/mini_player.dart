// lib/presentation/widgets/mini_player.dart
import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/constants/app_colors.dart';
import '../providers/player_provider.dart';
import '../../data/services/audio_player_service.dart';

class MiniPlayer extends ConsumerWidget {
  const MiniPlayer({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final playerState = ref.watch(playerStateProvider);

    return playerState.when(
      data: (state) {
        if (state.currentSong == null) return const SizedBox.shrink();
        return _MiniPlayerContent(state: state);
      },
      loading: () => const SizedBox.shrink(),
      error: (_, __) => const SizedBox.shrink(),
    );
  }
}

class _MiniPlayerContent extends ConsumerStatefulWidget {
  final PlayerState state;
  const _MiniPlayerContent({required this.state});

  @override
  ConsumerState<_MiniPlayerContent> createState() => _MiniPlayerContentState();
}

class _MiniPlayerContentState extends ConsumerState<_MiniPlayerContent>
    with SingleTickerProviderStateMixin {
  late AnimationController _rotationController;

  @override
  void initState() {
    super.initState();
    _rotationController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 10),
    );
    if (widget.state.isPlaying) _rotationController.repeat();
  }

  @override
  void didUpdateWidget(_MiniPlayerContent oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.state.isPlaying && !_rotationController.isAnimating) {
      _rotationController.repeat();
    } else if (!widget.state.isPlaying && _rotationController.isAnimating) {
      _rotationController.stop();
    }
  }

  @override
  void dispose() {
    _rotationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final song = widget.state.currentSong!;
    final audioService = ref.read(audioPlayerServiceProvider);

    return GestureDetector(
      onTap: () => context.push('/player'),
      child: ClipRRect(
        borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
          child: Container(
            height: 72,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  AppColors.primary.withOpacity(0.25),
                  AppColors.surface.withOpacity(0.9),
                ],
              ),
              border: const Border(
                top: BorderSide(color: AppColors.glassBorder, width: 1),
              ),
            ),
            child: Column(
              children: [
                // Progress indicator
                LinearProgressIndicator(
                  value: widget.state.progress,
                  backgroundColor: Colors.transparent,
                  valueColor:
                      const AlwaysStoppedAnimation<Color>(AppColors.primary),
                  minHeight: 2,
                ),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: Row(
                      children: [
                        // Rotating thumbnail
                        RotationTransition(
                          turns: _rotationController,
                          child: Container(
                            width: 44,
                            height: 44,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              border: Border.all(
                                  color: AppColors.primary, width: 2),
                            ),
                            child: ClipOval(
                              child: CachedNetworkImage(
                                imageUrl: song.thumbnailUrl,
                                fit: BoxFit.cover,
                                placeholder: (_, __) => Container(
                                    color: AppColors.surfaceVariant,
                                    child: const Icon(Icons.music_note,
                                        color: AppColors.textTertiary)),
                                errorWidget: (_, __, ___) => Container(
                                    color: AppColors.surfaceVariant,
                                    child: const Icon(Icons.music_note,
                                        color: AppColors.textTertiary)),
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 12),
                        // Song info
                        Expanded(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                song.title,
                                style: const TextStyle(
                                  color: AppColors.textPrimary,
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 2),
                              Text(
                                song.artist,
                                style: const TextStyle(
                                  color: AppColors.textSecondary,
                                  fontSize: 11,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                        // Controls
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            IconButton(
                              icon: const Icon(Icons.skip_previous_rounded,
                                  color: AppColors.textSecondary, size: 26),
                              onPressed: audioService.skipToPrevious,
                            ),
                            GestureDetector(
                              onTap: audioService.playOrPause,
                              child: Container(
                                width: 40,
                                height: 40,
                                decoration: const BoxDecoration(
                                  color: AppColors.primary,
                                  shape: BoxShape.circle,
                                ),
                                child: widget.state.isLoading
                                    ? const Padding(
                                        padding: EdgeInsets.all(10),
                                        child: CircularProgressIndicator(
                                          color: Colors.white,
                                          strokeWidth: 2,
                                        ),
                                      )
                                    : Icon(
                                        widget.state.isPlaying
                                            ? Icons.pause_rounded
                                            : Icons.play_arrow_rounded,
                                        color: Colors.white,
                                        size: 22,
                                      ),
                              ),
                            ).animate(target: widget.state.isPlaying ? 1 : 0).scaleXY(begin: 0.95, end: 1.0),
                            IconButton(
                              icon: const Icon(Icons.skip_next_rounded,
                                  color: AppColors.textSecondary, size: 26),
                              onPressed: audioService.skipToNext,
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
