# Body Data Cloud Sync Implementation

This document describes the implementation of cloud synchronization for body tracking data in the QuantumLift app.

## Overview

The body cloud sync feature allows users to:
- Save their physical parameters (weight, height, BMI, etc.) and body measurements to the cloud
- Retrieve their data from the cloud when reinstalling the app or using multiple devices
- Keep their progress history synchronized across devices

## Architecture

### Server Side

#### Database Tables

**physical_parameters**
- Stores user physical parameters with timestamps
- Fields: id, user_id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes
- Unique constraint on (user_id, date) to prevent duplicates

**body_measurements**
- Stores detailed body measurements linked to physical parameters
- Fields: id, parameters_id, measurement_type, value, unit
- Links to physical_parameters via foreign key

#### API Endpoints

**GET /api/body/parameters**
- Retrieves all physical parameters for a user
- Supports pagination with limit/offset parameters

**GET /api/body/parameters/latest**
- Retrieves the most recent physical parameters for a user

**POST /api/body/parameters**
- Creates or updates physical parameters
- Validates input data (weight, height, BMI ranges)

**GET /api/body/measurements/{parametersId}**
- Retrieves body measurements for specific parameters

**POST /api/body/measurements**
- Creates or updates body measurements
- Validates measurement types and values

**DELETE /api/body/parameters/{id}**
- Deletes physical parameters and associated measurements

**POST /api/body/sync**
- Bulk sync endpoint for efficient data transfer
- Handles both parameters and measurements in one request

### Client Side

#### Data Models

**BodyDataModels.kt**
- Request/response models for API communication
- Conversion helpers between local and cloud formats

#### Network Layer

**ApiService.kt**
- Retrofit interface for body tracking endpoints
- Handles authentication and error responses

**BodySyncRepository.kt**
- Repository pattern for body data sync operations
- Handles token management and error handling
- Converts between local and cloud data formats

#### ViewModel Layer

**PhysicalParametersViewModel.kt**
- Enhanced with cloud sync capabilities
- Maintains local data and syncs with cloud
- Provides sync status and error handling

#### UI Layer

**BodyScreen.kt**
- Added sync buttons for manual sync triggers
- Displays sync status and error messages
- Shows loading indicators during sync operations

## Usage

### Manual Sync

Users can manually trigger sync operations using the cloud buttons in the Body tab:

- **Download Button** (☁️⬇️): Syncs data from cloud to local device
- **Upload Button** (☁️⬆️): Syncs local data to cloud

### Automatic Sync

The sync operations can be triggered:
- When the app starts (if user is authenticated)
- After adding new physical parameters or measurements
- Periodically in the background (future enhancement)

### Offline Handling

- Local data is always preserved
- Sync operations fail gracefully when offline
- Error messages inform users of sync status
- Data is queued for sync when connection is restored

## Data Flow

### Sync to Cloud
1. User adds physical parameters or measurements locally
2. Data is saved to local Room database
3. User triggers sync or automatic sync occurs
4. Local data is converted to cloud format
5. Bulk sync request is sent to server
6. Server validates and stores data
7. Local IDs are updated with cloud IDs
8. Success/error status is displayed to user

### Sync from Cloud
1. User triggers sync from cloud
2. API requests all user's physical parameters
3. For each parameter, body measurements are fetched
4. Cloud data is converted to local format
5. Local database is updated with cloud data
6. UI is refreshed with new data
7. Success/error status is displayed to user

## Security

- All API endpoints require JWT authentication
- User data is isolated by user_id
- Input validation prevents invalid data
- HTTPS communication for production

## Error Handling

- Network errors are caught and displayed to user
- Validation errors are handled gracefully
- Partial sync failures are logged and reported
- Local data is never lost due to sync failures

## Testing

### Server Testing

Use the provided test script:
```bash
cd server
node test-body-endpoints.js
```

### Manual Testing

1. Start the server: `npm run dev`
2. Run the Android app
3. Navigate to Body tab
4. Add some physical parameters and measurements
5. Use sync buttons to test cloud operations
6. Check server logs for API calls

## Future Enhancements

- **Automatic Background Sync**: Periodic sync without user intervention
- **Conflict Resolution**: Handle conflicts when data is modified on multiple devices
- **Incremental Sync**: Only sync changed data since last sync
- **Offline Queue**: Queue sync operations when offline
- **Sync Status Indicators**: Visual indicators showing sync status of individual records
- **Data Export/Import**: Allow users to export/import their data

## Database Migration

To add the body tracking tables to your existing database:

```sql
-- Run the migration script
\i src/config/add-body-tracking-tables.sql
```

## Configuration

### Server Environment Variables

No additional environment variables are required. The body sync uses the existing database connection and authentication system.

### Android Configuration

The body sync uses the same network configuration as the existing authentication system:
- Base URL: `https://quantum-lift.vercel.app/`
- Authentication: JWT tokens via TokenManager
- Network security: Configured in `network_security_config.xml`

## Troubleshooting

### Common Issues

1. **"Cloud sync not available"**
   - Ensure the app has network connectivity
   - Check that the user is authenticated (has valid JWT token)

2. **Sync fails with authentication error**
   - User needs to log in again
   - JWT token may have expired

3. **Data not syncing**
   - Check server logs for API errors
   - Verify database tables exist
   - Check network connectivity

4. **Duplicate data after sync**
   - The system prevents duplicates using unique constraints
   - If duplicates occur, check the date field format

### Debug Logs

Enable debug logging to troubleshoot sync issues:
- Android: Look for "BodySyncRepository" and "PhysicalParametersViewModel" logs
- Server: Look for "[BODY_*]" prefixed logs

## Performance Considerations

- Bulk sync reduces API calls for better performance
- Pagination prevents loading large datasets at once
- Local caching reduces server requests
- Background sync can be implemented to avoid blocking UI

## Privacy

- All body data is private to the user
- No data is shared with other users
- Data is encrypted in transit (HTTPS)
- Users can delete their data via the delete endpoint
