package com.example.data

import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBookById(id: Int): Flow<BookEntity?> = bookDao.getBookById(id)

    suspend fun insert(book: BookEntity) = bookDao.insertBook(book)
    
    suspend fun update(book: BookEntity) = bookDao.updateBook(book)

    suspend fun updateProgress(id: Int, progress: Float) = 
        bookDao.updateProgress(id, progress, System.currentTimeMillis())

    suspend fun deleteById(id: Int) = bookDao.deleteBookById(id)
}
