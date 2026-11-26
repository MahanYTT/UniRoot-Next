package com.uniroot.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniroot.app.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class SegmentedOption(
    val title: String,
    val subtitle: String? = null,
)

internal data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Calm, precision sliding-thumb segmented control using `BoxWithConstraints` + spring physics.
 * Reserves the primary brand accent for CTAs rather than flooding toggles with solid blue.
 */
@Composable
internal fun UniSlidingSegmentedControl(
    options: List<SegmentedOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val u = LocalUniColors.current
    val haptic = LocalHapticFeedback.current
    val safeIndex = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    val hasSubtitle = options.any { !it.subtitle.isNullOrBlank() }
    val height = if (hasSubtitle) 52.dp else 42.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(u.trackSurface)
            .padding(4.dp),
    ) {
        val segmentWidth = if (options.isNotEmpty()) maxWidth / options.size else maxWidth
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * safeIndex,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 480f),
            label = "seg_offset",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(u.trackThumb)
                .border(0.75.dp, u.trackThumbBorder, RoundedCornerShape(10.dp)),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { idx, opt ->
                val selected = idx == safeIndex
                val titleColor by animateColorAsState(
                    targetValue = when {
                        !enabled -> MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        selected -> MiuixTheme.colorScheme.onSurface
                        else -> MiuixTheme.colorScheme.onSurfaceSecondary
                    },
                    animationSpec = tween(180),
                    label = "seg_title_$idx",
                )
                val subColor by animateColorAsState(
                    targetValue = if (selected) u.indigo else u.textTertiary,
                    animationSpec = tween(180),
                    label = "seg_sub_$idx",
                )
                val interaction = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            enabled = enabled,
                            interactionSource = interaction,
                            indication = null,
                        ) {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(idx)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = opt.title,
                        fontSize = 13.5.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!opt.subtitle.isNullOrBlank()) {
                        Text(
                            text = opt.subtitle,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun UniStatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
    icon: ImageVector? = null,
    monospace: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .then(
                if (borderColor != Color.Transparent) Modifier.border(0.75.dp, borderColor, CircleShape)
                else Modifier
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal enum class PulseMode { RADAR, BREATHE, STATIC }

/** Animated Canvas beacon used on the Hero Status Card. */
@Composable
internal fun UniPulseBeacon(
    color: Color,
    mode: PulseMode,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val transition = rememberInfiniteTransition(label = "pulse_beacon")
    val radarProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar",
    )
    val breatheAlpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxR = this.size.minDimension / 2f
        val coreR = maxR * 0.44f
        when (mode) {
            PulseMode.RADAR -> {
                val ringR = coreR + (maxR - coreR) * radarProgress
                val ringAlpha = (1f - radarProgress) * 0.52f
                drawCircle(color = color.copy(alpha = ringAlpha), radius = ringR, center = center)
                drawCircle(color = color, radius = coreR, center = center)
            }
            PulseMode.BREATHE -> {
                drawCircle(color = color.copy(alpha = breatheAlpha * 0.35f), radius = maxR * 0.82f, center = center)
                drawCircle(color = color, radius = coreR, center = center)
            }
            PulseMode.STATIC -> {
                drawCircle(color = color.copy(alpha = 0.22f), radius = maxR * 0.78f, center = center)
                drawCircle(color = color, radius = coreR, center = center)
            }
        }
    }
}

@Composable
internal fun UniCompatChip(
    label: String,
    value: String,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    val u = LocalUniColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = u.textTertiary,
            maxLines = 1,
        )
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun UniCapacityBar(
    label: String,
    detail: String,
    fraction: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val u = LocalUniColors.current
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "cap_bar",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
            )
            Text(
                text = detail,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(u.trackSurface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor),
            )
        }
    }
}

@Composable
internal fun UniTactilePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color = MiuixTheme.colorScheme.primary,
    contentColor: Color = MiuixTheme.colorScheme.onPrimary,
    disabledContainerColor: Color = MiuixTheme.colorScheme.disabledPrimaryButton,
    disabledContentColor: Color = MiuixTheme.colorScheme.disabledOnPrimaryButton,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.985f else 1f,
        animationSpec = tween(90),
        label = "btn_scale",
    )
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        cornerRadius = 16.dp,
        minHeight = 54.dp,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            color = containerColor,
            disabledColor = disabledContainerColor,
        ),
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        val fg = if (enabled) contentColor else disabledContentColor
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun UniSectionEyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    val u = LocalUniColors.current
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.7.sp,
        color = u.textTertiary,
        modifier = modifier,
    )
}

@Composable
internal fun UniInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    valueColor: Color = MiuixTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = LocalUniColors.current.textTertiary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value.ifBlank { "—" },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            color = valueColor,
            lineHeight = 17.sp,
        )
    }
}

/** Docked 3-tab bottom navigation bar (`Home`, `Logs`, `Settings`) with live indicator dots. */
@Composable
internal fun UniBottomNavBar(
    selectedTab: UniTab,
    rooted: Boolean,
    running: Boolean,
    hasAppUpdate: Boolean,
    onSelect: (UniTab) -> Unit,
) {
    val u = LocalUniColors.current
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface),
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val items = listOf(
                Triple(UniTab.HOME, stringResource(R.string.tab_home), if (rooted) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield),
                Triple(UniTab.LOGS, stringResource(R.string.tab_logs), Icons.Rounded.Terminal),
                Triple(UniTab.SETTINGS, stringResource(R.string.tab_settings), Icons.Rounded.Tune),
            )
            for ((tab, label, icon) in items) {
                val selected = tab == selectedTab
                val pillColor by animateColorAsState(
                    targetValue = if (selected) MiuixTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = tween(200),
                    label = "nav_pill_${tab.name}",
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selected) u.indigo else MiuixTheme.colorScheme.onSurfaceSecondary,
                    animationSpec = tween(200),
                    label = "nav_icon_${tab.name}",
                )
                val labelColor by animateColorAsState(
                    targetValue = if (selected) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceSecondary,
                    animationSpec = tween(200),
                    label = "nav_lbl_${tab.name}",
                )
                val showDot = (tab == UniTab.LOGS && running) || (tab == UniTab.SETTINGS && hasAppUpdate)
                val dotColor = if (tab == UniTab.LOGS) u.sky else u.indigo

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tab)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(pillColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = iconTint,
                            modifier = Modifier.size(21.dp),
                        )
                        if (showDot) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 12.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor),
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = labelColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** High-contrast obsidian syntax-highlighted console reused across Logs Tab & Execution Sheet. */
@Composable
internal fun UniLogConsole(
    lines: List<String>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    emptyText: String = stringResource(R.string.logs_empty),
) {
    val u = LocalUniColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(u.consoleBg)
            .border(1.dp, u.consoleBorder, RoundedCornerShape(16.dp)),
    ) {
        if (lines.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = u.consoleDefault.copy(alpha = 0.35f),
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = emptyText,
                    fontSize = 12.5.sp,
                    color = u.consoleDefault.copy(alpha = 0.62f),
                )
            }
        } else {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(lines) { raw ->
                        val (clean, tone) = formatLogTone(raw)
                        val color = when (tone) {
                            LogTone.SUCCESS -> u.consoleSuccess
                            LogTone.WARNING -> u.consoleWarning
                            LogTone.ERROR -> u.consoleError
                            LogTone.PROGRESS -> u.consoleProgress
                            LogTone.DEFAULT -> u.consoleDefault
                        }
                        Text(
                            text = clean,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberSpinAngle(spinning: Boolean): Float {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin_deg",
    )
    return if (spinning) angle else 0f
}
