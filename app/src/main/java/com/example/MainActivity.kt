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
import com.example.ui.theme.OmniReaderTheme

import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: ReaderViewModel = koinViewModel()
            val useDarkTheme = isSystemInDarkTheme()

            OmniReaderTheme(darkTheme = useDarkTheme) {
                OmniApp(viewModel)
            }
        }
    }
}
