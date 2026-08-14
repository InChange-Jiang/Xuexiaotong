package com.xuexiaotong.data

import android.content.Context
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 学习通数据抓取服务（对应 uni-app 版 chaoxing.js）
 * 4 步链路：
 *  1. POST courselistdata 获取全部课程（clazzId / courseId / cpi）
 *  2. GET stucoursemiddle → 302 课程主页，解析隐藏字段 enc / workEnc
 *  3. GET work/list 获取作业列表
 *  4. GET work/task 解析作答起止时间（截止时间）
 */
class ChaoxingApi(private val context: Context) {

    private val UA = "Mozilla/5.0 (Linux; Android 10; HD1910) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"

    private val cookieJar = PersistentCookieJar(context)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(cookieJar)
        .build()

    // ---- 会话状态 ----

    fun hasSession(): Boolean = Store.hasCookie()

    fun clearSession() {
        Store.clearCookie()
        cookieJar.clear()
    }

    /* ==================== 登录 ==================== */

    /** 账号密码登录学习通（fanyalogin） */
    suspend fun loginByPassword(phone: String, pwd: String): String = withContext(Dispatchers.IO) {
        val uname = Aes.encrypt(phone.trim())
        val password = Aes.encrypt(pwd)

        val form = FormBody.Builder()
            .add("fid", "-1")
            .add("uname", uname)
            .add("password", password)
            .add("refer", "http%3A%2F%2Fi.mooc.chaoxing.com")
            .add("t", "true")
            .add("forbidotherlogin", "0")
            .add("validate", "")
            .add("doubleFactorLogin", "0")
            .add("independentId", "0")
            .add("independentNameId", "0")
            .build()

        val req = Request.Builder()
            .url("https://passport2.chaoxing.com/fanyalogin")
            .post(form)
            .header("User-Agent", UA)
            .header("Referer", "https://passport2.chaoxing.com/login")
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .build()

        client.newCall(req).execute().use { res ->
            val body = res.body?.string() ?: throw IOException("登录接口返回空")
            val data = try { JSONObject(body) } catch (e: Exception) { throw IOException("登录接口返回异常") }
            if (!data.optBoolean("status", false)) {
                val msg = data.optString("msg2", data.optString("msg", "用户名或密码错误"))
                throw IOException(msg)
            }
            val jumpUrl = data.optString("url", "")
            // 登录后遍历全部业务域，确保各域 cookie 被捕获（CookieJar 自动保存 Set-Cookie）
            try {
                if (jumpUrl.isNotEmpty()) get(jumpUrl)
            } catch (e: Exception) { /* 忽略 */ }
            val domains = listOf(
                "https://mooc2-ans.chaoxing.com/visit/interaction",
                "https://mooc1.chaoxing.com/visit/interaction",
                "https://mobilelearn.chaoxing.com/page/active/stuActiveList?courseid=1&clazzid=1&cpi=1&ut=s&t=${System.currentTimeMillis()}&stuenc=1&fid=1",
                "https://i.mooc.chaoxing.com/space/index",
                "https://passport2-api.chaoxing.com/",
                "https://stat2-ans.chaoxing.com/"
            )
            for (d in domains) {
                try { get(d) } catch (e: Exception) { /* 忽略 */ }
            }

            val cookie = cookieJar.cookieString()
            Store.saveCookie(cookie)
            cookie
        }
    }

    /** 静默退出重登：用本地加密保存的账号密码刷新登录态（刷新按钮点击时、抓取流程前调用） */
    suspend fun silentRelogin(): Boolean = withContext(Dispatchers.IO) {
        val cred = Store.getCredential() ?: return@withContext false
        val backup = Store.getCookie()
        clearSession()
        try {
            loginByPassword(cred.first, cred.second)
            true
        } catch (e: Exception) {
            // 重登失败：回滚旧登录态，不阻塞后续抓取
            if (backup.isNotEmpty()) {
                cookieJar.restoreFromString(backup)
                Store.saveCookie(backup)
            }
            false
        }
    }

    /** 校验登录是否有效 */
    suspend fun checkLogin(): Boolean = withContext(Dispatchers.IO) {
        try {
            val html = postText(
                "https://mooc2-ans.chaoxing.com/mooc2-ans/visit/courselistdata",
                "courseType=1&courseFolderId=0&query=&superstarClass=0&courseFolderSize=0",
                referer = "https://mooc2-ans.chaoxing.com/visit/interaction"
            )
            html.contains("course-list") || html.contains("learnCourse") || html.contains("course clearfix")
        } catch (e: Exception) {
            false
        }
    }

