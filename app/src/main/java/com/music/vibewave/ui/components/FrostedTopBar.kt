package com.music.vibewave.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFireDepartment
import com.music.vibewave.data.firebase.ListenTogetherManager
import com.music.vibewave.data.gamification.VibeRewardsManager
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.music.vibewave.BuildConfig
import com.music.vibewave.R
import com.music.vibewave.data.model.Account
import com.music.vibewave.data.settings.AppSettings

/**
 * The bar's own height, above whatever inset it is sitting under.
 *
 * The single source of truth for it: the bar lays itself out to this, and
 * everything that has to clear the bar — page content padding, [TopFadeBlur]'s
 * ramp, fixed headers that sit directly beneath it — measures from here rather
 * than from a copy of the number.
 */
val TopBarContentHeight = 52.dp

/**
 * The breathing room between the bar's bottom edge and the first thing under
 * it, so content rests below the glass instead of against it.
 */
val TopBarContentGap = 12.dp

/**
 * How far down the window the bar actually ends: the status bar inset it is
 * pinned under, plus its own height.
 *
 * This has to be read at composition rather than baked in as a constant — the
 * inset is a property of the device and of the window, not of the app. A phone
 * with a cutout, one without, and a freeform window with no status bar at all
 * are all different numbers, and a fixed guess is wrong on all but one of them:
 * too tight and content is clipped under the bar, too loose and every page
 * opens on a band of empty space.
 */
