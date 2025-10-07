package com.example.gymtracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.gymtracker.data.*
import com.example.gymtracker.data.MeasurementDataPoint
import com.example.gymtracker.services.BodySyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import android.util.Log

class PhysicalParametersViewModel(
    private val physicalParametersDao: PhysicalParametersDao,
    private val context: Context? = null
) : ViewModel() {

    private val _physicalParameters = MutableStateFlow<List<PhysicalParameters>>(emptyList())
    val physicalParameters: StateFlow<List<PhysicalParameters>> = _physicalParameters.asStateFlow()

    private val _latestParameters = MutableStateFlow<PhysicalParameters?>(null)
    val latestParameters: StateFlow<PhysicalParameters?> = _latestParameters.asStateFlow()

    private val _bodyMeasurements = MutableStateFlow<List<BodyMeasurement>>(emptyList())
    val bodyMeasurements: StateFlow<List<BodyMeasurement>> = _bodyMeasurements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    // Initialize BodySyncRepository if context is available
    private val bodySyncRepository by lazy {
        if (context != null) BodySyncRepository(context) else null
    }

    fun loadPhysicalParameters(userId: String) {
        Log.d("PhysicalParametersViewModel", "Loading physical parameters for userId: $userId")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                physicalParametersDao.getPhysicalParametersByUser(userId).collect { parameters ->
                    Log.d("PhysicalParametersViewModel", "Loaded ${parameters.size} physical parameters")
                    Log.d("PhysicalParametersViewModel", "Parameters: $parameters")
                    _physicalParameters.value = parameters
                    _latestParameters.value = parameters.firstOrNull()
                    Log.d("PhysicalParametersViewModel", "Latest parameters: ${_latestParameters.value}")
                    
                    // Load body measurements for the latest parameters
                    _latestParameters.value?.let { latest ->
                        loadBodyMeasurementsForParameters(latest.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("PhysicalParametersViewModel", "Error loading physical parameters", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadBodyMeasurementsForParameters(parametersId: Long) {
        viewModelScope.launch {
            try {
                val measurements = physicalParametersDao.getBodyMeasurementsByParametersId(parametersId)
                Log.d("PhysicalParametersViewModel", "Loaded ${measurements.size} body measurements for parametersId: $parametersId")
                Log.d("PhysicalParametersViewModel", "Body measurements: $measurements")
                _bodyMeasurements.value = measurements
            } catch (e: Exception) {
                Log.e("PhysicalParametersViewModel", "Error loading body measurements", e)
            }
        }
    }

    fun loadAllBodyMeasurements(userId: String) {
        viewModelScope.launch {
            try {
                // Get all physical parameters for the user
                physicalParametersDao.getPhysicalParametersByUser(userId).collect { allParameters ->
                    // Get all body measurements for all parameters
                    val allMeasurements = mutableListOf<BodyMeasurement>()
                    allParameters.forEach { parameters ->
                        val measurements = physicalParametersDao.getBodyMeasurementsByParametersId(parameters.id)
                        allMeasurements.addAll(measurements)
                    }
                    
                    Log.d("PhysicalParametersViewModel", "Loaded ${allMeasurements.size} total body measurements for userId: $userId")
                    Log.d("PhysicalParametersViewModel", "Body measurements: $allMeasurements")
                    _bodyMeasurements.value = allMeasurements
                }
            } catch (e: Exception) {
                Log.e("PhysicalParametersViewModel", "Error loading all body measurements", e)
            }
        }
    }

    // Get body measurements with their associated physical parameters for proper date handling
    fun getBodyMeasurementsWithDates(userId: String): List<MeasurementDataPoint> {
        val measurements = _bodyMeasurements.value
        val parameters = _physicalParameters.value
        
        return measurements.mapNotNull { measurement ->
            // Find the associated physical parameters to get the date
            val associatedParameters = parameters.find { it.id == measurement.parametersId }
            associatedParameters?.let { params ->
                MeasurementDataPoint(
                    date = params.date,
                    value = measurement.value,
                    type = measurement.measurementType
                )
            }
        }.sortedBy { it.date }
    }

    suspend fun addPhysicalParameters(
        userId: String,
        weight: Float?,
        height: Float?,
        bodyFatPercentage: Float? = null,
        muscleMass: Float? = null,
        notes: String = ""
    ): Long {
        Log.d("PhysicalParametersViewModel", "Adding physical parameters - userId: $userId, weight: $weight, height: $height, bodyFat: $bodyFatPercentage, muscleMass: $muscleMass")
        
        return try {
            val bmi = if (weight != null && height != null) {
                calculateBMI(weight, height)
            } else null

            Log.d("PhysicalParametersViewModel", "Calculated BMI: $bmi")

            val parameters = PhysicalParameters(
                userId = userId,
                date = System.currentTimeMillis(),
                weight = weight,
                height = height,
                bmi = bmi,
                bodyFatPercentage = bodyFatPercentage,
                muscleMass = muscleMass,
                notes = notes
            )

            Log.d("PhysicalParametersViewModel", "Created PhysicalParameters object: $parameters")

            val parametersId = physicalParametersDao.insertPhysicalParameters(parameters)
            Log.d("PhysicalParametersViewModel", "Inserted physical parameters with ID: $parametersId")
            
            // Reload data
            loadPhysicalParameters(userId)
            
            parametersId
        } catch (e: Exception) {
            Log.e("PhysicalParametersViewModel", "Error adding physical parameters", e)
            -1L
        }
    }

    fun addBodyMeasurement(
        parametersId: Long,
        measurementType: String,
        value: Float,
        unit: String = "cm",
        userId: String = "current_user"
    ) {
        Log.d("PhysicalParametersViewModel", "Adding body measurement - parametersId: $parametersId, type: $measurementType, value: $value, unit: $unit, userId: $userId")
        
        viewModelScope.launch {
            try {
                // If parametersId is 0 or invalid, create a new physical parameters entry
                val actualParametersId = if (parametersId <= 0) {
                    Log.d("PhysicalParametersViewModel", "Creating new physical parameters entry for body measurement")
                    val newParameters = PhysicalParameters(
                        userId = userId,
                        date = System.currentTimeMillis(),
                        weight = null,
                        height = null,
                        bmi = null,
                        bodyFatPercentage = null,
                        muscleMass = null,
                        notes = "Body measurement entry"
                    )
                    val newParametersId = physicalParametersDao.insertPhysicalParameters(newParameters)
                    Log.d("PhysicalParametersViewModel", "Created new physical parameters with ID: $newParametersId")
                    newParametersId
                } else {
                    parametersId
                }
                
                val measurement = BodyMeasurement(
                    parametersId = actualParametersId,
                    measurementType = measurementType,
                    value = value,
                    unit = unit
                )
                
                Log.d("PhysicalParametersViewModel", "Created BodyMeasurement object: $measurement")
                
                val measurementId = physicalParametersDao.insertBodyMeasurement(measurement)
                Log.d("PhysicalParametersViewModel", "Inserted body measurement with ID: $measurementId")
                
                // Reload both physical parameters and body measurements
                loadPhysicalParameters(userId)
                loadAllBodyMeasurements(userId)
            } catch (e: Exception) {
                Log.e("PhysicalParametersViewModel", "Error adding body measurement", e)
            }
        }
    }

    fun getBodyMeasurementsByType(userId: String, measurementType: String) {
        viewModelScope.launch {
            try {
                val measurements = physicalParametersDao.getBodyMeasurementsByType(userId, measurementType)
                _bodyMeasurements.value = measurements
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getMeasurementHistory(userId: String, measurementType: String): List<MeasurementDataPoint> {
        val measurements = _bodyMeasurements.value
        return measurements.map { measurement ->
            MeasurementDataPoint(
                date = measurement.parametersId, // This should be the actual date
                value = measurement.value,
                type = measurement.measurementType
            )
        }.sortedBy { it.date }
    }

    fun getWeightHistory(userId: String): List<MeasurementDataPoint> {
        return _physicalParameters.value
            .filter { it.weight != null }
            .map { parameters ->
                MeasurementDataPoint(
                    date = parameters.date,
                    value = parameters.weight!!,
                    type = "weight"
                )
            }
            .sortedBy { it.date }
    }

    private fun calculateBMI(weight: Float, height: Float): Float {
        val heightInMeters = height / 100
        return weight / (heightInMeters * heightInMeters)
    }

    fun getStatistics(userId: String) {
        viewModelScope.launch {
            try {
                val minWeight = physicalParametersDao.getMinWeight(userId)
                val maxWeight = physicalParametersDao.getMaxWeight(userId)
                val avgWeight = physicalParametersDao.getAverageWeight(userId)
                
                // You can add more statistics here
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Debug function to check if table exists
    fun debugCheckTable() {
        viewModelScope.launch {
            try {
                val tableExists = physicalParametersDao.checkTableExists()
                Log.d("PhysicalParametersViewModel", "Table exists check: $tableExists")
            } catch (e: Exception) {
                Log.e("PhysicalParametersViewModel", "Error checking table existence", e)
            }
        }
    }

    // Cloud sync methods
    
    fun syncFromCloud(userId: String) {
        val repository = bodySyncRepository
        if (repository == null) {
            Log.w("PhysicalParametersViewModel", "Cannot sync - no context available")
            _syncError.value = "Cloud sync not available - no context"
            return
        }
        
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            
            try {
                Log.d("PhysicalParametersViewModel", "Starting sync from cloud for userId: $userId")
                
                // Sync physical parameters from cloud
                val parametersResult = repository.getPhysicalParameters()
                if (parametersResult.isSuccess) {
                    val cloudParameters = parametersResult.getOrNull() ?: emptyList()
                    Log.d("PhysicalParametersViewModel", "Retrieved ${cloudParameters.size} parameters from cloud")
                    
                    // Log detailed cloud data
                    cloudParameters.forEach { cloudParam ->
                        Log.d("PhysicalParametersViewModel", "Cloud parameter: id=${cloudParam.id}, date=${cloudParam.date}, weight=${cloudParam.weight}, height=${cloudParam.height}")
                    }
                    
                    // Save cloud data to local database with cloud IDs
                    cloudParameters.forEach { cloudParam ->
                        try {
                            val localParam = repository.convertToPhysicalParameters(cloudParam)
                            Log.d("PhysicalParametersViewModel", "Converting cloud param to local: id=${localParam.id}, userId=${localParam.userId}, weight=${localParam.weight}, height=${localParam.height}")
                            
                            // Use INSERT OR REPLACE to ensure cloud ID is used
                            val insertedId = physicalParametersDao.insertPhysicalParameters(localParam)
                            Log.d("PhysicalParametersViewModel", "Saved parameter with ID: $insertedId (cloud ID was: ${cloudParam.id})")
                            
                            // Fetch and save measurements for this parameter using CLOUD ID
                            try {
                                val measurementsResult = repository.getBodyMeasurements(cloudParam.id)
                                if (measurementsResult.isSuccess) {
                                    val cloudMeasurements = measurementsResult.getOrNull() ?: emptyList()
                                    Log.d("PhysicalParametersViewModel", "Retrieved ${cloudMeasurements.size} measurements for cloud parameter ${cloudParam.id}")
                                    
                                    // Save measurements with cloud IDs
                                    cloudMeasurements.forEach { cloudMeasurement ->
                                        try {
                                            val localMeasurement = repository.convertToBodyMeasurement(cloudMeasurement)
                                            val measurementId = physicalParametersDao.insertBodyMeasurement(localMeasurement)
                                            Log.d("PhysicalParametersViewModel", "Saved measurement with ID: $measurementId")
                                        } catch (e: Exception) {
                                            Log.e("PhysicalParametersViewModel", "Error saving cloud measurement ${cloudMeasurement.id}", e)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("PhysicalParametersViewModel", "Error fetching measurements for parameter ${cloudParam.id}", e)
                            }
                        } catch (e: Exception) {
                            Log.e("PhysicalParametersViewModel", "Error saving cloud parameter ${cloudParam.id}", e)
                        }
                    }
                } else {
                    Log.w("PhysicalParametersViewModel", "Failed to sync parameters from cloud: ${parametersResult.exceptionOrNull()?.message}")
                }
                
                // Reload local data
                loadPhysicalParameters(userId)
                loadAllBodyMeasurements(userId)
                
                Log.d("PhysicalParametersViewModel", "Sync from cloud completed successfully")
                
            } catch (e: Exception) {
                Log.e("PhysicalParametersViewModel", "Error during sync from cloud", e)
                _syncError.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    fun syncToCloud(userId: String) {
        val repository = bodySyncRepository
        if (repository == null) {
            Log.w("PhysicalParametersViewModel", "Cannot sync - no context available")
            _syncError.value = "Cloud sync not available - no context"
            return
        }
        
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            
            try {
                Log.d("PhysicalParametersViewModel", "Starting sync to cloud for userId: $userId")
                
                // Convert local data to cloud format
                val localParameters = _physicalParameters.value
                val localMeasurements = _bodyMeasurements.value
                
                val parametersRequests = localParameters.map { param ->
                    repository.convertToPhysicalParametersRequest(param)
                }
                
                val measurementsRequests = localMeasurements.map { measurement ->
                    repository.convertToBodyMeasurementRequest(measurement)
                }
                
                Log.d("PhysicalParametersViewModel", "Syncing ${parametersRequests.size} parameters and ${measurementsRequests.size} measurements to cloud")
                
                // Perform bulk sync
                val syncResult = repository.syncBodyData(parametersRequests, measurementsRequests)
                
                if (syncResult.isSuccess) {
                    val syncResponse = syncResult.getOrNull()
                    if (syncResponse != null) {
                        Log.d("PhysicalParametersViewModel", "Successfully synced to cloud: ${syncResponse.parameters.size} parameters, ${syncResponse.measurements.size} measurements")
                        
                        if (syncResponse.errors.isNotEmpty()) {
                            Log.w("PhysicalParametersViewModel", "Sync completed with ${syncResponse.errors.size} errors")
                            _syncError.value = "Sync completed with ${syncResponse.errors.size} errors"
                        }
                        
                        // Update local IDs with cloud IDs for newly created items
                        syncResponse.parameters.forEach { cloudParam ->
                            val localParam = localParameters.find { it.date == cloudParam.date }
                            if (localParam != null && localParam.id != cloudParam.id) {
                                // Update local parameter with cloud ID
                                val updatedParam = localParam.copy(id = cloudParam.id)
                                physicalParametersDao.updatePhysicalParameters(updatedParam)
                            }
                        }
                        
                        syncResponse.measurements.forEach { cloudMeasurement ->
                            val localMeasurement = localMeasurements.find { 
                                it.parametersId == cloudMeasurement.parametersId && 
                                it.measurementType == cloudMeasurement.measurementType &&
                                it.value == cloudMeasurement.value
                            }
                            if (localMeasurement != null && localMeasurement.id != cloudMeasurement.id) {
                                // Update local measurement with cloud ID
                                val updatedMeasurement = localMeasurement.copy(id = cloudMeasurement.id)
                                physicalParametersDao.updateBodyMeasurement(updatedMeasurement)
                            }
                        }
                        
                        // Reload local data to reflect any ID updates
                        loadPhysicalParameters(userId)
                        loadAllBodyMeasurements(userId)
                    }
                } else {
                    Log.e("PhysicalParametersViewModel", "Failed to sync to cloud: ${syncResult.exceptionOrNull()?.message}")
                    _syncError.value = "Sync failed: ${syncResult.exceptionOrNull()?.message}"
                }
                
                Log.d("PhysicalParametersViewModel", "Sync to cloud completed")
                
            } catch (e: Exception) {
                Log.e("PhysicalParametersViewModel", "Error during sync to cloud", e)
                _syncError.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    fun clearSyncError() {
        _syncError.value = null
    }
    
    // Method to check if cloud sync is available
    fun isCloudSyncAvailable(): Boolean {
        return bodySyncRepository != null
    }
} 