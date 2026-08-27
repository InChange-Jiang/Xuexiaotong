package com.xuexiaotong.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.xuexiaotong.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 自动更新：比对 GitHub Releases 版本号、下载 APK、引导安装。
 *
 * 流程：
 * 1. checkVersion() 通过 GitHub API 获取最新 release tag_name，与本地 BuildConfig 比较
 * 2. 有新版本时 startDownload() 通过 OkHttp 下载 APK 到缓存目录
 * 3. 下载完成后 installApk() 通过 FileProvider 拉起安装器
 */
class AppUpdater(private val context: Context) {

    companion object {
        private const val GH_OWNER = "InChange-Jiang"
        private const val GH_REPO = "Xuexiaotong"
        private const val API_URL = "https://api.github.com/repos/$GH_OWNER/$GH_REPO/releases/latest"
        // 缓存目录下的 APK 命名
        private const val APK_FILENAME = "update.apk"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    /**
     * 检查 GitHub 最新 release 版本号是否高于本地版本。
     * 成功返回 UpdateInfo，网络/解析失败抛出异常。
     */
    suspend fun checkVersion(): UpdateInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(API_URL).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("网络异常 (${response.code})，请稍后重试")
        }
        val body = response.body?.string() ?: throw Exception("响应为空")
        val json = JSONObject(body)
        val tagName = json.optString("tag_name", "")
        val releaseNotes = json.optString("body", "")

        if (tagName.isBlank()) throw Exception("未获取到版本信息")

        // 解析 tag_name 如 "v4.1.1" → "4.1.1"
        val latestVer = tagName.removePrefix("v")
        val localVer = BuildConfig.VERSION_NAME

        // 从 release assets 中找到第一个 .apk 文件
        val assets = json.optJSONArray("assets")
        val assetUrl = if (assets != null && assets.length() > 0) {
            var url = ""
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i)
                val name = a?.optString("name", "") ?: ""
                if (name.endsWith(".apk")) {
                    url = a.optString("browser_download_url", "")
                    break
                }
            }
            url
        } else ""

        UpdateInfo(
            hasUpdate = compareVersions(latestVer, localVer) > 0,
            latestVersion = latestVer,
            downloadUrl = assetUrl,
            releaseNotes = releaseNotes
        )
    }

    /** 对比版本号：返回正数表示 v1 > v2，负数表示 v1 < v2，0 表示相等 */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".")
        val parts2 = v2.split(".")
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val n1 = parts1.getOrElse(i) { "0" }.toIntOrNull() ?: 0
            val n2 = parts2.getOrElse(i) { "0" }.toIntOrNull() ?: 0
            if (n1 != n2) return n1 - n2
        }
        return 0
    }

    /**
     * 下载 APK 到缓存目录，完成后返回本地文件路径。
     */
    suspend fun downloadApk(url: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates")
        dir.mkdirs()
        // 清理旧文件防止磁盘堆积
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, APK_FILENAME)

        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("下载失败 (${response.code})")
        }
        val body = response.body ?: throw Exception("下载内容为空")
        // 流式写入文件
        body.byteStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file
    }

    /**
     * 通过 FileProvider 拉起系统安装器安装 APK。
     */
    fun installApk(file: File): Boolean {
        if (!file.exists()) return false
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}