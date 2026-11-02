package com.ginckl.thumbplan.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.sqrt

/** Pure, Android-free business rules (unit-tested). */

object Streaks {
    fun toLocalDates(times: List<Long>, zone: ZoneId = ZoneId.systemDefault()): Set<LocalDate> =
        times.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()

    /** Consecutive days with at least one completion, ending today (or yesterday, so the streak survives until midnight). */
    fun current(days: Set<LocalDate>, today: LocalDate): Int {
        var cursor = when {
            today in days -> today
            today.minusDays(1) in days -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    fun longest(days: Set<LocalDate>): Int {
        var best = 0
        for (d in days) {
            if (d.minusDays(1) in days) continue // not the start of a run
            var len = 0
            var c = d
            while (c in days) { len++; c = c.plusDays(1) }
            if (len > best) best = len
        }
        return best
    }
}

enum class RepeatMode(val key: String, val label: String) {
    NONE("none", "Once"),
    DAILY("daily", "Daily"),
    WEEKDAYS("weekdays", "Weekdays"),
    WEEKENDS("weekends", "Weekends"),
    WEEKLY("weekly", "Weekly");

    companion object {
        fun from(key: String?): RepeatMode = entries.firstOrNull { it.key == key } ?: NONE
    }
}

object Recurrence {
    private fun isWeekend(d: LocalDate) = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY

    /**
     * The next occurrence strictly after [fromTime] (epoch ms), keeping the time of day of [fromTime].
     * Returns null for non repeating tasks.
     */
    fun next(fromTime: Long, mode: RepeatMode, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (mode == RepeatMode.NONE) return null
        val start = Instant.ofEpochMilli(fromTime).atZone(zone)
        var date = start.toLocalDate().plusDays(1)
        when (mode) {
            RepeatMode.DAILY -> Unit
            RepeatMode.WEEKLY -> date = start.toLocalDate().plusWeeks(1)
            RepeatMode.WEEKDAYS -> while (isWeekend(date)) date = date.plusDays(1)
            RepeatMode.WEEKENDS -> while (!isWeekend(date)) date = date.plusDays(1)
            RepeatMode.NONE -> return null
        }
        return date.atTime(start.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
    }
}

/** Minutes ("15,10", "15m,5m") → list of positive minute values, largest first, de-duplicated. */
fun parseReminderMinutes(raw: String): List<Int> =
    raw.split(',')
        .mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        .filter { it > 0 }
        .distinct()
        .sortedDescending()

data class Level(val number: Int, val title: String, val progress: Float, val coinsIntoLevel: Int, val coinsForLevel: Int)

object Progression {
    private val titles = listOf("Beginner", "Planner", "Achiever", "Focused", "Pro", "Master", "Legend")

    /** Level n starts at 100 * (n-1)^2 coins: 0, 100, 400, 900, 1600 … */
    fun level(totalCoins: Int): Level {
        val coins = totalCoins.coerceAtLeast(0)
        val n = sqrt(coins / 100.0).toInt() + 1
        val start = 100 * (n - 1) * (n - 1)
        val span = 100 * (2 * n - 1)
        val into = coins - start
        return Level(n, titles[(n - 1).coerceAtMost(titles.lastIndex)], (into.toFloat() / span).coerceIn(0f, 1f), into, span)
    }
}

data class Stats(
    val totalDone: Int,
    val coins: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val earlyBirdDone: Int
)

data class Badge(val id: String, val emoji: String, val title: String, val description: String, val unlocked: Boolean)

object Badges {
    fun evaluate(s: Stats): List<Badge> = listOf(
        Badge("first", "🌱", "First Step", "Complete your first task", s.totalDone >= 1),
        Badge("ten", "⚡", "Getting Going", "Complete 10 tasks", s.totalDone >= 10),
        Badge("fifty", "🏆", "Unstoppable", "Complete 50 tasks", s.totalDone >= 50),
        Badge("streak3", "🔥", "On Fire", "Reach a 3-day streak", s.longestStreak >= 3),
        Badge("streak7", "🚀", "Week Warrior", "Reach a 7-day streak", s.longestStreak >= 7),
        Badge("streak30", "👑", "Habit Master", "Reach a 30-day streak", s.longestStreak >= 30),
        Badge("coins500", "🪙", "Coin Collector", "Earn 500 coins", s.coins >= 500),
        Badge("coins2000", "💎", "Treasure Hunter", "Earn 2,000 coins", s.coins >= 2000),
        Badge("early", "🌅", "Early Bird", "Complete a task before 8:00", s.earlyBirdDone >= 1)
    )
}
