package com.hexacore.aidaapplications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Stop the alarm service
        context.stopService(Intent(context, AlarmService::class.java))

        // Tell MainActivity (and other local listeners) to hide the banner
        val hideIntent = Intent("com.hexacore.aidaapplications.ALARM_DISMISSED")
        LocalBroadcastManager.getInstance(context).sendBroadcast(hideIntent)
    }
}
