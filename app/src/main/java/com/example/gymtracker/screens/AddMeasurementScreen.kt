package com.example.gymtracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymtracker.viewmodels.PhysicalParametersViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import android.util.Log
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMeasurementScreen(
    navController: NavController,
    viewModel: PhysicalParametersViewModel
) {
    val scope = rememberCoroutineScope()
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var bodyFatPercentage by remember { mutableStateOf("") }
    var muscleMass by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            
            Text(
                text = "Add Measurement",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp)) // Balance the header
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Measurements Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Basic Measurements",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Weight
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Height
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Body Fat Percentage
                OutlinedTextField(
                    value = bodyFatPercentage,
                    onValueChange = { bodyFatPercentage = it },
                    label = { Text("Body Fat % (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Muscle Mass
                OutlinedTextField(
                    value = muscleMass,
                    onValueChange = { muscleMass = it },
                    label = { Text("Muscle Mass (kg, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Notes (Optional)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Add notes about your measurement") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                Log.d("AddMeasurementScreen", "Save button clicked")
                Log.d("AddMeasurementScreen", "Form data - Weight: '$weight', Height: '$height', BodyFat: '$bodyFatPercentage', MuscleMass: '$muscleMass'")
                
                val userId = "current_user" // This should come from auth
                Log.d("AddMeasurementScreen", "Using userId: $userId")
                
                // Validate input
                val weightValue = weight.toFloatOrNull()
                val heightValue = height.toFloatOrNull()
                val bodyFatValue = bodyFatPercentage.toFloatOrNull()
                val muscleMassValue = muscleMass.toFloatOrNull()
                
                Log.d("AddMeasurementScreen", "Parsed values - Weight: $weightValue, Height: $heightValue, BodyFat: $bodyFatValue, MuscleMass: $muscleMassValue")
                
                if (weightValue == null && heightValue == null) {
                    Log.w("AddMeasurementScreen", "No valid weight or height provided")
                    return@Button
                }
                
                scope.launch {
                    // Save physical parameters
                    val parametersId = viewModel.addPhysicalParameters(
                        userId = userId,
                        weight = weightValue,
                        height = heightValue,
                        bodyFatPercentage = bodyFatValue,
                        muscleMass = muscleMassValue,
                        notes = notes
                    )
                    
                    Log.d("AddMeasurementScreen", "Physical parameters saved, navigating back")
                    navController.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = weight.isNotEmpty() || height.isNotEmpty()
        ) {
            Text("Save Measurement")
        }
    }
} 