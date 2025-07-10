-- Feed Migration for Supabase
-- Run this in your Supabase SQL Editor

-- Feed posts table
CREATE TABLE IF NOT EXISTS feed_posts (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
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
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)
);

-- Post comments table
CREATE TABLE IF NOT EXISTS post_comments (
    id SERIAL PRIMARY KEY,
    post_id INTEGER NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User privacy settings table
CREATE TABLE IF NOT EXISTS user_privacy_settings (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
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

-- Enable Row Level Security (RLS) for Supabase
ALTER TABLE feed_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_privacy_settings ENABLE ROW LEVEL SECURITY;

-- RLS Policies for feed_posts
CREATE POLICY "Users can view their own posts" ON feed_posts
    FOR SELECT USING (auth.uid()::text = user_id::text);

CREATE POLICY "Users can view public posts" ON feed_posts
    FOR SELECT USING (privacy_level = 'PUBLIC');

CREATE POLICY "Users can view friends' posts" ON feed_posts
    FOR SELECT USING (
        privacy_level = 'FRIENDS' AND (
            EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id::text = auth.uid()::text AND fc.friend_id::text = feed_posts.user_id::text)
            OR EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id::text = feed_posts.user_id::text AND fc.friend_id::text = auth.uid()::text)
        )
    );

CREATE POLICY "Users can create their own posts" ON feed_posts
    FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);

CREATE POLICY "Users can update their own posts" ON feed_posts
    FOR UPDATE USING (auth.uid()::text = user_id::text);

CREATE POLICY "Users can delete their own posts" ON feed_posts
    FOR DELETE USING (auth.uid()::text = user_id::text);

-- RLS Policies for post_likes
CREATE POLICY "Users can view likes on visible posts" ON post_likes
    FOR SELECT USING (
        EXISTS(SELECT 1 FROM feed_posts fp WHERE fp.id = post_likes.post_id AND (
            fp.privacy_level = 'PUBLIC' 
            OR fp.user_id::text = auth.uid()::text
            OR (fp.privacy_level = 'FRIENDS' AND (
                EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id::text = auth.uid()::text AND fc.friend_id::text = fp.user_id::text)
                OR EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id::text = fp.user_id::text AND fc.friend_id::text = auth.uid()::text)
            ))
        ))
    );

CREATE POLICY "Users can like/unlike visible posts" ON post_likes
    FOR ALL USING (
        EXISTS(SELECT 1 FROM feed_posts fp WHERE fp.id = post_likes.post_id AND (
            fp.privacy_level = 'PUBLIC' 
            OR fp.user_id::text = auth.uid()::text
            OR (fp.privacy_level = 'FRIENDS' AND (
                EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id::text = auth.uid()::text AND fc.friend_id::text = fp.user_id::text)
                OR EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id::text = fp.user_id::text AND fc.friend_id::text = auth.uid()::text)
            ))
        ))
    );

-- RLS Policies for post_comments
CREATE POLICY "Users can view comments on visible posts" ON post_comments
    FOR SELECT USING (
        EXISTS(SELECT 1 FROM feed_posts fp WHERE fp.id = post_comments.post_id AND (
            fp.privacy_level = 'PUBLIC' 
            OR fp.user_id::text = auth.uid()::text
            OR (fp.privacy_level = 'FRIENDS' AND (
                EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id::text = auth.uid()::text AND fc.friend_id::text = fp.user_id::text)
                OR EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id::text = fp.user_id::text AND fc.friend_id::text = auth.uid()::text)
            ))
        ))
    );

CREATE POLICY "Users can comment on visible posts" ON post_comments
    FOR INSERT WITH CHECK (
        EXISTS(SELECT 1 FROM feed_posts fp WHERE fp.id = post_comments.post_id AND (
            fp.privacy_level = 'PUBLIC' 
            OR fp.user_id::text = auth.uid()::text
            OR (fp.privacy_level = 'FRIENDS' AND (
                EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id::text = auth.uid()::text AND fc.friend_id::text = fp.user_id::text)
                OR EXISTS(SELECT 1 FROM friend_connections fc WHERE fc.user_id::text = fp.user_id::text AND fc.friend_id::text = auth.uid()::text)
            ))
        ))
    );

-- RLS Policies for user_privacy_settings
CREATE POLICY "Users can view their own privacy settings" ON user_privacy_settings
    FOR SELECT USING (auth.uid()::text = user_id::text);

CREATE POLICY "Users can update their own privacy settings" ON user_privacy_settings
    FOR ALL USING (auth.uid()::text = user_id::text); 