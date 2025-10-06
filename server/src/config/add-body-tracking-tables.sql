-- Add body tracking tables for physical parameters and body measurements
-- This extends the existing schema with cloud sync capabilities for body data

-- Physical Parameters table (matches Android PhysicalParameters entity)
CREATE TABLE IF NOT EXISTS physical_parameters (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date TIMESTAMP NOT NULL,
    weight DECIMAL(5,2), -- in kg
    height DECIMAL(5,2), -- in cm
    bmi DECIMAL(4,2), -- calculated
    body_fat_percentage DECIMAL(4,2), -- optional
    muscle_mass DECIMAL(5,2), -- optional
    notes TEXT DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Body Measurements table (matches Android BodyMeasurement entity)
CREATE TABLE IF NOT EXISTS body_measurements (
    id SERIAL PRIMARY KEY,
    parameters_id INTEGER NOT NULL REFERENCES physical_parameters(id) ON DELETE CASCADE,
    measurement_type VARCHAR(50) NOT NULL, -- e.g., "chest", "waist", "biceps", "thighs", "calves", "neck", "shoulders"
    value DECIMAL(6,2) NOT NULL, -- in cm
    unit VARCHAR(10) DEFAULT 'cm',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_physical_parameters_user_id ON physical_parameters(user_id);
CREATE INDEX IF NOT EXISTS idx_physical_parameters_date ON physical_parameters(date DESC);
CREATE INDEX IF NOT EXISTS idx_body_measurements_parameters_id ON body_measurements(parameters_id);
CREATE INDEX IF NOT EXISTS idx_body_measurements_type ON body_measurements(measurement_type);

-- Add unique constraint to prevent duplicate entries for same user and date
CREATE UNIQUE INDEX IF NOT EXISTS idx_physical_parameters_user_date 
ON physical_parameters(user_id, DATE(date));

-- Add comments for documentation
COMMENT ON TABLE physical_parameters IS 'Stores user physical parameters (weight, height, BMI, etc.) with timestamps';
COMMENT ON TABLE body_measurements IS 'Stores detailed body measurements linked to physical parameters entries';
COMMENT ON COLUMN physical_parameters.weight IS 'Weight in kilograms';
COMMENT ON COLUMN physical_parameters.height IS 'Height in centimeters';
COMMENT ON COLUMN physical_parameters.bmi IS 'Body Mass Index (calculated)';
COMMENT ON COLUMN physical_parameters.body_fat_percentage IS 'Body fat percentage (optional)';
COMMENT ON COLUMN physical_parameters.muscle_mass IS 'Muscle mass in kg (optional)';
COMMENT ON COLUMN body_measurements.measurement_type IS 'Type of measurement: chest, waist, biceps, thighs, calves, neck, shoulders, forearms, hips';
COMMENT ON COLUMN body_measurements.value IS 'Measurement value in the specified unit';
