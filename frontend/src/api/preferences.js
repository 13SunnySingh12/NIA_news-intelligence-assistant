import { api } from './client';

export function getPreferences({ signal } = {}) {
  return api.get('/api/preferences', { signal });
}

export function updatePreferences({ favoriteCategories, languages, countries }) {
  return api.put('/api/preferences', { favoriteCategories, languages, countries });
}
