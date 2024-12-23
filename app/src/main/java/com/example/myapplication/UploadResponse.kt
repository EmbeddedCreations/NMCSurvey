package com.example.myapplication

data class UploadResponse(
    val success: Boolean,
    val message: String?,
    val imageUrls: List<String>? // Ensure this matches the API's response field name and type
)

