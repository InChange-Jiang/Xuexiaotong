package com.xuexiaotong.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json

/**
 * 本地数据持久化（对应 uni-app 版 store.js）
 * SharedPreferences + JSON 序列化
 */
object Store {
    private const val PREFS = "xxt_prefs"
    private lateinit var sp: SharedPreferences

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun init(context: Context) {
        sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun getString(key: String, def: String = ""): String =
        sp.getString(key, def) ?: def

    private fun putString(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }

    private fun getBool(key: String, def: Boolean): Boolean =
        sp.getBoolean(key, def)

    private fun putBool(key: String, value: Boolean) {
        sp.edit().putBoolean(key, value).apply()
    }

    private fun getLong(key: String, def: Long): Long =
        sp.getLong(key, def)

    private fun putLong(key: String, value: Long) {
        sp.edit().putLong(key, value).apply()
    }

    private fun remove(key: String) {
        sp.edit().remove(key).apply()
    }

    /* ---------------- Cookie ---------------- */
    fun saveCookie(cookie: String) = putString("cx_cookie", cookie)
    fun getCookie(): String = getString("cx_cookie")
    fun hasCookie(): Boolean = getCookie().length > 20
    fun clearCookie() = remove("cx_cookie")

    /* ---------------- 登录凭证（Keystore 加密，用于静默重登） ---------------- */
    fun saveCredential(phone: String, pwd: String) {
        putString("cx_cred_phone", CredentialCrypto.encrypt(phone))
        putString("cx_cred_pwd", CredentialCrypto.encrypt(pwd))
    }
    fun getCredential(): Pair<String, String>? {
        val p = getString("cx_cred_phone")
        val w = getString("cx_cred_pwd")
        if (p.isEmpty() || w.isEmpty()) return null
        val dp = CredentialCrypto.decrypt(p) ?: return null
        val dw = CredentialCrypto.decrypt(w) ?: return null
        return dp to dw
    }
    fun hasCredential(): Boolean = getString("cx_cred_phone").isNotEmpty()
    fun clearCredential() {
        remove("cx_cred_phone")
        remove("cx_cred_pwd")
    }

    /* ---------------- 保持登录 ---------------- */
    fun getKeepLogin(): Boolean = sp.getBoolean("cx_keep_login", true)
    fun saveKeepLogin(v: Boolean) = putBool("cx_keep_login", v)

    /* ---------------- 课程 ---------------- */
    fun saveCourses(list: List<Course>) {
        putString("cx_courses", json.encodeToString(list))
    }
    fun getCourses(): List<Course> {
        val raw = getString("cx_courses")
        if (raw.isEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<Course>>(raw)
        } catch (e: Exception) { emptyList() }
    }

    /* ---------------- 作业 ---------------- */
    fun saveWorks(list: List<Work>) {
        putString("cx_works", json.encodeToString(list))
    }
    fun getWorks(): List<Work> {
        val raw = getString("cx_works")
        if (raw.isEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<Work>>(raw)
        } catch (e: Exception) { emptyList() }
    }

    /* ---------------- 同步时间 ---------------- */
    fun saveLastSync(ts: Long) = putLong("cx_last_sync", ts)
    fun getLastSync(): Long = getLong("cx_last_sync", 0)

    /* ---------------- 课程颜色（缓存） ---------------- */
    fun saveCourseColor(courseId: String, bg: String, text: String) {
        putString("cx_color_$courseId", "$bg|$text")
    }
    fun getCourseColorCached(courseId: String): ColorPair? {
        val raw = getString("cx_color_$courseId")
        if (raw.isEmpty()) return null
        val parts = raw.split("|")
        return if (parts.size == 2) ColorPair(parts[0], parts[1]) else null
    }

    /* ---------------- 暗色模式 ---------------- */
    fun getDarkMode(): Boolean = sp.getBoolean("cx_dark_mode", false)
    fun saveDarkMode(dark: Boolean) = putBool("cx_dark_mode", dark)
    fun toggleDark(): Boolean {
        val next = !getDarkMode()
        saveDarkMode(next)
        return next
    }

    /* ---------------- 主题配色 ---------------- */
    fun getThemeId(): String = getString("cx_theme_color", "coral")
    fun saveThemeId(id: String) = putString("cx_theme_color", id)

    /* ---------------- 提醒设置 ---------------- */
    fun saveRemindSetting(s: RemindSetting) {
        putString("cx_remind_setting", json.encodeToString(s))
    }
    fun getRemindSetting(): RemindSetting {
        val raw = getString("cx_remind_setting")
        if (raw.isEmpty()) return RemindSetting()
        return try {
            json.decodeFromString<RemindSetting>(raw)
        } catch (e: Exception) { RemindSetting() }
    }

    /* ---------------- 已提醒 map ---------------- */
    fun saveRemindedMap(map: Map<String, Int>) {
        putString("cx_reminded", json.encodeToString(map))
    }
    fun getRemindedMap(): Map<String, Int> {
        val raw = getString("cx_reminded")
        if (raw.isEmpty()) return emptyMap()
        return try {
            json.decodeFromString<Map<String, Int>>(raw)
        } catch (e: Exception) { emptyMap() }
    }

    /* ---------------- 自定义日程 ---------------- */
    fun saveCustomEvents(list: List<CustomEvent>) {
        putString("cx_custom_events", json.encodeToString(list))
    }
    fun getCustomEvents(): List<CustomEvent> {
        val raw = getString("cx_custom_events")
        if (raw.isEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<CustomEvent>>(raw)
        } catch (e: Exception) { emptyList() }
    }

    /* ---------------- 毛玻璃（原生无意义，保留占位） ---------------- */
    fun getGlassEnabled(): Boolean = sp.getBoolean("cx_glass_enabled", false)
    fun saveGlassEnabled(v: Boolean) = putBool("cx_glass_enabled", v)

    /* ---------------- 无任务点课程开关 ---------------- */
    fun getShowEmptyCourses(): Boolean = sp.getBoolean("cx_show_empty_courses", false)
    fun saveShowEmptyCourses(v: Boolean) = putBool("cx_show_empty_courses", v)

    /* ---------------- 已完成置灰开关 ---------------- */
    fun getDoneGray(): Boolean = sp.getBoolean("cx_done_gray", true)
    fun saveDoneGray(v: Boolean) = putBool("cx_done_gray", v)

    /* ---------------- 课程进度 ---------------- */
    fun saveCourseProgress(list: List<CourseProgress>) {
        putString("cx_course_progress", json.encodeToString(list))
    }
    fun getCourseProgress(): List<CourseProgress> {
        val raw = getString("cx_course_progress")
        if (raw.isEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<CourseProgress>>(raw)
        } catch (e: Exception) { emptyList() }
    }

    /* ---------------- 退出登录（清除学习通相关数据，保留自定义日程） ---------------- */
    fun clearLoginData() {
        remove("cx_cookie")
        remove("cx_courses")
        remove("cx_works")
        remove("cx_last_sync")
        remove("cx_reminded")
    }
}
