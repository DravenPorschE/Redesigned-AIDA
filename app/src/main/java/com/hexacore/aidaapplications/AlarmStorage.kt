package com.hexacore.aidaapplications

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object AlarmStorage {
    private const val PREFS_NAME = "alarm_prefs"
    private const val KEY_ALARMS = "alarms"

    fun saveAlarms(context: Context, alarms: List<AlarmItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Always save in array format (["Mon","Tue"]) instead of a single string
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(alarms)

        editor.putString(KEY_ALARMS, json)
        editor.apply()
    }

    fun loadAlarms(context: Context): MutableList<AlarmItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ALARMS, null)
        return if (json != null) {
            val gson = GsonBuilder()
                .registerTypeAdapter(AlarmItem::class.java, AlarmItemDeserializer())
                .create()

            val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}
