package com.music.bitchord.data.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.ai.AiMusicEngine.toSongOrNull
import com.music.bitchord.playback.LastPlayed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Intelligent on-device AI playlist & dynamic curation manager.
 * Tracks all user activity (song plays, likes, skips, repeats) in real-time, builds user-specific taste models,
 * and curates custom folders and playlists in the Library and Home screen with dedicated glowing artwork.
 */
object AiPicksManager {
    private const val TAG = "AiPicksManager"
    private const val PREFS_NAME = "ai_picks_store"
    private const val KEY_AI_PICKS_JSON = "ai_picks_songs"
    private const val KEY_LIKED_REC_JSON = "ai_liked_rec_songs"
    private const val KEY_DAILY_MIX_JSON = "ai_daily_mix_songs"
    private const val KEY_STATION_JSON = "ai_station_songs"
    private const val KEY_FREQUENT_JSON = "ai_frequent_songs"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    data class AiFolderMeta(
        val title: String,
        val subtitle: String,
        val description: String,
        val badge: String,
    )

    private val _aiPicksSongs = MutableStateFlow<List<Song>>(emptyList())
    val aiPicksSongs = _aiPicksSongs.asStateFlow()

    private val _dailyMixSongs = MutableStateFlow<List<Song>>(emptyList())
    val dailyMixSongs = _dailyMixSongs.asStateFlow()

    private val _likedAndRecSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedAndRecSongs = _likedAndRecSongs.asStateFlow()

    private val _stationSongs = MutableStateFlow<List<Song>>(emptyList())
    val stationSongs = _stationSongs.asStateFlow()

    private val _frequentSongs = MutableStateFlow<List<Song>>(emptyList())
    val frequentSongs = _frequentSongs.asStateFlow()

    private val _circadianSongs = MutableStateFlow<List<Song>>(emptyList())
    val circadianSongs = _circadianSongs.asStateFlow()

