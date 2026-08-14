package com.xuexiaotong.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xuexiaotong.data.ChaoxingApi
import com.xuexiaotong.data.Course
import com.xuexiaotong.data.CourseProgress
import com.xuexiaotong.data.CustomEvent
import com.xuexiaotong.data.RemindSetting
import com.xuexiaotong.data.Store
import com.xuexiaotong.data.Work
import com.xuexiaotong.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SyncProgress(val done: Int = 0, val total: Int = 0, val message: String = "")

class AppViewModel(val api: ChaoxingApi, private val appContext: Context) : ViewModel() {

    private val _loggedIn = MutableStateFlow(api.hasSession())
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _works = MutableStateFlow<List<Work>>(Store.getWorks())
    val works: StateFlow<List<Work>> = _works.asStateFlow()

    private val _courses = MutableStateFlow<List<Course>>(Store.getCourses())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _progress = MutableStateFlow<List<CourseProgress>>(Store.getCourseProgress())
    val progress: StateFlow<List<CourseProgress>> = _progress.asStateFlow()

    // 日程同步（作业）独立状态
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    // 课程进度同步独立状态
    private val _courseSyncing = MutableStateFlow(false)
    val courseSyncing: StateFlow<Boolean> = _courseSyncing.asStateFlow()

    private val _courseSyncProgress = MutableStateFlow(SyncProgress())
    val courseSyncProgress: StateFlow<SyncProgress> = _courseSyncProgress.asStateFlow()

    // 最近一次成功同步时间（毫秒，0 = 从未同步）
    private val _lastSync = MutableStateFlow(Store.getLastSync())
    val lastSync: StateFlow<Long> = _lastSync.asStateFlow()

    private val _dark = MutableStateFlow(Store.getDarkMode())
    val dark: StateFlow<Boolean> = _dark.asStateFlow()

    private val _themeId = MutableStateFlow(Store.getThemeId())
    val themeId: StateFlow<String> = _themeId.asStateFlow()

    private val _remindSetting = MutableStateFlow(Store.getRemindSetting())
    val remindSetting: StateFlow<RemindSetting> = _remindSetting.asStateFlow()

    private val _customEvents = MutableStateFlow<List<CustomEvent>>(Store.getCustomEvents())
    val customEvents: StateFlow<List<CustomEvent>> = _customEvents.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    fun consumeSnackbar() {
        _snackbar.value = null
    }

    fun showMsg(msg: String) {
        _snackbar.value = msg
    }

    fun refreshState() {
        _loggedIn.value = api.hasSession()
        _works.value = Store.getWorks()
        _courses.value = Store.getCourses()
        _progress.value = Store.getCourseProgress()
    }

    fun onLoginSuccess() {
        _loggedIn.value = true
        refreshState()
        syncWorks()
    }

    fun logout() {
        api.clearSession()
        Store.clearLoginData()
        Store.clearCredential()   // 手动退出时一并清除加密保存的账号密码
        _loggedIn.value = false
        _works.value = emptyList()
        _courses.value = emptyList()
        _progress.value = emptyList()
    }

