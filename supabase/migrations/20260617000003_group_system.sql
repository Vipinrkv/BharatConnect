-- Database Migration: Group System & Polls Integration

-- 1. Extend Chats Table with group properties
ALTER TABLE public.chats 
  ADD COLUMN IF NOT EXISTS description TEXT,
  ADD COLUMN IF NOT EXISTS banner_url TEXT,
  ADD COLUMN IF NOT EXISTS invite_token VARCHAR(100) UNIQUE,
  ADD COLUMN IF NOT EXISTS pinned_message_id UUID;

-- 2. Modify Chat Members Roles Check Constraint
-- Drop old constraint first
ALTER TABLE public.chat_members DROP CONSTRAINT IF EXISTS chat_members_role_check;
-- Add expanded constraint
ALTER TABLE public.chat_members ADD CONSTRAINT chat_members_role_check 
  CHECK (role IN ('owner', 'admin', 'moderator', 'member'));

-- 3. Create Polls Table
CREATE TABLE public.polls (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    chat_id UUID REFERENCES public.chats(id) ON DELETE CASCADE NOT NULL,
    creator_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL NOT NULL,
    question TEXT NOT NULL,
    is_anonymous BOOLEAN DEFAULT TRUE,
    allow_multiple_answers BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 4. Create Poll Options Table
CREATE TABLE public.poll_options (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    poll_id UUID REFERENCES public.polls(id) ON DELETE CASCADE NOT NULL,
    option_text TEXT NOT NULL
);

-- 5. Create Poll Votes Table (Composite PK)
CREATE TABLE public.poll_votes (
    poll_id UUID REFERENCES public.polls(id) ON DELETE CASCADE NOT NULL,
    option_id UUID REFERENCES public.poll_options(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    PRIMARY KEY (poll_id, option_id, user_id)
);

-- Indices
CREATE INDEX idx_polls_chat ON public.polls (chat_id);
CREATE INDEX idx_poll_options_poll ON public.poll_options (poll_id);
CREATE INDEX idx_poll_votes_user ON public.poll_votes (user_id);

-- Bind pinned_message_id Foreign Key now that tables are generated
ALTER TABLE public.chats 
  ADD CONSTRAINT fk_chats_pinned_message 
  FOREIGN KEY (pinned_message_id) REFERENCES public.messages(id) ON DELETE SET NULL;

-- 6. Row Level Security for Polls
ALTER TABLE public.polls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.poll_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.poll_votes ENABLE ROW LEVEL SECURITY;

-- Polls Policies
CREATE POLICY select_polls ON public.polls FOR SELECT TO authenticated
  USING (EXISTS (SELECT 1 FROM public.chat_members WHERE chat_members.chat_id = polls.chat_id AND chat_members.profile_id = auth.uid()));

CREATE POLICY insert_polls ON public.polls FOR INSERT TO authenticated
  WITH CHECK (EXISTS (SELECT 1 FROM public.chat_members WHERE chat_members.chat_id = polls.chat_id AND chat_members.profile_id = auth.uid()));

-- Poll Options Policies
CREATE POLICY select_options ON public.poll_options FOR SELECT TO authenticated USING (true);
CREATE POLICY insert_options ON public.poll_options FOR INSERT TO authenticated USING (true);

-- Poll Votes Policies
CREATE POLICY select_votes ON public.poll_votes FOR SELECT TO authenticated USING (true);
CREATE POLICY insert_votes ON public.poll_votes FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY delete_own_vote ON public.poll_votes FOR DELETE TO authenticated USING (user_id = auth.uid());
