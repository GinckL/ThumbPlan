package com.ginckl.thumbplan.ui.features.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.ginckl.thumbplan.MainActivity
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.data.prefs.SettingsStore
import com.ginckl.thumbplan.data.prefs.ThemeMode
import com.ginckl.thumbplan.data.repo.CompletionResult
import com.ginckl.thumbplan.data.repo.TaskRepository
import com.ginckl.thumbplan.services.Notifier
import com.ginckl.thumbplan.services.ToneSynth
import com.ginckl.thumbplan.services.alarm.AlarmPlayer
import com.ginckl.thumbplan.services.alarm.AlarmService
import com.ginckl.thumbplan.ui.Format
import com.ginckl.thumbplan.ui.components.BrandGradient
import com.ginckl.thumbplan.ui.components.Pill
import com.ginckl.thumbplan.ui.features.dashboard.parseColorOrNull
import com.ginckl.thumbplan.ui.theme.Accents
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.ThumbPlanTheme
import com.ginckl.thumbplan.ui.theme.Typography
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * The full-screen alarm page. The music is played by [AlarmService] (so it starts even before this page is up);
 * this page only offers Complete / Snooze / Dismiss and tells the service to stop.
 * Preview mode (Settings → Preview alarm) plays its own short sound instead.
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var settings: SettingsStore

    private var previewPlayer: AlarmPlayer? = null
    private val taskId = mutableLongStateOf(-1L)
    private var preview = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        readIntent(intent)

        // Back = dismiss: silence the alarm instead of leaving it ringing in the background.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = silenceAndClose()
        })

        enableEdgeToEdge()
        setContent {
            val mode by settings.themeMode.collectAsState()
            val accentId by settings.accent.collectAsState()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            ThumbPlanTheme(darkTheme = dark, accent = Accents.get(accentId)) { AlarmContent() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent) // another alarm fired while this one is showing
    }

    private fun readIntent(intent: Intent) {
        preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        taskId.longValue = intent.getLongExtra(EXTRA_TASK_ID, -1L)
    }

    private fun silence() {
        previewPlayer?.stop()
        AlarmService.stop(this)
        Notifier.cancelAlarm(this, taskId.longValue)
    }

    private fun silenceAndClose() {
        silence()
        finish()
    }

    override fun onDestroy() {
        previewPlayer?.stop()
        super.onDestroy()
    }

    @Composable
    private fun AlarmContent() {
        val id = taskId.longValue
        val task by produceState<PlanTaskEntity?>(null, id, preview) {
            value = if (preview) previewTask() else repository.getTask(id)
            if (value == null) finish()
        }
        val sound by settings.sound.collectAsState()
        val vibrate by settings.vibrate.collectAsState()
        val snooze by settings.snoozeMinutes.collectAsState()

        if (preview) {
            LaunchedEffect(Unit) {
                val tone = ToneSynth.fileFor(this@AlarmActivity, settings.tone.value)
                previewPlayer = AlarmPlayer(this@AlarmActivity).also { it.start(tone?.absolutePath, sound, vibrate) }
                delay(8_000) // a preview shouldn't go on forever
                previewPlayer?.stop()
            }
        }

        val current = task
        if (current == null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            return
        }
        AlarmScreen(
            task = current,
            snoozeMinutes = snooze,
            onComplete = { complete(current) },
            onSnooze = {
                silence()
                if (!preview) {
                    repository.snooze(current)
                    Toast.makeText(this, "Snoozed for $snooze min", Toast.LENGTH_SHORT).show()
                }
                finish()
            },
            onDismiss = { silenceAndClose() }
        )
    }

    private fun complete(task: PlanTaskEntity) {
        silence()
        if (preview) { finish(); return }
        lifecycleScope.launch {
            val result = repository.completeTask(task.id)
            if (result !is CompletionResult.Completed) { finish(); return@launch }
            if (settings.openAppAfterSwipe.value) {
                // Default: jump into the app and celebrate there.
                startActivity(
                    Intent(this@AlarmActivity, MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_CELEBRATE_TITLE, result.task.title)
                        putExtra(MainActivity.EXTRA_CELEBRATE_COINS, result.coins)
                        putExtra(MainActivity.EXTRA_CELEBRATE_STREAK, result.streak)
                        putExtra(MainActivity.EXTRA_CELEBRATE_DONE_ID, result.doneTaskId)
                        putExtra(MainActivity.EXTRA_CELEBRATE_TASK_ID, result.task.id)
                        putExtra(MainActivity.EXTRA_CELEBRATE_BOOSTED, result.boosted)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                )
            } else {
                // Opted out: just a quiet confirmation, then the user continues whatever they were doing.
                val streak = if (result.streak > 1) " · 🔥 ${result.streak}" else ""
                Toast.makeText(this@AlarmActivity, "+${result.coins} coins$streak", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun previewTask() = PlanTaskEntity(
        id = -1, userId = 0, title = "Test alarm", description = "This is how your alarm will look.",
        category = "Preview", scheduledTime = System.currentTimeMillis(), rewardCoins = 50
    )

    companion object {
        const val EXTRA_TASK_ID = "TASK_ID"
        const val EXTRA_PREVIEW = "PREVIEW"

        fun previewIntent(context: Context) = Notifier.alarmIntent(context, -1L, preview = true)
    }
}

@Composable
fun AlarmScreen(
    task: PlanTaskEntity,
    snoozeMinutes: Int,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = parseColorOrNull(task.colorHex) ?: MaterialTheme.colorScheme.primary
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulseScale"
    )

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // soft glow behind the content
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent)))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                Format.time(task.scheduledTime),
                style = Typography.displayLarge.copy(fontSize = TextUnit(56f, TextUnitType.Sp), lineHeight = TextUnit(64f, TextUnitType.Sp)),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(Dimens.spacingLg))

            // 1:1 hero, rounded corners
            Box(Modifier.size(220.dp).scale(pulse).clip(RoundedCornerShape(36.dp))) {
                if (!task.imagePath.isNullOrEmpty()) {
                    AsyncImage(
                        model = task.imagePath,
                        contentDescription = "Task image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(BrandGradient), contentAlignment = Alignment.Center) {
                        Text("⏰", style = Typography.displayLarge.copy(fontSize = TextUnit(88f, TextUnitType.Sp)))
                    }
                }
            }

            Spacer(Modifier.height(Dimens.spacingLg))
            Text(
                task.title,
                style = Typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            if (!task.description.isNullOrBlank()) {
                Spacer(Modifier.height(Dimens.spacingXs))
                Text(task.description, style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(Dimens.spacingMd))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                task.category?.let { Pill(it, MaterialTheme.colorScheme.onSurfaceVariant) }
                Pill("🪙 ${task.rewardCoins}", MaterialTheme.colorScheme.secondary)
            }

            Spacer(Modifier.height(Dimens.spacingXl))
            Text("Small steps. Big results.", style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Dimens.spacingLg))

            SwipeToComplete(onComplete = onComplete)

            Spacer(Modifier.height(Dimens.spacingMd))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onSnooze) {
                    Text("Snooze $snoozeMinutes min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
