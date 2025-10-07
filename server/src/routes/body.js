const express = require('express');
const { query } = require('../config/database');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

// Helper: Validate measurement type
function isValidMeasurementType(type) {
  const validTypes = [
    'neck', 'shoulders', 'chest', 'biceps', 'forearms', 
    'waist', 'hips', 'thighs', 'calves'
  ];
  return validTypes.includes(type);
}

// Helper: Validate physical parameters data
function validatePhysicalParameters(data) {
  const errors = [];
  
  if (data.weight !== null && data.weight !== undefined) {
    if (typeof data.weight !== 'number' || data.weight <= 0 || data.weight > 500) {
      errors.push('Weight must be a positive number between 0 and 500 kg');
    }
  }
  
  if (data.height !== null && data.height !== undefined) {
    if (typeof data.height !== 'number' || data.height <= 0 || data.height > 300) {
      errors.push('Height must be a positive number between 0 and 300 cm');
    }
  }
  
  if (data.bmi !== null && data.bmi !== undefined) {
    if (typeof data.bmi !== 'number' || data.bmi <= 0 || data.bmi > 100) {
      errors.push('BMI must be a positive number between 0 and 100');
    }
  }
  
  if (data.bodyFatPercentage !== null && data.bodyFatPercentage !== undefined) {
    if (typeof data.bodyFatPercentage !== 'number' || data.bodyFatPercentage < 0 || data.bodyFatPercentage > 100) {
      errors.push('Body fat percentage must be between 0 and 100');
    }
  }
  
  if (data.muscleMass !== null && data.muscleMass !== undefined) {
    if (typeof data.muscleMass !== 'number' || data.muscleMass <= 0 || data.muscleMass > 200) {
      errors.push('Muscle mass must be a positive number between 0 and 200 kg');
    }
  }
  
  return errors;
}

// Helper: Validate body measurement data
function validateBodyMeasurement(data) {
  const errors = [];
  
  if (!data.measurementType || !isValidMeasurementType(data.measurementType)) {
    errors.push('Invalid measurement type. Must be one of: neck, shoulders, chest, biceps, forearms, waist, hips, thighs, calves');
  }
  
  if (typeof data.value !== 'number' || data.value <= 0 || data.value > 1000) {
    errors.push('Measurement value must be a positive number between 0 and 1000');
  }
  
  if (data.unit && data.unit !== 'cm' && data.unit !== 'in') {
    errors.push('Unit must be either "cm" or "in"');
  }
  
  return errors;
}

// Get all physical parameters for a user
router.get('/parameters', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { limit = 50, offset = 0, since } = req.query;

    console.log('[BODY_PARAMETERS] 📊 Fetching physical parameters:', {
      userId,
      limit,
      offset,
      since: since ? new Date(parseInt(since)) : null
    });

    // Delta sync: if 'since' timestamp is provided, only return data updated after that time
    let result;
    if (since) {
      const sinceDate = new Date(parseInt(since));
      console.log('[BODY_PARAMETERS] 🔄 Delta sync requested - fetching data since:', sinceDate);
      
      result = await query(
        `SELECT id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes, created_at, updated_at
         FROM physical_parameters 
         WHERE user_id = $1 AND updated_at > $2
         ORDER BY date DESC 
         LIMIT $3 OFFSET $4`,
        [userId, sinceDate, limit, offset]
      );
    } else {
      // Full sync: return all data
      console.log('[BODY_PARAMETERS] 📥 Full sync requested - fetching all data');
      
      result = await query(
        `SELECT id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes, created_at, updated_at
         FROM physical_parameters 
         WHERE user_id = $1 
         ORDER BY date DESC 
         LIMIT $2 OFFSET $3`,
        [userId, limit, offset]
      );
    }

    const parameters = result.rows.map(row => ({
      id: row.id,
      userId: userId,
      date: row.date.getTime(), // Convert to milliseconds for Android
      weight: row.weight,
      height: row.height,
      bmi: row.bmi,
      bodyFatPercentage: row.body_fat_percentage,
      muscleMass: row.muscle_mass,
      notes: row.notes || '',
      createdAt: row.created_at.getTime(),
      updatedAt: row.updated_at.getTime()
    }));

    console.log(`[BODY_PARAMETERS] ✅ Retrieved ${parameters.length} physical parameters`);

    res.json({
      success: true,
      parameters
    });

  } catch (error) {
    console.error('[BODY_PARAMETERS] ❌ Error fetching physical parameters:', error);
    res.status(500).json({
      error: 'Failed to fetch physical parameters',
      message: 'Internal server error'
    });
  }
});

