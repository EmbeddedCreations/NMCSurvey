package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class SurveyItem(
    val ids: List<String>, // Correct type to handle JSON array
    @SerializedName("latitude") val latitude: String?, // Latitude from SQL alias
    @SerializedName("longitude") val longitude: String?, // Longitude from SQL alias
    @SerializedName("image_urls") val imageUrls: List<String>?, // Image URLs from GROUP_CONCAT
    @SerializedName("remark") val remark: String?, // Remark column
    @SerializedName("clicked_date") val clickedDate: String?, // Clicked date column
    @SerializedName("clicked_time") val clickedTime: String?, // Clicked time column
    @SerializedName("user_upload_date") val userUploadDate: String?, // User upload date
    @SerializedName("user_upload_time") val userUploadTime: String?, // User upload time
    @SerializedName("uploaded_by") val uploadedBy: String?, // Uploaded by column
    @SerializedName("status") val status: String?, // Status column
    @SerializedName("accurracy") val accurracy: String?, // Accuracy with correct spelling from SQL alias
    @SerializedName("distance") val distance: String?, // Accuracy with correct spelling from SQL alias

)





