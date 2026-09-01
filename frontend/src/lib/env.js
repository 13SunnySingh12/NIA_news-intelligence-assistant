// Frontend configuration. Only VITE_* values exist in the browser; backend
// secrets never reach here. Validated once at startup so missing config shows a
// clear message instead of an unexplained blank/loading screen.

export const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
export const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/** Names of any required public variables that are missing. */
export const missingFrontendEnv = Object.entries({
  VITE_SUPABASE_URL: supabaseUrl,
  VITE_SUPABASE_ANON_KEY: supabaseAnonKey,
})
  .filter(([, value]) => !value)
  .map(([name]) => name);

if (missingFrontendEnv.length && import.meta.env.DEV) {
  // Non-secret, developer-facing guidance.
  console.warn(
    `[NIA] Missing frontend config: ${missingFrontendEnv.join(', ')}. ` +
      'Set them (VITE_* only) in the root .env. Authentication stays disabled until then.',
  );
}
