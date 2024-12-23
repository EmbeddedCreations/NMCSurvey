package com.example.myapplication

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UploadEntity::class], version = 2) // Increment version for schema changes
abstract class UploadDatabase : RoomDatabase() {

    abstract fun uploadDao(): UploadDao

    companion object {
        @Volatile
        private var INSTANCE: UploadDatabase? = null

        fun getDatabase(context: Context): UploadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UploadDatabase::class.java,
                    "upload_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    // Define migration strategy
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE uploads ADD COLUMN imageDate TEXT")
            database.execSQL("ALTER TABLE uploads ADD COLUMN imageTime TEXT")
            database.execSQL("ALTER TABLE uploads ADD COLUMN imageName TEXT")
            database.execSQL("ALTER TABLE uploads ADD COLUMN remarks TEXT")
        }
    }
}

