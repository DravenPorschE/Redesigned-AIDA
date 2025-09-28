package com.hexacore.aidaapplications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Stop the alarm service
        context.stopService(Intent(context, AlarmService::class.java))

        // 🔔 Tell MainActivity to hide the banner
        val hideIntent = Intent("com.hexacore.aidaapplications.ALARM_DISMISSED")
        context.sendBroadcast(hideIntent)
    }
}
