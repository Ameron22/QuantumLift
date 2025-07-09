-- Enable Row Level Security (RLS) for GymTracker tables
-- This script should be run in your Supabase SQL editor

-- Enable RLS on users table
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- Enable RLS on friend_invitations table
ALTER TABLE public.friend_invitations ENABLE ROW LEVEL SECURITY;

-- Enable RLS on friend_connections table
ALTER TABLE public.friend_connections ENABLE ROW LEVEL SECURITY;

-- Create policies for users table
-- Users can read their own profile
CREATE POLICY "Users can view own profile" ON public.users
    FOR SELECT USING (auth.uid()::text = id::text);

-- Users can update their own profile
CREATE POLICY "Users can update own profile" ON public.users
    FOR UPDATE USING (auth.uid()::text = id::text);

-- Allow registration (insert) for new users
CREATE POLICY "Allow user registration" ON public.users
    FOR INSERT WITH CHECK (true);

-- Create policies for friend_invitations table
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

-- Create policies for friend_connections table
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

-- Note: For your current setup without Supabase Auth, you might want to temporarily disable RLS
-- or create more permissive policies. Here's how to disable RLS if needed:

-- ALTER TABLE public.users DISABLE ROW LEVEL SECURITY;
-- ALTER TABLE public.friend_invitations DISABLE ROW LEVEL SECURITY;
-- ALTER TABLE public.friend_connections DISABLE ROW LEVEL SECURITY; 