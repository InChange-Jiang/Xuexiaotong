package com.xuexiaotong.data

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-128-CBC 加密，与 uni-app 版 aes.js 完全兼容：
 *  - AES-128 / CBC / PKCS7
 *  - key 与 iv 相同（16 字节字符串的 UTF-8 字节）
 *  - 输出 Base64
 */
object Aes {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

    /** 学习通登录 AES 密钥 */
    const val CX_AES_KEY = "u2oh6Vu^HWe4_AES"

    fun encrypt(message: String, key: String = CX_AES_KEY): String {
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        val keySpec = SecretKeySpec(keyBytes, ALGORITHM)
        val ivSpec = IvParameterSpec(keyBytes) // key == iv

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(message.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }
}