    private val playFrequencies = mutableMapOf<String, Int>()

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadCached()
        refreshSmartCuration()
    }

    private fun loadCached() {
        prefs?.getString(KEY_AI_PICKS_JSON, null)?.let {
            if (it.isNotBlank()) _aiPicksSongs.value = parseSongsJson(it)
        }
        prefs?.getString(KEY_LIKED_REC_JSON, null)?.let {
            if (it.isNotBlank()) _likedAndRecSongs.value = parseSongsJson(it)
        }
        prefs?.getString(KEY_DAILY_MIX_JSON, null)?.let {
            if (it.isNotBlank()) _dailyMixSongs.value = parseSongsJson(it)
        }
        prefs?.getString(KEY_STATION_JSON, null)?.let {
            if (it.isNotBlank()) _stationSongs.value = parseSongsJson(it)
        }
        prefs?.getString(KEY_FREQUENT_JSON, null)?.let {
            if (it.isNotBlank()) _frequentSongs.value = parseSongsJson(it)
        }
    }

    /**
     * Called whenever a user plays a song.
     * Updates frequencies, records activity, and dynamically seeds AI Picks and Frequent.
     */
    fun onSongPlayed(song: Song) {
        scope.launch {
            val freq = (playFrequencies[song.videoId] ?: 0) + 1
            playFrequencies[song.videoId] = freq

            // 1. Add / bump in AI Picks
            addOrBumpSong(song)

            // 2. If played multiple times, add/bump in Frequent
            if (freq >= 2) {
                val currentFreq = _frequentSongs.value.toMutableList()
                currentFreq.removeAll { it.videoId == song.videoId }
                currentFreq.add(0, song)
                _frequentSongs.value = currentFreq.take(40)
                prefs?.edit()?.putString(KEY_FREQUENT_JSON, songsToJson(_frequentSongs.value))?.apply()
            }

            // 3. Asynchronously fetch 2-3 contextual AI recommendations matching this song's artist or genre
            expandAiPicksFromTrack(song)
        }
    }

    /**
     * Called whenever a user likes a song.
     * Immediately prioritizes it in the AI Picks and Heart Beats folders.
     */
    fun onSongLiked(song: Song, isLiked: Boolean) {
        scope.launch {
            if (isLiked) {
                // Instantly insert at top of AI Picks
                val current = _aiPicksSongs.value.toMutableList()
                current.removeAll { it.videoId == song.videoId }
                current.add(0, song)
                _aiPicksSongs.value = current.take(60)
                savePicks()

                // Insert into Heart Beats (liked & recommendations)
                val likedRec = _likedAndRecSongs.value.toMutableList()
                likedRec.removeAll { it.videoId == song.videoId }
                likedRec.add(0, song)
                _likedAndRecSongs.value = likedRec.take(60)
                prefs?.edit()?.putString(KEY_LIKED_REC_JSON, songsToJson(_likedAndRecSongs.value))?.apply()

                // Trigger smart kindred song expansion
                expandAiPicksFromTrack(song)
            }
        }
    }

    /**
     * Called on song skip to learn negative preference.
     */
    fun onSongSkipped(song: Song?) {
        if (song == null) return
        scope.launch {
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

    /**
     * Returns top artists derived from user activity.
     */
    fun getTopArtists(): List<String> {
        val counts = mutableMapOf<String, Int>()
        for (s in _aiPicksSongs.value) {
            if (s.artist.isNotBlank()) counts[s.artist] = (counts[s.artist] ?: 0) + 1
        }
        for (s in _likedAndRecSongs.value) {
            if (s.artist.isNotBlank()) counts[s.artist] = (counts[s.artist] ?: 0) + 2
        }
        runCatching {
            LastPlayed.load()?.songs?.forEach {
                if (it.artist.isNotBlank()) counts[it.artist] = (counts[it.artist] ?: 0) + 1
            }
        }
        return counts.entries.sortedByDescending { it.value }.take(5).map { it.key }
    }

    fun refreshSmartCuration() {
        scope.launch {
            // 1. Generate Daily Mix based on top artists + circadian flow
            val mix = AiMusicEngine.getPersonalizedMix(25).getOrNull().orEmpty()
            if (mix.isNotEmpty()) {
                _dailyMixSongs.value = mix
                prefs?.edit()?.putString(KEY_DAILY_MIX_JSON, songsToJson(mix))?.apply()
            }

            // 2. Generate Circadian Tracks based on current hour
            val timeMood = AiMusicEngine.getContextualTimeVibe()
            val circQueue = AiMusicEngine.generateAiQueue(timeMood, 20).getOrNull().orEmpty()
            if (circQueue.isNotEmpty()) {
                _circadianSongs.value = circQueue
            }

            // 3. Generate AI Station
            val station = AiMusicEngine.generateAiStation(timeMood, AiMusicEngine.Energy.BALANCED, 20).getOrNull().orEmpty()
            if (station.isNotEmpty()) {
                _stationSongs.value = station
                prefs?.edit()?.putString(KEY_STATION_JSON, songsToJson(station))?.apply()
            }

            // 4. Seed AI Picks if empty
            if (_aiPicksSongs.value.isEmpty()) {
                val seed = circQueue.ifEmpty { mix }
                if (seed.isNotEmpty()) {
                    _aiPicksSongs.value = seed
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
            "ai:station" -> _stationSongs.value.ifEmpty { _dailyMixSongs.value }
            "ai:frequent" -> _frequentSongs.value.ifEmpty { _aiPicksSongs.value }
            "ai:circadian" -> _circadianSongs.value.ifEmpty { _dailyMixSongs.value }
            else -> _aiPicksSongs.value
        }
    }

    /**
     * Guarantees songs for a given browseId, generating them on-demand if the cache is currently empty.
     */
    suspend fun ensureSongsForBrowseId(browseId: String): List<Song> = withContext(Dispatchers.IO) {
        val cached = getSongsForBrowseId(browseId)
        if (cached.isNotEmpty()) return@withContext cached

        val timeMood = AiMusicEngine.getContextualTimeVibe()
        val generated = when (browseId) {
            "ai:daily_mix" -> AiMusicEngine.getPersonalizedMix(25).getOrNull().orEmpty()
            "ai:station" -> AiMusicEngine.generateAiStation(timeMood, AiMusicEngine.Energy.BALANCED, 20).getOrNull().orEmpty()
            "ai:circadian" -> AiMusicEngine.generateAiQueue(timeMood, 20).getOrNull().orEmpty()
            "ai:liked_rec" -> {
                val topArtists = getTopArtists()
                if (topArtists.isNotEmpty()) {
                    val pool = mutableListOf<Song>()
                    for (artist in topArtists) {
                        val res = YtMusicRepository.search("$artist best songs", SearchFilter.SONGS).getOrNull().orEmpty()
                        pool.addAll(res.mapNotNull { it.toSongOrNull() })
                    }
                    pool.distinctBy { it.videoId }
                } else {
                    AiMusicEngine.generateAiQueue(AiMusicEngine.Mood.ROMANTIC, 20).getOrNull().orEmpty()
                }
            }
            "ai:frequent" -> {
                AiMusicEngine.generateAiQueue(AiMusicEngine.Mood.WORKOUT, 20).getOrNull().orEmpty()
            }
            else -> {
                AiMusicEngine.generateAiQueue(timeMood, 20).getOrNull().orEmpty()
            }
        }

        if (generated.isNotEmpty()) {
            when (browseId) {
                "ai:picks" -> { _aiPicksSongs.value = generated; savePicks() }
                "ai:daily_mix" -> { _dailyMixSongs.value = generated; prefs?.edit()?.putString(KEY_DAILY_MIX_JSON, songsToJson(generated))?.apply() }
                "ai:liked_rec" -> { _likedAndRecSongs.value = generated; prefs?.edit()?.putString(KEY_LIKED_REC_JSON, songsToJson(generated))?.apply() }
                "ai:station" -> { _stationSongs.value = generated; prefs?.edit()?.putString(KEY_STATION_JSON, songsToJson(generated))?.apply() }
                "ai:frequent" -> { _frequentSongs.value = generated; prefs?.edit()?.putString(KEY_FREQUENT_JSON, songsToJson(generated))?.apply() }
                "ai:circadian" -> { _circadianSongs.value = generated }
            }
            return@withContext generated
        }

        cached
    }

    fun getMetaForBrowseId(browseId: String): AiFolderMeta {
        return when (browseId) {
            "ai:picks" -> AiFolderMeta(
                title = "AI Smart Picks ✨",
                subtitle = "Tuned to your listening flow & favorite artists",
                description = "Intelligent on-device neural selection tuned to your play history, likes, and acoustic flow.",
                badge = "AI PICKS",
            )
            "ai:daily_mix" -> AiFolderMeta(
                title = "Daily AI Mix 🔮",
                subtitle = "Circadian rhythm & top artists",
                description = "Fresh dynamic daily sequence adapted to time of day and your most frequently enjoyed music genres.",
                badge = "DAILY MIX",
            )
            "ai:liked_rec" -> AiFolderMeta(
                title = "Heart Beats AI 💖",
                subtitle = "Liked tracks & kindred acoustic vibes",
                description = "Expanded acoustic universe radiating from songs you have marked as favorites.",
                badge = "HEART BEATS",
            )
            "ai:station" -> AiFolderMeta(
                title = "AI Smart Station 🚀",
                subtitle = "20-track dynamic acoustic journey",
                description = "Harmonically sequenced AI radio flow matching your energy and current mood.",
                badge = "SMART STATION",
            )
            "ai:frequent" -> AiFolderMeta(
                title = "Heavy Rotation AI ⚡",
                subtitle = "Your most repeated tracks",
                description = "High-replay tracks and energetic favorites that keep your rhythm going.",
                badge = "HEAVY ROTATION",
            )
            "ai:circadian" -> AiFolderMeta(
                title = "Circadian Wave 🌅",
                subtitle = "Contextual morning, afternoon & night flow",
                description = "Real-time diurnal acoustic mood tracking for peak focus, motivation, and peaceful rest.",
                badge = "CIRCADIAN",
            )
            else -> AiFolderMeta(
                title = "AI Smart Curation ✨",
                subtitle = "Personalized neural music",
                description = "AI recommendations crafted from your taste profile.",
                badge = "AI SMART",
            )
        }
    }

    private fun savePicks() {
        prefs?.edit()?.putString(KEY_AI_PICKS_JSON, songsToJson(_aiPicksSongs.value))?.apply()
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
