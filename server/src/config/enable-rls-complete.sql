-- Complete RLS setup for QuantumLift GymTracker
-- This script enables Row Level Security on all tables and creates appropriate policies
-- Run this in your Supabase SQL editor to resolve the RLS errors

-- Enable RLS on all tables
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friend_invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friend_connections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.post_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feed_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.post_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_privacy_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.exercises ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workout_exercises ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workout_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.exercise_sets ENABLE ROW LEVEL SECURITY;

-- ==============================================
-- USERS TABLE POLICIES
-- ==============================================

-- Users can view their own profile
CREATE POLICY "Users can view own profile" ON public.users
    FOR SELECT USING (auth.uid()::text = id::text);

-- Users can update their own profile
CREATE POLICY "Users can update own profile" ON public.users
    FOR UPDATE USING (auth.uid()::text = id::text);

-- Allow user registration (insert)
CREATE POLICY "Allow user registration" ON public.users
    FOR INSERT WITH CHECK (true);

-- Users can view public profiles (for friend discovery)
CREATE POLICY "Users can view public profiles" ON public.users
    FOR SELECT USING (is_public = true);

-- ==============================================
-- FRIEND_INVITATIONS TABLE POLICIES
-- ==============================================

-- Users can view invitations they sent
CREATE POLICY "Users can view sent invitations" ON public.friend_invitations
    FOR SELECT USING (auth.uid()::text = sender_id::text);

-- Users can create invitations
CREATE POLICY "Users can create invitations" ON public.friend_invitations
    FOR INSERT WITH CHECK (auth.uid()::text = sender_id::text);

-- Users can update invitations they sent
CREATE POLICY "Users can update sent invitations" ON public.friend_invitations
    FOR UPDATE USING (auth.uid()::text = sender_id::text);

-- Allow reading invitations by email (for accepting invitations)
CREATE POLICY "Allow reading invitations by email" ON public.friend_invitations
    FOR SELECT USING (true);

-- ==============================================
-- FRIEND_CONNECTIONS TABLE POLICIES
-- ==============================================

-- Users can view their own connections
CREATE POLICY "Users can view own connections" ON public.friend_connections
    FOR SELECT USING (
        auth.uid()::text = user_id::text OR 
        auth.uid()::text = friend_id::text
    );

-- Users can create connections
CREATE POLICY "Users can create connections" ON public.friend_connections
    FOR INSERT WITH CHECK (
        auth.uid()::text = user_id::text OR 
        auth.uid()::text = friend_id::text
    );

-- Users can update their own connections
CREATE POLICY "Users can update own connections" ON public.friend_connections
    FOR UPDATE USING (
        auth.uid()::text = user_id::text OR 
        auth.uid()::text = friend_id::text
    );

-- ==============================================
-- FEED_POSTS TABLE POLICIES
-- ==============================================

-- Users can view their own posts
CREATE POLICY "Users can view own posts" ON public.feed_posts
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Users can view public posts
CREATE POLICY "Users can view public posts" ON public.feed_posts
    FOR SELECT USING (privacy_level = 'PUBLIC');

-- Users can view friends' posts (if they are friends)
CREATE POLICY "Users can view friends posts" ON public.feed_posts
    FOR SELECT USING (
        privacy_level = 'FRIENDS' AND (
            auth.uid()::text = user_id::text OR
            EXISTS (
                SELECT 1 FROM public.friend_connections fc 
                WHERE (fc.user_id::text = auth.uid()::text AND fc.friend_id::text = user_id::text)
                   OR (fc.friend_id::text = auth.uid()::text AND fc.user_id::text = user_id::text)
            )
        )
    );

-- Users can create their own posts
CREATE POLICY "Users can create own posts" ON public.feed_posts
    FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);

-- Users can update their own posts
CREATE POLICY "Users can update own posts" ON public.feed_posts
    FOR UPDATE USING (auth.uid()::text = user_id::text);

-- Users can delete their own posts
CREATE POLICY "Users can delete own posts" ON public.feed_posts
    FOR DELETE USING (auth.uid()::text = user_id::text);

-- ==============================================
-- POST_LIKES TABLE POLICIES
-- ==============================================

-- Users can view likes on posts they can see
CREATE POLICY "Users can view likes on visible posts" ON public.post_likes
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.feed_posts fp 
            WHERE fp.id = post_likes.post_id AND (
                fp.user_id::text = auth.uid()::text OR
                fp.privacy_level = 'PUBLIC' OR
                (fp.privacy_level = 'FRIENDS' AND EXISTS (
                    SELECT 1 FROM public.friend_connections fc 
                    WHERE (fc.user_id::text = auth.uid()::text AND fc.friend_id::text = fp.user_id::text)
                       OR (fc.friend_id::text = auth.uid()::text AND fc.user_id::text = fp.user_id::text)
                ))
            )
        )
    );

