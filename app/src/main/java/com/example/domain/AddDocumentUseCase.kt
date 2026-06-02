package com.example.domain

import android.content.Context
import android.content.Context
import android.net.Uri
import com.example.data.BookEntity
import com.example.data.BookRepository
import com.example.utils.DocumentExtractor
import java.io.File
import java.io.FileOutputStream

class AddDocumentUseCase(
    private val bookRepository: BookRepository,
    private val context: Context
) {
    suspend operator fun invoke(uri: Uri, title: String) {
        val extension = uri.toString().substringAfterLast('.', "").uppercase()
        val type = when {
            extension.contains("EPUB") -> "EPUB"
            extension.contains("PDF") -> "PDF"
            extension.contains("CBZ") || extension.contains("CBR") -> "COMIC"
            else -> "DOC"
        }
        
        // Copy file to internal storage to avoid Scoped Storage permission issues
        val booksDir = File(context.filesDir, "books")
        booksDir.mkdirs()
        val safeFileName = "${System.currentTimeMillis()}_${title.replace(Regex("[^a-zA-Z0-9.-]"), "_")}"
        val localFile = File(booksDir, safeFileName)
        
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return // Abort if we can't read the original file
        }
        
        val localUri = Uri.fromFile(localFile)

        // Extract real cover path using the local file
        val coverPath = DocumentExtractor.extractCover(context, localUri, type) 
            ?: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400&h=600"
        
        bookRepository.insert(
            BookEntity(
                title = title,
                type = type,
                coverUrl = coverPath,
                filePath = localUri.toString(), // Save local path
                isSynced = false,
                progress = 0f
            )
        )
    }
}
