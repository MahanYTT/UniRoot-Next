package com.uniroot.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniroot.app.BuildConfig
import com.uniroot.app.R
import com.uniroot.app.engine.AppUpdateUiState
import com.uniroot.app.engine.CompatibilityLevel
import com.uniroot.app.engine.DeviceInfoPanel
import com.uniroot.app.newmethod.KsudUpdateCheckState
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
internal fun UniHomeTab(
    state: UniUiState,
    actions: UniActions,
    scrollBehavior: ScrollBehavior,
    onOpenProfileSheet: () -> Unit,
    onAddProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.47f)
                        .fillMaxSize()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "app_update") {
                        AppUpdateHomeBanner(state = state, actions = actions)
                    }
                    item(key = "hero") {
                        HeroStatusCard(state = state, actions = actions)
                    }
                    item(key = "device") {
                        DeviceTelemetryCard(
                            state = state,
                            actions = actions,
                            defaultExpanded = true,
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(0.53f)
                        .fillMaxSize()
                        .overScrollVertical(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "deck") {
                        ControlDeckCard(state = state, actions = actions)
                    }
                    item(key = "cta") {
                        PrimaryActionBlock(state = state, actions = actions)
                    }
                    item(key = "method_cfg") {
                        ActiveMethodConfigSection(
                            state = state,
                            actions = actions,
                            onOpenProfileSheet = onOpenProfileSheet,
                            onAddProfile = onAddProfile,
                        )
                    }
                    item(key = "footer") { HomeFooter() }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "app_update") {
                    AppUpdateHomeBanner(state = state, actions = actions)
                }
                item(key = "hero") {
                    HeroStatusCard(state = state, actions = actions)
                }
                item(key = "device") {
                    DeviceTelemetryCard(
                        state = state,
                        actions = actions,
                        defaultExpanded = false,
                    )
                }
                item(key = "deck") {
                    ControlDeckCard(state = state, actions = actions)
                }
                item(key = "cta") {
                    PrimaryActionBlock(state = state, actions = actions)
                }
                item(key = "method_cfg") {
                    ActiveMethodConfigSection(
                        state = state,
                        actions = actions,
                        onOpenProfileSheet = onOpenProfileSheet,
                        onAddProfile = onAddProfile,
                    )
                }
                item(key = "footer") { HomeFooter() }
            }
        }
    }
}

