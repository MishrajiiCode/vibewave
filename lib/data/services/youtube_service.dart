// lib/data/services/youtube_service.dart
import 'package:youtube_explode_dart/youtube_explode_dart.dart';

class YouTubeService {
  static final YoutubeExplode _yt = YoutubeExplode();

  /// Extract the best audio-only stream URL from a YouTube video URL or ID
  static Future<String?> getAudioStreamUrl(String youtubeUrlOrId) async {
    try {
      final videoId = _extractVideoId(youtubeUrlOrId);
      if (videoId.isEmpty) return null;

      final manifest = await _yt.videos.streamsClient.getManifest(videoId);
      final audioStream = manifest.audioOnly.withHighestBitrate();
      return audioStream.url.toString();
    } catch (e) {
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
        'thumbnailUrl': video.thumbnails.maxResUrl ??
            video.thumbnails.highResUrl ??
            video.thumbnails.standardResUrl,
        'videoId': videoId,
      };
    } catch (e) {
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
