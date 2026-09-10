package com.music.vibewave.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.vibewave.playback.AudioEqualizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEnabled by AudioEqualizer.isEnabled.collectAsStateWithLifecycle()
    val bands by AudioEqualizer.bands.collectAsStateWithLifecycle()
    val currentPreset by AudioEqualizer.currentPreset.collectAsStateWithLifecycle()
    val bassBoost by AudioEqualizer.bassBoostStrength.collectAsStateWithLifecycle()
    val virtualizer by AudioEqualizer.virtualizerStrength.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF0F111E),
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Studio Equalizer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = if (isEnabled) "Active · Studio Grade DSP" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) Color(0xFF34D399) else Color(0xFF94A3B8),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { AudioEqualizer.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF8B5CF6),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF1E2238),
                        ),
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Presets Carousel
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PRESETS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.2.sp,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AudioEqualizer.availablePresets.forEach { preset ->
                                val isSelected = currentPreset == preset
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { AudioEqualizer.applyPreset(preset) }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            brush = if (isSelected) {
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF262B46), Color(0xFF1E2238))
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                        ),
                                    color = if (isSelected) Color(0xFF241C48) else Color(0xFF151829),
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }

                    // FX Enhancements: Bass Boost & 3D Surround
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF14172B))
                            .border(1.dp, Color(0xFF242944), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "AUDIO ENHANCEMENTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.2.sp,
                        )

                        // Bass Boost
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Speaker,
                                        contentDescription = null,
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "Sub-Bass Punch",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
                                }
                                Text(
                                    text = "${(bassBoost / 10)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF43F5E),
                                )
                            }
                            Slider(
                                value = bassBoost.toFloat(),
                                onValueChange = { AudioEqualizer.setBassBoost(it.toInt()) },
                                valueRange = 0f..1000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFF43F5E),
                                    activeTrackColor = Color(0xFFF43F5E),
                                    inactiveTrackColor = Color(0xFF262A44),
                                ),
                            )
                        }

                        // 3D Virtualizer
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SurroundSound,
                                        contentDescription = null,
                                        tint = Color(0xFF06B6D4),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "3D Spatial Width",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
                                }
                                Text(
                                    text = "${(virtualizer / 10)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF06B6D4),
                                )
                            }
                            Slider(
                                value = virtualizer.toFloat(),
                                onValueChange = { AudioEqualizer.setVirtualizer(it.toInt()) },
                                valueRange = 0f..1000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF06B6D4),
                                    activeTrackColor = Color(0xFF06B6D4),
                                    inactiveTrackColor = Color(0xFF262A44),
                                ),
                            )
                        }
                    }

                    // Equalizer Frequency Bands
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF14172B))
                            .border(1.dp, Color(0xFF242944), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "FREQUENCY BANDS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.2.sp,
                            )
                            Button(
                                onClick = { AudioEqualizer.resetToFlat() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E2238),
                                    contentColor = Color(0xFFCBD5E1),
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 4.dp,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Flat", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (bands.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Start playing music to calibrate hardware bands",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            bands.forEach { band ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = band.formattedFreq,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFCBD5E1),
                                        )
                                        val levelDb = band.currentLevelDb
                                        Text(
                                            text = if (levelDb > 0) "+%.1f dB".format(levelDb) else "%.1f dB".format(levelDb),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (levelDb > 0) Color(0xFF34D399) else if (levelDb < 0) Color(0xFFF87171) else Color(0xFF94A3B8),
                                        )
                                    }
                                    Slider(
                                        value = band.currentLevelMilliBels.toFloat(),
                                        onValueChange = { AudioEqualizer.setBandLevel(band.index, it.toInt().toShort()) },
                                        valueRange = band.minLevelMilliBels.toFloat()..band.maxLevelMilliBels.toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF8B5CF6),
                                            activeTrackColor = Color(0xFF8B5CF6),
                                            inactiveTrackColor = Color(0xFF262A44),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = "Equalizer is switched off",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                        )
                        Text(
                            text = "Turn the switch ON above to sculpt your sound frequencies, punch up the bass, or enable 3D spatial widening.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { AudioEqualizer.setEnabled(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6),
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Turn On Studio Equalizer", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
