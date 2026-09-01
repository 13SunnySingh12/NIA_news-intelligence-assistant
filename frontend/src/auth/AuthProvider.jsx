import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { supabase, isSupabaseConfigured } from '../lib/supabase';
import { api } from '../api/client';

const AuthContext = createContext(null);

const NOT_CONFIGURED = {
  error: { message: 'Authentication isn’t configured yet. Add your Supabase keys to continue.' },
};

async function withClient(action) {
  if (!supabase) return NOT_CONFIGURED;
  return action();
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!supabase) {
      setLoading(false);
      return undefined;
    }
    let active = true;

    supabase.auth.getSession().then(({ data }) => {
      if (!active) return;
      setSession(data.session ?? null);
      setLoading(false);
    });

    const { data: listener } = supabase.auth.onAuthStateChange((event, newSession) => {
      setSession(newSession ?? null);
      if (event === 'SIGNED_IN') {
        // Ensure the user has a preferences row. Best-effort; ignore failures.
        api.post('/api/auth/sync').catch(() => {});
      }
    });

    return () => {
      active = false;
      listener?.subscription?.unsubscribe();
    };
  }, []);

  const value = useMemo(
    () => ({
      session,
      user: session?.user ?? null,
      loading,
      isConfigured: isSupabaseConfigured,
      signInWithPassword: (email, password) =>
        withClient(() => supabase.auth.signInWithPassword({ email, password })),
      signUp: (email, password) =>
        withClient(() =>
          supabase.auth.signUp({
            email,
            password,
            options: { emailRedirectTo: window.location.origin },
          }),
        ),
      signInWithOAuth: (provider) =>
        withClient(() =>
          supabase.auth.signInWithOAuth({
            provider,
            options: { redirectTo: window.location.origin },
          }),
        ),
      resetPassword: (email) =>
        withClient(() =>
          supabase.auth.resetPasswordForEmail(email, {
            redirectTo: `${window.location.origin}/reset-password`,
          }),
        ),
      updatePassword: (password) =>
        withClient(() => supabase.auth.updateUser({ password })),
      // Re-send the sign-up confirmation email (for a user who never got it).
      resendVerification: (email) =>
        withClient(() =>
          supabase.auth.resend({
            type: 'signup',
            email,
            options: { emailRedirectTo: window.location.origin },
          }),
        ),
      signOut: () => withClient(() => supabase.auth.signOut()),
    }),
    [session, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
