-- Feed Tables Migration for Supabase
-- Run this in your Supabase SQL Editor

-- Feed posts table
CREATE TABLE IF NOT EXISTS feed_posts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
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
    id SERIAL PRIMARY KEY,
    post_id INTEGER NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)
);

-- Post comments table
CREATE TABLE IF NOT EXISTS post_comments (
    id SERIAL PRIMARY KEY,
    post_id INTEGER NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User privacy settings table
CREATE TABLE IF NOT EXISTS user_privacy_settings (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    auto_share_workouts BOOLEAN DEFAULT false,
    auto_share_achievements BOOLEAN DEFAULT true,
    default_post_privacy VARCHAR(20) DEFAULT 'FRIENDS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_feed_posts_user_id ON feed_posts(user_id);
CREATE INDEX IF NOT EXISTS idx_feed_posts_created_at ON feed_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feed_posts_privacy ON feed_posts(privacy_level);
CREATE INDEX IF NOT EXISTS idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_post_comments_post_id ON post_comments(post_id);

-- Enable Row Level Security (RLS)
ALTER TABLE feed_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_privacy_settings ENABLE ROW LEVEL SECURITY;

-- RLS Policies for feed_posts
CREATE POLICY "Users can view public posts" ON feed_posts
    FOR SELECT USING (privacy_level = 'PUBLIC');

CREATE POLICY "Users can view their own posts" ON feed_posts
    FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "Users can view friends' posts" ON feed_posts
    FOR SELECT USING (
        privacy_level = 'FRIENDS' AND (
            EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id = auth.uid() AND fc.friend_id = feed_posts.user_id)
            OR EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id = feed_posts.user_id AND fc.friend_id = auth.uid())
        )
    );

CREATE POLICY "Users can create their own posts" ON feed_posts
    FOR INSERT WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update their own posts" ON feed_posts
    FOR UPDATE USING (user_id = auth.uid());

CREATE POLICY "Users can delete their own posts" ON feed_posts
    FOR DELETE USING (user_id = auth.uid());

-- RLS Policies for post_likes
CREATE POLICY "Users can view all likes" ON post_likes
    FOR SELECT USING (true);

CREATE POLICY "Users can like posts" ON post_likes
    FOR INSERT WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can unlike their own likes" ON post_likes
    FOR DELETE USING (user_id = auth.uid());

-- RLS Policies for post_comments
CREATE POLICY "Users can view all comments" ON post_comments
    FOR SELECT USING (true);

CREATE POLICY "Users can create comments" ON post_comments
    FOR INSERT WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update their own comments" ON post_comments
    FOR UPDATE USING (user_id = auth.uid());

CREATE POLICY "Users can delete their own comments" ON post_comments
    FOR DELETE USING (user_id = auth.uid());

-- RLS Policies for user_privacy_settings
CREATE POLICY "Users can view their own privacy settings" ON user_privacy_settings
    FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "Users can create their own privacy settings" ON user_privacy_settings
    FOR INSERT WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update their own privacy settings" ON user_privacy_settings
    FOR UPDATE USING (user_id = auth.uid()); 