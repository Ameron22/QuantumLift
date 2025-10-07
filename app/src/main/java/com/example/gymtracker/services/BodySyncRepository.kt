package com.example.gymtracker.services

import android.content.Context
import android.util.Log
import com.example.gymtracker.data.*
import com.example.gymtracker.network.ApiService
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class BodySyncRepository(private val context: Context) {
    
    private val tokenManager = TokenManager(context)
    private val apiService = createApiService()
    
    private fun createApiService(): ApiService {
        // Use the same base URL as AuthRepository
        val baseUrl = "https://quantum-lift.vercel.app/"
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(ApiService::class.java)
    }
    
    private suspend fun getAuthHeader(): String? {
        return tokenManager.getStoredToken()?.let { "Bearer $it" }
    }
    
    // Physical Parameters sync methods
    
    suspend fun getPhysicalParameters(
        limit: Int = 50, 
        offset: Int = 0,
        since: Long? = null  // Delta sync: only fetch data updated after this timestamp
    ): Result<List<PhysicalParametersData>> {
        return try {
            val authHeader = getAuthHeader()
            if (authHeader == null) {
                Log.e("BodySyncRepository", "No auth token available")
                return Result.failure(Exception("No authentication token"))
            }
            
            if (since != null) {
                Log.d("BodySyncRepository", "Fetching physical parameters from cloud (delta sync since: ${java.util.Date(since)})")
            } else {
                Log.d("BodySyncRepository", "Fetching physical parameters from cloud (full sync)")
            }
            
            val response = apiService.getPhysicalParameters(limit, offset, since, authHeader)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Log.d("BodySyncRepository", "Successfully fetched ${body.parameters.size} physical parameters")
                    Result.success(body.parameters)
                } else {
                    Log.e("BodySyncRepository", "Server returned unsuccessful response")
                    Result.failure(Exception("Server returned unsuccessful response"))
                }
            } else {
                Log.e("BodySyncRepository", "API call failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API call failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("BodySyncRepository", "Error fetching physical parameters", e)
            Result.failure(e)
        }
    }
    
    suspend fun getLatestPhysicalParameters(): Result<PhysicalParametersData?> {
        return try {
            val authHeader = getAuthHeader()
            if (authHeader == null) {
                Log.e("BodySyncRepository", "No auth token available")
                return Result.failure(Exception("No authentication token"))
            }
            
            Log.d("BodySyncRepository", "Fetching latest physical parameters from cloud")
            val response = apiService.getLatestPhysicalParameters(authHeader)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Log.d("BodySyncRepository", "Successfully fetched latest physical parameters: ${body.parameters?.id}")
                    Result.success(body.parameters)
                } else {
                    Log.e("BodySyncRepository", "Server returned unsuccessful response")
                    Result.failure(Exception("Server returned unsuccessful response"))
                }
            } else {
                Log.e("BodySyncRepository", "API call failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API call failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("BodySyncRepository", "Error fetching latest physical parameters", e)
            Result.failure(e)
        }
    }
    
    suspend fun savePhysicalParameters(parameters: PhysicalParametersRequest): Result<PhysicalParametersData> {
        return try {
            val authHeader = getAuthHeader()
            if (authHeader == null) {
                Log.e("BodySyncRepository", "No auth token available")
                return Result.failure(Exception("No authentication token"))
            }
            
            Log.d("BodySyncRepository", "Saving physical parameters to cloud: ${parameters.id}")
            val response = apiService.savePhysicalParameters(parameters, authHeader)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.parameters != null) {
                    Log.d("BodySyncRepository", "Successfully saved physical parameters: ${body.parameters.id}")
                    Result.success(body.parameters)
                } else {
                    Log.e("BodySyncRepository", "Server returned unsuccessful response")
                    Result.failure(Exception("Server returned unsuccessful response"))
                }
            } else {
                Log.e("BodySyncRepository", "API call failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API call failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("BodySyncRepository", "Error saving physical parameters", e)
            Result.failure(e)
        }
    }
    
    // Body Measurements sync methods
    
    suspend fun getBodyMeasurements(parametersId: Long): Result<List<BodyMeasurementData>> {
        return try {
            val authHeader = getAuthHeader()
            if (authHeader == null) {
                Log.e("BodySyncRepository", "No auth token available")
                return Result.failure(Exception("No authentication token"))
            }
            
            Log.d("BodySyncRepository", "Fetching body measurements from cloud for parametersId: $parametersId")
            val response = apiService.getBodyMeasurements(parametersId, authHeader)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Log.d("BodySyncRepository", "Successfully fetched ${body.measurements.size} body measurements")
                    Result.success(body.measurements)
                } else {
                    Log.e("BodySyncRepository", "Server returned unsuccessful response")
                    Result.failure(Exception("Server returned unsuccessful response"))
                }
            } else {
                Log.e("BodySyncRepository", "API call failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API call failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("BodySyncRepository", "Error fetching body measurements", e)
            Result.failure(e)
        }
    }
    
    suspend fun saveBodyMeasurements(measurements: List<BodyMeasurementRequest>): Result<List<BodyMeasurementData>> {
        return try {
            val authHeader = getAuthHeader()
            if (authHeader == null) {
                Log.e("BodySyncRepository", "No auth token available")
                return Result.failure(Exception("No authentication token"))
            }
            
            val request = BodyMeasurementsRequest(measurements)
            Log.d("BodySyncRepository", "Saving ${measurements.size} body measurements to cloud")
            val response = apiService.saveBodyMeasurements(request, authHeader)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Log.d("BodySyncRepository", "Successfully saved ${body.measurements.size} body measurements")
                    Result.success(body.measurements)
                } else {
                    Log.e("BodySyncRepository", "Server returned unsuccessful response")
                    Result.failure(Exception("Server returned unsuccessful response"))
                }
            } else {
                Log.e("BodySyncRepository", "API call failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API call failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("BodySyncRepository", "Error saving body measurements", e)
            Result.failure(e)
        }
    }
    
    suspend fun deletePhysicalParameters(id: Long): Result<Boolean> {
        return try {
            val authHeader = getAuthHeader()
            if (authHeader == null) {
                Log.e("BodySyncRepository", "No auth token available")
                return Result.failure(Exception("No authentication token"))
            }
            
            Log.d("BodySyncRepository", "Deleting physical parameters from cloud: $id")
            val response = apiService.deletePhysicalParameters(id, authHeader)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d("BodySyncRepository", "Successfully deleted physical parameters: $id")
                    Result.success(true)
                } else {
                    Log.e("BodySyncRepository", "Server returned empty response")
                    Result.failure(Exception("Server returned empty response"))
                }
            } else {
                Log.e("BodySyncRepository", "API call failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API call failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("BodySyncRepository", "Error deleting physical parameters", e)
            Result.failure(e)
        }
    }
    
    // Bulk sync method
    
    suspend fun syncBodyData(
        parameters: List<PhysicalParametersRequest>,
        measurements: List<BodyMeasurementRequest>
    ): Result<BodySyncResponse> {
        return try {
            val authHeader = getAuthHeader()
            if (authHeader == null) {
                Log.e("BodySyncRepository", "No auth token available")
                return Result.failure(Exception("No authentication token"))
            }
            
            val request = BodySyncRequest(parameters, measurements)
            Log.d("BodySyncRepository", "Syncing body data: ${parameters.size} parameters, ${measurements.size} measurements")
            val response = apiService.syncBodyData(request, authHeader)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Log.d("BodySyncRepository", "Successfully synced body data: ${body.parameters.size} parameters, ${body.measurements.size} measurements")
                    if (body.errors.isNotEmpty()) {
                        Log.w("BodySyncRepository", "Sync completed with ${body.errors.size} errors: ${body.errors}")
                    }
                    Result.success(body)
                } else {
                    Log.e("BodySyncRepository", "Server returned unsuccessful response")
                    Result.failure(Exception("Server returned unsuccessful response"))
                }
            } else {
                Log.e("BodySyncRepository", "API call failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API call failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("BodySyncRepository", "Error syncing body data", e)
            Result.failure(e)
        }
    }
    
    // Helper methods to convert between local and cloud data models
    
    fun convertToPhysicalParametersRequest(local: PhysicalParameters): PhysicalParametersRequest {
        return PhysicalParametersRequest(
            id = local.id,
            date = local.date,
            weight = local.weight,
            height = local.height,
            bmi = local.bmi,
            bodyFatPercentage = local.bodyFatPercentage,
            muscleMass = local.muscleMass,
            notes = local.notes
        )
    }
    
    fun convertToPhysicalParameters(cloud: PhysicalParametersData): PhysicalParameters {
        return PhysicalParameters(
            id = cloud.id,
            userId = cloud.userId,
            date = cloud.date,
            weight = cloud.weight,
            height = cloud.height,
            bmi = cloud.bmi,
            bodyFatPercentage = cloud.bodyFatPercentage,
            muscleMass = cloud.muscleMass,
            notes = cloud.notes
        )
    }
    
    fun convertToBodyMeasurementRequest(local: BodyMeasurement): BodyMeasurementRequest {
        return BodyMeasurementRequest(
            id = local.id,
            parametersId = local.parametersId,
            measurementType = local.measurementType,
            value = local.value,
            unit = local.unit
        )
    }
    
    fun convertToBodyMeasurement(cloud: BodyMeasurementData): BodyMeasurement {
        return BodyMeasurement(
            id = cloud.id,
            parametersId = cloud.parametersId,
            measurementType = cloud.measurementType,
            value = cloud.value,
            unit = cloud.unit
        )
    }
}
