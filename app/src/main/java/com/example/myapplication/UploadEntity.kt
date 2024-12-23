package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uploads")
data class UploadEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val imagePath: String,
    val date: String?,
    val time: String?,       // Store the local path of the image     // New field for image time
    val imageName: String?,        // New field for image name
    val remarks: String?
)
