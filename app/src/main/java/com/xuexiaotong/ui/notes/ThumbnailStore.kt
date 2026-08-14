package com.xuexiaotong.ui.notes

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * 相册缩略图加载器：采样解码（最长边约 256px）+ LruCache 内存缓存。
 * 核心思想借鉴 Luban：先按比例采样（inSampleSize）再解码，内存占用比全尺寸解码低两个数量级；
 * 缓存键含文件修改时间，文件被覆盖时自动失效。应在 IO 线程调用。
 */
object ThumbnailStore {
    private val cache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt() // 约 12.5% 堆内存用于缩略图
    ) {}

    /** 目标缩略图最长边（像素） */
    private const val MAX_DIM = 256

    fun get(path: String): ImageBitmap? {
        val key = "$path|${File(path).lastModified()}"
        cache.get(key)?.let { return it }
        val bmp = decodeScaled(path) ?: return null
        cache.put(key, bmp)
        return bmp
    }

    private fun decodeScaled(path: String): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_DIM || bounds.outHeight / (sample * 2) >= MAX_DIM) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
    }
}
