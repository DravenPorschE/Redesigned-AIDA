package com.hexacore.aidaapplications

import java.text.SimpleDateFormat
import java.util.*

data class AlarmItem(
    var time: String,                   // e.g. "07:00 AM"
    var repeatDays: MutableList<String>,// e.g. ["Mon","Wed","Fri"]
    var isEnabled: Boolean
) {
    /**
     * Parse the "hh:mm a" style time and return a Calendar set to that time.
     * If the time already passed today, it advances to the next day.
     * For repeating alarms you'll compute day-of-week separately when scheduling.
     */
    fun toCalendar(): Calendar {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val parsed = try {
            format.parse(time)
        } catch (e: Exception) {
            null
        } ?: Date()

        val calNow = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            time = parsed
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // preserve today's date (we'll adjust day-of-week when scheduling repeats)
            set(Calendar.YEAR, calNow.get(Calendar.YEAR))
            set(Calendar.MONTH, calNow.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, calNow.get(Calendar.DAY_OF_MONTH))
        }

        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return cal
    }
}
