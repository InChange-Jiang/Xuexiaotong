package com.xuexiaotong.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.xuexiaotong.MainActivity
import com.xuexiaotong.R
import com.xuexiaotong.data.Store
import java.util.Calendar

/**
 * 提醒调度器：AlarmManager 定时触发通知（对应 uni-app 版 reminder.js）
 *
 * 后台存活方案：
 * 1. setExactAndAllowWhileIdle 精确闹钟（Doze 下也能准时触发；Android 12+ 需"闹钟和提醒"权限）
 * 2. BootReceiver 监听 开机/解锁启动/应用更新/改时间/改时区 自动重调度
 * 3. requestCode 使用稳定 key，重调度可覆盖旧闹钟、可整体取消（设置变更即时生效）
 * 4. 提醒 key 记录到 Store，幂等不重复
 */
object ReminderScheduler {

    const val CHANNEL_ID = "xxt_reminder"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_CONTENT = "extra_content"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "作业提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "作业截止与自定义日程提醒"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /** 全部可能的提醒 key（仅按未来时间筛选，不受提醒设置影响；用于取消闹钟，防止关闭提醒后旧闹钟残留） */
    private fun allReminderKeys(): List<String> {
        val now = System.currentTimeMillis()
        val keys = mutableListOf<String>()
        Store.getWorks().forEach { w ->
            val endTs = w.endTs ?: return@forEach
            if (endTs > now) keys.add("${w.workId}|$endTs")
        }
        Store.getCustomEvents().forEach { ev ->
            val startTs = ev.startTs
            if (startTs > 0 && startTs > now) keys.add("event_${ev.id}|$startTs")
        }
        return keys
    }

    /** 调度所有提醒（幂等，重启/同步后调用） */
    fun scheduleAll(context: Context) {
        ensureChannel(context)
        val setting = Store.getRemindSetting()
        if (!setting.enabled) return

        val now = System.currentTimeMillis()
        val works = Store.getWorks()
        val events = Store.getCustomEvents()
        val reminded = Store.getRemindedMap().toMutableMap()

        works.forEach { w ->
            val endTs = w.endTs ?: return@forEach
            if (endTs <= now) return@forEach
            if (w.isDone && !setting.onlyTodo) return@forEach

            val remindAt = endTs - setting.leadMinutes * 60000L
            if (remindAt <= now) return@forEach

            val key = "${w.workId}|$endTs"
            if (reminded.containsKey(key)) return@forEach

            val timeStr = formatTime(endTs)
            val ok = scheduleNotification(
                context, key, remindAt, w.courseName.ifEmpty { "作业提醒" },
                "${w.title} 将于 $timeStr 截止"
            )
            if (ok) reminded[key] = 1
        }

        events.forEach { ev ->
            if (ev.done) return@forEach
            val startTs = ev.startTs
            if (startTs <= 0 || startTs <= now) return@forEach

            val remindAt = startTs - setting.leadMinutes * 60000L
            if (remindAt <= now) return@forEach

            val key = "event_${ev.id}|$startTs"
            if (reminded.containsKey(key)) return@forEach

            val ok = scheduleNotification(
                context, key, remindAt, "日程提醒",
                "${ev.title} 将于 ${ev.startDate.substring(5)} ${ev.startTime} 开始"
            )
            if (ok) reminded[key] = 1
        }

        Store.saveRemindedMap(reminded)
    }

    /** 取消全部已调度闹钟（设置变更/清空数据时调用） */
    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        allReminderKeys().forEach { key ->
            val pi = PendingIntent.getBroadcast(
                context, key.hashCode(),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let {
                am.cancel(it)
                it.cancel()
            }
        }
    }

    /** 设置变更后整体重调度：取消旧闹钟 + 清空提醒记录 + 按新设置重新调度 */
    fun rescheduleAll(context: Context) {
        cancelAll(context)
        Store.saveRemindedMap(emptyMap())
        scheduleAll(context)
    }

    private fun formatTime(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val month = c.get(Calendar.MONTH) + 1
        val day = c.get(Calendar.DAY_OF_MONTH)
        val hh = c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val mm = c.get(Calendar.MINUTE).toString().padStart(2, '0')
        return "$month-$day $hh:$mm"
    }

    /** 稳定 requestCode = key.hashCode()：同 key 重调度覆盖旧闹钟 */
    private fun scheduleNotification(context: Context, key: String, fireTs: Long, title: String, content: String): Boolean {
        return try {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CONTENT, content)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                key.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    // 无精确闹钟权限：普通闹钟（可能延迟数分钟，提醒设置内已引导授权）
                    alarmManager.set(AlarmManager.RTC_WAKEUP, fireTs, pending)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireTs, pending)
                }
            } catch (e: SecurityException) {
                // Android 14+ 未授权时 setExactAndAllowWhileIdle 抛异常：降级普通闹钟
                alarmManager.set(AlarmManager.RTC_WAKEUP, fireTs, pending)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 是否拥有精确闹钟权限（Android 12+） */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        }
        return true
    }

    /** 测试通知：立即发送（不经闹钟调度，点击即有反馈） */
    fun sendTest(context: Context): Boolean {
        return try {
            ensureChannel(context)
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.notify(
                System.currentTimeMillis().hashCode(),
                buildReminderNotification(context, "学小通", "这是一条测试通知")
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}

/** 构造通知（AlarmReceiver 使用），点击跳转应用 */
fun buildReminderNotification(context: Context, title: String, content: String): android.app.Notification {
    ReminderScheduler.ensureChannel(context)
    val launch = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_notify)
        .setContentTitle(title)
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(launch)
        .build()
}
