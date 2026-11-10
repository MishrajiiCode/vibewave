// lib/presentation/screens/player/equalizer_sheet.dart
// 5-band equalizer bottom sheet using just_audio's AndroidEqualizer.

import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import '../../../core/constants/app_colors.dart';
import '../../providers/player_provider.dart';

// Preset definitions: [bass, lowMid, mid, highMid, treble] gains in dB
const _presets = <String, List<double>>{
  'Flat': [0, 0, 0, 0, 0],
  'Bass Boost': [6, 4, 0, -2, -2],
  'Rock': [5, 3, -1, 3, 5],
  'Pop': [-1, 2, 4, 2, -1],
  'Classical': [4, 3, -2, 3, 4],
  'Jazz': [3, 2, 0, 2, 3],
  'Electronic': [5, 2, 1, 4, 3],
};

class EqualizerSheet extends ConsumerStatefulWidget {
  const EqualizerSheet({super.key});

  @override
  ConsumerState<EqualizerSheet> createState() => _EqualizerSheetState();
}

class _EqualizerSheetState extends ConsumerState<EqualizerSheet> {
  String _selectedPreset = 'Flat';
  final List<double> _gains = [0, 0, 0, 0, 0];
  final _bandLabels = ['Bass', 'Low-Mid', 'Mid', 'High-Mid', 'Treble'];
  AndroidEqualizerParameters? _parameters;
  bool _isEnabled = true;

  @override
  void initState() {
    super.initState();
    _loadEqualizerParams();
  }

  Future<void> _loadEqualizerParams() async {
    if (!Platform.isAndroid) return;
    final service = ref.read(audioPlayerServiceProvider);
    final eq = service.equalizer;
    if (eq == null) return;

    final params = await eq.parameters;
    if (mounted) {
      setState(() {
        _parameters = params;
        _isEnabled = eq.enabled.value;
        for (int i = 0; i < params.bands.length && i < 5; i++) {
          _gains[i] = params.bands[i].gain;
        }
      });
    }
  }

  Future<void> _applyGain(int bandIndex, double gain) async {
    if (!Platform.isAndroid) return;
    final params = _parameters;
    if (params == null) return;

    if (bandIndex < params.bands.length) {
      await params.bands[bandIndex].setGain(gain);
    }
    setState(() => _gains[bandIndex] = gain);
  }

  Future<void> _applyPreset(String presetName) async {
    final gains = _presets[presetName]!;
    setState(() {
      _selectedPreset = presetName;
      for (int i = 0; i < gains.length; i++) {
        _gains[i] = gains[i];
      }
    });
    for (int i = 0; i < gains.length; i++) {
      await _applyGain(i, gains[i]);
    }
  }

  Future<void> _toggleEqualizer(bool enabled) async {
    if (!Platform.isAndroid) return;
    final service = ref.read(audioPlayerServiceProvider);
    final eq = service.equalizer;
    if (eq == null) return;
    eq.setEnabled(enabled);
    setState(() => _isEnabled = enabled);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Handle
            Container(
              margin: const EdgeInsets.only(top: 12),
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.textTertiary,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            // Header
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
              child: Row(
                children: [
                  const Text(
                    'Equalizer',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 20,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const Spacer(),
                  Switch(
                    value: _isEnabled,
                    activeColor: AppColors.primary,
                    onChanged: _toggleEqualizer,
                  ),
                ],
              ),
            ),
            // Preset chips
            SizedBox(
              height: 44,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                children: _presets.keys.map((preset) {
                  final isSelected = _selectedPreset == preset;
                  return GestureDetector(
                    onTap: _isEnabled ? () => _applyPreset(preset) : null,
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 200),
                      margin: const EdgeInsets.only(right: 8),
                      padding: const EdgeInsets.symmetric(
                          horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(
                        color: isSelected
                            ? AppColors.primary.withOpacity(0.2)
                            : AppColors.surfaceVariant,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: isSelected
                              ? AppColors.primary
                              : Colors.transparent,
                          width: 1.5,
                        ),
                      ),
                      child: Text(
                        preset,
                        style: TextStyle(
                          color: isSelected
                              ? AppColors.primary
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
            ),
            const SizedBox(height: 24),
            // EQ bands
            Opacity(
              opacity: _isEnabled ? 1.0 : 0.4,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: List.generate(5, (i) {
                  return _EQBand(
                    label: _bandLabels[i],
                    gain: _gains[i],
                    enabled: _isEnabled,
                    onChanged: (v) => _applyGain(i, v),
                  );
                }),
              ),
            ),
            const SizedBox(height: 24),
            if (!Platform.isAndroid)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceVariant,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Text(
                    'Hardware equalizer is only available on Android. '
                    'On other platforms, preset display is for reference.',
                    style: TextStyle(
                        color: AppColors.textTertiary, fontSize: 12),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }
}

class _EQBand extends StatelessWidget {
  final String label;
  final double gain;
  final bool enabled;
  final ValueChanged<double> onChanged;

  const _EQBand({
    required this.label,
    required this.gain,
    required this.enabled,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          '${gain >= 0 ? '+' : ''}${gain.toStringAsFixed(1)}',
          style: const TextStyle(
            color: AppColors.primary,
            fontSize: 11,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 4),
        SizedBox(
          height: 160,
          child: RotatedBox(
            quarterTurns: 3,
            child: Slider(
              value: gain.clamp(-10.0, 10.0),
              min: -10,
              max: 10,
              divisions: 40,
              activeColor: AppColors.primary,
              inactiveColor: AppColors.surfaceVariant,
              onChanged: enabled ? onChanged : null,
            ),
          ),
        ),
        Text(
          label,
          style: const TextStyle(
            color: AppColors.textSecondary,
            fontSize: 11,
          ),
        ),
      ],
    );
  }
}
