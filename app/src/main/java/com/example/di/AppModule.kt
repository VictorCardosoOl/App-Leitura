package com.example.di

import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.BookRepository
import com.example.data.SettingsRepository
import com.example.domain.AddDocumentUseCase
import com.example.ui.ReaderViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java, "omnireader_db"
        ).fallbackToDestructiveMigration()
        .build()
    }
    
    single { get<AppDatabase>().bookDao() }
    single { BookRepository(get()) }
    single { SettingsRepository(androidContext()) }
    
    factory { AddDocumentUseCase(get(), androidContext()) }
    
    viewModel { ReaderViewModel(get(), get(), get()) }
}
