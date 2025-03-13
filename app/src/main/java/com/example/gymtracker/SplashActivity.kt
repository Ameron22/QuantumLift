package com.example.gymtracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

private const val TAG = "SplashActivity"

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // Install splash screen and keep it on screen until we're ready to show our animation
            val splashScreen = installSplashScreen()
            
            super.onCreate(savedInstanceState)
            Log.d(TAG, "Starting BicepAnimationActivity")
            
            // Keep splash screen on screen until we're ready to show our animation
            splashScreen.setKeepOnScreenCondition { true }
            
            // Start our animation activity
            startActivity(Intent(this, BicepAnimationActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error in SplashActivity", e)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
 