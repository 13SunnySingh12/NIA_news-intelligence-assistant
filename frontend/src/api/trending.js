import { api } from './client';

export function getTrending(category, { signal } = {}) {
  return api.get('/api/trending', { params: category ? { category } : undefined, signal });
}
