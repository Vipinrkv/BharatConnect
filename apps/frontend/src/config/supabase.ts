import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://placeholder.supabase.co';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || 'placeholder_key';

// Initialize Supabase Client
export const supabase = createClient(supabaseUrl, supabaseAnonKey);

/**
 * Trigger SMS OTP verification
 */
export async function sendOtp(phoneNumber: string) {
  const { data, error } = await supabase.auth.signInWithOtp({
    phone: phoneNumber,
  });
  if (error) throw error;
  return data;
}

/**
 * Verify SMS OTP code
 */
export async function verifyOtp(phoneNumber: string, token: string) {
  const { data, error } = await supabase.auth.verifyOtp({
    phone: phoneNumber,
    token: token,
    type: 'sms',
  });
  if (error) throw error;
  return data; // Returns session JWT
}
