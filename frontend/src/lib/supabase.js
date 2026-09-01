import { createClient } from '@supabase/supabase-js';
import { supabaseUrl as url, supabaseAnonKey as anonKey } from './env';

/** True only when both public Supabase values are present. */
export const isSupabaseConfigured = Boolean(url && anonKey);

/**
 * The Supabase client, or null when the app hasn't been configured yet.
 * Auth helpers degrade gracefully so the UI still renders before setup.
 */
export const supabase = isSupabaseConfigured
  ? createClient(url, anonKey, {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
        detectSessionInUrl: true,
      },
    })
  : null;
