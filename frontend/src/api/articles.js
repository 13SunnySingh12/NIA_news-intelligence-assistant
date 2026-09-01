import { api } from './client';

/** Personalized home feed. */
export function getFeed({ page = 0, size = 20, signal } = {}) {
  return api.get('/api/articles', { params: { page, size }, signal });
}

export function getByCategory(category, { page = 0, size = 20, signal } = {}) {
  return api.get(`/api/articles/category/${encodeURIComponent(category)}`, {
    params: { page, size },
    signal,
  });
}

export function getArticle(id, { signal } = {}) {
  return api.get(`/api/articles/${id}`, { signal });
}

/** mode: 'keyword' | 'semantic' */
export function searchArticles(query, { mode = 'keyword', page = 0, size = 20, signal } = {}) {
  return api.get('/api/articles/search', { params: { q: query, mode, page, size }, signal });
}

export function markRead(id) {
  return api.post(`/api/articles/${id}/read`);
}

export function refreshNews(category) {
  return api.post('/api/news/refresh', undefined, { params: category ? { category } : undefined });
}
