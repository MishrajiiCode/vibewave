// lib/presentation/screens/ai_discover/ai_discover_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_strings.dart';
import '../../providers/ai_provider.dart';
import '../../providers/player_provider.dart';
import '../../widgets/song_tile.dart';
import '../../widgets/glassmorphic_card.dart';

class AiDiscoverScreen extends ConsumerWidget {
  const AiDiscoverScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedMood = ref.watch(selectedMoodProvider);
    final aiAnalysis = ref.watch(aiAnalysisProvider);
    final recommendations = ref.watch(aiRecommendationsProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      body: Stack(
        children: [
          // Background gradient orbs
          Positioned(
            top: -50,
            right: -50,
            child: Container(
              width: 250,
              height: 250,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    AppColors.primary.withOpacity(0.2),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),
          Positioned(
            bottom: 100,
            left: -50,
            child: Container(
              width: 200,
              height: 200,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    AppColors.secondary.withOpacity(0.2),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),
          // Main content
          SafeArea(
            child: CustomScrollView(
              slivers: [
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(8),
                              decoration: BoxDecoration(
                                gradient: AppColors.primaryGradient,
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: const Icon(Icons.auto_awesome_rounded,
                                  color: Colors.white, size: 22),
                            ),
                            const SizedBox(width: 12),
                            const Text(
                              'AI Discover',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 28,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          aiAnalysis['suggestion'] as String,
                          style: const TextStyle(
                            color: AppColors.textSecondary,
                            fontSize: 14,
                          ),
                        ),
                      ],
                    ),
                  ).animate().fadeIn(duration: 400.ms).slideY(begin: -0.2, end: 0),
                ),

                // Mood selector
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(20, 24, 20, 12),
                    child: GlassmorphicCard(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'How are you feeling?',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 16,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          const SizedBox(height: 12),
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: AppStrings.moods.map((mood) {
                              final isSelected = selectedMood == mood;
                              return GestureDetector(
                                onTap: () {
                                  ref.read(selectedMoodProvider.notifier).state =
                                      isSelected ? null : mood;
                                },
                                child: AnimatedContainer(
                                  duration: const Duration(milliseconds: 200),
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 14, vertical: 8),
                                  decoration: BoxDecoration(
                                    gradient: isSelected
                                        ? AppColors.primaryGradient
                                        : null,
                                    color: isSelected
                                        ? null
                                        : AppColors.surfaceVariant,
                                    borderRadius: BorderRadius.circular(20),
                                    border: Border.all(
                                      color: isSelected
                                          ? Colors.transparent
                                          : AppColors.glassBorder,
                                    ),
                                  ),
                                  child: Text(
                                    mood,
                                    style: TextStyle(
                                      color: isSelected
                                          ? Colors.white
                                          : AppColors.textSecondary,
                                      fontSize: 13,
                                      fontWeight: isSelected
                                          ? FontWeight.w600
                                          : FontWeight.w400,
                                    ),
                                  ),
                                ),
                              );
                            }).toList(),
                          ),
                        ],
                      ),
                    ),
                  ).animate().fadeIn(delay: 200.ms, duration: 400.ms),
                ),

                // AI recommendations
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
                    child: Row(
                      children: [
                        const Icon(Icons.auto_awesome_rounded,
                            color: AppColors.primary, size: 20),
                        const SizedBox(width: 8),
                        const Text(
                          'AI Picks For You',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),

                SliverToBoxAdapter(
                  child: recommendations.when(
                    data: (songs) {
                      if (songs.isEmpty) {
                        return Padding(
                          padding: const EdgeInsets.all(40),
                          child: Column(
                            children: [
                              const Icon(Icons.music_note_rounded,
                                  color: AppColors.textTertiary, size: 60),
                              const SizedBox(height: 16),
                              const Text(
                                'No recommendations yet',
                                style:
                                    TextStyle(color: AppColors.textSecondary),
                              ),
                              const SizedBox(height: 8),
                              const Text(
                                'Add some songs via the Admin panel\nto get started',
                                style: TextStyle(
                                    color: AppColors.textTertiary, fontSize: 13),
                                textAlign: TextAlign.center,
                              ),
                            ],
                          ),
                        );
                      }
                      return Column(
                        children: [
                          // Play all button
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 20),
                            child: SizedBox(
                              width: double.infinity,
                              child: ElevatedButton.icon(
                                icon: const Icon(Icons.play_arrow_rounded),
                                label: const Text('Play All AI Picks'),
                                onPressed: () {
                                  if (songs.isNotEmpty) {
                                    ref
                                        .read(audioPlayerServiceProvider)
                                        .playSong(songs.first, queue: songs);
                                  }
                                },
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: AppColors.primary,
                                  padding: const EdgeInsets.symmetric(
                                      vertical: 14),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(14),
                                  ),
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: 12),
                          ListView.builder(
                            shrinkWrap: true,
                            physics: const NeverScrollableScrollPhysics(),
                            itemCount: songs.length,
                            itemBuilder: (_, i) => SongTile(
                              song: songs[i],
                              queue: songs,
                            ).animate(delay: (i * 50).ms).fadeIn().slideX(
                                  begin: 0.2,
                                  end: 0,
                                ),
                          ),
                        ],
                      );
                    },
                    loading: () => Padding(
                      padding: const EdgeInsets.all(40),
                      child: Column(
                        children: [
                          const CircularProgressIndicator(
                              color: AppColors.primary),
                          const SizedBox(height: 16),
                          Text(
                            selectedMood != null
                                ? 'Finding songs for ${selectedMood.split(" ").first} mood...'
                                : 'Analyzing your taste...',
                            style: const TextStyle(
                                color: AppColors.textSecondary),
                          ),
                        ],
                      ),
                    ),
                    error: (e, _) => Padding(
                      padding: const EdgeInsets.all(20),
                      child: Text(e.toString(),
                          style: const TextStyle(color: AppColors.error)),
                    ),
                  ),
                ),
                const SliverToBoxAdapter(child: SizedBox(height: 160)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

