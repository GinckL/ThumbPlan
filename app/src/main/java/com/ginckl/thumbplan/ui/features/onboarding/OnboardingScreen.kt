package com.ginckl.thumbplan.ui.features.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ginckl.thumbplan.ui.components.BrandGradient
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.Typography

@Composable
fun OnboardingScreen(onStart: (String) -> Unit, onRestore: (android.net.Uri) -> Unit = {}) {
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onRestore(uri) }
    var name by rememberSaveable { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(BrandGradient), contentAlignment = Alignment.Center) {
                Text("✓", style = Typography.displayLarge, color = Color.White)
            }
            Spacer(Modifier.height(Dimens.spacingLg))
            Text("Welcome to ThumbPlan", style = Typography.displayMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Dimens.spacingSm))
            Text("Plan your day. Get woken up for what matters. Earn coins for every win.", style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

            Spacer(Modifier.height(Dimens.spacingXl))
            Feature("⏰", "Real alarms", "A full-screen alarm with your photo and sound when a task is due.")
            Feature("🪙", "Coins & levels", "Every finished task pays out — level up as you go.")
            Feature("🔥", "Streaks", "Do something every day and watch your streak grow.")

            Spacer(Modifier.height(Dimens.spacingXl))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                label = { Text("What should we call you?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.radiusSm),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onStart(name) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(Modifier.height(Dimens.spacingMd))
            Button(
                onClick = { onStart(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
                shape = RoundedCornerShape(Dimens.radiusSm)
            ) { Text("Get started", style = Typography.labelLarge) }
            androidx.compose.material3.TextButton(onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, modifier = Modifier.padding(top = Dimens.spacingSm)) {
                Text("I already use ThumbPlan - restore a backup")
            }
        }
    }
}

@Composable
private fun Feature(emoji: String, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, style = Typography.titleLarge) }
        Spacer(Modifier.width(Dimens.spacingMd))
        Column {
            Text(title, style = Typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
            Text(text, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
