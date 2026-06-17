-- 1. Enable Required Extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Core Tables Definitions
CREATE TABLE public.profiles (
    id UUID REFERENCES auth.users ON DELETE CASCADE PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    avatar_url TEXT,
    location_coordinates GEOGRAPHY(Point, 4326),
    location_updated_at TIMESTAMPTZ,
    is_verified_helper BOOLEAN DEFAULT FALSE,
    helper_trust_score NUMERIC(3, 2) DEFAULT 5.00 CHECK (helper_trust_score BETWEEN 0.00 AND 5.00),
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TABLE public.chats (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    type VARCHAR(10) CHECK (type IN ('direct', 'group')) NOT NULL,
    title VARCHAR(100),
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TABLE public.chat_members (
    chat_id UUID REFERENCES public.chats(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    role VARCHAR(10) CHECK (role IN ('admin', 'member')) DEFAULT 'member',
    joined_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    last_read_message_id UUID,
    PRIMARY KEY (chat_id, profile_id)
);

CREATE TABLE public.messages (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    chat_id UUID REFERENCES public.chats(id) ON DELETE CASCADE NOT NULL,
    sender_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    content_type VARCHAR(15) CHECK (content_type IN ('text', 'image', 'video', 'audio', 'location')) NOT NULL,
    text_content TEXT,
    attachment_url TEXT,
    location_content GEOGRAPHY(Point, 4326),
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TABLE public.message_receipts (
    message_id UUID REFERENCES public.messages(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    status VARCHAR(10) CHECK (status IN ('sent', 'delivered', 'read')) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    PRIMARY KEY (message_id, profile_id)
);

CREATE TABLE public.help_requests (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    requester_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    location GEOGRAPHY(Point, 4326) NOT NULL,
    status VARCHAR(15) CHECK (status IN ('open', 'assigned', 'resolved')) DEFAULT 'open',
    min_trust_score NUMERIC(3, 2) DEFAULT 3.00,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TABLE public.help_responses (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    request_id UUID REFERENCES public.help_requests(id) ON DELETE CASCADE NOT NULL,
    volunteer_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    status VARCHAR(15) CHECK (status IN ('proposed', 'accepted', 'rejected')) DEFAULT 'proposed',
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TABLE public.need_it_now_requests (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    requester_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    budget_estimate NUMERIC(10, 2),
    location GEOGRAPHY(Point, 4326) NOT NULL,
    status VARCHAR(15) CHECK (status IN ('active', 'fulfilled', 'expired')) DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TABLE public.need_it_now_bids (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    request_id UUID REFERENCES public.need_it_now_requests(id) ON DELETE CASCADE NOT NULL,
    bidder_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    bid_amount NUMERIC(10, 2) NOT NULL,
    message TEXT,
    status VARCHAR(15) CHECK (status IN ('pending', 'accepted', 'rejected')) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 3. Performance & Geospatial Indices
CREATE INDEX idx_profiles_location ON public.profiles USING GIST (location_coordinates);
CREATE INDEX idx_help_requests_location ON public.help_requests USING GIST (location);
CREATE INDEX idx_need_it_now_requests_location ON public.need_it_now_requests USING GIST (location);

CREATE INDEX idx_messages_chat_created ON public.messages (chat_id, created_at DESC);
CREATE INDEX idx_message_receipts_profile_status ON public.message_receipts (profile_id, status);

-- 4. Storage Bucket Setup for Media
INSERT INTO storage.buckets (id, name, public) 
VALUES ('media-attachments', 'media-attachments', true)
ON CONFLICT (id) DO NOTHING;

-- 5. Automations: Profile Synced Trigger
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, phone, display_name, avatar_url)
  VALUES (
    new.id,
    COALESCE(new.phone, ''),
    COALESCE(new.raw_user_meta_data->>'display_name', 'User_' || substr(new.id::text, 1, 8)),
    new.raw_user_meta_data->>'avatar_url'
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 6. Helper Functions (Geospatial Discovery)
CREATE OR REPLACE FUNCTION public.get_nearby_users(
  user_lat DOUBLE PRECISION,
  user_lng DOUBLE PRECISION,
  radius_meters DOUBLE PRECISION
)
RETURNS TABLE (
  id UUID,
  display_name VARCHAR,
  avatar_url TEXT,
  distance DOUBLE PRECISION,
  is_verified_helper BOOLEAN,
  helper_trust_score NUMERIC
) AS $$
BEGIN
  RETURN QUERY
  SELECT 
    p.id,
    p.display_name,
    p.avatar_url,
    ST_Distance(p.location_coordinates, ST_SetSRID(ST_Point(user_lng, user_lat), 4326)::geography) AS distance,
    p.is_verified_helper,
    p.helper_trust_score
  FROM public.profiles p
  WHERE ST_DWithin(p.location_coordinates, ST_SetSRID(ST_Point(user_lng, user_lat), 4326)::geography, radius_meters)
  ORDER BY distance ASC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.get_nearby_help_requests(
  user_lat DOUBLE PRECISION,
  user_lng DOUBLE PRECISION,
  radius_meters DOUBLE PRECISION
)
RETURNS TABLE (
  id UUID,
  requester_id UUID,
  title VARCHAR,
  description TEXT,
  category VARCHAR,
  distance DOUBLE PRECISION,
  status VARCHAR,
  min_trust_score NUMERIC,
  created_at TIMESTAMPTZ
) AS $$
BEGIN
  RETURN QUERY
  SELECT 
    hr.id,
    hr.requester_id,
    hr.title,
    hr.description,
    hr.category,
    ST_Distance(hr.location, ST_SetSRID(ST_Point(user_lng, user_lat), 4326)::geography) AS distance,
    hr.status,
    hr.min_trust_score,
    hr.created_at
  FROM public.help_requests hr
  WHERE hr.status = 'open' 
    AND ST_DWithin(hr.location, ST_SetSRID(ST_Point(user_lng, user_lat), 4326)::geography, radius_meters)
  ORDER BY distance ASC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 7. Row Level Security Policies Configurations
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_receipts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.help_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.help_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.need_it_now_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.need_it_now_bids ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
CREATE POLICY select_profiles ON public.profiles FOR SELECT TO authenticated USING (true);
CREATE POLICY update_own_profile ON public.profiles FOR UPDATE TO authenticated USING (id = auth.uid());

-- Chats Policies
CREATE POLICY select_chats ON public.chats FOR SELECT TO authenticated
  USING (EXISTS (SELECT 1 FROM public.chat_members WHERE chat_members.chat_id = chats.id AND chat_members.profile_id = auth.uid()));
CREATE POLICY insert_chats ON public.chats FOR INSERT TO authenticated WITH CHECK (true);

-- Chat Members Policies
CREATE POLICY select_members ON public.chat_members FOR SELECT TO authenticated
  USING (EXISTS (SELECT 1 FROM public.chat_members cm WHERE cm.chat_id = chat_members.chat_id AND cm.profile_id = auth.uid()));
CREATE POLICY insert_members ON public.chat_members FOR INSERT TO authenticated WITH CHECK (true);

-- Messages Policies
CREATE POLICY select_messages ON public.messages FOR SELECT TO authenticated
  USING (EXISTS (SELECT 1 FROM public.chat_members WHERE chat_members.chat_id = messages.chat_id AND chat_members.profile_id = auth.uid()));
CREATE POLICY insert_messages ON public.messages FOR INSERT TO authenticated
  WITH CHECK (sender_id = auth.uid() AND EXISTS (SELECT 1 FROM public.chat_members WHERE chat_members.chat_id = messages.chat_id AND chat_members.profile_id = auth.uid()));

-- Message Receipts Policies
CREATE POLICY select_receipts ON public.message_receipts FOR SELECT TO authenticated
  USING (EXISTS (SELECT 1 FROM public.chat_members WHERE chat_members.chat_id = (SELECT chat_id FROM public.messages WHERE messages.id = message_receipts.message_id) AND chat_members.profile_id = auth.uid()));
CREATE POLICY insert_update_receipts ON public.message_receipts FOR ALL TO authenticated
  USING (profile_id = auth.uid());

-- Help Requests Policies
CREATE POLICY select_help ON public.help_requests FOR SELECT TO authenticated USING (true);
CREATE POLICY insert_help ON public.help_requests FOR INSERT TO authenticated WITH CHECK (requester_id = auth.uid());
CREATE POLICY update_own_help ON public.help_requests FOR UPDATE TO authenticated USING (requester_id = auth.uid());

-- Help Responses Policies
CREATE POLICY select_responses ON public.help_responses FOR SELECT TO authenticated USING (true);
CREATE POLICY insert_own_response ON public.help_responses FOR INSERT TO authenticated WITH CHECK (volunteer_id = auth.uid());
CREATE POLICY update_own_response ON public.help_responses FOR UPDATE TO authenticated USING (volunteer_id = auth.uid());

-- Need It Now Policies
CREATE POLICY select_gigs ON public.need_it_now_requests FOR SELECT TO authenticated USING (true);
CREATE POLICY insert_gig ON public.need_it_now_requests FOR INSERT TO authenticated WITH CHECK (requester_id = auth.uid());
CREATE POLICY update_own_gig ON public.need_it_now_requests FOR UPDATE TO authenticated USING (requester_id = auth.uid());

-- Need It Now Bids Policies
CREATE POLICY select_bids ON public.need_it_now_bids FOR SELECT TO authenticated
  USING (bidder_id = auth.uid() OR EXISTS (SELECT 1 FROM public.need_it_now_requests r WHERE r.id = need_it_now_bids.request_id AND r.requester_id = auth.uid()));
CREATE POLICY insert_own_bid ON public.need_it_now_bids FOR INSERT TO authenticated WITH CHECK (bidder_id = auth.uid());
CREATE POLICY update_own_bid ON public.need_it_now_bids FOR UPDATE TO authenticated USING (bidder_id = auth.uid());

-- Storage Objects Policies
CREATE POLICY "Allow media upload" ON storage.objects FOR INSERT TO authenticated WITH CHECK (bucket_id = 'media-attachments');
CREATE POLICY "Allow public media download" ON storage.objects FOR SELECT TO authenticated USING (bucket_id = 'media-attachments');
