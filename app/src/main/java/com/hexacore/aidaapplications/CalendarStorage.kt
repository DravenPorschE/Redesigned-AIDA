package com.hexacore.aidaapplications

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CalendarStorage {
    private const val PREFS_NAME = "calendar_storage"
    private const val KEY_EVENTS = "events"

    private val gson = Gson()

    // Save events map
    fun saveEvents(context: Context, events: Map<String, MutableList<CalendarEvent>>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(events)
        prefs.edit().putString(KEY_EVENTS, json).apply()
    }

    // Load events map
    fun loadEvents(context: Context): MutableMap<String, MutableList<CalendarEvent>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_EVENTS, null)

        return if (json != null) {
            val type = object : TypeToken<MutableMap<String, MutableList<CalendarEvent>>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableMapOf()
        }
    }
}