-- Users can create likes on posts they can see
CREATE POLICY "Users can create likes on visible posts" ON public.post_likes
    FOR INSERT WITH CHECK (
        auth.uid()::text = user_id::text AND
        EXISTS (
            SELECT 1 FROM public.feed_posts fp 
            WHERE fp.id = post_likes.post_id AND (
                fp.user_id::text = auth.uid()::text OR
                fp.privacy_level = 'PUBLIC' OR
                (fp.privacy_level = 'FRIENDS' AND EXISTS (
                    SELECT 1 FROM public.friend_connections fc 
                    WHERE (fc.user_id::text = auth.uid()::text AND fc.friend_id::text = fp.user_id::text)
                       OR (fc.friend_id::text = auth.uid()::text AND fc.user_id::text = fp.user_id::text)
                ))
            )
        )
    );

-- Users can delete their own likes
CREATE POLICY "Users can delete own likes" ON public.post_likes
    FOR DELETE USING (auth.uid()::text = user_id::text);

-- ==============================================
-- POST_COMMENTS TABLE POLICIES
-- ==============================================

-- Users can view comments on posts they can see
CREATE POLICY "Users can view comments on visible posts" ON public.post_comments
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.feed_posts fp 
            WHERE fp.id = post_comments.post_id AND (
                fp.user_id::text = auth.uid()::text OR
                fp.privacy_level = 'PUBLIC' OR
                (fp.privacy_level = 'FRIENDS' AND EXISTS (
                    SELECT 1 FROM public.friend_connections fc 
                    WHERE (fc.user_id::text = auth.uid()::text AND fc.friend_id::text = fp.user_id::text)
                       OR (fc.friend_id::text = auth.uid()::text AND fc.user_id::text = fp.user_id::text)
                ))
            )
        )
    );

-- Users can create comments on posts they can see
CREATE POLICY "Users can create comments on visible posts" ON public.post_comments
    FOR INSERT WITH CHECK (
        auth.uid()::text = user_id::text AND
        EXISTS (
            SELECT 1 FROM public.feed_posts fp 
            WHERE fp.id = post_comments.post_id AND (
                fp.user_id::text = auth.uid()::text OR
                fp.privacy_level = 'PUBLIC' OR
                (fp.privacy_level = 'FRIENDS' AND EXISTS (
                    SELECT 1 FROM public.friend_connections fc 
                    WHERE (fc.user_id::text = auth.uid()::text AND fc.friend_id::text = fp.user_id::text)
                       OR (fc.friend_id::text = auth.uid()::text AND fc.user_id::text = fp.user_id::text)
                ))
            )
        )
    );

-- Users can update their own comments
CREATE POLICY "Users can update own comments" ON public.post_comments
    FOR UPDATE USING (auth.uid()::text = user_id::text);

-- Users can delete their own comments
CREATE POLICY "Users can delete own comments" ON public.post_comments
    FOR DELETE USING (auth.uid()::text = user_id::text);

-- ==============================================
-- USER_PRIVACY_SETTINGS TABLE POLICIES
-- ==============================================

-- Users can view their own privacy settings
CREATE POLICY "Users can view own privacy settings" ON public.user_privacy_settings
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Users can create their own privacy settings
CREATE POLICY "Users can create own privacy settings" ON public.user_privacy_settings
    FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);

-- Users can update their own privacy settings
CREATE POLICY "Users can update own privacy settings" ON public.user_privacy_settings
    FOR UPDATE USING (auth.uid()::text = user_id::text);

-- ==============================================
-- WORKOUTS TABLE POLICIES
-- ==============================================

-- Users can view their own workouts
CREATE POLICY "Users can view own workouts" ON public.workouts
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Users can create their own workouts
CREATE POLICY "Users can create own workouts" ON public.workouts
    FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);

-- Users can update their own workouts
CREATE POLICY "Users can update own workouts" ON public.workouts
    FOR UPDATE USING (auth.uid()::text = user_id::text);

-- Users can delete their own workouts
CREATE POLICY "Users can delete own workouts" ON public.workouts
    FOR DELETE USING (auth.uid()::text = user_id::text);

-- ==============================================
-- EXERCISES TABLE POLICIES
-- ==============================================

-- Users can view standard exercises (is_custom = false)
CREATE POLICY "Users can view standard exercises" ON public.exercises
    FOR SELECT USING (is_custom = false);

-- Users can view their own custom exercises
CREATE POLICY "Users can view own custom exercises" ON public.exercises
    FOR SELECT USING (
        is_custom = true AND auth.uid()::text = user_id::text
    );

