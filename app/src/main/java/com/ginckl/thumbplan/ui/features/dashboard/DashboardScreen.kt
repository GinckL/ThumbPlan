package com.ginckl.thumbplan.ui.features.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ginckl.thumbplan.Insights
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.data.local.entity.UserEntity
import com.ginckl.thumbplan.domain.RepeatMode
import com.ginckl.thumbplan.ui.Format
import com.ginckl.thumbplan.ui.components.BrandGradient
import com.ginckl.thumbplan.ui.components.EmptyState
import com.ginckl.thumbplan.ui.components.Pill
import com.ginckl.thumbplan.ui.components.ProgressRing
import com.ginckl.thumbplan.ui.theme.Danger
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.Success
import com.ginckl.thumbplan.ui.theme.Typography
import com.ginckl.thumbplan.util.PermissionHealth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class Tab(val label: String) { TODAY("Today"), UPCOMING("Upcoming"), ALL("All"), DONE("Done") }

private fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

fun parseColorOrNull(hex: String?): Color? =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }

@Composable
fun DashboardScreen(
    tasks: List<PlanTaskEntity>,
    user: UserEntity,
    avatarEmoji: String,
    boostActive: Boolean,
    insights: Insights,
    permissions: PermissionHealth,
    onFixPermissions: () -> Unit,
    onTaskClick: (PlanTaskEntity) -> Unit,
    onTaskComplete: (PlanTaskEntity) -> Unit,
    onTaskDelete: (PlanTaskEntity) -> Unit,
    onAddTask: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }

    val today = LocalDate.now()
    val now = System.currentTimeMillis()

    val pending = remember(tasks) { tasks.filter { it.status != "completed" } }
    val done = remember(tasks) { tasks.filter { it.status == "completed" }.sortedByDescending { it.scheduledTime } }
    val counts = mapOf(
        Tab.TODAY to pending.count { localDate(it.scheduledTime) <= today },
        Tab.UPCOMING to pending.count { localDate(it.scheduledTime) > today },
        Tab.ALL to pending.size,
        Tab.DONE to done.size
    )

    val tab = Tab.entries[tabIndex]
    val visible = remember(tasks, tab, query) {
        val base = when (tab) {
            Tab.TODAY -> pending.filter { localDate(it.scheduledTime) <= today }
            Tab.UPCOMING -> pending.filter { localDate(it.scheduledTime) > today }
            Tab.ALL -> pending
            Tab.DONE -> done
        }
        val q = query.trim()
        if (q.isEmpty()) base else base.filter {
            it.title.contains(q, true) || (it.description?.contains(q, true) == true) || (it.category?.contains(q, true) == true)
        }
    }

    val todayAll = tasks.filter { localDate(it.scheduledTime) == today }
    val todayDone = todayAll.count { it.status == "completed" }
    val overdueCount = pending.count { it.scheduledTime < now }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.spacingMd, end = Dimens.spacingMd,
            top = contentPadding.calculateTopPadding() + Dimens.spacingSm,
            bottom = contentPadding.calculateBottomPadding() + Dimens.fabSize + Dimens.spacingXl
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        item(key = "header") {
            Header(user = user, avatarEmoji = avatarEmoji, boostActive = boostActive, searching = searching, onSearch = { searching = !searching; if (!searching) query = "" })
        }
        item(key = "hero") {
            HeroCard(done = todayDone, total = todayAll.size, overdue = overdueCount, streak = insights.currentStreak)
        }
        if (!permissions.allGood) {
            item(key = "permissions") { PermissionBanner(permissions, onFixPermissions) }
        }
        item(key = "search") {
            AnimatedVisibility(visible = searching) {
                SearchField(query = query, onChange = { query = it })
            }
        }
        item(key = "tabs") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                items(Tab.entries.toList()) { t ->
                    TabChip(
                        text = t.label,
                        count = counts[t] ?: 0,
                        selected = t == tab,
                        onClick = { tabIndex = t.ordinal }
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                when {
                    query.isNotBlank() -> EmptyState("🔍", "No matches", "Nothing found for “$query”.")
                    tab == Tab.DONE -> EmptyState("🏁", "Nothing completed yet", "Finish a task and it will show up here.")
                    tab == Tab.TODAY -> EmptyState("🎉", if (todayAll.isEmpty()) "A clean slate" else "All done for today!", "Plan something small — small steps, big results.") {
                        TextButton(onClick = onAddTask) { Text("Add a task") }
                    }
                    else -> EmptyState("🗓️", "Nothing planned", "Create a task and ThumbPlan will wake you up for it.") {
                        TextButton(onClick = onAddTask) { Text("Add a task") }
                    }
                }
            }
        } else {
            val grouped = visible.groupBy { if (localDate(it.scheduledTime) < today && tab != Tab.DONE) null else localDate(it.scheduledTime) }
            // null key == earlier days (overdue) – shown first
            val keys = grouped.keys.sortedWith { a, b -> when { a == null && b == null -> 0; a == null -> -1; b == null -> 1; else -> a.compareTo(b) } }
            val orderedKeys = if (tab == Tab.DONE) keys.reversed() else keys
            orderedKeys.forEach { day ->
                val list = grouped.getValue(day)
                val label = if (day == null) "Overdue" else Format.date(day, today)
                if (tab != Tab.TODAY || day == null) {
                    item(key = "h_${day ?: "overdue"}") {
                        Text(
                            label,
                            style = Typography.labelLarge,
                            color = if (day == null) Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Dimens.spacingSm)
                        )
                    }
                }
                items(list, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onComplete = { onTaskComplete(task) },
                        onDelete = { onTaskDelete(task) },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(user: UserEntity, avatarEmoji: String, boostActive: Boolean, searching: Boolean, onSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(avatarEmoji, style = Typography.headlineMedium, modifier = Modifier.padding(end = 10.dp))
        Column(Modifier.weight(1f)) {
            Text(Format.greeting(), style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                user.name,
                style = Typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onSearch) {
            Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
        }
        if (boostActive) {
            Surface(color = Color(0xFFFBBF24).copy(alpha = 0.2f), shape = RoundedCornerShape(Dimens.radiusPill), modifier = Modifier.padding(end = 6.dp)) {
                Text("⚡ 2×", style = Typography.labelLarge, color = Color(0xFFB45309), modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(Dimens.radiusPill), shadowElevation = 1.dp) {
            Text(
                "🪙 ${"%,d".format(user.totalCoins)}",
                style = Typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun HeroCard(done: Int, total: Int, overdue: Int, streak: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusLg))
            .background(BrandGradient)
            .padding(Dimens.spacingLg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress = progress, size = 88.dp, strokeWidth = 9.dp) {
                Text(
                    if (total == 0) "–" else "${(progress * 100).toInt()}%",
                    style = Typography.titleMedium,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(Dimens.spacingLg))
            Column(Modifier.weight(1f)) {
                Text("Today's progress", style = Typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                Text(
                    if (total == 0) "No tasks planned" else "$done of $total done",
                    style = Typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(Dimens.spacingSm))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                    if (streak > 0) HeroChip("🔥 $streak-day streak")
                    if (overdue > 0) HeroChip("⏰ $overdue overdue")
                    if (streak == 0 && overdue == 0) HeroChip("Start your streak")
                }
            }
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(Dimens.radiusPill)) {
        Text(text, style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun PermissionBanner(p: PermissionHealth, onFix: () -> Unit) {
    val missing = buildList {
        if (!p.notifications) add("notifications")
        if (!p.exactAlarms) add("exact alarms")
        if (!p.fullScreen) add("full-screen alerts")
        if (!p.overlay) add("display over other apps")
    }.joinToString(", ")
    Surface(
        color = Danger.copy(alpha = 0.12f),
        shape = RoundedCornerShape(Dimens.radiusMd),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onFix)
    ) {
        Row(Modifier.padding(Dimens.spacingMd), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️", style = Typography.titleLarge)
            Spacer(Modifier.width(Dimens.spacingMd))
            Column(Modifier.weight(1f)) {
                Text("Alarms may not ring", style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
                Text("Allow $missing so ThumbPlan can wake you.", style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Fix", style = Typography.labelLarge, color = Danger)
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Search tasks…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun TabChip(text: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Dimens.radiusPill),
        shadowElevation = if (selected) 0.dp else 1.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                style = Typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "$count",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: PlanTaskEntity,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val accent = parseColorOrNull(task.colorHex) ?: MaterialTheme.colorScheme.primary
    val isDone = task.status == "completed"
    val isOverdue = !isDone && task.scheduledTime < System.currentTimeMillis()
    val repeat = RepeatMode.from(task.repeatMode)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onComplete()
                    false // list update (or a "too early" message) decides what happens next
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = !isDone,
        backgroundContent = {
            val dir = dismissState.dismissDirection
            val color = when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> Success
                SwipeToDismissBoxValue.EndToStart -> Danger
                else -> Color.Transparent
            }
            Box(
                Modifier.fillMaxSize().background(color, RoundedCornerShape(Dimens.radiusMd)).padding(horizontal = Dimens.spacingLg),
                contentAlignment = if (dir == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Text(
                    when (dir) {
                        SwipeToDismissBoxValue.StartToEnd -> "✓ Complete"
                        SwipeToDismissBoxValue.EndToStart -> "Delete"
                        else -> ""
                    },
                    color = Color.White,
                    style = Typography.labelLarge
                )
            }
        }
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Dimens.radiusMd),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, accent.copy(alpha = if (isDone) 0.15f else 0.45f), RoundedCornerShape(Dimens.radiusMd))
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier.padding(Dimens.cardPadding).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (isDone) "Completed" else "Complete task",
                    tint = if (isDone) Success else accent,
                    modifier = Modifier.size(30.dp).clip(CircleShape).clickable(enabled = !isDone) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onComplete()
                    }
                )
                Spacer(Modifier.width(Dimens.spacingMd))
                if (!task.imagePath.isNullOrEmpty()) {
                    AsyncImage(
                        model = task.imagePath,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(Dimens.imageThumbnailRadius)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(Dimens.spacingMd))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style = Typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        ),
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!task.description.isNullOrBlank()) {
                        Text(
                            task.description,
                            style = Typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pill(Format.time(task.scheduledTime), if (isOverdue) Danger else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (isOverdue) Pill("Overdue", Danger)
                        if (repeat != RepeatMode.NONE) {
                            Icon(Icons.Filled.Repeat, contentDescription = repeat.label, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(Modifier.width(Dimens.spacingSm))
                Text(
                    "🪙 ${task.rewardCoins}",
                    style = Typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
