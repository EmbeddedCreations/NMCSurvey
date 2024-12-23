package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class Login : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var textViewError: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingMessage: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize UI components
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        textViewError = findViewById(R.id.textViewError)
        progressBar = findViewById(R.id.progressBar)
        loadingMessage = findViewById(R.id.loadingMessage)

        // Set up button click listener
        buttonLogin.setOnClickListener {
            val username = editTextUsername.text.toString()
            val password = editTextPassword.text.toString()
            // Show loader and disable button
            showLoading()
            // Perform API login
            performLogin(username, password)
        }
    }

    private fun showLoading() {
        progressBar.visibility = ProgressBar.VISIBLE
        loadingMessage.visibility = TextView.VISIBLE
        buttonLogin.isEnabled = false // Disable the login button
    }

    private fun hideLoading() {
        progressBar.visibility = ProgressBar.GONE
        loadingMessage.visibility = TextView.GONE
        buttonLogin.isEnabled = true // Re-enable the login button
    }
    private fun performLogin(username: String, password: String) {
        // Create JSON object for the request body
        val loginData = mapOf("username" to username, "password" to password)
        val json = Gson().toJson(loginData)

        // Create the OkHttpClient instance
        val client = OkHttpClient()

        // Build the request body with JSON data
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

        // Build the POST request
        val request = Request.Builder()
            .url("https://nmcnagpurskysign.co.in/admin/app/app_login.php")
            .post(requestBody)
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                runOnUiThread {
                    hideLoading()
                    Toast.makeText(this@Login, "Failed to connect to server", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { hideLoading() }
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()

                    // Parse the response data
                    val jsonResponse = Gson().fromJson(responseData, ApiResponse::class.java)

                    // Check if login was successful by evaluating RecordCount
                    if (jsonResponse.result[0].RecordCount == 1) {
                        // Successful login
                        runOnUiThread {
                            Toast.makeText(this@Login, "Login successful", Toast.LENGTH_SHORT).show()
                            textViewError.visibility = TextView.GONE

                            // Pass the username to the next activity via Intent
                            val intent = Intent(this@Login, UploadScreen::class.java)
                            intent.putExtra("username", username) // Passing username
                            startActivity(intent)
                            finish() // Finish the Login activity
                        }

                    } else {
                        // Failed login
                        runOnUiThread {
                            textViewError.text = "Invalid username or password"
                            textViewError.visibility = TextView.VISIBLE

                            // Hide the error message after 2-3 seconds
                            Handler(Looper.getMainLooper()).postDelayed({
                                textViewError.visibility = TextView.GONE
                            }, 3000)
                        }
                    }
                } else {
                    // Handle unsuccessful response
                    runOnUiThread {
                        Toast.makeText(this@Login, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Data class to parse the API response
    data class ApiResponse(val result: List<Result>)
    data class Result(val RecordCount: Int)
}
