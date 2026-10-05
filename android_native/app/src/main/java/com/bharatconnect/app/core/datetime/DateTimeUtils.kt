package com.bharatconnect.app.core.datetime

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Robust ISO 8601 & SQL timestamp parser and formatter.
 * Prevents timezone offset truncation bugs (e.g. "23+00:00" or "98+00")
 * and converts UTC server timestamps into clean local device times ("13:12").
 */
object DateTimeUtils {

    fun formatMessageTime(rawTime: String?): String {
        if (rawTime.isNullOrBlank()) return ""
        val clean = rawTime.trim()

        // 1. If it's already HH:mm or H:mm or has AM/PM
        if (clean.matches(Regex("""^\d{1,2}:\d{2}(\s?[APap][Mm])?$"""))) {
            return clean
        }

        // 2. Try parsing known UTC timestamp formats to convert to user's local device timezone
        val utcFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm"
        )
        for (pattern in utcFormats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date: Date? = sdf.parse(clean)
                if (date != null) {
                    val outSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    return outSdf.format(date)
                }
            } catch (_: Exception) {}
        }

        // 3. Local space-separated timestamps (e.g. "2026-10-05 14:35:40")
        val localFormats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm"
        )
        for (pattern in localFormats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                val date: Date? = sdf.parse(clean)
                if (date != null) {
                    val outSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    return outSdf.format(date)
                }
            } catch (_: Exception) {}
        }

        // 4. Space-separated fallback
        if (clean.contains(" ")) {
            val parts = clean.split(" ")
            val timePart = parts.getOrNull(1) ?: clean
            val hoursMins = timePart.take(5)
            if (hoursMins.length == 5 && hoursMins[2] == ':') {
                return hoursMins
            }
        }

        // 5. Fallback: extract the first "HH:mm" pattern found in the string
        val match = Regex("""(\d{1,2}:\d{2})""").find(clean)
        return match?.value ?: clean.take(5)
    }
}