@Composable
private fun AppUpdateHomeBanner(
    state: UniUiState,
    actions: UniActions,
) {
    val u = LocalUniColors.current
    val avail = state.appUpdateState as? AppUpdateUiState.Available
    val visible = avail != null && state.appUpdateBannerDismissedTag != avail.release.tag

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + expandVertically(tween(240)),
        exit = fadeOut(tween(160)) + shrinkVertically(tween(200)),
    ) {
        val release = avail?.release ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(u.indigoContainer)
                .border(1.dp, u.indigoBorder, RoundedCornerShape(16.dp))
                .clickable { actions.onOpenAppUpdateSheet() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(u.indigo.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdateAlt,
                        contentDescription = null,
                        tint = u.indigo,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Update available · ${release.tag}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = u.onIndigoContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Tap to inspect release notes & install",
                        fontSize = 12.sp,
                        color = u.onIndigoContainer.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary)
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_update_inspect),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { actions.onDismissAppUpdateBanner(release.tag) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Close,
                        contentDescription = "Dismiss",
                        tint = u.onIndigoContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatusCard(
    state: UniUiState,
    actions: UniActions,
) {
    val u = LocalUniColors.current
    val verdict = state.compatibility
    val isRooted = state.rooted || state.telemetry.rootedLive
    val isRunning = state.running

    val (targetWashStart, targetWashEnd, targetBorder, targetAccent, targetChipBg, targetChipFg) = when {
        isRunning -> HeroPalette(u.skyContainer, MiuixTheme.colorScheme.surface, u.skyBorder, u.sky, u.sky.copy(alpha = 0.16f), u.onSkyContainer)
        isRooted -> HeroPalette(u.emeraldContainer, u.emeraldWashEnd, u.emeraldBorder, u.emerald, u.emerald.copy(alpha = 0.16f), u.onEmeraldContainer)
        verdict.level == CompatibilityLevel.COMPATIBLE -> HeroPalette(u.indigoContainer, u.indigoWashEnd, u.indigoBorder, u.indigo, u.indigo.copy(alpha = 0.14f), u.onIndigoContainer)
        verdict.level == CompatibilityLevel.CAUTION -> HeroPalette(u.amberContainer, u.amberWashEnd, u.amberBorder, u.amber, u.amber.copy(alpha = 0.16f), u.onAmberContainer)
        else -> HeroPalette(u.roseContainer, u.roseWashEnd, u.roseBorder, u.rose, u.rose.copy(alpha = 0.16f), u.onRoseContainer)
    }

    val washStart by animateColorAsState(targetWashStart, tween(320), label = "hero_ws")
    val washEnd by animateColorAsState(targetWashEnd, tween(320), label = "hero_we")
    val border by animateColorAsState(targetBorder, tween(320), label = "hero_bd")
    val accent by animateColorAsState(targetAccent, tween(320), label = "hero_ac")

    val statusPill = when {
        isRunning -> "ROOTING"
        isRooted -> "ROOTED"
        verdict.level == CompatibilityLevel.COMPATIBLE -> "READY"
        verdict.level == CompatibilityLevel.CAUTION -> "ACTION NEEDED"
        else -> "UNSUPPORTED"
    }
    val pulseMode = when {
        isRunning || isRooted -> PulseMode.RADAR
        verdict.level == CompatibilityLevel.COMPATIBLE -> PulseMode.BREATHE
        else -> PulseMode.STATIC
    }
    val headline = when {
        isRunning -> if (state.newMethod) (state.newMethodStage.ifBlank { "Running DirtyFrag…" }) else "Running Exploit…"
        isRooted -> stringResource(R.string.status_rooted)
        else -> verdict.headline
    }
    val subtitle = when {
        isRunning -> if (state.newMethod) "Progress ${state.newMethodProgress}% · Tap progress bar to view console" else "Executing legacy pipeline…"
        isRooted -> buildString {
            append(state.telemetry.rootDetail.ifBlank { "su uid=0 active" })
            val mgr = state.telemetry.managerForFlavor(state.ksuNextMode) ?: state.telemetry.installedManagers.firstOrNull()
            if (mgr != null) append(" · ${mgr.label} ${mgr.versionName}")
            if (state.telemetry.selinuxState.isNotBlank() && state.telemetry.selinuxState != "Checking…") {
                append(" · SELinux ${state.telemetry.selinuxState}")
            }
        }
        else -> verdict.reason
    }
    val methodTag = if (state.newMethod) {
        val kmi = state.telemetry.kmi ?: "Non-GKI"
        "DirtyFrag · $kmi"
    } else {
        state.selectedProfileName ?: "Old Exploit"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(washStart, washEnd)))
            .border(1.dp, border, RoundedCornerShape(20.dp)),
    ) {
        // Subtle ambient watermark anchored bottom-end
        Icon(
            imageVector = if (isRooted) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield,
            contentDescription = null,
            tint = accent.copy(alpha = 0.08f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 6.dp)
                .size(86.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    UniPulseBeacon(color = accent, mode = pulseMode, size = 16.dp)
                    Text(
                        text = statusPill,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                        color = accent,
                    )
                }
                UniStatusBadge(
                    text = methodTag,
                    containerColor = targetChipBg,
                    contentColor = targetChipFg,
                    monospace = state.newMethod,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = headline,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Quick rooted actions
            if (isRooted && !isRunning) {
                val bootActive = state.newMethodBootEnabled || state.instantRootEnabled || state.autoRootOnBoot
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val mgrLabel = if (state.ksuNextMode) "Open KSU Next" else "Open KernelSU"
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(u.emerald.copy(alpha = 0.16f))
                            .border(0.75.dp, u.emeraldBorder, RoundedCornerShape(10.dp))
                            .clickable { actions.onOpenActiveManager() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = u.onEmeraldContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = mgrLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = u.onEmeraldContainer,
                        )
                    }
                    if (bootActive) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.7f))
                                .border(0.75.dp, u.cardBorder, RoundedCornerShape(10.dp))
                                .clickable { actions.onDisableRoot() }
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PowerSettingsNew,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Disable boot root",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class HeroPalette(
    val washStart: Color,
    val washEnd: Color,
    val border: Color,
    val accent: Color,
    val chipBg: Color,
    val chipFg: Color,
)

@Composable
private fun DeviceTelemetryCard(
    state: UniUiState,
    actions: UniActions,
    defaultExpanded: Boolean,
) {
    val u = LocalUniColors.current
    val t = state.telemetry
    val verdict = state.compatibility
    var expanded by rememberSaveable(defaultExpanded) { mutableStateOf(defaultExpanded) }
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "chev",
    )

    val (badgeBg, badgeFg) = when (verdict.level) {
        CompatibilityLevel.COMPATIBLE -> u.emeraldContainer to u.onEmeraldContainer
        CompatibilityLevel.CAUTION -> u.amberContainer to u.onAmberContainer
        CompatibilityLevel.UNSUPPORTED -> u.roseContainer to u.onRoseContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Clickable Glanceable Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = t.marketingName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (t.modelCode.isNotBlank() && !t.marketingName.contains(t.modelCode, ignoreCase = true)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MiuixTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = t.modelCode,
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        val osLine = listOfNotNull(
                            t.socName.takeIf { it.isNotBlank() },
                            t.oneUiVersion?.let { "One UI $it" },
                            "Android ${t.androidVersion}",
                        ).joinToString(" · ")
                        Text(
                            text = osLine,
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    UniStatusBadge(
                        text = verdict.badge,
                        containerColor = badgeBg,
                        contentColor = badgeFg,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = chevronAngle },
                    )
                }

                // 5-Chip Scannable Compatibility Matrix
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val kmiSupported = t.kmi != null && t.kmi in DeviceInfoPanel.BUNDLED_DF_KMIS &&
                        !DeviceInfoPanel.DF_UNSUPPORTED_MODELS.any { t.modelCode.startsWith(it, true) }
                    UniCompatChip(
                        label = "KMI",
                        value = t.kmi ?: "Non-GKI",
                        accentColor = if (kmiSupported) u.emerald else u.rose,
                        containerColor = if (kmiSupported) u.emeraldContainer else u.roseContainer,
                    )
                    UniCompatChip(
                        label = "ABI",
                        value = t.abi,
                        accentColor = if (t.is64Bit) MiuixTheme.colorScheme.onSurface else u.rose,
                        containerColor = MiuixTheme.colorScheme.surfaceVariant,
                    )
                    t.knoxState?.let {
                        val tripped = t.knoxTripped == true
                        UniCompatChip(
                            label = "Knox",
                            value = if (tripped) "0x1" else "0x0",
                            accentColor = if (tripped) u.amber else u.emerald,
                            containerColor = if (tripped) u.amberContainer else u.emeraldContainer,
                        )
                    }
                    UniCompatChip(
                        label = "SELinux",
                        value = t.selinuxState,
                        accentColor = when (t.selinuxState) {
                            "Permissive" -> u.amber
                            "Enforcing" -> MiuixTheme.colorScheme.onSurface
                            else -> MiuixTheme.colorScheme.onSurfaceSecondary
                        },
                        containerColor = MiuixTheme.colorScheme.surfaceVariant,
                    )
                    val activeMgr = t.managerForFlavor(state.ksuNextMode) ?: t.installedManagers.firstOrNull()
                    UniCompatChip(
                        label = "Manager",
                        value = activeMgr?.let { "${if (it.isNext) "Next" else "Classic"} ${it.versionName}" } ?: "Missing",
                        accentColor = if (activeMgr != null) u.emerald else u.amber,
                        containerColor = if (activeMgr != null) u.emeraldContainer else u.amberContainer,
                    )
                }
            }

            // Expanded Diagnostic Drawer
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200)) + expandVertically(spring(dampingRatio = 0.88f, stiffness = 400f)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
            ) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)

                        // OS & Security 2-column grid
                        UniSectionEyebrow("OS & Security")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            UniInfoRow(
                                label = "Android / One UI",
                                value = listOfNotNull("Android ${t.androidVersion}", t.oneUiVersion?.let { "One UI $it" }).joinToString(" · "),
                                modifier = Modifier.weight(1f),
                            )
                            UniInfoRow(
                                label = "Security Patch",
                                value = t.securityPatch,
                                monospace = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            UniInfoRow(
                                label = "Bootloader",
                                value = t.bootloaderState,
                                modifier = Modifier.weight(1f),
                            )
                            UniInfoRow(
                                label = "Knox / SELinux",
                                value = listOfNotNull(t.knoxState?.let { "Knox $it" }, t.selinuxState).joinToString(" · "),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        UniInfoRow(
                            label = "Kernel Release",
                            value = t.kernelRelease + (t.kmi?.let { "  [$it]" } ?: "  [Non-GKI]"),
                            monospace = true,
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)

                        // Root & Managers
                        UniSectionEyebrow("Root & Managers")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            UniInfoRow(
                                label = "Live Root",
                                value = t.rootState,
                                valueColor = if (t.rootedLive) u.emerald else MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            UniInfoRow(
                                label = "Installed Manager",
                                value = t.installedManagers
                                    .joinToString("\n") { "${it.label} ${it.versionName} (${it.versionCode})" }
                                    .ifEmpty { "None detected" },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)

                        // Hardware & Runtime
                        UniSectionEyebrow("Hardware & Runtime")
                        if (t.ramTotalBytes > 0L) {
                            val ramFrac = t.ramUsedBytes.toFloat() / t.ramTotalBytes.toFloat()
                            UniCapacityBar(
                                label = "Memory (RAM)",
                                detail = "${DeviceInfoPanel.fmtBytesGb(t.ramAvailBytes)} free / ${DeviceInfoPanel.fmtBytesGb(t.ramTotalBytes)}",
                                fraction = ramFrac,
                                barColor = u.indigo,
                            )
                        }
                        if (t.storageTotalBytes > 0L) {
                            val stFrac = t.storageUsedBytes.toFloat() / t.storageTotalBytes.toFloat()
                            UniCapacityBar(
                                label = "Internal Storage",
                                detail = "${DeviceInfoPanel.fmtBytesGb(t.storageUsedBytes)} / ${DeviceInfoPanel.fmtBytesGb(t.storageTotalBytes)}",
                                fraction = stFrac,
                                barColor = u.sky,
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            UniInfoRow("Display", t.displaySummary, modifier = Modifier.weight(1f))
                            UniInfoRow("Battery / Uptime", "${t.batterySummary} · ${t.uptimeSummary}", modifier = Modifier.weight(1f))
                        }
                        UniInfoRow(
                            label = "Build Fingerprint",
                            value = t.fingerprint,
                            monospace = true,
                        )

                        // Action Footer
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { actions.onCopyDiagnostics() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Copy,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Copy Diagnostics",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                )
                            }
                            val spin = rememberSpinAngle(state.telemetryRefreshing)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                    .clickable(enabled = !state.telemetryRefreshing) { actions.onRefreshTelemetry() }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer { rotationZ = spin },
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Refresh",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Unified Exploit & Flavor Control Deck:
 * - Exploit Method: NEW EXPLOIT FIRST & DEFAULT | Old Exploit second
 * - KernelSU Flavor: KERNELSU NEXT FIRST & DEFAULT | KernelSU Classic second
 */
@Composable
private fun ControlDeckCard(
    state: UniUiState,
    actions: UniActions,
) {
    val u = LocalUniColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
        insideMargin = PaddingValues(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            UniSectionEyebrow("Exploit Method")
            Spacer(Modifier.height(8.dp))
            UniSlidingSegmentedControl(
                options = listOf(
                    SegmentedOption("New Exploit", "Fast · DirtyFrag"),
                    SegmentedOption("Old Exploit", "Legacy · Profiles"),
                ),
                selectedIndex = if (state.newMethod) 0 else 1,
                onSelect = { idx -> actions.onRootMethodChanged(idx == 0) },
                enabled = !state.running,
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
            Spacer(Modifier.height(14.dp))

            UniSectionEyebrow("KernelSU Flavor")
            Spacer(Modifier.height(8.dp))
            UniSlidingSegmentedControl(
                options = listOf(
                    SegmentedOption("KernelSU Next"),
                    SegmentedOption("KernelSU"),
                ),
                selectedIndex = if (state.ksuNextMode) 0 else 1,
                onSelect = { idx -> actions.onKsuFlavorChanged(idx == 0) },
                enabled = !state.running,
            )
        }
    }
}

@Composable
private fun PrimaryActionBlock(
    state: UniUiState,
    actions: UniActions,
) {
    val u = LocalUniColors.current
    val isRooted = state.rooted || state.telemetry.rootedLive
    val verdict = state.compatibility
    val canRun = when {
        state.running -> false
        isRooted -> false
        state.newMethod -> state.telemetry.loaded && verdict.level != CompatibilityLevel.UNSUPPORTED
        else -> state.selectedProfileName != null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedContent(
            targetState = state.running,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
            label = "cta_morph",
        ) { running ->
            if (running) {
                val frac by animateFloatAsState(
                    targetValue = (state.newMethodProgress / 100f).coerceIn(0.05f, 1f),
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    label = "run_bar",
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(u.skyContainer)
                        .border(1.dp, u.skyBorder, RoundedCornerShape(16.dp))
                        .clickable { actions.onReopenExecutionSheet() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (state.newMethod) state.newMethodStage.ifBlank { "Rooting…" } else "Running legacy exploit…",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = u.onSkyContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.newMethod) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${state.newMethodProgress}%",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = u.sky,
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = if (state.newMethod) frac else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                val (label, icon) = when {
                    isRooted -> stringResource(R.string.action_already_root) to Icons.Rounded.CheckCircle
                    state.newMethod && !state.telemetry.loaded -> "Scanning device…" to Icons.Rounded.Refresh
                    !state.newMethod && verdict.badge == "Shizuku Required" -> "Start Shizuku First" to Icons.Rounded.WarningAmber
                    !canRun -> "Not Supported" to Icons.Rounded.Block
                    else -> stringResource(R.string.action_run) to Icons.Rounded.Bolt
                }
                UniTactilePrimaryButton(
                    text = label,
                    onClick = actions::onRun,
                    enabled = canRun,
                    icon = icon,
                    containerColor = if (!state.newMethod && verdict.badge == "Shizuku Required") u.amber else MiuixTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = if (isRooted) u.emeraldContainer else MiuixTheme.colorScheme.disabledPrimaryButton,
                    disabledContentColor = if (isRooted) u.onEmeraldContainer else MiuixTheme.colorScheme.disabledOnPrimaryButton,
                )
            }
        }

        if (!isRooted && !state.running && state.newMethod && verdict.suggestOldExploit) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant)
                    .clickable { actions.onRootMethodChanged(false) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Switch to Old Exploit profiles →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = u.indigo,
                )
            }
        }
    }
}

@Composable
private fun ActiveMethodConfigSection(
    state: UniUiState,
    actions: UniActions,
    onOpenProfileSheet: () -> Unit,
    onAddProfile: () -> Unit,
) {
    AnimatedContent(
        targetState = state.newMethod,
        transitionSpec = {
            (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 18 }) togetherWith
                fadeOut(tween(120)) using SizeTransform(clip = false)
        },
        label = "method_cfg",
    ) { isNew ->
        if (isNew) {
            NewMethodConfigCards(state = state, actions = actions)
        } else {
            OldMethodConfigCards(
                state = state,
                actions = actions,
                onOpenProfileSheet = onOpenProfileSheet,
                onAddProfile = onAddProfile,
            )
        }
    }
}

@Composable
private fun NewMethodConfigCards(
    state: UniUiState,
    actions: UniActions,
) {
    val u = LocalUniColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Card A: ksud Profile + Calm Smart Update Strip (NO loud blue button when up to date!)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
            cornerRadius = 20.dp,
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
            insideMargin = PaddingValues(0.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.ksuNextMode && state.ksudNextProfiles.isNotEmpty()) {
                    OverlaySpinnerPreference(
                        title = stringResource(R.string.ksud_next_profile_label),
                        items = state.ksudNextProfiles.map { DropdownItem(icon = null, title = it.label) },
                        selectedIndex = state.ksudNextProfiles
                            .indexOfFirst { it.id == state.ksudNextProfileId }
                            .coerceAtLeast(0),
                        showValue = true,
                        onSelectedIndexChange = { idx ->
                            state.ksudNextProfiles.getOrNull(idx)?.let { actions.onKsudNextProfileChanged(it.id) }
                        },
                    )
                } else if (!state.ksuNextMode && state.ksudClassicProfiles.isNotEmpty()) {
                    OverlaySpinnerPreference(
                        title = stringResource(R.string.ksud_next_profile_label),
                        items = state.ksudClassicProfiles.map { DropdownItem(icon = null, title = it.label) },
                        selectedIndex = state.ksudClassicProfiles
                            .indexOfFirst { it.id == state.ksudClassicProfileId }
                            .coerceAtLeast(0),
                        showValue = true,
                        onSelectedIndexChange = { idx ->
                            state.ksudClassicProfiles.getOrNull(idx)?.let { actions.onKsudClassicProfileChanged(it.id) }
                        },
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)

                SmartKsudUpdateStrip(
                    ksuNextMode = state.ksuNextMode,
                    checkState = state.ksudCheckState,
                    updating = state.ksudUpdateRunning,
                    onRefreshCheck = { actions.onCheckKsudUpdates(forceRefresh = true) },
                    onUpdateNow = { actions.onUpdateKsud(forceRepatch = false) },
                )
            }
        }

        // Card B: Boot & Soft-Reboot Behavior
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
            cornerRadius = 20.dp,
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
            insideMargin = PaddingValues(0.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SwitchPreference(
                    checked = state.newMethodBootEnabled,
                    onCheckedChange = actions::onNewMethodBootChanged,
                    title = stringResource(R.string.new_method_boot_title),
                    summary = stringResource(R.string.new_method_boot_summary),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MiuixTheme.colorScheme.dividerLine,
                )
                SwitchPreference(
                    checked = state.newMethodAutoSoftReboot,
                    onCheckedChange = actions::onNewMethodSoftRebootChanged,
                    title = stringResource(R.string.new_method_soft_reboot_title),
                    summary = stringResource(R.string.new_method_soft_reboot_summary),
                )
            }
        }
    }
}

