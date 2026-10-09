package com.replock.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Buckets (timestamp, amount) entries into per-day totals for the last [days]
 * days, oldest first, ending on [today]. Days with no activity are 0.
 */
fun bucketByDay(
    entries: List<Pair<Long, Int>>,
    days: Int,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): List<Int> {
    if (days <= 0) return emptyList()
    val start = today.minusDays((days - 1).toLong())
    val buckets = IntArray(days)
    for ((timestamp, amount) in entries) {
        val day = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val index = ChronoUnit.DAYS.between(start, day).toInt()
        if (index in 0 until days) buckets[index] += amount
    }
    return buckets.toList()
}
