package com.music.vibewave.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.music.vibewave.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EqualizerBand(
    val index: Short,
    val centerFreqMilliHz: Int,
    val minLevelMilliBels: Short,
    val maxLevelMilliBels: Short,
    val currentLevelMilliBels: Short,
) {
    val centerFreqHz: Int get() = centerFreqMilliHz / 1000
    val formattedFreq: String
        get() = if (centerFreqHz >= 1000) {
            val khz = centerFreqHz / 1000f
            if (khz % 1.0f == 0f) "${khz.toInt()}k" else "%.1fk".format(khz)
        } else {
            "$centerFreqHz"
        }
    val currentLevelDb: Float get() = currentLevelMilliBels / 100f
}

object AudioEqualizer {
    private const val TAG = "AudioEqualizer"
    private const val PREFS_NAME = "vibewave_equalizer_prefs"
    private const val KEY_ENABLED = "eq_enabled"
    private const val KEY_PRESET = "eq_preset"
    private const val KEY_BASS_BOOST = "eq_bass_boost"
    private const val KEY_VIRTUALIZER = "eq_virtualizer"
    private const val KEY_BAND_PREFIX = "eq_band_"

    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = 0

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _bands = MutableStateFlow<List<EqualizerBand>>(emptyList())
    val bands: StateFlow<List<EqualizerBand>> = _bands.asStateFlow()

