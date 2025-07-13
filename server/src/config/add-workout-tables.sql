-- Migration: Add workout-related tables
-- This adds the missing tables needed for workout sharing functionality

-- Workouts table
CREATE TABLE IF NOT EXISTS workouts (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    difficulty VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Exercises table (for both standard and custom exercises)
CREATE TABLE IF NOT EXISTS exercises (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    equipment VARCHAR(100),
    body_part VARCHAR(100),
    target_muscle VARCHAR(100),
    instructions TEXT,
    gif_url VARCHAR(500),
    is_custom BOOLEAN DEFAULT false,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE, -- NULL for standard exercises
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Workout exercises junction table
CREATE TABLE IF NOT EXISTS workout_exercises (
    id SERIAL PRIMARY KEY,
    workout_id INTEGER NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
    exercise_id INTEGER REFERENCES exercises(id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL DEFAULT 0,
    is_custom_exercise BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Workout sessions table (for tracking completed workouts)
CREATE TABLE IF NOT EXISTS workout_sessions (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workout_id INTEGER REFERENCES workouts(id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration INTEGER, -- in milliseconds
    total_sets INTEGER DEFAULT 0,
    total_weight DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Exercise sets table (for tracking individual exercise performance)
CREATE TABLE IF NOT EXISTS exercise_sets (
    id SERIAL PRIMARY KEY,
    session_id INTEGER NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    set_number INTEGER NOT NULL,
    reps INTEGER,
    weight DECIMAL(8,2),
    duration INTEGER, -- in milliseconds
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for workout tables
CREATE INDEX IF NOT EXISTS idx_workouts_user_id ON workouts(user_id);
CREATE INDEX IF NOT EXISTS idx_workouts_created_at ON workouts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_exercises_user_id ON exercises(user_id);
CREATE INDEX IF NOT EXISTS idx_exercises_is_custom ON exercises(is_custom);
CREATE INDEX IF NOT EXISTS idx_workout_exercises_workout_id ON workout_exercises(workout_id);
CREATE INDEX IF NOT EXISTS idx_workout_exercises_order ON workout_exercises(order_index);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_user_id ON workout_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_workout_id ON workout_sessions(workout_id);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_start_time ON workout_sessions(start_time DESC);
CREATE INDEX IF NOT EXISTS idx_exercise_sets_session_id ON exercise_sets(session_id);
CREATE INDEX IF NOT EXISTS idx_exercise_sets_exercise_id ON exercise_sets(exercise_id);

-- Insert some standard exercises for testing
INSERT INTO exercises (name, equipment, body_part, target_muscle, instructions, is_custom) VALUES
('Bench Press', 'Barbell', 'Chest', 'Pectoralis Major', 'Lie on bench, lower bar to chest, press up', false),
('Squat', 'Barbell', 'Legs', 'Quadriceps', 'Stand with bar on shoulders, squat down, stand up', false),
('Deadlift', 'Barbell', 'Back', 'Erector Spinae', 'Stand with bar on ground, lift with straight back', false),
('Pull-up', 'Bodyweight', 'Back', 'Latissimus Dorsi', 'Hang from bar, pull body up', false),
('Push-up', 'Bodyweight', 'Chest', 'Pectoralis Major', 'Plank position, lower body, push up', false),
('Lunge', 'Bodyweight', 'Legs', 'Quadriceps', 'Step forward, lower back knee, return', false),
('Plank', 'Bodyweight', 'Core', 'Abdominals', 'Hold plank position', false),
('Bicep Curl', 'Dumbbell', 'Arms', 'Biceps', 'Hold dumbbells, curl up and down', false),
('Tricep Dip', 'Bodyweight', 'Arms', 'Triceps', 'Dip on parallel bars', false),
('Shoulder Press', 'Dumbbell', 'Shoulders', 'Deltoids', 'Press dumbbells overhead', false)
ON CONFLICT DO NOTHING; 