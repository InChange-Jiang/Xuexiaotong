package com.xuexiaotong.data

import kotlinx.serialization.Serializable

/** 课程 */
@Serializable
data class Course(
    val courseId: String = "",
    val clazzId: String = "",
    val cpi: String = "",
    val name: String = "",
    val href: String = ""
)

/** 课程主页密钥 */
data class CourseKeys(
    val enc: String = "",
    val workEnc: String = ""
)

/** 作业 */
@Serializable
data class Work(
    val workId: String = "",
    val answerId: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val title: String = "",
    val status: String = "",          // 已完成 / 未完成
    val detailUrl: String = "",
    val startTs: Long? = null,
    val endTs: Long? = null,
    val rawStart: String = "",
    val rawEnd: String = "",
    val colorBg: String = "#FFE0CC",
    val colorText: String = "#B3451E"
) {
    val isDone: Boolean
        get() = when (status) {
            // 超星真实文案："待批阅"=学生已提交（未批改），视为完成
            "已完成", "待批阅", "已批改", "未批改", "待批改", "已提交" -> true
            else -> false   // "未完成" 及未知状态保守判为未完成
        }
    val remainMs: Long get() = (endTs ?: 0L) - System.currentTimeMillis()
}

/** 课程学习进度 */
@Serializable
data class CourseProgress(
    val courseId: String = "",
    val clazzId: String = "",
    val cpi: String = "",
    val name: String = "",
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val percent: Int = 0,
    val updatedAt: Long = 0L
)

/** 自定义日程 */
@Serializable
data class CustomEvent(
    val id: String = "",
    val title: String = "",
    val startDate: String = "",   // YYYY-MM-DD
    val startTime: String = "",   // HH:mm
    val endDate: String = "",
    val endTime: String = "",
    val startTs: Long = 0L,
    val endTs: Long = 0L,
    val done: Boolean = false,
    val colorBg: String = "#E8EAF6",
    val colorText: String = "#3F51B5"
)

/** 提醒设置 */
@Serializable
data class RemindSetting(
    val enabled: Boolean = false,
    val leadMinutes: Int = 60,
    val onlyTodo: Boolean = true
)

/** 糖果色主题 */
@Serializable
data class ThemeColor(
    val id: String = "coral",
    val name: String = "赫弈流明",
    val swatch: String = "#FF4245",
    val lightPrimary: String = "#FF4245",
    val lightPrimaryLight: String = "#FF6B6E",
    val lightPrimaryBg: String = "#FFF0F0",
    val darkPrimary: String = "#FF5255",
    val darkPrimaryLight: String = "#FF7A7D",
    val darkPrimaryBg: String = "#3A2122",
    val bgGradLightStart: String = "#FFF0F0",
    val bgGradLightEnd: String = "#FDE6E6",
    val bgGradDarkStart: String = "#1A1616",
    val bgGradDarkEnd: String = "#201414"
)

/** 全部 8 套糖果色主题（与 uni-app 版 THEME_COLORS 一致） */
object Themes {
    val ALL = listOf(
        ThemeColor("coral", "赫弈流明", "#FF4245", "#FF4245", "#FF6B6E", "#FFF0F0", "#FF5255", "#FF7A7D", "#3A2122", "#FFF0F0", "#FDE6E6", "#1A1616", "#201414"),
        ThemeColor("peach", "昭日译注", "#FF8C42", "#FF8C42", "#FFA96B", "#FFF4EA", "#FF9952", "#FFB27D", "#3A2A1E", "#FFF4EA", "#FDEAD9", "#1A1716", "#201A14"),
        ThemeColor("lemon", "焰光裁定", "#F5C542", "#F5C542", "#F7D76E", "#FDF8E8", "#F8CE55", "#F9DC82", "#3A331E", "#FDF8E8", "#FAF0D8", "#1A1915", "#201D14"),
        ThemeColor("matcha", "苍鳞千嶂", "#58B878", "#58B878", "#7ECF97", "#EDF9F0", "#63C983", "#88DBA2", "#1F3527", "#EDF9F0", "#E2F2E8", "#151A16", "#17211A"),
        ThemeColor("mint", "溢彩荧辉", "#2FB8A6", "#2FB8A6", "#63D2C4", "#EAF9F6", "#38C9B7", "#6DDCCE", "#1E3532", "#EAF9F6", "#DDF2EE", "#141A19", "#15211F"),
        ThemeColor("sky", "海的呢喃", "#3E9BFF", "#3E9BFF", "#6FBCFF", "#EDF5FF", "#4FA8FF", "#7FC1FF", "#1E2C3D", "#EDF5FF", "#DFEBFA", "#14161C", "#151C24"),
        ThemeColor("grape", "诸方玄枢", "#8B6CF0", "#8B6CF0", "#A78FF5", "#F3EFFE", "#9878F5", "#B39AF8", "#2A2340", "#F3EFFE", "#E7E1F8", "#171520", "#1B1930"),
        ThemeColor("sakura", "琼枝冰绡", "#F472B6", "#F472B6", "#F89ACD", "#FDF0F7", "#F780C0", "#FAA2D3", "#3A2230", "#FDF0F7", "#F8E3EE", "#1A1519", "#221820")
    )

    fun byId(id: String): ThemeColor = ALL.find { it.id == id } ?: ALL[0]
}

/** 笔记科目（独立于学习通课程，用户自定义） */
@Serializable
data class NoteSubject(
    val id: String = "",
    val name: String = "",
    val order: Int = 0,
    val createdAt: Long = 0L
)

/** 笔记照片元数据 */
@Serializable
data class NotePhoto(
    val id: String = "",
    val subjectId: String = "",          // 未分类固定为 UNCATEGORIZED_ID
    val fileName: String = "",
    val takenAt: Long = 0L,
    val sizeBytes: Long = 0L
)

/** 笔记未分类科目的固定 id */
const val UNCATEGORIZED_ID = "_uncategorized"

/** 课程颜色（马卡龙色板，14 色） */
data class ColorPair(val bg: String, val text: String)

object CourseColors {
    val PALETTE = listOf(
        ColorPair("#FFE8E0", "#C05621"),
        ColorPair("#FFF3D6", "#B7791F"),
        ColorPair("#FFF8DC", "#9C7B0A"),
        ColorPair("#DDF5E4", "#276749"),
        ColorPair("#E0F4F9", "#1E6FA3"),
        ColorPair("#E9E4FD", "#5B45A8"),
        ColorPair("#FDE4F0", "#8B4B93"),
        ColorPair("#DFF7F2", "#157A6E"),
        ColorPair("#F6EEE2", "#7A5C40"),
        ColorPair("#FFE2E5", "#B23A5E"),
        ColorPair("#E8F1F8", "#35597A"),
        ColorPair("#F2E8DD", "#8A6B4A"),
        ColorPair("#E6F4E6", "#2E6B3A"),
        ColorPair("#FDE8F2", "#A3486B")
    )

    fun byCourseId(courseId: String): ColorPair {
        val hash = courseId.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
        return PALETTE[hash % PALETTE.size]
    }
}
