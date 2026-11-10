// lib/data/services/youtube_service.dart
import 'package:youtube_explode_dart/youtube_explode_dart.dart';

enum AudioQuality { auto, low, medium, high, highest }

class YouTubeService {
  static final YoutubeExplode _yt = YoutubeExplode();

  /// Extract the best audio-only stream URL from a YouTube video URL or ID
  /// based on the requested [quality].
  static Future<String?> getAudioStreamUrl(
    String youtubeUrlOrId, {
    AudioQuality quality = AudioQuality.highest,
  }) async {
    try {
      final videoId = _extractVideoId(youtubeUrlOrId);
      if (videoId.isEmpty) return null;

      final manifest = await _yt.videos.streamsClient.getManifest(videoId);
      final audioStreams = manifest.audioOnly.toList();

      if (audioStreams.isEmpty) return null;

      // Sort by bitrate descending
      audioStreams.sort((a, b) => b.bitrate.compareTo(a.bitrate));

      AudioStreamInfo? chosen;

      switch (quality) {
        case AudioQuality.highest:
          // Pick the highest bitrate available (prefers 320kbps+, falls back gracefully)
          chosen = audioStreams.first;
          break;
        case AudioQuality.high:
          // 128–256 kbps range
          chosen = audioStreams.firstWhere(
            (s) {
              final kbps = s.bitrate.kiloBitsPerSecond;
              return kbps >= 128 && kbps <= 256;
            },
            orElse: () => audioStreams.first,
          );
          break;
        case AudioQuality.medium:
          // Around 96–128 kbps
          chosen = audioStreams.firstWhere(
            (s) {
              final kbps = s.bitrate.kiloBitsPerSecond;
              return kbps >= 64 && kbps <= 128;
            },
            orElse: () => audioStreams.last,
          );
          break;
        case AudioQuality.low:
          // Lowest bitrate available
          chosen = audioStreams.last;
          break;
        case AudioQuality.auto:
          // Let the network decide — pick the highest by default
          chosen = audioStreams.first;
          break;
      }

      return chosen.url.toString();
    } catch (e) {
      // ignore: avoid_print
      print('YouTubeService error: $e');
      return null;
    }
  }

  /// Get video metadata from YouTube
  static Future<Map<String, dynamic>?> getVideoInfo(String youtubeUrl) async {
    try {
      final videoId = _extractVideoId(youtubeUrl);
      if (videoId.isEmpty) return null;

      final video = await _yt.videos.get(videoId);
      return {
        'title': video.title,
        'author': video.author,
        'duration': video.duration?.inSeconds ?? 0,
        'thumbnailUrl': video.thumbnails.highResUrl,
        'videoId': videoId,
      };
    } catch (e) {
      // ignore: avoid_print
      print('YouTubeService getVideoInfo error: $e');
      return null;
    }
  }

  /// Extract video ID from various YouTube URL formats
  static String _extractVideoId(String urlOrId) {
    if (urlOrId.length == 11 && !urlOrId.contains('/')) {
      return urlOrId; // Already a video ID
    }
    final regExp = RegExp(
      r'(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})',
    );
    final match = regExp.firstMatch(urlOrId);
    return match?.group(1) ?? '';
  }

  /// Get YouTube thumbnail URL (no API needed)
  static String getThumbnailUrl(String videoId) {
    return 'https://img.youtube.com/vi/$videoId/maxresdefault.jpg';
  }

  static void dispose() {
    _yt.close();
  }
}
