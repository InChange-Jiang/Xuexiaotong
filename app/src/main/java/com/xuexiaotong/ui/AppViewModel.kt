package com.xuexiaotong.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xuexiaotong.data.AppUpdater
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

    // 自动更新状态
    private val updater = AppUpdater(appContext)
    private val _checkingUpdate = MutableStateFlow(false)
    val checkingUpdate: StateFlow<Boolean> = _checkingUpdate.asStateFlow()

    private val _updateInfo = MutableStateFlow<AppUpdater.UpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdater.UpdateInfo?> = _updateInfo.asStateFlow()

    private val _downloading = MutableStateFlow(false)
    val downloading: StateFlow<Boolean> = _downloading.asStateFlow()

    /** 关闭更新弹窗，清除更新信息 */
    fun dismissUpdate() {
        _updateInfo.value = null
    }

    /** 检查更新：比对 GitHub 最新 release 版本号。
     * @param silent true=静默检查（仅更新 _updateInfo，不弹 snackbar），用于启动时自动检查
     *                false=显示 snackbar 结果反馈，用于手动菜单点击
     */
    fun checkForUpdate(silent: Boolean = false) {
        if (_checkingUpdate.value || _downloading.value) return
        viewModelScope.launch {
            _checkingUpdate.value = true
            _updateInfo.value = null
            try {
                val info = withContext(Dispatchers.IO) { updater.checkVersion() }
                _updateInfo.value = info
                if (!silent) {
                    if (info.hasUpdate) {
                        showMsg("发现新版本 v${info.latestVersion}")
                    } else {
                        showMsg("当前已是最新版本 (v${info.latestVersion})")
                    }
                }
            } catch (e: Exception) {
                if (!silent) showMsg(e.message ?: "检查更新失败，请稍后重试")
            } finally {
                _checkingUpdate.value = false
            }
        }
    }

    /** 开始下载最新 APK，下载完成后引导安装 */
    fun startUpdate() {
        val info = _updateInfo.value ?: return
        if (info.downloadUrl.isBlank()) {
            showMsg("下载链接无效")
            return
        }
        if (_downloading.value) return
        viewModelScope.launch {
            _downloading.value = true
            showMsg("正在下载更新...")
            try {
                val file = withContext(Dispatchers.IO) {
                    updater.downloadApk(info.downloadUrl)
                }
                _downloading.value = false
                if (updater.installApk(file)) {
                    showMsg("正在安装...")
                } else {
                    showMsg("安装失败，请手动安装 APK")
                }
            } catch (e: Exception) {
                _downloading.value = false
                showMsg(e.message ?: "下载失败，请稍后重试")
            }
        }
    }

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
        // 退出登录前取消所有提醒，避免旧闹钟残留
        ReminderScheduler.cancelAll(appContext)
        Store.saveRemindedMap(emptyMap())
        api.clearSession()
        Store.clearLoginData()
        Store.clearCredential()
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
        // 先取消所有提醒，此时 Store 中还是旧列表，能取消已删除事件的闹钟
        ReminderScheduler.cancelAll(appContext)
        Store.saveCustomEvents(list)
        _customEvents.value = list
        // 清空 reminded map + 重新调度
        Store.saveRemindedMap(emptyMap())
        ReminderScheduler.scheduleAll(appContext)
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
        // 先取消所有提醒，此时 Store 中还是旧列表，能取消所有已有事件的闹钟
        ReminderScheduler.cancelAll(appContext)
        Store.saveCustomEvents(emptyList())
        _customEvents.value = emptyList()
        Store.saveRemindedMap(emptyMap())
    }

    class Factory(private val api: ChaoxingApi, private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(api, appContext) as T
        }
    }
}
