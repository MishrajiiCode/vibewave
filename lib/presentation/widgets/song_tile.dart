// lib/presentation/widgets/song_tile.dart
import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/constants/app_colors.dart';
import '../../data/models/song_model.dart';
import '../providers/player_provider.dart';
import '../providers/music_provider.dart';

class SongTile extends ConsumerWidget {
  final SongModel song;
  final List<SongModel>? queue;
  final VoidCallback? onTap;
  final bool showMenu;
  final bool isCompact;

  const SongTile({
    super.key,
    required this.song,
    this.queue,
    this.onTap,
    this.showMenu = true,
    this.isCompact = false,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final playerState = ref.watch(playerStateProvider);
    final isCurrentSong = playerState.whenOrNull(
          data: (s) => s.currentSong?.id == song.id,
        ) ??
        false;
    final isPlaying = playerState.whenOrNull(data: (s) => s.isPlaying) ?? false;

    return InkWell(
      onTap: onTap ??
          () {
            ref
                .read(audioPlayerServiceProvider)
                .playSong(song, queue: queue ?? [song]);
          },
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: EdgeInsets.symmetric(
          horizontal: 12,
          vertical: isCompact ? 8 : 10,
        ),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: isCurrentSong
              ? AppColors.primary.withOpacity(0.12)
              : Colors.transparent,
        ),
        child: Row(
          children: [
            // Thumbnail
            ClipRRect(
              borderRadius: BorderRadius.circular(10),
              child: Stack(
                alignment: Alignment.center,
                children: [
                  CachedNetworkImage(
                    imageUrl: song.thumbnailUrl,
                    width: isCompact ? 44 : 56,
                    height: isCompact ? 44 : 56,
                    fit: BoxFit.cover,
                    placeholder: (context, url) => Container(
                      color: AppColors.surfaceVariant,
                      child: const Icon(Icons.music_note,
                          color: AppColors.textTertiary, size: 24),
                    ),
                    errorWidget: (context, url, error) => Container(
                      color: AppColors.surfaceVariant,
                      child: const Icon(Icons.music_note,
                          color: AppColors.textTertiary, size: 24),
                    ),
                  ),
                  if (isCurrentSong)
                    Container(
                      width: isCompact ? 44 : 56,
                      height: isCompact ? 44 : 56,
                      color: Colors.black45,
                      child: Icon(
                        isPlaying ? Icons.pause : Icons.play_arrow,
                        color: AppColors.primary,
                        size: 24,
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            // Song info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    song.title,
                    style: TextStyle(
                      color: isCurrentSong
                          ? AppColors.primary
                          : AppColors.textPrimary,
                      fontSize: isCompact ? 13 : 14,
                      fontWeight: FontWeight.w600,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 3),
                  Text(
                    song.artist,
                    style: TextStyle(
                      color: AppColors.textSecondary,
                      fontSize: isCompact ? 11 : 12,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            // Duration
            if (!isCompact)
              Text(
                song.formattedDuration,
                style: const TextStyle(
                  color: AppColors.textTertiary,
                  fontSize: 11,
                ),
              ),
            if (showMenu) ...[
              const SizedBox(width: 8),
              _SongMenu(song: song),
            ],
          ],
        ),
      ),
    );
  }
}

class _SongMenu extends ConsumerWidget {
  final SongModel song;
  const _SongMenu({required this.song});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final liked = ref.watch(likedSongIdsProvider).contains(song.id);
    return IconButton(
      icon: Icon(
        liked ? Icons.favorite : Icons.favorite_border,
        size: 20,
        color: liked ? AppColors.secondary : AppColors.textTertiary,
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
    );
  }
}

// Horizontal card variant for featured songs
class SongCard extends ConsumerWidget {
  final SongModel song;
  final List<SongModel>? queue;
  final double width;

  const SongCard({
    super.key,
    required this.song,
    this.queue,
    this.width = 140,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return GestureDetector(
      onTap: () {
        ref
            .read(audioPlayerServiceProvider)
            .playSong(song, queue: queue ?? [song]);
      },
      child: SizedBox(
        width: width,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(14),
              child: Stack(
                children: [
                  CachedNetworkImage(
                    imageUrl: song.thumbnailUrl,
                    width: width,
                    height: width,
                    fit: BoxFit.cover,
                    placeholder: (context, url) => Container(
                      color: AppColors.surfaceVariant,
                      child: const Icon(Icons.music_note,
                          color: AppColors.textTertiary, size: 40),
                    ),
                    errorWidget: (context, url, error) => Container(
                      color: AppColors.surfaceVariant,
                      child: const Icon(Icons.music_note,
                          color: AppColors.textTertiary, size: 40),
                    ),
                  ),
                  Positioned(
                    bottom: 8,
                    right: 8,
                    child: Container(
                      width: 36,
                      height: 36,
                      decoration: const BoxDecoration(
                        color: AppColors.primary,
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.play_arrow,
                          color: Colors.white, size: 20),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 8),
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
    );
  }
}
