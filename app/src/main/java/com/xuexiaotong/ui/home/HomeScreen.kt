package com.xuexiaotong.ui.home

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlin.math.roundToInt
import com.xuexiaotong.BuildConfig
import com.xuexiaotong.data.NoteStore
import com.xuexiaotong.data.UNCATEGORIZED_ID
import com.xuexiaotong.data.CourseProgress
import com.xuexiaotong.data.RemindSetting
import com.xuexiaotong.data.Store
import com.xuexiaotong.R
import com.xuexiaotong.data.ThemeColor
import com.xuexiaotong.data.Themes
import com.xuexiaotong.data.Work
import com.xuexiaotong.ui.AppViewModel
import com.xuexiaotong.ui.SyncProgress
import com.xuexiaotong.ui.home.CalendarModel.buildMonth
import com.xuexiaotong.ui.notes.NotesCameraScreen
import com.xuexiaotong.ui.notes.NotesGalleryScreen
import com.xuexiaotong.ui.notes.NoteSubjectManageDialog
import com.xuexiaotong.ui.theme.GlassBackdropBox
import com.xuexiaotong.ui.theme.GlassConfirmDialog
import com.xuexiaotong.ui.theme.GlassDialog
import com.xuexiaotong.ui.theme.GlassDialogButton
import com.xuexiaotong.ui.theme.GlassPopup
import com.xuexiaotong.ui.theme.GlassToast
import com.xuexiaotong.ui.theme.glassCard
import com.xuexiaotong.ui.theme.glassPill
import com.xuexiaotong.ui.theme.glassSheet
import com.xuexiaotong.ui.theme.gaussianShadow
import com.xuexiaotong.ui.theme.glassSurface
import com.xuexiaotong.ui.theme.noRippleClickable
import com.xuexiaotong.ui.theme.parseHex
import com.xuexiaotong.ui.theme.rememberGlassBackdrop
import com.xuexiaotong.ui.theme.SoftwarePrivacyContent
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

