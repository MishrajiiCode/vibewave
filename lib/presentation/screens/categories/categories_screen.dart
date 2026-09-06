// lib/presentation/screens/categories/categories_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../providers/music_provider.dart';
import '../../widgets/song_tile.dart';

class CategoriesScreen extends ConsumerWidget {
  const CategoriesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final categories = ref.watch(categoriesProvider);
    final selectedCategory = ref.watch(selectedCategoryProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        title: Text(
          selectedCategory ?? 'Categories',
          style: const TextStyle(
              color: Colors.white, fontWeight: FontWeight.w700, fontSize: 20),
        ),
        leading: selectedCategory != null
            ? IconButton(
                icon: const Icon(Icons.arrow_back_ios_rounded, color: Colors.white),
                onPressed: () {
                  ref.read(selectedCategoryProvider.notifier).state = null;
                },
              )
            : null,
      ),
      body: selectedCategory != null
          ? _CategorySongsView(category: selectedCategory)
          : _CategoriesGrid(),
    );
  }
}

class _CategoriesGrid extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final categories = ref.watch(categoriesProvider);
    return categories.when(
      data: (cats) => GridView.builder(
        padding: const EdgeInsets.all(16),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          mainAxisSpacing: 12,
          crossAxisSpacing: 12,
          childAspectRatio: 1.6,
        ),
        itemCount: cats.length,
        itemBuilder: (context, i) {
          final cat = cats[i];
          return GestureDetector(
            onTap: () {
              ref.read(selectedCategoryProvider.notifier).state = cat.name;
            },
            child: Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    cat.displayColor,
                    cat.displayColor.withOpacity(0.5),
                  ],
                ),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Stack(
                children: [
                  Positioned(
                    right: -12,
                    bottom: -12,
                    child: Icon(Icons.music_note_rounded,
                        size: 80,
                        color: Colors.white.withOpacity(0.15)),
                  ),
                  Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        Text(
                          cat.name,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        if (cat.description.isNotEmpty)
                          Text(
                            cat.description,
                            style: TextStyle(
                              color: Colors.white.withOpacity(0.7),
                              fontSize: 12,
                            ),
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ).animate(delay: (i * 50).ms).fadeIn().slideY(begin: 0.3, end: 0);
        },
      ),
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, __) => const Center(
          child: Text('Error loading categories',
              style: TextStyle(color: AppColors.textSecondary))),
    );
  }
}

class _CategorySongsView extends ConsumerWidget {
  final String category;
  const _CategorySongsView({required this.category});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final songs = ref.watch(songsByCategoryProvider(category));
    return songs.when(
      data: (list) => list.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.music_off_rounded,
                      color: AppColors.textTertiary, size: 60),
                  const SizedBox(height: 16),
                  Text('No songs in $category yet',
                      style: const TextStyle(color: AppColors.textSecondary)),
                ],
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.only(bottom: 160),
              itemCount: list.length,
              itemBuilder: (_, i) => SongTile(song: list[i], queue: list)
                  .animate(delay: (i * 30).ms)
                  .fadeIn()
                  .slideX(begin: 0.2, end: 0),
            ),
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, __) => const Center(
          child: Text('Error loading songs',
              style: TextStyle(color: AppColors.textSecondary))),
    );
  }
}
