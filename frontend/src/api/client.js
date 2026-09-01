import { supabase } from '../lib/supabase';
import { apiBaseUrl as BASE_URL } from '../lib/env';

async function getAccessToken() {
  if (!supabase) return null;
  const { data } = await supabase.auth.getSession();
  return data?.session?.access_token ?? null;
}

function friendlyForStatus(status) {
  if (status === 401) return 'Please sign in to continue.';
  if (status === 403) return "You don't have access to that.";
  if (status === 404) return "We couldn't find what you were looking for.";
  if (status === 429) return "You're doing that a lot — please try again shortly.";
  if (status >= 500) return 'Something went wrong on our side. Please try again.';
  return 'That request could not be completed.';
}

/**
 * Single entry point for backend calls. Attaches the Supabase JWT, serializes
 * JSON, and normalizes errors into a friendly Error with { code, status }.
 */
export async function apiRequest(path, { method = 'GET', body, params, signal } = {}) {
  const url = new URL(path, BASE_URL);
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, value);
      }
    });
  }

  const headers = { 'Content-Type': 'application/json' };
  const token = await getAccessToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(url, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal,
    });
  } catch (networkError) {
    if (networkError.name === 'AbortError') throw networkError;
    const err = new Error('Cannot reach the server. Check your connection and try again.');
    err.code = 'network_error';
    throw err;
  }

  if (response.status === 204) return null;

  let data = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    const err = new Error(data?.message || friendlyForStatus(response.status));
    err.code = data?.error || 'error';
    err.status = response.status;
    throw err;
  }
  return data;
}

export const api = {
  get: (path, options) => apiRequest(path, { ...options, method: 'GET' }),
  post: (path, body, options) => apiRequest(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => apiRequest(path, { ...options, method: 'PUT', body }),
  del: (path, options) => apiRequest(path, { ...options, method: 'DELETE' }),
};
