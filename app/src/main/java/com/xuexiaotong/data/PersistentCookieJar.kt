package com.xuexiaotong.data

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONObject

/**
 * 持久化 CookieJar：所有域 cookie 存内存，变化时写入 SharedPreferences
 * 对应 uni-app 版手动捕获各域 cookie 的逻辑
 */
class PersistentCookieJar(private val context: Context) : CookieJar {

    private val cache = mutableMapOf<String, MutableList<Cookie>>()

    init {
        restore()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val list = cache.getOrPut(url.host) { mutableListOf() }
        cookies.forEach { c ->
            list.removeAll { it.name == c.name }
            list.add(c)
        }
        persist()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // 学习通 cookie 跨域共享（Domain=.chaoxing.com），与 uni-app 版行为一致：
        // 把全部有效 cookie 按 name 去重后发给所有请求，避免按 host 精确匹配导致跨域 cookie 丢失
        val now = System.currentTimeMillis()
        val map = linkedMapOf<String, Cookie>()
        cache.forEach { (_, list) ->
            list.filter { it.expiresAt > now }.forEach { c ->
                map[c.name] = c
            }
        }
        return map.values.toList()
    }

    /** 拼接全部有效 cookie（登录成功后保存到 Store） */
    fun cookieString(): String {
        val now = System.currentTimeMillis()
        val map = linkedMapOf<String, String>()
        cache.forEach { (_, list) ->
            list.filter { it.expiresAt > now }.forEach { c ->
                map[c.name] = "${c.name}=${c.value}"
            }
        }
        return map.values.joinToString("; ")
    }

    fun clear() {
        cache.clear()
        context.getSharedPreferences("xxt_cookies", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    /** 从 Store 的 cookie 字符串恢复（静默重登失败时回滚旧登录态） */
    fun restoreFromString(cookieStr: String) {
        if (cookieStr.isEmpty()) return
        clear()
        val list = cookieStr.split("; ").mapNotNull { kv ->
            val eq = kv.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            try {
                Cookie.Builder()
                    .domain(".chaoxing.com")
                    .name(kv.substring(0, eq))
                    .value(kv.substring(eq + 1))
                    .expiresAt(Long.MAX_VALUE)
                    .build()
            } catch (e: Exception) { null }
        }
        if (list.isNotEmpty()) {
            cache[".chaoxing.com"] = list.toMutableList()
            persist()
        }
    }

    private fun persist() {
        try {
            val all = JSONObject()
            cache.forEach { (host, list) ->
                // 保存 name|domain=value 以便恢复时保留原始域
                all.put(host, list.joinToString("; ") { "${it.name}|${it.domain}=${it.value}" })
            }
            context.getSharedPreferences("xxt_cookies", Context.MODE_PRIVATE)
                .edit().putString("cookies", all.toString()).apply()
        } catch (e: Exception) { /* 忽略 */ }
    }

    private fun restore() {
        try {
            val raw = context.getSharedPreferences("xxt_cookies", Context.MODE_PRIVATE)
                .getString("cookies", null) ?: return
            val all = JSONObject(raw)
            all.keys().forEach { host ->
                val str = all.getString(host)
                val list = str.split("; ").mapNotNull { kv ->
                    val pipeIdx = kv.indexOf("|")
                    val eqIdx = kv.indexOf("=")
                    if (pipeIdx > 0 && eqIdx > pipeIdx) {
                        val name = kv.substring(0, pipeIdx)
                        val domain = kv.substring(pipeIdx + 1, eqIdx)
                        val value = kv.substring(eqIdx + 1)
                        try {
                            Cookie.Builder()
                                .domain(domain)
                                .name(name)
                                .value(value)
                                .expiresAt(Long.MAX_VALUE)
                                .build()
                        } catch (e: Exception) { null }
                    } else null
                }
                if (list.isNotEmpty()) cache[host] = list.toMutableList()
            }
        } catch (e: Exception) { /* 忽略 */ }
    }
}
