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
    let autoShareEnabled = false;
    let defaultPrivacy = 'FRIENDS';
    
    try {
      const privacySettings = await query(
        'SELECT auto_share_workouts, default_post_privacy FROM user_privacy_settings WHERE user_id = $1',
        [userId]
      );

      if (privacySettings.rows.length > 0) {
        autoShareEnabled = privacySettings.rows[0].auto_share_workouts;
        defaultPrivacy = privacySettings.rows[0].default_post_privacy || 'FRIENDS';
      }
    } catch (error) {
      console.log('[WORKOUT_COMPLETE] ⚠️ Could not fetch privacy settings:', error.message);
      // Continue with defaults
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

      const content = `Just completed ${workoutName}! 💪\nDuration: ${Math.round(duration / (60 * 1000))} minutes\nExercises: ${exercises?.length || 0}\nTotal sets: ${totalSets || 0}${achievementText}`;

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

module.exports = router; 