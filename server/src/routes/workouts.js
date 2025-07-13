const express = require('express');
const { query } = require('../config/database');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

// Helper: Validate UUID format
function isValidUUID(uuid) {
  return /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(uuid);
}

// Helper: Get achievement display name
function getAchievementName(id) {
  switch(id) {
    case 'first_workout': return '🏆 First Workout';
    case 'workout_warrior': return '💪 Workout Warrior';
    case 'workout_master': return '🌟 Workout Master';
    case 'bench_press_100': return '💯 Centurion';
    case 'consistency_week': return '📅 Week Warrior';
    case 'consistency_month': return '🗓️ Monthly Master';
    case 'night_owl': return '🦉 Night Owl';
    default: return id;
  }
}

// Complete workout and optionally share to feed
router.post('/complete', authenticateToken, async (req, res) => {
  try {
    const { 
      workoutId, 
      workoutName, 
      duration, 
      exercises, 
      totalSets, 
      totalWeight,
      achievements = [],
      shareToFeed = false,
      privacyLevel = 'FRIENDS'
    } = req.body;
    
    const userId = req.user.userId;

    console.log('[WORKOUT_COMPLETE] 📝 Workout completion request:', {
      userId,
      workoutId,
      workoutName,
      duration,
      exercisesCount: exercises?.length,
      totalSets,
      totalWeight,
      shareToFeed,
      privacyLevel
    });

    // Validate required fields
    if (!workoutId || !workoutName || !duration) {
      return res.status(400).json({
        error: 'Missing required fields',
        message: 'Workout ID, name, and duration are required'
      });
    }

    // Validate privacy level
    const validPrivacyLevels = ['PUBLIC', 'FRIENDS', 'PRIVATE'];
    if (!validPrivacyLevels.includes(privacyLevel)) {
      return res.status(400).json({
        error: 'Invalid privacy level',
        message: 'Privacy level must be PUBLIC, FRIENDS, or PRIVATE'
      });
    }

    // Check user's auto-share settings
    let autoShareEnabled = true; // Default to true for new users
    let defaultPrivacy = 'FRIENDS';
    
    try {
      const privacySettings = await query(
        'SELECT auto_share_workouts, default_post_privacy FROM user_privacy_settings WHERE user_id = $1',
        [userId]
      );

      if (privacySettings.rows.length > 0) {
        autoShareEnabled = privacySettings.rows[0].auto_share_workouts;
        defaultPrivacy = privacySettings.rows[0].default_post_privacy || 'FRIENDS';
      } else {
        // Create default settings for new users
        await query(
          'INSERT INTO user_privacy_settings (user_id, auto_share_workouts, default_post_privacy) VALUES ($1, $2, $3)',
          [userId, true, 'FRIENDS']
        );
        console.log('[WORKOUT_COMPLETE] ✅ Created default privacy settings for new user');
      }
    } catch (error) {
      console.log('[WORKOUT_COMPLETE] ⚠️ Could not fetch privacy settings:', error.message);
      // Continue with defaults (autoShareEnabled = true)
    }

    // Determine if we should share to feed
    const shouldShare = shareToFeed || autoShareEnabled;
    const finalPrivacyLevel = shareToFeed ? privacyLevel : defaultPrivacy;

    console.log('[WORKOUT_COMPLETE] 🔐 Privacy settings:', {
      autoShareEnabled,
      defaultPrivacy,
      shouldShare,
      finalPrivacyLevel
    });

    if (shouldShare) {
      // Create feed post for workout completion
      const workoutData = {
        duration: duration,
        exercises: exercises || [],
        totalSets: totalSets || 0,
        totalWeight: totalWeight || 0,
        workoutId: workoutId,
        workoutName: workoutName,
        achievements: achievements || []
      };

      // Build achievement text if any achievements were unlocked
      let achievementText = '';
      if (achievements && achievements.length > 0) {
        const achievementDetails = achievements.map(achievement => {
          const achievementName = getAchievementName(achievement.id);
          if (achievement.additionalInfo) {
            return `${achievementName}: ${achievement.additionalInfo}`;
          } else {
            return achievementName;
          }
        });
        achievementText = `\n🎉 Achievements unlocked:\n${achievementDetails.join('\n')}`;
      }

      // Format duration appropriately
      let durationText;
      const durationInSeconds = Math.round(duration / 1000);
      if (durationInSeconds < 60) {
        durationText = `${durationInSeconds} seconds`;
      } else {
        const durationInMinutes = Math.round(duration / (60 * 1000));
        durationText = `${durationInMinutes} minutes`;
      }

      const content = `Just completed ${workoutName}! 💪\nDuration: ${durationText}\nExercises: ${exercises?.length || 0}\nTotal sets: ${totalSets || 0}${achievementText}`;

      try {
        const postResult = await query(
          `INSERT INTO feed_posts 
           (user_id, post_type, content, workout_data, privacy_level) 
           VALUES ($1, $2, $3, $4, $5) 
           RETURNING id`,
          [userId, 'WORKOUT_COMPLETED', content, JSON.stringify(workoutData), finalPrivacyLevel]
        );

        console.log('[WORKOUT_COMPLETE] ✅ Feed post created:', postResult.rows[0].id);
      } catch (error) {
        console.error('[WORKOUT_COMPLETE] ❌ Error creating feed post:', error);
        // Don't fail the workout completion if feed post fails
      }
    }

    res.json({
      message: 'Workout completed successfully',
      shared: shouldShare,
      privacyLevel: finalPrivacyLevel
    });

  } catch (error) {
    console.error('[WORKOUT_COMPLETE] ❌ Error completing workout:', error);
    res.status(500).json({
      error: 'Failed to complete workout',
      message: 'Internal server error'
    });
  }
});

