package com.xuexiaotong.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.xuexiaotong.data.ThemeColor
import com.xuexiaotong.data.Themes

/** 解析 #RRGGBB → Color */
fun parseHex(hex: String): Color {
    return try {
        val h = hex.removePrefix("#")
        val r = h.substring(0, 2).toInt(16) / 255f
        val g = h.substring(2, 4).toInt(16) / 255f
        val b = h.substring(4, 6).toInt(16) / 255f
        Color(r, g, b)
    } catch (e: Exception) {
        Color(0xFF4B3FE3)
    }
}

/**
 * 学小通糖果色主题
 */
@Composable
fun XuexiaotongTheme(
    theme: ThemeColor = Themes.byId("coral"),
    dark: Boolean = false,
    content: @Composable () -> Unit
) {
    val primary = parseHex(if (dark) theme.darkPrimary else theme.lightPrimary)
    val primaryLight = parseHex(if (dark) theme.darkPrimaryLight else theme.lightPrimaryLight)
    val primaryBg = parseHex(if (dark) theme.darkPrimaryBg else theme.lightPrimaryBg)
    val surface = if (dark) Color(0xFF1D1F24) else Color(0xFFFFFFFF)

    val colorScheme = if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryBg,
            onPrimaryContainer = primary,
            secondary = primaryLight,
            background = Color(0xFF121316),
            onBackground = Color(0xFFE8EAED),
            surface = surface,
            onSurface = Color(0xFFE8EAED),
            surfaceVariant = Color(0x8A1D1F24),
            onSurfaceVariant = Color(0xFF9AA0A8),
            outline = Color(0x1AFFFFFF),
            error = Color(0xFFE8463A)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryBg,
            onPrimaryContainer = primary,
            secondary = primaryLight,
            background = Color(0xFFF5F6FA),
            onBackground = Color(0xFF1F2329),
            surface = surface,
            onSurface = Color(0xFF1F2329),
            surfaceVariant = Color(0x80FFFFFF),
            onSurfaceVariant = Color(0xFF8A919F),
            outline = Color(0x141F2329),
            error = Color(0xFFE8463A)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
