-- Database Migration: Verified Help & Service Recommendations Integration

-- 1. Helper Profiles Table
CREATE TABLE public.helper_profiles (
    id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    category VARCHAR(50) CHECK (category IN ('Maid', 'Cook', 'Tutor', 'Driver', 'Electrician', 'Plumber')) NOT NULL,
    experience_years INTEGER DEFAULT 0 CHECK (experience_years >= 0),
    skills_description TEXT,
    verification_level VARCHAR(10) CHECK (verification_level IN ('bronze', 'silver', 'gold')) DEFAULT 'bronze',
    average_rating NUMERIC(3, 2) DEFAULT 0.00 CHECK (average_rating BETWEEN 0.00 AND 5.00),
    review_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 2. Helper Reviews Table
CREATE TABLE public.helper_reviews (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    helper_id UUID REFERENCES public.helper_profiles(id) ON DELETE CASCADE NOT NULL,
    reviewer_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    rating INTEGER CHECK (rating BETWEEN 1 AND 5) NOT NULL,
    review_text TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT unique_helper_reviewer UNIQUE (helper_id, reviewer_id)
);

-- 3. Helper Bookings Table
CREATE TABLE public.helper_bookings (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    helper_id UUID REFERENCES public.helper_profiles(id) ON DELETE CASCADE NOT NULL,
    customer_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    chat_id UUID REFERENCES public.chats(id) ON DELETE SET NULL, -- Chat integration
    booking_date DATE NOT NULL,
    status VARCHAR(15) CHECK (status IN ('requested', 'confirmed', 'completed', 'cancelled')) DEFAULT 'requested',
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Indices
CREATE INDEX idx_helper_profiles_category ON public.helper_profiles (category);
CREATE INDEX idx_helper_reviews_helper ON public.helper_reviews (helper_id);
CREATE INDEX idx_helper_bookings_helper ON public.helper_bookings (helper_id);
CREATE INDEX idx_helper_bookings_customer ON public.helper_bookings (customer_id);

-- 4. Enable Row Level Security
ALTER TABLE public.helper_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.helper_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.helper_bookings ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY select_helper_profiles ON public.helper_profiles FOR SELECT TO authenticated USING (true);
CREATE POLICY manage_own_helper_profile ON public.helper_profiles FOR ALL TO authenticated USING (id = auth.uid());

CREATE POLICY select_reviews ON public.helper_reviews FOR SELECT TO authenticated USING (true);
CREATE POLICY insert_reviews ON public.helper_reviews FOR INSERT TO authenticated WITH CHECK (reviewer_id = auth.uid());

CREATE POLICY select_bookings ON public.helper_bookings FOR SELECT TO authenticated 
  USING (customer_id = auth.uid() OR helper_id = auth.uid());
CREATE POLICY insert_bookings ON public.helper_bookings FOR INSERT TO authenticated WITH CHECK (customer_id = auth.uid());
CREATE POLICY update_bookings ON public.helper_bookings FOR UPDATE TO authenticated 
  USING (customer_id = auth.uid() OR helper_id = auth.uid());

-- 5. Trigger Function to recalculate average rating on new reviews
CREATE OR REPLACE FUNCTION public.recalculate_helper_rating()
RETURNS TRIGGER AS $$
DECLARE
    new_avg NUMERIC(3, 2);
    new_count INTEGER;
BEGIN
    SELECT COALESCE(AVG(rating), 0.00), COUNT(*) 
    INTO new_avg, new_count
    FROM public.helper_reviews
    WHERE helper_id = NEW.helper_id;
    
    UPDATE public.helper_profiles 
    SET average_rating = new_avg,
        review_count = new_count
    WHERE id = NEW.helper_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER on_review_changed
  AFTER INSERT OR UPDATE OR DELETE ON public.helper_reviews
  FOR EACH ROW EXECUTE FUNCTION public.recalculate_helper_rating();