// Get user's privacy settings
router.get('/privacy-settings', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const result = await query(
      'SELECT auto_share_workouts, default_post_privacy FROM user_privacy_settings WHERE user_id = $1',
      [userId]
    );

    if (result.rows.length === 0) {
      // Create default settings if none exist
      await query(
        'INSERT INTO user_privacy_settings (user_id, auto_share_workouts, default_post_privacy) VALUES ($1, $2, $3)',
        [userId, true, 'FRIENDS']
      );

      res.json({
        autoShareWorkouts: true,
        defaultPostPrivacy: 'FRIENDS'
      });
    } else {
      const settings = result.rows[0];
      res.json({
        autoShareWorkouts: settings.auto_share_workouts,
        defaultPostPrivacy: settings.default_post_privacy
      });
    }

  } catch (error) {
    console.error('[PRIVACY_SETTINGS] ❌ Error fetching privacy settings:', error);
    res.status(500).json({
      error: 'Failed to fetch privacy settings',
      message: 'Internal server error'
    });
  }
});

// Update user's privacy settings
router.put('/privacy-settings', authenticateToken, async (req, res) => {
  try {
    const { autoShareWorkouts, defaultPostPrivacy } = req.body;
    const userId = req.user.userId;

    // Validate privacy level
    const validPrivacyLevels = ['PUBLIC', 'FRIENDS', 'PRIVATE'];
    if (defaultPostPrivacy && !validPrivacyLevels.includes(defaultPostPrivacy)) {
      return res.status(400).json({
        error: 'Invalid privacy level',
        message: 'Privacy level must be PUBLIC, FRIENDS, or PRIVATE'
      });
    }

    // Upsert privacy settings
    await query(
      `INSERT INTO user_privacy_settings (user_id, auto_share_workouts, default_post_privacy) 
       VALUES ($1, $2, $3) 
       ON CONFLICT (user_id) 
       DO UPDATE SET 
         auto_share_workouts = EXCLUDED.auto_share_workouts,
         default_post_privacy = EXCLUDED.default_post_privacy,
         updated_at = CURRENT_TIMESTAMP`,
      [userId, autoShareWorkouts, defaultPostPrivacy]
    );

    res.json({
      message: 'Privacy settings updated successfully',
      autoShareWorkouts,
      defaultPostPrivacy
    });

  } catch (error) {
    console.error('[PRIVACY_SETTINGS] ❌ Error updating privacy settings:', error);
    res.status(500).json({
      error: 'Failed to update privacy settings',
      message: 'Internal server error'
    });
  }
});

