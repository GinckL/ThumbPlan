package com.ginckl.thumbplan.ui.features.shop

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ginckl.thumbplan.data.local.entity.UserEntity
import com.ginckl.thumbplan.domain.ItemKind
import com.ginckl.thumbplan.domain.ShopCatalog
import com.ginckl.thumbplan.domain.ShopItem
import com.ginckl.thumbplan.services.ToneSynth
import com.ginckl.thumbplan.ui.components.BrandGradient
import com.ginckl.thumbplan.ui.components.InfoCard
import com.ginckl.thumbplan.ui.components.SectionTitle
import com.ginckl.thumbplan.ui.theme.Accents
import com.ginckl.thumbplan.ui.theme.Dimens
import com.ginckl.thumbplan.ui.theme.Success
import com.ginckl.thumbplan.ui.theme.Typography

@Composable
fun ShopScreen(
    user: UserEntity,
    inventory: Map<String, Int>,
    equippedAccent: String,
    equippedAvatar: String,
    equippedTone: String,
    contentPadding: PaddingValues,
    onBuy: (String) -> Unit,
    onEquip: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val preview = rememberTonePreview()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.spacingMd, end = Dimens.spacingMd,
            top = contentPadding.calculateTopPadding() + Dimens.spacingSm,
            bottom = contentPadding.calculateBottomPadding() + Dimens.spacingLg
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        item { Text("Coin shop", style = Typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) }

        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.radiusLg)).background(BrandGradient).padding(Dimens.spacingLg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Your balance", style = Typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                        Text("🪙 ${"%,d".format(user.totalCoins)}", style = Typography.displayLarge, color = Color.White, modifier = Modifier.testTag("shop_balance"))
                        Text("Finish tasks to earn more", style = Typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                    }
                    Text("🛍️", style = Typography.displayLarge)
                }
            }
        }

        ItemKind.entries.forEach { kind ->
            item(key = "title_${kind.name}") { SectionTitle(kind.title) }
            val list = ShopCatalog.ofKind(kind)
            items(list) { item ->
                val owned = ShopCatalog.isFree(item) || (inventory[item.id] ?: 0) > 0
                val qty = inventory[item.id] ?: 0
                val equipped = when (kind) {
                    ItemKind.ACCENT -> item.id == equippedAccent
                    ItemKind.AVATAR -> item.id == equippedAvatar
                    ItemKind.TONE -> item.id == equippedTone
                    ItemKind.BOOSTER -> false
                }
                ShopRow(
                    item = item,
                    owned = owned,
                    quantity = qty,
                    equipped = equipped,
                    canAfford = user.totalCoins >= item.price,
                    onBuy = { onBuy(item.id) },
                    onEquip = { onEquip(item.id) },
                    onPreview = if (kind == ItemKind.TONE && item.id != "system") ({ preview(item.id) }) else null
                )
            }
        }
    }
}


@Composable
private fun ShopRow(
    item: ShopItem,
    owned: Boolean,
    quantity: Int,
    equipped: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onPreview: (() -> Unit)?
) {
    val accent = if (item.kind == ItemKind.ACCENT) Accents.get(item.id) else null
    InfoCard(Modifier.fillMaxWidth().testTag("shop_item_${item.id}")) {
        Row(Modifier.padding(Dimens.spacingMd), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .then(if (accent != null) Modifier.background(Brush.linearGradient(accent.gradient)) else Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)))
                    .then(if (equipped) Modifier.border(3.dp, Success, CircleShape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(if (accent != null) "" else item.emoji, style = Typography.titleLarge)
            }
            Spacer(Modifier.width(Dimens.spacingMd))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(item.description, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.consumable && quantity > 0) {
                    Text("You have $quantity", style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                }
            }
            if (onPreview != null) {
                IconButton(onClick = onPreview) { Icon(Icons.Filled.PlayArrow, contentDescription = "Preview ${item.name}") }
            }
            when {
                item.consumable -> Button(
                    onClick = onBuy,
                    enabled = canAfford,
                    shape = RoundedCornerShape(Dimens.radiusPill),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("🪙 ${item.price}") }
                equipped -> Text("Equipped", style = Typography.labelLarge, color = Success)
                owned -> OutlinedButton(onClick = onEquip, shape = RoundedCornerShape(Dimens.radiusPill)) { Text("Use") }
                else -> Button(
                    onClick = onBuy,
                    enabled = canAfford,
                    shape = RoundedCornerShape(Dimens.radiusPill),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("🪙 ${item.price}") }
            }
        }
    }
}

/** Plays a tone for a few seconds; a new preview replaces the previous one. */
@Composable
private fun rememberTonePreview(): (String) -> Unit {
    val context = LocalContext.current
    val holder = remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { runCatching { holder.value?.release() }; holder.value = null } }
    return { toneId ->
        runCatching { holder.value?.release() }
        val file = ToneSynth.fileFor(context, toneId)
        holder.value = file?.let {
            runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                    setDataSource(it.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener { mp -> mp.release(); if (holder.value === mp) holder.value = null }
                }
            }.getOrNull()
        }
    }
}
