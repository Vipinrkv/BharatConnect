-- RLS Policies for Chats & Chat Members Tables

-- Enable Row Level Security
ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_members ENABLE ROW LEVEL SECURITY;

-- 1. Chats - Select Policy
-- Users can only see chat metadata for groups or direct channels they are active members of.
CREATE POLICY select_chats ON public.chats 
    FOR SELECT 
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.chat_members 
            WHERE chat_members.chat_id = chats.id 
            AND chat_members.profile_id = auth.uid()
        )
    );

-- 2. Chats - Insert Policy
-- Authenticated users can establish direct or group chats.
CREATE POLICY insert_chats ON public.chats 
    FOR INSERT 
    TO authenticated 
    WITH CHECK (true);

-- 3. Chat Members - Select Policy
-- Users can view membership records for chats they are belonging to.
CREATE POLICY select_members ON public.chat_members 
    FOR SELECT 
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.chat_members cm 
            WHERE cm.chat_id = chat_members.chat_id 
            AND cm.profile_id = auth.uid()
        )
    );

-- 4. Chat Members - Insert Policy
-- Members can register themselves or add peers to group channels.
CREATE POLICY insert_members ON public.chat_members 
    FOR INSERT 
    TO authenticated 
    WITH CHECK (true);
