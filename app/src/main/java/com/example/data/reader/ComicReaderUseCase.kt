package com.example.data.reader

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ComicReaderUseCase(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "comic_cache").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Extracts the comic archive into a local cache directory and returns a sorted list of image File paths.
     */
    suspend fun extractComic(uri: Uri, isCbr: Boolean): List<File> = withContext(Dispatchers.IO) {
        // Clear previous cache to save space
        clearCache()

        val imageFiles = mutableListOf<File>()

        try {
            if (isCbr) {
                // junrar requires a File, not an InputStream.
                // We must copy the content URI to a temporary file first if it's from SAF.
                val tempRar = copyUriToTempFile(uri, "temp.cbr")
                if (tempRar != null) {
                    val archive = Archive(tempRar)
                    var fileHeader: FileHeader? = archive.nextFileHeader()
                    while (fileHeader != null) {
                        if (!fileHeader.isDirectory && isImageFile(fileHeader.fileName)) {
                            val outFile = File(cacheDir, sanitizeFilename(fileHeader.fileName))
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                archive.extractFile(fileHeader, fos)
                            }
                            imageFiles.add(outFile)
                        }
                        fileHeader = archive.nextFileHeader()
                    }
                    archive.close()
                    tempRar.delete()
                }
            } else {
                // CBZ (ZIP) extraction using standard Java library
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var zipEntry: ZipEntry? = zis.nextEntry
                        while (zipEntry != null) {
                            if (!zipEntry.isDirectory && isImageFile(zipEntry.name)) {
                                val outFile = File(cacheDir, sanitizeFilename(zipEntry.name))
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                                imageFiles.add(outFile)
                            }
                            zis.closeEntry()
                            zipEntry = zis.nextEntry
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort naturally by filename
        return@withContext imageFiles.sortedBy { it.name }
    }

    private fun copyUriToTempFile(uri: Uri, tempName: String): File? {
        return try {
            val tempFile = File(context.cacheDir, tempName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isImageFile(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp")
    }

    private fun sanitizeFilename(filename: String): String {
        return filename.replace("/", "_").replace("\\", "_")
    }

    fun clearCache() {
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }
}
