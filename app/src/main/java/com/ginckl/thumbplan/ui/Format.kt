package com.ginckl.thumbplan.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object Format {
    private val timeFormatter get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())

    fun time(millis: Long): String =
        timeFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    fun time(t: LocalTime): String = timeFormatter.format(t)

    fun date(d: LocalDate, today: LocalDate = LocalDate.now()): String = when (d) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> DateTimeFormatter.ofPattern(if (d.year == today.year) "EEE, d MMM" else "EEE, d MMM yyyy", Locale.getDefault()).format(d)
    }

    fun dateTime(millis: Long): String {
        val z = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return "${date(z.toLocalDate())} · ${time(millis)}"
    }

    fun greeting(hour: Int = LocalTime.now().hour): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}
