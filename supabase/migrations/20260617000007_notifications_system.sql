-- Database Migration: Notifications & Activity System Integration

-- 1. Create User Notification Settings Table (Quiet Hours & Muting)
CREATE TABLE public.user_notification_settings (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    quiet_hours_enabled BOOLEAN DEFAULT FALSE,
    quiet_hours_start TIME DEFAULT '22:00:00'::TIME,
    quiet_hours_end TIME DEFAULT '07:00:00'::TIME,
    mute_groups BOOLEAN DEFAULT FALSE,
    mute_nearby BOOLEAN DEFAULT FALSE,
    mute_help BOOLEAN DEFAULT FALSE,
    mute_marketplace BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 2. Create Notification History Table
CREATE TABLE public.notification_history (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(30) CHECK (category IN ('message', 'group', 'nearby', 'help', 'marketplace')) NOT NULL,
    metadata JSONB,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Indices
CREATE INDEX idx_notification_history_user ON public.notification_history (user_id);
CREATE INDEX idx_notification_history_unread ON public.notification_history (user_id, is_read);

-- 3. Enable Row Level Security
ALTER TABLE public.user_notification_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_history ENABLE ROW LEVEL SECURITY;

-- 4. RLS Policies
CREATE POLICY manage_own_notification_settings ON public.user_notification_settings
    FOR ALL TO authenticated USING (user_id = auth.uid());

CREATE POLICY select_own_notification_history ON public.notification_history
    FOR SELECT TO authenticated USING (user_id = auth.uid());

CREATE POLICY update_own_notification_history ON public.notification_history
    FOR UPDATE TO authenticated USING (user_id = auth.uid());

-- 5. Trigger to automatically create default notification settings on new profile registration
CREATE OR REPLACE FUNCTION public.handle_new_user_notification_settings()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.user_notification_settings (user_id) VALUES (NEW.id);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER on_profile_created_notification_settings
  AFTER INSERT ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user_notification_settings();
