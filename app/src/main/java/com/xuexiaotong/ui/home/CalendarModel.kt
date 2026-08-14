package com.xuexiaotong.ui.home

import com.xuexiaotong.data.CustomEvent
import com.xuexiaotong.data.Store
import com.xuexiaotong.data.Work
import java.util.Calendar

/**
 * 月历模型（对应 uni-app 版 renderCalendar + lane 分轨算法）
 * 固定 42 格（6行×7列，周日起始），作业按起止日期渲染为跨天色块，
 * 通过 lane 贪心分轨避免重叠。
 */

data class CalendarCell(
    val day: Int,
    val outside: Boolean,
    val isToday: Boolean,
    val ts: Long
)

data class CalendarBlock(
    val work: Work,
    val colStart: Int,
    val colEnd: Int,
    val continueNext: Boolean,
    val lane: Int = 0
)

data class CalendarRow(
    val cells: List<CalendarCell>,
    val blocks: List<CalendarBlock>,
    val laneCount: Int,
    val layerHeight: Int
)

data class MonthModel(
    val rows: List<CalendarRow>,
    val noWorks: Boolean
)

object CalendarModel {

    private const val DAY_MS = 86400000L
    // 色块尺寸：uni-app 版 30rpx ≈ 真机(375-411dp 宽) 15-16dp，此处直接以 dp 表达
    const val BLOCK_H = 16
    private const val BLOCK_GAP = 3
    const val BLOCK_STEP = BLOCK_H + BLOCK_GAP
    const val LAYER_PAD = 4

