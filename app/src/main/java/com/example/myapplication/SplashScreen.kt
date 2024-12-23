package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Delay for 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            // Start MainActivity after the delay
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() // Close SplashActivity
        }, 2000) // 3000 milliseconds (3 seconds)
    }
}