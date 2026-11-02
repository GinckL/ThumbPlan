package com.ginckl.thumbplan.domain

import java.time.LocalDate

enum class ItemKind(val title: String) {
    BOOSTER("Power-ups"),
    ACCENT("App colours"),
    AVATAR("Avatars"),
    TONE("Alarm tones")
}

data class ShopItem(
    val id: String,
    val kind: ItemKind,
    val name: String,
    val emoji: String,
    val price: Int,
    val description: String,
    /** Consumables can be bought repeatedly and are used up; everything else is a one-time unlock. */
    val consumable: Boolean = false
)

object ShopCatalog {
    const val DEFAULT_ACCENT = "violet"
    const val DEFAULT_AVATAR = "classic"
    const val DEFAULT_TONE = "system"
    const val FREEZE = "streak_freeze"
    const val BOOST = "double_coins"
    const val MAX_STACK = 10

    val items: List<ShopItem> = listOf(
        ShopItem(FREEZE, ItemKind.BOOSTER, "Streak Freeze", "🧊", 250, "Covers a missed day so your streak survives. Used automatically.", consumable = true),
        ShopItem(BOOST, ItemKind.BOOSTER, "Double Coins", "⚡", 200, "Your next completed task pays 2× coins.", consumable = true),

        ShopItem("violet", ItemKind.ACCENT, "Violet", "🟣", 0, "The classic ThumbPlan look."),
        ShopItem("ocean", ItemKind.ACCENT, "Ocean", "🔵", 300, "Cool blues and teal."),
        ShopItem("sunset", ItemKind.ACCENT, "Sunset", "🟠", 300, "Warm orange to pink."),
        ShopItem("forest", ItemKind.ACCENT, "Forest", "🟢", 300, "Fresh greens."),
        ShopItem("rose", ItemKind.ACCENT, "Rose", "🌸", 400, "Soft pink and magenta."),
        ShopItem("gold", ItemKind.ACCENT, "Gold", "🟡", 600, "For true legends."),

        ShopItem("classic", ItemKind.AVATAR, "Classic", "🙂", 0, "Where everyone starts."),
        ShopItem("fox", ItemKind.AVATAR, "Fox", "🦊", 150, "Clever and quick."),
        ShopItem("panda", ItemKind.AVATAR, "Panda", "🐼", 150, "Calm and steady."),
        ShopItem("unicorn", ItemKind.AVATAR, "Unicorn", "🦄", 250, "Rare and magical."),
        ShopItem("rocket", ItemKind.AVATAR, "Rocket", "🚀", 250, "Going places."),
        ShopItem("dragon", ItemKind.AVATAR, "Dragon", "🐲", 400, "Fearless."),
        ShopItem("crown", ItemKind.AVATAR, "Crown", "👑", 600, "Royalty of routines."),

        ShopItem("system", ItemKind.TONE, "Phone default", "📱", 0, "Your phone's alarm tone."),
        ShopItem("chime", ItemKind.TONE, "Chime", "🔔", 200, "A gentle rising arpeggio."),
        ShopItem("digital", ItemKind.TONE, "Digital", "⏱️", 200, "Classic double beeps."),
        ShopItem("marimba", ItemKind.TONE, "Marimba", "🎵", 350, "Warm, bouncy notes."),
        ShopItem("sunrise", ItemKind.TONE, "Sunrise", "🌅", 350, "A slow rise that wakes you kindly."),
        ShopItem("zen", ItemKind.TONE, "Zen bowl", "🪘", 500, "Soft singing-bowl tones.")
    )

    private val byId = items.associateBy { it.id }
    fun find(id: String?): ShopItem? = id?.let { byId[it] }
    fun ofKind(kind: ItemKind) = items.filter { it.kind == kind }

    /** Items every user owns from the start. */
    fun isFree(item: ShopItem) = item.price == 0
}

/** Decides which missed days a stack of Streak Freezes should cover. */
object FreezePlanner {
    /**
     * [active] = days with a completion (plus already frozen days). If there is a gap between the last active day and
     * yesterday that the [available] freezes can fully cover, returns those days (oldest first); otherwise nothing,
     * because a partly covered gap would not save the streak anyway.
     */
    fun plan(active: Set<LocalDate>, today: LocalDate, available: Int): List<LocalDate> {
        if (available <= 0 || active.isEmpty()) return emptyList()
        val last = active.max()
        val yesterday = today.minusDays(1)
        if (last >= yesterday) return emptyList()
        val gap = (yesterday.toEpochDay() - last.toEpochDay()).toInt()
        if (gap > available) return emptyList()
        return (1..gap).map { last.plusDays(it.toLong()) }
    }
}
