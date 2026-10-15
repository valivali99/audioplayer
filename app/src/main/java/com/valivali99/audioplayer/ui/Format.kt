package com.valivali99.audioplayer.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 3:07, or 1:02:45 for long recordings. */
fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

fun formatDate(epochSec: Long): String = dateFormat.format(Date(epochSec * 1000))
