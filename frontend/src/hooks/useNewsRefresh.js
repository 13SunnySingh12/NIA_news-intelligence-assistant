import { useCallback, useEffect, useRef, useState } from 'react';
import { getActiveOperation, getOperation } from '../api/operations';
import { refreshNews } from '../api/articles';

const NEWS_REFRESH = 'NEWS_REFRESH';
const POLL_MS = 2500;

function isTerminal(status) {
  return status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED';
}

/**
 * Tracks the backend news-refresh operation. The backend/Supabase record is the
 * source of truth — this hook only reads it. It reconnects to any in-flight
 * refresh on mount (so navigating away and back never restarts or loses it),
 * polls for UI visibility while running, and re-checks when the tab becomes
 * visible again. Polling never performs the work; the backend does.
 */
export function useNewsRefresh({ onCompleted } = {}) {
  const [operation, setOperation] = useState(null);
  const [error, setError] = useState('');

  const pollRef = useRef(null);
  const operationRef = useRef(null);
  operationRef.current = operation;
  const onCompletedRef = useRef(onCompleted);
  onCompletedRef.current = onCompleted;

  const stopPolling = useCallback(() => {
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
  }, []);

  const refreshStatus = useCallback(
    async (id) => {
      try {
        const op = await getOperation(id);
        setOperation(op);
        if (op.status === 'COMPLETED') {
          stopPolling();
          onCompletedRef.current?.(op);
        } else if (isTerminal(op.status)) {
          stopPolling();
        }
      } catch {
        // Temporary network issue — keep polling; the backend remains authoritative.
      }
    },
    [stopPolling],
  );

  const startPolling = useCallback(
    (id) => {
      stopPolling();
      pollRef.current = setInterval(() => refreshStatus(id), POLL_MS);
    },
    [refreshStatus, stopPolling],
  );

  // On mount: reconnect to an existing refresh. Never auto-starts one.
  useEffect(() => {
    let cancelled = false;
    getActiveOperation(NEWS_REFRESH)
      .then((op) => {
        if (cancelled || !op) return;
        setOperation(op);
        startPolling(op.id);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
      stopPolling();
    };
  }, [startPolling, stopPolling]);

  // When the tab becomes visible again, re-fetch the latest status (UI only).
  useEffect(() => {
    const onVisible = () => {
      const current = operationRef.current;
      if (document.visibilityState === 'visible' && current && !isTerminal(current.status)) {
        refreshStatus(current.id);
      }
    };
    document.addEventListener('visibilitychange', onVisible);
    return () => document.removeEventListener('visibilitychange', onVisible);
  }, [refreshStatus]);

  const start = useCallback(
    async (category) => {
      setError('');
      try {
        // The backend returns the existing operation if one is already running (dedup).
        const op = await refreshNews(category);
        setOperation(op);
        if (op.status === 'PENDING' || op.status === 'RUNNING') {
          startPolling(op.id);
        } else if (op.status === 'COMPLETED') {
          onCompletedRef.current?.(op);
        }
      } catch (err) {
        setError(err.message || 'Could not start the update. Please try again.');
      }
    },
    [startPolling],
  );

  const isActive = Boolean(operation) && (operation.status === 'PENDING' || operation.status === 'RUNNING');

  return { operation, isActive, error, start };
}
