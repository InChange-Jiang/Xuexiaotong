package com.xuexiaotong.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xuexiaotong.data.Store

/**
 * 开机/解锁启动/应用更新/改时间/改时区后自动重调度提醒（对抗杀后台丢闹钟）
 *
 * 注意：系统重启会清空 AlarmManager 的闹钟注册，但 Store 中持久化的
 * reminded map 仍然存在。因此调度前必须先清空 reminded map，否则
 * scheduleAll 认为所有提醒已触发而跳过，导致重启后闹钟全部丢失。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // 系统重启后清空提醒标记，确保所有闹钟重新注册到 AlarmManager
                Store.saveRemindedMap(emptyMap())
                ReminderScheduler.scheduleAll(context)
            }
        }
    }
}
