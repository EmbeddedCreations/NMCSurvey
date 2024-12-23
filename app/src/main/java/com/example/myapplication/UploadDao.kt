package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadDao {
    @Insert
    suspend fun insertUpload(uploadEntity: UploadEntity)

    @Query("SELECT * FROM uploads")
    fun getAllUploads(): Flow<List<UploadEntity>>
}


