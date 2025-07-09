package com.example.gymtracker.components

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.gymtracker.navigation.Screen
import com.example.gymtracker.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthState(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            // User is logged in, navigate to home
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
                popUpTo(Screen.Register.route) { inclusive = true }
            }
        } else {
            // User is not logged in, navigate to login
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
} 