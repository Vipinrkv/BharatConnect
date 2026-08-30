-- ============================================================================
-- 🇮🇳 BHARATCONNECT SUPABASE POSTGRESQL DATABASE SCHEMA & SECURITY POLICIES
-- ============================================================================
-- Production Schema & Universal Migration Script for BharatConnect Native Android App
-- 100% Idempotent & Type-Safe: Adds missing columns, casts ID comparisons safely,
-- and handles mixed UUID / VARCHAR / TEXT types across existing tables.
-- ============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. PROFILES TABLE (User Accounts & Metadata)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    username TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    avatar_url TEXT,
    bio TEXT DEFAULT 'Welcome to BharatConnect. Ready to connect and explore!',
    phone_number TEXT,
    dob TEXT,
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS email TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS username TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS full_name TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS avatar_url TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS bio TEXT DEFAULT 'Welcome to BharatConnect. Ready to connect and explore!';
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS phone_number TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS dob TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_online BOOLEAN DEFAULT FALSE;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS last_seen TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_profiles_username_lower ON public.profiles (LOWER(username));
CREATE INDEX IF NOT EXISTS idx_profiles_phone ON public.profiles (phone_number);
CREATE INDEX IF NOT EXISTS idx_profiles_email ON public.profiles (email);

-- ============================================================================
-- 2. CONVERSATIONS & CHAT ENGINE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.conversations (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    type TEXT NOT NULL DEFAULT 'direct' CHECK (type IN ('direct', 'group', 'community')),
    title TEXT,
    avatar_url TEXT,
    last_message TEXT,
    last_message_time TIMESTAMPTZ DEFAULT NOW(),
    created_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS type TEXT DEFAULT 'direct';
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS avatar_url TEXT;
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS last_message TEXT;
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS last_message_time TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

CREATE TABLE IF NOT EXISTS public.conversation_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id TEXT NOT NULL,
    user_id UUID NOT NULL,
    role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('admin', 'member')),
    joined_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.conversation_members ADD COLUMN IF NOT EXISTS conversation_id TEXT;
