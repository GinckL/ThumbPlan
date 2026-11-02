package com.ginckl.thumbplan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.ginckl.thumbplan.data.prefs.ThemeMode
import com.ginckl.thumbplan.domain.ItemKind
import com.ginckl.thumbplan.domain.ShopCatalog
import com.ginckl.thumbplan.services.work.SyncWorker
import com.ginckl.thumbplan.ui.components.BrandGradient
import com.ginckl.thumbplan.ui.components.LiquidGlassNavBar
import com.ginckl.thumbplan.ui.features.dashboard.DashboardScreen
import com.ginckl.thumbplan.ui.features.onboarding.OnboardingScreen
import com.ginckl.thumbplan.ui.features.rewards.RewardsScreen
import com.ginckl.thumbplan.ui.features.settings.SettingsScreen
import com.ginckl.thumbplan.ui.features.shop.ShopScreen
import com.ginckl.thumbplan.ui.features.task.TaskCompletionModal
import com.ginckl.thumbplan.ui.features.task.TaskDetailSheet
import com.ginckl.thumbplan.ui.features.task.TaskEditorSheet
import com.ginckl.thumbplan.ui.theme.Accents
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.ThumbPlanTheme
import com.ginckl.thumbplan.util.PermissionHealth
import com.ginckl.thumbplan.util.Permissions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private var permissions by mutableStateOf(PermissionHealth(true, true, true, true))

    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissions = Permissions.check(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissions = Permissions.check(this)
        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            val mode by viewModel.themeMode.collectAsState()
            val accentId by viewModel.accent.collectAsState()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            ThumbPlanTheme(darkTheme = dark, accent = Accents.get(accentId)) {
                val userState by viewModel.userState.collectAsState()
                when (val s = userState) {
                    UserState.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    UserState.Missing -> OnboardingScreen(
                        onStart = { viewModel.completeOnboarding(it) },
                        onRestore = { viewModel.restoreFromUri(it) }
                    )
                    is UserState.Ready -> {
                        // Ask for notifications once the user is in (not as a cold-start surprise).
                        LaunchedEffect(Unit) { requestNotificationPermissionIfNeeded() }
                        ThumbPlanMainScreen(
                            viewModel = viewModel,
                            user = s.user,
                            dark = dark,
                            permissions = permissions,
                            onFixPermissions = ::fixNextPermission
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppStart()
    }

    override fun onResume() {
        super.onResume()
        // Settings screens can change these behind our back.
        val before = permissions
        permissions = Permissions.check(this)
        if (permissions.exactAlarms && !before.exactAlarms) viewModel.reschedule()
    }

    override fun onStop() {
        super.onStop()
        // push local changes to the sync folder shortly after the user leaves
        if (viewModel.syncUi.value.enabled) SyncWorker.syncSoon(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val id = intent.getLongExtra(EXTRA_OPEN_TASK_ID, -1L)
        if (id > 0) {
            viewModel.openTask(id)
            intent.removeExtra(EXTRA_OPEN_TASK_ID)
        }
        val title = intent.getStringExtra(EXTRA_CELEBRATE_TITLE)
        if (title != null) {
            viewModel.showCelebration(
                Celebration(
                    title = title,
                    coins = intent.getIntExtra(EXTRA_CELEBRATE_COINS, 0),
                    streak = intent.getIntExtra(EXTRA_CELEBRATE_STREAK, 0),
                    doneTaskId = intent.getLongExtra(EXTRA_CELEBRATE_DONE_ID, -1L),
                    taskId = intent.getLongExtra(EXTRA_CELEBRATE_TASK_ID, -1L),
                    boosted = intent.getBooleanExtra(EXTRA_CELEBRATE_BOOSTED, false)
                )
            )
            intent.removeExtra(EXTRA_CELEBRATE_TITLE)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun fixNextPermission() {
        val p = permissions
        when {
            !p.notifications -> {
                // After two denials the system dialog no longer appears, so fall back to settings.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                ) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else startActivity(Permissions.notificationSettings(this))
            }
            !p.exactAlarms -> startActivity(Permissions.exactAlarmSettings(this))
            !p.fullScreen -> startActivity(Permissions.fullScreenSettings(this))
            !p.overlay -> startActivity(Permissions.overlaySettings(this))
        }
    }

    companion object {
        const val EXTRA_OPEN_TASK_ID = "OPEN_TASK_ID"
        const val EXTRA_CELEBRATE_TITLE = "CELEBRATE_TITLE"
        const val EXTRA_CELEBRATE_COINS = "CELEBRATE_COINS"
        const val EXTRA_CELEBRATE_STREAK = "CELEBRATE_STREAK"
        const val EXTRA_CELEBRATE_DONE_ID = "CELEBRATE_DONE_ID"
        const val EXTRA_CELEBRATE_TASK_ID = "CELEBRATE_TASK_ID"
        const val EXTRA_CELEBRATE_BOOSTED = "CELEBRATE_BOOSTED"
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    REWARDS("Rewards", Icons.Filled.Star),
    SHOP("Shop", Icons.Filled.ShoppingBag),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@Composable
fun ThumbPlanMainScreen(
    viewModel: MainViewModel,
    user: com.ginckl.thumbplan.data.local.entity.UserEntity,
    dark: Boolean,
    permissions: PermissionHealth,
    onFixPermissions: () -> Unit
) {
    var destination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val snackbar = remember { SnackbarHostState() }

    val tasks by viewModel.tasks.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val history by viewModel.coinHistory.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val selectedId by viewModel.selectedTaskId.collectAsState()
    val editor by viewModel.editor.collectAsState()
    val celebration by viewModel.celebration.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val snooze by viewModel.snoozeMinutes.collectAsState()
    val vibrate by viewModel.vibrate.collectAsState()
    val sound by viewModel.sound.collectAsState()
    val openAfter by viewModel.openAppAfterSwipe.collectAsState()
    val accent by viewModel.accent.collectAsState()
    val avatar by viewModel.avatar.collectAsState()
    val tone by viewModel.tone.collectAsState()
    val syncUi by viewModel.syncUi.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val pendingImport by viewModel.pendingImport.collectAsState()
    val conflict by viewModel.syncConflict.collectAsState()

    val avatarEmoji = ShopCatalog.find(avatar)?.emoji ?: "🙂"
    val ownedTones = ShopCatalog.ofKind(ItemKind.TONE).filter { viewModel.owns(it.id, inventory) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbar.currentSnackbarData?.dismiss()
            when (event) {
                is UiEvent.Message -> snackbar.showSnackbar(event.text, duration = SnackbarDuration.Short)
                is UiEvent.Deleted -> {
                    val r = snackbar.showSnackbar("“${event.task.title}” deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
                    if (r == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.task)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (destination == AppDestinations.HOME) {
                Box(
                    modifier = Modifier
                        .size(Dimens.fabSize)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(BrandGradient)
                        .testTag("fab_add")
                        .clickable { viewModel.startCreate() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Add, contentDescription = "Add task", tint = Color.White) }
            }
        },
        bottomBar = {
            LiquidGlassNavBar(
                items = AppDestinations.entries.toTypedArray(),
                currentDestination = destination,
                isDarkTheme = dark,
                onNavigate = { destination = it }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = destination,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            label = "screen",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            when (screen) {
                AppDestinations.HOME -> DashboardScreen(
                    tasks = tasks,
                    user = user,
                    avatarEmoji = avatarEmoji,
                    boostActive = (inventory[ShopCatalog.BOOST] ?: 0) > 0,
                    insights = insights,
                    permissions = permissions,
                    onFixPermissions = onFixPermissions,
                    onTaskClick = { viewModel.openTask(it.id) },
                    onTaskComplete = viewModel::completeTask,
                    onTaskDelete = viewModel::deleteTask,
                    onAddTask = viewModel::startCreate,
                    contentPadding = padding
                )
                AppDestinations.REWARDS -> RewardsScreen(user = user, insights = insights, history = history, contentPadding = padding)
                AppDestinations.SHOP -> ShopScreen(
                    user = user,
                    inventory = inventory,
                    equippedAccent = accent,
                    equippedAvatar = avatar,
                    equippedTone = tone,
                    contentPadding = padding,
                    onBuy = viewModel::buy,
                    onEquip = viewModel::equip
                )
                AppDestinations.SETTINGS -> SettingsScreen(
                    user = user,
                    avatarEmoji = avatarEmoji,
                    themeMode = themeMode,
                    snoozeMinutes = snooze,
                    vibrate = vibrate,
                    sound = sound,
                    openAppAfterSwipe = openAfter,
                    equippedTone = tone,
                    ownedTones = ownedTones,
                    permissions = permissions,
                    sync = syncUi,
                    syncing = syncing,
                    pendingImport = pendingImport,
                    syncConflict = conflict,
                    contentPadding = padding,
                    onRename = viewModel::renameUser,
                    onTheme = viewModel::setThemeMode,
                    onSnooze = viewModel::setSnoozeMinutes,
                    onVibrate = viewModel::setVibrate,
                    onSound = viewModel::setSound,
                    onOpenAppAfterSwipe = viewModel::setOpenAppAfterSwipe,
                    onTone = viewModel::equip,
                    onExport = viewModel::exportTo,
                    onPickImport = viewModel::prepareImport,
                    onConfirmImport = viewModel::confirmImport,
                    onCancelImport = viewModel::cancelImport,
                    onConnectFolder = viewModel::connectSyncFolder,
                    onSyncEnabled = viewModel::setSyncEnabled,
                    onSyncNow = { viewModel.syncNow() },
                    onDisconnectSync = viewModel::disconnectSync,
                    onResolveConflict = viewModel::resolveConflict,
                    onReset = viewModel::resetAll
                )
            }
        }
    }

    val selected = tasks.firstOrNull { it.id == selectedId }
    if (selected != null) {
        TaskDetailSheet(
            task = selected,
            onDismiss = { viewModel.openTask(null) },
            onComplete = { viewModel.openTask(null); viewModel.completeTask(selected) },
            onEdit = { viewModel.openTask(null); viewModel.startEdit(selected) },
            onDelete = { viewModel.deleteTask(selected) }
        )
    }

    editor?.let { initial ->
        TaskEditorSheet(
            initial = initial,
            onDismissRequest = viewModel::closeEditor,
            onSave = { viewModel.saveTask(it); viewModel.closeEditor() }
        )
    }

    celebration?.let { c ->
        Dialog(
            onDismissRequest = { viewModel.finishCelebration("", false) },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            TaskCompletionModal(
                taskTitle = c.title,
                coinsEarned = c.coins,
                streak = c.streak,
                boosted = c.boosted,
                onDone = { note -> viewModel.finishCelebration(note, false) },
                onViewDetails = { note -> viewModel.finishCelebration(note, true) }
            )
        }
    }
}
