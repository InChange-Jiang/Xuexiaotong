package com.xuexiaotong

import android.app.Application
import com.xuexiaotong.data.NoteStore
import com.xuexiaotong.data.Store
import com.xuexiaotong.reminder.ReminderScheduler

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        NoteStore.init(this)
        ReminderScheduler.ensureChannel(this)
        ReminderScheduler.scheduleAll(this)
    }
}
