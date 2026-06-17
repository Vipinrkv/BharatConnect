-- RLS Policies for Help Requests & Volunteer Responses

-- Enable Row Level Security
ALTER TABLE public.help_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.help_responses ENABLE ROW LEVEL SECURITY;

-- 1. Help Requests - Select Policy
-- Open SOS help requests are publicly queryable by all authenticated users to allow volunteer matches.
CREATE POLICY select_help ON public.help_requests 
    FOR SELECT 
    TO authenticated 
    USING (true);

-- 2. Help Requests - Insert Policy
-- Users can broadcast SOS requests, marking themselves as the requester.
CREATE POLICY insert_help ON public.help_requests 
    FOR INSERT 
    TO authenticated 
    WITH CHECK (requester_id = auth.uid());

-- 3. Help Requests - Update Policy
-- Requesters can edit or resolve their active SOS requests.
CREATE POLICY update_own_help ON public.help_requests 
    FOR UPDATE 
    TO authenticated 
    USING (requester_id = auth.uid());

-- 4. Help Responses - Select Policy
-- Users can view volunteering proposals matching community requests.
CREATE POLICY select_responses ON public.help_responses 
    FOR SELECT 
    TO authenticated 
    USING (true);

-- 5. Help Responses - Insert Policy
-- Vetted volunteers can submit support proposals.
CREATE POLICY insert_own_response ON public.help_responses 
    FOR INSERT 
    TO authenticated 
    WITH CHECK (volunteer_id = auth.uid());

-- 6. Help Responses - Update Policy
-- Volunteers can modify or withdraw their service offers.
CREATE POLICY update_own_response ON public.help_responses 
    FOR UPDATE 
    TO authenticated 
    USING (volunteer_id = auth.uid());
