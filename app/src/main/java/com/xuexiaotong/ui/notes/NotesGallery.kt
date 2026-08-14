package com.xuexiaotong.ui.notes

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.xuexiaotong.data.NotePhoto
import com.xuexiaotong.data.NoteStore
import com.xuexiaotong.data.NoteSubject
import com.xuexiaotong.data.ThemeColor
import com.xuexiaotong.data.UNCATEGORIZED_ID
import com.xuexiaotong.ui.theme.GlassConfirmDialog
import com.xuexiaotong.ui.theme.GlassDialog
import com.xuexiaotong.ui.theme.GlassPopup
import com.xuexiaotong.ui.theme.glassBackground
import com.xuexiaotong.ui.theme.glassPill
import com.xuexiaotong.ui.theme.noRippleClickable
import com.xuexiaotong.ui.theme.parseHex
import com.xuexiaotong.ui.theme.rememberGlassBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 笔记专属相册全屏页：标题栏与主页一致（玻璃胶囊），
 * 左侧为科目下拉、主标题为当前科目名、右侧多选时出现删除。
 * 独立玻璃 backdrop：相册页为全屏覆盖层，必须自建 backdrop（引用主页 backdrop 会折射出主页内容）。
 */
@Composable
fun NotesGalleryScreen(
    theme: ThemeColor,
    dark: Boolean,
    initialSubjectId: String?,
    onDismiss: () -> Unit,
    onManageSubjects: () -> Unit
) {
    // 相册页独立玻璃背景：录制本页背景与内容层
    val galleryBackdrop = rememberGlassBackdrop()
    // 与主页同源的背景：基础色 + 半透明主题渐变
    val gradTop = parseHex(if (dark) theme.bgGradDarkStart else theme.bgGradLightStart).copy(alpha = 0.42f)
    val gradBottom = parseHex(if (dark) theme.bgGradDarkEnd else theme.bgGradLightEnd).copy(alpha = 0.42f)
    val baseColor = if (dark) Color(0xFF17171A) else Color(0xFFFDFDFE)

    var currentSubjectId by remember { mutableStateOf(initialSubjectId) }
    // 每次重组读取最新科目（新建/重命名/删除后自动反映到下拉列表）
    val subjects = NoteStore.getSubjects()
    var photos by remember { mutableStateOf(NoteStore.getPhotos(currentSubjectId)) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    val multiSelect = selection.isNotEmpty()
    val context = LocalContext.current

    fun refresh() {
        photos = NoteStore.getPhotos(currentSubjectId)
    }

    fun toast(msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    // 移动选中照片到目标科目
    fun moveSelectionTo(targetSubjectId: String) {
        NoteStore.movePhotos(selection, targetSubjectId)
        showMoveDialog = false
        selection = emptySet()
        refresh()
    }

    // 导出选中照片到系统相册
    fun exportSelection() {
        val n = NoteStore.exportToGallery(selection)
        showActionDialog = false
        toast("已导出 $n 张照片到系统相册")
    }

    val subjectName = when (currentSubjectId) {
        null -> "全部"
        UNCATEGORIZED_ID -> "未分类"
        else -> subjects.find { it.id == currentSubjectId }?.name ?: "全部"
    }

    BackHandler { if (multiSelect) selection = emptySet() else onDismiss() }

    // 悬浮标题栏占位高度（状态栏 + 6 + 60 + 6 + 列表顶部留白 4）
    val density = LocalDensity.current
    val statusBarPx = runCatching {
        val res = context.resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        if (id > 0) res.getDimensionPixelSize(id) else 0
    }.getOrDefault(0)
    val topPlaceholder = with(density) { (statusBarPx + 76.dp.toPx()).toDp() }

    // 独立页面：与主页同源的超浅主题色背景，仅复用标题栏样式
    Box(Modifier.fillMaxSize()) {
        // 玻璃录制层：背景 + 滚动内容（悬浮标题栏/底部条/弹窗折射本页内容，不穿帮主页）
        Box(
            Modifier
                .fillMaxSize()
                .background(baseColor)
                .background(Brush.verticalGradient(listOf(gradTop, gradBottom)))
                .glassBackground(galleryBackdrop)
        ) {
            // 内容层：全屏，列表顶部以占位让出悬浮标题栏（照片从标题栏玻璃下方滚过）
            if (photos.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = topPlaceholder),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (currentSubjectId == null) "相册暂无照片\n点击相机按钮拍摄课件笔记" else "该科目暂无照片",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // groupBy 保持 photos 的降序时间顺序
            val groups = photos.groupBy { formatDateLabel(it.takenAt) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = topPlaceholder, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEach { (label, list) ->
                    item(key = "header_$label") {
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    items(list.chunked(4), key = { it.first().id }) { rowPhotos ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            rowPhotos.forEach { photo ->
                                Box(Modifier.weight(1f)) {
                                    GalleryPhotoItem(
                                        photo = photo,
                                        selected = photo.id in selection,
                                        onClick = {
                                            if (multiSelect) {
                                                selection = if (photo.id in selection) selection - photo.id else selection + photo.id
                                            } else {
                                                openSystemViewer(context, photo)
                                            }
                                        },
                                        onLongPress = {
                                            if (!multiSelect) selection = setOf(photo.id)
                                            else selection = if (photo.id in selection) selection - photo.id else selection + photo.id
                                        }
                                    )
                                }
                            }
                            // 补齐不足 4 列的空位
                            repeat(4 - rowPhotos.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        }

        // 悬浮标题栏（液态玻璃胶囊，不占布局：列表滚动到其下方）
        // 主副标题绝对居中（不受左右按钮宽度不对称影响）
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .height(60.dp)
                .glassPill(galleryBackdrop, blurRadius = 8.dp, tintAlpha = 0.05f)
        ) {
            // 居中：主标题 + 副标题科目（绝对居中）
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "笔记相册",
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    subjectName,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            // 左侧：科目下拉按钮（固定左端）
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .offset(x = 6.dp)
                    .noRippleClickable(onClick = { showPicker = true }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "选择科目",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // 右侧：多选时 删除按钮（左移）+ 多功能按钮（最右，固定右端）
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = -6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (multiSelect) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .noRippleClickable { showDeleteConfirm = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "删除选中照片",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .noRippleClickable { showActionDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 多选底部提示条
        AnimatedVisibility(
            visible = multiSelect,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .glassPill(galleryBackdrop, blurRadius = 8.dp, tintAlpha = 0.08f)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "已选 ${selection.size} 张",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 全选/取消全选（当前科目全部照片）
                Text(
                    if (selection.size == photos.size && photos.isNotEmpty()) "取消全选" else "全选",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.noRippleClickable {
                        selection = if (selection.size == photos.size && photos.isNotEmpty()) {
                            emptySet()
                        } else {
                            photos.map { it.id }.toSet()
                        }
                    }
                )
                Text(
                    "取消",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.noRippleClickable { selection = emptySet() }
                )
            }
        }
    }

    // 科目选择弹窗（复用主界面弹窗动画：淡入 + 弹性缩放）
    GlassPopup(showPicker) {
        GlassDialog(
            backdrop = galleryBackdrop,
            onDismiss = { showPicker = false },
            title = "选择科目",
            dismissText = "关闭"
        ) {
            Column(Modifier.fillMaxWidth()) {
                SubjectPickRow("全部", NoteStore.countPhotos(null), currentSubjectId == null) {
                    currentSubjectId = null
                    refresh()
                    showPicker = false
                }
                SubjectPickRow("未分类", NoteStore.countPhotos(UNCATEGORIZED_ID), currentSubjectId == UNCATEGORIZED_ID) {
                    currentSubjectId = UNCATEGORIZED_ID
                    refresh()
                    showPicker = false
                }
                subjects.forEach { s ->
                    SubjectPickRow(s.name, NoteStore.countPhotos(s.id), currentSubjectId == s.id) {
                        currentSubjectId = s.id
                        refresh()
                        showPicker = false
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "管理科目 ›",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleClickable {
                            showPicker = false
                            onManageSubjects()
                        }
                )
            }
        }
    }

    // 删除确认（复用主界面弹窗动画）
    GlassPopup(showDeleteConfirm) {
        GlassConfirmDialog(
            backdrop = galleryBackdrop,
            onDismiss = { showDeleteConfirm = false },
            title = "删除照片",
            message = "确定删除选中的 ${selection.size} 张照片吗？删除后不可恢复。",
            confirmText = "删除",
            onConfirm = {
                NoteStore.deletePhotos(selection)
                selection = emptySet()
                showDeleteConfirm = false
                refresh()
            }
        )
    }

    // 多功能弹窗（多选三点按钮）：更改科目 / 导出到系统相册
    GlassPopup(showActionDialog) {
        GlassDialog(
            backdrop = galleryBackdrop,
            onDismiss = { showActionDialog = false },
            title = "更多操作",
            dismissText = "关闭"
        ) {
            Column(Modifier.fillMaxWidth()) {
                ActionRow(
                    icon = {
                        Icon(
                            Icons.Outlined.DriveFileMove,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = "更改选中照片的科目",
                    sub = "${selection.size} 张",
                    onClick = {
                        showActionDialog = false
                        showMoveDialog = true
                    }
                )
                Spacer(Modifier.height(4.dp))
                ActionRow(
                    icon = {
                        Icon(
                            Icons.Outlined.IosShare,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = "导出到系统相册",
                    sub = "${selection.size} 张",
                    onClick = { exportSelection() }
                )
            }
        }
    }

    // 更改科目二级弹窗：选择目标科目（含未分类），选中即移动
    GlassPopup(showMoveDialog) {
        GlassDialog(
            backdrop = galleryBackdrop,
            onDismiss = { showMoveDialog = false },
            title = "更改科目",
            dismissText = "关闭"
        ) {
            Column(Modifier.fillMaxWidth()) {
                SubjectPickRow("未分类", selection.size, false) { moveSelectionTo(UNCATEGORIZED_ID) }
                subjects.forEach { s ->
                    SubjectPickRow(s.name, selection.size, false) { moveSelectionTo(s.id) }
                }
            }
        }
    }
}

/** 多功能弹窗操作行：图标 + 名称 + 数量副标题 */
@Composable
private fun ActionRow(
    icon: @Composable () -> Unit,
    label: String,
    sub: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            sub,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubjectPickRow(name: String, count: Int, active: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) primary.copy(alpha = 0.10f) else Color.Transparent)
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "$count",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryPhotoItem(
    photo: NotePhoto,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val corner = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(corner)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .then(
                if (selected) Modifier.border(2.5.dp, primary, corner) else Modifier
            )
    ) {
        PhotoThumb(photo, Modifier.fillMaxSize())
        if (selected) {
            // 选中角标（Icon 居中无文本基线偏移）
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .background(primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "已选中",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/** 缩略图：本地解码（采样降内存），无第三方图片库依赖 */
@Composable
private fun PhotoThumb(photo: NotePhoto, modifier: Modifier = Modifier) {
    // IO 线程采样解码 + LruCache 缓存（避免主线程全尺寸解码导致卡顿）
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photo.fileName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = NoteStore.photoFile(photo)
                if (file.exists()) ThumbnailStore.get(file.absolutePath) else null
            }.getOrNull()
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)))
    }
}

/**
 * 科目管理弹窗：新建 / 重命名 / 删除（删除后照片移入未分类）
 */
@Composable
fun NoteSubjectManageDialog(
    backdrop: Backdrop,
    onDismiss: () -> Unit
) {
    var subjects by remember { mutableStateOf(NoteStore.getSubjects()) }
    var newName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<NoteSubject?>(null) }
    var renameName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<NoteSubject?>(null) }
    val context = LocalContext.current

    fun toast(msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    if (renameTarget == null && deleteTarget == null) {
        GlassDialog(
            backdrop = backdrop,
            onDismiss = onDismiss,
            title = "科目管理",
            confirmText = "完成",
            onConfirm = { onDismiss() }
        ) {
            Column(Modifier.fillMaxWidth()) {
                // 新建
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("输入科目名", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .noRippleClickable {
                                val s = NoteStore.addSubject(newName)
                                if (s == null) toast("科目已存在或名称为空")
                                else {
                                    newName = ""
                                    subjects = NoteStore.getSubjects()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "新建科目",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (subjects.isEmpty()) {
                    Text(
                        "暂无科目，输入名称新建",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    subjects.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "重命名",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(start = 12.dp)
                                    .noRippleClickable {
                                        renameTarget = s
                                        renameName = s.name
                                    }
                            )
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "删除科目",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(start = 10.dp)
                                    .noRippleClickable { deleteTarget = s }
                            )
                        }
                    }
                }
            }
        }
    }

    // 重命名子弹窗
    renameTarget?.let { target ->
        GlassDialog(
            backdrop = backdrop,
            onDismiss = { renameTarget = null },
            title = "重命名科目",
            confirmText = "保存",
            dismissText = "取消",
            onConfirm = {
                if (NoteStore.renameSubject(target.id, renameName)) {
                    subjects = NoteStore.getSubjects()
                } else toast("科目已存在或名称为空")
                renameTarget = null
            }
        ) {
            TextField(
                value = renameName,
                onValueChange = { renameName = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }

    // 删除确认（照片移入未分类）
    deleteTarget?.let { target ->
        GlassConfirmDialog(
            backdrop = backdrop,
            onDismiss = { deleteTarget = null },
            title = "删除科目",
            message = "确定删除科目「${target.name}」吗？该科目下 ${NoteStore.countPhotos(target.id)} 张照片将移入「未分类」。",
            confirmText = "删除",
            onConfirm = {
                NoteStore.deleteSubject(target.id)
                deleteTarget = null
                subjects = NoteStore.getSubjects()
            }
        )
    }
}

/** 调起系统原生图片查看器（自带翻页/缩放/平移手势）；相册页与拍照页共用 */
internal fun openSystemViewer(context: android.content.Context, photo: NotePhoto) {
    val file = NoteStore.photoFile(photo)
    if (!file.exists()) return
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context, "com.xuexiaotong.fileprovider", file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/jpeg")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}

/** 日期分组标签：今天显示"今天"，其余显示日期 */
private fun formatDateLabel(takenAt: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = fmt.format(Date())
    val day = fmt.format(Date(takenAt))
    return if (day == today) "今天" else day
}
