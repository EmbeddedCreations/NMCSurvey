package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.github.dhaval2404.imagepicker.ImagePicker
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadScreen : AppCompatActivity() {
    private var gpsLatitude = 0.0
    private var gpsLongitude = 0.0
    private var accuracy = 0.0f
    private var targetUri: Uri? = null
    private var imageChanged = false
    private lateinit var progressBar: ProgressBar // Loader for showing upload progress

    private lateinit var pickImageButton: Button
    private lateinit var buttonUploadImage: Button
    private lateinit var buttonViewHistory: Button
    private lateinit var editTextDescription: EditText
    private lateinit var textViewLat: TextView
    private lateinit var textViewLng: TextView
    private lateinit var textViewAccuracy: TextView
    private lateinit var db: UploadDatabase
    private lateinit var loginname: TextView
    private  lateinit var logout :Button
    private lateinit var imageSlider: ViewPager2
    private lateinit var addImageButton: ImageView
    private lateinit var removeImageButton: ImageView
    private lateinit var imageAdapter: ImageSliderAdapterUpload


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_screen)

        // Handling window insets for fullscreen experience
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.uploadscreen)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize views
        imageSlider = findViewById(R.id.imageSlider)
        addImageButton = findViewById(R.id.addImageButton)
        removeImageButton = findViewById(R.id.removeImageButton)
        // Initialize progress bar
        progressBar = findViewById(R.id.progressBar)

        // Hide progress bar initially
        progressBar.visibility = View.GONE
        // Initialize image adapter for ViewPager2
        imageAdapter = ImageSliderAdapterUpload(imageList)
        imageSlider.adapter = imageAdapter

        // Set click listeners
        addImageButton.setOnClickListener { showImageOptionsDialog() }
        removeImageButton.setOnClickListener { removeLastImage() }

        // Initial visibility of ViewPager2
        updateImageUI()
        // Initialize views
        pickImageButton = findViewById(R.id.pickimage)
        buttonUploadImage = findViewById(R.id.buttonUploadImage)
        editTextDescription = findViewById(R.id.editTextDescription)
        textViewLat = findViewById(R.id.textViewLat)
        textViewLng = findViewById(R.id.textViewLng)
        textViewAccuracy = findViewById(R.id.textViewAccuracy)
        buttonViewHistory = findViewById(R.id.buttonViewHistory)
        loginname = findViewById(R.id.LoginPerson)
        logout = findViewById(R.id.buttonLogout)
        // Retrieve the username passed from the Login activity
        val username = intent.getStringExtra("username")

        // Set the username in loginname TextView
        loginname.text = "$username"

        buttonViewHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            intent.putExtra("username", username) // Pass the username
            startActivity(intent)
        }

        logout.setOnClickListener{
            logout()
        }
        // Retrieve and display location data passed from MainActivity
        gpsLatitude = intent.getDoubleExtra("latitude", 0.0)
        gpsLongitude = intent.getDoubleExtra("longitude", 0.0)
        accuracy = intent.getFloatExtra("accuracy", 0.0f)
        db = UploadDatabase.getDatabase(this)
        // Update UI with location data
        if (gpsLatitude != 0.0 && gpsLongitude != 0.0) {
            textViewLat.text = "Latitude: $gpsLatitude"
            textViewLng.text = "Longitude: $gpsLongitude"
            textViewAccuracy.text = "Accuracy: $accuracy meters"
        }

        // Set click listeners
        pickImageButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("imageUri", targetUri?.toString()) // Preserve image URI
            intent.putExtra(
                "description",
                editTextDescription.text.toString()
            ) // Preserve description
            locationActivityLauncher.launch(intent)

        }
        buttonUploadImage.setOnClickListener{
            uploadDatatoServer();
        }
    }

    private fun removeLastImage() {
        if (imageList.isNotEmpty()) {
            imageList.removeAt(imageList.size - 1) // Remove the last image
            updateImageUI() // Refresh the slider
        } else {
            showToast("No images to remove.")
        }
    }

    private fun updateImageUI() {
        imageAdapter.notifyDataSetChanged()
        imageSlider.visibility = if (imageList.isEmpty()) View.GONE else View.VISIBLE
    }
    private fun logout() {

        // Finish all current activities and return to the login screen
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Close the current activity
    }

    private fun generateImageName(uri: Uri): String {
        return "Image_${System.currentTimeMillis()}.jpg"
    }

    private fun resetViews() {
        editTextDescription.setText("")
        textViewLat.text = "Latitude:"
        textViewLng.text = "Longitude:"
        textViewAccuracy.text = "Accuray:"
        imageChanged = false
        targetUri = null
        // Clear images and refresh slider
        imageList.clear()
        updateImageUI()
    }

    private fun showImageOptionsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose an option")
            .setItems(arrayOf("Capture from Camera", "Select from Gallery")) { _, which ->
                when (which) {
                    0 -> // "Capture from Camera" option is selected
                        ImagePicker.with(this)
                            .cameraOnly()
                            .crop()
                            .compress(1024)
                            .maxResultSize(720, 720)
                            .createIntent { intent -> imagePickerLauncher.launch(intent) }

                    1 -> // "Select from Gallery" option is selected
                        ImagePicker.with(this)
                            .galleryOnly()
                            .crop()
                            .compress(1024)
                            .maxResultSize(720, 720)
                            .createIntent { intent -> imagePickerLauncher.launch(intent) }
                }
            }
        builder.show()
    }

    private fun uploadDatatoServer() {
        if (imageList.isEmpty()) {
            showToast("Please select at least one image to upload.")
            return
        }

        if (gpsLatitude == 0.0 || gpsLongitude == 0.0 || accuracy == 0.0f) {
            showToast("Please make sure location is available (Latitude, Longitude, and Accuracy).")
            return
        }
        showLoader(true)

        // Convert images to Base64 and prepare a comma-separated list of image URLs
        val base64List = imageList.mapNotNull { (uri, imageName) ->
            val filePath = uri.path
            if (filePath == null) {
                Log.e("UploadScreen", "Invalid image URI: $uri")
                return@mapNotNull null
            }

            val file = File(filePath)
            val base64 = convertToBase64(file)
            if (base64.isEmpty()) {
                Log.e("UploadScreen", "Failed to convert image to Base64: $filePath")
                return@mapNotNull null
            }

            Pair(imageName, base64)
        }

        if (base64List.isEmpty()) {
            showToast("Failed to process images.")
            return
        }

        // Initialize date and time if null
        // Initialize date and time dynamically at the time of upload
        val clickedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val clickedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        Log.d("UploadScreen", "Generated Date: $clickedDate")
        Log.d("UploadScreen", "Generated Time: $clickedTime")


        // Prepare JSON payload
        val description = editTextDescription.text.toString()
        val entryBy = loginname.text.toString()

        val requestBody = mutableMapOf(
            "Latitude" to gpsLatitude.toString(),
            "Longitude" to gpsLongitude.toString(),
            "EntryBy" to entryBy,
            "Description" to description,
            "ClickedDate" to clickedDate,
            "ClickedTime" to clickedTime,
            "Accuracy" to accuracy.toString()
        )

        base64List.forEachIndexed { index, (imageName, base64) ->
            requestBody["ImageName_$index"] = imageName
            requestBody["ImageBase64_$index"] = base64
        }

        val jsonBody = JSONObject(requestBody as Map<*, *>).toString()
        Log.d("UploadScreen", "Request Payload: $jsonBody")

        val request = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        CoroutineScope(Dispatchers.IO).launch {
            ApiClient.apiService.uploadData(request).enqueue(object : Callback<UploadResponse> {
                override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                    if (response.isSuccessful) {
                        val uploadedUrls = response.body()?.imageUrls ?: emptyList()
                        val urlList = uploadedUrls.joinToString(",") // Comma-separated URLs

                        runOnUiThread {
                            showLoader(false) // Hide loader after response

                            showToast("Upload successful! Image URLs: $urlList")
                            resetViews()
                        }
                    } else {
                        Log.e("UploadScreen", "Upload failed: ${response.errorBody()?.string()}")
                        runOnUiThread {
                            showLoader(false) // Hide loader after response
                            showToast("Upload failed: ${response.message()}") }

                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    Log.e("UploadScreen", "Upload failed: ${t.message}")
                    runOnUiThread { showToast("Upload failed: ${t.message}") }
                }
            })
        }

}

    private var imageList: MutableList<Pair<Uri, String>> = mutableListOf() // Pair<Uri, ImageName>

    // Launcher for picking images
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                uri?.let {
                    val imageName = generateImageName(uri)
                    imageList.add(Pair(it, imageName)) // Add the image to the list
                    updateImageUI() // Refresh the slider
                }
            } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
                showToast(ImagePicker.getError(result.data))
            } else {
                showToast("Task Cancelled")
            }
        }
    // Launcher to get location from MainActivity
    private val locationActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    gpsLatitude = data.getDoubleExtra("latitude", 0.0)
                    gpsLongitude = data.getDoubleExtra("longitude", 0.0)
                    accuracy = data.getFloatExtra("accuracy", 0.0f)

                    // Update the TextViews with the received location data
                    textViewLat.text = "Latitude: $gpsLatitude"
                    textViewLng.text = "Longitude: $gpsLongitude"
                    textViewAccuracy.text = "Accuracy: $accuracy meters"
                }
            }
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
    private fun showLoader(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        buttonUploadImage.isEnabled = !show
        addImageButton.isEnabled = !show
        removeImageButton.isEnabled = !show
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}