// Share workout with friends
router.post('/share', authenticateToken, async (req, res) => {
  try {
    const { workoutId, targetUserIds, shareType = 'DIRECT' } = req.body;
    const userId = req.user.userId;

    console.log('[WORKOUT_SHARE] 📤 Workout share request:', {
      userId,
      workoutId,
      targetUserIds,
      shareType
    });

    // Validate required fields
    if (!workoutId || !targetUserIds || !Array.isArray(targetUserIds) || targetUserIds.length === 0) {
      return res.status(400).json({
        error: 'Missing required fields',
        message: 'Workout ID and target user IDs are required'
      });
    }

    // Get user info
    const userResult = await query(
      'SELECT username FROM users WHERE id = $1',
      [userId]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({
        error: 'User not found',
        message: 'User does not exist'
      });
    }

    const creatorUsername = userResult.rows[0].username;

    // Get workout data (template only - no sets/reps/weights)
    const workoutResult = await query(
      `SELECT w.name as workout_name, w.difficulty
       FROM workouts w 
       WHERE w.id = $1 AND w.user_id = $2`,
      [workoutId, userId]
    );

    if (workoutResult.rows.length === 0) {
      return res.status(404).json({
        error: 'Workout not found',
        message: 'Workout does not exist or you do not have permission to share it'
      });
    }

    const workout = workoutResult.rows[0];

    // Get exercises for this workout (template only)
    const exercisesResult = await query(
      `SELECT 
         we.exercise_id,
         we.is_custom_exercise,
         e.name as exercise_name,
         e.equipment,
         e.body_part,
         e.target_muscle,
         e.instructions,
         e.gif_url
       FROM workout_exercises we
       LEFT JOIN exercises e ON we.exercise_id = e.id
       WHERE we.workout_id = $1
       ORDER BY we.order_index`,
      [workoutId]
    );

    const exercises = exercisesResult.rows.map(row => ({
      exerciseId: row.exercise_id,
      exerciseName: row.exercise_name,
      isCustomExercise: row.is_custom_exercise,
      equipment: row.equipment,
      bodyPart: row.body_part,
      targetMuscle: row.target_muscle,
      instructions: row.instructions,
      gifUrl: row.gif_url
    }));

    // Create shared workout data
    const sharedWorkoutData = {
      workoutId: `shared_${Date.now()}_${userId}_${workoutId}`,
      workoutTitle: workout.workout_name,
      creatorId: userId,
      creatorUsername: creatorUsername,
      exercises: exercises,
      difficulty: workout.difficulty,
      createdAt: new Date().toISOString(),
      expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString() // 7 days
    };

    // Create feed posts for each target user
    const postPromises = targetUserIds.map(async (targetUserId) => {
      try {
        // Verify the target user exists and is a friend
        const friendCheck = await query(
          `SELECT 1 FROM friendships 
           WHERE (user_id = $1 AND friend_id = $2) OR (user_id = $2 AND friend_id = $1)
           AND status = 'accepted'`,
          [userId, targetUserId]
        );

        if (friendCheck.rows.length === 0) {
          console.log(`[WORKOUT_SHARE] ⚠️ User ${targetUserId} is not a friend of ${userId}`);
          return null;
        }

        const content = `${creatorUsername} shared a workout with you: "${workout.workout_name}" 💪`;

        const postResult = await query(
          `INSERT INTO feed_posts 
           (user_id, post_type, content, workout_share_data, privacy_level) 
           VALUES ($1, $2, $3, $4, $5) 
           RETURNING id`,
          [targetUserId, 'WORKOUT_SHARED', content, JSON.stringify(sharedWorkoutData), 'PRIVATE']
        );

        console.log(`[WORKOUT_SHARE] ✅ Shared workout post created for user ${targetUserId}:`, postResult.rows[0].id);
        return postResult.rows[0].id;
      } catch (error) {
        console.error(`[WORKOUT_SHARE] ❌ Error creating share post for user ${targetUserId}:`, error);
        return null;
      }
    });

    const postResults = await Promise.all(postPromises);
    const successfulShares = postResults.filter(id => id !== null);

    if (successfulShares.length === 0) {
      return res.status(400).json({
        error: 'No valid shares',
        message: 'No valid friends found to share with'
      });
    }

    res.json({
      success: true,
      message: `Workout shared successfully with ${successfulShares.length} friend(s)`,
      sharedWorkoutId: sharedWorkoutData.workoutId,
      sharesCount: successfulShares.length
    });

  } catch (error) {
    console.error('[WORKOUT_SHARE] ❌ Error sharing workout:', error);
    res.status(500).json({
      error: 'Failed to share workout',
      message: 'Internal server error'
    });
  }
});

