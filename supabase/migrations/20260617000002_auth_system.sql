-- Database Migration: Auth & Session Tracking System Upgrade

-- 1. Alter Profiles Table with extended auth metadata
ALTER TABLE public.profiles 
  ADD COLUMN IF NOT EXISTS username VARCHAR(50) UNIQUE,
  ADD COLUMN IF NOT EXISTS email VARCHAR(255),
  ADD COLUMN IF NOT EXISTS verification_level INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS language VARCHAR(10) DEFAULT 'en';

-- Create index on username for quick search
CREATE INDEX IF NOT EXISTS idx_profiles_username ON public.profiles (username);

-- 2. Create User Sessions Table (Multi-Device Login & Revocation)
CREATE TABLE public.user_sessions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    device_name VARCHAR(150),
    ip_address VARCHAR(45),
    user_agent TEXT,
    refresh_token_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    last_active_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_user_sessions_user ON public.user_sessions (user_id);
CREATE INDEX idx_user_sessions_token_hash ON public.user_sessions (refresh_token_hash);

-- 3. Create Audit Logs Table
CREATE TABLE public.audit_logs (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL, -- e.g., 'LOGIN_EMAIL', 'LOGOUT', 'TOKEN_REFRESH', 'SESSION_REVOKED'
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_audit_logs_user_action ON public.audit_logs (user_id, action);

-- 4. Row Level Security for Sessions & Audit Logs
ALTER TABLE public.user_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- Sessions RLS Policies
CREATE POLICY "Users can view own sessions" ON public.user_sessions
    FOR SELECT TO authenticated USING (user_id = auth.uid());

CREATE POLICY "Users can revoke own sessions" ON public.user_sessions
    FOR UPDATE TO authenticated USING (user_id = auth.uid());

-- Audit Logs RLS Policies
CREATE POLICY "Users can view own audit logs" ON public.audit_logs
    FOR SELECT TO authenticated USING (user_id = auth.uid());