// Get latest physical parameters for a user
router.get('/parameters/latest', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    console.log('[BODY_PARAMETERS_LATEST] 📊 Fetching latest physical parameters:', { userId });

    const result = await query(
      `SELECT id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes, created_at, updated_at
       FROM physical_parameters 
       WHERE user_id = $1 
       ORDER BY date DESC 
       LIMIT 1`,
      [userId]
    );

    if (result.rows.length === 0) {
      return res.json({
        success: true,
        parameters: null
      });
    }

    const row = result.rows[0];
    const parameters = {
      id: row.id,
      userId: userId,
      date: row.date.getTime(),
      weight: row.weight,
      height: row.height,
      bmi: row.bmi,
      bodyFatPercentage: row.body_fat_percentage,
      muscleMass: row.muscle_mass,
      notes: row.notes || '',
      createdAt: row.created_at.getTime(),
      updatedAt: row.updated_at.getTime()
    };

    console.log('[BODY_PARAMETERS_LATEST] ✅ Retrieved latest physical parameters:', parameters.id);

    res.json({
      success: true,
      parameters
    });

  } catch (error) {
    console.error('[BODY_PARAMETERS_LATEST] ❌ Error fetching latest physical parameters:', error);
    res.status(500).json({
      error: 'Failed to fetch latest physical parameters',
      message: 'Internal server error'
    });
  }
});

// Create or update physical parameters
router.post('/parameters', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { id, date, weight, height, bmi, bodyFatPercentage, muscleMass, notes } = req.body;

    console.log('[BODY_PARAMETERS_CREATE] 📝 Creating/updating physical parameters:', {
      userId,
      id,
      date,
      weight,
      height,
      bmi,
      bodyFatPercentage,
      muscleMass,
      notes
    });

    // Validate input data
    const validationErrors = validatePhysicalParameters({
      weight, height, bmi, bodyFatPercentage, muscleMass
    });

    if (validationErrors.length > 0) {
      return res.status(400).json({
        error: 'Validation failed',
        message: validationErrors.join(', ')
      });
    }

    // Convert date from milliseconds to Date object
    const dateObj = new Date(date);

    let result;
    if (id) {
      // Update existing parameters
      result = await query(
        `UPDATE physical_parameters 
         SET date = $2, weight = $3, height = $4, bmi = $5, 
             body_fat_percentage = $6, muscle_mass = $7, notes = $8, updated_at = CURRENT_TIMESTAMP
         WHERE id = $1 AND user_id = $9
         RETURNING id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes, created_at, updated_at`,
        [id, dateObj, weight, height, bmi, bodyFatPercentage, muscleMass, notes || '', userId]
      );
    } else {
      // Create new parameters
      result = await query(
        `INSERT INTO physical_parameters 
         (user_id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes) 
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
         RETURNING id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes, created_at, updated_at`,
        [userId, dateObj, weight, height, bmi, bodyFatPercentage, muscleMass, notes || '']
      );
    }

    if (result.rows.length === 0) {
      return res.status(404).json({
        error: 'Not found',
        message: 'Physical parameters not found or not owned by user'
      });
    }

    const row = result.rows[0];
    const parameters = {
      id: row.id,
      userId: userId,
      date: row.date.getTime(),
      weight: row.weight,
      height: row.height,
      bmi: row.bmi,
      bodyFatPercentage: row.body_fat_percentage,
      muscleMass: row.muscle_mass,
      notes: row.notes || '',
      createdAt: row.created_at.getTime(),
      updatedAt: row.updated_at.getTime()
    };

    console.log('[BODY_PARAMETERS_CREATE] ✅ Physical parameters saved:', parameters.id);

    res.json({
      success: true,
      parameters
    });

  } catch (error) {
    console.error('[BODY_PARAMETERS_CREATE] ❌ Error saving physical parameters:', error);
    res.status(500).json({
      error: 'Failed to save physical parameters',
      message: 'Internal server error'
    });
  }
});

