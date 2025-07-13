-- Migration: Add workout sharing support
-- Add workout_share_data column to feed_posts table
ALTER TABLE feed_posts 
ADD COLUMN IF NOT EXISTS workout_share_data JSONB;

-- Update post_type check constraint to include WORKOUT_SHARED
ALTER TABLE feed_posts 
DROP CONSTRAINT IF EXISTS feed_posts_post_type_check;

ALTER TABLE feed_posts 
ADD CONSTRAINT feed_posts_post_type_check 
CHECK (post_type IN ('WORKOUT_COMPLETED', 'ACHIEVEMENT', 'CHALLENGE', 'TEXT_POST', 'WORKOUT_SHARED'));

-- Add index for workout sharing queries
CREATE INDEX IF NOT EXISTS idx_feed_posts_workout_share_data ON feed_posts USING GIN (workout_share_data);

-- Add index for finding shared workouts by workoutId
CREATE INDEX IF NOT EXISTS idx_feed_posts_workout_share_workout_id ON feed_posts ((workout_share_data->>'workoutId'));

-- Add index for finding expired shared workouts
CREATE INDEX IF NOT EXISTS idx_feed_posts_workout_share_expires_at ON feed_posts ((workout_share_data->>'expiresAt'));

-- Create a function to clean up expired shared workouts
CREATE OR REPLACE FUNCTION cleanup_expired_shared_workouts()
RETURNS void AS $$
BEGIN
    DELETE FROM feed_posts 
    WHERE post_type = 'WORKOUT_SHARED' 
    AND workout_share_data->>'expiresAt' < NOW()::text;
END;
$$ LANGUAGE plpgsql;

-- Create a scheduled job to clean up expired shared workouts (runs daily)
-- Note: This requires pg_cron extension which may not be available on all hosting platforms
-- For now, we'll handle cleanup manually or through application logic
-- SELECT cron.schedule('cleanup-expired-workouts', '0 0 * * *', 'SELECT cleanup_expired_shared_workouts();'); 