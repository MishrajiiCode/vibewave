import 'package:flutter_test/flutter_test.dart';
import 'package:vibewave/core/utils/helpers.dart';
import 'package:vibewave/data/models/category_model.dart';
import 'package:vibewave/data/models/song_model.dart';
import 'package:vibewave/data/services/youtube_service.dart';

void main() {
  group('Helpers Tests', () {
    test('formatDuration formats seconds to MM:SS', () {
      expect(Helpers.formatDuration(0), '00:00');
      expect(Helpers.formatDuration(65), '01:05');
      expect(Helpers.formatDuration(215), '03:35');
    });

    test('formatNumber formats thousands and millions', () {
      expect(Helpers.formatNumber(500), '500');
      expect(Helpers.formatNumber(1500), '1.5K');
      expect(Helpers.formatNumber(2400000), '2.4M');
    });

    test('isValidYouTubeUrl detects valid YouTube URLs', () {
      expect(Helpers.isValidYouTubeUrl('https://www.youtube.com/watch?v=dQw4w9WgXcQ'), isTrue);
      expect(Helpers.isValidYouTubeUrl('https://youtu.be/dQw4w9WgXcQ'), isTrue);
      expect(Helpers.isValidYouTubeUrl('https://example.com/audio.mp3'), isFalse);
    });
  });

  group('YouTubeService Tests', () {
    test('getThumbnailUrl returns correct maxresdefault URL', () {
      const videoId = 'dQw4w9WgXcQ';
      expect(
        YouTubeService.getThumbnailUrl(videoId),
        'https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg',
      );
    });
  });

  group('CategoryModel Tests', () {
    test('defaults contains standard categories', () {
      final categories = CategoryModel.defaults;
      expect(categories.isNotEmpty, isTrue);
      expect(categories.any((c) => c.name == 'Pop'), isTrue);
      expect(categories.any((c) => c.name == 'Rock'), isTrue);
      expect(categories.any((c) => c.name == 'Lofi'), isTrue);
    });
  });

  group('SongModel Tests', () {
    test('SongModel instantiates with correct fields', () {
      final now = DateTime.now();
      final song = SongModel(
        id: 'test_1',
        title: 'Starboy',
        artist: 'The Weeknd',
        album: 'Starboy',
        youtubeUrl: 'https://youtube.com/watch?v=12345678901',
        videoId: '12345678901',
        thumbnailUrl: 'https://example.com/thumb.jpg',
        category: 'Pop',
        duration: 230,
        createdAt: now,
        updatedAt: now,
      );

      expect(song.title, 'Starboy');
      expect(song.artist, 'The Weeknd');
      expect(song.duration, 230);
      expect(song.category, 'Pop');
      expect(song.isFeatured, isFalse);

      final updated = song.copyWith(isFeatured: true, playCount: 42);
      expect(updated.isFeatured, isTrue);
      expect(updated.playCount, 42);
      expect(updated.title, 'Starboy');
    });
  });
}
