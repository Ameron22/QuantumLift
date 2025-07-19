package com.example.gymtracker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymtracker.viewmodels.PhysicalParametersViewModel
import com.example.gymtracker.navigation.Screen
import com.example.gymtracker.components.MeasurementChart
import java.text.SimpleDateFormat
import java.util.*
import com.example.gymtracker.data.MeasurementDataPoint
import com.example.gymtracker.data.BodyMeasurement
import android.util.Log
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// BMI color thresholds and colors
object BMIColors {
    const val UNDERWEIGHT_THRESHOLD = 18.5f
    const val NORMAL_THRESHOLD = 25.0f
    const val OVERWEIGHT_THRESHOLD = 30.0f
    const val OBESE_CLASS_I_THRESHOLD = 35.0f
    const val OBESE_CLASS_II_THRESHOLD = 40.0f
    
    val UNDERWEIGHT_COLOR = Color(0xFF2196F3) // Blue
    val NORMAL_COLOR = Color(0xFF4CAF50) // Green
    val OVERWEIGHT_COLOR = Color(0xFFFFEB3B) // Yellow
    val OBESE_CLASS_I_COLOR = Color(0xFFFF9800) // Orange
    val OBESE_CLASS_II_COLOR = Color(0xFFF44336) // Red
    val EXTREMELY_OBESE_COLOR = Color(0xFFD32F2F) // Dark Red
    
    fun getBMIColor(bmi: Float): Color {
        return when {
            bmi < UNDERWEIGHT_THRESHOLD -> UNDERWEIGHT_COLOR
            bmi < NORMAL_THRESHOLD -> NORMAL_COLOR
            bmi < OVERWEIGHT_THRESHOLD -> OVERWEIGHT_COLOR
            bmi < OBESE_CLASS_I_THRESHOLD -> OBESE_CLASS_I_COLOR
            bmi < OBESE_CLASS_II_THRESHOLD -> OBESE_CLASS_II_COLOR
            else -> EXTREMELY_OBESE_COLOR
        }
    }
    
