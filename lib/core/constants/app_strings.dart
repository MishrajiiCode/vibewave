// lib/core/constants/app_strings.dart
class AppStrings {
  static const String appName = 'VibeWave';
  static const String appTagline = 'Feel the rhythm';

  // Navigation
  static const String home = 'Home';
  static const String search = 'Search';
  static const String library = 'Library';
  static const String discover = 'Discover';

  // Home
  static const String featuredPlaylists = 'Featured Playlists';
  static const String recentlyPlayed = 'Recently Played';
  static const String aiPicksForYou = 'AI Picks For You';
  static const String trendingNow = 'Trending Now';
  static const String newReleases = 'New Releases';

  // Player
  static const String nowPlaying = 'Now Playing';
  static const String addToPlaylist = 'Add to Playlist';
  static const String addToFavorites = 'Add to Favorites';
  static const String removeFromFavorites = 'Remove from Favorites';

  // Search
  static const String searchHint = 'Songs, artists, moods...';
  static const String browseCategories = 'Browse Categories';
  static const String noResults = 'No results found';
  static const String tryDifferentSearch = 'Try a different search term';

  // Library
  static const String likedSongs = 'Liked Songs';
  static const String recentlyPlayedFull = 'Recently Played';
  static const String createPlaylist = 'Create Playlist';
  static const String myPlaylists = 'My Playlists';
  static const String noSongsYet = 'No songs yet';
  static const String startListening = 'Start listening to save your music here';

  // AI Discover
  static const String aiDiscover = 'AI Discover';
  static const String howAreYouFeeling = 'How are you feeling?';
  static const String aiRecommendations = 'AI Recommendations';
  static const String basedOnHistory = 'Based on your listening history';
  static const String generatingRecommendations = 'Generating recommendations...';

  // Admin
  static const String adminPanel = 'Admin Panel';
  static const String addSong = 'Add Song';
  static const String editSong = 'Edit Song';
  static const String deleteSong = 'Delete Song';
  static const String addCategory = 'Add Category';
  static const String songTitle = 'Song Title';
  static const String artistName = 'Artist Name';
  static const String albumName = 'Album Name';
  static const String youtubeUrl = 'YouTube URL';
  static const String youtubeUrlHint = 'https://www.youtube.com/watch?v=...';
  static const String category = 'Category';
  static const String thumbnailUrl = 'Thumbnail URL';
  static const String tags = 'Tags';
  static const String lyrics = 'Lyrics (Optional)';
  static const String featured = 'Featured';
  static const String save = 'Save';
  static const String cancel = 'Cancel';
  static const String delete = 'Delete';
  static const String confirmDelete = 'Confirm Delete';
  static const String areYouSureDelete = 'Are you sure you want to delete this song?';
  static const String songSaved = 'Song saved successfully!';
  static const String songDeleted = 'Song deleted successfully!';
  static const String errorSaving = 'Error saving song. Please try again.';

  // Auth
  static const String adminEmail = 'Admin Email';
  static const String password = 'Password';
  static const String signIn = 'Sign In';
  static const String invalidCredentials = 'Invalid email or password';

  // Notifications
  static const String notificationPermission = 'Allow Notifications';
  static const String notificationPermissionDesc =
      'VibeWave would like to send you notifications about new music and updates.';
  static const String allow = 'Allow';
  static const String dontAllow = "Don't Allow";

  // Errors
  static const String networkError = 'No internet connection';
  static const String loadError = 'Failed to load. Tap to retry.';
  static const String playbackError = 'Playback error. Trying to recover...';
  static const String invalidUrl = 'Please enter a valid YouTube URL';

  // Categories
  static const List<String> defaultCategories = [
    'Pop',
    'Rock',
    'Hip-Hop',
    'Classical',
    'EDM',
    'Jazz',
    'R&B',
    'Country',
    'Bollywood',
    'Lofi',
    'Devotional',
    'Indie',
  ];

  // Moods
  static const List<String> moods = [
    'Happy ??',
    'Sad ??',
    'Energetic ?',
    'Chill ??',
    'Romantic ??',
    'Focused ??',
    'Party ??',
    'Workout ??',
  ];
}
