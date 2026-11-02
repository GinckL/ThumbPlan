package com.ginckl.thumbplan.ui.features.task

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.domain.RepeatMode
import com.ginckl.thumbplan.domain.parseReminderMinutes
import com.ginckl.thumbplan.ui.Format
import com.ginckl.thumbplan.ui.features.dashboard.parseColorOrNull
import com.ginckl.thumbplan.ui.theme.Danger
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.PrimaryAccent
import com.ginckl.thumbplan.ui.theme.SecondaryAccent
import com.ginckl.thumbplan.ui.theme.Success
import com.ginckl.thumbplan.ui.theme.Typography
import com.ginckl.thumbplan.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

data class CategoryOption(val name: String, val emoji: String, val coins: Int)

val TaskCategories = listOf(
    CategoryOption("Cleaning", "🧹", 50),
    CategoryOption("Reading a Book", "📖", 75),
    CategoryOption("Watching a Movie", "🎬", 45),
    CategoryOption("Doing Homework", "📝", 75),
    CategoryOption("Studying", "🎓", 75),
    CategoryOption("Reading Article", "📰", 70),
    CategoryOption("Gaming", "🎮", 35),
    CategoryOption("Workout", "💪", 60)
)

private const val CUSTOM = "__custom__"
private val ReminderChoices = listOf(30, 15, 10, 5, 1)
private val ColorChoices = listOf(
    PrimaryAccent, Danger, SecondaryAccent, Success, Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF3B82F6)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    initial: PlanTaskEntity,
    onDismissRequest: () -> Unit,
    onSave: (PlanTaskEntity) -> Unit
) {
    val isNew = initial.id == 0L
    val zone = ZoneId.systemDefault()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialDateTime = remember {
        if (isNew) LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
        else Instant.ofEpochMilli(initial.scheduledTime).atZone(zone).toLocalDateTime()
    }
    val matchingCategory = TaskCategories.firstOrNull { it.name == initial.category }
    val startsCustom = !isNew && initial.category != null && matchingCategory == null

    var title by remember { mutableStateOf(initial.title) }
    var description by remember { mutableStateOf(initial.description.orEmpty()) }
    var date by remember { mutableStateOf(initialDateTime.toLocalDate()) }
    var time by remember { mutableStateOf(initialDateTime.toLocalTime()) }
    var repeat by remember { mutableStateOf(RepeatMode.from(initial.repeatMode)) }
    var categoryKey by remember { mutableStateOf(if (startsCustom) CUSTOM else matchingCategory?.name) }
    var customName by remember { mutableStateOf(if (startsCustom) initial.category.orEmpty() else "") }
    var coins by remember { mutableStateOf((if (isNew) 50 else initial.rewardCoins).coerceIn(30, 80).toFloat()) }
    var color by remember { mutableStateOf(parseColorOrNull(initial.colorHex) ?: ColorChoices[0]) }
    var imagePath by remember { mutableStateOf(initial.imagePath) }
    var audioPath by remember { mutableStateOf(initial.audioPath) }
    val reminders = remember { mutableStateListOf<Int>().also { it.addAll(if (isNew) listOf(10) else parseReminderMinutes(initial.reminderIntervals)) } }

    var titleError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Files copied during this editing session; removed again if the sheet is closed without saving.
    val newFiles = remember { mutableListOf<String>() }
    fun discardNewFiles() { newFiles.forEach(FileUtil::delete); newFiles.clear() }
    fun dismiss() { discardNewFiles(); onDismissRequest() }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { FileUtil.copyUriToLocal(context, uri) }
            if (path != null) {
                if (imagePath in newFiles) { FileUtil.delete(imagePath); newFiles.remove(imagePath) }
                imagePath = path; newFiles += path
            }
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { FileUtil.copyAudioToLocal(context, uri) }
            if (path != null) {
                if (audioPath in newFiles) { FileUtil.delete(audioPath); newFiles.remove(audioPath) }
                audioPath = path; newFiles += path
            }
        }
    }

    if (showDatePicker) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = !isNew || utcTimeMillis >= todayUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // The picker reports UTC midnight – converting with the local zone would shift the day.
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    timeError = null
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(time.hour, time.minute, DateFormat.is24HourFormat(context))
        androidx.compose.material3.TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    timeError = null
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        ) { TimePicker(state = state) }
    }

    ModalBottomSheet(
        onDismissRequest = { dismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingLg)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = Dimens.spacingLg)
        ) {
            Text(
                if (isNew) "New task" else "Edit task",
                style = Typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Dimens.spacingMd))

            Field(
                value = title,
                onChange = { title = it.take(80); titleError = null },
                placeholder = "e.g. Read a book",
                error = titleError,
                singleLine = true
            )
            Spacer(Modifier.height(Dimens.spacingSm))
            Field(
                value = description,
                onChange = { description = it.take(300) },
                placeholder = "Add a short description…",
                maxLines = 3
            )

            Label("When")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                ChoiceChip("Today", date == LocalDate.now(), onClick = { date = LocalDate.now(); timeError = null })
                ChoiceChip("Tomorrow", date == LocalDate.now().plusDays(1), onClick = { date = LocalDate.now().plusDays(1); timeError = null })
            }
            Spacer(Modifier.height(Dimens.spacingSm))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(Dimens.radiusSm)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(Format.date(date))
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(Dimens.radiusSm)) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(Format.time(time))
                }
            }
            timeError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = Typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }

            Label("Repeat")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                RepeatMode.entries.forEach { mode ->
                    ChoiceChip(mode.label, repeat == mode, onClick = { repeat = mode })
                }
            }

            Label("Category & reward")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                TaskCategories.forEach { c ->
                    ChoiceChip("${c.emoji} ${c.name}", categoryKey == c.name, onClick = {
                        categoryKey = if (categoryKey == c.name) null else c.name
                        coins = c.coins.toFloat()
                    })
                }
                ChoiceChip("✏️ Custom", categoryKey == CUSTOM, onClick = { categoryKey = if (categoryKey == CUSTOM) null else CUSTOM })
            }
            if (categoryKey == CUSTOM) {
                Spacer(Modifier.height(Dimens.spacingSm))
                Field(value = customName, onChange = { customName = it.take(40) }, placeholder = "Custom category name", singleLine = true)
            }
            Spacer(Modifier.height(Dimens.spacingSm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reward", style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("🪙 ${coins.toInt()}", style = Typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Slider(value = coins, onValueChange = { coins = (Math.round(it / 5f) * 5).toFloat() }, valueRange = 30f..80f, steps = 9)

            Label("Colour")
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                ColorChoices.forEach { c ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .background(c, CircleShape)
                            .border(3.dp, if (color == c) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .clickable { color = c }
                    )
                }
            }

            Label("Photo")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
                if (imagePath.isNullOrEmpty()) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(Dimens.radiusSm))
                            .background(MaterialTheme.colorScheme.background)
                            .clickable { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Add photo", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("Shown on the alarm screen", style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Box {
                        AsyncImage(
                            model = imagePath,
                            contentDescription = "Selected photo",
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(Dimens.radiusSm)).clickable {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            contentScale = ContentScale.Crop
                        )
                    }
                    TextButton(onClick = {
                        if (imagePath in newFiles) { FileUtil.delete(imagePath); newFiles.remove(imagePath) }
                        imagePath = null
                    }) { Text("Remove") }
                }
            }

            Label("Alarm sound")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { audioPicker.launch("audio/*") }, shape = RoundedCornerShape(Dimens.radiusPill)) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (audioPath.isNullOrEmpty()) "Pick custom sound" else "Custom sound attached")
                }
                if (!audioPath.isNullOrEmpty()) {
                    IconButton(onClick = {
                        if (audioPath in newFiles) { FileUtil.delete(audioPath); newFiles.remove(audioPath) }
                        audioPath = null
                    }) { Icon(Icons.Filled.Close, contentDescription = "Remove sound") }
                }
            }
            if (audioPath.isNullOrEmpty()) {
                Text("Default: your phone's alarm tone", style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }

            Label("Remind me before")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                ReminderChoices.forEach { m ->
                    ChoiceChip("$m min", m in reminders, onClick = { if (m in reminders) reminders.remove(m) else reminders.add(m) })
                }
            }

            Spacer(Modifier.height(Dimens.spacingLg))
            Button(
                onClick = {
                    val trimmed = title.trim()
                    val scheduled = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
                    var ok = true
                    if (trimmed.isEmpty()) { titleError = "Give your task a name"; ok = false }
                    val mustBeFuture = isNew || initial.status == "pending" && scheduled != initial.scheduledTime
                    if (mustBeFuture && scheduled <= System.currentTimeMillis()) { timeError = "Pick a time in the future"; ok = false }
                    if (!ok) return@Button

                    val category = when (categoryKey) {
                        null -> null
                        CUSTOM -> customName.trim().ifBlank { null }
                        else -> categoryKey
                    }
                    val result = initial.copy(
                        title = trimmed,
                        description = description.trim().ifBlank { null },
                        category = category,
                        colorHex = String.format("#%06X", 0xFFFFFF and color.toArgb()),
                        imagePath = imagePath,
                        audioPath = audioPath,
                        scheduledTime = scheduled,
                        rewardCoins = coins.toInt(),
                        isWeekend = repeat == RepeatMode.WEEKENDS,
                        repeatMode = repeat.key,
                        reminderIntervals = reminders.sortedDescending().joinToString(",")
                    )
                    // Files that were replaced/removed on an existing task are no longer referenced.
                    if (initial.imagePath != null && initial.imagePath != imagePath) FileUtil.delete(initial.imagePath)
                    if (initial.audioPath != null && initial.audioPath != audioPath) FileUtil.delete(initial.audioPath)
                    newFiles.clear()
                    onSave(result)
                },
                modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
                shape = RoundedCornerShape(Dimens.radiusSm),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isNew) "Save task" else "Save changes", style = Typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Spacer(Modifier.height(Dimens.spacingLg))
    Text(text, style = Typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(Dimens.spacingSm))
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        shape = RoundedCornerShape(Dimens.radiusPill),
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    error: String? = null,
    singleLine: Boolean = false,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else maxLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusSm),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            errorContainerColor = MaterialTheme.colorScheme.background,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}