// Copy shared workout
router.post('/copy', authenticateToken, async (req, res) => {
  try {
    const { sharedWorkoutId, targetUserId } = req.body;
    const userId = req.user.userId;

    console.log('[WORKOUT_COPY] 📋 Copy workout request:', {
      userId,
      sharedWorkoutId,
      targetUserId
    });

    // Validate that the user is copying their own shared workout
    if (userId !== targetUserId) {
      return res.status(403).json({
        error: 'Unauthorized',
        message: 'You can only copy workouts shared with you'
      });
    }

    // Find the shared workout post
    const postResult = await query(
      `SELECT workout_share_data, user_id 
       FROM feed_posts 
       WHERE post_type = 'WORKOUT_SHARED' 
       AND workout_share_data->>'workoutId' = $1
       AND user_id = $2`,
      [sharedWorkoutId, userId]
    );

    if (postResult.rows.length === 0) {
      return res.status(404).json({
        error: 'Shared workout not found',
        message: 'The shared workout was not found or has expired'
      });
    }

    const post = postResult.rows[0];
    const sharedWorkoutData = JSON.parse(post.workout_share_data);

    // Check if workout has expired (7 days)
    const expiresAt = new Date(sharedWorkoutData.expiresAt);
    if (new Date() > expiresAt) {
      return res.status(410).json({
        error: 'Workout expired',
        message: 'This shared workout has expired'
      });
    }

    // Create new workout for the user
    const newWorkoutResult = await query(
      `INSERT INTO workouts (user_id, name, difficulty) 
       VALUES ($1, $2, $3) 
       RETURNING id`,
      [userId, sharedWorkoutData.workoutTitle, sharedWorkoutData.difficulty]
    );

    const newWorkoutId = newWorkoutResult.rows[0].id;

    // Copy exercises to the new workout
    const exercisePromises = sharedWorkoutData.exercises.map(async (exercise, index) => {
      try {
        if (exercise.isCustomExercise) {
          // For custom exercises, we need to create the exercise first
          const customExerciseResult = await query(
            `INSERT INTO exercises (name, equipment, body_part, target_muscle, instructions, gif_url, is_custom, user_id) 
             VALUES ($1, $2, $3, $4, $5, $6, true, $7) 
             RETURNING id`,
            [exercise.exerciseName, exercise.equipment, exercise.bodyPart, exercise.targetMuscle, exercise.instructions, exercise.gifUrl, userId]
          );
          
          const customExerciseId = customExerciseResult.rows[0].id;
          
          // Add to workout
          await query(
            `INSERT INTO workout_exercises (workout_id, exercise_id, order_index, is_custom_exercise) 
             VALUES ($1, $2, $3, true)`,
            [newWorkoutId, customExerciseId, index]
          );
        } else {
          // For standard exercises, just add to workout
          await query(
            `INSERT INTO workout_exercises (workout_id, exercise_id, order_index, is_custom_exercise) 
             VALUES ($1, $2, $3, false)`,
            [newWorkoutId, exercise.exerciseId, index]
          );
        }
      } catch (error) {
        console.error(`[WORKOUT_COPY] ❌ Error copying exercise ${exercise.exerciseName}:`, error);
        throw error;
      }
    });

    await Promise.all(exercisePromises);

    // Delete the shared workout post after successful copy
    await query(
      'DELETE FROM feed_posts WHERE id = $1',
      [post.id]
    );

    console.log(`[WORKOUT_COPY] ✅ Workout copied successfully: ${newWorkoutId}`);

    res.json({
      success: true,
      message: 'Workout copied successfully',
      newWorkoutId: newWorkoutId,
      workoutName: sharedWorkoutData.workoutTitle
    });

  } catch (error) {
    console.error('[WORKOUT_COPY] ❌ Error copying workout:', error);
    res.status(500).json({
      error: 'Failed to copy workout',
      message: 'Internal server error'
    });
  }
});

module.exports = router; 