// Get body measurements for specific parameters
router.get('/measurements/:parametersId', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const parametersId = parseInt(req.params.parametersId);

    console.log('[BODY_MEASUREMENTS] 📏 Fetching body measurements:', {
      userId,
      parametersId
    });

    // First verify that the parameters belong to the user
    const parametersCheck = await query(
      'SELECT id FROM physical_parameters WHERE id = $1 AND user_id = $2',
      [parametersId, userId]
    );

    if (parametersCheck.rows.length === 0) {
      return res.status(404).json({
        error: 'Not found',
        message: 'Physical parameters not found or not owned by user'
      });
    }

    const result = await query(
      `SELECT id, measurement_type, value, unit, created_at, updated_at
       FROM body_measurements 
       WHERE parameters_id = $1 
       ORDER BY measurement_type`,
      [parametersId]
    );

    const measurements = result.rows.map(row => ({
      id: row.id,
      parametersId: parametersId,
      measurementType: row.measurement_type,
      value: row.value,
      unit: row.unit,
      createdAt: row.created_at.getTime(),
      updatedAt: row.updated_at.getTime()
    }));

    console.log(`[BODY_MEASUREMENTS] ✅ Retrieved ${measurements.length} body measurements`);

    res.json({
      success: true,
      measurements
    });

  } catch (error) {
    console.error('[BODY_MEASUREMENTS] ❌ Error fetching body measurements:', error);
    res.status(500).json({
      error: 'Failed to fetch body measurements',
      message: 'Internal server error'
    });
  }
});

// Create or update body measurements
router.post('/measurements', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { measurements } = req.body; // Array of measurements

    console.log('[BODY_MEASUREMENTS_CREATE] 📏 Creating/updating body measurements:', {
      userId,
      measurementsCount: measurements?.length
    });

    if (!measurements || !Array.isArray(measurements) || measurements.length === 0) {
      return res.status(400).json({
        error: 'Invalid input',
        message: 'Measurements array is required'
      });
    }

    // Validate all measurements first
    for (const measurement of measurements) {
      const validationErrors = validateBodyMeasurement(measurement);
      if (validationErrors.length > 0) {
        return res.status(400).json({
          error: 'Validation failed',
          message: `Measurement ${measurement.measurementType}: ${validationErrors.join(', ')}`
        });
      }
    }

    const savedMeasurements = [];

    // Process each measurement
    for (const measurement of measurements) {
      const { id, parametersId, measurementType, value, unit = 'cm' } = measurement;

      // Verify that the parameters belong to the user
      const parametersCheck = await query(
        'SELECT id FROM physical_parameters WHERE id = $1 AND user_id = $2',
        [parametersId, userId]
      );

      if (parametersCheck.rows.length === 0) {
        return res.status(404).json({
          error: 'Not found',
          message: `Physical parameters with ID ${parametersId} not found or not owned by user`
        });
      }

      let result;
      if (id) {
        // Update existing measurement
        result = await query(
          `UPDATE body_measurements 
           SET measurement_type = $2, value = $3, unit = $4, updated_at = CURRENT_TIMESTAMP
           WHERE id = $1 AND parameters_id = $5
           RETURNING id, measurement_type, value, unit, created_at, updated_at`,
          [id, measurementType, value, unit, parametersId]
        );
      } else {
        // Create new measurement
        result = await query(
          `INSERT INTO body_measurements 
           (parameters_id, measurement_type, value, unit) 
           VALUES ($1, $2, $3, $4)
           RETURNING id, measurement_type, value, unit, created_at, updated_at`,
          [parametersId, measurementType, value, unit]
        );
      }

      if (result.rows.length === 0) {
        return res.status(404).json({
          error: 'Not found',
          message: 'Measurement not found or not owned by user'
        });
      }

      const row = result.rows[0];
      savedMeasurements.push({
        id: row.id,
        parametersId: parametersId,
        measurementType: row.measurement_type,
        value: row.value,
        unit: row.unit,
        createdAt: row.created_at.getTime(),
        updatedAt: row.updated_at.getTime()
      });
    }

    console.log(`[BODY_MEASUREMENTS_CREATE] ✅ Saved ${savedMeasurements.length} body measurements`);

    res.json({
      success: true,
      measurements: savedMeasurements
    });

  } catch (error) {
    console.error('[BODY_MEASUREMENTS_CREATE] ❌ Error saving body measurements:', error);
    res.status(500).json({
      error: 'Failed to save body measurements',
      message: 'Internal server error'
    });
  }
});