    fun getBMICategory(bmi: Float): String {
        return when {
            bmi < UNDERWEIGHT_THRESHOLD -> "Underweight"
            bmi < NORMAL_THRESHOLD -> "Normal"
            bmi < OVERWEIGHT_THRESHOLD -> "Overweight"
            bmi < OBESE_CLASS_I_THRESHOLD -> "Obese Class I"
            bmi < OBESE_CLASS_II_THRESHOLD -> "Obese Class II"
            else -> "Extremely Obese"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(
    navController: NavController,
    viewModel: PhysicalParametersViewModel,
    paddingValues: PaddingValues
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Overview & History", "Measurements")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Body Tracking",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Tab Row
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> OverviewAndHistoryTab(navController, viewModel, paddingValues)
            1 -> MeasurementsTab(viewModel, paddingValues)
        }
    }
}

@Composable
fun OverviewAndHistoryTab(navController: NavController, viewModel: PhysicalParametersViewModel, paddingValues: PaddingValues) {
    val latestParameters by viewModel.latestParameters.collectAsState()
    val physicalParameters by viewModel.physicalParameters.collectAsState()
    var showWeightDialog by remember { mutableStateOf(false) }
    var showHeightDialog by remember { mutableStateOf(false) }
    var showBodyFatDialog by remember { mutableStateOf(false) }
    
    Log.d("BodyScreen", "OverviewAndHistoryTab - latestParameters: $latestParameters")
    Log.d("BodyScreen", "OverviewAndHistoryTab - physicalParameters count: ${physicalParameters.size}")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Current Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Stats",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                latestParameters?.let { params ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("Weight", "${params.weight?.toString() ?: "N/A"} kg")
                        StatItem("Height", "${params.height?.toString() ?: "N/A"} cm")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItemWithBackground(
                            label = "BMI",
                            value = "${params.bmi?.toString() ?: "N/A"}",
                            bmi = params.bmi ?: 0f
                        )
                        StatItemWithBackground(
                            label = "Body Fat",
                            value = "${params.bodyFatPercentage?.toString() ?: "N/A"}%",
                            bmi = params.bmi ?: 0f
                        )
                    }
                } ?: run {
                    Text("No data available. Add your first measurement!")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Update buttons - always visible
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { 
                            Log.d("BodyScreen", "Weight button clicked")
                            showWeightDialog = true 
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update Weight")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { 
                            Log.d("BodyScreen", "Height button clicked")
                            showHeightDialog = true 
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update Height")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { 
                        Log.d("BodyScreen", "Body Fat button clicked")
                        showBodyFatDialog = true 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Body Fat")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Charts Section
        if (physicalParameters.isNotEmpty()) {
            
            // Weight Chart
            val weightData = physicalParameters
                .filter { it.weight != null }
                .map { MeasurementDataPoint(it.date, it.weight!!, "weight") }
                .sortedBy { it.date }
            
            if (weightData.isNotEmpty()) {
                MeasurementChart(
                    dataPoints = weightData,
                    title = "Weight Progress (kg)",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // BMI Chart
            val bmiData = physicalParameters
                .filter { it.bmi != null }
                .map { MeasurementDataPoint(it.date, it.bmi!!, "bmi") }
                .sortedBy { it.date }
            
            if (bmiData.isNotEmpty()) {
                MeasurementChart(
                    dataPoints = bmiData,
                    title = "BMI Progress",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Recent measurements list
            Text(
                text = "Recent Measurements",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            physicalParameters.take(5).forEach { parameters ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(Date(parameters.date)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            parameters.weight?.let { weight ->
                                Text("Weight: ${weight} kg")
                            }
                            parameters.bmi?.let { bmi ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = BMIColors.getBMIColor(bmi).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "BMI: ${String.format("%.1f", bmi)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BMIColors.getBMIColor(bmi)
                                        )
                                        Text(
                                            text = BMIColors.getBMICategory(bmi),
                                            fontSize = 10.sp,
                                            color = BMIColors.getBMIColor(bmi),
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        
                        parameters.bodyFatPercentage?.let { bodyFat ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Body Fat: ${bodyFat}%")
                        }
                    }
                }
            }
        } else {
            // No data message
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No measurements yet",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Start tracking your progress by adding your first measurement",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // Weight input dialog
    if (showWeightDialog) {
        Log.d("BodyScreen", "Showing weight dialog")
        WeightInputDialog(
            currentValue = latestParameters?.weight,
            onDismiss = { 
                Log.d("BodyScreen", "Weight dialog dismissed")
                showWeightDialog = false 
            },
            onConfirm = { newValue ->
                Log.d("BodyScreen", "Weight confirmed: $newValue")
                viewModel.viewModelScope.launch {
                    viewModel.addPhysicalParameters(
                        userId = "current_user",
                        weight = newValue,
                        height = latestParameters?.height,
                        bodyFatPercentage = latestParameters?.bodyFatPercentage
                    )
                }
                showWeightDialog = false
            }
        )
    }
    
    // Height input dialog
    if (showHeightDialog) {
        Log.d("BodyScreen", "Showing height dialog")
        HeightInputDialog(
            currentValue = latestParameters?.height,
            onDismiss = { 
                Log.d("BodyScreen", "Height dialog dismissed")
                showHeightDialog = false 
            },
            onConfirm = { newHeight ->
                Log.d("BodyScreen", "Height confirmed: $newHeight")
                viewModel.viewModelScope.launch {
                    viewModel.addPhysicalParameters(
                        userId = "current_user",
                        weight = latestParameters?.weight,
                        height = newHeight,
                        bodyFatPercentage = latestParameters?.bodyFatPercentage
                    )
                }
                showHeightDialog = false
            }
        )
    }
    
    // Body Fat input dialog
    if (showBodyFatDialog) {
        Log.d("BodyScreen", "Showing body fat dialog")
        BodyFatInputDialog(
            currentValue = latestParameters?.bodyFatPercentage,
            onDismiss = { 
                Log.d("BodyScreen", "Body fat dialog dismissed")
                showBodyFatDialog = false 
            },
            onConfirm = { newValue ->
                Log.d("BodyScreen", "Body fat confirmed: $newValue")
                viewModel.viewModelScope.launch {
                    viewModel.addPhysicalParameters(
                        userId = "current_user",
                        weight = latestParameters?.weight,
                        height = latestParameters?.height,
                        bodyFatPercentage = newValue
                    )
                }
                showBodyFatDialog = false
            }
        )
    }
}

@Composable
fun MeasurementsTab(viewModel: PhysicalParametersViewModel, paddingValues: PaddingValues) {
    val latestParameters by viewModel.latestParameters.collectAsState()
    val bodyMeasurements by viewModel.bodyMeasurements.collectAsState()
    
    Log.d("BodyScreen", "MeasurementsTab - latestParameters: $latestParameters")
    Log.d("BodyScreen", "MeasurementsTab - bodyMeasurements: $bodyMeasurements")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Body Measurements",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Measurement types - ordered from top to bottom of body
        val measurementTypes = listOf(
            "neck", "shoulders", "chest", "biceps", "forearms", 
            "waist", "hips", "thighs", "calves"
        )

        measurementTypes.forEach { measurementType ->
            ExpandableMeasurementCard(
                measurementType = measurementType,
                bodyMeasurements = bodyMeasurements,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ExpandableMeasurementCard(
    measurementType: String,
    bodyMeasurements: List<BodyMeasurement>,
    viewModel: PhysicalParametersViewModel
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    
    // Get all measurements for this type
    val measurementsForType = bodyMeasurements
        .filter { it.measurementType == measurementType }
        .sortedBy { it.parametersId }
    
    val latestMeasurement = measurementsForType.lastOrNull()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = measurementType.replaceFirstChar { it.uppercase() },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    latestMeasurement?.let { measurement ->
                        Text(
                            text = "${measurement.value} ${measurement.unit}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add/Update button
                    IconButton(
                        onClick = { 
                            if (viewModel.latestParameters.value != null) {
                                showDialog = true 
                            } else {
                                Log.w("BodyScreen", "No physical parameters found, cannot add body measurements")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (latestMeasurement != null) Icons.Filled.Edit else Icons.Filled.Add,
                            contentDescription = if (latestMeasurement != null) "Update" else "Add",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Expand/Collapse button
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
            
            // Expanded content
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (measurementsForType.isNotEmpty()) {
                    // Chart
                    val allMeasurementsWithDates = viewModel.getBodyMeasurementsWithDates("current_user")
                    val chartData = allMeasurementsWithDates.filter { it.type == measurementType }
                    
                    Log.d("BodyScreen", "Chart data for $measurementType: $chartData")
                    
                    MeasurementChart(
                        dataPoints = chartData,
                        title = "${measurementType.replaceFirstChar { it.uppercase() }} Progress",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // History list
                    Text(
                        text = "History",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    measurementsForType.reversed().map { measurement ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    .format(Date(measurement.parametersId)),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${measurement.value} ${measurement.unit}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No measurements recorded yet",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
    
    // Measurement input dialog
    if (showDialog) {
        MeasurementInputDialog(
            measurementType = measurementType,
            currentValue = latestMeasurement?.value,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                val latestParams = viewModel.latestParameters.value
                if (latestParams != null) {
                    Log.d("BodyScreen", "Adding body measurement with parametersId: ${latestParams.id}")
                    viewModel.addBodyMeasurement(
                        parametersId = latestParams.id,
                        measurementType = measurementType,
                        value = newValue
                    )
                } else {
                    Log.e("BodyScreen", "No latest parameters found, cannot add body measurement")
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun MeasurementInputDialog(
    measurementType: String,
    currentValue: Float?,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentValue?.toString() ?: "") }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentValue != null) "Update $measurementType" else "Add $measurementType",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter measurement value (cm):",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { 
                        inputValue = it
                        isError = false
                    },
                    label = { Text("Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isError) {
                    Text(
                        text = "Please enter a valid number",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = inputValue.toFloatOrNull()
                    if (value != null && value > 0) {
                        onConfirm(value)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WeightInputDialog(
    currentValue: Float?,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentValue?.toString() ?: "") }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentValue != null) "Update Weight" else "Add Weight",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter weight value (kg):",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { 
                        inputValue = it
                        isError = false
                    },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isError) {
                    Text(
                        text = "Please enter a valid number",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = inputValue.toFloatOrNull()
                    if (value != null && value > 0) {
                        onConfirm(value)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HeightInputDialog(
    currentValue: Float?,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentValue?.toString() ?: "") }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentValue != null) "Update Height" else "Add Height",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter height value (cm):",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { 
                        inputValue = it
                        isError = false
                    },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isError) {
                    Text(
                        text = "Please enter a valid number",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = inputValue.toFloatOrNull()
                    if (value != null && value > 0 && value <= 300) {
                        onConfirm(value)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BodyFatInputDialog(
    currentValue: Float?,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentValue?.toString() ?: "") }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentValue != null) "Update Body Fat" else "Add Body Fat",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter body fat percentage:",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { 
                        inputValue = it
                        isError = false
                    },
                    label = { Text("Body Fat (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isError) {
                    Text(
                        text = "Please enter a valid number",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = inputValue.toFloatOrNull()
                    if (value != null && value >= 0 && value <= 100) {
                        onConfirm(value)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

 

@Composable
fun StatItemWithBackground(label: String, value: String, bmi: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (label == "BMI" && bmi > 0) {
            // BMI with colored background
            Box(
                modifier = Modifier
                    .background(
                        color = BMIColors.getBMIColor(bmi).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format("%.1f", bmi),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BMIColors.getBMIColor(bmi)
                    )
                    Text(
                        text = BMIColors.getBMICategory(bmi),
                        fontSize = 10.sp,
                        color = BMIColors.getBMIColor(bmi),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // Regular stat item
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
} 