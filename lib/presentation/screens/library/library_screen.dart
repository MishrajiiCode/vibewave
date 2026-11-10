// lib/presentation/screens/library/library_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../providers/music_provider.dart';
import '../../widgets/song_tile.dart';
import '../../../data/services/history_service.dart';
import '../../../data/models/song_model.dart';

// History provider
final historyServiceProvider = Provider<HistoryService>((ref) {
  final service = HistoryService();
  service.init();
  return service;
});

final historyProvider = Provider<List<SongModel>>((ref) {
  final service = ref.watch(historyServiceProvider);
  return service.getHistory();
});

class LibraryScreen extends ConsumerWidget {
  const LibraryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final likedIds = ref.watch(likedSongIdsProvider);
    final allSongs = ref.watch(allSongsProvider);
    final history = ref.watch(historyProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Your Library',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 28,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    Row(
                      children: [
                        IconButton(
                          icon: const Icon(Icons.settings_outlined,
                              color: AppColors.textSecondary),
                          onPressed: () => context.push('/settings'),
                        ),
                        Container(
                          width: 40,
                          height: 40,
                          decoration: BoxDecoration(
                            color: AppColors.primary,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Icon(Icons.add_rounded,
                              color: Colors.white),
                        ),
                      ],
                    ),
                  ],
                ),
              ).animate().fadeIn(duration: 400.ms),
            ),

            // Library cards row
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Row(
                  children: [
                    Expanded(
                      child: _LibraryCard(
                        icon: Icons.favorite_rounded,
                        title: 'Liked Songs',
                        subtitle: '${likedIds.length} songs',
                        gradient: const LinearGradient(
                          colors: [Color(0xFF6C63FF), Color(0xFFFF6B9D)],
                        ),
                        onTap: () {},
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _LibraryCard(
                        icon: Icons.people_alt_rounded,
                        title: 'Listen Together',
                        subtitle: 'Listen with friends',
                        gradient: const LinearGradient(
                          colors: [Color(0xFF00D9FF), Color(0xFF6C63FF)],
                        ),
                        onTap: () => context.push('/listen-together'),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SliverToBoxAdapter(child: SizedBox(height: 24)),

            // History Section
            if (history.isNotEmpty) ...[
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 0, 20, 12),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Recently Played',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 20,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      TextButton(
                        onPressed: () {},
                        child: const Text('See all',
                            style: TextStyle(
                                color: AppColors.primary, fontSize: 13)),
                      ),
                    ],
                  ),
                ),
              ),
              SliverToBoxAdapter(
                child: SizedBox(
                  height: 150,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    itemCount: history.take(10).length,
                    itemBuilder: (ctx, i) => Padding(
                      padding: const EdgeInsets.only(right: 12),
                      child: SongCard(
                          song: history[i], queue: history, width: 110),
                    ),
                  ),
                ),
              ),
              const SliverToBoxAdapter(child: SizedBox(height: 24)),
            ],

            // Liked songs section
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 12),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Liked Songs',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 20,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    Text(
                      '${likedIds.length} songs',
                      style: const TextStyle(
                          color: AppColors.textTertiary, fontSize: 13),
                    ),
                  ],
                ),
              ),
            ),

            SliverToBoxAdapter(
              child: allSongs.when(
                data: (songs) {
                  final liked =
                      songs.where((s) => likedIds.contains(s.id)).toList();
                  if (liked.isEmpty) {
                    return Padding(
                      padding: const EdgeInsets.all(40),
                      child: Column(
                        children: [
                          const Icon(Icons.favorite_border_rounded,
                              color: AppColors.textTertiary, size: 60),
                          const SizedBox(height: 16),
                          const Text(
                            'No liked songs yet',
                            style: TextStyle(
                                color: AppColors.textSecondary, fontSize: 16),
                          ),
                          const SizedBox(height: 8),
                          const Text(
                            'Tap the heart icon on any song\nto add it here',
                            style: TextStyle(
                                color: AppColors.textTertiary, fontSize: 13),
                            textAlign: TextAlign.center,
                          ),
                        ],
                      ),
                    );
                  }
                  return ListView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: liked.length,
                    itemBuilder: (_, i) =>
                        SongTile(song: liked[i], queue: liked),
                  );
                },
                loading: () =>
                    const Center(child: CircularProgressIndicator()),
                error: (_, __) => const SizedBox(),
              ),
            ),

            const SliverToBoxAdapter(child: SizedBox(height: 160)),
          ],
        ),
      ),
    );
  }
}

class _LibraryCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final Gradient gradient;
  final VoidCallback onTap;

  const _LibraryCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.gradient,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 100,
        decoration: BoxDecoration(
          gradient: gradient,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Stack(
          children: [
            Positioned(
              right: -8,
              bottom: -8,
              child: Icon(icon,
                  size: 70, color: Colors.white.withOpacity(0.2)),
            ),
            Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  Icon(icon, color: Colors.white, size: 24),
                  const SizedBox(height: 8),
                  Text(
                    title,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Text(
                    subtitle,
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.7),
                      fontSize: 11,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
