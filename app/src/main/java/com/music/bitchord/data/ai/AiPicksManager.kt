package com.music.bitchord.data.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Intelligent on-device AI playlist & dynamic curation manager.
 * Tracks all user activity (song plays, likes, skips) in real-time, builds user-specific taste models,
 * and automatically curates and populates the "AI Picks" folder and smart playlists.
 */
object AiPicksManager {
    private const val TAG = "AiPicksManager"
    private const val PREFS_NAME = "ai_picks_store"
    private const val KEY_AI_PICKS_JSON = "ai_picks_songs"
    private const val KEY_LIKED_REC_JSON = "ai_liked_rec_songs"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var prefs: SharedPreferences? = null

    private val _aiPicksSongs = MutableStateFlow<List<Song>>(emptyList())
    val aiPicksSongs = _aiPicksSongs.asStateFlow()

    private val _dailyMixSongs = MutableStateFlow<List<Song>>(emptyList())
    val dailyMixSongs = _dailyMixSongs.asStateFlow()

    private val _likedAndRecSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedAndRecSongs = _likedAndRecSongs.asStateFlow()

    private val playFrequencies = mutableMapOf<String, Int>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadCached()
        refreshSmartCuration()
    }

    private fun loadCached() {
        val cachedPicks = prefs?.getString(KEY_AI_PICKS_JSON, null)
        if (!cachedPicks.isNullOrBlank()) {
            _aiPicksSongs.value = parseSongsJson(cachedPicks)
        }
        val cachedLiked = prefs?.getString(KEY_LIKED_REC_JSON, null)
        if (!cachedLiked.isNullOrBlank()) {
            _likedAndRecSongs.value = parseSongsJson(cachedLiked)
        }
    }

    /**
     * Called whenever a user plays a song.
     * Updates frequencies, records activity, and dynamically seeds AI Picks.
     */
    fun onSongPlayed(song: Song) {
        scope.launch {
            val freq = (playFrequencies[song.videoId] ?: 0) + 1
            playFrequencies[song.videoId] = freq

            // Prioritize frequently played tracks into the AI Picks collection
            addOrBumpSong(song)

            // Asynchronously fetch 2 contextual AI recommendations matching this song's artist or genre
            expandAiPicksFromTrack(song)
        }
    }

    /**
     * Called whenever a user likes a song.
     * Immediately prioritizes it in the AI Picks folder and discovers kindred music.
     */
    fun onSongLiked(song: Song, isLiked: Boolean) {
        scope.launch {
            if (isLiked) {
                // Instantly insert at top of AI Picks and Liked Recommendations
                val current = _aiPicksSongs.value.toMutableList()
                current.removeAll { it.videoId == song.videoId }
                current.add(0, song)
                _aiPicksSongs.value = current.take(60)
                savePicks()

                val likedRec = _likedAndRecSongs.value.toMutableList()
                likedRec.removeAll { it.videoId == song.videoId }
                likedRec.add(0, song)
                _likedAndRecSongs.value = likedRec.take(60)
                saveLiked()

                // Trigger smart kindred song expansion
                expandAiPicksFromTrack(song)
            }
        }
    }

    /**
     * Called on song skip.
     */
    fun onSongSkipped(song: Song?) {
        if (song == null) return
        scope.launch {
            // Deprioritize skipped song from current AI Picks if desired
            val current = _aiPicksSongs.value.toMutableList()
            if (current.removeIf { it.videoId == song.videoId }) {
                _aiPicksSongs.value = current
                savePicks()
            }
        }
    }

    private fun addOrBumpSong(song: Song) {
        val current = _aiPicksSongs.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.videoId == song.videoId }
        if (existingIndex >= 0) {
            val existing = current.removeAt(existingIndex)
            current.add(0, existing)
        } else {
            current.add(0, song)
        }
        _aiPicksSongs.value = current.take(60)
        savePicks()
    }

    private suspend fun expandAiPicksFromTrack(seed: Song) {
        runCatching {
            val query = "${seed.artist} songs"
            val results = YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull().orEmpty()
            val songs = results.mapNotNull {
                when (it) {
                    is SearchResult.Track -> it.song
                    is SearchResult.TopTrack -> it.song
                    else -> null
                }
            }.filter { it.videoId != seed.videoId }

            if (songs.isNotEmpty()) {
                val newRecommendations = songs.take(3)
                val current = _aiPicksSongs.value.toMutableList()
                for (rec in newRecommendations) {
                    if (current.none { it.videoId == rec.videoId }) {
                        current.add(rec)
                    }
                }
                _aiPicksSongs.value = current.take(60)
                savePicks()
            }
        }.onFailure {
            Log.w(TAG, "Failed expanding AI picks: ${it.message}")
        }
    }

    fun refreshSmartCuration() {
        scope.launch {
            // Generate contextual Daily Mix
            val mix = AiMusicEngine.getPersonalizedMix(25).getOrNull().orEmpty()
            if (mix.isNotEmpty()) {
                _dailyMixSongs.value = mix
            }

            // If AI picks is empty, seed it with contextual time vibe
            if (_aiPicksSongs.value.isEmpty()) {
                val best = AiMusicEngine.pickBestSong(AiMusicEngine.getContextualTimeVibe()).getOrNull()
                best?.song?.let { addOrBumpSong(it) }
                val queue = AiMusicEngine.generateAiQueue(AiMusicEngine.getContextualTimeVibe(), 15).getOrNull().orEmpty()
                if (queue.isNotEmpty()) {
                    val current = _aiPicksSongs.value.toMutableList()
                    current.addAll(queue)
                    _aiPicksSongs.value = current.distinctBy { it.videoId }
                    savePicks()
                }
            }
        }
    }

    fun generateSmartPicks(): List<Song> {
        return _aiPicksSongs.value.ifEmpty { _dailyMixSongs.value }
    }

    fun getSongsForBrowseId(browseId: String): List<Song> {
        return when (browseId) {
            "ai:picks" -> _aiPicksSongs.value
            "ai:daily_mix" -> _dailyMixSongs.value.ifEmpty { _aiPicksSongs.value }
            "ai:liked_rec" -> _likedAndRecSongs.value.ifEmpty { _aiPicksSongs.value }
            "ai:frequent" -> _aiPicksSongs.value
            else -> _aiPicksSongs.value
        }
    }

    private fun savePicks() {
        prefs?.edit()?.putString(KEY_AI_PICKS_JSON, songsToJson(_aiPicksSongs.value))?.apply()
    }

    private fun saveLiked() {
        prefs?.edit()?.putString(KEY_LIKED_REC_JSON, songsToJson(_likedAndRecSongs.value))?.apply()
    }

    private fun songsToJson(songs: List<Song>): String {
        val array = JSONArray()
        for (song in songs) {
            val obj = JSONObject().apply {
                put("videoId", song.videoId)
                put("title", song.title)
                put("artist", song.artist)
                put("thumbnailUrl", song.thumbnailUrl ?: "")
                put("durationText", song.durationText ?: "")
                put("artistId", song.artistId ?: "")
                put("albumId", song.albumId ?: "")
                put("albumName", song.albumName ?: "")
                put("isVideo", song.isVideo)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseSongsJson(jsonStr: String): List<Song> {
        val list = mutableListOf<Song>()
        runCatching {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Song(
                        videoId = obj.getString("videoId"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        thumbnailUrl = obj.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                        durationText = obj.optString("durationText").takeIf { it.isNotBlank() },
                        artistId = obj.optString("artistId").takeIf { it.isNotBlank() },
                        albumId = obj.optString("albumId").takeIf { it.isNotBlank() },
                        albumName = obj.optString("albumName").takeIf { it.isNotBlank() },
                        isVideo = obj.optBoolean("isVideo", false),
                    )
                )
            }
        }
        return list
    }
}
