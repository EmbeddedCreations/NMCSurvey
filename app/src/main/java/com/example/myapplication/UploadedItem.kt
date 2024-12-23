package com.example.myapplication

data class UploadedItem(
    val id: List<String>, // List of IDs
    val imageUrls: List<String>, // List of image URLs
    val description: String?,
    val latitude: String?,
    val longitude: String?,
    val clickedDate: String?,
    val clickedTime: String?,
    val accurracy: String?,
    var currentImageIndex: Int = 0, // Track the currently displayed image index
    var distance : String?
)

