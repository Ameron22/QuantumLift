const express = require('express');
const router = express.Router();
const { authenticateToken } = require('../middleware/auth');
const { query } = require('../config/database');

// Get feed posts for current user (friends + public posts)
router.get('/posts', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { page = 1, limit = 20 } = req.query;
    const offset = (page - 1) * limit;

    // Get posts from friends and public posts
    const result = await query(`
      SELECT 
        fp.id,
        fp.post_type,
        fp.content,
        fp.workout_data,
        fp.achievement_data,
        fp.challenge_data,
        fp.privacy_level,
        fp.likes_count,
        fp.comments_count,
        fp.created_at,
        u.id as user_id,
        u.username,
        u.profile_picture,
        EXISTS(SELECT 1 FROM post_likes pl WHERE pl.post_id = fp.id AND pl.user_id = $1) as is_liked_by_user
      FROM feed_posts fp
      JOIN users u ON fp.user_id = u.id
      WHERE (
        fp.privacy_level = 'PUBLIC' 
        OR fp.user_id = $1
        OR (
          fp.privacy_level = 'FRIENDS' 
          AND (
            EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id = $1 AND fc.friend_id = fp.user_id)
            OR EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id = fp.user_id AND fc.friend_id = $1)
          )
        )
      )
      ORDER BY fp.created_at DESC
      LIMIT $2 OFFSET $3
    `, [userId, limit, offset]);

    const posts = result.rows.map(row => ({
      id: row.id,
      postType: row.post_type,
      content: row.content,
      workoutData: row.workout_data,
      achievementData: row.achievement_data,
      challengeData: row.challenge_data,
      privacyLevel: row.privacy_level,
      likesCount: row.likes_count,
      commentsCount: row.comments_count,
      createdAt: row.created_at,
      user: {
        id: row.user_id,
        username: row.username,
        profilePicture: row.profile_picture
      },
      isLikedByUser: row.is_liked_by_user
    }));

    res.json({
      posts,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        hasMore: posts.length === parseInt(limit)
      }
    });

  } catch (error) {
    console.error('Get feed posts error:', error);
    res.status(500).json({ 
      error: 'Failed to get feed posts',
      message: 'Internal server error' 
    });
  }
});

// Create a new post
router.post('/posts', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { 
      postType, 
      content, 
      workoutData, 
      achievementData, 
      challengeData, 
      privacyLevel = 'FRIENDS' 
    } = req.body;

    // Validate post type
    const validPostTypes = ['WORKOUT_COMPLETED', 'ACHIEVEMENT', 'CHALLENGE', 'TEXT_POST'];
    if (!validPostTypes.includes(postType)) {
      return res.status(400).json({
        error: 'Invalid post type',
        message: 'Post type must be one of: WORKOUT_COMPLETED, ACHIEVEMENT, CHALLENGE, TEXT_POST'
      });
    }

    // Validate privacy level
    const validPrivacyLevels = ['PUBLIC', 'FRIENDS', 'PRIVATE'];
    if (!validPrivacyLevels.includes(privacyLevel)) {
      return res.status(400).json({
        error: 'Invalid privacy level',
        message: 'Privacy level must be one of: PUBLIC, FRIENDS, PRIVATE'
      });
    }

    // Validate content
    if (!content || content.trim().length === 0) {
      return res.status(400).json({
        error: 'Content required',
        message: 'Post content cannot be empty'
      });
    }

    const result = await query(`
      INSERT INTO feed_posts (user_id, post_type, content, workout_data, achievement_data, challenge_data, privacy_level)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING id, created_at
    `, [userId, postType, content, workoutData, achievementData, challengeData, privacyLevel]);

    const post = result.rows[0];

    res.status(201).json({
      message: 'Post created successfully',
      post: {
        id: post.id,
        createdAt: post.created_at
      }
    });

  } catch (error) {
    console.error('Create post error:', error);
    res.status(500).json({ 
      error: 'Failed to create post',
      message: 'Internal server error' 
    });
  }
});

