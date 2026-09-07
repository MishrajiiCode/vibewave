package com.music.bitchord.data.firebase

import com.music.bitchord.data.model.Song

/**
 * Convenient wrapper for logging granular in-app events to Firestore.
 */
object ActivityTracker {

    fun onAppOpen() {
        FirestoreManager.logActivity(
            activityType = "APP_OPEN",
            title = "Session Started",
            details = "User launched VibeWave",
        )
    }

    fun onScreenView(screenName: String) {
        FirestoreManager.logActivity(
            activityType = "SCREEN_VIEW",
            title = "Navigated to $screenName",
            details = "User viewed the $screenName screen",
        )
    }

    fun onSongPlay(song: Song) {
        FirestoreManager.recordPlay(song)
        com.music.bitchord.data.ai.AiPicksManager.onSongPlayed(song)
        FirestoreManager.logActivity(
            activityType = "SONG_PLAY",
            title = "Playing: ${song.title}",
            details = "Artist: ${song.artist} · ID: ${song.videoId}",
        )
    }

    fun onSongPause(song: Song?) {
        if (song == null) return
        FirestoreManager.logActivity(
            activityType = "SONG_PAUSE",
            title = "Paused: ${song.title}",
            details = "Artist: ${song.artist}",
        )
    }

    fun onSongSkip(song: Song?) {
        if (song == null) return
        FirestoreManager.recordSkip(song)
        com.music.bitchord.data.ai.AiPicksManager.onSongSkipped(song)
        FirestoreManager.logActivity(
            activityType = "SONG_SKIP",
            title = "Skipped: ${song.title}",
            details = "Artist: ${song.artist} · ID: ${song.videoId}",
        )
    }

    fun onSongLike(song: Song, isLiked: Boolean) {
        FirestoreManager.recordLike(song, isLiked)
        com.music.bitchord.data.ai.AiPicksManager.onSongLiked(song, isLiked)
        val action = if (isLiked) "Liked" else "Unliked"
        FirestoreManager.logActivity(
            activityType = "SONG_RATING",
            title = "$action: ${song.title}",
            details = "Artist: ${song.artist} · ID: ${song.videoId}",
        )
    }

    fun onSongLike(songTitle: String, artist: String, videoId: String, isLiked: Boolean) {
        FirestoreManager.recordLike(songTitle, artist, videoId, isLiked)
        val action = if (isLiked) "Liked" else "Unliked"
        FirestoreManager.logActivity(
            activityType = "SONG_RATING",
            title = "$action: $songTitle",
            details = "Artist: $artist · ID: $videoId",
        )
    }

    fun onSearch(query: String) {
        if (query.isBlank()) return
        FirestoreManager.logActivity(
            activityType = "SEARCH",
            title = "Search: \"$query\"",
            details = "User searched the catalog for $query",
        )
    }

    fun onAiPick(vibe: String, songTitle: String, artist: String) {
        FirestoreManager.logActivity(
            activityType = "AI_SONG_PICK",
            title = "AI Curated: $songTitle",
            details = "Vibe: $vibe · Artist: $artist",
        )
    }

    fun onPlaylistCreate(title: String) {
        FirestoreManager.logActivity(
            activityType = "PLAYLIST_CREATE",
            title = "Created Playlist: $title",
            details = "User generated a new custom playlist",
        )
    }
}
