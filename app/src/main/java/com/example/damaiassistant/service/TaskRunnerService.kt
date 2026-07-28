package com.example.damaiassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.SystemClock
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.damaiassistant.R
import com.example.damaiassistant.data.SharedPreferencesStore
import com.example.damaiassistant.data.TaskRepository

class TaskRunnerService : Service() {
    private var releaseAlarm: PendingIntent? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_assistant)
            .setContentTitle(getString(R.string.runner_notification_title))
            .setContentText(getString(R.string.runner_notification_text))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        if (intent?.action == ACTION_START) scheduleRelease()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseAlarm?.let { getSystemService(AlarmManager::class.java).cancel(it) }
        super.onDestroy()
    }

    private fun scheduleRelease() {
        val task = TaskRepository(
            SharedPreferencesStore(getSharedPreferences("damai_assistant", MODE_PRIVATE))
        ).load() ?: return
        if (task.releaseAtEpochMs <= System.currentTimeMillis()) return
        val intent = Intent(this, TaskRunnerService::class.java).setAction(ACTION_RELEASE)
        releaseAlarm = PendingIntent.getService(
            this,
            RELEASE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.releaseAtEpochMs,
                releaseAlarm!!
            )
        } catch (_: SecurityException) {
            releaseAlarm = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object {
        const val ACTION_START = "com.example.damaiassistant.action.START"
        const val ACTION_STOP = "com.example.damaiassistant.action.STOP"
        const val ACTION_RELEASE = "com.example.damaiassistant.action.RELEASE"
        private const val CHANNEL_ID = "task_runner"
        private const val NOTIFICATION_ID = 1001
        private const val RELEASE_REQUEST_CODE = 1002
    }
}
