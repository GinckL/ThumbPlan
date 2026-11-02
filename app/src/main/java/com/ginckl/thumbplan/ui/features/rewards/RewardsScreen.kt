package com.ginckl.thumbplan.ui.features.rewards

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ginckl.thumbplan.Insights
import com.ginckl.thumbplan.data.local.dao.CoinHistoryItem
import com.ginckl.thumbplan.data.local.entity.UserEntity
import com.ginckl.thumbplan.domain.Badge
import com.ginckl.thumbplan.ui.Format
import com.ginckl.thumbplan.ui.components.BrandGradient
import com.ginckl.thumbplan.ui.components.EmptyState
import com.ginckl.thumbplan.ui.components.InfoCard
import com.ginckl.thumbplan.ui.components.LinearProgress
import com.ginckl.thumbplan.ui.components.SectionTitle
import com.ginckl.thumbplan.ui.components.StatTile
import com.ginckl.thumbplan.ui.components.WeekBarChart
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.Typography
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RewardsScreen(
    user: UserEntity,
    insights: Insights,
    history: List<CoinHistoryItem>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.spacingMd, end = Dimens.spacingMd,
            top = contentPadding.calculateTopPadding() + Dimens.spacingSm,
            bottom = contentPadding.calculateBottomPadding() + Dimens.spacingLg
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        item { Text("Rewards", style = Typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) }

        item {
            val level = insights.level
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.radiusLg)).background(BrandGradient).padding(Dimens.spacingLg)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Level ${level.number} · ${level.title}", style = Typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                            Text("🪙 ${"%,d".format(user.totalCoins)}", style = Typography.displayLarge, color = Color.White)
                            Text(
                                "to spend in the Shop · ${"%,d".format(insights.lifetimeCoins)} earned in total",
                                style = Typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(64.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${level.number}", style = Typography.headlineMedium, color = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.height(Dimens.spacingMd))
                    LinearProgress(level.progress)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${level.coinsForLevel - level.coinsIntoLevel} coins to level ${level.number + 1}",
                        style = Typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                StatTile("🔥", "${insights.currentStreak}", "Current streak", Modifier.weight(1f))
                StatTile("🏅", "${insights.longestStreak}", "Best streak", Modifier.weight(1f))
                StatTile("✅", "${insights.totalDone}", "Tasks done", Modifier.weight(1f))
            }
        }

        item {
            InfoCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Dimens.spacingMd)) {
                    SectionTitle("Last 7 days")
                    Spacer(Modifier.height(Dimens.spacingMd))
                    if (insights.last7.isNotEmpty()) WeekBarChart(insights.last7)
                }
            }
        }

        item {
            val unlocked = insights.badges.count { it.unlocked }
            InfoCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Dimens.spacingMd)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Achievements", Modifier.weight(1f))
                        Text("$unlocked / ${insights.badges.size}", style = Typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(Dimens.spacingMd))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
                        maxItemsInEachRow = 3
                    ) {
                        insights.badges.forEach { BadgeCell(it) }
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(Dimens.radiusMd),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(Dimens.spacingMd), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Share ThumbPlan", style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                        Text("Invite friends to plan together", style = Typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    }
                    Button(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "I'm getting things done with ThumbPlan — wake-up alarms, coins and streaks! Use my code: ${user.referralCode}")
                            }
                            context.startActivity(Intent.createChooser(send, null))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Invite", color = MaterialTheme.colorScheme.onPrimary) }
                }
            }
        }

        item { SectionTitle("Coin history") }
        if (history.isEmpty()) {
            item { EmptyState("🪙", "No coins yet", "Complete a task to earn your first coins.") }
        } else {
            items(history, key = { it.id }) { HistoryRow(it) }
        }
    }
}

@Composable
private fun BadgeCell(badge: Badge) {
    Column(
        Modifier
            .width(100.dp)
            .alpha(if (badge.unlocked) 1f else 0.35f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = if (badge.unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            modifier = Modifier.size(52.dp)
        ) { Box(contentAlignment = Alignment.Center) { Text(if (badge.unlocked) badge.emoji else "🔒", style = Typography.titleLarge) } }
        Spacer(Modifier.height(4.dp))
        Text(badge.title, style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(badge.description, style = Typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HistoryRow(item: CoinHistoryItem) {
    InfoCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Dimens.spacingMd), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (item.amount < 0) "Bought ${item.label ?: "an item"}" else item.title ?: item.label ?: "Completed task",
                    style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                val sub = buildString {
                    item.doneTime?.let { append(Format.dateTime(it)) }
                    if (!item.comment.isNullOrBlank()) { if (isNotEmpty()) append(" · "); append("“${item.comment}”") }
                }
                if (sub.isNotEmpty()) Text(sub, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (item.amount >= 0) "+${item.amount} 🪙" else "${item.amount} 🪙",
                style = Typography.labelLarge,
                color = if (item.amount >= 0) MaterialTheme.colorScheme.primary else com.ginckl.thumbplan.ui.theme.Danger
            )
        }
    }
}
