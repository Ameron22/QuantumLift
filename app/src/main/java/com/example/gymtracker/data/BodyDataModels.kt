package com.example.gymtracker.data

/**
 * Data classes for body tracking cloud sync
 */

// Request/Response models for Physical Parameters
data class PhysicalParametersRequest(
    val id: Long? = null,
    val date: Long,
    val weight: Float?,
    val height: Float?,
    val bmi: Float?,
    val bodyFatPercentage: Float?,
    val muscleMass: Float?,
    val notes: String = ""
)

data class PhysicalParametersResponse(
    val success: Boolean,
    val parameters: PhysicalParametersData? = null
)

data class PhysicalParametersListResponse(
    val success: Boolean,
    val parameters: List<PhysicalParametersData> = emptyList()
)

data class PhysicalParametersData(
    val id: Long,
    val userId: String,
    val date: Long,
    val weight: Float?,
    val height: Float?,
    val bmi: Float?,
    val bodyFatPercentage: Float?,
    val muscleMass: Float?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

// Request/Response models for Body Measurements
data class BodyMeasurementRequest(
    val id: Long? = null,
    val parametersId: Long,
    val measurementType: String,
    val value: Float,
    val unit: String = "cm"
)

data class BodyMeasurementsRequest(
    val measurements: List<BodyMeasurementRequest>
)

data class BodyMeasurementsResponse(
    val success: Boolean,
    val measurements: List<BodyMeasurementData> = emptyList()
)

data class BodyMeasurementData(
    val id: Long,
    val parametersId: Long,
    val measurementType: String,
    val value: Float,
    val unit: String,
    val createdAt: Long,
    val updatedAt: Long
)

// Sync request/response models
data class BodySyncRequest(
    val parameters: List<PhysicalParametersRequest> = emptyList(),
    val measurements: List<BodyMeasurementRequest> = emptyList()
)

data class BodySyncResponse(
    val success: Boolean,
    val parameters: List<PhysicalParametersData> = emptyList(),
    val measurements: List<BodyMeasurementData> = emptyList(),
    val errors: List<String> = emptyList()
)

// Error response model
data class BodyErrorResponse(
    val error: String,
    val message: String
)
