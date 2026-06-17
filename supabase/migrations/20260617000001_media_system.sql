-- Database Migration: Media Storage System Upgrade

-- 1. Create Storage Buckets
INSERT INTO storage.buckets (id, name, public) VALUES 
  ('avatars', 'avatars', true),
  ('banners', 'banners', true),
  ('chat-media', 'chat-media', false),
  ('voice-notes', 'voice-notes', false),
  ('documents', 'documents', false),
  ('temp-uploads', 'temp-uploads', false)
ON CONFLICT (id) DO NOTHING;

-- 2. Alter Profiles Table to support avatar and banner thumbnail caching
ALTER TABLE public.profiles 
  ADD COLUMN IF NOT EXISTS banner_url TEXT,
  ADD COLUMN IF NOT EXISTS avatar_thumbnail_url TEXT,
  ADD COLUMN IF NOT EXISTS banner_thumbnail_url TEXT,
  ADD COLUMN IF NOT EXISTS avatar_updated_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS banner_updated_at TIMESTAMPTZ;

-- 3. Alter Messages Table to support E2EE media size, integrity checks, and key references
ALTER TABLE public.messages 
  ADD COLUMN IF NOT EXISTS media_size INTEGER,
  ADD COLUMN IF NOT EXISTS checksum VARCHAR(64),
  ADD COLUMN IF NOT EXISTS encryption_key_reference TEXT;

-- 4. Storage Security Policies

-- A. Avatars Policies
CREATE POLICY "Public read avatars" ON storage.objects
    FOR SELECT TO public USING (bucket_id = 'avatars');

CREATE POLICY "Users can manage own avatar" ON storage.objects
    FOR ALL TO authenticated
    USING (bucket_id = 'avatars' AND (storage.foldername(name))[1] = auth.uid()::text);

-- B. Banners Policies
CREATE POLICY "Public read banners" ON storage.objects
    FOR SELECT TO public USING (bucket_id = 'banners');

CREATE POLICY "Users can manage own banner" ON storage.objects
    FOR ALL TO authenticated
    USING (bucket_id = 'banners' AND (storage.foldername(name))[1] = auth.uid()::text);

-- C. Chat Media (Images & Videos) Policies
CREATE POLICY "Allow members access to chat media" ON storage.objects
    FOR SELECT TO authenticated
    USING (
        bucket_id = 'chat-media'
        AND EXISTS (
            SELECT 1 FROM public.chat_members
            WHERE chat_members.chat_id::text = (storage.foldername(name))[1]
            AND chat_members.profile_id = auth.uid()
        )
    );

CREATE POLICY "Allow members upload chat media" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'chat-media'
        AND EXISTS (
            SELECT 1 FROM public.chat_members
            WHERE chat_members.chat_id::text = (storage.foldername(name))[1]
            AND chat_members.profile_id = auth.uid()
        )
    );

-- D. Voice Notes Policies
CREATE POLICY "Allow members access to voice notes" ON storage.objects
    FOR SELECT TO authenticated
    USING (
        bucket_id = 'voice-notes'
        AND EXISTS (
            SELECT 1 FROM public.chat_members
            WHERE chat_members.chat_id::text = (storage.foldername(name))[1]
            AND chat_members.profile_id = auth.uid()
        )
    );

CREATE POLICY "Allow members upload voice notes" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'voice-notes'
        AND EXISTS (
            SELECT 1 FROM public.chat_members
            WHERE chat_members.chat_id::text = (storage.foldername(name))[1]
            AND chat_members.profile_id = auth.uid()
        )
    );

-- E. Documents Policies
CREATE POLICY "Allow members access to documents" ON storage.objects
    FOR SELECT TO authenticated
    USING (
        bucket_id = 'documents'
        AND EXISTS (
            SELECT 1 FROM public.chat_members
            WHERE chat_members.chat_id::text = (storage.foldername(name))[1]
            AND chat_members.profile_id = auth.uid()
        )
    );

CREATE POLICY "Allow members upload documents" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'documents'
        AND EXISTS (
            SELECT 1 FROM public.chat_members
            WHERE chat_members.chat_id::text = (storage.foldername(name))[1]
            AND chat_members.profile_id = auth.uid()
        )
    );

-- F. Temp Uploads Policies
CREATE POLICY "Users can manage own temp uploads" ON storage.objects
    FOR ALL TO authenticated
    USING (bucket_id = 'temp-uploads' AND (storage.foldername(name))[1] = auth.uid()::text);
