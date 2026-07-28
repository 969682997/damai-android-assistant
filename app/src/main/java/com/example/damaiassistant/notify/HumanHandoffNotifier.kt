package com.example.damaiassistant.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.damaiassistant.R

class HumanHandoffNotifier(private val context: Context) {
    fun notifyHandoff() {
        notify(
            title = context.getString(R.string.handoff_notification_title),
            text = context.getString(R.string.handoff_notification_text)
        )
    }

    fun notifyPaymentStop() {
        notify(
            title = context.getString(R.string.payment_notification_title),
            text = context.getString(R.string.payment_notification_text)
        )
    }

    private fun notify(title: String, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH)
            )
        }
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_assistant)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    private companion object {
        const val CHANNEL_ID = "human_handoff"
        const val NOTIFICATION_ID = 1003
    }
}

