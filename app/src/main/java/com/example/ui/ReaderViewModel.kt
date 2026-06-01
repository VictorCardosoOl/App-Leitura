package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BookEntity
import com.example.data.BookRepository
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean?> = settingsRepository.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val library: StateFlow<List<BookEntity>> = bookRepository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Seed some mock data if empty.
        viewModelScope.launch {
            val books = bookRepository.allBooks.first()
            if (books.isEmpty()) {
                bookRepository.insert(BookEntity(title = "Attack on Titan Vol. 1", type = "COMIC", coverUrl = "https://images.unsplash.com/photo-1612480797665-c96d261ae092?auto=format&fit=crop&q=80&w=400&h=600", isSynced = true))
                bookRepository.insert(BookEntity(title = "The Great Gatsby", type = "EPUB", coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400&h=600", isSynced = true, progress = 0.45f))
                bookRepository.insert(BookEntity(title = "Android Architecture", type = "PDF", coverUrl = "https://images.unsplash.com/photo-1512820200508-8df04dbf5619?auto=format&fit=crop&q=80&w=400&h=600", isSynced = false))
            }
        }
    }

    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(isDark)
        }
    }

    fun getBook(id: Int): StateFlow<BookEntity?> {
        return bookRepository.getBookById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun updateProgress(id: Int, progress: Float) {
        viewModelScope.launch {
            bookRepository.updateProgress(id, progress)
        }
    }

    fun insertBook(uri: android.net.Uri, title: String) {
        viewModelScope.launch {
            val extension = uri.toString().substringAfterLast('.', "").uppercase()
            val type = when {
                extension.contains("EPUB") -> "EPUB"
                extension.contains("PDF") -> "PDF"
                extension.contains("CBZ") || extension.contains("CBR") -> "COMIC"
                else -> "DOC"
            }
            
            bookRepository.insert(
                BookEntity(
                    title = title,
                    type = type,
                    coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400&h=600", // Default cover
                    isSynced = false,
                    progress = 0f
                )
            )
        }
    }
}

class ReaderViewModelFactory(
    private val bookRepo: BookRepository,
    private val settingsRepo: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReaderViewModel(bookRepo, settingsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