/**
 * Calm-by-default ksud update strip:
 * - Up to date: subtle emerald check + version + quiet refresh button (NO loud blue CTA!).
 * - Update available: expands cleanly into an Indigo update row with an "Update & Patch" button.
 */
@Composable
private fun SmartKsudUpdateStrip(
    ksuNextMode: Boolean,
    checkState: KsudUpdateCheckState,
    updating: Boolean,
    onRefreshCheck: () -> Unit,
    onUpdateNow: () -> Unit,
) {
    val u = LocalUniColors.current
    val flavorLabel = if (ksuNextMode) "ksud Next" else "ksud Classic"

    AnimatedContent(
        targetState = checkState,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) using SizeTransform(clip = false) },
        label = "ksud_strip",
    ) { st ->
        when (st) {
            is KsudUpdateCheckState.UpdateAvailable -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(u.indigoContainer)
                        .border(1.dp, u.indigoBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "$flavorLabel update",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = u.onIndigoContainer,
                            )
                            Text(
                                text = "v${st.localVersion} → ${st.remoteTag}",
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = u.indigo,
                            )
                        }
                        Text(
                            text = "Patch upstream binary with 8 Samsung KDP+DEFEX modules",
                            fontSize = 11.5.sp,
                            color = u.onIndigoContainer.copy(alpha = 0.78f),
                            maxLines = 2,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MiuixTheme.colorScheme.primary)
                            .clickable(enabled = !updating) { onUpdateNow() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (updating) "Patching…" else "Update",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
            is KsudUpdateCheckState.Checking -> {
                val spin = rememberSpinAngle(true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = spin },
                    )
                    Text(
                        text = "Checking upstream $flavorLabel (local v${st.localVersion})…",
                        fontSize = 12.5.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            is KsudUpdateCheckState.UpToDate -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = u.emerald,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "$flavorLabel up to date · v${st.localVersion}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = onRefreshCheck) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Check for ksud update",
                            tint = u.textTertiary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
            is KsudUpdateCheckState.Error -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = u.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "$flavorLabel v${st.localVersion} · ${st.message}",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Retry",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = u.indigo,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRefreshCheck() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            KsudUpdateCheckState.Idle -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Tap to check latest $flavorLabel release",
                        fontSize = 12.5.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onRefreshCheck) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Check for ksud update",
                            tint = u.indigo,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OldMethodConfigCards(
    state: UniUiState,
    actions: UniActions,
    onOpenProfileSheet: () -> Unit,
    onAddProfile: () -> Unit,
) {
    val u = LocalUniColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Card A: Device Profile Picker + Add/Manage
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
            cornerRadius = 20.dp,
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
            insideMargin = PaddingValues(0.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.profiles.isNotEmpty()) {
                    OverlaySpinnerPreference(
                        title = stringResource(R.string.profile_label),
                        items = state.profiles.map { DropdownItem(icon = null, title = it.name) },
                        selectedIndex = state.profiles.indexOfFirst { it.name == state.selectedProfileName }.coerceAtLeast(0),
                        showValue = true,
                        onSelectedIndexChange = actions::onProfileSelected,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_profile_selected),
                        fontSize = 13.5.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onAddProfile() }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.action_add_profile),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onOpenProfileSheet() }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Manage",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                        )
                    }
                }
            }
        }

        // Card B: Shizuku Toggle + Live Binder Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
            cornerRadius = 20.dp,
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
            insideMargin = PaddingValues(0.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SwitchPreference(
                    checked = state.shizukuEnabled,
                    onCheckedChange = actions::onShizukuChanged,
                    title = stringResource(R.string.shizuku_label),
                    summary = stringResource(R.string.shizuku_summary),
                )
                HorizontalDivider(thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
                val (shizText, shizColor, shizBg) = when {
                    state.shizukuActive && state.shizukuPermitted ->
                        Triple("Shizuku binder active · Permission granted", u.onEmeraldContainer, u.emeraldContainer)
                    state.shizukuActive ->
                        Triple("Shizuku running · Permission required", u.onAmberContainer, u.amberContainer)
                    else ->
                        Triple("Shizuku not running", MiuixTheme.colorScheme.onSurfaceSecondary, MiuixTheme.colorScheme.surfaceVariant)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UniStatusBadge(text = shizText, containerColor = shizBg, contentColor = shizColor)
                }
            }
        }

        // Card C: Legacy Tools Jump Link
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, u.cardBorder, RoundedCornerShape(20.dp)),
            cornerRadius = 20.dp,
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
            insideMargin = PaddingValues(0.dp),
        ) {
            ArrowPreference(
                title = "Legacy Exploit Tools",
                summary = "Root My Galaxy feed, Instant Root, Auto-root & Latest KSU",
                onClick = actions::onJumpToLegacySettings,
            )
        }
    }
}

@Composable
private fun HomeFooter() {
    Text(
        text = "UniRoot Next v${BuildConfig.VERSION_NAME} · ${stringResource(R.string.made_by)}",
        fontSize = 11.5.sp,
        color = LocalUniColors.current.textTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
    )
}
