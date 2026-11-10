// lib/presentation/screens/settings/settings_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_strings.dart';
import '../../../data/services/youtube_service.dart';
import '../../providers/player_provider.dart';
import '../../providers/settings_provider.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  final String _appVersion = '2.0.0 (Build 2)';
  int? _sleepTimerMinutes;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final quality = ref.watch(audioQualityProvider);
    final crossfade = ref.watch(crossfadeDurationProvider);
    final skipSilence = ref.watch(skipSilenceProvider);
    final speed = ref.watch(playbackSpeedProvider);
    final audioService = ref.read(audioPlayerServiceProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: Colors.white),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          AppStrings.settings,
          style: TextStyle(
            color: Colors.white,
            fontSize: 20,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      body: ListView(
        children: [
          // ── Audio Quality ────────────────────────────────────────────
          _SectionHeader(title: AppStrings.audioQuality),
          ...AudioQuality.values.map((q) {
            return RadioListTile<AudioQuality>(
              value: q,
              groupValue: quality,
              activeColor: AppColors.primary,
              title: Text(
                _qualityLabel(q),
                style: const TextStyle(color: Colors.white),
              ),
              subtitle: Text(
                _qualityDescription(q),
                style: const TextStyle(color: AppColors.textTertiary, fontSize: 12),
              ),
              onChanged: (v) async {
                if (v == null) return;
                ref.read(audioQualityProvider.notifier).state = v;
                audioService.setAudioQuality(v);
                final prefs = await SharedPreferences.getInstance();
                persistAudioQuality(prefs, v);
              },
            );
          }),

          // ── Playback ─────────────────────────────────────────────────
          _SectionHeader(title: AppStrings.playback),

          // Crossfade
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      AppStrings.crossfade,
                      style: TextStyle(color: Colors.white, fontSize: 15),
                    ),
                    Text(
                      crossfade == 0
                          ? AppStrings.off
                          : '${crossfade}s',
                      style: const TextStyle(
                          color: AppColors.primary, fontSize: 14),
                    ),
                  ],
                ),
                Slider(
                  value: crossfade.toDouble(),
                  min: 0,
                  max: 10,
                  divisions: 10,
                  activeColor: AppColors.primary,
                  inactiveColor: AppColors.surfaceVariant,
                  onChanged: (v) async {
                    final seconds = v.round();
                    ref.read(crossfadeDurationProvider.notifier).state = seconds;
                    audioService.setCrossfadeDuration(seconds);
                    final prefs = await SharedPreferences.getInstance();
                    saveCrossfade(prefs, seconds);
                  },
                ),
              ],
            ),
          ),

          // Skip silence
          SwitchListTile(
            value: skipSilence,
            activeColor: AppColors.primary,
            title: const Text(AppStrings.skipSilence,
                style: TextStyle(color: Colors.white)),
            subtitle: const Text(
              AppStrings.skipSilenceDesc,
              style: TextStyle(color: AppColors.textTertiary, fontSize: 12),
            ),
            onChanged: (v) async {
              ref.read(skipSilenceProvider.notifier).state = v;
              audioService.setSkipSilence(v);
              final prefs = await SharedPreferences.getInstance();
              saveSkipSilence(prefs, v);
            },
          ),

          // Speed
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      AppStrings.playbackSpeed,
                      style: TextStyle(color: Colors.white, fontSize: 15),
                    ),
                    Text(
                      '${speed.toStringAsFixed(2)}x',
                      style: const TextStyle(
                          color: AppColors.primary, fontSize: 14),
                    ),
                  ],
                ),
                Slider(
                  value: speed,
                  min: 0.5,
                  max: 2.0,
                  divisions: 15,
                  activeColor: AppColors.primary,
                  inactiveColor: AppColors.surfaceVariant,
                  onChanged: (v) async {
                    ref.read(playbackSpeedProvider.notifier).state = v;
                    await audioService.setSpeed(v);
                    final prefs = await SharedPreferences.getInstance();
                    savePlaybackSpeed(prefs, v);
                  },
                ),
              ],
            ),
          ),

          // ── Sleep Timer ───────────────────────────────────────────────
          _SectionHeader(title: AppStrings.sleepTimer),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
            child: Wrap(
              spacing: 10,
              runSpacing: 10,
              children: [
                ...[15, 30, 45, 60, 90].map((m) => _TimerChip(
                      label: '${m}min',
                      isSelected: _sleepTimerMinutes == m,
                      onTap: () {
                        setState(() => _sleepTimerMinutes = m);
                        audioService.setSleepTimer(Duration(minutes: m));
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text('Sleep timer set for $m minutes'),
                            backgroundColor: AppColors.surface,
                          ),
                        );
                      },
                    )),
                _TimerChip(
                  label: AppStrings.off,
                  isSelected: _sleepTimerMinutes == null,
                  onTap: () {
                    setState(() => _sleepTimerMinutes = null);
                    audioService.cancelSleepTimer();
                  },
                ),
              ],
            ),
          ),

          // ── About ─────────────────────────────────────────────────────
          _SectionHeader(title: AppStrings.about),
          ListTile(
            leading: const Icon(Icons.info_outline_rounded,
                color: AppColors.textSecondary),
            title: const Text(AppStrings.appName,
                style: TextStyle(color: Colors.white)),
            subtitle: Text(
              'Version $_appVersion',
              style: const TextStyle(
                  color: AppColors.textTertiary, fontSize: 12),
            ),
          ),
          ListTile(
            leading: const Icon(Icons.code_rounded,
                color: AppColors.textSecondary),
            title: const Text('GitHub',
                style: TextStyle(color: Colors.white)),
            subtitle: const Text('View source code',
                style: TextStyle(color: AppColors.textTertiary, fontSize: 12)),
            onTap: () async {
              final uri = Uri.parse('https://github.com/vibewave-app/vibewave');
              if (await canLaunchUrl(uri)) launchUrl(uri);
            },
          ),

          const SizedBox(height: 100),
        ],
      ),
    );
  }

  String _qualityLabel(AudioQuality q) {
    switch (q) {
      case AudioQuality.auto:
        return 'Auto';
      case AudioQuality.low:
        return 'Low';
      case AudioQuality.medium:
        return 'Normal';
      case AudioQuality.high:
        return 'High';
      case AudioQuality.highest:
        return 'Highest (320kbps+)';
    }
  }

  String _qualityDescription(AudioQuality q) {
    switch (q) {
      case AudioQuality.auto:
        return 'Automatically picks the best quality';
      case AudioQuality.low:
        return 'Saves data, lowest bitrate';
      case AudioQuality.medium:
        return '96–128 kbps';
      case AudioQuality.high:
        return '128–256 kbps';
      case AudioQuality.highest:
        return 'Best available, 320kbps+ where possible';
    }
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 8),
      child: Text(
        title,
        style: const TextStyle(
          color: AppColors.primary,
          fontSize: 13,
          fontWeight: FontWeight.w700,
          letterSpacing: 1.2,
        ),
      ),
    );
  }
}

class _TimerChip extends StatelessWidget {
  final String label;
  final bool isSelected;
  final VoidCallback onTap;

  const _TimerChip({
    required this.label,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          color: isSelected
              ? AppColors.primary.withOpacity(0.2)
              : AppColors.surfaceVariant,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isSelected ? AppColors.primary : Colors.transparent,
            width: 1.5,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: isSelected ? AppColors.primary : AppColors.textSecondary,
            fontWeight:
                isSelected ? FontWeight.w600 : FontWeight.w400,
            fontSize: 14,
          ),
        ),
      ),
    );
  }
}

