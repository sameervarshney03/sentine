package com.example.sentine.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.sentine.DISMISS_ALL_ALERTS") {
            val notifId = intent.getIntExtra("NOTIFICATION_ID", -1)
            if (notifId != -1) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(notifId)
            }
        }
    }
}
