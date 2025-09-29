package com.hexacore.aidaapplications

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmTime = intent?.getStringExtra("alarm_time")

        // ✅ Use chosen ringtone (default = ringtone1)
        val ringtoneId = intent?.getIntExtra("ringtoneId", R.raw.ringtone1) ?: R.raw.ringtone1

        // stop previous sound if service restarts
        mediaPlayer?.stop()
        mediaPlayer?.release()

        // 🔊 Play alarm sound
        mediaPlayer = MediaPlayer.create(this, ringtoneId)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        // Show notification styled like system alarm
        startForeground(1, createNotification(alarmTime))

        // 🔔 Tell MainActivity to show banner
        val localIntent = Intent("com.hexacore.aidaapplications.ALARM_RINGING")
        localIntent.putExtra("message", "Alarm at $alarmTime is ringing!")
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)

        return START_STICKY
    }

    private fun createNotification(alarmTime: String?): Notification {
        val channelId = "alarm_channel"
        val channelName = "Alarm Notifications"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 👉 PendingIntent for dismiss (uses AlarmStopReceiver)
        val dismissIntent = Intent(this, AlarmStopReceiver::class.java)
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alarm")
            .setContentText(alarmTime ?: "Your alarm is ringing!")
            .setSmallIcon(R.drawable.alarm_app_icon)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
