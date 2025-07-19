package com.example.gymtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import android.util.Log

@Dao
interface PhysicalParametersDao {
    // Physical Parameters methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhysicalParameters(parameters: PhysicalParameters): Long

    @Update
    suspend fun updatePhysicalParameters(parameters: PhysicalParameters)

    @Delete
    suspend fun deletePhysicalParameters(parameters: PhysicalParameters)

    @Query("SELECT * FROM physical_parameters WHERE userId = :userId ORDER BY date DESC")
    fun getPhysicalParametersByUser(userId: String): Flow<List<PhysicalParameters>>

    @Query("SELECT * FROM physical_parameters WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestPhysicalParameters(userId: String): PhysicalParameters?

    @Query("SELECT * FROM physical_parameters WHERE userId = :userId AND date >= :startDate ORDER BY date ASC")
    suspend fun getPhysicalParametersByDateRange(userId: String, startDate: Long): List<PhysicalParameters>

    // Debug query to check if table exists
    @Query("SELECT name FROM sqlite_master WHERE type='table' AND name='physical_parameters'")
    suspend fun checkTableExists(): String?

    // Body Measurements methods
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBodyMeasurement(measurement: BodyMeasurement): Long

    @Update
    suspend fun updateBodyMeasurement(measurement: BodyMeasurement)

    @Delete
    suspend fun deleteBodyMeasurement(measurement: BodyMeasurement)

    @Query("SELECT * FROM body_measurements WHERE parametersId = :parametersId")
    suspend fun getBodyMeasurementsByParametersId(parametersId: Long): List<BodyMeasurement>

    @Query("SELECT * FROM body_measurements WHERE parametersId IN (SELECT id FROM physical_parameters WHERE userId = :userId) AND measurementType = :measurementType ORDER BY parametersId DESC")
    suspend fun getBodyMeasurementsByType(userId: String, measurementType: String): List<BodyMeasurement>

    // Combined queries - removed complex query for now
    // @Query("SELECT pp.*, bm.* FROM physical_parameters pp LEFT JOIN body_measurements bm ON pp.id = bm.parametersId WHERE pp.userId = :userId ORDER BY pp.date DESC")
    // suspend fun getPhysicalParametersWithMeasurements(userId: String): List<PhysicalParametersWithMeasurements>

    // Statistics queries
    @Query("SELECT MIN(weight) FROM physical_parameters WHERE userId = :userId AND weight IS NOT NULL")
    suspend fun getMinWeight(userId: String): Float?

    @Query("SELECT MAX(weight) FROM physical_parameters WHERE userId = :userId AND weight IS NOT NULL")
    suspend fun getMaxWeight(userId: String): Float?

    @Query("SELECT AVG(weight) FROM physical_parameters WHERE userId = :userId AND weight IS NOT NULL")
    suspend fun getAverageWeight(userId: String): Float?
}

// Data class to combine physical parameters with body measurements
data class PhysicalParametersWithMeasurements(
    val parameters: PhysicalParameters,
    val measurements: List<BodyMeasurement>
) 