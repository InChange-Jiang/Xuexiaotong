package com.xuexiaotong.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule

/**
 * 无涟漪点击：取消 Material ripple 点击阴影，改为轻按缩放反馈
 */
@Composable
fun Modifier.noRippleClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * 自定义玻璃弹窗容器（替代 AlertDialog）
 * 半透明遮罩 + 液态玻璃卡片
 */
/**
 * 弹窗进出动画容器：淡入 + 弹性缩放弹出，淡出 + 收缩（对应 Glass 版 overlayPop）。
 * 背景遮罩为独立固定全屏层（不随弹窗缩放/位移），打开时从完全透明渐暗到 0.34，关闭反向渐出
 */
@Composable
fun GlassPopup(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val transition = updateTransition(visible, label = "popup")
    val maskAlpha by transition.animateFloat(
        transitionSpec = { tween(240) },
        label = "maskAlpha"
    ) { if (it) 0.34f else 0f }

    // 固定层：遮罩永远全屏静止，不参与下方弹窗的缩放/位移动画
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = maskAlpha))
        )
        // 弹窗内容（含各自透明点击遮罩）在固定遮罩之上做缩放动画
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(140)) + scaleIn(
                initialScale = 0.90f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(140)) + scaleOut(
                targetScale = 0.90f,
                animationSpec = tween(140)
            )
        ) {
            content()
        }
    }
}

