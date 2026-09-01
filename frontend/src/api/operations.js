import { api } from './client';

/** Current state of one backend operation (status polling is UI-only). */
export function getOperation(id, { signal } = {}) {
  return api.get(`/api/operations/${id}`, { signal });
}

/** The user's in-flight operation of a type, or null when nothing is running. */
export function getActiveOperation(type, { signal } = {}) {
  return api.get('/api/operations/active', { params: { type }, signal });
}
