package com.example.gymtracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

private const val TAG = "SplashActivity"

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            Log.d(TAG, "Starting BicepAnimationActivity")
            startActivity(Intent(this, BicepAnimationActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error in SplashActivity", e)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
 