    /* ==================== 请求封装 ==================== */

    private fun baseHeaders(referer: String?): Map<String, String> {
        val h = mutableMapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "zh-CN,zh;q=0.9",
            "User-Agent" to UA
        )
        if (referer != null) h["Referer"] = referer
        // Cookie 由 OkHttp CookieJar 按域自动附加，不手动设置（避免重复/冲突）
        return h
    }

    private suspend fun get(url: String, referer: String? = null): Response = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).get().apply {
            baseHeaders(referer).forEach { (k, v) -> header(k, v) }
        }.build()
        client.newCall(req).execute()
    }

    private suspend fun getText(url: String, referer: String? = null): String = withContext(Dispatchers.IO) {
        val res = get(url, referer)
        res.use { r ->
            if (r.code in 200..299) r.body?.string() ?: "" else throw IOException("HTTP ${r.code} $url")
        }
    }

    private suspend fun postText(url: String, body: String, referer: String? = null): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url)
                .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .apply {
                    baseHeaders(referer).forEach { (k, v) -> header(k, v) }
                }.build()
            client.newCall(req).execute().use { r ->
                if (r.code in 200..299) r.body?.string() ?: "" else throw IOException("HTTP ${r.code} $url")
            }
        }

    private suspend fun postForm(url: String, form: FormBody, referer: String? = null): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url)
                .post(form)
                .apply {
                    baseHeaders(referer).forEach { (k, v) -> header(k, v) }
                }.build()
            client.newCall(req).execute().use { r ->
                if (r.code in 200..299) r.body?.string() ?: "" else throw IOException("HTTP ${r.code} $url")
            }
        }

    /* ==================== Step 1: 课程列表 ==================== */

    suspend fun fetchCourses(): List<Course> = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("courseType", "1")
            .add("courseFolderId", "0")
            .add("query", "")
            .add("superstarClass", "0")
            .add("courseFolderSize", "0")
            .build()
        val html = postForm(
            "https://mooc2-ans.chaoxing.com/mooc2-ans/visit/courselistdata",
            form,
            "https://mooc2-ans.chaoxing.com/visit/interaction"
        )

        val courses = mutableListOf<Course>()
        val blocks = html.split("<div class=\"course clearfix")
        for (i in 1 until blocks.size) {
            val block = blocks[i]
            val info = Regex("""info="(\d+)_(\d+)"""").find(block)
                ?: continue
            val clazzId = info.groupValues[1]
            val cpi = info.groupValues[2]

            var name = Regex("""class="course-name[^"]*"[^>]*title="([^"]*)"""").find(block)
                ?.groupValues?.get(1)
                ?: Regex("""title="([^"]*)"[^>]*class="course-name""").find(block)?.groupValues?.get(1)
            if (name.isNullOrEmpty()) continue
            name = name.trim()

            val href = Regex("""href="(https?://mooc1\.chaoxing\.com/visit/stucoursemiddle[^"]*)"""").find(block)
                ?.groupValues?.get(1) ?: ""
            val courseId = Regex("""class="courseId"[^>]*value="(\d+)"""").find(block)
                ?.groupValues?.get(1) ?: ""

            courses.add(Course(courseId, clazzId, cpi, name, href))
        }

        if (courses.isNotEmpty()) Store.saveCourses(courses)
        courses
    }

    /* ==================== Step 2: 课程主页密钥 ==================== */

    suspend fun fetchCourseKeys(course: Course): CourseKeys = withContext(Dispatchers.IO) {
        val url = "https://mooc1.chaoxing.com/visit/stucoursemiddle?courseid=${course.courseId}" +
            "&clazzid=${course.clazzId}&cpi=${course.cpi}&ismooc2=1&v=2"

        // 请求课程主页，OkHttp 自动跟随 302 到最终页；enc 出现在最终跳转 URL 中
        var html: String
        var enc: String
        var workEnc: String
        try {
            val res = get(url, "https://mooc2-ans.chaoxing.com/visit/interaction")
            val finalUrl = res.request.url.toString()
            html = res.use { r ->
                if (r.code in 200..299) r.body?.string() ?: "" else throw IOException("HTTP ${r.code} $url")
            }
            // enc：优先从最终跳转 URL 参数提取（302 Location 携带）
            enc = Regex("""[?&]enc=([a-f0-9]{32})""", RegexOption.IGNORE_CASE).find(finalUrl)?.groupValues?.get(1) ?: ""
            workEnc = extractHiddenValue(html, "workEnc")
        } catch (e: Exception) {
            html = ""
            enc = ""
            workEnc = ""
        }

        // 兜底：HTML 中提取 enc（页面脚本/链接里通常含 enc 参数）
        if (enc.isEmpty() && html.isNotEmpty()) {
            enc = Regex("""enc=([a-f0-9]{32})""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1) ?: ""
        }

        // 重试一次（先访问互动页）
        if (enc.isEmpty() || workEnc.isEmpty()) {
            try {
                getText("https://mooc2-ans.chaoxing.com/visit/interaction")
                val res2 = get(url, "https://mooc2-ans.chaoxing.com/visit/interaction")
                val finalUrl2 = res2.request.url.toString()
                html = res2.use { r -> r.body?.string() ?: "" }
                enc = Regex("""[?&]enc=([a-f0-9]{32})""", RegexOption.IGNORE_CASE).find(finalUrl2)?.groupValues?.get(1) ?: ""
                if (enc.isEmpty()) {
                    enc = Regex("""enc=([a-f0-9]{32})""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1) ?: ""
                }
                workEnc = extractHiddenValue(html, "workEnc")
            } catch (e: Exception) { /* 忽略 */ }
        }

        // 兜底 2：workEnc 从作业列表页提取
        if (workEnc.isEmpty() && enc.isNotEmpty()) {
            try {
                val listUrl = "https://mooc1.chaoxing.com/mooc2/work/list?courseId=${course.courseId}" +
                    "&classId=${course.clazzId}&cpi=${course.cpi}&ut=s&t=${System.currentTimeMillis()}" +
                    "&stuenc=$enc&enc=&status=0&pageNum=1"
                val listHtml = getText(listUrl, "https://mooc2-ans.chaoxing.com/mooc2-ans/mycourse/stu")
                workEnc = Regex("""enc=([a-f0-9]{32})""", RegexOption.IGNORE_CASE).find(listHtml)?.groupValues?.get(1) ?: ""
            } catch (e: Exception) { /* 忽略 */ }
        }

        if (enc.isEmpty() || workEnc.isEmpty()) {
            throw IOException("课程 ${course.name} 密钥解析失败: enc=${enc.length} workEnc=${workEnc.length}")
        }
        CourseKeys(enc, workEnc)
    }

    private fun extractHiddenValue(html: String, id: String): String {
        val re = Regex("""<input[^>]*id=["']?$id["']?[^>]*value=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        val m = re.find(html)
        if (m != null) return m.groupValues[1]
        val re2 = Regex("""<input[^>]*value=["']([^"']*)["'][^>]*id=["']?$id["']?""", RegexOption.IGNORE_CASE)
        return re2.find(html)?.groupValues?.get(1) ?: ""
    }

    /* ==================== Step 3: 作业列表 ==================== */

    suspend fun fetchCourseWorks(course: Course, keys: CourseKeys): List<Work> =
        withContext(Dispatchers.IO) {
            val works = mutableListOf<Work>()
            val baseParams = "courseId=${course.courseId}&classId=${course.clazzId}&cpi=${course.cpi}" +
                "&ut=s&t=${System.currentTimeMillis()}&stuenc=${keys.enc}&enc=${keys.workEnc}"

            var pageNum = 1
            var totalPages = 1
            var hasMore = true

            while (hasMore && pageNum <= 50) {
                val url = "https://mooc1.chaoxing.com/mooc2/work/list?$baseParams&status=0&pageNum=$pageNum"
                val referer = "https://mooc2-ans.chaoxing.com/mooc2-ans/mycourse/stu?courseid=${course.courseId}" +
                    "&clazzid=${course.clazzId}&cpi=${course.cpi}"
                val html = getText(url, referer)

                if (pageNum == 1) {
                    val totalMatch = Regex("""共(\d+)页""", RegexOption.IGNORE_CASE).find(html)
                        ?: Regex("""totalPage['":\s]+(\d+)""", RegexOption.IGNORE_CASE).find(html)
                    if (totalMatch != null) {
                        totalPages = totalMatch.groupValues[1].toIntOrNull() ?: 1
                    } else if (!html.contains("work/task")) {
                        hasMore = false
                        break
                    }
                }

                val liRe = Regex(
                    """<li[^>]*data="(https?://mooc1\.chaoxing\.com/mooc-ans/mooc2/work/task[^"]*)"[^>]*>([\s\S]*?)</li>"""
                )
                var pageWorks = 0
                for (m in liRe.findAll(html)) {
                    var detailUrl = m.groupValues[1].replace("&amp;", "&")
                    val liContent = m.groupValues[2]

                    var title = Regex("""<p[^>]*class="[^"]*overHidden2[^"]*"[^>]*>([^<]*)</p>""").find(liContent)
                        ?.groupValues?.get(1)?.trim()
                    if (title.isNullOrEmpty()) {
                        title = Regex("""aria-label="([^"]*?)"""").find(m.value)
                            ?.groupValues?.get(1)?.split(";")?.firstOrNull()?.trim()
                    }
                    if (title.isNullOrEmpty()) title = "未命名作业"

                    val statusRaw = Regex("""<p[^>]*class="[^"]*status[^"]*"[^>]*>([\s\S]*?)</p>""").find(liContent)
                        ?.groupValues?.get(1)?.replace(Regex("""<[^>]+>"""), "")?.trim() ?: ""
                    // 调试日志：打印实际抓取到的状态文本
                    android.util.Log.d("WorkSync", "status=[$statusRaw] title=[$title]")
                    val status = statusRaw

                    val workId = Regex("""workId=(\d+)""").find(detailUrl)?.groupValues?.get(1) ?: ""
                    val answerId = Regex("""answerId=(\d+)""").find(detailUrl)?.groupValues?.get(1) ?: ""

                    val color = Store.getCourseColorCached(course.courseId)
                        ?: CourseColors.byCourseId(course.courseId)

                    works.add(
                        Work(
                            workId = workId,
                            answerId = answerId,
                            courseId = course.courseId,
                            courseName = course.name,
                            title = title,
                            status = status,
                            detailUrl = detailUrl,
                            colorBg = color.bg,
                            colorText = color.text
                        )
                    )
                    pageWorks++
                }

                if (pageWorks == 0) {
                    hasMore = false
                    break
                }

                if (pageNum >= totalPages) {
                    hasMore = false
                } else {
                    pageNum++
                    delay(800)
                }
            }
            works
        }

    /* ==================== Step 4: 作业截止时间 ==================== */

    suspend fun fetchWorkDeadline(work: Work): Pair<Long, Long>? = withContext(Dispatchers.IO) {
        val url = work.detailUrl
        val referer = "https://mooc1.chaoxing.com/mooc2/work/list?courseId=${work.courseId}"
        val html = getText(url, referer)

        val timeRe = Regex("""作答时间:\s*<em>([\d-]+\s[\d:]+)</em>\s*至\s*<em>([\d-]+\s[\d:]+)</em>""")
        val m1 = timeRe.find(html)
        if (m1 != null) {
            val start = parseMMDD(m1.groupValues[1])
            val end = parseMMDD(m1.groupValues[2])
            if (start != null && end != null) return@withContext Pair(start, end)
        }
        val plainRe = Regex("""作答时间:\s*([\d-]+\s[\d:]+)\s*至\s*([\d-]+\s[\d:]+)""")
        val m2 = plainRe.find(html)
        if (m2 != null) {
            val start = parseMMDD(m2.groupValues[1])
            val end = parseMMDD(m2.groupValues[2])
            if (start != null && end != null) return@withContext Pair(start, end)
        }
        null
    }

    /** "MM-DD HH:mm" → 时间戳（年份取当前年） */
    private fun parseMMDD(str: String): Long? {
        val m = Regex("""^(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{2})$""").find(str) ?: return null
        val month = m.groupValues[1].toInt()
        val day = m.groupValues[2].toInt()
        val hour = m.groupValues[3].toInt()
        val minute = m.groupValues[4].toInt()
        return try {
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.MONTH, month - 1)
                set(java.util.Calendar.DAY_OF_MONTH, day)
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (e: Exception) { null }
    }

    /* ==================== 全量同步 ==================== */

    interface ProgressListener {
        fun onProgress(done: Int, total: Int, message: String)
    }

    suspend fun syncAllWorks(listener: ProgressListener? = null): List<Work> =
        withContext(Dispatchers.IO) {
            listener?.onProgress(0, 0, "正在获取课程列表...")
            var courses = Store.getCourses()
            if (courses.isEmpty()) courses = fetchCourses()
            if (courses.isEmpty()) {
                throw IOException("课程列表获取为空，已保留本地数据，请稍后重试")
            }

            val existing = Store.getWorks()
            val existingMap = existing.filter { it.workId.isNotEmpty() && it.endTs != null }
                .associateBy { it.workId }

            val allWorks = mutableListOf<Work>()
            var done = 0
            val total = courses.size

            for (course in courses) {
                listener?.onProgress(done, total, "正在处理：${course.name}")
                try {
                    val keys = fetchCourseKeys(course)
                    delay(600)
                    val works = fetchCourseWorks(course, keys)
                    delay(600)

                    for (work in works) {
                        val prev = existingMap[work.workId]
                        var startTs: Long? = prev?.startTs
                        var endTs: Long? = prev?.endTs

                        if (prev == null) {
                            try {
                                val dl = fetchWorkDeadline(work)
                                if (dl != null) {
                                    startTs = dl.first
                                    endTs = dl.second
                                }
                                delay(800)
                            } catch (e: Exception) { /* 保留列表信息 */ }
                        }

                        allWorks.add(
                            work.copy(
                                startTs = startTs,
                                endTs = endTs,
                                rawStart = prev?.rawStart ?: "",
                                rawEnd = prev?.rawEnd ?: ""
                            )
                        )
                    }
                } catch (e: Exception) {
                    // 单门课程失败不中断
                }
                done++
                listener?.onProgress(done, total, "已完成 $done/$total 门课程")
            }

            if (allWorks.isEmpty() && Store.getWorks().isNotEmpty()) {
                throw IOException("同步结果为空，已保留本地作业数据，请稍后重试")
            }

            Store.saveWorks(allWorks)
            Store.saveLastSync(System.currentTimeMillis())
            listener?.onProgress(total, total, "同步完成")
            allWorks
        }

    /* ==================== 课程进度 ==================== */

    suspend fun syncCourseProgress(listener: ProgressListener? = null): List<CourseProgress> =
        withContext(Dispatchers.IO) {
            listener?.onProgress(0, 0, "正在获取课程进度...")
            var courses = Store.getCourses()
            if (courses.isEmpty()) courses = fetchCourses()
            if (courses.isEmpty()) throw IOException("课程列表获取为空，请稍后重试")

            val result = mutableListOf<CourseProgress>()
            var done = 0
            val total = courses.size

            for (course in courses) {
                listener?.onProgress(done, total, "正在处理：${course.name}")
                try {
                    val url = "https://mooc2-ans.chaoxing.com/mooc2-ans/mycourse/studentcourse" +
                        "?courseid=${course.courseId}&clazzid=${course.clazzId}&cpi=${course.cpi}&ut=s"
                    val html = getText(url, "https://mooc2-ans.chaoxing.com/visit/interaction")
                    val m = Regex("""已完成任务点[\s\S]*?<span[^>]*>(\d+)</span>\s*/\s*(\d+)""", RegexOption.IGNORE_CASE)
                        .find(html)
                    if (m != null) {
                        val finish = m.groupValues[1].toIntOrNull() ?: 0
                        val jobcount = m.groupValues[2].toIntOrNull() ?: 0
                        val percent = if (jobcount > 0) (finish * 100 / jobcount) else 0
                        result.add(
                            CourseProgress(
                                courseId = course.courseId,
                                clazzId = course.clazzId,
                                cpi = course.cpi,
                                name = course.name,
                                doneCount = finish,
                                totalCount = jobcount,
                                percent = percent,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    delay(800)
                } catch (e: Exception) { /* 忽略 */ }
                done++
                listener?.onProgress(done, total, "已完成 $done/$total 门课程")
            }

            if (result.isEmpty() && Store.getCourseProgress().isNotEmpty()) {
                throw IOException("课程进度获取为空，已保留本地数据，请稍后重试")
            }
            Store.saveCourseProgress(result)
            listener?.onProgress(total, total, "同步完成")
            result
        }
}
