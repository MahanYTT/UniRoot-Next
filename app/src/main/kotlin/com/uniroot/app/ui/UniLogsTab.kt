package com.uniroot.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniroot.app.R
import com.uniroot.app.engine.AppUpdater
import com.uniroot.app.engine.RunLogFile
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LogFilter(val label: String) {
    ALL("All"),
    SUCCESS("Success"),
    WARNING("Warn"),
    ERROR("Error"),
    PROGRESS("Progress"),
}

@Composable
internal fun UniLogsTab(
    state: UniUiState,
    actions: UniActions,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<RunLogFile?>(null) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UniSlidingSegmentedControl(
                options = listOf(
                    SegmentedOption("Live Session (${state.logLines.size})"),
                    SegmentedOption("Run History (${state.runLogs.size})"),
                ),
                selectedIndex = subTab,
                onSelect = { subTab = it },
            )

            AnimatedContent(
                targetState = subTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "logs_subtab",
            ) { page ->
                if (page == 0) {
                    LiveLogsWorkbench(
                        lines = state.logLines,
                        running = state.running,
                    )
                } else {
                    RunHistoryList(
                        runLogs = state.runLogs,
                        scrollBehavior = scrollBehavior,
                        onOpen = actions::onRunLogOpen,
                        onShare = actions::onRunLogShare,
                        onRequestDelete = { pendingDelete = it },
                    )
                }
            }
        }
    }

    pendingDelete?.let { victim ->
        OverlayDialog(
            show = true,
            title = "Delete run log",
            onDismissRequest = { pendingDelete = null },
            content = {
                Text(
                    text = "Delete saved log \"${victim.file.name}\"?",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { pendingDelete = null },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.action_delete),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            actions.onRunLogDelete(victim)
                            pendingDelete = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        )
    }
}

@Composable
private fun LiveLogsWorkbench(
    lines: List<String>,
    running: Boolean,
) {
    val u = LocalUniColors.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(LogFilter.ALL) }
    var autoScroll by rememberSaveable { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val classified = remember(lines) { lines.map { it to formatLogTone(it) } }
    val counts = remember(classified) {
        mapOf(
            LogFilter.ALL to classified.size,
            LogFilter.SUCCESS to classified.count { it.second.second == LogTone.SUCCESS },
            LogFilter.WARNING to classified.count { it.second.second == LogTone.WARNING },
            LogFilter.ERROR to classified.count { it.second.second == LogTone.ERROR },
            LogFilter.PROGRESS to classified.count { it.second.second == LogTone.PROGRESS },
        )
    }
    val filteredLines = remember(classified, query, filter) {
        val q = query.trim()
        classified.asSequence()
            .filter { (_, pair) ->
                when (filter) {
                    LogFilter.ALL -> true
                    LogFilter.SUCCESS -> pair.second == LogTone.SUCCESS
                    LogFilter.WARNING -> pair.second == LogTone.WARNING
                    LogFilter.ERROR -> pair.second == LogTone.ERROR
                    LogFilter.PROGRESS -> pair.second == LogTone.PROGRESS
                }
            }
            .filter { (_, pair) -> q.isEmpty() || pair.first.contains(q, ignoreCase = true) }
            .map { it.first }
            .toList()
    }

    val isAtBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total == 0) true
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= total - 2
            }
        }
    }

    LaunchedEffect(filteredLines.size, autoScroll) {
        if (autoScroll && filteredLines.isNotEmpty()) {
            listState.scrollToItem(filteredLines.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextField(
            value = query,
            onValueChange = { query = it },
            label = "Filter session logs…",
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                    modifier = Modifier.padding(start = 12.dp).size(18.dp),
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = MiuixIcons.Close,
                            contentDescription = "Clear search",
                            tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogFilter.entries.forEach { f ->
                val selected = filter == f
                val count = counts[f] ?: 0
                val dotColor = when (f) {
                    LogFilter.ALL -> null
                    LogFilter.SUCCESS -> u.emerald
                    LogFilter.WARNING -> u.amber
                    LogFilter.ERROR -> u.rose
                    LogFilter.PROGRESS -> u.sky
                }
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = if (selected) u.indigoBorder else u.cardBorder,
                            shape = CircleShape,
                        )
                        .clickable { filter = f }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (dotColor != null) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                    }
                    Text(
                        text = if (count > 0 && f != LogFilter.ALL) "${f.label} ($count)" else f.label,
                        fontSize = 11.5.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (autoScroll) u.emeraldContainer else MiuixTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = if (autoScroll) u.emeraldBorder else u.cardBorder,
                        shape = CircleShape,
                    )
                    .clickable { autoScroll = !autoScroll }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (autoScroll) "Auto-scroll ON" else "Auto-scroll OFF",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (autoScroll) u.onEmeraldContainer else MiuixTheme.colorScheme.onSurfaceSecondary,
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            UniLogConsole(
                lines = filteredLines,
                listState = listState,
                emptyText = if (lines.isEmpty()) stringResource(R.string.logs_empty) else "No log lines match your filter.",
                modifier = Modifier.fillMaxSize(),
            )

            if (!isAtBottom && filteredLines.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary)
                        .clickable {
                            autoScroll = true
                            scope.launch { listState.animateScrollToItem(filteredLines.lastIndex) }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (running) "Follow live" else "Bottom",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun RunHistoryList(
    runLogs: List<RunLogFile>,
    scrollBehavior: ScrollBehavior,
    onOpen: (RunLogFile) -> Unit,
    onShare: (RunLogFile) -> Unit,
    onRequestDelete: (RunLogFile) -> Unit,
) {
    val u = LocalUniColors.current
    if (runLogs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = u.textTertiary,
                    modifier = Modifier.size(34.dp),
                )
                Text(
                    text = "No saved run logs yet",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Every root attempt is automatically archived here so you can inspect or share it.",
                    fontSize = 12.5.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
            }
        }
        return
    }

    val fmt = remember { SimpleDateFormat("MMM d, yyyy · HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(runLogs, key = { it.file.name }) { item ->
            val (railColor, pillBg, pillFg, statusLabel) = when (item.status) {
                "Success" -> Quad(u.emerald, u.emeraldContainer, u.onEmeraldContainer, "Completed")
                "Reboot" -> Quad(u.sky, u.skyContainer, u.onSkyContainer, "Reboot Required")
                "Crash" -> Quad(u.amber, u.amberContainer, u.onAmberContainer, "Crash")
                "Running" -> Quad(u.sky, u.skyContainer, u.onSkyContainer, "Running")
                "Failed-Interrupted" -> Quad(u.amber, u.amberContainer, u.onAmberContainer, "Interrupted")
                else -> Quad(u.rose, u.roseContainer, u.onRoseContainer, "Failed")
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, u.cardBorder, RoundedCornerShape(18.dp)),
                cornerRadius = 18.dp,
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
                insideMargin = PaddingValues(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clickable { onOpen(item) },
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(railColor),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = fmt.format(Date(item.timeMillis)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            UniStatusBadge(
                                text = statusLabel,
                                containerColor = pillBg,
                                contentColor = pillFg,
                            )
                        }

                        Text(
                            text = "${item.file.name} · ${AppUpdater.formatBytes(item.file.length())}",
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = u.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MiuixTheme.colorScheme.primaryContainer)
                                    .clickable { onOpen(item) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "View",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { onShare(item) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = stringResource(R.string.log_share),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(u.roseContainer)
                                    .clickable { onRequestDelete(item) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = u.rose,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