@Composable
fun topBarHeight(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarContentHeight

/**
 * Where page content should start: clear of the bar, plus [TopBarContentGap].
 */
@Composable
fun topBarContentPadding(): Dp = topBarHeight() + TopBarContentGap

/**
 * The top bar's content — title, back affordance, actions — over no backdrop
 * of its own.
 *
 * The glass behind it is [TopFadeBlur]'s, drawn underneath: a blur that starts
 * full at the status bar and ramps to nothing below, so the bar has no bottom
 * edge to draw a line across the page with. A uniform pane would put that line
 * back, which is the one thing every surface here is built to avoid.
 *
 * The exception is Reduce dynamic blur, where there is no fade to sit on and
 * the bar fills itself solid instead — title over raw scrolling content is
 * unreadable, so something has to carry it.
 *
 * Apple Music behaviour: the big in-list header owns the title at rest;
 * once the list scrolls, the small centered title fades in.
 */
@Composable
fun FrostedTopBar(
    title: String,
    scrolled: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    refreshing: Boolean = false,
    // A lambda, not a value: the drag changes every frame, and reading it in
    // the caller would recompose the whole app on each one.
    pullFraction: () -> Float = { 0f },
    onCommunityClick: (() -> Unit)? = null,
    onListenTogetherClick: (() -> Unit)? = null,
    onLeaderboardClick: (() -> Unit)? = null,
    onAiChatClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val titleAlpha by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(220),
        label = "topBarTitleAlpha",
    )
    // Only the solid bar wants a hairline under it. A faded one has no edge for
    // the line to mark, and drawing it there would be inventing the very seam
    // the fade exists to remove.
    val dividerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.outline.copy(
            alpha = if (scrolled && reduceDynamicBlur) 0.6f else 0f,
        ),
        animationSpec = tween(220),
        label = "topBarDivider",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (reduceDynamicBlur) Modifier.background(MaterialTheme.colorScheme.surface)
                else Modifier,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(TopBarContentHeight),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    // Reserve room for the back button and the actions so a
                    // long title truncates instead of running under them.
                    .padding(horizontal = 96.dp)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = titleAlpha },
            )
            // On a pushed page the back affordance is always visible, since
            // there is no large in-list header to fall back on.
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // App Logo Icon
                    Image(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = "VibeWave",
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onLeaderboardClick?.invoke() },
                    )

                    // 1. Daily Streak Pill (Directly replaces "VibeWave" text)
                    if (onLeaderboardClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val rewardProfile by VibeRewardsManager.profile.collectAsStateWithLifecycle()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFFFF4757).copy(alpha = 0.35f),
                                            Color(0xFFFFA502).copy(alpha = 0.25f),
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            Color(0xFFFF4757).copy(alpha = 0.85f),
                                            Color(0xFFFFD32A).copy(alpha = 0.85f),
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .clickable { onLeaderboardClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "Daily Streak",
                                    tint = Color(0xFFFF4757),
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${rewardProfile.streakDays}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                    ),
                                    color = Color(0xFFFFD32A),
                                )
                            }
                        }
                    }

                    // 2. Engagement Center / Hub Pill
                    if (onCommunityClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6C5CE7).copy(alpha = 0.35f), Color(0xFF00CEC9).copy(alpha = 0.20f))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF00CEC9).copy(alpha = 0.6f), Color(0xFF6C5CE7).copy(alpha = 0.4f))
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .clickable { onCommunityClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Forum,
                                    contentDescription = "Engagement Hub",
                                    tint = Color(0xFF81ECEC),
                                    modifier = Modifier.size(13.5.dp),
                                )
                                Spacer(modifier = Modifier.width(3.5.dp))
                                Text(
                                    text = "Hub",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                    ),
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    // 3. Listen Together Pill
                    if (onListenTogetherClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val isRoomActive by ListenTogetherManager.isInRoom.collectAsStateWithLifecycle()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF00D9FF).copy(alpha = if (isRoomActive) 0.45f else 0.22f),
                                            Color(0xFF6C63FF).copy(alpha = if (isRoomActive) 0.45f else 0.15f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            Color(0xFF00D9FF).copy(alpha = if (isRoomActive) 0.9f else 0.5f),
                                            Color(0xFF6C63FF).copy(alpha = if (isRoomActive) 0.8f else 0.3f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .clickable { onListenTogetherClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isRoomActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00FFA3)),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(
                                        Icons.Rounded.WifiTethering,
                                        contentDescription = "Listen Together",
                                        tint = Color(0xFF00D9FF),
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(modifier = Modifier.width(3.5.dp))
                                }
                                Text(
                                    text = if (isRoomActive) "Live" else "Together",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                    ),
                                    color = Color.White,
                                )
                            }
                        }
                    }
                    if (BuildConfig.FLAVOR == "dev") {
                        Text(
                            text = "Dev",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onAiChatClick != null) {
                    IconButton(
                        onClick = onAiChatClick,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = "VibeWave AI Companion",
                            tint = Color(0xFF00CEC9),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }
                actions()
            }
        }
        // The divider and the loader line share the bar's bottom edge; the box
        // only grows to the line's height while a refresh is actually showing.
        Box(Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            RefreshLine(
                refreshing = refreshing,
                pullFraction = pullFraction,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * The account affordance at the right end of the bar.
 *
 * It is the signed-in Google account's own photo — the same one YouTube Music
 * shows there — and tapping it opens Settings, where the account lives. Signed
 * out, or before the account menu has come back, it falls back to a person
 * glyph on a filled circle so the tap target never disappears.
 *
 * The hairline ring is what keeps a photo with light edges from dissolving into
 * the bar's glass; it is the same one thumbnails elsewhere carry.
 */
@Composable
fun TopBarAccountButton(
    account: Account?,
    onClick: () -> Unit,
    onSwipeProfile: ((forward: Boolean) -> Boolean)? = null,
    modifier: Modifier = Modifier,
) {
    val translation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // Wrapped in an IconButton so it keeps the 48dp target, the ripple and the
    // spacing every other action in this bar has.
    IconButton(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer { translationY = translation.value }
            .pointerInput(onSwipeProfile) {
                if (onSwipeProfile == null) return@pointerInput
                var drag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, amount -> change.consume(); drag += amount },
                    onDragEnd = {
                        if (kotlin.math.abs(drag) < 28f) return@detectVerticalDragGestures
                        if (!onSwipeProfile.invoke(drag > 0f)) scope.launch {
                            translation.snapTo(if (drag > 0f) 9f else -9f)
                            translation.animateTo(0f, spring())
                        }
                    },
                )
            },
    ) {
        val photo = account?.thumbnailUrl
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = stringResource(R.string.switch_account),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .thumbnailBorder(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .thumbnailBorder(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = stringResource(R.string.switch_account),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * The refresh indicator: a line along the bottom of the bar, directly under the
 * status bar. It tracks the drag on the way down — filling left to right as the
 * pull approaches the threshold — then sweeps indefinitely once the refresh is
 * away, so the two phases read as one continuous gesture.
 */
@Composable
private fun RefreshLine(refreshing: Boolean, pullFraction: () -> Float, modifier: Modifier = Modifier) {
    val fraction = pullFraction()
    val pulling = fraction > 0.01f
    AnimatedVisibility(
        visible = refreshing || pulling,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(220)),
        modifier = modifier,
    ) {
        val lineModifier = Modifier
            .fillMaxWidth()
            .height(LINE_HEIGHT)
        if (refreshing) {
            LinearProgressIndicator(
                modifier = lineModifier,
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
            )
        } else {
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = lineModifier,
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

private val LINE_HEIGHT = 2.5.dp

/**
 * The account photo's diameter.
 *
 * Smaller than an icon's 24dp box: a filled circle carries more weight than a
 * glyph does, and at 24 it sat heavier in the bar than the wordmark opposite it.
 */
private val AVATAR_SIZE = 28.dp
