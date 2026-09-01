import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

// Supabase is mocked so the client can be tested without a real session.
const getSession = vi.fn();
vi.mock('../lib/supabase', () => ({ supabase: { auth: { getSession: () => getSession() } } }));
vi.mock('../lib/env', () => ({ apiBaseUrl: 'http://api.test' }));

const { apiRequest } = await import('../api/client');

function jsonResponse(body, status = 200) {
  return { ok: status < 400, status, json: async () => body };
}

describe('apiRequest', () => {
  beforeEach(() => {
    getSession.mockResolvedValue({ data: { session: { access_token: 'test-token' } } });
    global.fetch = vi.fn().mockResolvedValue(jsonResponse({ ok: true }));
  });
  afterEach(() => vi.restoreAllMocks());

  it('attaches the Supabase JWT as a Bearer token', async () => {
    await apiRequest('/api/articles');
    const [, options] = global.fetch.mock.calls[0];
    expect(options.headers.Authorization).toBe('Bearer test-token');
  });

  it('omits the Authorization header when there is no session', async () => {
    getSession.mockResolvedValue({ data: { session: null } });
    await apiRequest('/api/articles');
    const [, options] = global.fetch.mock.calls[0];
    expect(options.headers.Authorization).toBeUndefined();
  });

  it('drops empty query params instead of sending blanks', async () => {
    await apiRequest('/api/articles', { params: { category: 'tech', page: '', missing: null } });
    const [url] = global.fetch.mock.calls[0];
    expect(url.toString()).toContain('category=tech');
    expect(url.toString()).not.toContain('page=');
    expect(url.toString()).not.toContain('missing');
  });

  it('surfaces the server message on an error response', async () => {
    global.fetch.mockResolvedValue(jsonResponse({ error: 'rate_limited', message: 'Slow down.' }, 429));
    await expect(apiRequest('/api/assistant/chat', { method: 'POST' }))
      .rejects.toMatchObject({ message: 'Slow down.', code: 'rate_limited', status: 429 });
  });

  it('never leaks internals when the server sends no body', async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 500, json: async () => { throw new Error('no body'); } });
    await expect(apiRequest('/api/articles')).rejects.toThrow(/something went wrong on our side/i);
  });

  it('reports a friendly message when the network is unreachable', async () => {
    global.fetch.mockRejectedValue(Object.assign(new Error('boom'), { name: 'TypeError' }));
    await expect(apiRequest('/api/articles')).rejects.toMatchObject({ code: 'network_error' });
  });

  it('returns null for a 204 No Content response', async () => {
    global.fetch.mockResolvedValue({ ok: true, status: 204, json: async () => ({}) });
    await expect(apiRequest('/api/bookmarks/x', { method: 'DELETE' })).resolves.toBeNull();
  });
});
