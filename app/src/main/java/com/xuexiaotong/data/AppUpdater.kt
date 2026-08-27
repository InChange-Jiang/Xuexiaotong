package com.xuexiaotong.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.xuexiaotong.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AppUpdater(private val context: Context) {

    companion object {
        private const val GH_OWNER = "InChange-Jiang"
        private const val GH_REPO = "Xuexiaotong"
        private const val API_URL = "https://api.github.com/repos/$GH_OWNER/$GH_REPO/releases/latest"
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

        val latestVer = tagName.removePrefix("v")
        val localVer = BuildConfig.VERSION_NAME

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

    fun openInBrowser(url: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}