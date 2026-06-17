-- RLS Policies for Messages & Receipts Tables

-- Enable Row Level Security
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_receipts ENABLE ROW LEVEL SECURITY;

-- 1. Messages - Select Policy
-- Users can only read messages from chats they are registered in.
CREATE POLICY select_messages ON public.messages 
    FOR SELECT 
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.chat_members 
            WHERE chat_members.chat_id = messages.chat_id 
            AND chat_members.profile_id = auth.uid()
        )
    );

-- 2. Messages - Insert Policy
-- Users can write messages to chats they belong to, signing themselves as the sender.
CREATE POLICY insert_messages ON public.messages 
    FOR INSERT 
    TO authenticated
    WITH CHECK (
        sender_id = auth.uid() 
        AND EXISTS (
            SELECT 1 FROM public.chat_members 
            WHERE chat_members.chat_id = messages.chat_id 
            AND chat_members.profile_id = auth.uid()
        )
    );

-- 3. Receipts - Select Policy
-- Users can view delivery checkmarks matching their chats.
CREATE POLICY select_receipts ON public.message_receipts 
    FOR SELECT 
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.chat_members 
            WHERE chat_members.chat_id = (
                SELECT chat_id FROM public.messages WHERE messages.id = message_receipts.message_id
            ) 
            AND chat_members.profile_id = auth.uid()
        )
    );

-- 4. Receipts - Write/Update Policy
-- Users can mark checkmark status flags matching their own profile.
CREATE POLICY insert_update_receipts ON public.message_receipts 
    FOR ALL 
    TO authenticated
    USING (profile_id = auth.uid());
