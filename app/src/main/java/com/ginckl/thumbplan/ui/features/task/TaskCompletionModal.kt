package com.ginckl.thumbplan.ui.features.task

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ginckl.thumbplan.ui.components.BrandGradient
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.Typography
import kotlinx.coroutines.delay

private val Gold = Color(0xFFFBBF24)

@Composable
fun TaskCompletionModal(
    taskTitle: String,
    coinsEarned: Int,
    streak: Int,
    boosted: Boolean = false,
    onDone: (note: String) -> Unit,
    onViewDetails: (note: String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val iconScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "iconScale"
    )
    // Count the coins up for a little dopamine.
    var counted by remember { mutableStateOf(0f) }
    val shownCoins by animateFloatAsState(counted, tween(900), label = "coins")
    LaunchedEffect(coinsEarned) { delay(350); counted = coinsEarned.toFloat() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Dimens.spacingXl))
            Box(Modifier.size(132.dp).scale(iconScale).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = "Success", tint = Color.White, modifier = Modifier.size(72.dp))
            }
            Spacer(Modifier.height(Dimens.spacingLg))
            Text("Task completed!", style = Typography.displayMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Dimens.spacingSm))
            Text(taskTitle, style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

            Spacer(Modifier.height(Dimens.spacingLg))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Surface(color = Gold.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("🪙", style = Typography.titleLarge) }
                }
                Spacer(Modifier.width(Dimens.spacingSm))
                Text("+${shownCoins.toInt()}", style = Typography.displayLarge.copy(color = Gold))
            }
            if (boosted) {
                Spacer(Modifier.height(Dimens.spacingSm))
                Text("⚡ Double Coins applied!", style = Typography.bodyLarge, color = Gold)
            }
            if (streak > 1) {
                Spacer(Modifier.height(Dimens.spacingSm))
                Text("🔥 $streak-day streak — keep it going!", style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(Dimens.spacingXl))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 60) note = it },
                placeholder = { Text("Add a quick note… (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(Dimens.radiusSm),
                supportingText = { Text("${note.length}/60", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) }
            )

            Spacer(Modifier.height(Dimens.spacingLg))
            Button(
                onClick = { onDone(note) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Great!", style = Typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary) }
            TextButton(onClick = { onViewDetails(note) }) {
                Text("View in ThumbPlan", color = MaterialTheme.colorScheme.onSurfaceVariant, style = Typography.labelLarge)
            }
        }
    }
}