// Delete physical parameters and associated measurements
router.delete('/parameters/:id', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const parametersId = parseInt(req.params.id);

    console.log('[BODY_PARAMETERS_DELETE] 🗑️ Deleting physical parameters:', {
      userId,
      parametersId
    });

    // Verify ownership and delete
    const result = await query(
      'DELETE FROM physical_parameters WHERE id = $1 AND user_id = $2 RETURNING id',
      [parametersId, userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        error: 'Not found',
        message: 'Physical parameters not found or not owned by user'
      });
    }

    console.log('[BODY_PARAMETERS_DELETE] ✅ Physical parameters deleted:', parametersId);

    res.json({
      success: true,
      message: 'Physical parameters and associated measurements deleted successfully'
    });

  } catch (error) {
    console.error('[BODY_PARAMETERS_DELETE] ❌ Error deleting physical parameters:', error);
    res.status(500).json({
      error: 'Failed to delete physical parameters',
      message: 'Internal server error'
    });
  }
});

// Sync endpoint for bulk operations
router.post('/sync', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { parameters, measurements } = req.body;

    console.log('[BODY_SYNC] 🔄 Syncing body data:', {
      userId,
      parametersCount: parameters?.length || 0,
      measurementsCount: measurements?.length || 0
    });

    const syncResults = {
      parameters: [],
      measurements: [],
      errors: []
    };

    // Sync parameters if provided
    if (parameters && Array.isArray(parameters)) {
      for (const param of parameters) {
        try {
          const validationErrors = validatePhysicalParameters(param);
          if (validationErrors.length > 0) {
            syncResults.errors.push(`Parameter validation failed: ${validationErrors.join(', ')}`);
            continue;
          }

          const dateObj = new Date(param.date);
          let result;

          // Use UPSERT logic: try to insert, update on conflict
          // The unique constraint is on (user_id, DATE(date)), so we need to check if a record exists first
          const existingCheck = await query(
            `SELECT id FROM physical_parameters 
             WHERE user_id = $1 AND DATE(date) = DATE($2)`,
            [userId, dateObj]
          );

          if (existingCheck.rows.length > 0) {
            // Update existing record - ONLY if the new timestamp is newer
            const existingId = existingCheck.rows[0].id;
            
            // First, get the existing record to compare timestamps
            const existingRecord = await query(
              `SELECT date FROM physical_parameters WHERE id = $1`,
              [existingId]
            );
            
            const existingDate = existingRecord.rows[0].date;
            
            // Only update if the new data is newer
            if (dateObj > existingDate) {
              console.log(`[BODY_SYNC] 📅 Updating with newer data: ${dateObj} > ${existingDate}`);
              result = await query(
                `UPDATE physical_parameters 
                 SET date = $2, weight = $3, height = $4, bmi = $5, 
                 body_fat_percentage = $6, muscle_mass = $7, notes = $8, updated_at = CURRENT_TIMESTAMP
                 WHERE id = $1
                 RETURNING id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes, created_at, updated_at`,
                [existingId, dateObj, param.weight, param.height, param.bmi, param.bodyFatPercentage, param.muscleMass, param.notes || '']
              );
            } else {
              console.log(`[BODY_SYNC] ⏭️ Skipping update - existing data is newer: ${existingDate} >= ${dateObj}`);
              // Return existing record without updating
              result = await query(
                `SELECT id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes, created_at, updated_at
                 FROM physical_parameters WHERE id = $1`,
                [existingId]
              );
            }
          } else {
            // Insert new record
            result = await query(
              `INSERT INTO physical_parameters 
               (user_id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes) 
               VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
               RETURNING id, date, weight, height, bmi, body_fat_percentage, muscle_mass, notes, created_at, updated_at`,
              [userId, dateObj, param.weight, param.height, param.bmi, param.bodyFatPercentage, param.muscleMass, param.notes || '']
            );
          }

          if (result.rows.length > 0) {
            const row = result.rows[0];
            syncResults.parameters.push({
              id: row.id,
              userId: userId,
              date: row.date.getTime(),
              weight: row.weight,
              height: row.height,
              bmi: row.bmi,
              bodyFatPercentage: row.body_fat_percentage,
              muscleMass: row.muscle_mass,
              notes: row.notes || '',
              createdAt: row.created_at.getTime(),
              updatedAt: row.updated_at.getTime()
            });
          }
        } catch (error) {
          syncResults.errors.push(`Failed to sync parameter: ${error.message}`);
        }
      }
    }

    // Sync measurements if provided
    if (measurements && Array.isArray(measurements)) {
      for (const measurement of measurements) {
        try {
          const validationErrors = validateBodyMeasurement(measurement);
          if (validationErrors.length > 0) {
            syncResults.errors.push(`Measurement validation failed: ${validationErrors.join(', ')}`);
            continue;
          }

          const { id, parametersId, measurementType, value, unit = 'cm' } = measurement;

          // Verify parameters ownership
          const parametersCheck = await query(
            'SELECT id FROM physical_parameters WHERE id = $1 AND user_id = $2',
            [parametersId, userId]
          );

          if (parametersCheck.rows.length === 0) {
            syncResults.errors.push(`Parameters ${parametersId} not found or not owned by user`);
            continue;
          }

          let result;
          if (id) {
            // Update existing
            result = await query(
              `UPDATE body_measurements 
               SET measurement_type = $2, value = $3, unit = $4, updated_at = CURRENT_TIMESTAMP
               WHERE id = $1 AND parameters_id = $5
               RETURNING id, measurement_type, value, unit, created_at, updated_at`,
              [id, measurementType, value, unit, parametersId]
            );
          } else {
            // Create new
            result = await query(
              `INSERT INTO body_measurements 
               (parameters_id, measurement_type, value, unit) 
               VALUES ($1, $2, $3, $4)
               RETURNING id, measurement_type, value, unit, created_at, updated_at`,
              [parametersId, measurementType, value, unit]
            );
          }

          if (result.rows.length > 0) {
            const row = result.rows[0];
            syncResults.measurements.push({
              id: row.id,
              parametersId: parametersId,
              measurementType: row.measurement_type,
              value: row.value,
              unit: row.unit,
              createdAt: row.created_at.getTime(),
              updatedAt: row.updated_at.getTime()
            });
          }
        } catch (error) {
          syncResults.errors.push(`Failed to sync measurement: ${error.message}`);
        }
      }
    }

    console.log('[BODY_SYNC] ✅ Sync completed:', {
      parametersSynced: syncResults.parameters.length,
      measurementsSynced: syncResults.measurements.length,
      errors: syncResults.errors.length
    });

    res.json({
      success: true,
      ...syncResults
    });

  } catch (error) {
    console.error('[BODY_SYNC] ❌ Error syncing body data:', error);
    res.status(500).json({
      error: 'Failed to sync body data',
      message: 'Internal server error'
    });
  }
});

module.exports = router;
