package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.ProfileRepository

class ProfileManagerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ProfileRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "multiprofile_manager_db"
        )
        .fallbackToDestructiveMigration() // Graceful handling of model updates
        .build()

        // Initialize Repository injector
        repository = ProfileRepository(applicationContext, database)
    }
}