@Composable
fun GlassDialog(
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    title: String,
    confirmText: String? = null,
    dismissText: String? = null,
    middleText: String? = null,
    onConfirm: (() -> Unit)? = null,
    onMiddle: (() -> Unit)? = null,
    // 卡片不透明度：backdrop 折射染色 / 无背景降级底色（提高可读性）
    cardTintAlpha: Float? = null,
    cardFallbackAlpha: Float? = null,
    // 深色弹窗：不透明黑底 + 白字（相机等黑色沉浸页面）
    dark: Boolean = false,
    // 浅色实底弹窗：近似不透明白玻璃卡 + 黑字（无 backdrop 时复刻主页玻璃弹窗观感）
    lightSurface: Boolean = false,
    // 真液态玻璃弹窗 + 白字：折射 backdrop 玻璃质感，黑背景页面（相机）使用
    glassText: Boolean = false,
    // 玻璃弹窗文字模式：true=白字（暗色主题）/ false=黑字（亮色主题）
    glassTextDark: Boolean = true,
    content: @Composable (BoxScope.() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 透明遮罩：仅保留点击空白关闭，不做全屏压暗（阴影只来自卡片高斯模糊）
            .background(Color.Transparent)
            .noRippleClickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // 与登录卡片同款液态玻璃逻辑：glassCard 折射 backdrop + 硬边高光 + 高斯软阴影
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .gaussianShadow(blur = 11.dp, offsetY = 2.5.dp)
                .then(
                    when {
                        dark -> {
                            // 深色模式：不透明近黑底，贴合相机黑色沉浸背景
                            Modifier.background(Color(0xFF17171A), RoundedCornerShape(20.dp))
                        }
                        lightSurface -> {
                            // 浅色实底：近白玻璃卡（复刻主页玻璃弹窗观感）
                            Modifier.background(Color(0xFFF7F7F9), RoundedCornerShape(20.dp))
                        }
                        glassText -> {
                            // 真液态玻璃：折射 backdrop（模糊 + 透镜），与主页弹窗同质感，染色略提以在黑背景上显现
                            Modifier.glassCard(
                                backdrop = backdrop,
                                radius = 20.dp,
                                blurRadius = 16.dp,
                                tintAlpha = 0.22f
                            )
                        }
                        else -> {
                            Modifier.glassCard(
                                backdrop = backdrop,
                                radius = 20.dp,
                                blurRadius = 16.dp,
                                tintAlpha = cardTintAlpha ?: 0.16f,
                                fallbackAlpha = cardFallbackAlpha ?: 0.38f
                            )
                        }
                    }
                )
                .noRippleClickable(onClick = {}),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 主标题：居左，超长省略号截断
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    dark -> Color.White
                    glassText -> if (glassTextDark) Color.White else Color(0xFF111111)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp)
            )
            if (content != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 10.dp,
                            // 无按钮弹窗：底部与标题顶部对齐（18dp），保持上下边距对称
                            bottom = if (dismissText == null && confirmText == null) 18.dp else 4.dp
                        )
                ) { content() }
            }
            // 按钮行：仅当存在按钮时渲染（纯信息弹窗点击外部即可关闭）
            if (dismissText != null || confirmText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissText != null) {
                        GlassDialogButton(backdrop, dismissText, isPrimary = false, dark = dark, lightSurface = lightSurface, glassText = glassText, glassTextDark = glassTextDark) { onDismiss() }
                    }
                    if (middleText != null) {
                        GlassDialogButton(backdrop, middleText, isPrimary = false, dark = dark, lightSurface = lightSurface, glassText = glassText, glassTextDark = glassTextDark) {
                            onMiddle?.invoke() ?: onDismiss()
                        }
                    }
                    if (confirmText != null) {
                        GlassDialogButton(backdrop, confirmText, isPrimary = true, dark = dark, lightSurface = lightSurface, glassText = glassText, glassTextDark = glassTextDark) {
                            onConfirm?.invoke() ?: onDismiss()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 液态玻璃胶囊按钮：全圆角、低厚度、铺满所在行剩余宽度（等分），
 * 主按钮带主题色淡底
 */
@Composable
fun RowScope.GlassDialogButton(
    backdrop: Backdrop?,
    text: String,
    isPrimary: Boolean,
    dark: Boolean = false,
    lightSurface: Boolean = false,
    glassText: Boolean = false,
    glassTextDark: Boolean = true,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "btn")

    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .scale(scale)
            .then(
                when {
                    dark || glassText -> {
                        // 深色/玻璃白字弹窗按钮：实底胶囊（主按钮主题色 / 次按钮白 8%）
                        Modifier.background(
                            if (isPrimary) primary else Color.White.copy(alpha = 0.08f),
                            Capsule()
                        )
                    }
                    lightSurface -> {
                        // 浅色实底弹窗按钮：复刻主页玻璃按钮观感（主按钮主题淡底 / 次按钮文字样式）
                        if (isPrimary) {
                            Modifier.background(primary.copy(alpha = 0.14f), Capsule())
                        } else {
                            Modifier
                        }
                    }
                    else -> {
                        Modifier.glassPill(backdrop, blurRadius = 8.dp, tintAlpha = if (isPrimary) 0.14f else 0.07f)
                            .then(
                                if (isPrimary) Modifier.background(primary.copy(alpha = 0.22f), Capsule())
                                else Modifier
                            )
                    }
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                dark -> if (isPrimary) Color.White else Color.White.copy(alpha = 0.85f)
                glassText -> if (glassTextDark) {
                    if (isPrimary) Color.White else Color.White.copy(alpha = 0.85f)
                } else {
                    if (isPrimary) MaterialTheme.colorScheme.primary else Color(0xFF111111)
                }
                else -> if (isPrimary) primary else MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * 玻璃 Toast（轻提示）：保留玻璃染色质感（不透明度加倍）+ 全圆角 + 灰字
 */
@Composable
fun GlassToast(message: String, backdrop: Backdrop? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 120.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .glassPill(backdrop, blurRadius = 10.dp, withLens = false, tintAlpha = 0.20f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(message, fontSize = 13.sp, color = Color(0xFF808080))
        }
    }
}

/**
 * 自定义确认弹窗（替代确认型 AlertDialog）
 */
@Composable
fun GlassConfirmDialog(
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "确定",
    onConfirm: () -> Unit
) {
    GlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        title = title,
        confirmText = confirmText,
        dismissText = "取消",
        onConfirm = onConfirm
    ) {
        Text(
            message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
