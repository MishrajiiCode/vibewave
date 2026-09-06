// lib/core/constants/app_colors.dart
import 'package:flutter/material.dart';

class AppColors {
  // Primary gradient colors
  static const Color primary = Color(0xFF6C63FF);
  static const Color primaryLight = Color(0xFF9B8FFF);
  static const Color primaryDark = Color(0xFF4A41CC);
  static const Color secondary = Color(0xFFFF6B9D);
  static const Color accent = Color(0xFF00D9FF);

  // Background colors
  static const Color background = Color(0xFF0A0A0F);
  static const Color surface = Color(0xFF12121A);
  static const Color surfaceVariant = Color(0xFF1A1A28);
  static const Color card = Color(0xFF1E1E2E);

  // Glassmorphic
  static const Color glass = Color(0x20FFFFFF);
  static const Color glassBorder = Color(0x30FFFFFF);
  static const Color glassDark = Color(0x15FFFFFF);

  // Text colors
  static const Color textPrimary = Color(0xFFFFFFFF);
  static const Color textSecondary = Color(0xFFB0B0C8);
  static const Color textTertiary = Color(0xFF6B6B8A);
  static const Color textHint = Color(0xFF4A4A6A);

  // Status colors
  static const Color success = Color(0xFF00E676);
  static const Color warning = Color(0xFFFFD740);
  static const Color error = Color(0xFFFF5252);
  static const Color info = Color(0xFF40C4FF);

  // Category colors
  static const List<Color> categoryColors = [
    Color(0xFF6C63FF), // Pop
    Color(0xFFFF6B9D), // Rock
    Color(0xFF00D9FF), // Hip-Hop
    Color(0xFFFFD740), // Classical
    Color(0xFF00E676), // EDM
    Color(0xFFFF6E40), // Jazz
    Color(0xFFCE93D8), // R&B
    Color(0xFF80DEEA), // Country
    Color(0xFFF48FB1), // Bollywood
    Color(0xFFA5D6A7), // Lofi
  ];

  // Gradients
  static const LinearGradient primaryGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF6C63FF), Color(0xFF9B8FFF)],
  );

  static const LinearGradient darkGradient = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [Color(0xFF0A0A0F), Color(0xFF12121A)],
  );

  static const LinearGradient playerGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF1A0050), Color(0xFF0A0A0F), Color(0xFF001A30)],
    stops: [0.0, 0.5, 1.0],
  );

  static const LinearGradient cardGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0x306C63FF), Color(0x30FF6B9D)],
  );

  static LinearGradient vibrantGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      primary.withOpacity(0.8),
      secondary.withOpacity(0.8),
    ],
  );
}
