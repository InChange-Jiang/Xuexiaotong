@file:OptIn(ExperimentalCamera2Interop::class)

package com.xuexiaotong.ui.notes

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.exifinterface.media.ExifInterface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.xuexiaotong.MainActivity
import com.xuexiaotong.data.NotePhoto
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.xuexiaotong.data.NoteStore
import com.xuexiaotong.data.UNCATEGORIZED_ID
import com.xuexiaotong.ui.theme.GlassDialog
import com.xuexiaotong.ui.theme.GlassPopup
import com.xuexiaotong.ui.theme.glassBackground
import com.xuexiaotong.ui.theme.glassPill
import com.xuexiaotong.ui.theme.noRippleClickable
import com.xuexiaotong.ui.theme.rememberGlassBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * 拍照页（C1）：CameraX 取景（4:3 预览框）+ 快门拍摄，照片直接存入当前科目目录并登记元数据。
 * 相机权限释放：页面销毁（onDispose）与退到后台（ON_STOP）时均 unbind，回到前台自动重建。
 * 独立玻璃 backdrop：相机页为全屏覆盖层，必须自建 backdrop（引用主页 backdrop 会折射出主页内容）。
 */
@Composable
fun NotesCameraScreen(
    dark: Boolean,
    subjectId: String,
    onDismiss: () -> Unit,
    onNewSubject: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // 相机页独立玻璃背景：只录制相机页自身黑色背景层
    val cameraBackdrop = rememberGlassBackdrop()

    var currentSubjectId by remember { mutableStateOf(subjectId) }
    var subjects by remember { mutableStateOf(NoteStore.getSubjects()) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lastPhoto by remember { mutableStateOf<NotePhoto?>(null) }
    var showSubjectPicker by remember { mutableStateOf(false) }
    var taking by remember { mutableStateOf(false) }
    // 快门快闪反馈（黑色闪屏增强确认感）
    val flashAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // 设备方向（陀螺仪/传感器检测），拍摄时写入照片 EXIF orientation
    var targetRotation by remember { mutableStateOf(Surface.ROTATION_0) }

    // 方向传感器监听：四舍五入量化到 90°，映射为 CameraX targetRotation
    // 映射依据 CamerAwesome（生产级相机库）验证过的方案：
    // 设备顶部朝左（45-135°）→ ROTATION_270；顶部朝右（225-315°）→ ROTATION_90
    val orientationListener = remember(context) {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                var current = (orientation + 45) / 90 * 90
                if (current == 360) current = 0
                targetRotation = when (current) {
                    in 225 until 315 -> Surface.ROTATION_90
                    in 135 until 225 -> Surface.ROTATION_180
                    in 45 until 135 -> Surface.ROTATION_270
                    else -> Surface.ROTATION_0
                }
            }
        }
    }
    DisposableEffect(Unit) {
        orientationListener.enable()
        onDispose { orientationListener.disable() }
    }

    // 相机权限：经由 MainActivity 的 launcher 请求
    fun requestPermission() {
        (context as? MainActivity)?.requestCameraPermission { granted ->
            hasPermission = granted
        }
    }

    fun subjectName(): String = when (currentSubjectId) {
        UNCATEGORIZED_ID -> "未分类"
        else -> subjects.find { it.id == currentSubjectId }?.name ?: "未分类"
    }

    BackHandler { onDismiss() }

    Box(Modifier.fillMaxSize()) {
        // 玻璃录制层：纯黑背景（相机页独立 backdrop 的折射源，避免透出主页）
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .glassBackground(cameraBackdrop)
        )
        if (hasPermission) {
            // ===== CameraX 预览 + 释放管理 =====
            // COMPATIBLE（TextureView）：预览画面渲染在视图层级内，Compose 的位移/淡出返回动画
            // 能作用于预览本身，避免 SurfaceView 独立窗口层导致的返回动画"跳变"
            val previewView = remember {
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            }
            var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
            var camera by remember { mutableStateOf<Camera?>(null) }
            var bound by remember { mutableStateOf(false) }
            // 全后置镜头列表（主摄/广角/长焦，按 35mm 等效焦距分类，方案参考 CamerAwesome）与当前索引
            var backLenses by remember { mutableStateOf<List<BackLens>>(emptyList()) }
            var lensIndex by remember { mutableStateOf(0) }

            fun unbindCamera() {
                cameraProvider?.unbindAll()
                camera = null
                bound = false
            }

            fun bindCamera() {
                val provider = cameraProvider ?: return
                // 首次绑定：枚举所有后置镜头
                if (backLenses.isEmpty()) {
                    backLenses = buildBackLenses(provider)
                    lensIndex = backLenses.indexOfFirst { it.type == LensType.WIDE }.takeIf { it >= 0 }
                        ?: 0
                }
                val preview = Preview.Builder().build().also {
                    // 应用锁竖屏：预览始终按竖屏渲染，避免异常旋转 90°
                    it.targetRotation = Surface.ROTATION_0
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture
                // 按目标镜头 cameraId 过滤构造 CameraSelector
                val targetId = backLenses.getOrNull(lensIndex)?.id
                val selector = if (targetId != null) {
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .addCameraFilter { infos ->
                            infos.filter {
                                Camera2CameraInfo.from(it).cameraId == targetId
                            }
                        }
                        .build()
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                try {
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        lifecycleOwner, selector, preview, capture
                    )
                    bound = true
                } catch (e: Exception) {
                    bound = false
                }
            }

            // 轮换切换后置镜头（主摄 → 广角 → 长焦 → 主摄…）
            fun switchLens() {
                if (backLenses.size > 1) {
                    lensIndex = (lensIndex + 1) % backLenses.size
                    bindCamera()
                }
            }

            DisposableEffect(Unit) {
                val providerFuture = ProcessCameraProvider.getInstance(context)
                var disposed = false
                val listener = Runnable {
                    if (!disposed) {
                        cameraProvider = providerFuture.get()
                        bindCamera()
                    }
                }
                providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

                // 退后台释放相机，回前台重建（页面在栈中时）
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> unbindCamera()
                        Lifecycle.Event.ON_START -> bindCamera()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    disposed = true
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    unbindCamera()
                }
            }

            fun takePhoto() {
                val capture = imageCapture ?: return
                if (taking) return
                taking = true
                // 快闪黑屏：增强快门确认感
                scope.launch {
                    flashAlpha.snapTo(0.9f)
                    flashAlpha.animateTo(0f, tween(300))
                }
                // 将检测到的设备方向设为输出目标旋转（影响 imageInfo.rotationDegrees）
                capture.targetRotation = targetRotation
                val dir = NoteStore.subjectDir(currentSubjectId)
                val file = File(dir, "${UUID.randomUUID().toString().take(8)}.jpg")
                capture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            try {
                                // CameraX 计算的真实旋转角（已综合传感器朝向与 targetRotation）
                                val degrees = image.imageInfo.rotationDegrees
                                val raw = image.toBitmap()
                                // 像素按实际旋转角旋转（所见即所得，与预览一致）
                                val rotated = if (degrees != 0) {
                                    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
                                    Bitmap.createBitmap(
                                        raw, 0, 0, raw.width, raw.height, matrix, true
                                    ).also { if (raw != it) raw.recycle() }
                                } else raw
                                FileOutputStream(file).use { out ->
                                    rotated.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                }
                                rotated.recycle()
                                // EXIF 写死为 NORMAL：像素已旋转，避免查看器二次旋转
                                runCatching {
                                    val exif = ExifInterface(file.absolutePath)
                                    exif.setAttribute(
                                        ExifInterface.TAG_ORIENTATION,
                                        ExifInterface.ORIENTATION_NORMAL.toString()
                                    )
                                    exif.saveAttributes()
                                }
                                val photo = NoteStore.addCapturedPhoto(currentSubjectId, file)
                                if (photo != null) lastPhoto = photo
                            } catch (e: Exception) {
                                file.delete()
                            } finally {
                                image.close()
                                taking = false
                            }
                        }

                        override fun onError(exc: ImageCaptureException) {
                            file.delete()
                            taking = false
                        }
                    }
                )
            }

            // 屏幕垂直三段布局：顶部菜单(16%) / 取景框(60%) / 底部快门区(24%)
            Column(Modifier.fillMaxSize()) {
                // ===== 顶部菜单区 0-16%（中线 8%）=====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(16f)
                        .statusBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                .noRippleClickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "关闭相机",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        // 科目胶囊：点击切换科目
                        Row(
                            modifier = Modifier
                                .glassPill(backdrop = null, blurRadius = 8.dp, tintAlpha = 0.10f)
                                .noRippleClickable(onClick = { showSubjectPicker = true })
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                subjectName(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "切换科目",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        // 右侧占位（前后摄切换在 C3）
                        Box(Modifier.size(40.dp))
                    }
                }

                // ===== 取景框区 12-72%（中线 42%）：4:3 预览框居中 + 捏合变焦 + 点击对焦/测光 =====
                var zoomRatio by remember { mutableStateOf(1f) }
                // 点击对焦/测光：焦点位置 + 指示动画进度（方案参考 OpenCamera FocusVFPlugin：点击同时设置对焦与测光区域）
                var focusPoint by remember { mutableStateOf<Offset?>(null) }
                val focusAnim = remember { Animatable(0f) }
                LaunchedEffect(focusPoint) {
                    if (focusPoint != null) {
                        focusAnim.snapTo(0f)
                        focusAnim.animateTo(1f, tween(250))
                        delay(900)
                        focusPoint = null
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(60f),
                    contentAlignment = Alignment.Center
                ) {
                    // 预览 + 手势容器（与 PreviewView 同尺寸，保证点击坐标与焦点框对齐）
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier
                                .fillMaxSize()
                                // 双指捏合变焦：手势开始时记录基准倍率，此后 target = 基准 × 手势缩放，
                                // 避免用实时倍率相乘导致只能放大不能缩小
                                .pointerInput(camera) {
                                    var baseRatio = 0f
                                    detectTransformGestures { _, _, zoom, _ ->
                                        val st = camera?.cameraInfo?.zoomState?.value
                                            ?: return@detectTransformGestures
                                        if (baseRatio <= 0f) baseRatio = st.zoomRatio
                                        val target =
                                            (baseRatio * zoom).coerceIn(st.minZoomRatio, st.maxZoomRatio)
                                        camera?.cameraControl?.setZoomRatio(target)
                                        zoomRatio = target
                                    }
                                }
                                // 单击对焦 + 测光（CameraX FocusMeteringAction，等价 OpenCamera 的 AF/AE REGIONS）
                                .pointerInput(camera) {
                                    detectTapGestures { offset ->
                                        val point = previewView.meteringPointFactory
                                            .createPoint(offset.x, offset.y)
                                        val action = FocusMeteringAction.Builder(
                                            point,
                                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                        )
                                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                            .build()
                                        camera?.cameraControl?.startFocusAndMetering(action)
                                        focusPoint = offset
                                    }
                                }
                        )
                        // 对焦指示框（相对容器左上角绝对定位，与点击点对齐）
                        focusPoint?.let { p ->
                            FocusIndicator(
                                progress = focusAnim.value,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset {
                                        IntOffset(
                                            p.x.roundToInt() - 22,
                                            p.y.roundToInt() - 22
                                        )
                                    }
                            )
                        }
                    }
                    // 预览框底部正中心变焦拉杆：右半区持续放大 / 左半区持续缩小，偏移越大速度越快
                    val zs = camera?.cameraInfo?.zoomState?.value
                    if (zs != null && zs.maxZoomRatio > zs.minZoomRatio) {
                        ZoomLever(
                            backdrop = cameraBackdrop,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                            minRatio = zs.minZoomRatio,
                            maxRatio = zs.maxZoomRatio,
                            getCurrentRatio = {
                                camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                            },
                            setRatio = { r -> camera?.cameraControl?.setZoomRatio(r) }
                        )
                    }
                }

                // ===== 底部快门区 76-100%（中线 88%）：缩略图 + 快门居中 =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(24f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            val latest = remember(currentSubjectId, lastPhoto?.id) {
                                NoteStore.getPhotos(currentSubjectId).firstOrNull()
                            }
                            if (latest != null) {
                                val bmp by produceState<ImageBitmap?>(initialValue = null, latest.fileName) {
                                    value = withContext(Dispatchers.IO) {
                                        val f = NoteStore.photoFile(latest)
                                        if (f.exists()) ThumbnailStore.get(f.absolutePath) else null
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(43.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .noRippleClickable { openSystemViewer(context, latest) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val img = bmp
                                    if (img != null) {
                                        Image(
                                            bitmap = img,
                                            contentDescription = "该科目最新照片",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    "暂无照片",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // 中：双圆快门（底层大圆玻璃质感 + 上层小圆纯白），拍摄中禁用
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                .noRippleClickable(onClick = { takePhoto() }, enabled = !taking),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .background(Color.White, CircleShape)
                            )
                            if (taking) {
                                Box(
                                    Modifier
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                )
                            }
                        }

                        // 右：切换后置镜头（主摄/广角/长焦轮换，显示当前倍率，与左侧缩略图对称）
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                            Box(
                                modifier = Modifier
                                    .size(43.dp)
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .noRippleClickable(onClick = { switchLens() }),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    backLenses.getOrNull(lensIndex)?.label ?: "WIDE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 快门快闪层（最上层，不拦截点击）
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = flashAlpha.value))
            )
        } else {
            // 无权限引导
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "需要相机权限才能拍摄课件笔记",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "照片仅保存在应用内，不会上传",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .noRippleClickable(onClick = onDismiss)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("返回", fontSize = 14.sp, color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White, CircleShape)
                            .noRippleClickable(onClick = { requestPermission() })
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "授予权限",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }

    // 科目切换弹窗（复用主界面弹窗动画：淡入 + 弹性缩放；真液态玻璃折射相机页自身背景，文字随主题亮/暗反色）
    GlassPopup(showSubjectPicker) {
        GlassDialog(
            backdrop = cameraBackdrop,
            onDismiss = { showSubjectPicker = false },
            title = "选择科目",
            dismissText = "关闭",
            glassText = true,
            glassTextDark = dark
        ) {
            Column(Modifier.fillMaxWidth()) {
                CameraSubjectRow("未分类", currentSubjectId == UNCATEGORIZED_ID, dark) {
                    currentSubjectId = UNCATEGORIZED_ID
                    showSubjectPicker = false
                }
                subjects.forEach { s ->
                    CameraSubjectRow(s.name, currentSubjectId == s.id, dark) {
                        currentSubjectId = s.id
                        showSubjectPicker = false
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "新建科目 ›",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleClickable {
                            showSubjectPicker = false
                            onNewSubject()
                        }
                )
            }
        }
    }
}

/**
 * 变焦拉杆（油门语义）：拉杆偏移只决定"方向 + 速率"，不决定倍率位置。
 * 右半区持续放大、左半区持续缩小，偏移越大速率越快；中心附近为死区（停止）。
 * 松手停止变焦（倍率保持），仅圆点回弹到中心。
 * 拖杆圆点为液态玻璃：按压透镜折射 + 放大 + 高光内外阴影（与主页 Dock 滑块同款管线）。
 */
@Composable
private fun ZoomLever(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    minRatio: Float,
    maxRatio: Float,
    getCurrentRatio: () -> Float,
    setRatio: (Float) -> Unit
) {
    val density = LocalDensity.current
    val maxOffsetPx = with(density) { 56.dp.toPx() }
    val leverOffset = remember { Animatable(0f) }
    val ratio = remember { Animatable(1f) }
    // 按压液态动画：按下放大 + 透镜折射渐进
    val pressProgress = remember { Animatable(0f) }
    val pressScale = remember { Animatable(1f) }
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    // 按偏移更新变焦速率（取消旧任务、按新速率循环推进倍率）
    fun updateRate(offsetPx: Float) {
        zoomJob?.cancel()
        val k = (kotlin.math.abs(offsetPx) / maxOffsetPx).coerceIn(0f, 1f)
        if (k < 0.06f) return // 死区：中心附近不动作
        val rate = ((k - 0.06f) / 0.94f).let { it * it } // 平方曲线：近中心慢、远端快
        val dir = if (offsetPx >= 0f) 1f else -1f
        zoomJob = scope.launch {
            while (isActive) {
                val next = (ratio.value * (1f + dir * rate * 0.035f))
                    .coerceIn(minRatio, maxRatio)
                ratio.snapTo(next)
                setRatio(next)
                delay(16)
            }
        }
    }

    fun pressIn() {
        scope.launch {
            launch { pressProgress.animateTo(1f, tween(120)) }
            launch { pressScale.animateTo(1.4f, tween(120)) }
        }
    }

    fun pressOut() {
        scope.launch {
            launch { pressProgress.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)) }
            launch { pressScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)) }
        }
    }

    Box(
        modifier = modifier
            .width(140.dp)
            .height(30.dp)
            // 液态玻璃胶囊：半透明白 + 细白描边（贴合原软件 UI）
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(15.dp))
            .border(0.8.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(15.dp))
            .pointerInput(maxOffsetPx) {
                detectDragGestures(
                    onDragStart = {
                        pressIn()
                        // 以当前真实倍率为基准继续变焦（兼容捏合/外部修改）
                        scope.launch {
                            ratio.snapTo(getCurrentRatio().coerceIn(minRatio, maxRatio))
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            leverOffset.snapTo(
                                (leverOffset.value + dragAmount.x).coerceIn(-maxOffsetPx, maxOffsetPx)
                            )
                        }
                        updateRate(leverOffset.value)
                    },
                    onDragEnd = {
                        zoomJob?.cancel()
                        pressOut()
                        scope.launch {
                            leverOffset.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
                        }
                    },
                    onDragCancel = {
                        zoomJob?.cancel()
                        pressOut()
                        scope.launch {
                            leverOffset.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 轨道（胶囊内细线）
        Box(
            Modifier
                .fillMaxWidth(0.72f)
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.3f), CircleShape)
        )
        // 拖杆圆点：液态玻璃（折射 + 按压动画 + 高光阴影）
        Box(
            Modifier
                .align(Alignment.Center)
                .offset { IntOffset(leverOffset.value.roundToInt(), 0) }
                .size(18.dp)
                .graphicsLayer {
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        val p = pressProgress.value
                        // 静止：磨砂模糊；按压：液态透镜折射（含色散）
                        blur(10f.dp.toPx() * (1f - p))
                        lens(
                            8f.dp.toPx() * p,
                            12f.dp.toPx() * p,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = pressProgress.value)
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.15f)
                        )
                    },
                    innerShadow = {
                        val p = pressProgress.value
                        InnerShadow(radius = 4f.dp * p, alpha = p)
                    },
                    onDrawSurface = {
                        val p = pressProgress.value
                        // 静止时白色罩面；按压时透出玻璃折射
                        drawRect(Color.White.copy(alpha = 1f - p))
                    }
                )
        )
    }
}

