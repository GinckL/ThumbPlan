package com.ginckl.thumbplan.ui.features.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.domain.RepeatMode
import com.ginckl.thumbplan.domain.parseReminderMinutes
import com.ginckl.thumbplan.ui.Format
import com.ginckl.thumbplan.ui.components.Pill
import com.ginckl.thumbplan.ui.features.dashboard.parseColorOrNull
import com.ginckl.thumbplan.ui.theme.Danger
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.Success
import com.ginckl.thumbplan.ui.theme.Typography

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    task: PlanTaskEntity,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by remember { mutableStateOf(false) }
    val accent = parseColorOrNull(task.colorHex) ?: MaterialTheme.colorScheme.primary
    val done = task.status == "completed"
    val overdue = !done && task.scheduledTime < System.currentTimeMillis()
    val repeat = RepeatMode.from(task.repeatMode)
    val reminders = parseReminderMinutes(task.reminderIntervals)

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${task.title}”?") },
            text = { Text(if (repeat != RepeatMode.NONE && !done) "This also stops the ${repeat.label.lowercase()} repeat once no later occurrence exists." else "You can undo right after.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = Danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spacingLg)
                .navigationBarsPadding()
                .padding(bottom = Dimens.spacingLg)
        ) {
            if (!task.imagePath.isNullOrEmpty()) {
                AsyncImage(
                    model = task.imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(Dimens.radiusMd))
                )
                Spacer(Modifier.height(Dimens.spacingMd))
            }
            Text(task.title, style = Typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Dimens.spacingSm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(Format.dateTime(task.scheduledTime), if (overdue) Danger else accent)
                if (done) Pill("Completed", Success)
                if (overdue) Pill("Overdue", Danger)
                task.category?.let { Pill(it, MaterialTheme.colorScheme.onSurfaceVariant) }
                if (repeat != RepeatMode.NONE) Pill("🔁 ${repeat.label}", MaterialTheme.colorScheme.onSurfaceVariant)
                Pill("🪙 ${task.rewardCoins}", MaterialTheme.colorScheme.secondary)
            }
            if (!task.description.isNullOrBlank()) {
                Spacer(Modifier.height(Dimens.spacingMd))
                Text(task.description, style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            if (reminders.isNotEmpty()) {
                Spacer(Modifier.height(Dimens.spacingMd))
                Text(
                    "🔔 Reminders: " + reminders.joinToString(", ") { "$it min before" },
                    style = Typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!task.audioPath.isNullOrEmpty()) {
                Text("🎵 Custom alarm sound", style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(Dimens.spacingLg))
            if (!done) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = androidx.compose.ui.graphics.Color.Black)
                ) { Text("Mark as done · +${task.rewardCoins} 🪙", style = Typography.labelLarge) }
                Spacer(Modifier.height(Dimens.spacingSm))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                if (!done) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(Dimens.radiusSm)) { Text("Edit") }
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(Dimens.radiusSm)
                ) { Text("Delete", color = Danger) }
            }
        }
    }
}
