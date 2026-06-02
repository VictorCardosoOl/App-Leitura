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

import com.example.domain.AddDocumentUseCase
import kotlinx.coroutines.flow.combine

data class AppUiState(
    val books: List<BookEntity> = emptyList(),
    val isRtl: Boolean = false,
    val isScrollMode: Boolean = false
)

class ReaderViewModel(
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository,
    private val addDocumentUseCase: AddDocumentUseCase
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = combine(
        bookRepository.allBooks,
        settingsRepository.isRtlFlow,
        settingsRepository.isScrollModeFlow
    ) { books, isRtl, isScrollMode ->
        AppUiState(books = books, isRtl = isRtl, isScrollMode = isScrollMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppUiState())

    init {
        // Seed some mock data if empty.
        viewModelScope.launch {
            val books = bookRepository.allBooks.first()
            if (books.isEmpty()) {
                bookRepository.insert(BookEntity(title = "Attack on Titan Vol. 1", type = "COMIC", coverUrl = "https://images.unsplash.com/photo-1612480797665-c96d261ae092?auto=format&fit=crop&q=80&w=400&h=600", filePath = "", isSynced = true))
                bookRepository.insert(BookEntity(title = "The Great Gatsby", type = "EPUB", coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400&h=600", filePath = "", isSynced = true, progress = 0.45f))
                bookRepository.insert(BookEntity(title = "Android Architecture", type = "PDF", coverUrl = "https://images.unsplash.com/photo-1512820200508-8df04dbf5619?auto=format&fit=crop&q=80&w=400&h=600", filePath = "", isSynced = false))
            }
        }
    }

    fun toggleRtlMode(isRtl: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRtlMode(isRtl)
        }
    }
    
    fun toggleScrollMode(isScroll: Boolean) {
        viewModelScope.launch {
            settingsRepository.setScrollMode(isScroll)
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
            addDocumentUseCase(uri, title)
        }
    }
}