private enum class Tab { SCHEDULE, COURSE }

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    theme: ThemeColor,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val works by viewModel.works.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val syncMsg by viewModel.syncProgress.collectAsState()
    val courseSyncing by viewModel.courseSyncing.collectAsState()
    val courseSyncMsg by viewModel.courseSyncProgress.collectAsState()
    val lastSync by viewModel.lastSync.collectAsState()
    val remind by viewModel.remindSetting.collectAsState()
    val customEvents by viewModel.customEvents.collectAsState()
    val showCompleted by viewModel.showCompleted.collectAsState()
    val doneGray by viewModel.doneGray.collectAsState()
    val showEmptyCourses by viewModel.showEmptyCourses.collectAsState()
    val glassEnabled by viewModel.glassEnabled.collectAsState()
    val darkState by viewModel.dark.collectAsState()
    val themeId by viewModel.themeId.collectAsState()

    var tab by remember { mutableStateOf(Tab.SCHEDULE) }
    var showMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showRemindDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showOpenSourceDialog by remember { mutableStateOf(false) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var galleryInitSubject by remember { mutableStateOf<String?>(null) }
    var showSubjectManage by remember { mutableStateOf(false) }
    var showCameraCapture by remember { mutableStateOf(false) }
    var cameraSubjectId by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showClearEventsConfirm by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showDeleteEventConfirm by remember { mutableStateOf(false) }
    var pendingDeleteEvent by remember { mutableStateOf<Work?>(null) }
    var selectedWork by remember { mutableStateOf<Work?>(null) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    fun toast(msg: String) {
        toastMsg = msg
        scope.launch {
            kotlinx.coroutines.delay(1800)
            toastMsg = null
        }
    }

    // 日历状态
    val today = Calendar.getInstance()
    var year by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(today.get(Calendar.MONTH) + 1) }

    // 最底层：白/黑基础色 + 半透明主题渐变（柔和且保留主题感，不单调）
    val gradTop = parseHex(if (dark) theme.bgGradDarkStart else theme.bgGradLightStart).copy(alpha = 0.42f)
    val gradBottom = parseHex(if (dark) theme.bgGradDarkEnd else theme.bgGradLightEnd).copy(alpha = 0.42f)

    // 液态玻璃 backdrop：页面内外共享，弹窗同样折射它（与登录卡片同款逻辑）
    val glassBackdrop = rememberGlassBackdrop()

    // 液态玻璃背景盒：backdrop 层录制「渐变 + 页面主体」，玻璃组件悬浮其上折射真实内容（毛玻璃）
    Box(Modifier.fillMaxSize()) {
        GlassBackdropBox(
            gradientStart = gradTop,
            gradientEnd = gradBottom,
            baseColor = if (dark) Color(0xFF17171A) else Color(0xFFFDFDFE),
            backdrop = glassBackdrop
        ) { backdrop ->
            // ===== backdrop 层内：页面主体内容（日历/课程），作为玻璃的折射源 =====
            // 顶部给悬浮 TopBar 让位（72dp）；底部不收缩，内容延伸到 Dock 下方
            Box(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 72.dp)
            ) {
                // 日程 / 课程页切换：滑动 + 淡入（与 Dock 滑动方向一致）
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { it } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally { -it / 2 } + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally { -it } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally { it / 2 } + fadeOut(tween(180)))
                        }
                    }
                ) { t ->
                    when (t) {
                        Tab.SCHEDULE -> {
                            MonthCalendarView(
                                backdrop = backdrop,
                                works = works,
                                showCompleted = showCompleted,
                                doneGray = doneGray,
                                customEvents = customEvents,
                                theme = theme,
                                dark = dark,
                                year = year,
                                month = month,
                                onChangeMonth = { delta ->
                                    var m = month + delta
                                    var y = year
                                    if (m < 1) { m = 12; y-- }
                                    if (m > 12) { m = 1; y++ }
                                    year = y; month = m
                                },
                                onWorkClick = { selectedWork = it }
                            )
                        }
                        Tab.COURSE -> {
                            CourseProgressList(
                                backdrop = backdrop,
                                progress = progress,
                                theme = theme,
                                dark = dark,
                                showEmpty = showEmptyCourses
                            )
                        }
                    }
                }
            }
            // FAB 位于 backdrop 层内但独立于内容区（全屏无 padding 的坐标基准，恢复碰撞弹出逻辑）；
            // 纯色不折射 backdrop（防自折射崩溃），悬浮层 Dock 折射本层时可透出拖入 Dock 区域的 FAB（透光感）
            Box(Modifier.fillMaxSize()) {
                if (tab == Tab.SCHEDULE) {
                    FloatingAddButton(
                        backdrop = null,
                        onTap = { showAddEventDialog = true },
                        onToast = ::toast
                    )
                }
            }
        }

        // ===== backdrop 层外：玻璃组件悬浮层（折射层内真实内容） =====

        // TopBar：悬浮顶部（液态玻璃胶囊）
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            TopBar(
                backdrop = glassBackdrop,
                tab = tab,
                monthTitle = "${year}年${month}月",
                scheduleSyncing = syncing,
                scheduleProgress = syncMsg,
                courseSyncing = courseSyncing,
                courseProgress = courseSyncMsg,
                lastSync = lastSync,
                onSync = {
                    if (tab == Tab.COURSE) viewModel.syncCourseProgress() else viewModel.syncWorks()
                },
                onMenu = { showMenu = true }
            )
        }

        // 悬浮新增日程按钮已移至 backdrop 层内（便于 Dock 折射透光）

        // 液态玻璃 ProDock 导航（Dock + 相机按钮整体居中；点击 + 滑动切换）
        LiquidDock(
            backdrop = glassBackdrop,
            current = tab,
            dark = dark,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            onSelect = { tab = it },
            onCameraClick = { showCameraDialog = true }
        )

        // 侧边菜单：固定全屏遮罩（不随抽屉滑动，动画变暗）+ 抽屉滑动层
        val menuMaskAlpha by animateFloatAsState(
            targetValue = if (showMenu) 0.30f else 0f,
            animationSpec = tween(240),
            label = "menuMask"
        )
        // 固定遮罩层：仅绘制，无任何点击处理，不拦截主页面事件
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = menuMaskAlpha))
        )
        AnimatedVisibility(
            visible = showMenu,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(220)),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(220))
        ) {
            // 点击关闭层：仅在菜单显示/动画期间存在，关闭后不拦截
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .noRippleClickable { showMenu = false }
            ) {
                SideMenu(
                    backdrop = glassBackdrop,
                    dark = darkState,
                    showCompleted = showCompleted,
                    doneGray = doneGray,
                    showEmptyCourses = showEmptyCourses,
                    themeName = Themes.byId(themeId).name,
                    remindText = if (remind.enabled) "已开启" else "未开启",
                    onToggleDark = { viewModel.toggleDark() },
                    onToggleCompleted = { viewModel.toggleShowCompleted() },
                    onToggleDoneGray = { viewModel.toggleDoneGray() },
                    onToggleEmptyCourses = { viewModel.toggleShowEmptyCourses() },
                    onOpenTheme = { showMenu = false; showThemeDialog = true },
                    onOpenRemind = { showMenu = false; showRemindDialog = true },
                    onDonate = { showMenu = false; showDonateDialog = true },
                    onFeedback = { showMenu = false; showFeedbackDialog = true },
                    onAbout = { showMenu = false; showAboutDialog = true },
                    onClearEvents = { showMenu = false; showClearEventsConfirm = true },
                    onLogout = { showMenu = false; showLogoutConfirm = true },
                    onDismiss = { showMenu = false },
                    onToast = ::toast
                )
            }
        }
    }

    // 玻璃弹窗：主题 / 提醒 / 捐赠 / 反馈 / 详情 / 隐私（与登录卡片同款液态玻璃，进出带动画）
    GlassPopup(showThemeDialog) {
        GlassThemeDialog(
            backdrop = glassBackdrop,
            current = themeId,
            onSelect = {
                viewModel.selectTheme(it)
                toast("已切换主题")
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    GlassPopup(showRemindDialog) {
        GlassRemindDialog(
            backdrop = glassBackdrop,
            setting = remind,
            onSave = { viewModel.saveRemind(it); toast("已保存") },
            onDismiss = { showRemindDialog = false },
            onTest = { viewModel.sendTestNotification() }
        )
    }

    GlassPopup(showDonateDialog) {
        GlassDialog(
            backdrop = glassBackdrop,
            onDismiss = { showDonateDialog = false },
            title = "支持捐赠",
            confirmText = "知道了",
            onConfirm = { showDonateDialog = false }
        ) {
            Column(Modifier.fillMaxWidth()) {
                // 主标题下方：捐赠二维码（圆角）
                Image(
                    painter = painterResource(R.drawable.donate_qr),
                    contentDescription = "捐赠二维码",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(14.dp))
                Text("欢迎免费使用喵", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("本项目完全开源喵", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("但听说截图打赏作者有可能变猫娘喵 (˘ω˘)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    GlassPopup(showFeedbackDialog) {
        GlassDialog(
            backdrop = glassBackdrop,
            onDismiss = { showFeedbackDialog = false },
            title = "问题反馈",
            confirmText = "知道了",
            onConfirm = { showFeedbackDialog = false }
        ) {
            Column {
                Text("感谢你使用学小通。如果遇到问题，请通过以下方式联系开发者：", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    "反馈邮箱：inchangefeedback@126.com",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleClickable {
                            toast("邮箱已复制")
                        }
                )
                Spacer(Modifier.height(8.dp))
                Text("1. 请描述遇到的问题现象，以及操作步骤", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("2. 附上出现问题的截图或录屏", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("3. 说明你的手机型号与系统版本", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    GlassPopup(showAboutDialog) {
        GlassDialog(
            backdrop = glassBackdrop,
            onDismiss = { showAboutDialog = false },
            title = "软件详情"
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("学小通", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("版本 ${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("本软件由 InChange@AHUniversity 开发", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("TRAE Work @ DeepSeek-V4-Flash 辅助开发", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text(
                    "技术路线：Kotlin + Jetpack Compose 单 Activity 架构；液态玻璃 UI 基于 Backdrop 实时模糊/折射组件二次开发，Dock 滑块液态交互（拖动跟手、透镜折射、速度形变）移植自 AndroidLiquidGlass；相机基于 CameraX，支持多后置镜头等效焦距分类轮换（UW/WIDE/TELE）、点击对焦测光、捏合与拉杆变焦，方案参考 CamerAwesome 与 OpenCamera。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "数据链路：模拟学习通 Web 登录获取会话凭证，调用官方接口抓取课程、作业与任务点进度，数据仅本地解析存储、不经任何第三方服务器；提醒采用原生 AlarmManager 精确闹钟，应用被清理后台后仍准时通知。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "查看开源致谢 ›",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .noRippleClickable {
                            showAboutDialog = false
                            showOpenSourceDialog = true
                        }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "查看本软件隐私声明 ›",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .noRippleClickable {
                            showAboutDialog = false
                            showPrivacyDialog = true
                        }
                )
            }
        }
    }

    GlassPopup(showPrivacyDialog) {
        GlassPrivacyDialog(backdrop = glassBackdrop, onDismiss = { showPrivacyDialog = false })
    }

    GlassPopup(showOpenSourceDialog) {
        GlassOpenSourceDialog(backdrop = glassBackdrop, onDismiss = { showOpenSourceDialog = false })
    }

    GlassPopup(showCameraDialog) {
        GlassCameraGuideDialog(
            backdrop = glassBackdrop,
            onDismiss = { showCameraDialog = false },
            onOpenCamera = { subjectId ->
                showCameraDialog = false
                cameraSubjectId = subjectId
                showCameraCapture = true
            },
            onOpenGallery = {
                showCameraDialog = false
                galleryInitSubject = null
                showGallery = true
            },
            onNewSubject = {
                showCameraDialog = false
                showSubjectManage = true
            }
        )
    }

    // 拍照页全屏层（黑色沉浸；右侧滑入进入，体现更深功能层级；退出即销毁并释放相机）
    AnimatedVisibility(
        visible = showCameraCapture,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(220)),
        exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut(tween(180))
    ) {
        Box(Modifier.fillMaxSize()) {
            NotesCameraScreen(
                dark = dark,
                subjectId = cameraSubjectId ?: UNCATEGORIZED_ID,
                onDismiss = { showCameraCapture = false },
                onNewSubject = { showSubjectManage = true }
            )
        }
    }

    // 笔记相册全屏层（右侧滑入进入，体现不同功能层级；系统返回键关闭）
    AnimatedVisibility(
        visible = showGallery,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(220)),
        exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut(tween(180))
    ) {
        Box(Modifier.fillMaxSize()) {
            NotesGalleryScreen(
                theme = theme,
                dark = dark,
                initialSubjectId = galleryInitSubject,
                onDismiss = { showGallery = false },
                onManageSubjects = { showSubjectManage = true }
            )
        }
    }

    GlassPopup(showSubjectManage) {
        NoteSubjectManageDialog(
            backdrop = glassBackdrop,
            onDismiss = { showSubjectManage = false }
        )
    }

    GlassPopup(showAddEventDialog) {
        AddEventDialog(
            backdrop = glassBackdrop,
            onDismiss = { showAddEventDialog = false },
            onSave = {
                viewModel.addCustomEvent(it)
                showAddEventDialog = false
                toast("已添加日程")
            }
        )
    }

    GlassPopup(showDeleteEventConfirm) {
        // 待删除事件独立保存：先关闭详情弹窗再显示确认弹窗，避免二级弹窗被详情遮挡/死循环
        val w = pendingDeleteEvent
        if (w != null) {
            GlassConfirmDialog(
                backdrop = glassBackdrop,
                onDismiss = { showDeleteEventConfirm = false },
                title = "删除日程",
                message = "确定删除「${w.title}」吗？",
                confirmText = "删除",
                onConfirm = {
                    val evId = w.workId.removePrefix("event_")
                    viewModel.deleteCustomEvent(evId)
                    showDeleteEventConfirm = false
                    pendingDeleteEvent = null
                    toast("已删除")
                }
            )
        }
    }

    GlassPopup(showLogoutConfirm) {
        GlassConfirmDialog(
            backdrop = glassBackdrop,
            onDismiss = { showLogoutConfirm = false },
            title = "退出登录",
            message = "确定要退出登录吗？本地作业数据将一并清除",
            confirmText = "退出",
            onConfirm = {
                showLogoutConfirm = false
                viewModel.logout()
            }
        )
    }

    GlassPopup(showClearEventsConfirm) {
        GlassConfirmDialog(
            backdrop = glassBackdrop,
            onDismiss = { showClearEventsConfirm = false },
            title = "清空自建日程",
            message = "确定删除全部 ${customEvents.size} 条自建日程吗？此操作不可恢复",
            confirmText = "全部删除",
            onConfirm = {
                showClearEventsConfirm = false
                viewModel.clearCustomEvents()
                toast("已清空")
            }
        )
    }

    GlassPopup(selectedWork != null) {
        selectedWork?.let { w ->
            val isCustom = w.workId.startsWith("event_")
            val primary = MaterialTheme.colorScheme.primary
            GlassDialog(
                backdrop = glassBackdrop,
                onDismiss = { selectedWork = null },
                title = w.title
            ) {
                Column(Modifier.fillMaxWidth()) {
                    // 副标题：课程名（自定义日程则显示类型），居左省略
                    Text(
                        if (isCustom) "自定义日程" else w.courseName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    // 状态
                    Text(
                        if (w.isDone) "已完成" else "未完成",
                        fontSize = 12.sp,
                        color = if (w.isDone) MaterialTheme.colorScheme.onSurfaceVariant else primary
                    )
                    Spacer(Modifier.height(12.dp))
                    // 开始时间：标签居左、具体时间居右
                    w.startTs?.let {
                        Row(Modifier.fillMaxWidth()) {
                            Text("开始时间", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Text(formatFullTime(it), fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(7.dp))
                    }
                    // 截止时间：标签居左、具体时间居右
                    w.endTs?.let {
                        Row(Modifier.fillMaxWidth()) {
                            Text("截止时间", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Text(formatFullTime(it), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (isCustom) {
                        Spacer(Modifier.height(16.dp))
                        // 操作按钮：液态玻璃胶囊、低厚度、等分铺满整行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .glassPill(glassBackdrop, blurRadius = 8.dp, tintAlpha = 0.07f)
                                    .then(
                                        if (w.isDone) Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                        else Modifier.background(primary.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                                    )
                                    .noRippleClickable {
                                        viewModel.toggleCustomEventDone(w.workId.removePrefix("event_"))
                                        // 标记完成 / 恢复未完成：均自动退出详情弹窗
                                        toast(if (w.isDone) "已恢复未完成" else "已标记完成")
                                        selectedWork = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (w.isDone) "恢复未完成" else "标记完成",
                                    fontSize = 13.sp,
                                    color = if (w.isDone) MaterialTheme.colorScheme.onSurface else primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .glassPill(glassBackdrop, blurRadius = 8.dp, tintAlpha = 0.07f)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                                    .noRippleClickable {
                                        // 先记录待删事件并关闭详情，再打开确认弹窗（避免弹窗遮挡死循环）
                                        pendingDeleteEvent = w
                                        selectedWork = null
                                        showDeleteEventConfirm = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "删除该日程",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Toast 置于所有弹窗之后：始终最顶层，淡入淡出
    var lastToastMsg by remember { mutableStateOf<String?>(null) }
    toastMsg?.let { lastToastMsg = it }
    AnimatedVisibility(
        visible = toastMsg != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(240))
    ) {
        lastToastMsg?.let { GlassToast(it, glassBackdrop) }
    }
}

private fun formatFullTime(ts: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    val hh = c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val mm = c.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日 $hh:$mm"
}

/* ==================== 顶部栏 ==================== */

@Composable
private fun TopBar(
    backdrop: Backdrop,
    tab: Tab,
    monthTitle: String,
    scheduleSyncing: Boolean,
    scheduleProgress: SyncProgress,
    courseSyncing: Boolean,
    courseProgress: SyncProgress,
    lastSync: Long,
    onSync: () -> Unit,
    onMenu: () -> Unit
) {
    val isCourse = tab == Tab.COURSE
    val spinning = if (isCourse) courseSyncing else scheduleSyncing
    val prog = if (isCourse) courseProgress else scheduleProgress

    // 副标题三态：同步中显示进度 / 已同步显示时间 / 未同步提示
    val subtitle = when {
        spinning -> {
            val base = prog.message.ifEmpty { "正在同步..." }
            if (prog.total > 0) "$base（${prog.done}/${prog.total}）" else base
        }
        lastSync > 0 -> "上次同步 ${formatSyncTime(lastSync)}"
        else -> "请按右上角按钮开始同步"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .height(60.dp)
            .glassPill(backdrop, blurRadius = 8.dp, tintAlpha = 0.05f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左右按钮内移 6dp：让按钮中心贴近胶囊圆角圆心
        Box(
            modifier = Modifier
                .size(44.dp)
                .offset(x = 6.dp)
                .noRippleClickable(onClick = onMenu),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Menu, contentDescription = "菜单", tint = MaterialTheme.colorScheme.primary)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isCourse) "课程任务" else monthTitle,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .offset(x = -6.dp)
                .noRippleClickable(onClick = onSync),
            contentAlignment = Alignment.Center
        ) {
            if (spinning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = "同步", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatSyncTime(ts: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    val mm = (c.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val dd = c.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    val hh = c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val mi = c.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$mm-$dd $hh:$mi"
}

/* ==================== 液态玻璃 Dock ==================== */

@Composable
private fun LiquidDock(
    backdrop: Backdrop,
    current: Tab,
    dark: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (Tab) -> Unit,
    onCameraClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    // 滑块静止罩面色：浅色主题白色 / 暗色主题深灰（与主题适配）
    val sliderSurface = if (dark) Color(0xFF2C2C2E) else Color.White

    // Dock 胶囊固定宽度（两个 tab 各 84dp），左侧留空由整体居中补偿，右侧挂相机按钮
    val dockWidthDp = 168.dp
    var dockWidth by remember { mutableStateOf(0) }
    val animationScope = rememberCoroutineScope()
    val density = LocalDensity.current
    // 手势状态：区分 tap（无位移，按按下位置切换）与 drag（跟手 + 松手吸附切换）
    var didDrag by remember { mutableStateOf(false) }
    var startX by remember { mutableStateOf(0f) }

    // 选中滑块拖拽阻尼动画（移植自 AndroidLiquidGlass-kmp LiquidBottomTabs）：
    // 拖动跟手 + 按压渐进 + 速度形变 + 松手弹簧回弹吸附；点击切换与拖动并存
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = if (current == Tab.COURSE) 1f else 0f,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.25f,
            onDragStarted = { position ->
                didDrag = false
                startX = position.x
            },
            onDragStopped = {
                val half = if (dockWidth > 0) dockWidth / 2f else with(density) { 84f.dp.toPx() }
                val target = if (didDrag) {
                    if (targetValue >= 0.5f) 1f else 0f
                } else {
                    // 点击切换：按按下位置左右半区选择
                    if (startX >= half) 1f else 0f
                }
                animateToValue(target)
                onSelect(if (target >= 0.5f) Tab.COURSE else Tab.SCHEDULE)
            },
            onDrag = { _, dragAmount ->
                if (dragAmount.x != 0f) didDrag = true
                val tabWidth = if (dockWidth > 0) dockWidth / 2f else with(density) { 84f.dp.toPx() }
                updateValue(targetValue + dragAmount.x / tabWidth)
            }
        )
    }
    // 外部选中变化 → 滑块动画跟随
    LaunchedEffect(current) {
        dampedDragAnimation.animateToValue(if (current == Tab.COURSE) 1f else 0f)
    }

    Row(
        modifier = modifier
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ===== Dock 胶囊（缩短，固定宽度） =====
        Box(
            modifier = Modifier
                .width(dockWidthDp)
                .height(56.dp)
                .onSizeChanged { dockWidth = it.width }
        ) {
            // 第 1 层（最底）：玻璃底座背景图形（液态玻璃胶囊，无透镜更清透）——保持不动
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .glassPill(backdrop, blurRadius = 6.dp, withLens = false, tintAlpha = 0.05f)
            )

            // 第 2 层（中间）：液态玻璃选中滑块——液化改造：
                    // 边缘透镜折射、按压/速度形变、高光内外阴影（手势在最顶手势层）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(56.dp)
                            .offset {
                                IntOffset(
                                    x = (dampedDragAnimation.progress * dockWidth / 2f).roundToInt(),
                                    y = 0
                                )
                            }
                            .padding(4.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { Capsule() },
                                effects = {
                                    val p = dampedDragAnimation.pressProgress
                                    // 静止：磨砂模糊；按压：液态透镜折射（含色散）
                                    blur(6f.dp.toPx() * (1f - p))
                                    lens(
                                        10f.dp.toPx() * p,
                                        14f.dp.toPx() * p,
                                        chromaticAberration = true
                                    )
                                },
                                highlight = {
                                    Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress)
                                },
                                shadow = {
                                    Shadow(
                                        radius = 4f.dp,
                                        color = Color.Black.copy(alpha = 0.05f)
                                    )
                                },
                                innerShadow = {
                                    val p = dampedDragAnimation.pressProgress
                                    InnerShadow(radius = 4f.dp * p, alpha = p)
                                },
                                layerBlock = {
                                    scaleX = dampedDragAnimation.scaleX
                                    scaleY = dampedDragAnimation.scaleY
                                    // 拖动速度驱动的挤压形变（快速拖动时滑块拉伸）
                                    val velocity = dampedDragAnimation.velocity / 10f
                                    scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                    scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                                },
                                onDrawSurface = {
                                    val p = dampedDragAnimation.pressProgress
                                    // 静止时主题罩面；按压时透出玻璃折射
                                    drawRect(sliderSurface.copy(alpha = 1f - p))
                                    drawRect(primary.copy(alpha = 0.08f))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {}

                    // 第 3 层（中间上）：纯文本层（点击已由最顶手势层统一处理）
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DockItem("日程", current == Tab.SCHEDULE, primary)
                        DockItem("课程", current == Tab.COURSE, primary)
                    }

                    // 第 4 层（最顶）：透明手势层——承接拖动跟手
                    // （clickable 消费 down 前，此层在 Initial/Main pass 优先收到事件，拖动不会被吞）
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(dampedDragAnimation.modifier)
                    ) {}
        }

        // ===== 相机按钮（独立圆形，与 Dock 同款液态玻璃，整体居中的另一半） =====
        Spacer(Modifier.width(12.dp))
        CameraDockButton(backdrop, primary, onClick = onCameraClick)
    }
}

/** ProDock 相机按钮：独立圆形，液态玻璃与 Dock 同款，图标随主题变色 */
@Composable
private fun CameraDockButton(backdrop: Backdrop, primary: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .glassPill(backdrop, blurRadius = 6.dp, withLens = false, tintAlpha = 0.05f)
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = "笔记相机", tint = primary)
    }
}

@Composable
private fun RowScope.DockItem(label: String, active: Boolean, primary: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ==================== 悬浮新增日程按钮（可拖动 + Dock 碰撞弹出） ==================== */

/**
 * 悬浮新增按钮：可拖动，与底部 Dock 碰撞时动画弹出到最近一侧（对应 Glass 版 fab + ejectFromDock）
 */
@Composable
private fun BoxScope.FloatingAddButton(
    backdrop: Backdrop?,
    onTap: () -> Unit,
    onToast: (String) -> Unit
) {
    val density = LocalDensity.current
    val fabSize = 56.dp
    val pad = 12.dp

    // 屏幕尺寸
    val screenW = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenH = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val navBarH = with(density) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().toPx()
    }
    val statusBarH = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }
    // 顶部移动下限：状态栏 + 标题栏区域（含标题栏 60dp 与上下留白），FAB 不能进入标题栏
    val topLimitPx = statusBarH + with(density) { 96.dp.toPx() }

    val sizePx = with(density) { fabSize.toPx() }
    val padPx = with(density) { pad.toPx() }
    val dockBottomPx = with(density) { 16.dp.toPx() } // LiquidDock vertical padding

    // 位置（px），初始右下角（Dock 上方）
    val initX = screenW - sizePx - padPx * 2
    val initY = screenH - navBarH - dockBottomPx - with(density) { 56.dp.toPx() } - sizePx - with(density) { 16.dp.toPx() }
    var fabX by remember { mutableStateOf(initX) }
    var fabY by remember { mutableStateOf(initY) }
    // 动画弹出
    val ejectAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // 计算 Dock 碰撞箱（与 LiquidDock 几何一致：水平居中、宽 = 屏宽-120dp、高56dp、底边距 nav+16dp）
    fun getDockRect(): androidx.compose.ui.geometry.Rect {
        val dockW = screenW - with(density) { 120.dp.toPx() }
        val dockX = (screenW - dockW) / 2
        val dockY = screenH - navBarH - dockBottomPx - with(density) { 56.dp.toPx() }
        return androidx.compose.ui.geometry.Rect(dockX, dockY, dockX + dockW, dockY + with(density) { 56.dp.toPx() })
    }

    fun ejectFromDock() {
        val rect = getDockRect()
        val bx = fabX; val by = fabY; val bw = sizePx; val bh = sizePx

        val hit = bx < rect.right + padPx && bx + bw > rect.left - padPx &&
            by < rect.bottom + padPx && by + bh > rect.top - padPx
        if (!hit) return

        val cx = bx + bw / 2
        val cy = by + bh / 2
        val dcx = rect.center.x
        val dcy = rect.center.y
        val dx = cx - dcx
        val dy = cy - dcy

        var nx = bx; var ny = by
        if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
            nx = if (dx >= 0) rect.right + padPx else rect.left - padPx - bw
        } else if (dy < 0) {
            ny = rect.top - padPx - bh
        } else {
            ny = rect.bottom + padPx
        }
        nx = nx.coerceIn(0f, screenW - bw)
        ny = ny.coerceIn(topLimitPx, screenH - bh)

        // 动画弹出：Animatable 作进度插值，逐帧更新位置
        val fromX = fabX; val fromY = fabY
        val toX = nx; val toY = ny
        scope.launch {
            ejectAnim.snapTo(0f)
            ejectAnim.animateTo(1f, tween(320)) {
                val v = this.value
                fabX = fromX + (toX - fromX) * v
                fabY = fromY + (toY - fromY) * v
            }
            fabX = toX; fabY = toY
        }
    }

    // FAB：graphicsLayer.translation 移动（Float 精度），阴影用 shadowElevation（硬件层，与内容同步）。
    // 关键：change.position 是相对按钮渲染位置的局部坐标（随按钮移动被补偿），
    // 因此用"局部位移 + 上次节点实际移动量"还原出手指真实窗口位移，消除半速与反馈振荡。
    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = fabX
                translationY = fabY
                shadowElevation = with(density) { 10.dp.toPx() }
                shape = CircleShape
            }
            .size(fabSize)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragging = false
                    var moved = false
                    // 上次事件的局部坐标与按钮位置
                    var prevP = down.position
                    var prevFabX = fabX
                    var prevFabY = fabY
                    drag(down.id) { change ->
                        change.consume()
                        val p = change.position
                        // 本次事件前按钮已移动的量（上次移动）
                        val nodeDx = fabX - prevFabX
                        val nodeDy = fabY - prevFabY
                        // 手指真实窗口位移 = 局部位移差 + 节点移动补偿
                        val fingerDx = (p.x - prevP.x) + nodeDx
                        val fingerDy = (p.y - prevP.y) + nodeDy
                        // 记录本次基准（更新前）
                        prevP = p
                        prevFabX = fabX
                        prevFabY = fabY
                        if (!dragging && (kotlin.math.abs(fingerDx) > 6f || kotlin.math.abs(fingerDy) > 6f)) {
                            dragging = true
                            moved = true
                        }
                        if (dragging) {
                            fabX = (fabX + fingerDx).coerceIn(0f, screenW - sizePx)
                            fabY = (fabY + fingerDy).coerceIn(topLimitPx, screenH - sizePx)
                        }
                    }
                    if (moved) {
                        ejectFromDock()
                    } else {
                        onTap()
                    }
                }
            }
            .glassCard(
                backdrop = backdrop,
                radius = 28.dp,
                blurRadius = 10.dp,
                withLens = false,
                tintAlpha = 0.21f
            )
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // 标准加号图标：矢量居中（替代全角字符，解决视觉偏移）
        Icon(
            Icons.Filled.Add,
            contentDescription = "新增日程",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

/* ==================== 侧边菜单（左滑上浮） ==================== */

@Composable
private fun SideMenu(
    backdrop: Backdrop,
    dark: Boolean,
    showCompleted: Boolean,
    doneGray: Boolean,
    showEmptyCourses: Boolean,
    themeName: String,
    remindText: String,
    onToggleDark: () -> Unit,
    onToggleCompleted: () -> Unit,
    onToggleDoneGray: () -> Unit,
    onToggleEmptyCourses: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenRemind: () -> Unit,
    onDonate: () -> Unit,
    onFeedback: () -> Unit,
    onAbout: () -> Unit,
    onClearEvents: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
    onToast: (String) -> Unit
) {
    // 左滑抽屉：悬浮在页面上方，进出动画由外层 AnimatedVisibility 驱动；
    // 全屏固定遮罩由 HomeScreen 外层提供（不随抽屉滑动）

    // 系统返回键关闭抽屉
    BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize()) {
        // 左侧抽屉面板：液态玻璃（glassCard 折射 backdrop + 硬边高光 + 高斯软阴影），
        // 顶部 statusBarsPadding 避开系统状态栏
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .gaussianShadow()
                .glassCard(
                    backdrop = backdrop,
                    radius = 20.dp,
                    blurRadius = 16.dp,
                    tintAlpha = 0.16f
                )
                .statusBarsPadding()
                .noRippleClickable(onClick = {})
        ) {
            // 品牌头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 软件图标
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = "学小通",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("学小通", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("作业日历 · 学习通", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // 可滚动内容区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                MenuSwitchItem("深色模式", "🌓", dark, onToggleDark)
                MenuArrowItem("主题配色", "🍬", themeName, onOpenTheme)
                MenuSwitchItem("查看已完成作业", "✅", showCompleted, onToggleCompleted)
                MenuSwitchItem("查看无任务点课程", "📭", showEmptyCourses, onToggleEmptyCourses)
                MenuSwitchItem("已完成任务置灰", "🎨", doneGray, onToggleDoneGray)

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                MenuArrowItem("提醒设置", "⏰", remindText, onOpenRemind)
                MenuArrowItem("支持捐赠", "💝", "", onDonate)
                MenuArrowItem("问题反馈", "💬", "", onFeedback)
                MenuArrowItem("软件详情", "ℹ️", "", onAbout)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            // 底部操作（固定，不随内容滚动）
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        // 全圆角玻璃质感按钮
                        .glassPill(backdrop, blurRadius = 8.dp, tintAlpha = 0.08f)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            RoundedCornerShape(20.dp)
                        )
                        .noRippleClickable(onClick = onClearEvents),
                    contentAlignment = Alignment.Center
                ) {
                    Text("清空自建日程", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        // 全圆角玻璃质感按钮（error 语义色）
                        .glassPill(backdrop, blurRadius = 8.dp, tintAlpha = 0.12f)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                        .noRippleClickable(onClick = onLogout),
                    contentAlignment = Alignment.Center
                ) {
                    Text("退出登录", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MenuSwitchItem(label: String, emoji: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onToggle)
            // vertical padding 比箭头项小，使总高与子菜单项（MenuArrowItem）一致
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.scale(0.78f),
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun MenuArrowItem(label: String, emoji: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        if (value.isNotEmpty()) {
            Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(6.dp))
        Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MenuNote(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 20.dp, top = 0.dp, bottom = 6.dp),
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/* ==================== 月历视图（跨天色块 + lane） ==================== */

private val WEEK_LABELS = listOf("日", "一", "二", "三", "四", "五", "六")

@Composable
private fun MonthCalendarView(
    backdrop: Backdrop,
    works: List<Work>,
    showCompleted: Boolean,
    doneGray: Boolean,
    customEvents: List<com.xuexiaotong.data.CustomEvent>,
    theme: ThemeColor,
    dark: Boolean,
    year: Int,
    month: Int,
    onChangeMonth: (Int) -> Unit,
    onWorkClick: (Work) -> Unit
) {
    val primary = parseHex(if (dark) theme.darkPrimary else theme.lightPrimary)
    // 月份线性 key：year*12 + (month-1)，保证 month=12 不跨年进位，反解 y=key/12, m=key%12+1
    val monthKey = year * 12 + (month - 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 日历卡片：层内玻璃（glassSheet 模拟标题栏液态玻璃视觉，防自折射崩溃）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .glassSheet(tint = if (dark) Color(0xFF33333B) else Color.White, radius = 16.dp, dark = dark)
        ) {
            Column(
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var accumulated = 0f
                        var started = false
                        detectHorizontalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onDragEnd = {
                                if (started) {
                                    if (accumulated < -40f) onChangeMonth(1)
                                    else if (accumulated > 40f) onChangeMonth(-1)
                                }
                            },
                            onDragCancel = {}
                        ) { change, dragAmount ->
                            change.consume()
                            started = true
                            accumulated += dragAmount
                        }
                    }
            ) {
                // 星期表头
                Row(Modifier.fillMaxWidth()) {
                    WEEK_LABELS.forEach {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                // 日历主体：相邻月份滑动切换（新月份从相应方向滑入，对应 Glass 版 slide-left/right）
                AnimatedContent(
                    targetState = monthKey,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally { -it / 2 } + fadeOut(tween(220)))
                        } else {
                            (slideInHorizontally { -it } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally { it / 2 } + fadeOut(tween(220)))
                        }
                    }
                ) { key ->
                    // 反解 key：y = key/12，m = key%12 + 1（与 monthKey 的 year*12+(month-1) 对应）
                    val y = key / 12
                    val m = key % 12 + 1
                    val model = remember(key, works, customEvents, showCompleted, doneGray) {
                        buildMonth(y, m, works, customEvents, showCompleted)
                    }
                    Column {
                        // 6 行日历
                        model.rows.forEachIndexed { ri, row ->
                            MonthRowView(
                                row = row,
                                isShade = ri % 2 == 1,
                                primary = primary,
                                dark = dark,
                                doneGray = doneGray,
                                onWorkClick = onWorkClick
                            )
                        }

                        // 空状态
                        if (model.noWorks) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(6.dp).background(primary.copy(alpha = 0.5f), CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text("本月暂无作业安排", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun MonthRowView(
    row: CalendarRow,
    isShade: Boolean,
    primary: Color,
    dark: Boolean,
    doneGray: Boolean,
    onWorkClick: (Work) -> Unit
) {
    val cellBg = if (isShade) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f) else Color.Transparent

    Column(Modifier.fillMaxWidth()) {
        // 日期层
        Row(Modifier.fillMaxWidth().background(cellBg)) {
            row.cells.forEach { cell ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (cell.isToday) primary.copy(alpha = 0.16f) else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (cell.day == 0) "" else "${cell.day}",
                            fontSize = 12.sp,
                            color = when {
                                cell.isToday -> primary
                                cell.outside -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // 色块层
        if (row.blocks.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((row.layerHeight).dp)
                    .padding(top = 2.dp)
            ) {
                row.blocks.forEach { b ->
                    WorkBlockView(b, dark = dark, doneGray = doneGray, onWorkClick = onWorkClick)
                }
            }
        } else {
            Spacer(Modifier.height(CalendarModel.LAYER_PAD.dp))
        }
    }
}

@Composable
private fun BoxScope.WorkBlockView(
    b: CalendarBlock,
    dark: Boolean,
    doneGray: Boolean,
    onWorkClick: (Work) -> Unit
) {
    val cellPct = 100f / 7f
    val leftPct = b.colStart * cellPct
    val widthPct = (b.colEnd - b.colStart + 1) * cellPct
    val topPx = b.lane * CalendarModel.BLOCK_STEP

    // 父容器宽度（色块层 Box 的宽度，px），用于计算 x 偏移
    var parentWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val done = b.work.isDone
    val gray = done && doneGray
    val bg = if (gray) Color(0xFFE5E7EB) else parseHex(b.work.colorBg).copy(alpha = if (dark) 0.75f else 1f)
    val text = if (gray) Color(0xFF9CA3AF) else parseHex(b.work.colorText)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { parentWidth = it.width }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthPct / 100f)
                .offset {
                    // offset lambda 直接以 px 计算，避免 px/dp 混淆
                    IntOffset(
                        x = (parentWidth * leftPct / 100f).roundToInt(),
                        y = with(density) { topPx.dp.roundToPx() }
                    )
                }
                .padding(horizontal = 1.dp)
                .height(CalendarModel.BLOCK_H.dp)
                .background(bg, RoundedCornerShape(6.dp))
                .noRippleClickable { onWorkClick(b.work) },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    b.work.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = text
                )
                if (b.continueNext) {
                    Text("▸", fontSize = 8.sp, lineHeight = 11.sp, color = text)
                }
            }
        }
    }
}

/* ==================== 课程进度 ==================== */

@Composable
private fun CourseProgressList(
    backdrop: Backdrop,
    progress: List<CourseProgress>,
    theme: ThemeColor,
    dark: Boolean,
    showEmpty: Boolean
) {
    val primary = parseHex(if (dark) theme.darkPrimary else theme.lightPrimary)

    Column(Modifier.fillMaxSize()) {
        val filtered = progress.filter { showEmpty || it.totalCount > 0 }
        if (filtered.isEmpty()) {
            Text(
                "暂无课程进度，点击右上角同步获取",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // 底部占位：避免最后一项被 Dock 遮挡
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // 顶部总览：液态玻璃卡片（课程数 / 总进度 / 已完成任务点）
                item(key = "__overview__") {
                    CourseOverviewCard(backdrop = backdrop, list = filtered, primary = primary, dark = dark)
                }
                items(filtered, key = { it.courseId }) { p ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            // 层内玻璃（glassSheet 模拟标题栏液态玻璃视觉）
                            .glassSheet(tint = if (dark) Color(0xFF33333B) else Color.White, radius = 12.dp, dark = dark)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            // 第一行：课程名（左） + 百分比（右）
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    p.name,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (p.totalCount > 0) "${p.percent}%" else "暂无任务点",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (p.totalCount > 0) primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // 第二行：全长进度条
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(p.percent / 100f)
                                        .height(6.dp)
                                        .background(primary, RoundedCornerShape(3.dp))
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // 第三行：居左 已完成任务点 xx/xx
                            Text(
                                if (p.totalCount > 0) "已完成任务点 ${p.doneCount}/${p.totalCount}" else "暂无任务点",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 课程总览：液态玻璃卡片，左中右三块均等分配（课程数 / 总进度 / 已完成任务点）
 */
@Composable
private fun CourseOverviewCard(
    backdrop: Backdrop,
    list: List<CourseProgress>,
    primary: Color,
    dark: Boolean
) {
    val totalDone = list.sumOf { it.doneCount }
    val totalAll = list.sumOf { it.totalCount }
    val percent = if (totalAll > 0) (totalDone * 100 / totalAll).coerceAtMost(100) else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            // 层内玻璃（glassSheet 模拟标题栏液态玻璃视觉）
            .glassSheet(tint = if (dark) Color(0xFF33333B) else Color.White, radius = 16.dp, dark = dark)
    ) {
        // 左中右三块均等分配
        Row(Modifier.padding(vertical = 16.dp)) {
            CourseOverviewStat(
                value = "${list.size}",
                label = "门课程",
                valueColor = primary,
                modifier = Modifier.weight(1f)
            )
            CourseOverviewStat(
                value = "$percent%",
                label = "总进度",
                valueColor = primary,
                modifier = Modifier.weight(1f)
            )
            CourseOverviewStat(
                value = if (totalAll > 0) "$totalDone/$totalAll" else "0/0",
                label = "已完成任务点",
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 总览单块：上数字（加粗）+ 下小标签 */
@Composable
private fun RowScope.CourseOverviewStat(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ==================== 主题弹窗 ==================== */

@Composable
private fun GlassThemeDialog(
    backdrop: Backdrop,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        title = "主题配色",
        confirmText = "完成",
        onConfirm = onDismiss
    ) {
        Column {
            Themes.ALL.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { t ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.noRippleClickable { onSelect(t.id) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(parseHex(t.swatch), CircleShape)
                                    .noRippleClickable { onSelect(t.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (t.id == current) {
                                    Text("✓", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                t.name,
                                fontSize = 11.sp,
                                color = if (t.id == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ==================== 提醒设置弹窗 ==================== */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlassRemindDialog(
    backdrop: Backdrop,
    setting: RemindSetting,
    onSave: (RemindSetting) -> Unit,
    onDismiss: () -> Unit,
    onTest: () -> Unit
) {
    var enabled by remember { mutableStateOf(setting.enabled) }
    var lead by remember { mutableStateOf(setting.leadMinutes) }
    var onlyTodo by remember { mutableStateOf(setting.onlyTodo) }

    val leadOptions = listOf(0 to "截止时", 60 to "提前1小时", 360 to "提前6小时", 720 to "提前12小时", 1440 to "提前1天")
    val context = LocalContext.current

    GlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        title = "提醒设置",
        confirmText = "保存",
        middleText = "测试通知",
        onConfirm = {
            onSave(RemindSetting(enabled, lead, onlyTodo))
            onDismiss()
        },
        onMiddle = onTest
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("开启作业提醒", Modifier.weight(1f), fontSize = 14.sp)
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    modifier = Modifier.scale(0.78f),
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
            if (enabled) {
                HorizontalDivider()
                Text("提前提醒", fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                // FlowRow 自动换行：5 个选项完整显示，不再横向溢出裁剪
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    leadOptions.forEach { (v, label) ->
                        Box(
                            modifier = Modifier
                                .noRippleClickable { lead = v }
                                .background(
                                    if (lead == v) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(label, fontSize = 11.sp, color = if (lead == v) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("仅提醒未完成", Modifier.weight(1f), fontSize = 14.sp)
                    Switch(
                        checked = onlyTodo,
                        onCheckedChange = { onlyTodo = it },
                        modifier = Modifier.scale(0.78f),
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
                // 精确闹钟权限引导（Android 12+：未授权时提醒会延迟）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val am = context.getSystemService(AlarmManager::class.java)
                    if (!am.canScheduleExactAlarms()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .noRippleClickable {
                                    try {
                                        context.startActivity(
                                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        )
                                    } catch (_: Exception) { /* 部分机型无此入口 */ }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "精确提醒未开启，提醒可能延迟。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "去授权",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ==================== 笔记相机引导弹窗 ==================== */

@Composable
private fun GlassCameraGuideDialog(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onOpenCamera: (String) -> Unit,
    onOpenGallery: () -> Unit,
    onNewSubject: () -> Unit
) {
    var subjects by remember { mutableStateOf(NoteStore.getSubjects()) }
    val primary = MaterialTheme.colorScheme.primary

    GlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        title = "笔记相机",
        confirmText = "关闭",
        onConfirm = { onDismiss() }
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                "拍摄前先选择科目；照片仅保存在应用内，不会进入系统相册。",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // 科目区：科目列表（点击进入相册并定位该科目）；空态引导新建
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .padding(8.dp)
            ) {
                if (subjects.isEmpty()) {
                    Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text(
                            "尚未创建科目",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "新建科目后，拍摄的照片将按科目自动归档。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "+ 新建科目",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primary,
                            modifier = Modifier.noRippleClickable(onClick = onNewSubject)
                        )
                    }
                } else {
                    subjects.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .noRippleClickable { onOpenCamera(s.id) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .background(primary.copy(alpha = 0.8f), CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                s.name,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${NoteStore.countPhotos(s.id)} 张",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "+ 新建科目",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primary,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .noRippleClickable(onClick = onNewSubject)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // 进入相册入口（与科目按钮视觉区分：图标行 + 边框）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        primary.copy(alpha = 0.45f),
                        RoundedCornerShape(12.dp)
                    )
                    .noRippleClickable(onClick = onOpenGallery)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "进入相册",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = primary
                )
            }
        }
    }
}

/* ==================== 隐私声明弹窗 ==================== */

@Composable
private fun GlassOpenSourceDialog(backdrop: Backdrop, onDismiss: () -> Unit) {
    GlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        title = "开源致谢",
        confirmText = "我知道了",
        onConfirm = { onDismiss() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "本软件基于以下开源项目构建，均遵循各自开源许可证：",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            OpenSourceItem("AndroidLiquidGlass", "Apache-2.0", "液态玻璃渲染方案参考；Dock 滑块拖拽阻尼动画与液态交互移植", "https://github.com/burulangtu/AndroidLiquidGlass")
            Spacer(Modifier.height(8.dp))
            OpenSourceItem("Backdrop", "Apache-2.0", "界面模糊折射组件（液态玻璃底层渲染）", "https://github.com/kyant0/backdrop")
            Spacer(Modifier.height(8.dp))
            OpenSourceItem("ComposeShapes", "Apache-2.0", "圆角形状扩展", "https://github.com/kyant0/ComposeShapes")
            Spacer(Modifier.height(8.dp))
            OpenSourceItem("CamerAwesome", "MIT", "相机多镜头识别（35mm 等效焦距分类）与镜头轮换切换方案", "https://github.com/Apparence-io/camerawesome")
            Spacer(Modifier.height(8.dp))
            OpenSourceItem("OpenCamera", "GPL-3.0", "点击对焦与测光（AF/AE REGIONS）方案", "https://github.com/almalence/OpenCamera")
            Spacer(Modifier.height(8.dp))
            OpenSourceItem("PictureSelector / Luban", "Apache-2.0", "相册缩略图采样解码与缓存思想", "https://github.com/LuckSiege/PictureSelector")
            Spacer(Modifier.height(10.dp))
            Text(
                "许可证全文见各项目仓库 LICENSE 文件。",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun OpenSourceItem(name: String, license: String, desc: String, url: String) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            "$name（$license）",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // 项目地址：点击调用外部浏览器打开
        Text(
            url,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.noRippleClickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) { /* 无浏览器时忽略 */ }
            }
        )
    }
}

/* ==================== 隐私声明弹窗 ==================== */

@Composable
private fun GlassPrivacyDialog(backdrop: Backdrop, onDismiss: () -> Unit) {
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
                .padding(horizontal = 24.dp)
                .height(480.dp)
                .gaussianShadow(blur = 11.dp, offsetY = 2.5.dp)
                .glassCard(
                    backdrop = backdrop,
                    radius = 20.dp,
                    blurRadius = 16.dp,
                    tintAlpha = 0.16f
                )
                .noRippleClickable(onClick = {}),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("学小通 · 隐私声明", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 14.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
            ) {
                SoftwarePrivacyContent()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassDialogButton(backdrop, "我知道了", isPrimary = true) { onDismiss() }
            }
        }
    }
}
