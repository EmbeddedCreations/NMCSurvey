package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import com.bumptech.glide.Glide
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class EditActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var buttonChooseImage: Button
    private lateinit var textViewDescription: TextView
    private lateinit var textViewLatitude: TextView
    private lateinit var textViewLongitude: TextView
    private lateinit var textViewAccuracy: TextView
    private lateinit var textViewClickedDate: TextView
    private lateinit var textViewClickedTime: TextView
    private lateinit var buttonUploadImage: Button
    private lateinit var buttonSeeNearbyLocations: Button

    private lateinit var progressBarLoader: ProgressBar

    private var selectedImageUri: Uri? = null // To store the selected image URI
    private var originalImageUrl: String? = null
    private lateinit var originalData: Map<String, String?> // To store data from the intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit)

        // Initialize views
        imageView = findViewById(R.id.imageView)
        buttonChooseImage = findViewById(R.id.buttonChooseImage)
        textViewDescription = findViewById(R.id.textViewDescription)
        textViewLatitude = findViewById(R.id.textViewLatitude)
        textViewLongitude = findViewById(R.id.textViewLongitude)
        textViewAccuracy = findViewById(R.id.textViewAccuracy)
        textViewClickedDate = findViewById(R.id.textViewClickedDate)
        textViewClickedTime = findViewById(R.id.textViewClickedTime)
        buttonUploadImage = findViewById(R.id.buttonUploadImage)
        buttonSeeNearbyLocations = findViewById(R.id.buttonSeeNearbyLocations)

        // Get data from the intent
        originalImageUrl = intent.getStringExtra("IMAGE_URL")
        originalData = mapOf(
            "DESCRIPTION" to intent.getStringExtra("DESCRIPTION"),
            "LATITUDE" to intent.getStringExtra("LATITUDE"),
            "LONGITUDE" to intent.getStringExtra("LONGITUDE"),
            "CLICKED_DATE" to intent.getStringExtra("CLICKED_DATE"),
            "CLICKED_TIME" to intent.getStringExtra("CLICKED_TIME"),
            "ACCURACY" to intent.getStringExtra("ACCURACY"),
            "EntryBy" to intent.getStringExtra("EntryBy"),
            "last_edited_ID" to intent.getStringExtra("IMAGE_ID")
        )

        Log.d("EditActivity", "Received IMAGE_ID: ${intent.getStringExtra("IMAGE_ID")}")

        // Load the image using Glide
        Glide.with(this)
            .load(originalImageUrl)
            .placeholder(R.drawable.image_placeholder)
            .into(imageView)

        // Set the passed data in the respective views
        textViewDescription.text = "Description: ${originalData["DESCRIPTION"]}"
        textViewLatitude.text = "Latitude: ${originalData["LATITUDE"]}"
        textViewLongitude.text = "Longitude: ${originalData["LONGITUDE"]}"
        textViewAccuracy.text = "Accuracy: ${originalData["ACCURACY"]}"
        textViewClickedDate.text = "Clicked Date: ${originalData["CLICKED_DATE"]}"
        textViewClickedTime.text = "Clicked Time: ${originalData["CLICKED_TIME"]}"

        // Handle the "Choose Image" button click
        buttonChooseImage.setOnClickListener {
            showImageOptionsDialog()
        }
        buttonUploadImage.setOnClickListener { uploadUpdatedData() }
        buttonSeeNearbyLocations.setOnClickListener {
            val latitude = originalData["LATITUDE"]
            val longitude = originalData["LONGITUDE"]
            val username = intent.getStringExtra("EntryBy") ?: "Unknown User"

            if (latitude.isNullOrEmpty() || longitude.isNullOrEmpty()) {
                showToast("Latitude and Longitude are required to see nearby locations.")
                return@setOnClickListener
            }

            val intent = Intent(this, HistoryActivity::class.java).apply {
                putExtra("lat", latitude.toDoubleOrNull())
                putExtra("long", longitude.toDoubleOrNull())
                putExtra("username", username)
            }
            startActivity(intent)
        }


    }

    // Show options to pick an image
    private fun showImageOptionsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose an option")
            .setItems(arrayOf("Capture from Camera", "Select from Gallery")) { _, which ->
                when (which) {
                    0 -> ImagePicker.with(this)
                        .cameraOnly()
                        .crop()
                        .compress(1024)
                        .maxResultSize(720, 720)
                        .createIntent { intent -> imagePickerLauncher.launch(intent) }
                    1 -> ImagePicker.with(this)
                        .galleryOnly()
                        .crop()
                        .compress(1024)
                        .maxResultSize(720, 720)
                        .createIntent { intent -> imagePickerLauncher.launch(intent) }
                }
            }
        builder.show()
    }

    // Launcher for picking images
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let {
                    // Update the ImageView with the selected image
                    imageView.setImageURI(it)
                }
            } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
                showToast(ImagePicker.getError(result.data))
            } else {
                showToast("Task Cancelled")
            }
        }

    // Function to upload the new image along with the original data
    private fun uploadUpdatedData() {
        if (selectedImageUri == null) {
            Snackbar.make(imageView, "Please select an different image to edit", Snackbar.LENGTH_SHORT).show()
            return
        }

        val filePath = selectedImageUri?.path
        if (filePath.isNullOrEmpty()) {
            runOnUiThread {
                showToast("Invalid image path.")
            }
            return
        }

        val file = File(filePath)
        val base64Image = convertToBase64(file)
        if (base64Image.isEmpty()) {
            runOnUiThread {
                showToast("Failed to process the image.")
            }
            return
        }

        val imageId = intent.getStringExtra("IMAGE_ID")

        val requestBody = mutableMapOf(
            "Latitude" to (originalData["LATITUDE"] ?: ""),
            "Longitude" to (originalData["LONGITUDE"] ?: ""),
            "Description" to (originalData["DESCRIPTION"] ?: ""),
            "ClickedDate" to (originalData["CLICKED_DATE"] ?: ""),
            "ClickedTime" to (originalData["CLICKED_TIME"] ?: ""),
            "Accuracy" to (originalData["ACCURACY"] ?: ""),
            "LastEditedId" to imageId,
            "EntryBy" to (originalData["EntryBy"]),
            "ImageBase64_0" to base64Image,
            "ImageName_0" to "UpdatedImage_${System.currentTimeMillis()}.jpg"
        )

        CoroutineScope(Dispatchers.IO).launch {
            ApiClient.apiService.uploadData(
                JSONObject(requestBody as Map<*, *>).toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            ).enqueue(object : Callback<UploadResponse> {
                override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            showToast("Image updated successfully!")
                            clearFields()
                            redirectToPreviousPage()
                        } else {
                            showToast("Failed to update image: ${response.message()}")
                        }
                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    runOnUiThread {
                        showToast("Upload failed: ${t.message}")
                    }
                }
            })
        }
    }



    private fun clearFields() {
        // Reset all fields to their default state
        imageView.setImageResource(R.drawable.image_placeholder) // Reset to placeholder image
        textViewDescription.text = "Description:"
        textViewLatitude.text = "Latitude:"
        textViewLongitude.text = "Longitude:"
        textViewAccuracy.text = "Accuracy:"
        textViewClickedDate.text = "Clicked Date:"
        textViewClickedTime.text = "Clicked Time:"
        selectedImageUri = null // Clear selected image URI
    }
    private fun redirectToPreviousPage() {
        Snackbar.make(imageView, "Redirecting to the Upload Screen...", Snackbar.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            val username = intent.getStringExtra("EntryBy") ?: "Unknown User" // Retrieve username
            val intent = Intent(this, UploadScreen::class.java).apply {
                putExtra("username", username) // Pass username to Upload Screen
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }, 2000) // 2 seconds delay
    }


    private fun convertToBase64(file: File): String {
        return try {
            val bytes = file.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("UploadScreen", "Error reading file: ${e.message}")
            ""
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
