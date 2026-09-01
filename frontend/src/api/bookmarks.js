import { api } from './client';

export function listBookmarks({ page = 0, size = 20, signal } = {}) {
  return api.get('/api/bookmarks', { params: { page, size }, signal });
}

export function addBookmark(articleId) {
  return api.post('/api/bookmarks', { articleId });
}

export function removeBookmark(articleId) {
  return api.del(`/api/bookmarks/${articleId}`);
}