    /** 构建某年某月的日历模型（含作业/自定义日程色块） */
    fun buildMonth(
        year: Int,
        month: Int,
        works: List<Work>,
        customEvents: List<CustomEvent>,
        showDone: Boolean = true
    ): MonthModel {
        // 1. 构建 42 格（周日起始，前后月补齐）
        val cal = Calendar.getInstance()
        val now = Calendar.getInstance()

        cal.set(year, month - 1, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.clear(Calendar.MINUTE); cal.clear(Calendar.SECOND); cal.clear(Calendar.MILLISECOND)
        val startWeekday = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=周日
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<CalendarCell>()
        // 上月补齐
        val prevCal = Calendar.getInstance().apply {
            set(year, month - 2, 1)
            set(Calendar.HOUR_OF_DAY, 0); clear(Calendar.MINUTE); clear(Calendar.SECOND); clear(Calendar.MILLISECOND)
        }
        val prevDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in startWeekday - 1 downTo 0) {
            val d = prevDays - i
            val ts = prevCal.apply { set(Calendar.DAY_OF_MONTH, d) }.timeInMillis
            cells.add(CalendarCell(d, true, false, ts))
        }
        // 当月
        for (d in 1..daysInMonth) {
            val ts = cal.apply { set(Calendar.DAY_OF_MONTH, d) }.timeInMillis
            val isToday = now.get(Calendar.YEAR) == year && (now.get(Calendar.MONTH) + 1) == month && now.get(Calendar.DAY_OF_MONTH) == d
            cells.add(CalendarCell(d, false, isToday, ts))
        }
        // 下月补齐到 42
        var fill = 42 - cells.size
        val nextCal = Calendar.getInstance().apply {
            set(year, month, 1)
            set(Calendar.HOUR_OF_DAY, 0); clear(Calendar.MINUTE); clear(Calendar.SECOND); clear(Calendar.MILLISECOND)
        }
        var d = 1
        while (fill > 0) {
            val ts = nextCal.apply { set(Calendar.DAY_OF_MONTH, d) }.timeInMillis
            cells.add(CalendarCell(d, true, false, ts))
            d++; fill--
        }

        val gridStart = cells.first().ts
        val gridEnd = cells.last().ts

        // 2. 过滤作业（默认只显示未完成，除非 showDone）
        val filteredWorks = if (showDone) works else works.filter { !it.isDone }

        // 3. 每行色块分段（跨行拆分）
        val rowBlocks = Array(6) { mutableListOf<CalendarBlock>() }

        filteredWorks.forEach { w ->
            val ws = w.startTs ?: return@forEach
            val we = w.endTs ?: return@forEach
            val s = maxOf(startOfDay(ws), gridStart)
            val e = minOf(startOfDay(we), gridEnd)
            if (s > e) return@forEach

            val sIdx = ((s - gridStart) / DAY_MS).toInt()
            val eIdx = ((e - gridStart) / DAY_MS).toInt()

            var idx = sIdx
            while (idx <= eIdx) {
                val row = idx / 7
                val rowStart = row * 7
                val segEnd = minOf(eIdx, rowStart + 6)
                rowBlocks[row].add(
                    CalendarBlock(
                        work = w,
                        colStart = idx - rowStart,
                        colEnd = segEnd - rowStart,
                        continueNext = segEnd < eIdx
                    )
                )
                idx = segEnd + 1
            }
        }

        // 4. 合并自定义日程（同样渲染为色块）
        customEvents.forEach { ev ->
            // 色块按天归位（startOfDay），但 Work 保留真实时间戳（详情页/通知显示真实时间）；
            // 兼容旧数据（startTs=0）：回退按日期字符串解析为当天起点
            val st = if (ev.startTs > 0) ev.startTs else (parseDateTs(ev.startDate) ?: return@forEach)
            val et = if (ev.endTs > 0) ev.endTs else (parseDateTs(ev.endDate) ?: st)
            val es = startOfDay(st).coerceAtLeast(gridStart)
            val ee = startOfDay(et).coerceAtMost(gridEnd)
            if (es > ee) return@forEach

            val item = Work(
                workId = "event_${ev.id}",
                courseId = "",
                courseName = "自定义日程",
                title = ev.title,
                status = if (ev.done) "已完成" else "未完成",
                startTs = st,
                endTs = et,
                colorBg = ev.colorBg,
                colorText = ev.colorText
            )

            val sIdx = ((es - gridStart) / DAY_MS).toInt()
            val eIdx = ((ee - gridStart) / DAY_MS).toInt()
            var idx = sIdx
            while (idx <= eIdx) {
                val row = idx / 7
                val rowStart = row * 7
                val segEnd = minOf(eIdx, rowStart + 6)
                rowBlocks[row].add(
                    CalendarBlock(
                        work = item,
                        colStart = idx - rowStart,
                        colEnd = segEnd - rowStart,
                        continueNext = segEnd < eIdx
                    )
                )
                idx = segEnd + 1
            }
        }

        // 5. lane 分轨（每行独立贪心）
        val rows = mutableListOf<CalendarRow>()
        var hasAnyBlock = false
        for (r in 0 until 6) {
            val blocks = rowBlocks[r].sortedWith(
                compareBy<CalendarBlock> { it.colStart }.thenByDescending { it.colEnd }
            )
            val laneEnds = mutableListOf<Int>()
            val placed = blocks.map { b ->
                var lane = 0
                var found = false
                for (li in laneEnds.indices) {
                    if (b.colStart > laneEnds[li]) {
                        laneEnds[li] = b.colEnd
                        lane = li
                        found = true
                        break
                    }
                }
                if (!found) {
                    laneEnds.add(b.colEnd)
                    lane = laneEnds.size - 1
                }
                b.copy(lane = lane)
            }
            if (placed.isNotEmpty()) hasAnyBlock = true
            val laneCount = maxOf(1, laneEnds.size)
            rows.add(
                CalendarRow(
                    cells = cells.slice(r * 7 until (r + 1) * 7),
                    blocks = placed,
                    laneCount = laneCount,
                    layerHeight = if (placed.isNotEmpty()) laneCount * BLOCK_STEP + LAYER_PAD else LAYER_PAD
                )
            )
        }

        return MonthModel(rows, !hasAnyBlock)
    }

    private fun startOfDay(ts: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        c.set(Calendar.HOUR_OF_DAY, 0); c.clear(Calendar.MINUTE); c.clear(Calendar.SECOND); c.clear(Calendar.MILLISECOND)
        return c.timeInMillis
    }

    private fun parseDateTs(date: String): Long? {
        val parts = date.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return null
        return try {
            val c = Calendar.getInstance()
            c.clear()
            c.set(parts[0], parts[1] - 1, parts[2])
            c.timeInMillis
        } catch (e: Exception) { null }
    }
}
