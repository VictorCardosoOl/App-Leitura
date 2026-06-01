package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.BookRepository
import com.example.data.SettingsRepository
import com.example.ui.OmniApp
import com.example.ui.ReaderViewModel
import com.example.ui.ReaderViewModelFactory
import com.example.ui.theme.OmniReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "omnireader_db"
        ).build()
        val bookRepo = BookRepository(database.bookDao())
        val settingsRepo = SettingsRepository(applicationContext)

        val factory = ReaderViewModelFactory(application, bookRepo, settingsRepo)
        val viewModel = ViewModelProvider(this, factory)[ReaderViewModel::class.java]

        setContent {
            val isDarkModePref by viewModel.isDarkMode.collectAsState()
            val useDarkTheme = isDarkModePref ?: isSystemInDarkTheme()

            OmniReaderTheme(darkTheme = useDarkTheme) {
                OmniApp(viewModel)
            }
        }
    }
}
