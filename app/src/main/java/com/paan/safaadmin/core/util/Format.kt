package com.paan.safaadmin.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Int.asRupees(): String = "₹$this"

private val orderTimeFormat = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")

fun Long.asOrderTime(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(orderTimeFormat)

/** "Just now", "5 min ago", "2 h ago", otherwise the date. */
fun Long.asAgo(now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - this) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        else -> asOrderTime()
    }
}

/** "9876543210" -> "+91 98765 43210" */
fun String.asIndianPhone(): String =
    if (length == 10) "+91 ${take(5)} ${drop(5)}" else this