-- Users can create their own custom exercises
CREATE POLICY "Users can create own custom exercises" ON public.exercises
    FOR INSERT WITH CHECK (
        is_custom = true AND auth.uid()::text = user_id::text
    );

-- Users can update their own custom exercises
CREATE POLICY "Users can update own custom exercises" ON public.exercises
    FOR UPDATE USING (
        is_custom = true AND auth.uid()::text = user_id::text
    );

-- Users can delete their own custom exercises
CREATE POLICY "Users can delete own custom exercises" ON public.exercises
    FOR DELETE USING (
        is_custom = true AND auth.uid()::text = user_id::text
    );

-- ==============================================
-- WORKOUT_EXERCISES TABLE POLICIES
-- ==============================================

-- Users can view workout exercises for their own workouts
CREATE POLICY "Users can view own workout exercises" ON public.workout_exercises
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.workouts w 
            WHERE w.id = workout_exercises.workout_id 
            AND w.user_id::text = auth.uid()::text
        )
    );

-- Users can create workout exercises for their own workouts
CREATE POLICY "Users can create own workout exercises" ON public.workout_exercises
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.workouts w 
            WHERE w.id = workout_exercises.workout_id 
            AND w.user_id::text = auth.uid()::text
        )
    );

-- Users can update workout exercises for their own workouts
CREATE POLICY "Users can update own workout exercises" ON public.workout_exercises
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM public.workouts w 
            WHERE w.id = workout_exercises.workout_id 
            AND w.user_id::text = auth.uid()::text
        )
    );

-- Users can delete workout exercises for their own workouts
CREATE POLICY "Users can delete own workout exercises" ON public.workout_exercises
    FOR DELETE USING (
        EXISTS (
            SELECT 1 FROM public.workouts w 
            WHERE w.id = workout_exercises.workout_id 
            AND w.user_id::text = auth.uid()::text
        )
    );

-- ==============================================
-- WORKOUT_SESSIONS TABLE POLICIES
-- ==============================================

-- Users can view their own workout sessions
CREATE POLICY "Users can view own workout sessions" ON public.workout_sessions
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Users can create their own workout sessions
CREATE POLICY "Users can create own workout sessions" ON public.workout_sessions
    FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);

-- Users can update their own workout sessions
CREATE POLICY "Users can update own workout sessions" ON public.workout_sessions
    FOR UPDATE USING (auth.uid()::text = user_id::text);

-- Users can delete their own workout sessions
CREATE POLICY "Users can delete own workout sessions" ON public.workout_sessions
    FOR DELETE USING (auth.uid()::text = user_id::text);

-- ==============================================
-- EXERCISE_SETS TABLE POLICIES
-- ==============================================

-- Users can view exercise sets for their own workout sessions
CREATE POLICY "Users can view own exercise sets" ON public.exercise_sets
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.workout_sessions ws 
            WHERE ws.id = exercise_sets.session_id 
            AND ws.user_id::text = auth.uid()::text
        )
    );

-- Users can create exercise sets for their own workout sessions
CREATE POLICY "Users can create own exercise sets" ON public.exercise_sets
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.workout_sessions ws 
            WHERE ws.id = exercise_sets.session_id 
            AND ws.user_id::text = auth.uid()::text
        )
    );

-- Users can update exercise sets for their own workout sessions
CREATE POLICY "Users can update own exercise sets" ON public.exercise_sets
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM public.workout_sessions ws 
            WHERE ws.id = exercise_sets.session_id 
            AND ws.user_id::text = auth.uid()::text
        )
    );

-- Users can delete exercise sets for their own workout sessions
CREATE POLICY "Users can delete own exercise sets" ON public.exercise_sets
    FOR DELETE USING (
        EXISTS (
            SELECT 1 FROM public.workout_sessions ws 
            WHERE ws.id = exercise_sets.session_id 
            AND ws.user_id::text = auth.uid()::text
        )
    );

-- ==============================================
-- NOTES FOR CUSTOM AUTHENTICATION
-- ==============================================

-- If you're using custom authentication instead of Supabase Auth,
-- you may need to modify these policies to work with your auth system.
-- The policies above assume auth.uid() returns the current user's UUID.
-- 
-- For custom auth, you might need to:
-- 1. Create a function that returns the current user ID from your auth system
-- 2. Replace auth.uid()::text with your custom function
-- 3. Or temporarily use more permissive policies for development
--
-- Example for custom auth:
-- CREATE POLICY "Users can view own profile" ON public.users
--     FOR SELECT USING (get_current_user_id() = id::text);
--
-- Where get_current_user_id() is your custom function that returns the current user's ID.
