package com.example.domain

import android.content.Context
import android.net.Uri
import com.example.data.BookEntity
import com.example.data.BookRepository
import com.example.utils.DocumentExtractor

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
        
        // Extract real cover path
        val coverPath = DocumentExtractor.extractCover(context, uri, type) 
            ?: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400&h=600"
        
        bookRepository.insert(
            BookEntity(
                title = title,
                type = type,
                coverUrl = coverPath,
                filePath = uri.toString(),
                isSynced = false,
                progress = 0f
            )
        )
    }
}
