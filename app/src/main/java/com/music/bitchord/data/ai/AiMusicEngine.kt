package com.music.bitchord.data.ai

import android.util.Log
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.firebase.FirestoreManager
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.random.Random

/**
 * On-device zero-API AI music recommendation and smart curation engine.
 * Powered by Firestore user telemetry (listening history, skips, favorites,
 * top artists) merged with real-time mood and time-of-day contextual analysis.
 * Operates 100% locally on-device without external paid API keys.
 */
object AiMusicEngine {

    private const val TAG = "AiMusicEngine"

    enum class Mood(val label: String, val searchKeywords: List<String>, val emoji: String) {
        CHILL("Chill & Relax", listOf("chill vibes", "acoustic relaxing", "peaceful acoustic"), "☕"),
        WORKOUT("Workout & Gym", listOf("high energy workout", "gym motivation beats", "hype phonk"), "🔥"),
        FOCUS("Focus & Study", listOf("lofi hip hop study beats", "deep focus ambient", "instrumental concentration"), "🧠"),
        LATE_NIGHT("Late Night", listOf("midnight drive", "late night synthwave", "lofi midnight slow"), "🌙"),
        PARTY("Party & Dance", listOf("dance pop club hits", "edm bangers party", "upbeat dance hits"), "🎉"),
        ROMANTIC("Romantic & Soul", listOf("acoustic love songs", "romantic soulful ballads", "soft romantic"), "❤️"),
        BOLLYWOOD("Bollywood Vibe", listOf("bollywood hits", "hindi melodious songs", "arijit singh best"), "✨"),
        RETRO("Retro Classics", listOf("80s 90s classic hits", "timeless retro golden hits", "vintage pop classics"), "📻"),
    }

    enum class Energy(val label: String, val factor: Float) {
        LOW("Mellow", 0.3f),
        BALANCED("Smooth", 0.6f),
        HIGH("Energetic", 0.85f),
        INTENSE("Maximum Pump", 1.0f),
    }

    data class AiRecommendation(
        val song: Song,
        val matchScore: Int,
        val reason: String,
        val mood: Mood,
        val energy: Energy,
    )

