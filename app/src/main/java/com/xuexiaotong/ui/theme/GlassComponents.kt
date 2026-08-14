package com.xuexiaotong.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle

/**
 * 高斯模糊软阴影：只在卡片边缘外一定范围内柔和扩散，
 * 替代硬边 shadow（动画时无明显阴影边缘）
 * 默认（菜单栏等）blur=22dp / offsetY=5dp / alpha=0.18；
 * 弹窗等紧凑场景可传入减半参数（blur=11.dp, offsetY=2.5.dp）
 */
fun Modifier.gaussianShadow(
    blur: Dp = 22.dp,
    alpha: Float = 0.18f,
    offsetY: Dp = 5.dp
): Modifier = drawBehind {
    drawIntoCanvas { canvas ->
        val b = blur.toPx()
        val oy = offsetY.toPx()
        val corner = 24.dp.toPx()
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            this.alpha = (255 * alpha).toInt()
            maskFilter = BlurMaskFilter(b, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawRoundRect(
            -b, -b + oy, size.width + b, size.height + b,
            corner, corner, paint
        )
    }
}

/**
 * 液态玻璃效果封装（Liquid Glass Backdrop）
 * 用法：
 *   val backdrop = rememberGlassBackdrop()
 *   Box { 背景内容.layerBackdrop(backdrop); 玻璃组件.drawGlassBackdrop(backdrop, ...) }
 */

@Composable
fun rememberGlassBackdrop(): LayerBackdrop = rememberLayerBackdrop()

/** 把内容录制为玻璃背景层 */
fun Modifier.glassBackground(backdrop: LayerBackdrop): Modifier = this.layerBackdrop(backdrop)

/**
 * 玻璃容器（圆角矩形）
 * @param radius 圆角
 * @param blurRadius 背景模糊半径
 * @param refractionHeight 折射高度（液态效果强度）
 * @param refractionAmount 折射量
 * @param withLens 是否启用透镜折射（Android 13+ 生效，低版本自动降级）
 * @param tintColor 表面染色（玻璃底色）
 */
fun Modifier.glassCard(
    backdrop: Backdrop?,
    radius: Dp = 16.dp,
    blurRadius: Dp = 12.dp,
    refractionHeight: Dp = 10.dp,
    refractionAmount: Dp = 14.dp,
    withLens: Boolean = true,
    tintColor: Color = Color.White,
    tintAlpha: Float = 0.07f,
    fallbackAlpha: Float = 0.38f,
    highlight: (() -> Highlight?)? = { Highlight.Default }
): Modifier {
    if (backdrop == null) {
        // 无背景层（层内内容等）：降级为纯色圆角卡片，底色略低于标题栏视觉厚度
        return this.background(tintColor.copy(alpha = fallbackAlpha), androidx.compose.foundation.shape.RoundedCornerShape(radius))
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedRectangle(radius) },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            if (withLens) {
                lens(
                    refractionHeight = refractionHeight.toPx(),
                    refractionAmount = refractionAmount.toPx(),
                    depthEffect = true
                )
            }
        },
        highlight = highlight,
        onDrawSurface = {
            drawRect(tintColor.copy(alpha = tintAlpha))
        }
    )
}

/**
 * 轻量玻璃（无透镜，性能优先：列表项等大量复用场景）
 * 仅模糊 + 染色，开销远低于 lens
 */
fun Modifier.glassSurface(
    backdrop: Backdrop?,
    radius: Dp = 12.dp,
    blurRadius: Dp = 10.dp,
    tintColor: Color = Color.White,
    tintAlpha: Float = 0.07f
): Modifier {
    if (backdrop == null) {
        return this.background(tintColor.copy(alpha = 0.32f), androidx.compose.foundation.shape.RoundedCornerShape(radius))
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedRectangle(radius) },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
        },
        highlight = { Highlight.Default },
        onDrawSurface = {
            drawRect(tintColor.copy(alpha = tintAlpha))
        }
    )
}

/**
 * 层内玻璃卡片（模拟标题栏液态玻璃视觉，用于 backdrop 录制层内的卡片）：
 * 高斯软阴影 + 上下微差渐变底 + 四边硬边高光描边，避免层内自折射崩溃
 */
fun Modifier.glassSheet(
    tint: Color = Color.White,
    radius: Dp = 16.dp,
    dark: Boolean = false
): Modifier = this
    // 无阴影：避免浅色模式下黑色阴影使卡片发灰
    .background(
        Brush.verticalGradient(
            colors = listOf(
                tint.copy(alpha = if (dark) 0.44f else 0.52f),
                tint.copy(alpha = if (dark) 0.32f else 0.38f)
            )
        ),
        androidx.compose.foundation.shape.RoundedCornerShape(radius)
    )
    .drawBehind {
        // 硬边高光描边（液态玻璃边缘反光）
        val stroke = 1.4.dp.toPx()
        val r = radius.toPx()
        drawRoundRect(
            color = Color.White.copy(alpha = if (dark) 0.16f else 0.42f),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(r, r),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
    }

/**
 * 玻璃胶囊（底部导航等条状控件）
 * @param withLens 是否启用透镜折射；false 时仅模糊+染色，更清透（适合需要透光的条状玻璃）
 */
fun Modifier.glassPill(
    backdrop: Backdrop?,
    blurRadius: Dp = 12.dp,
    refractionHeight: Dp = 12.dp,
    refractionAmount: Dp = 16.dp,
    withLens: Boolean = true,
    tintColor: Color = Color.White,
    tintAlpha: Float = 0.08f
): Modifier {
    if (backdrop == null) {
        return this.background(tintColor.copy(alpha = 0.2f), com.kyant.shapes.Capsule())
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { com.kyant.shapes.Capsule() },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            if (withLens) {
                lens(
                    refractionHeight = refractionHeight.toPx(),
                    refractionAmount = refractionAmount.toPx(),
                    depthEffect = true
                )
            }
        },
        highlight = { Highlight.Default },
        onDrawSurface = {
            drawRect(tintColor.copy(alpha = tintAlpha))
        }
    )
}

/**
 * 玻璃背景容器：全屏渐变 + 内容 + 底部玻璃导航等
 * 将渐变背景与 content 一并录制进 backdrop 层，供上方玻璃组件折射出真实内容（毛玻璃）
 * @param baseColor 底层基础色（白/黑），主题渐变以半透明叠加其上，柔和且保留主题感
 */
@Composable
fun GlassBackdropBox(
    gradientStart: Color,
    gradientEnd: Color,
    modifier: Modifier = Modifier,
    baseColor: Color = Color.White,
    backdrop: LayerBackdrop = rememberGlassBackdrop(),
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit
) {
    Box(modifier.fillMaxSize()) {
        // 背景层：录制基础色 + 主题渐变 + 页面主体内容（作为玻璃的折射源）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassBackground(backdrop)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .background(baseColor)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(gradientStart, gradientEnd)))
            )
            content(backdrop)
        }
    }
}
