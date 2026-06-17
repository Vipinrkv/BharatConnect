-- Database Migration: Admin Platform & Moderation Upgrades

-- 1. Extend Profiles Table with Block Flags and System roles (RBAC)
ALTER TABLE public.profiles 
  ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS role VARCHAR(10) CHECK (role IN ('user', 'admin')) DEFAULT 'user';

-- Set seed Amit Admin as admin in database
UPDATE public.profiles SET role = 'admin' WHERE id = 'c3d4e5f6-a7b8-91c2-d3e4-f5a6b7c8d9e0';

-- 2. Create Helper Verification Requests Table
CREATE TABLE public.helper_verification_requests (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    target_level VARCHAR(10) CHECK (target_level IN ('bronze', 'silver', 'gold')) NOT NULL,
    document_url TEXT NOT NULL,
    status VARCHAR(15) CHECK (status IN ('pending', 'approved', 'rejected')) DEFAULT 'pending',
    reviewer_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    reviewed_at TIMESTAMPTZ
);

-- 3. Create Abuse Reports Table
CREATE TABLE public.user_reports (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    reporter_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL NOT NULL,
    reported_user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    content_type VARCHAR(30) CHECK (content_type IN ('post', 'message', 'profile')) NOT NULL,
    content_id UUID NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(15) CHECK (status IN ('pending', 'investigating', 'resolved', 'dismissed')) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Indices
CREATE INDEX idx_verification_requests_status ON public.helper_verification_requests (status);
CREATE INDEX idx_user_reports_status ON public.user_reports (status);

-- 4. Enable Row Level Security
ALTER TABLE public.helper_verification_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_reports ENABLE ROW LEVEL SECURITY;

-- 5. RLS Policies (RBAC Enforced: Only admins can view verifications and reports)
CREATE POLICY admin_select_verifications ON public.helper_verification_requests
    FOR SELECT TO authenticated USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));

CREATE POLICY user_manage_own_verification ON public.helper_verification_requests
    FOR ALL TO authenticated USING (user_id = auth.uid());

CREATE POLICY admin_manage_reports ON public.user_reports
    FOR ALL TO authenticated USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));

CREATE POLICY user_insert_reports ON public.user_reports
    FOR INSERT TO authenticated WITH CHECK (reporter_id = auth.uid());
