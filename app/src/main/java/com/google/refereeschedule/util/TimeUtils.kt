package com.google.refereeschedule.util

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private val format24h = SimpleDateFormat("HH:mm", Locale.US)
    private val format12h = SimpleDateFormat("h:mm a", Locale.US)

    /**
     * Converts a 24-hour format time string (HH:mm) to 12-hour format (h:mm AM/PM).
     * If parsing fails, returns the original string.
     */
    fun formatTo12h(time24h: String): String {
        return try {
            val date = format24h.parse(time24h)
            if (date != null) format12h.format(date) else time24h
        } catch (e: Exception) {
            time24h
        }
    }

    /**
     * Converts a 12-hour format time string (h:mm AM/PM) to 24-hour format (HH:mm).
     * If parsing fails, returns the original string.
     */
    fun formatTo24h(time12h: String): String {
        return try {
            val date = format12h.parse(time12h)
            if (date != null) format24h.format(date) else time12h
        } catch (e: Exception) {
            time12h
        }
    }
}
