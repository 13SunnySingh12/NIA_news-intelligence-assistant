import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Loads a paginated article list with idle/loading/error/empty handling and a
 * "load more" action. `fetcher(page)` returns the backend PageResponse; `deps`
 * triggers a fresh reload (e.g. when the category changes). Stale responses are
 * ignored so rapid changes never render out-of-order results.
 */
export function usePagedArticles(fetcher, deps = []) {
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;

  const [state, setState] = useState({
    items: [],
    page: 0,
    loading: true,
    loadingMore: false,
    error: null,
    hasMore: false,
  });
  const requestId = useRef(0);

  const run = useCallback(async (page, replace) => {
    const id = ++requestId.current;
    setState((s) => ({
      ...s,
      loading: replace ? true : s.loading,
      loadingMore: replace ? false : true,
      error: null,
    }));
    try {
      const data = await fetcherRef.current(page);
      if (id !== requestId.current) return;
      setState((s) => ({
        items: replace ? data?.content ?? [] : [...s.items, ...(data?.content ?? [])],
        page,
        loading: false,
        loadingMore: false,
        error: null,
        hasMore: Boolean(data?.hasNext),
      }));
    } catch (err) {
      if (id !== requestId.current) return;
      setState((s) => ({
        ...s,
        loading: false,
        loadingMore: false,
        error: err.message || 'Could not load articles.',
      }));
    }
  }, []);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    run(0, true);
  }, deps);

  const setItems = (updater) =>
    setState((s) => ({ ...s, items: typeof updater === 'function' ? updater(s.items) : updater }));

  return {
    ...state,
    loadMore: () => run(state.page + 1, false),
    reload: () => run(0, true),
    setItems,
  };
}
