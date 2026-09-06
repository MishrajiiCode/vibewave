// lib/presentation/screens/home/home_screen.dart
import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shimmer/shimmer.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_strings.dart';
import '../../providers/music_provider.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/song_tile.dart';
import '../../widgets/glassmorphic_card.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  int _featuredIndex = 0;
  late PageController _pageController;

  @override
  void initState() {
    super.initState();
    _pageController = PageController(viewportFraction: 0.88);
    _startAutoScroll();
  }

  void _startAutoScroll() async {
    while (mounted) {
      await Future.delayed(const Duration(seconds: 4));
      if (!mounted) break;
      final featured = ref.read(featuredSongsProvider);
      final count = featured.whenOrNull(data: (list) => list.length) ?? 0;
      if (count > 1 && _pageController.hasClients) {
        _featuredIndex = (_featuredIndex + 1) % count;
        _pageController.animateToPage(
          _featuredIndex,
          duration: const Duration(milliseconds: 600),
          curve: Curves.easeInOut,
        );
      }
    }
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final featured = ref.watch(featuredSongsProvider);
    final trending = ref.watch(trendingSongsProvider);
    final categories = ref.watch(categoriesProvider);
    final newReleases = ref.watch(newReleasesProvider);
    final isAdmin = ref.watch(isAdminProvider);
    final hour = DateTime.now().hour;
    final greeting = hour < 12 ? 'Good Morning' : hour < 17 ? 'Good Afternoon' : 'Good Evening';

    return Scaffold(
      backgroundColor: AppColors.background,
      body: CustomScrollView(
        slivers: [
          // AppBar
          SliverAppBar(
            expandedHeight: 0,
            floating: true,
            pinned: false,
            backgroundColor: AppColors.background,
            flexibleSpace: FlexibleSpaceBar(
              background: Container(color: AppColors.background),
            ),
            title: Row(
              children: [
                Container(
                  width: 36,
                  height: 36,
                  decoration: const BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: LinearGradient(
                      colors: [AppColors.primary, AppColors.secondary],
                    ),
                  ),
                  child: const Icon(Icons.waves_rounded, color: Colors.white, size: 20),
                ),
                const SizedBox(width: 10),
                const Text(
                  'VibeWave',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.5,
                  ),
                ),
              ],
            ),
            actions: [
              if (isAdmin)
                IconButton(
                  icon: const Icon(Icons.admin_panel_settings_outlined,
                      color: AppColors.primary),
                  onPressed: () => context.go('/admin/dashboard'),
                ),
              IconButton(
                icon: Container(
                  width: 36,
                  height: 36,
                  decoration: BoxDecoration(
                    color: AppColors.surfaceVariant,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.person_outline_rounded,
                      color: AppColors.textSecondary, size: 20),
                ),
                onPressed: () => context.go('/admin/login'),
              ),
              const SizedBox(width: 8),
            ],
          ),

          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    greeting,
                    style: const TextStyle(
                      color: AppColors.textSecondary,
                      fontSize: 14,
                      letterSpacing: 0.5,
                    ),
                  ),
                  const Text(
                    'What would you like\nto listen to?',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 26,
                      fontWeight: FontWeight.w800,
                      height: 1.2,
                    ),
                  ),
                ],
              ),
            ),
          ).animate().fadeIn(duration: 400.ms).slideY(begin: -0.2, end: 0),

          // Categories chips
          SliverToBoxAdapter(
            child: SizedBox(
              height: 50,
              child: categories.when(
                data: (cats) => ListView.builder(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  itemCount: cats.length,
                  itemBuilder: (context, i) {
                    final cat = cats[i];
                    return GestureDetector(
                      onTap: () {
                        ref.read(selectedCategoryProvider.notifier).state = cat.name;
                        context.go('/categories');
                      },
                      child: Container(
                        margin: const EdgeInsets.only(right: 8),
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [
                              cat.displayColor.withOpacity(0.3),
                              cat.displayColor.withOpacity(0.15),
                            ],
                          ),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(
                              color: cat.displayColor.withOpacity(0.5), width: 1),
                        ),
                        child: Text(
                          cat.name,
                          style: TextStyle(
                            color: cat.displayColor,
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    );
                  },
                ),
                loading: () => _shimmerRow(height: 34, count: 5, width: 80),
                error: (_, __) => const SizedBox(),
              ),
            ),
          ),

          // Featured banner
          SliverToBoxAdapter(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Padding(
                  padding: EdgeInsets.fromLTRB(20, 16, 20, 12),
                  child: Text(
                    'Featured',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 20,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                featured.when(
                  data: (songs) {
                    if (songs.isEmpty) return const SizedBox(height: 8);
                    return SizedBox(
                      height: 200,
                      child: PageView.builder(
                        controller: _pageController,
                        onPageChanged: (i) => setState(() => _featuredIndex = i),
                        itemCount: songs.length,
                        itemBuilder: (context, i) {
                          final song = songs[i];
                          return GestureDetector(
                            onTap: () {
                              ref
                                  .read(audioPlayerServiceProvider)
                                  .playSong(song, queue: songs);
                              context.push('/player');
                            },
                            child: Container(
                              margin: const EdgeInsets.symmetric(horizontal: 6),
                              child: ClipRRect(
                                borderRadius: BorderRadius.circular(20),
                                child: Stack(
                                  fit: StackFit.expand,
                                  children: [
                                    CachedNetworkImage(
                                      imageUrl: song.thumbnailUrl,
                                      fit: BoxFit.cover,
                                      placeholder: (_, __) => Container(
                                          color: AppColors.surfaceVariant),
                                      errorWidget: (_, __, ___) =>
                                          Container(color: AppColors.surfaceVariant),
                                    ),
                                    Container(
                                      decoration: BoxDecoration(
                                        gradient: LinearGradient(
                                          begin: Alignment.topCenter,
                                          end: Alignment.bottomCenter,
                                          colors: [
                                            Colors.transparent,
                                            Colors.black.withOpacity(0.8),
                                          ],
                                        ),
                                      ),
                                    ),
                                    Positioned(
                                      bottom: 16,
                                      left: 16,
                                      right: 60,
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            song.title,
                                            style: const TextStyle(
                                              color: Colors.white,
                                              fontSize: 18,
                                              fontWeight: FontWeight.w700,
                                            ),
                                            maxLines: 1,
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                          Text(
                                            song.artist,
                                            style: const TextStyle(
                                              color: AppColors.textSecondary,
                                              fontSize: 13,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                    Positioned(
                                      bottom: 16,
                                      right: 16,
                                      child: Container(
                                        width: 44,
                                        height: 44,
                                        decoration: const BoxDecoration(
                                          color: AppColors.primary,
                                          shape: BoxShape.circle,
                                        ),
                                        child: const Icon(Icons.play_arrow_rounded,
                                            color: Colors.white, size: 26),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          );
                        },
                      ),
                    );
                  },
                  loading: () => Shimmer.fromColors(
                    baseColor: AppColors.surfaceVariant,
                    highlightColor: AppColors.card,
                    child: Container(
                      height: 200,
                      margin: const EdgeInsets.symmetric(horizontal: 20),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceVariant,
                        borderRadius: BorderRadius.circular(20),
                      ),
                    ),
                  ),
                  error: (_, __) => const SizedBox(height: 8),
                ),
                // Page dots
                featured.whenOrNull(
                  data: (songs) => songs.length > 1
                      ? Padding(
                          padding: const EdgeInsets.only(top: 12),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: List.generate(songs.length, (i) {
                              return AnimatedContainer(
                                duration: const Duration(milliseconds: 300),
                                margin: const EdgeInsets.symmetric(horizontal: 3),
                                width: i == _featuredIndex ? 20 : 6,
                                height: 6,
                                decoration: BoxDecoration(
                                  color: i == _featuredIndex
                                      ? AppColors.primary
                                      : AppColors.textTertiary,
                                  borderRadius: BorderRadius.circular(3),
                                ),
                              );
                            }),
                          ),
                        )
                      : null,
                ) ?? const SizedBox(),
              ],
            ),
          ),

          // Trending Section
          _SectionHeader(title: AppStrings.trendingNow),
          SliverToBoxAdapter(
            child: trending.when(
              data: (songs) => SizedBox(
                height: 180,
                child: ListView.builder(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: songs.take(10).length,
                  itemBuilder: (context, i) => Padding(
                    padding: const EdgeInsets.only(right: 12),
                    child: SongCard(
                        song: songs[i], queue: songs, width: 130),
                  ),
                ),
              ),
              loading: () => _shimmerRow(height: 180, count: 4, width: 130),
              error: (_, __) => const SizedBox(),
            ),
          ),

          // New Releases
          _SectionHeader(title: AppStrings.newReleases),
          SliverToBoxAdapter(
            child: newReleases.when(
              data: (songs) => ListView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: songs.take(5).length,
                itemBuilder: (context, i) => SongTile(
                  song: songs[i],
                  queue: songs,
                ),
              ).animate().fadeIn(duration: 400.ms),
              loading: () => Column(
                children: List.generate(
                    4,
                    (_) => Shimmer.fromColors(
                          baseColor: AppColors.surfaceVariant,
                          highlightColor: AppColors.card,
                          child: Container(
                            margin: const EdgeInsets.symmetric(
                                horizontal: 16, vertical: 6),
                            height: 60,
                            decoration: BoxDecoration(
                              color: AppColors.surfaceVariant,
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                        )),
              ),
              error: (_, __) => const SizedBox(),
            ),
          ),
          const SliverToBoxAdapter(child: SizedBox(height: 160)),
        ],
      ),
    );
  }

  Widget _shimmerRow({required double height, required int count, required double width}) {
    return SizedBox(
      height: height,
      child: Shimmer.fromColors(
        baseColor: AppColors.surfaceVariant,
        highlightColor: AppColors.card,
        child: ListView.builder(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 16),
          itemCount: count,
          itemBuilder: (_, __) => Container(
            width: width,
            margin: const EdgeInsets.only(right: 12),
            decoration: BoxDecoration(
              color: AppColors.surfaceVariant,
              borderRadius: BorderRadius.circular(14),
            ),
          ),
        ),
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  final VoidCallback? onSeeAll;
  const _SectionHeader({required this.title, this.onSeeAll});

  @override
  Widget build(BuildContext context) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 12),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              title,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.w700,
              ),
            ),
            if (onSeeAll != null)
              TextButton(
                onPressed: onSeeAll,
                child: const Text(
                  'See all',
                  style: TextStyle(color: AppColors.primary, fontSize: 13),
                ),
              ),
          ],
        ),
      ),
    );
  }
}