// Like/unlike a post
router.post('/posts/:postId/like', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { postId } = req.params;

    // Check if post exists and user can see it
    const postCheck = await query(`
      SELECT id, user_id, privacy_level FROM feed_posts 
      WHERE id = $1 AND (
        privacy_level = 'PUBLIC' 
        OR user_id = $2
        OR (
          privacy_level = 'FRIENDS' 
          AND (
            EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id = $2 AND fc.friend_id = user_id)
            OR EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id = user_id AND fc.friend_id = $2)
          )
        )
      )
    `, [postId, userId]);

    if (postCheck.rows.length === 0) {
      return res.status(404).json({
        error: 'Post not found',
        message: 'Post does not exist or you do not have permission to view it'
      });
    }

    // Check if already liked
    const existingLike = await query(
      'SELECT id FROM post_likes WHERE post_id = $1 AND user_id = $2',
      [postId, userId]
    );

    if (existingLike.rows.length > 0) {
      // Unlike the post
      await query(
        'DELETE FROM post_likes WHERE post_id = $1 AND user_id = $2',
        [postId, userId]
      );
      
      await query(
        'UPDATE feed_posts SET likes_count = likes_count - 1 WHERE id = $1',
        [postId]
      );

      res.json({
        message: 'Post unliked successfully',
        liked: false
      });
    } else {
      // Like the post
      await query(
        'INSERT INTO post_likes (post_id, user_id) VALUES ($1, $2)',
        [postId, userId]
      );
      
      await query(
        'UPDATE feed_posts SET likes_count = likes_count + 1 WHERE id = $1',
        [postId]
      );

      res.json({
        message: 'Post liked successfully',
        liked: true
      });
    }

  } catch (error) {
    console.error('Like/unlike post error:', error);
    res.status(500).json({ 
      error: 'Failed to like/unlike post',
      message: 'Internal server error' 
    });
  }
});

// Get comments for a post
router.get('/posts/:postId/comments', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { postId } = req.params;
    const { page = 1, limit = 20 } = req.query;
    const offset = (page - 1) * limit;

    // Check if post exists and user can see it
    const postCheck = await query(`
      SELECT id FROM feed_posts 
      WHERE id = $1 AND (
        privacy_level = 'PUBLIC' 
        OR user_id = $2
        OR (
          privacy_level = 'FRIENDS' 
          AND (
            EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id = $2 AND fc.friend_id = user_id)
            OR EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id = user_id AND fc.friend_id = $2)
          )
        )
      )
    `, [postId, userId]);

    if (postCheck.rows.length === 0) {
      return res.status(404).json({
        error: 'Post not found',
        message: 'Post does not exist or you do not have permission to view it'
      });
    }

    const result = await query(`
      SELECT 
        pc.id,
        pc.content,
        pc.created_at,
        u.id as user_id,
        u.username,
        u.profile_picture
      FROM post_comments pc
      JOIN users u ON pc.user_id = u.id
      WHERE pc.post_id = $1
      ORDER BY pc.created_at ASC
      LIMIT $2 OFFSET $3
    `, [postId, limit, offset]);

    const comments = result.rows.map(row => ({
      id: row.id,
      content: row.content,
      createdAt: row.created_at,
      user: {
        id: row.user_id,
        username: row.username,
        profilePicture: row.profile_picture
      }
    }));

    res.json({
      comments,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        hasMore: comments.length === parseInt(limit)
      }
    });

  } catch (error) {
    console.error('Get comments error:', error);
    res.status(500).json({ 
      error: 'Failed to get comments',
      message: 'Internal server error' 
    });
  }
});

// Add a comment to a post
router.post('/posts/:postId/comments', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { postId } = req.params;
    const { content } = req.body;

    if (!content || content.trim().length === 0) {
      return res.status(400).json({
        error: 'Content required',
        message: 'Comment content cannot be empty'
      });
    }

    // Check if post exists and user can see it
    const postCheck = await query(`
      SELECT id FROM feed_posts 
      WHERE id = $1 AND (
        privacy_level = 'PUBLIC' 
        OR user_id = $2
        OR (
          privacy_level = 'FRIENDS' 
          AND (
            EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id = $2 AND fc.friend_id = user_id)
            OR EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id = user_id AND fc.friend_id = $2)
          )
        )
      )
    `, [postId, userId]);

    if (postCheck.rows.length === 0) {
      return res.status(404).json({
        error: 'Post not found',
        message: 'Post does not exist or you do not have permission to comment on it'
      });
    }

    // Add comment
    const commentResult = await query(`
      INSERT INTO post_comments (post_id, user_id, content)
      VALUES ($1, $2, $3)
      RETURNING id, created_at
    `, [postId, userId, content]);

    // Update comment count
    await query(
      'UPDATE feed_posts SET comments_count = comments_count + 1 WHERE id = $1',
      [postId]
    );

    const comment = commentResult.rows[0];

    res.status(201).json({
      message: 'Comment added successfully',
      comment: {
        id: comment.id,
        createdAt: comment.created_at
      }
    });

  } catch (error) {
    console.error('Add comment error:', error);
    res.status(500).json({ 
      error: 'Failed to add comment',
      message: 'Internal server error' 
    });
  }
});

