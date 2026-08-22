package com.bharatconnect.app.core.database

import android.content.Context
import androidx.room.Room
import com.bharatconnect.app.data.local.room.AppDatabase

object DatabaseProvider {
    private const val DATABASE_NAME = "bharatconnect_db"

    @Volatile
    private var instance: AppDatabase? = null

    fun init(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
            .also { instance = it }
        }
    }

    fun getDatabase(): AppDatabase {
        return instance ?: throw IllegalStateException("DatabaseProvider not initialized. Call init(context) in Application class.")
    }
}