/** 后置镜头类型（按 35mm 等效焦距分类，专业缩写标签） */
private enum class LensType(val label: String) {
    ULTRA("UW"),   // 超广角 UltraWide，等效焦距 < 24mm
    WIDE("WIDE"),  // 主摄 Wide，等效焦距 24-35mm
    TELE("TELE")   // 长焦 Telephoto，等效焦距 > 35mm
}

/** 单个后置镜头：cameraId + 类型标签 */
private data class BackLens(val id: String, val type: LensType, val label: String)

/** 35mm 胶片帧（用于换算等效焦距） */
private val Size35mm = Size(36, 24)

/** 枚举全部后置镜头并识别类型（35mm 等效焦距法，方案参考 CamerAwesome getSensorType） */
private fun buildBackLenses(provider: ProcessCameraProvider): List<BackLens> {
    val backInfos = provider.availableCameraInfos.filter { info ->
        runCatching {
            Camera2CameraInfo.from(info)
                .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
        }.getOrDefault(false)
    }
    return backInfos.map { info ->
        val id = Camera2CameraInfo.from(info).cameraId
        val type = classifyLens(info)
        BackLens(id, type, type.label)
    }
}

/** 按镜头焦距数据分类（参考 CamerAwesome：telephoto > wide > ultrawide 优先级） */
private fun classifyLens(info: CameraInfo): LensType {
    val c2 = Camera2CameraInfo.from(info)
    val focalLengths = c2.getCameraCharacteristic(
        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
    ) ?: return LensType.WIDE
    val sensorSize = c2.getCameraCharacteristic(
        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
    ) ?: return LensType.WIDE
    val cropFactor = kotlin.math.max(Size35mm.width, Size35mm.height).toFloat() /
        kotlin.math.max(sensorSize.width, sensorSize.height).toFloat()
    val hasTele = focalLengths.any { it * cropFactor > 35f }
    val hasWide = focalLengths.any { it * cropFactor in 24f..35f }
    val hasUltra = focalLengths.any { it * cropFactor < 24f }
    return when {
        hasTele -> LensType.TELE
        hasWide -> LensType.WIDE
        hasUltra -> LensType.ULTRA
        else -> LensType.WIDE
    }
}

/** 对焦指示框：从点击位置放大进入，后段淡出（参考 OpenCamera FocusIndicatorView） */
@Composable
private fun FocusIndicator(progress: Float, modifier: Modifier = Modifier) {
    val scale = 1.25f - 0.25f * progress
    val alpha = if (progress < 0.7f) 0.9f else 0.9f * (1f - (progress - 0.7f) / 0.3f)
    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha.coerceIn(0f, 1f)
            }
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
                shape = RoundedCornerShape(6.dp)
            )
    )
}

@Composable
private fun CameraSubjectRow(name: String, active: Boolean, dark: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            // 玻璃弹窗内：选中项主题色底 + 白字，未选中白 6% 底（底色不随主题变化）
            .background(
                if (active) primary.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(10.dp)
            )
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            // 文字颜色随主题：暗色白字 / 亮色黑字
            color = if (dark) Color.White else Color(0xFF111111),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
