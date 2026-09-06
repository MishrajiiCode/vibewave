// lib/core/utils/helpers.dart
import 'package:intl/intl.dart';

class Helpers {
  static String formatDuration(int seconds) {
    final d = Duration(seconds: seconds);
    final m = d.inMinutes.remainder(60).toString().padLeft(2, "0");
    final s = d.inSeconds.remainder(60).toString().padLeft(2, "0");
    return "$m:$s";
  }

  static String formatDate(DateTime date) {
    return DateFormat("MMM dd, yyyy").format(date);
  }

  static String formatNumber(int number) {
    if (number >= 1000000) {
      return "${(number / 1000000).toStringAsFixed(1)}M";
    } else if (number >= 1000) {
      return "${(number / 1000).toStringAsFixed(1)}K";
    }
    return number.toString();
  }

  static bool isValidYouTubeUrl(String url) {
    return url.contains("youtube.com") || url.contains("youtu.be");
  }
}
