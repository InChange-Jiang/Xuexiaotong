package com.xuexiaotong.data

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * 学习笔记存储：科目 + 照片元数据
 * 元数据以 JSON 文件形式存放于应用私有目录（便于测试导入与批量维护），
 * 照片文件按科目目录组织，写入无需任何权限。
 */
object NoteStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private lateinit var notesDir: File
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        notesDir = File(context.filesDir, "notes").apply { mkdirs() }
    }

    /* ---------------- 内部工具 ---------------- */

    private val subjectsFile: File get() = File(notesDir, "note_subjects.json")
    private val photosFile: File get() = File(notesDir, "note_photos.json")

    private fun readSubjects(): List<NoteSubject> =
        try {
            if (subjectsFile.exists()) json.decodeFromString<List<NoteSubject>>(subjectsFile.readText().removePrefix("\uFEFF")) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

    private fun readPhotos(): List<NotePhoto> =
        try {
            if (photosFile.exists()) json.decodeFromString<List<NotePhoto>>(photosFile.readText().removePrefix("\uFEFF")) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

    /** 科目目录（按科目 id 建目录，未分类使用固定目录） */
    fun subjectDir(subjectId: String): File =
        File(notesDir, if (subjectId == UNCATEGORIZED_ID) UNCATEGORIZED_ID else subjectId).apply { mkdirs() }

    fun photoFile(photo: NotePhoto): File = File(subjectDir(photo.subjectId), photo.fileName)

    /* ---------------- 科目 ---------------- */

    fun getSubjects(): List<NoteSubject> = readSubjects().sortedBy { it.order }

    private fun saveSubjects(list: List<NoteSubject>) =
        subjectsFile.writeText(json.encodeToString(list))

    /** 新增科目；同名（忽略首尾空格）返回 null */
    fun addSubject(name: String): NoteSubject? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        if (getSubjects().any { it.name == trimmed }) return null
        val list = getSubjects()
        val subject = NoteSubject(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            order = (list.maxOfOrNull { it.order } ?: 0) + 1,
            createdAt = System.currentTimeMillis()
        )
        saveSubjects(list + subject)
        return subject
    }

    /** 重命名科目；同名返回 false */
    fun renameSubject(id: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        val list = getSubjects()
        if (list.any { it.id != id && it.name == trimmed }) return false
        saveSubjects(list.map { if (it.id == id) it.copy(name = trimmed) else it })
        return true
    }

    /** 删除科目：其下照片全部移入未分类，科目移除 */
    fun deleteSubject(id: String) {
        saveSubjects(getSubjects().filter { it.id != id })
        val photos = getPhotos().map {
            if (it.subjectId == id) it.copy(subjectId = UNCATEGORIZED_ID) else it
        }
        savePhotos(photos)
    }

    /* ---------------- 照片 ---------------- */

    fun getPhotos(): List<NotePhoto> = readPhotos().sortedByDescending { it.takenAt }

    private fun savePhotos(list: List<NotePhoto>) =
        photosFile.writeText(json.encodeToString(list))

    fun getPhotos(subjectId: String?): List<NotePhoto> =
        if (subjectId == null) getPhotos() else getPhotos().filter { it.subjectId == subjectId }

    /** 复制外部图片到科目目录并登记元数据；成功返回 NotePhoto */
    fun importPhoto(subjectId: String, srcFile: File): NotePhoto? {
        if (!srcFile.exists()) return null
        val dir = subjectDir(subjectId)
        val fileName = "${UUID.randomUUID().toString().take(8)}.jpg"
        val dst = File(dir, fileName)
        return try {
            srcFile.copyTo(dst, overwrite = true)
            val photo = NotePhoto(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                fileName = fileName,
                takenAt = srcFile.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis(),
                sizeBytes = dst.length()
            )
            savePhotos(getPhotos() + photo)
            photo
        } catch (e: Exception) {
            null
        }
    }

    /** 拍摄照片直接入库：文件已写入科目目录，此处仅登记元数据 */
    fun addCapturedPhoto(subjectId: String, file: File): NotePhoto? {
        if (!file.exists()) return null
        val photo = NotePhoto(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            fileName = file.name,
            takenAt = System.currentTimeMillis(),
            sizeBytes = file.length()
        )
        savePhotos(getPhotos() + photo)
        return photo
    }

    /** 删除照片（文件 + 元数据），返回实际删除数 */
    fun deletePhotos(ids: Set<String>): Int {
        val photos = getPhotos()
        val toDelete = photos.filter { it.id in ids }
        toDelete.forEach { runCatching { photoFile(it).delete() } }
        savePhotos(photos.filter { it.id !in ids })
        return toDelete.size
    }

    /** 移动照片到目标科目（物理移动文件 + 更新元数据），返回实际移动数 */
    fun movePhotos(ids: Set<String>, targetSubjectId: String): Int {
        val photos = getPhotos()
        val toMove = photos.filter { it.id in ids && it.subjectId != targetSubjectId }
        toMove.forEach { p ->
            runCatching {
                val src = photoFile(p)
                if (src.exists()) {
                    val dst = File(subjectDir(targetSubjectId), p.fileName)
                    src.copyTo(dst, overwrite = true)
                    src.delete()
                }
            }
        }
        val movedIds = toMove.map { it.id }.toSet()
        savePhotos(
            photos.map { if (it.id in movedIds) it.copy(subjectId = targetSubjectId) else it }
        )
        return movedIds.size
    }

    /** 导出选中照片到系统相册（MediaStore，目录 Pictures/Xuexiaotong），返回成功数 */
    fun exportToGallery(ids: Set<String>): Int {
        var ok = 0
        getPhotos().filter { it.id in ids }.forEach { p ->
            val file = photoFile(p)
            if (!file.exists()) return@forEach
            runCatching {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, p.fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Xuexiaotong")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = appContext.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@forEach
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                ok++
            }
        }
        return ok
    }

    /** 科目下照片数量（相册空态提示用） */
    fun countPhotos(subjectId: String?): Int =
        if (subjectId == null) getPhotos().size else getPhotos().count { it.subjectId == subjectId }
}
