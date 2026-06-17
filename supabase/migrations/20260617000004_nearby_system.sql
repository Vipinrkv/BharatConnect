-- Database Migration: Nearby Right Now Hyperlocal Feed Integration

-- 1. Create Nearby Posts Table
CREATE TABLE public.nearby_posts (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    creator_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    title VARCHAR(150) NOT NULL,
    feed_type VARCHAR(15) CHECK (feed_type IN ('alert', 'discussion', 'observation')) NOT NULL,
    category VARCHAR(50) NOT NULL, -- e.g., 'water_supply', 'traffic', 'accident', 'power_cut', 'police'
    description TEXT NOT NULL,
    location GEOGRAPHY(Point, 4326) NOT NULL,
    attachment_url TEXT,
    reputation_score INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 2. Create Nearby Post Votes Table (Upvotes / Flags)
CREATE TABLE public.nearby_post_votes (
    post_id UUID REFERENCES public.nearby_posts(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    vote_type VARCHAR(10) CHECK (vote_type IN ('upvote', 'flag')) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    PRIMARY KEY (post_id, user_id)
);

-- 3. Spatial and Search Indexes
CREATE INDEX idx_nearby_posts_location ON public.nearby_posts USING GIST (location);
CREATE INDEX idx_nearby_posts_created ON public.nearby_posts (created_at DESC);

-- 4. Enable Row Level Security
ALTER TABLE public.nearby_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.nearby_post_votes ENABLE ROW LEVEL SECURITY;

-- 5. RLS Policies
CREATE POLICY select_nearby_posts ON public.nearby_posts
    FOR SELECT TO authenticated USING (true);

CREATE POLICY insert_nearby_posts ON public.nearby_posts
    FOR INSERT TO authenticated WITH CHECK (creator_id = auth.uid());

CREATE POLICY delete_own_post ON public.nearby_posts
    FOR DELETE TO authenticated USING (creator_id = auth.uid());

CREATE POLICY manage_nearby_votes ON public.nearby_post_votes
    FOR ALL TO authenticated USING (user_id = auth.uid());

-- 6. Trigger Function to auto-calculate reputation scores
CREATE OR REPLACE FUNCTION public.update_post_reputation()
RETURNS TRIGGER AS $$
DECLARE
    upvote_count INTEGER;
    flag_count INTEGER;
BEGIN
    -- Count upvotes & flags
    SELECT COUNT(*) INTO upvote_count FROM public.nearby_post_votes WHERE post_id = NEW.post_id AND vote_type = 'upvote';
    SELECT COUNT(*) INTO flag_count FROM public.nearby_post_votes WHERE post_id = NEW.post_id AND vote_type = 'flag';
    
    -- Update nearby_posts reputation
    UPDATE public.nearby_posts 
    SET reputation_score = (upvote_count - flag_count)
    WHERE id = NEW.post_id;
    
    -- Auto-moderation: If a post gets 5 or more flags, mark status or delete
    -- In this schema, we could decrement creator's trust score
    IF flag_count >= 5 THEN
        UPDATE public.profiles 
        SET helper_trust_score = GREATEST(0.00, helper_trust_score - 0.20)
        WHERE id = (SELECT creator_id FROM public.nearby_posts WHERE id = NEW.post_id);
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER on_vote_changed
  AFTER INSERT OR UPDATE ON public.nearby_post_votes
  FOR EACH ROW EXECUTE FUNCTION public.update_post_reputation();
