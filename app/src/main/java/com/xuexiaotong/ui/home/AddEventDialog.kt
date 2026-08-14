package com.xuexiaotong.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.xuexiaotong.data.CustomEvent
import com.xuexiaotong.ui.theme.GlassDialogButton
import com.xuexiaotong.ui.theme.gaussianShadow
import com.xuexiaotong.ui.theme.glassCard
import com.xuexiaotong.ui.theme.noRippleClickable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 日程表单数据 */
data class EventFormState(
    val title: String = "",
    val startDate: String = todayStr(),
    val startTime: String = "09:00",
    val endDate: String = todayStr(),
    val endTime: String = "18:00",
    val colorBg: String = "#FFE8E0",
    val colorText: String = "#C05621"
)

private val EVENT_COLORS = listOf(
    "#FFE8E0" to "#C05621",
    "#FFF3D6" to "#B7791F",
    "#DDF5E4" to "#276749",
    "#E0F4F9" to "#1E6FA3",
    "#E9E4FD" to "#5B45A8",
    "#FDE4F0" to "#8B4B93",
    "#DFF7F2" to "#157A6E",
    "#FFE2E5" to "#B23A5E"
)

private fun todayStr(): String {
    val c = Calendar.getInstance()
    val pad = { n: Int -> n.toString().padStart(2, '0') }
    return "${c.get(Calendar.YEAR)}-${pad(c.get(Calendar.MONTH) + 1)}-${pad(c.get(Calendar.DAY_OF_MONTH))}"
}

/** 解析 YYYY-MM-DD + HH:mm 为毫秒 */
private fun parseDateMs(date: String, time: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.parse("$date $time")?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

/**
 * 新建自定义日程弹窗（液态玻璃卡片）
 * 时间选择使用安卓原生 DatePickerDialog / TimePickerDialog
 */
@Composable
fun AddEventDialog(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onSave: (CustomEvent) -> Unit
) {
    var form by remember { mutableStateOf(EventFormState()) }
    var errText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .noRippleClickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .gaussianShadow(blur = 11.dp, offsetY = 2.5.dp)
                .glassCard(
                    backdrop = backdrop,
                    radius = 20.dp,
                    blurRadius = 16.dp,
                    tintAlpha = 0.16f
                )
                .noRippleClickable(onClick = {})
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("新建日程", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 14.dp))

            // 任务名
            OutlinedTextField(
                value = form.title,
                onValueChange = { form = form.copy(title = it) },
                label = { Text("任务名") },
                placeholder = { Text("给日程起个名字") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(14.dp))

            // 开始时间（原生选择器）
            NativeDateTimeRow(
                label = "开始",
                date = form.startDate,
                time = form.startTime,
                onDatePick = { date ->
                    val parts = date.split("-")
                    val y = parts[0].toInt()
                    val m = parts[1].toInt() - 1
                    val d = parts[2].toInt()
                    DatePickerDialog(context, { _, yy, mm, dd ->
                        form = form.copy(startDate = "$yy-${(mm + 1).toString().padStart(2, '0')}-${dd.toString().padStart(2, '0')}")
                    }, y, m, d).show()
                },
                onTimePick = { time ->
                    val parts = time.split(":")
                    TimePickerDialog(context, { _, h, m ->
                        form = form.copy(startTime = "$h:${m.toString().padStart(2, '0')}")
                    }, parts[0].toInt(), parts[1].toInt(), true).show()
                }
            )

            Spacer(Modifier.height(10.dp))

            // 结束时间（原生选择器）
            NativeDateTimeRow(
                label = "结束",
                date = form.endDate,
                time = form.endTime,
                onDatePick = { date ->
                    val parts = date.split("-")
                    val y = parts[0].toInt()
                    val m = parts[1].toInt() - 1
                    val d = parts[2].toInt()
                    DatePickerDialog(context, { _, yy, mm, dd ->
                        form = form.copy(endDate = "$yy-${(mm + 1).toString().padStart(2, '0')}-${dd.toString().padStart(2, '0')}")
                    }, y, m, d).show()
                },
                onTimePick = { time ->
                    val parts = time.split(":")
                    TimePickerDialog(context, { _, h, m ->
                        form = form.copy(endTime = "$h:${m.toString().padStart(2, '0')}")
                    }, parts[0].toInt(), parts[1].toInt(), true).show()
                }
            )

            Spacer(Modifier.height(14.dp))

            // 颜色
            Text("颜色", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .horizontalScroll(rememberScrollState()),   // 颜色多时可左右滑动
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EVENT_COLORS.forEach { (bg, text) ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .noRippleClickable { form = form.copy(colorBg = bg, colorText = text) }
                            .background(Color(android.graphics.Color.parseColor(bg)), CircleShape)
                            .then(
                                if (form.colorBg == bg) Modifier.background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    CircleShape
                                ).padding(3.dp).background(Color(android.graphics.Color.parseColor(bg)), CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (form.colorBg == bg) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                }
            }

            if (errText.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(errText, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassDialogButton(backdrop, "取消", isPrimary = false) { onDismiss() }
                GlassDialogButton(backdrop, "保存", isPrimary = true) {
                    val title = form.title.trim()
                    if (title.isEmpty()) {
                        errText = "请输入任务名"
                        return@GlassDialogButton
                    }
                    val startTs = parseDateMs(form.startDate, form.startTime)
                    val endTs = parseDateMs(form.endDate, form.endTime)
                    if (endTs < startTs) {
                        errText = "结束时间需晚于开始时间"
                        return@GlassDialogButton
                    }
                    val ev = CustomEvent(
                        id = "ev_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}",
                        title = title,
                        startDate = form.startDate,
                        startTime = form.startTime,
                        endDate = form.endDate,
                        endTime = form.endTime,
                        startTs = startTs,
                        endTs = endTs,
                        done = false,
                        colorBg = form.colorBg,
                        colorText = form.colorText
                    )
                    onSave(ev)
                }
            }
        }
    }
}

/** 日期/时间字段行：点击弹安卓原生选择器 */
@Composable
private fun NativeDateTimeRow(
    label: String,
    date: String,
    time: String,
    onDatePick: (String) -> Unit,
    onTimePick: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // 日期字段
            PickerField(
                text = date,
                modifier = Modifier.weight(1f),
                onClick = { onDatePick(date) }
            )
            // 时间字段
            PickerField(
                text = time,
                modifier = Modifier.weight(1f),
                onClick = { onTimePick(time) }
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PickerField(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .noRippleClickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.width(6.dp))
            Text("▾", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
