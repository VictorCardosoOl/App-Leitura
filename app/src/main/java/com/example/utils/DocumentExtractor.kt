package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.github.junrar.Archive
import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object DocumentExtractor {

    fun extractCover(context: Context, uri: Uri, type: String): String? {
        return try {
            val bitmap = when (type) {
                "EPUB" -> extractEpubCover(context, uri)
                "PDF" -> extractPdfCover(context, uri)
                "COMIC" -> extractComicCover(context, uri)
                else -> null
            }

            bitmap?.let {
                val cachePath = File(context.cacheDir, "covers")
                cachePath.mkdirs()
                val file = File(cachePath, "${uri.lastPathSegment ?: System.currentTimeMillis()}.jpg")
                val out = FileOutputStream(file)
                it.compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.flush()
                out.close()
                file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractEpubCover(context: Context, uri: Uri): Bitmap? {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val book = EpubReader().readEpub(stream)
            val coverImage = book.coverImage
            if (coverImage != null) {
                return BitmapFactory.decodeStream(coverImage.inputStream)
            }
        }
        return null
    }

    private fun extractPdfCover(context: Context, uri: Uri): Bitmap? {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val pdfRenderer = PdfRenderer(pfd)
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pdfRenderer.close()
                return bitmap
            }
        }
        return null
    }

    private fun extractComicCover(context: Context, uri: Uri): Bitmap? {
        // Try ZIP (CBZ) first, then RAR (CBR)
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val zis = ZipInputStream(stream)
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && isImageFile(entry.name)) {
                        return BitmapFactory.decodeStream(zis)
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // If ZIP fails, try RAR
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val archive = Archive(ParcelFileDescriptor.AutoCloseInputStream(pfd))
                val fileHeaders = archive.fileHeaders
                val imageHeader = fileHeaders.firstOrNull { !it.isDirectory && isImageFile(it.fileName) }
                if (imageHeader != null) {
                    val out = java.io.ByteArrayOutputStream()
                    archive.extractFile(imageHeader, out)
                    val bytes = out.toByteArray()
                    archive.close()
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                archive.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
               lower.endsWith(".png") || lower.endsWith(".webp")
    }

    // Used for Reading EPUB
    fun getEpubChapterHtml(context: Context, uri: Uri, chapterIndex: Int): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val book = EpubReader().readEpub(stream)
                val spine = book.spine
                if (chapterIndex >= 0 && chapterIndex < spine.size()) {
                    val resource = spine.getResource(chapterIndex)
                    String(resource.data) // Return HTML content
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getEpubChapterCount(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val book = EpubReader().readEpub(stream)
                book.spine.size()
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // Used for Reading Comics
    fun getComicPage(context: Context, uri: Uri, pageIndex: Int): Bitmap? {
        // Zip (CBZ)
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val zis = ZipInputStream(stream)
                var entry = zis.nextEntry
                var currentIndex = 0
                while (entry != null) {
                    if (!entry.isDirectory && isImageFile(entry.name)) {
                        if (currentIndex == pageIndex) {
                            return BitmapFactory.decodeStream(zis)
                        }
                        currentIndex++
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // RAR (CBR)
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val archive = Archive(ParcelFileDescriptor.AutoCloseInputStream(pfd))
                val imageHeaders = archive.fileHeaders.filter { !it.isDirectory && isImageFile(it.fileName) }
                if (pageIndex >= 0 && pageIndex < imageHeaders.size) {
                    val out = java.io.ByteArrayOutputStream()
                    archive.extractFile(imageHeaders[pageIndex], out)
                    val bytes = out.toByteArray()
                    archive.close()
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                archive.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getComicPageCount(context: Context, uri: Uri): Int {
        var count = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val zis = ZipInputStream(stream)
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && isImageFile(entry.name)) count++
                    entry = zis.nextEntry
                }
                if (count > 0) return count
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val archive = Archive(ParcelFileDescriptor.AutoCloseInputStream(pfd))
                count = archive.fileHeaders.count { !it.isDirectory && isImageFile(it.fileName) }
                archive.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
}
