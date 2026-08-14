package com.xuexiaotong.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 闹钟触发后的通知接收器（原生通知接口，点击跳转应用）
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "学小通"
        val content = intent.getStringExtra(ReminderScheduler.EXTRA_CONTENT) ?: "提醒"

        ReminderScheduler.ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(
            System.currentTimeMillis().hashCode(),
            buildReminderNotification(context, title, content)
        )
    }
}
