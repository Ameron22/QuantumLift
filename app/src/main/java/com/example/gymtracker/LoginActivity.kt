package com.example.gymtracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymtracker.ui.theme.QuantumLiftTheme
import com.example.gymtracker.ui.theme.GradientBackground
import com.example.gymtracker.screens.LoginScreen
import com.example.gymtracker.screens.RegisterScreen
import com.example.gymtracker.navigation.Screen
import com.example.gymtracker.viewmodels.LoginAuthViewModel

class LoginActivity : ComponentActivity() {
    private var hasNavigated = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            QuantumLiftTheme {
                GradientBackground {
                    val navController = rememberNavController()
                    val authViewModel: LoginAuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(LoginAuthViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return LoginAuthViewModel(applicationContext) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    })
                    
                    // Get auth state but don't show loading screen
                    val authState by authViewModel.authState.collectAsState()
                    
                    // Handle navigation based on auth state
                    LaunchedEffect(authState.isLoggedIn) {
                        if (authState.isLoggedIn && !hasNavigated) {
                            hasNavigated = true
                            // User is logged in, start MainActivity and clear the stack
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            // Don't call finish() - the flags will handle clearing the stack
                        }
                    }
                    
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Login.route
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(navController, authViewModel)
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(navController, authViewModel)
                        }
                    }
                }
            }
        }
    }
} 