    /** 同步全部作业（日程页刷新） */
    fun syncWorks() {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            _syncProgress.value = SyncProgress(message = "正在同步作业...")
            try {
                val works = withContext(Dispatchers.IO) {
                    // 抓取前静默退出重登，刷新最新登录态（失败自动回滚旧 cookie，不阻塞）
                    api.silentRelogin()
                    api.syncAllWorks(object : ChaoxingApi.ProgressListener {
                        override fun onProgress(done: Int, total: Int, message: String) {
                            _syncProgress.value = SyncProgress(done, total, message)
                        }
                    })
                }
                _works.value = works
                _courses.value = Store.getCourses()
                _syncProgress.value = SyncProgress(message = "同步完成")
                Store.saveLastSync(System.currentTimeMillis())
                _lastSync.value = Store.getLastSync()
            } catch (e: Exception) {
                _syncProgress.value = SyncProgress(message = e.message ?: "同步失败")
            } finally {
                _syncing.value = false
                ReminderScheduler.scheduleAll(appContext)
            }
        }
    }

    /** 同步课程进度（课程页刷新） */
    fun syncCourseProgress() {
        if (_courseSyncing.value) return
        viewModelScope.launch {
            _courseSyncing.value = true
            _courseSyncProgress.value = SyncProgress(message = "正在同步课程进度...")
            try {
                val list = withContext(Dispatchers.IO) {
                    // 抓取前静默退出重登，刷新最新登录态（失败自动回滚旧 cookie，不阻塞）
                    api.silentRelogin()
                    api.syncCourseProgress(object : ChaoxingApi.ProgressListener {
                        override fun onProgress(done: Int, total: Int, message: String) {
                            _courseSyncProgress.value = SyncProgress(done, total, message)
                        }
                    })
                }
                _progress.value = list
                _courseSyncProgress.value = SyncProgress(message = "同步完成")
                Store.saveLastSync(System.currentTimeMillis())
                _lastSync.value = Store.getLastSync()
            } catch (e: Exception) {
                _courseSyncProgress.value = SyncProgress(message = e.message ?: "同步失败")
            } finally {
                _courseSyncing.value = false
            }
        }
    }

    fun toggleDark() {
        val next = Store.toggleDark()
        _dark.value = next
    }

    fun selectTheme(id: String) {
        Store.saveThemeId(id)
        _themeId.value = id
    }

    fun saveRemind(setting: RemindSetting) {
        Store.saveRemindSetting(setting)
        _remindSetting.value = setting
        // 设置变更：取消旧闹钟 + 清空提醒记录 + 按新设置重新调度
        ReminderScheduler.rescheduleAll(appContext)
    }

    fun sendTestNotification() {
        ReminderScheduler.sendTest(appContext)
    }

    fun saveCustomEvents(list: List<CustomEvent>) {
        Store.saveCustomEvents(list)
        _customEvents.value = list
        // 增删改日程：整体重调度，避免旧闹钟残留
        ReminderScheduler.rescheduleAll(appContext)
    }

    /** 添加自定义日程 */
    fun addCustomEvent(ev: CustomEvent) {
        val list = _customEvents.value.toMutableList()
        list.add(ev)
        saveCustomEvents(list)
    }

    /** 标记自定义日程完成/未完成 */
    fun toggleCustomEventDone(id: String) {
        val list = _customEvents.value.map {
            if (it.id == id) it.copy(done = !it.done) else it
        }
        saveCustomEvents(list)
    }

    /** 删除自定义日程 */
    fun deleteCustomEvent(id: String) {
        val list = _customEvents.value.filter { it.id != id }
        saveCustomEvents(list)
    }

    // 菜单开关状态
    private val _showCompleted = MutableStateFlow(true)
    val showCompleted: StateFlow<Boolean> = _showCompleted.asStateFlow()

    private val _doneGray = MutableStateFlow(Store.getDoneGray())
    val doneGray: StateFlow<Boolean> = _doneGray.asStateFlow()

    private val _showEmptyCourses = MutableStateFlow(Store.getShowEmptyCourses())
    val showEmptyCourses: StateFlow<Boolean> = _showEmptyCourses.asStateFlow()

    private val _glassEnabled = MutableStateFlow(Store.getGlassEnabled())
    val glassEnabled: StateFlow<Boolean> = _glassEnabled.asStateFlow()

    fun toggleShowCompleted() {
        val next = !_showCompleted.value
        _showCompleted.value = next
    }

    fun toggleDoneGray() {
        val next = !_doneGray.value
        Store.saveDoneGray(next)
        _doneGray.value = next
    }

    fun toggleShowEmptyCourses() {
        val next = !_showEmptyCourses.value
        Store.saveShowEmptyCourses(next)
        _showEmptyCourses.value = next
    }

    fun toggleGlass() {
        val next = !_glassEnabled.value
        _glassEnabled.value = next
        Store.saveGlassEnabled(next)
    }

    fun clearCustomEvents() {
        Store.saveCustomEvents(emptyList())
        _customEvents.value = emptyList()
        // 清空日程：取消相关闹钟并重调度（rescheduleAll 会取消所有候选闹钟再按新数据调度）
        ReminderScheduler.rescheduleAll(appContext)
    }

    class Factory(private val api: ChaoxingApi, private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(api, appContext) as T
        }
    }
}
