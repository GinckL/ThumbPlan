package com.ginckl.thumbplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DomainTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private val today = LocalDate.of(2026, 10, 23) // Friday

    private fun ms(date: LocalDate, hour: Int = 9) = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    // ---- Streaks ----
    @Test fun streak_consecutiveDaysEndingToday() {
        val days = setOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, Streaks.current(days, today))
    }

    @Test fun streak_survivesUntilMidnightWhenTodayIsEmpty() {
        val days = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, Streaks.current(days, today))
    }

    @Test fun streak_breaksAfterAMissedDay() {
        val days = setOf(today.minusDays(2), today.minusDays(3))
        assertEquals(0, Streaks.current(days, today))
    }

    @Test fun streak_gapDoesNotCount() {
        val days = setOf(today, today.minusDays(2), today.minusDays(3))
        assertEquals(1, Streaks.current(days, today))
    }

    @Test fun longestStreak_findsBestRun() {
        val days = setOf(today, today.minusDays(5), today.minusDays(6), today.minusDays(7), today.minusDays(8))
        assertEquals(4, Streaks.longest(days))
        assertEquals(0, Streaks.longest(emptySet()))
    }

    @Test fun toLocalDates_usesTheGivenZone() {
        // 23:30 local on the 22nd is the 22nd in Tashkent, even though it is already 18:30 UTC... same day; use 01:00 local
        val oneAmLocal = LocalDateTime.of(2026, 10, 23, 1, 0).atZone(zone).toInstant().toEpochMilli() // 20:00 UTC on the 22nd
        assertEquals(setOf(LocalDate.of(2026, 10, 23)), Streaks.toLocalDates(listOf(oneAmLocal), zone))
    }

    // ---- Recurrence ----
    @Test fun recurrence_none() = assertNull(Recurrence.next(ms(today), RepeatMode.NONE, zone))

    @Test fun recurrence_dailyKeepsTimeOfDay() {
        assertEquals(ms(today.plusDays(1), 7), Recurrence.next(ms(today, 7), RepeatMode.DAILY, zone))
    }

    @Test fun recurrence_weekdaysSkipsWeekend() {
        // Friday -> Monday
        assertEquals(ms(today.plusDays(3)), Recurrence.next(ms(today), RepeatMode.WEEKDAYS, zone))
    }

    @Test fun recurrence_weekendsFromFridayGoesToSaturday() {
        assertEquals(ms(today.plusDays(1)), Recurrence.next(ms(today), RepeatMode.WEEKENDS, zone))
        // Saturday -> Sunday, Sunday -> next Saturday
        assertEquals(ms(today.plusDays(2)), Recurrence.next(ms(today.plusDays(1)), RepeatMode.WEEKENDS, zone))
        assertEquals(ms(today.plusDays(8)), Recurrence.next(ms(today.plusDays(2)), RepeatMode.WEEKENDS, zone))
    }

    @Test fun recurrence_weekly() {
        assertEquals(ms(today.plusWeeks(1)), Recurrence.next(ms(today), RepeatMode.WEEKLY, zone))
    }

    @Test fun repeatMode_unknownKeyFallsBackToNone() {
        assertEquals(RepeatMode.NONE, RepeatMode.from("garbage"))
        assertEquals(RepeatMode.NONE, RepeatMode.from(null))
        assertEquals(RepeatMode.WEEKDAYS, RepeatMode.from("weekdays"))
    }

    // ---- Reminders ----
    @Test fun reminders_parseBothLegacyAndNewFormat() {
        assertEquals(listOf(15, 10, 5, 1), parseReminderMinutes("15m,10m,5m,1m"))
        assertEquals(listOf(30, 5), parseReminderMinutes("5,30,5"))
        assertEquals(emptyList<Int>(), parseReminderMinutes(""))
        assertEquals(emptyList<Int>(), parseReminderMinutes("0,abc"))
    }

    // ---- Levels ----
    @Test fun level_boundaries() {
        assertEquals(1, Progression.level(0).number)
        assertEquals(1, Progression.level(99).number)
        assertEquals(2, Progression.level(100).number)
        assertEquals(2, Progression.level(399).number)
        assertEquals(3, Progression.level(400).number)
        assertEquals(1, Progression.level(-50).number)
    }

    @Test fun level_progressIsWithinLevel() {
        val l = Progression.level(250) // level 2: 100..399
        assertEquals(2, l.number)
        assertEquals(150, l.coinsIntoLevel)
        assertEquals(300, l.coinsForLevel)
        assertEquals(0.5f, l.progress, 0.001f)
    }

    @Test fun level_titleCapsAtLegend() {
        assertEquals("Legend", Progression.level(1_000_000).title)
    }

    // ---- Badges ----
    @Test fun badges_unlockFromStats() {
        val none = Badges.evaluate(Stats(0, 0, 0, 0, 0))
        assertTrue(none.none { it.unlocked })
        val some = Badges.evaluate(Stats(12, 600, 3, 7, 1)).filter { it.unlocked }.map { it.id }
        assertTrue("first" in some && "ten" in some && "streak3" in some && "streak7" in some && "coins500" in some && "early" in some)
        assertFalse("fifty" in some || "streak30" in some || "coins2000" in some)
    }
}

