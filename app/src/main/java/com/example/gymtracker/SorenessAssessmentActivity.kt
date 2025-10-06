package com.example.gymtracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.Modifier
import com.example.gymtracker.screens.SorenessAssessmentScreen
import com.example.gymtracker.ui.theme.QuantumLiftTheme
import kotlinx.coroutines.launch

/**
 * Activity for soreness assessment
 * Launched from notifications to collect user feedback
 */
class SorenessAssessmentActivity : ComponentActivity() {
    
    private var sessionId: Long by mutableStateOf(0L)
    private var muscleGroups: Array<String> by mutableStateOf(emptyArray())
    private var assessmentDay: Int by mutableStateOf(1)
    private var fromNotification: Boolean by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Extract data from intent
        sessionId = intent.getLongExtra("sessionId", 0L)
        muscleGroups = intent.getStringArrayExtra("muscleGroups") ?: emptyArray()
        assessmentDay = intent.getIntExtra("assessmentDay", 1)
        fromNotification = intent.getBooleanExtra("fromNotification", false)
        
        Log.d("SorenessAssessmentActivity", "Starting assessment for session $sessionId, muscles: ${muscleGroups.contentToString()}, day: $assessmentDay, fromNotification: $fromNotification")
        Log.d("SorenessAssessmentActivity", "Intent action: ${intent.action}, Intent flags: ${intent.flags}")
        
        setContent {
            QuantumLiftTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SorenessAssessmentScreen(
                        sessionId = sessionId,
                        muscleGroups = muscleGroups.toList(),
                        assessmentDay = assessmentDay,
                        onAssessmentComplete = { assessment ->
                            // Handle assessment completion
                            lifecycleScope.launch {
                                handleAssessmentComplete(assessment)
                            }
                        },
                        onCancel = {
                            // Handle cancellation - same logic as completion
                            if (fromNotification) {
                                returnToMainApp()
                            } else {
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if we have a new intent (when activity is already running)
        val currentIntent = intent
        Log.d("SorenessAssessmentActivity", "onResume called with intent: $currentIntent")
        
        // Update data if intent has changed
        val newSessionId = currentIntent.getLongExtra("sessionId", 0L)
        val newMuscleGroups = currentIntent.getStringArrayExtra("muscleGroups") ?: emptyArray()
        val newAssessmentDay = currentIntent.getIntExtra("assessmentDay", 1)
        val newFromNotification = currentIntent.getBooleanExtra("fromNotification", false)
        
        if (newSessionId != sessionId || !newMuscleGroups.contentEquals(muscleGroups) || 
            newAssessmentDay != assessmentDay || newFromNotification != fromNotification) {
            
            Log.d("SorenessAssessmentActivity", "Intent data changed, updating...")
            sessionId = newSessionId
            muscleGroups = newMuscleGroups
            assessmentDay = newAssessmentDay
            fromNotification = newFromNotification
            
            Log.d("SorenessAssessmentActivity", "Updated from intent - session: $sessionId, muscles: ${muscleGroups.contentToString()}, day: $assessmentDay, fromNotification: $fromNotification")
        }
    }
    
    private suspend fun handleAssessmentComplete(assessment: com.example.gymtracker.data.SorenessAssessment) {
        try {
            // Save assessment to database
            val database = com.example.gymtracker.data.AppDatabase.getDatabase(this)
            database.sorenessDao().insertSorenessAssessment(assessment)
            
            Log.d("SorenessAssessmentActivity", "Assessment saved successfully")
            
            // Handle navigation based on how we got here
            Log.d("SorenessAssessmentActivity", "Assessment complete. fromNotification: $fromNotification")
            if (fromNotification) {
                // If we came from notification, try to return to main app
                Log.d("SorenessAssessmentActivity", "Came from notification, returning to main app")
                returnToMainApp()
            } else {
                // If we came from within the app, just finish this activity
                Log.d("SorenessAssessmentActivity", "Came from within app, finishing activity")
                finish()
            }
            
        } catch (e: Exception) {
            Log.e("SorenessAssessmentActivity", "Error saving assessment", e)
            // TODO: Show error message to user
        }
    }
    
    private fun returnToMainApp() {
        try {
            Log.d("SorenessAssessmentActivity", "Attempting to return to main app")
            
            // Try to return to the main app using a simpler approach
            val mainIntent = Intent(this, com.example.gymtracker.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(mainIntent)
            Log.d("SorenessAssessmentActivity", "Successfully started MainActivity")
            finish()
            
        } catch (e: Exception) {
            Log.e("SorenessAssessmentActivity", "Error returning to main app: ${e.message}", e)
            // Fallback: just finish this activity
            finish()
        }
    }
}