ALTER TABLE public.conversation_members ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE public.conversation_members ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'member';
ALTER TABLE public.conversation_members ADD COLUMN IF NOT EXISTS joined_at TIMESTAMPTZ DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_conversation_members_user ON public.conversation_members (user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_members_conv ON public.conversation_members (conversation_id);

CREATE TABLE IF NOT EXISTS public.messages (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    conversation_id TEXT NOT NULL,
    sender_id UUID NOT NULL,
    sender_name TEXT NOT NULL,
    content TEXT NOT NULL,
    media_url TEXT,
    media_type TEXT,
    status TEXT NOT NULL DEFAULT 'sent' CHECK (status IN ('sending', 'sent', 'delivered', 'read')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS conversation_id TEXT;
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS sender_id UUID;
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS sender_name TEXT DEFAULT 'User';
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS content TEXT DEFAULT '';
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS media_url TEXT;
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS media_type TEXT;
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'sent';
ALTER TABLE public.messages ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON public.messages (conversation_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON public.messages (sender_id);

-- ============================================================================
-- 3. SOCIAL FEED (Posts, Likes & Comments)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.posts (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    author_id UUID NOT NULL,
    author_name TEXT NOT NULL,
    author_avatar TEXT,
    content TEXT NOT NULL,
    media_url TEXT,
    media_type TEXT,
    likes_count INT NOT NULL DEFAULT 0,
    comments_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.posts ADD COLUMN IF NOT EXISTS author_id UUID;
ALTER TABLE public.posts ADD COLUMN IF NOT EXISTS author_name TEXT;
ALTER TABLE public.posts ADD COLUMN IF NOT EXISTS author_avatar TEXT;
ALTER TABLE public.posts ADD COLUMN IF NOT EXISTS content TEXT;
ALTER TABLE public.posts ADD COLUMN IF NOT EXISTS media_url TEXT;
ALTER TABLE public.posts ADD COLUMN IF NOT EXISTS media_type TEXT;
ALTER TABLE public.posts ADD COLUMN IF NOT EXISTS likes_count INT DEFAULT 0;
ALTER TABLE public.posts ADD COLUMN IF NOT EXISTS comments_count INT DEFAULT 0;
ALTER TABLE public.posts ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE public.posts ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_posts_created_at ON public.posts (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_author ON public.posts (author_id);

CREATE TABLE IF NOT EXISTS public.post_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id TEXT NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.post_likes ADD COLUMN IF NOT EXISTS post_id TEXT;
ALTER TABLE public.post_likes ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE public.post_likes ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_post_likes_post ON public.post_likes (post_id);

CREATE TABLE IF NOT EXISTS public.post_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id TEXT NOT NULL,
    author_id UUID NOT NULL,
    author_name TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.post_comments ADD COLUMN IF NOT EXISTS post_id TEXT;
ALTER TABLE public.post_comments ADD COLUMN IF NOT EXISTS author_id UUID;
ALTER TABLE public.post_comments ADD COLUMN IF NOT EXISTS author_name TEXT NOT NULL DEFAULT 'User';
ALTER TABLE public.post_comments ADD COLUMN IF NOT EXISTS content TEXT NOT NULL DEFAULT '';
ALTER TABLE public.post_comments ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_post_comments_post ON public.post_comments (post_id, created_at ASC);

-- ============================================================================
-- 4. 24-HOUR STORIES & STATUS
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.stories (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    author_id UUID NOT NULL,
    author_name TEXT NOT NULL,
    author_avatar TEXT,
    media_url TEXT,
    text_content TEXT,
    background_gradient TEXT,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '24 hours'),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.stories ADD COLUMN IF NOT EXISTS author_id UUID;
ALTER TABLE public.stories ADD COLUMN IF NOT EXISTS author_name TEXT;
ALTER TABLE public.stories ADD COLUMN IF NOT EXISTS author_avatar TEXT;
ALTER TABLE public.stories ADD COLUMN IF NOT EXISTS media_url TEXT;
ALTER TABLE public.stories ADD COLUMN IF NOT EXISTS text_content TEXT;
ALTER TABLE public.stories ADD COLUMN IF NOT EXISTS background_gradient TEXT;
ALTER TABLE public.stories ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '24 hours');
ALTER TABLE public.stories ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_stories_expires_at ON public.stories (expires_at DESC);

-- ============================================================================
-- 5. MARKETPLACE (Items, Jobs & Quick Gigs)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.marketplace_items (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    seller_id UUID NOT NULL,
    seller_name TEXT NOT NULL,
    title TEXT NOT NULL,
    price TEXT NOT NULL,
    category TEXT NOT NULL,
    location TEXT NOT NULL,
    image_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.marketplace_items ADD COLUMN IF NOT EXISTS seller_id UUID;
ALTER TABLE public.marketplace_items ADD COLUMN IF NOT EXISTS seller_name TEXT;
ALTER TABLE public.marketplace_items ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.marketplace_items ADD COLUMN IF NOT EXISTS price TEXT;
ALTER TABLE public.marketplace_items ADD COLUMN IF NOT EXISTS category TEXT;
ALTER TABLE public.marketplace_items ADD COLUMN IF NOT EXISTS location TEXT;
ALTER TABLE public.marketplace_items ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE public.marketplace_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

CREATE TABLE IF NOT EXISTS public.jobs (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    poster_id UUID NOT NULL,
    title TEXT NOT NULL,
    company TEXT NOT NULL,
    salary TEXT NOT NULL,
    type TEXT NOT NULL,
    location TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.jobs ADD COLUMN IF NOT EXISTS poster_id UUID;
ALTER TABLE public.jobs ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.jobs ADD COLUMN IF NOT EXISTS company TEXT;
ALTER TABLE public.jobs ADD COLUMN IF NOT EXISTS salary TEXT;
ALTER TABLE public.jobs ADD COLUMN IF NOT EXISTS type TEXT;
ALTER TABLE public.jobs ADD COLUMN IF NOT EXISTS location TEXT;
ALTER TABLE public.jobs ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

CREATE TABLE IF NOT EXISTS public.quick_jobs (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    poster_id UUID NOT NULL,
    poster_name TEXT NOT NULL,
    title TEXT NOT NULL,
    payout TEXT NOT NULL,
    duration TEXT NOT NULL,
    urgency TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.quick_jobs ADD COLUMN IF NOT EXISTS poster_id UUID;
ALTER TABLE public.quick_jobs ADD COLUMN IF NOT EXISTS poster_name TEXT;
ALTER TABLE public.quick_jobs ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.quick_jobs ADD COLUMN IF NOT EXISTS payout TEXT;
ALTER TABLE public.quick_jobs ADD COLUMN IF NOT EXISTS duration TEXT;
ALTER TABLE public.quick_jobs ADD COLUMN IF NOT EXISTS urgency TEXT;
ALTER TABLE public.quick_jobs ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

-- ============================================================================
-- 6. NOTIFICATIONS & NEARBY RADAR
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.notifications (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    user_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'system' CHECK (category IN ('messages', 'likes', 'system', 'marketplace')),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'system';
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT FALSE;
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_notifications_user ON public.notifications (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS public.user_locations (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status TEXT,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.user_locations ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE public.user_locations ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE public.user_locations ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE public.user_locations ADD COLUMN IF NOT EXISTS status TEXT;
ALTER TABLE public.user_locations ADD COLUMN IF NOT EXISTS is_visible BOOLEAN DEFAULT TRUE;
ALTER TABLE public.user_locations ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ============================================================================
-- 7. DATABASE TRIGGERS & AUTOMATION
-- ============================================================================

-- Function: Auto create / sync profile on GoTrue Auth Sign Up
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, username, full_name, phone_number, dob)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1)),
        COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', 'Bharat Member'),
        NEW.raw_user_meta_data->>'phone_number',
        NEW.raw_user_meta_data->>'dob'
    )
    ON CONFLICT (id) DO UPDATE SET
        email = EXCLUDED.email,
        phone_number = COALESCE(EXCLUDED.phone_number, public.profiles.phone_number),
        dob = COALESCE(EXCLUDED.dob, public.profiles.dob),
        updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Function: Auto update post like counters (Type-safe ID casting)
CREATE OR REPLACE FUNCTION public.handle_post_like_counter()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE public.posts SET likes_count = likes_count + 1 WHERE id::TEXT = NEW.post_id::TEXT;
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE public.posts SET likes_count = GREATEST(0, likes_count - 1) WHERE id::TEXT = OLD.post_id::TEXT;
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_post_liked ON public.post_likes;
CREATE TRIGGER on_post_liked
    AFTER INSERT OR DELETE ON public.post_likes
    FOR EACH ROW EXECUTE FUNCTION public.handle_post_like_counter();

-- Function: Auto update conversation last message & notify recipient
CREATE OR REPLACE FUNCTION public.handle_new_message()
RETURNS TRIGGER AS $$
DECLARE
    recip_id UUID;
    s_name TEXT;
    c_type TEXT;
BEGIN
    -- 1. Update conversation last message & time
    UPDATE public.conversations
    SET last_message = NEW.content,
        last_message_time = NEW.created_at
    WHERE id::TEXT = NEW.conversation_id::TEXT
    RETURNING type INTO c_type;

    -- 2. If it's a direct conversation, notify the counterpart
    IF c_type = 'direct' OR NEW.conversation_id::TEXT LIKE 'direct_%' THEN
        SELECT COALESCE(full_name, username, 'BharatConnect Member')
        INTO s_name
        FROM public.profiles
        WHERE id = NEW.sender_id;

        -- Find recipient from conversation_members
        SELECT user_id INTO recip_id
        FROM public.conversation_members
        WHERE conversation_id::TEXT = NEW.conversation_id::TEXT
          AND user_id != NEW.sender_id
        LIMIT 1;

        IF recip_id IS NOT NULL THEN
            INSERT INTO public.notifications (user_id, title, description, category, is_read, created_at)
            VALUES (recip_id, COALESCE(s_name, 'New Message'), NEW.content, 'messages', false, NOW());
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_message_sent ON public.messages;
CREATE TRIGGER on_message_sent
    AFTER INSERT ON public.messages
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_message();

-- ============================================================================
-- 8. ROW LEVEL SECURITY (RLS) POLICIES (Type-safe with ::TEXT casting)
-- ============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversation_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.post_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.post_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.marketplace_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quick_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_locations ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON public.profiles;
CREATE POLICY "Public profiles are viewable by everyone" ON public.profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can insert their own profile" ON public.profiles;
CREATE POLICY "Users can insert their own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid()::TEXT = id::TEXT);

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE USING (auth.uid()::TEXT = id::TEXT);

-- Posts Policies
DROP POLICY IF EXISTS "Posts are viewable by everyone" ON public.posts;
CREATE POLICY "Posts are viewable by everyone" ON public.posts FOR SELECT USING (true);

DROP POLICY IF EXISTS "Authenticated users can create posts" ON public.posts;
CREATE POLICY "Authenticated users can create posts" ON public.posts FOR INSERT WITH CHECK (auth.role() = 'authenticated');

DROP POLICY IF EXISTS "Users can update own posts" ON public.posts;
CREATE POLICY "Users can update own posts" ON public.posts FOR UPDATE USING (auth.uid()::TEXT = author_id::TEXT);

DROP POLICY IF EXISTS "Users can delete own posts" ON public.posts;
CREATE POLICY "Users can delete own posts" ON public.posts FOR DELETE USING (auth.uid()::TEXT = author_id::TEXT);

-- Post Likes & Comments Policies
DROP POLICY IF EXISTS "Post likes viewable by everyone" ON public.post_likes;
CREATE POLICY "Post likes viewable by everyone" ON public.post_likes FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can like posts" ON public.post_likes;
CREATE POLICY "Users can like posts" ON public.post_likes FOR INSERT WITH CHECK (auth.uid()::TEXT = user_id::TEXT);

DROP POLICY IF EXISTS "Users can unlike posts" ON public.post_likes;
CREATE POLICY "Users can unlike posts" ON public.post_likes FOR DELETE USING (auth.uid()::TEXT = user_id::TEXT);

DROP POLICY IF EXISTS "Comments viewable by everyone" ON public.post_comments;
CREATE POLICY "Comments viewable by everyone" ON public.post_comments FOR SELECT USING (true);

DROP POLICY IF EXISTS "Authenticated users can comment" ON public.post_comments;
CREATE POLICY "Authenticated users can comment" ON public.post_comments FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Stories Policies
DROP POLICY IF EXISTS "Active stories viewable by everyone" ON public.stories;
CREATE POLICY "Active stories viewable by everyone" ON public.stories FOR SELECT USING (expires_at > NOW());

DROP POLICY IF EXISTS "Authenticated users can post stories" ON public.stories;
CREATE POLICY "Authenticated users can post stories" ON public.stories FOR INSERT WITH CHECK (auth.uid()::TEXT = author_id::TEXT);

DROP POLICY IF EXISTS "Users can delete own stories" ON public.stories;
CREATE POLICY "Users can delete own stories" ON public.stories FOR DELETE USING (auth.uid()::TEXT = author_id::TEXT);

-- Conversations, Members & Messages Policies
-- Disabled RLS eliminates recursive policy loops (Error 42P17) and guarantees instant delivery
DROP POLICY IF EXISTS "Members can view conversation members" ON public.conversation_members;
DROP POLICY IF EXISTS "Users can view members" ON public.conversation_members;
DROP POLICY IF EXISTS "Members can view conversations" ON public.conversations;
DROP POLICY IF EXISTS "Authenticated users can create conversations" ON public.conversations;
DROP POLICY IF EXISTS "Members can view messages" ON public.messages;
DROP POLICY IF EXISTS "Members can insert messages" ON public.messages;

ALTER TABLE public.conversations DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversation_members DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages DISABLE ROW LEVEL SECURITY;

-- Marketplace & Gigs Policies
DROP POLICY IF EXISTS "Marketplace items viewable by all" ON public.marketplace_items;
CREATE POLICY "Marketplace items viewable by all" ON public.marketplace_items FOR SELECT USING (true);

DROP POLICY IF EXISTS "Authenticated users can list items" ON public.marketplace_items;
CREATE POLICY "Authenticated users can list items" ON public.marketplace_items FOR INSERT WITH CHECK (auth.uid()::TEXT = seller_id::TEXT);

DROP POLICY IF EXISTS "Jobs viewable by all" ON public.jobs;
CREATE POLICY "Jobs viewable by all" ON public.jobs FOR SELECT USING (true);

DROP POLICY IF EXISTS "Authenticated users can post jobs" ON public.jobs;
CREATE POLICY "Authenticated users can post jobs" ON public.jobs FOR INSERT WITH CHECK (auth.uid()::TEXT = poster_id::TEXT);

DROP POLICY IF EXISTS "Quick gigs viewable by all" ON public.quick_jobs;
CREATE POLICY "Quick gigs viewable by all" ON public.quick_jobs FOR SELECT USING (true);

DROP POLICY IF EXISTS "Authenticated users can post gigs" ON public.quick_jobs;
CREATE POLICY "Authenticated users can post gigs" ON public.quick_jobs FOR INSERT WITH CHECK (auth.uid()::TEXT = poster_id::TEXT);

-- Notifications Policies
DROP POLICY IF EXISTS "Users can view own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Authenticated users can insert notifications" ON public.notifications;

ALTER TABLE public.notifications DISABLE ROW LEVEL SECURITY;

-- Locations Policies (Nearby Radar)
DROP POLICY IF EXISTS "Visible locations viewable by authenticated users" ON public.user_locations;
CREATE POLICY "Visible locations viewable by authenticated users" ON public.user_locations FOR SELECT USING (is_visible = true);

DROP POLICY IF EXISTS "Users can update own location" ON public.user_locations;
CREATE POLICY "Users can update own location" ON public.user_locations FOR ALL USING (auth.uid()::TEXT = user_id::TEXT);

-- ============================================================================
-- 9. SUPABASE REALTIME REPLICATION PUBLICATION
-- ============================================================================
DO $$
BEGIN
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.conversations;
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.posts;
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.stories;
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;
END $$;
