package com.ginckl.thumbplan.ui.features.alarm

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ginckl.thumbplan.ui.theme.Typography
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToComplete(
    modifier: Modifier = Modifier,
    label: String = "Swipe to complete",
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var trackWidth by remember { mutableIntStateOf(0) }
    var thumbWidth by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(0f) }
    val maxOffset = (trackWidth - thumbWidth - with(LocalDensity.current) { 12.dp.toPx() }).coerceAtLeast(1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
            .onSizeChanged { trackWidth = it.width },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            style = Typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center).alpha((1f - offsetX.value / maxOffset).coerceIn(0f, 1f))
        )

        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .padding(6.dp)
                .size(56.dp)
                .onSizeChanged { thumbWidth = it.width }
                .draggable(
                    enabled = !finished,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(0f, maxOffset)) }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (offsetX.value > maxOffset * 0.75f) {
                                finished = true
                                offsetX.animateTo(maxOffset)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onComplete()
                            } else {
                                offsetX.animateTo(0f)
                            }
                        }
                    }
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (finished) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Swipe to complete",
                    tint = Color.White
                )
            }
        }
    }
}
