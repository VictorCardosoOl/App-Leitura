package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "COMIC", "EPUB", "PDF"
    val coverUrl: String,
    val isSynced: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val lastRead: Long = System.currentTimeMillis()
)

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastRead DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Int): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)
    
    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET progress = :progress, lastRead = :lastRead WHERE id = :id")
    suspend fun updateProgress(id: Int, progress: Float, lastRead: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Int)
}

@Database(entities = [BookEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
