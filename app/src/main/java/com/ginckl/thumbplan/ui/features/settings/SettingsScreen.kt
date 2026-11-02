package com.ginckl.thumbplan.ui.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ginckl.thumbplan.data.backup.BackupInfo
import com.ginckl.thumbplan.data.backup.Resolution
import com.ginckl.thumbplan.data.backup.SyncUi
import com.ginckl.thumbplan.data.local.entity.UserEntity
import com.ginckl.thumbplan.data.prefs.ThemeMode
import com.ginckl.thumbplan.domain.ShopItem
import com.ginckl.thumbplan.ui.Format
import com.ginckl.thumbplan.ui.components.InfoCard
import com.ginckl.thumbplan.ui.components.SectionTitle
import com.ginckl.thumbplan.ui.features.alarm.AlarmActivity
import com.ginckl.thumbplan.ui.theme.Danger
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.Success
import com.ginckl.thumbplan.ui.theme.Typography
import com.ginckl.thumbplan.util.PermissionHealth
import com.ginckl.thumbplan.util.Permissions
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    user: UserEntity,
    avatarEmoji: String,
    themeMode: ThemeMode,
    snoozeMinutes: Int,
    vibrate: Boolean,
    sound: Boolean,
    openAppAfterSwipe: Boolean,
    equippedTone: String,
    ownedTones: List<ShopItem>,
    permissions: PermissionHealth,
    sync: SyncUi,
    syncing: Boolean,
    pendingImport: Pair<android.net.Uri, BackupInfo>?,
    syncConflict: BackupInfo?,
    contentPadding: PaddingValues,
    onRename: (String) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onSnooze: (Int) -> Unit,
    onVibrate: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onOpenAppAfterSwipe: (Boolean) -> Unit,
    onTone: (String) -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onPickImport: (android.net.Uri) -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onConnectFolder: (android.net.Uri) -> Unit,
    onSyncEnabled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onDisconnectSync: () -> Unit,
    onResolveConflict: (Resolution?) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingName by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onExport(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPickImport(uri)
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onConnectFolder(uri)
    }

    if (editingName) {
        var name by remember { mutableStateOf(user.name) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("Your name") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, singleLine = true) },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onRename(name); editingName = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text("Cancel") } }
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = Danger) },
            title = { Text("Erase all data?") },
            text = { Text("All tasks, coins, purchases, streaks and photos will be permanently deleted from this device. (Your sync folder is not touched.)") },
            confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text("Erase everything", color = Danger) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
    pendingImport?.let { (_, info) ->
        AlertDialog(
            onDismissRequest = onCancelImport,
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = Danger) },
            title = { Text("Replace everything with this backup?") },
            text = { Text("Backup from ${Format.dateTime(info.exportedAt)} (${info.device.ifBlank { "unknown device" }}): ${info.tasks} tasks, ${info.coins} coins.\n\nThe data on this phone will be overwritten.") },
            confirmButton = { TextButton(onClick = onConfirmImport) { Text("Restore", color = Danger) } },
            dismissButton = { TextButton(onClick = onCancelImport) { Text("Cancel") } }
        )
    }
    syncConflict?.let { remote ->
        AlertDialog(
            onDismissRequest = { onResolveConflict(null) },
            title = { Text("Changes on two devices") },
            text = {
                Text(
                    "This phone and your sync folder both changed since the last sync.\n\n" +
                        "Folder backup: ${Format.dateTime(remote.exportedAt)} from ${remote.device.ifBlank { "another device" }} (${remote.tasks} tasks, ${remote.coins} coins).\n\n" +
                        "Which one should win? The other is overwritten."
                )
            },
            confirmButton = { TextButton(onClick = { onResolveConflict(Resolution.KEEP_THIS_DEVICE) }) { Text("Keep this phone") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { onResolveConflict(Resolution.USE_BACKUP) }) { Text("Use folder backup", color = Danger) }
                    TextButton(onClick = { onResolveConflict(null) }) { Text("Later") }
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.spacingMd, end = Dimens.spacingMd,
            top = contentPadding.calculateTopPadding() + Dimens.spacingSm,
            bottom = contentPadding.calculateBottomPadding() + Dimens.spacingLg
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        item { Text("Settings", style = Typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) }

        item {
            SettingsCard("Profile") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(avatarEmoji, style = Typography.headlineMedium, modifier = Modifier.padding(end = Dimens.spacingMd))
                    Column(Modifier.weight(1f)) {
                        Text(user.name, style = Typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Referral code: ${user.referralCode}", style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { editingName = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit name") }
                }
            }
        }

        item {
            SettingsCard("Appearance") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { onTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                        ) { Text(mode.label) }
                    }
                }
                Text(
                    "Unlock more colours in the Shop.",
                    style = Typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.spacingSm)
                )
            }
        }

        item {
            SettingsCard("Alarm") {
                ToggleRow("Alarm sound", "Play your music when a task is due", sound, onSound, "toggle_sound")
                ToggleRow("Vibration", "Vibrate along with the alarm", vibrate, onVibrate, "toggle_vibrate")
                ToggleRow(
                    "Open the app after swiping",
                    "After you swipe to complete, jump into ThumbPlan. Turn off to just carry on with what you were doing.",
                    openAppAfterSwipe, onOpenAppAfterSwipe, "toggle_open_after_swipe"
                )
                Spacer(Modifier.height(Dimens.spacingSm))
                Text("Default alarm tone", style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Used when a task has no music of its own. Buy more tones in the Shop.",
                    style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
                    modifier = Modifier.padding(top = Dimens.spacingSm)
                ) {
                    ownedTones.forEach { t ->
                        FilterChip(selected = equippedTone == t.id, onClick = { onTone(t.id) }, label = { Text("${t.emoji} ${t.name}") }, shape = RoundedCornerShape(Dimens.radiusPill))
                    }
                }
                Spacer(Modifier.height(Dimens.spacingMd))
                Text("Snooze length", style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm), modifier = Modifier.padding(top = Dimens.spacingSm)) {
                    listOf(5, 10, 15, 30).forEach { m ->
                        FilterChip(selected = snoozeMinutes == m, onClick = { onSnooze(m) }, label = { Text("$m min") }, shape = RoundedCornerShape(Dimens.radiusPill))
                    }
                }
                Spacer(Modifier.height(Dimens.spacingMd))
                OutlinedButton(
                    onClick = { context.startActivity(AlarmActivity.previewIntent(context)) },
                    modifier = Modifier.fillMaxWidth().testTag("preview_alarm"),
                    shape = RoundedCornerShape(Dimens.radiusSm)
                ) { Text("Preview alarm (sound + screen)") }
            }
        }

        item {
            SettingsCard("Backup & sync") {
                Text(
                    "Keep your tasks, coins and purchases safe, or move them to another phone. Pick a folder in Google Drive, Dropbox, OneDrive… and ThumbPlan keeps a backup there that other devices can sync from.",
                    style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Dimens.spacingMd))
                if (sync.folderUri == null) {
                    Button(
                        onClick = { folderLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth().testTag("connect_folder"),
                        shape = RoundedCornerShape(Dimens.radiusSm)
                    ) { Text("Choose sync folder") }
                } else {
                    Text("📁 ${sync.folderName ?: "Sync folder"}", style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                    val last = if (sync.lastSyncTime > 0) "Last sync: ${Format.dateTime(sync.lastSyncTime)}" else "Not synced yet"
                    Text(last, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (sync.status.isNotBlank()) {
                        Text(sync.status, style = Typography.labelMedium, color = if (sync.status.startsWith("Sync failed") || sync.status.startsWith("Conflict")) Danger else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ToggleRow("Automatic sync", "On app start, when you leave it, and every 12 hours", sync.enabled, onSyncEnabled, "toggle_auto_sync")
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                        Button(onClick = onSyncNow, enabled = !syncing, modifier = Modifier.weight(1f).testTag("sync_now"), shape = RoundedCornerShape(Dimens.radiusSm)) {
                            if (syncing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            else Text("Sync now")
                        }
                        OutlinedButton(onClick = { folderLauncher.launch(null) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(Dimens.radiusSm)) { Text("Change folder") }
                    }
                    TextButton(onClick = onDisconnectSync) { Text("Disconnect folder", color = Danger) }
                }
                Spacer(Modifier.height(Dimens.spacingSm))
                Text("Manual backup file", style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm), modifier = Modifier.padding(top = Dimens.spacingSm)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("thumbplan-backup-${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}.zip") },
                        modifier = Modifier.weight(1f).testTag("export_backup"),
                        shape = RoundedCornerShape(Dimens.radiusSm)
                    ) { Text("Export") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                        modifier = Modifier.weight(1f).testTag("import_backup"),
                        shape = RoundedCornerShape(Dimens.radiusSm)
                    ) { Text("Import") }
                }
                Text(
                    "Android also backs this app up to your Google account automatically when device backup is on.",
                    style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.spacingSm)
                )
            }
        }

        item {
            SettingsCard("Permissions") {
                PermissionRow("Notifications", "Reminders before a task starts", permissions.notifications) {
                    context.startActivity(Permissions.notificationSettings(context))
                }
                PermissionRow("Exact alarms", "Ring at the exact minute", permissions.exactAlarms) {
                    context.startActivity(Permissions.exactAlarmSettings(context))
                }
                PermissionRow("Full-screen alerts", "Open the alarm over the lock screen", permissions.fullScreen) {
                    context.startActivity(Permissions.fullScreenSettings(context))
                }
                PermissionRow("Display over other apps", "Open the alarm straight away while you use your phone", permissions.overlay) {
                    context.startActivity(Permissions.overlaySettings(context))
                }
                Text(
                    "Tip: some phones (Xiaomi, Huawei, Samsung...) also kill background apps - set ThumbPlan to \"Unrestricted\" battery usage for the most reliable alarms.",
                    style = Typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.spacingSm)
                )
                OutlinedButton(
                    onClick = { context.startActivity(Permissions.appDetails(context)) },
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingSm),
                    shape = RoundedCornerShape(Dimens.radiusSm)
                ) { Text("Open app settings") }
            }
        }

        item {
            SettingsCard("Data") {
                Button(
                    onClick = { confirmReset = true },
                    modifier = Modifier.fillMaxWidth().testTag("erase_all"),
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger.copy(alpha = 0.15f), contentColor = Danger)
                ) { Text("Erase all data") }
            }
        }

        item {
            Text(
                "ThumbPlan v$version · Small steps. Big results.",
                style = Typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSm),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column {
        SectionTitle(title, Modifier.padding(start = 4.dp, bottom = Dimens.spacingSm))
        InfoCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(Dimens.spacingMd)) { content() } }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, tag: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = Dimens.spacingSm)) {
            Text(title, style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, granted: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            Icon(Icons.Filled.Check, contentDescription = "Allowed", tint = Success, modifier = Modifier.size(24.dp))
        } else {
            TextButton(onClick = onFix) { Text("Allow", color = Danger) }
        }
    }
}
