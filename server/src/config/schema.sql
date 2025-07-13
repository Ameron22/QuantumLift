-- QuantumLift Database Schema (UUID version)
-- Enable pgcrypto for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    profile_picture VARCHAR(255),
    bio TEXT,
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Friend invitations table
CREATE TABLE IF NOT EXISTS friend_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID REFERENCES users(id) ON DELETE CASCADE,
    recipient_email VARCHAR(100) NOT NULL,
    invitation_code VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED')),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Friend connections table (for accepted invitations)
CREATE TABLE IF NOT EXISTS friend_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    friend_id UUID REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'ACCEPTED' CHECK (status IN ('ACCEPTED', 'BLOCKED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, friend_id)
);

-- Feed posts table
CREATE TABLE IF NOT EXISTS feed_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_type VARCHAR(50) NOT NULL CHECK (post_type IN ('WORKOUT_COMPLETED', 'ACHIEVEMENT', 'CHALLENGE', 'TEXT_POST')),
    content TEXT NOT NULL,
    workout_data JSONB, -- For workout posts: exercises, duration, etc.
    achievement_data JSONB, -- For achievement posts: type, value, etc.
    challenge_data JSONB, -- For challenge posts: challenge type, participants, etc.
    privacy_level VARCHAR(20) DEFAULT 'FRIENDS' CHECK (privacy_level IN ('PUBLIC', 'FRIENDS', 'PRIVATE')),
    likes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Post likes table
CREATE TABLE IF NOT EXISTS post_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)
);

-- Post comments table
CREATE TABLE IF NOT EXISTS post_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User privacy settings table
CREATE TABLE IF NOT EXISTS user_privacy_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    auto_share_workouts BOOLEAN DEFAULT true,
    auto_share_achievements BOOLEAN DEFAULT true,
    default_post_privacy VARCHAR(20) DEFAULT 'FRIENDS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

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

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_friend_invitations_email ON friend_invitations(recipient_email);
CREATE INDEX IF NOT EXISTS idx_friend_invitations_code ON friend_invitations(invitation_code);
CREATE INDEX IF NOT EXISTS idx_friend_connections_user_id ON friend_connections(user_id);
CREATE INDEX IF NOT EXISTS idx_friend_connections_friend_id ON friend_connections(friend_id); 
CREATE INDEX IF NOT EXISTS idx_feed_posts_user_id ON feed_posts(user_id);
CREATE INDEX IF NOT EXISTS idx_feed_posts_created_at ON feed_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feed_posts_privacy ON feed_posts(privacy_level);
CREATE INDEX IF NOT EXISTS idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_post_comments_post_id ON post_comments(post_id);
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