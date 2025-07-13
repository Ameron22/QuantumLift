-- Simple RLS setup for QuantumLift with custom authentication
-- This script should be run in your Supabase SQL editor

-- Option 1: Enable RLS with permissive policies for custom auth
-- Enable RLS on all tables
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friend_invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friend_connections ENABLE ROW LEVEL SECURITY;

-- Create permissive policies for custom authentication
-- Allow all operations for now (you can make them more restrictive later)
CREATE POLICY "Allow all operations on users" ON public.users FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all operations on friend_invitations" ON public.friend_invitations FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all operations on friend_connections" ON public.friend_connections FOR ALL USING (true) WITH CHECK (true);

-- Option 2: If you prefer to disable RLS completely (simpler for development)
-- Uncomment the lines below if you want to disable RLS instead:

-- ALTER TABLE public.users DISABLE ROW LEVEL SECURITY;
-- ALTER TABLE public.friend_invitations DISABLE ROW LEVEL SECURITY;
-- ALTER TABLE public.friend_connections DISABLE ROW LEVEL SECURITY; 