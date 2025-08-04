package com.example.gymtracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.gymtracker.services.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SplashActivity"

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // Install splash screen and keep it on screen until we're ready to show our animation
            val splashScreen = installSplashScreen()
            
            super.onCreate(savedInstanceState)
            
            // Keep splash screen on screen until we're ready to show our animation
            splashScreen.setKeepOnScreenCondition { true }
            
            // Always start with BicepAnimationActivity, authentication check will happen during animation
            Log.d(TAG, "Starting BicepAnimationActivity")
            startActivity(Intent(this@SplashActivity, BicepAnimationActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error in SplashActivity", e)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
 