// Get user's privacy settings
router.get('/privacy-settings', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const result = await query(`
      SELECT auto_share_workouts, auto_share_achievements, default_post_privacy
      FROM user_privacy_settings
      WHERE user_id = $1
    `, [userId]);

    if (result.rows.length === 0) {
      // Create default privacy settings
      await query(`
        INSERT INTO user_privacy_settings (user_id, auto_share_workouts, auto_share_achievements, default_post_privacy)
        VALUES ($1, false, true, 'FRIENDS')
      `, [userId]);

      res.json({
        autoShareWorkouts: false,
        autoShareAchievements: true,
        defaultPostPrivacy: 'FRIENDS'
      });
    } else {
      const settings = result.rows[0];
      res.json({
        autoShareWorkouts: settings.auto_share_workouts,
        autoShareAchievements: settings.auto_share_achievements,
        defaultPostPrivacy: settings.default_post_privacy
      });
    }

  } catch (error) {
    console.error('Get privacy settings error:', error);
    res.status(500).json({ 
      error: 'Failed to get privacy settings',
      message: 'Internal server error' 
    });
  }
});

// Update user's privacy settings
router.put('/privacy-settings', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { autoShareWorkouts, autoShareAchievements, defaultPostPrivacy } = req.body;

    // Validate privacy level
    const validPrivacyLevels = ['PUBLIC', 'FRIENDS', 'PRIVATE'];
    if (defaultPostPrivacy && !validPrivacyLevels.includes(defaultPostPrivacy)) {
      return res.status(400).json({
        error: 'Invalid privacy level',
        message: 'Privacy level must be one of: PUBLIC, FRIENDS, PRIVATE'
      });
    }

    await query(`
      INSERT INTO user_privacy_settings (user_id, auto_share_workouts, auto_share_achievements, default_post_privacy)
      VALUES ($1, $2, $3, $4)
      ON CONFLICT (user_id) 
      DO UPDATE SET 
        auto_share_workouts = EXCLUDED.auto_share_workouts,
        auto_share_achievements = EXCLUDED.auto_share_achievements,
        default_post_privacy = EXCLUDED.default_post_privacy,
        updated_at = CURRENT_TIMESTAMP
    `, [userId, autoShareWorkouts, autoShareAchievements, defaultPostPrivacy]);

    res.json({
      message: 'Privacy settings updated successfully'
    });

  } catch (error) {
    console.error('Update privacy settings error:', error);
    res.status(500).json({ 
      error: 'Failed to update privacy settings',
      message: 'Internal server error' 
    });
  }
});

// Delete a post (only by the post author)
router.delete('/posts/:postId', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { postId } = req.params;

    // Check if post exists and belongs to user
    const postCheck = await query(
      'SELECT id FROM feed_posts WHERE id = $1 AND user_id = $2',
      [postId, userId]
    );

    if (postCheck.rows.length === 0) {
      return res.status(404).json({
        error: 'Post not found',
        message: 'Post does not exist or you do not have permission to delete it'
      });
    }

    // Delete the post (cascade will handle likes and comments)
    await query('DELETE FROM feed_posts WHERE id = $1', [postId]);

    res.json({
      message: 'Post deleted successfully'
    });

  } catch (error) {
    console.error('Delete post error:', error);
    res.status(500).json({ 
      error: 'Failed to delete post',
      message: 'Internal server error' 
    });
  }
});

module.exports = router; 