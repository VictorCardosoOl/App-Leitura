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
        // Mock data removed. Only user-imported books will be shown.
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


