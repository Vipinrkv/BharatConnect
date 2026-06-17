-- RLS Policies for Profiles Table

-- Enable Row Level Security
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- 1. Select Policy
-- Authenticated users can view all profiles for direct communication and discovery.
CREATE POLICY select_profiles ON public.profiles 
    FOR SELECT 
    TO authenticated 
    USING (true);

-- 2. Update Policy
-- Users can only modify their own profile data (avatars, display names, and geolocation).
CREATE POLICY update_own_profile ON public.profiles 
    FOR UPDATE 
    TO authenticated 
    USING (id = auth.uid());
