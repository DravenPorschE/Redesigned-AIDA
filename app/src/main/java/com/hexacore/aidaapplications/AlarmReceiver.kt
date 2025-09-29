package com.hexacore.aidaapplications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val time = intent.getStringExtra("alarm_time")
        val days = intent.getStringArrayExtra("alarm_days")

        Log.d("AlarmReceiver", "Alarm triggered at $time on ${days?.joinToString()}")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_time", time)
            putExtra("alarm_days", days)
        }

        context.startForegroundService(serviceIntent) // use foreground service
    }
}