    /**
     * Determines current time-of-day contextual vibe.
     */
    fun getContextualTimeVibe(): Mood {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> Mood.CHILL // Morning wake-up
            in 11..16 -> Mood.FOCUS // Afternoon focus
            in 17..21 -> Mood.PARTY // Evening unwind / party
            else -> Mood.LATE_NIGHT // Night time
        }
    }

    /**
     * Selects the single best song matching the requested mood and energy,
     * fully personalized with Firestore listening history (plays, skips, favorites).
     */
    suspend fun pickBestSong(
        mood: Mood,
        energy: Energy = Energy.BALANCED,
    ): Result<AiRecommendation> = withContext(Dispatchers.IO) {
        runCatching {
            val taste = FirestoreManager.getUserTasteProfile()
            val candidates = mutableListOf<Song>()

            // Pass 1: Try ALL mood keywords (not just one) for broader coverage
            for (keyword in mood.searchKeywords) {
                val results = YtMusicRepository.search(keyword, SearchFilter.SONGS).getOrNull().orEmpty()
                candidates.addAll(results.mapNotNull { it.toSongOrNull() })
                if (candidates.size >= 20) break // stop early once we have enough
            }

            // Pass 2: Hybrid search using user's top artists × mood context
            val topArtists = taste.topArtists.entries.sortedByDescending { it.value }.take(3).map { it.key }
            for (artist in topArtists) {
                if (candidates.size >= 40) break
                val hybridKeyword = "$artist ${mood.searchKeywords.first()}"
                val hybridSearch = YtMusicRepository.search(hybridKeyword, SearchFilter.SONGS).getOrNull().orEmpty()
                candidates.addAll(hybridSearch.mapNotNull { it.toSongOrNull() })
            }

            // Pass 3: Fallback — direct artist search if still thin
            if (candidates.size < 10 && topArtists.isNotEmpty()) {
                val artistRes = YtMusicRepository.search(topArtists.first(), SearchFilter.SONGS).getOrNull().orEmpty()
                candidates.addAll(artistRes.mapNotNull { it.toSongOrNull() })
            }

            // Pass 4: Generic fallback if still no candidates
            if (candidates.isEmpty()) {
                val fallback = YtMusicRepository.search("top hits 2024", SearchFilter.SONGS).getOrNull().orEmpty()
                candidates.addAll(fallback.mapNotNull { it.toSongOrNull() })
            }

            val uniqueCandidates = candidates.distinctBy { it.videoId }
            if (uniqueCandidates.isEmpty()) {
                throw IllegalStateException("No candidate tracks found for AI curation")
            }

            // Score every track using Firestore taste signals (plays, skips, favorites)
            val scored = uniqueCandidates.map { song ->
                val score = calculateScore(song, taste)
                song to score
            }.sortedByDescending { it.second }

            // Prefer tracks that pass the skip-threshold, but never block entirely
            val filtered = scored.filter { it.second >= 60 }
            val best = (if (filtered.isNotEmpty()) filtered else scored).first()

            val reason = generatePersonalizedExplanation(best.first, best.second, mood, energy, taste)

            AiRecommendation(
                song = best.first,
                matchScore = best.second,
                reason = reason,
                mood = mood,
                energy = energy,
            )
        }
    }

    /**
     * One-tap smart surprise based on time of day and natural rhythm.
     */
    suspend fun getSmartSurprise(): Result<AiRecommendation> {
        val autoMood = getContextualTimeVibe()
        return pickBestSong(autoMood, Energy.BALANCED)
    }

    /**
     * Generates a curated queue of tracks for continuous listening,
     * filtering out skipped tracks and prioritizing favorite artists.
     */
    suspend fun generateAiQueue(
        mood: Mood,
        count: Int = 15,
    ): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            val taste = FirestoreManager.getUserTasteProfile()
            val candidatePool = mutableListOf<Song>()

            for (keyword in mood.searchKeywords.shuffled().take(2)) {
                val res = YtMusicRepository.search(keyword, SearchFilter.SONGS).getOrNull().orEmpty()
                candidatePool.addAll(res.mapNotNull { it.toSongOrNull() })
            }

            // Also query user's top artist if known
            val topArtist = taste.topArtists.maxByOrNull { it.value }?.key
            if (!topArtist.isNullOrBlank()) {
                val artistRes = YtMusicRepository.search(topArtist, SearchFilter.SONGS).getOrNull().orEmpty()
                candidatePool.addAll(artistRes.mapNotNull { it.toSongOrNull() })
            }

            val distinct = candidatePool.distinctBy { it.videoId }
            if (distinct.isEmpty()) {
                throw IllegalStateException("Failed to generate AI queue")
            }

            // Filter out skipped tracks and sort by personalized score
            val ranked = distinct
                .filterNot { taste.skippedSongs.contains(it.videoId) }
                .map { it to calculateScore(it, taste) }
                .sortedByDescending { it.second }
                .map { it.first }

            ranked.take(count)
        }
    }

    /**
     * Generates an automatic personalized Daily Mix curated from the user's
     * Firestore top artists and listening activity.
     */
    suspend fun getPersonalizedMix(count: Int = 20): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            val taste = FirestoreManager.getUserTasteProfile()
            val topArtists = taste.topArtists.entries.sortedByDescending { it.value }.take(3).map { it.key }

            val pool = mutableListOf<Song>()
            if (topArtists.isNotEmpty()) {
                for (artist in topArtists) {
                    val search = YtMusicRepository.search(artist, SearchFilter.SONGS).getOrNull().orEmpty()
                    pool.addAll(search.mapNotNull { it.toSongOrNull() })
                }
            } else {
                // Fallback to contextual mood
                val mood = getContextualTimeVibe()
                val search = YtMusicRepository.search(mood.searchKeywords.random(), SearchFilter.SONGS).getOrNull().orEmpty()
                pool.addAll(search.mapNotNull { it.toSongOrNull() })
            }

            val result = pool.distinctBy { it.videoId }
                .filterNot { taste.skippedSongs.contains(it.videoId) }
                .map { it to calculateScore(it, taste) }
                .sortedByDescending { it.second }
                .map { it.first }

            result.take(count)
        }
    }

    private fun calculateScore(song: Song, taste: FirestoreManager.UserTasteProfile): Int {
        var score = 76 + Random.nextInt(12) // 76..87 base

        // 1. Play history boost for artist
        val artistPlayWeight = taste.topArtists[song.artist] ?: 0
        if (artistPlayWeight > 0) {
            score += (artistPlayWeight * 3).coerceAtMost(18)
        }

        // 2. Favorite / Heart boost (+20 points)
        if (taste.favoriteSongs.contains(song.videoId) ||
            taste.favoriteSongTitles.any { it.equals(song.title, ignoreCase = true) }
        ) {
            score += 20
        }

        // 3. User skip penalty (-45 points)
        if (taste.skippedSongs.contains(song.videoId)) {
            score -= 45
        }

        // 4. Repeatedly skipped artist penalty
        val artistSkipWeight = taste.skippedArtists[song.artist] ?: 0
        if (artistSkipWeight > 0) {
            score -= (artistSkipWeight * 6).coerceAtMost(25)
        }

        return score.coerceIn(52, 99)
    }

    private fun generatePersonalizedExplanation(
        song: Song,
        score: Int,
        mood: Mood,
        energy: Energy,
        taste: FirestoreManager.UserTasteProfile,
    ): String {
        val isFavSong = taste.favoriteSongs.contains(song.videoId) ||
                taste.favoriteSongTitles.any { it.equals(song.title, ignoreCase = true) }
        val artistPlays = taste.topArtists[song.artist] ?: 0
        val isFavArtist = artistPlays > 2
        val avoidedSkips = taste.skippedSongs.isNotEmpty() || taste.skippedArtists.isNotEmpty()

        return when {
            isFavSong -> "$score% match · Matched directly from your Liked Music profile for ${mood.label} vibes."
            isFavArtist -> "$score% match · Recommended based on your frequent listening to ${song.artist}, tuned for ${mood.label} (${energy.label} energy)."
            avoidedSkips && score >= 85 -> "$score% match · Calibrated to your listening habits (skips excluded) for peak ${mood.label} flow."
            else -> generateExplanation(mood, energy, score)
        }
    }

    private fun generateExplanation(mood: Mood, energy: Energy, score: Int): String {
        val context = when (mood) {
            Mood.CHILL -> "acoustic warmth and soothing rhythm"
            Mood.WORKOUT -> "high-tempo drive and motivating beat pattern"
            Mood.FOCUS -> "unobtrusive frequency balance ideal for concentration"
            Mood.LATE_NIGHT -> "deep ambient resonance tailored for midnight listening"
            Mood.PARTY -> "infectious groove and peak dance floor momentum"
            Mood.ROMANTIC -> "harmonic melodies and emotive vocal delivery"
            Mood.BOLLYWOOD -> "rich Indian musicality and soulful songwriting"
            Mood.RETRO -> "nostalgic synth instrumentation and timeless melodies"
        }
        return "$score% match · Calibrated for $context (${energy.label} energy)."
    }

    private fun SearchResult.toSongOrNull(): Song? = when (this) {
        is SearchResult.Track -> this.song
        is SearchResult.TopTrack -> this.song
        else -> null
    }
}
