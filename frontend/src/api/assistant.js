import { api } from './client';

/** Ask the news assistant. `conversation` is the recent client-side history. */
export function askAssistant(question, conversation = [], { signal } = {}) {
  return api.post('/api/assistant/chat', { question, conversation }, { signal });
}

/** length: 'short' | 'detailed' */
export function summarizeArticle(articleId, length, { signal } = {}) {
  return api.post('/api/assistant/summarize', { articleId, length }, { signal });
}
