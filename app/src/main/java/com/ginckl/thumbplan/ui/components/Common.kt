package com.ginckl.thumbplan.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.LocalAccent
import com.ginckl.thumbplan.ui.theme.Typography
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

val BrandGradient: Brush
    @Composable get() = Brush.linearGradient(LocalAccent.current.gradient)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = Typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Dimens.radiusMd),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) { content() }
}

@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    Surface(
        color = if (filled) color.copy(alpha = 0.16f) else Color.Transparent,
        shape = RoundedCornerShape(Dimens.radiusPill),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Dimens.spacingMd),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, style = Typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(value, style = Typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** Animated circular progress with arbitrary centre content. */
@Composable
fun ProgressRing(
    progress: Float,
    size: Dp,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = Color.White.copy(alpha = 0.25f),
    ringColor: Color = Color.White,
    content: @Composable () -> Unit = {}
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawArc(trackColor, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            drawArc(ringColor, -90f, 360f * animated, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
        }
        content()
    }
}

@Composable
fun LinearProgress(progress: Float, modifier: Modifier = Modifier, height: Dp = 10.dp, track: Color = Color.White.copy(alpha = 0.25f), fill: Color = Color.White) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "bar")
    Box(modifier.fillMaxWidth().height(height).background(track, CircleShape)) {
        Box(Modifier.fillMaxWidth(animated).height(height).background(fill, CircleShape))
    }
}

/** 7-day bar chart; last bar (today) is highlighted. */
@Composable
fun WeekBarChart(data: List<Pair<LocalDate, Int>>, modifier: Modifier = Modifier) {
    val max = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { ready = true }
    val grow by animateFloatAsState(if (ready) 1f else 0f, tween(800, easing = FastOutSlowInEasing), label = "bars")
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val barTop = MaterialTheme.colorScheme.secondary
    val barBottom = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, (date, count) ->
            val isToday = index == data.lastIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
                Text(
                    if (count > 0) "$count" else "",
                    style = Typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Canvas(Modifier.width(22.dp).height(90.dp)) {
                    val barH = if (count == 0) 6.dp.toPx() else (size.height * count / max) * grow
                    drawRoundRect(
                        brush = if (count > 0) Brush.verticalGradient(listOf(barTop, barBottom)) else Brush.verticalGradient(listOf(muted, muted)),
                        topLeft = Offset(0f, size.height - barH.coerceAtLeast(6.dp.toPx())),
                        size = Size(size.width, barH.coerceAtLeast(6.dp.toPx())),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                    style = Typography.labelSmall.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium),
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Dimens.spacingXl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, style = Typography.displayLarge.copy(fontSize = 56.sp))
        Spacer(Modifier.height(Dimens.spacingMd))
        Text(title, style = Typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Dimens.spacingSm))
        Text(subtitle, style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Dimens.spacingLg))
        action()
    }
}