class ShopDomainTest {
    private val today = LocalDate.of(2026, 10, 23)

    @Test fun freeze_coversGapWhenEnoughFreezes() {
        val active = setOf(today.minusDays(3), today.minusDays(4))
        // gap = yesterday(-1) and -2 -> 2 days
        assertEquals(listOf(today.minusDays(2), today.minusDays(1)), FreezePlanner.plan(active, today, 2))
    }

    @Test fun freeze_doesNothingWhenGapTooBig() {
        val active = setOf(today.minusDays(6))
        assertEquals(emptyList<LocalDate>(), FreezePlanner.plan(active, today, 2))
    }

    @Test fun freeze_noGapNoFreeze() {
        assertEquals(emptyList<LocalDate>(), FreezePlanner.plan(setOf(today), today, 5))
        assertEquals(emptyList<LocalDate>(), FreezePlanner.plan(setOf(today.minusDays(1)), today, 5))
        assertEquals(emptyList<LocalDate>(), FreezePlanner.plan(emptySet(), today, 5))
        assertEquals(emptyList<LocalDate>(), FreezePlanner.plan(setOf(today.minusDays(3)), today, 0))
    }

    @Test fun freeze_keepsStreakAlive() {
        val active = setOf(today.minusDays(4), today.minusDays(3))
        val frozen = FreezePlanner.plan(active, today, 5).toSet()
        // after freezing and completing something today the streak runs through the frozen days
        assertEquals(5, Streaks.current(active + frozen + today, today))
    }

    @Test fun catalog_idsUniqueAndSane() {
        val ids = ShopCatalog.items.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ShopCatalog.items.all { it.price >= 0 })
        // every kind has a free default the user owns from the start
        assertTrue(ShopCatalog.isFree(ShopCatalog.find(ShopCatalog.DEFAULT_ACCENT)!!))
        assertTrue(ShopCatalog.isFree(ShopCatalog.find(ShopCatalog.DEFAULT_AVATAR)!!))
        assertTrue(ShopCatalog.isFree(ShopCatalog.find(ShopCatalog.DEFAULT_TONE)!!))
        assertTrue(ShopCatalog.find(ShopCatalog.FREEZE)!!.consumable)
        assertTrue(ShopCatalog.find(ShopCatalog.BOOST)!!.consumable)
    }

    @Test fun catalog_allPaidItemsAreReachable() {
        // the cheapest paid item should be affordable with a handful of tasks (50 coins each)
        val cheapest = ShopCatalog.items.filter { it.price > 0 }.minOf { it.price }
        assertTrue(cheapest <= 250)
    }
}

class SyncPlannerTest {
    @Test fun noRemote_uploads() = assertEquals(SyncAction.UPLOAD, SyncPlanner.decide("L", null, null, false))
    @Test fun same_nothing() = assertEquals(SyncAction.NOTHING, SyncPlanner.decide("X", "X", "Y", false))
    @Test fun freshInstall_downloads() = assertEquals(SyncAction.DOWNLOAD, SyncPlanner.decide("empty", "R", null, true))
    @Test fun onlyRemoteChanged_downloads() = assertEquals(SyncAction.DOWNLOAD, SyncPlanner.decide("B", "R", "B", false))
    @Test fun onlyLocalChanged_uploads() = assertEquals(SyncAction.UPLOAD, SyncPlanner.decide("L", "B", "B", false))
    @Test fun bothChanged_conflict() = assertEquals(SyncAction.CONFLICT, SyncPlanner.decide("L", "R", "B", false))
    @Test fun neverSyncedButHasData_conflict() = assertEquals(SyncAction.CONFLICT, SyncPlanner.decide("L", "R", null, false))
    @Test fun conflictResolution_keepLocal() = assertEquals(SyncAction.UPLOAD, SyncPlanner.decide("L", "R", "B", false, true))
    @Test fun conflictResolution_useBackup() = assertEquals(SyncAction.DOWNLOAD, SyncPlanner.decide("L", "R", "B", false, false))
}