    private val _currentPreset = MutableStateFlow("Flat")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0) // 0 .. 1000
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(0) // 0 .. 1000
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    val availablePresets: List<String> = listOf(
        "Flat",
        "Deep Bass",
        "Bass & Treble",
        "Rock",
        "Club EDM",
        "Pop / Vocal",
        "Acoustic",
        "Classical",
        "Custom",
    )

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p

        _isEnabled.value = p.getBoolean(KEY_ENABLED, false)
        _currentPreset.value = p.getString(KEY_PRESET, "Flat") ?: "Flat"
        _bassBoostStrength.value = p.getInt(KEY_BASS_BOOST, 0)
        _virtualizerStrength.value = p.getInt(KEY_VIRTUALIZER, 0)

        // Observe AppSettings.audioSessionId to automatically attach when playback initializes
        scope.launch {
            AppSettings.audioSessionId.collect { sessionId ->
                if (sessionId > 0 && sessionId != currentSessionId) {
                    attachSession(sessionId)
                }
            }
        }
    }

    @Synchronized
    fun attachSession(sessionId: Int) {
        if (sessionId <= 0) return
        currentSessionId = sessionId
        releaseEffects()

        runCatching {
            val eq = Equalizer(0, sessionId)
            val bb = BassBoost(0, sessionId)
            val virt = Virtualizer(0, sessionId)

            equalizer = eq
            bassBoost = bb
            virtualizer = virt

            // Apply enabled state
            val enabled = _isEnabled.value
            eq.enabled = enabled
            bb.enabled = enabled && bb.strengthSupported
            virt.enabled = enabled && virt.strengthSupported

            // Restore bass & virtualizer
            if (bb.strengthSupported) {
                bb.setStrength(_bassBoostStrength.value.toShort())
            }
            if (virt.strengthSupported) {
                virt.setStrength(_virtualizerStrength.value.toShort())
            }

            // Read bands and restore levels
            refreshBandsFromHardware(eq)
        }.onFailure { e ->
            Log.e(TAG, "Failed to attach AudioEqualizer to session $sessionId", e)
        }
    }

    private fun refreshBandsFromHardware(eq: Equalizer) {
        val count = runCatching { eq.numberOfBands }.getOrDefault(0.toShort())
        if (count <= 0) return
        val range = runCatching { eq.bandLevelRange }.getOrNull() ?: shortArrayOf(-1500, 1500)
        val minLevel = range[0]
        val maxLevel = range[1]

        val bandList = mutableListOf<EqualizerBand>()
        for (i in 0 until count) {
            val bandIdx = i.toShort()
            val centerFreq = runCatching { eq.getCenterFreq(bandIdx) }.getOrDefault(1000 * (i + 1) * 200)
            val savedLevel = prefs?.getInt("$KEY_BAND_PREFIX$i", Int.MIN_VALUE) ?: Int.MIN_VALUE
            val currentLevel = if (savedLevel != Int.MIN_VALUE) {
                val clamped = savedLevel.coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                runCatching { eq.setBandLevel(bandIdx, clamped) }
                clamped
            } else {
                runCatching { eq.getBandLevel(bandIdx) }.getOrDefault(0.toShort())
            }

            bandList.add(
                EqualizerBand(
                    index = bandIdx,
                    centerFreqMilliHz = centerFreq,
                    minLevelMilliBels = minLevel,
                    maxLevelMilliBels = maxLevel,
                    currentLevelMilliBels = currentLevel,
                )
            )
        }
        _bands.value = bandList
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()

        runCatching {
            equalizer?.enabled = enabled
            bassBoost?.let {
                if (it.strengthSupported) it.enabled = enabled
            }
            virtualizer?.let {
                if (it.strengthSupported) it.enabled = enabled
            }
        }
    }

    fun setBandLevel(bandIndex: Short, milliBels: Short) {
        val eq = equalizer
        val currentList = _bands.value.toMutableList()
        val index = currentList.indexOfFirst { it.index == bandIndex }
        if (index != -1) {
            val band = currentList[index]
            val clamped = milliBels.coerceIn(band.minLevelMilliBels, band.maxLevelMilliBels)
            currentList[index] = band.copy(currentLevelMilliBels = clamped)
            _bands.value = currentList

            runCatching { eq?.setBandLevel(bandIndex, clamped) }
            prefs?.edit()?.putInt("$KEY_BAND_PREFIX$bandIndex", clamped.toInt())?.apply()
            _currentPreset.value = "Custom"
            prefs?.edit()?.putString(KEY_PRESET, "Custom")?.apply()
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassBoostStrength.value = clamped
        prefs?.edit()?.putInt(KEY_BASS_BOOST, clamped)?.apply()

        runCatching {
            bassBoost?.let {
                if (it.strengthSupported) it.setStrength(clamped.toShort())
            }
        }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _virtualizerStrength.value = clamped
        prefs?.edit()?.putInt(KEY_VIRTUALIZER, clamped)?.apply()

        runCatching {
            virtualizer?.let {
                if (it.strengthSupported) it.setStrength(clamped.toShort())
            }
        }
    }

    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        prefs?.edit()?.putString(KEY_PRESET, presetName)?.apply()

        if (presetName == "Custom") return

        val bandList = _bands.value
        if (bandList.isEmpty()) return

        // Normalized frequency gains (-1.0 to 1.0)
        val normalizedGains: List<Float> = when (presetName) {
            "Deep Bass" -> listOf(0.8f, 0.6f, 0.3f, 0.1f, 0.0f, 0.0f, 0.0f, 0.0f)
            "Bass & Treble" -> listOf(0.7f, 0.4f, 0.0f, -0.2f, 0.0f, 0.3f, 0.5f, 0.7f)
            "Rock" -> listOf(0.5f, 0.3f, 0.1f, -0.1f, 0.1f, 0.3f, 0.5f, 0.6f)
            "Club EDM" -> listOf(0.7f, 0.5f, 0.2f, -0.1f, 0.0f, 0.3f, 0.6f, 0.7f)
            "Pop / Vocal" -> listOf(-0.1f, 0.1f, 0.4f, 0.5f, 0.4f, 0.2f, 0.1f, 0.0f)
            "Acoustic" -> listOf(0.3f, 0.2f, 0.1f, 0.2f, 0.3f, 0.4f, 0.3f, 0.2f)
            "Classical" -> listOf(0.4f, 0.3f, 0.2f, 0.0f, 0.0f, 0.2f, 0.3f, 0.4f)
            else -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) // Flat
        }

        val updatedBands = bandList.mapIndexed { idx, band ->
            val ratio = if (bandList.size > 1) idx.toFloat() / (bandList.size - 1) else 0.5f
            val sampleIdx = (ratio * (normalizedGains.size - 1)).toInt().coerceIn(0, normalizedGains.size - 1)
            val gainFactor = normalizedGains[sampleIdx]

            val targetLevel = if (gainFactor >= 0) {
                (gainFactor * band.maxLevelMilliBels).toInt()
                    .coerceIn(band.minLevelMilliBels.toInt(), band.maxLevelMilliBels.toInt()).toShort()
            } else {
                (gainFactor.coerceAtLeast(-1.0f) * -band.minLevelMilliBels).toInt()
                    .coerceIn(band.minLevelMilliBels.toInt(), band.maxLevelMilliBels.toInt()).toShort()
            }

            runCatching { equalizer?.setBandLevel(band.index, targetLevel) }
            prefs?.edit()?.putInt("$KEY_BAND_PREFIX${band.index}", targetLevel.toInt())?.apply()
            band.copy(currentLevelMilliBels = targetLevel)
        }
        _bands.value = updatedBands

        when (presetName) {
            "Deep Bass" -> {
                setBassBoost(650)
                setVirtualizer(100)
            }
            "Club EDM" -> {
                setBassBoost(500)
                setVirtualizer(300)
            }
            "Bass & Treble" -> {
                setBassBoost(400)
                setVirtualizer(200)
            }
            "Rock" -> {
                setBassBoost(250)
                setVirtualizer(150)
            }
            "Pop / Vocal" -> {
                setBassBoost(150)
                setVirtualizer(150)
            }
            "Acoustic" -> {
                setBassBoost(100)
                setVirtualizer(100)
            }
            "Classical" -> {
                setBassBoost(0)
                setVirtualizer(250)
            }
            else -> { // "Flat"
                setBassBoost(0)
                setVirtualizer(0)
            }
        }
    }

    fun resetToFlat() {
        applyPreset("Flat")
    }

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
