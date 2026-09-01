import { useCallback } from 'react';
import { Newspaper, RefreshCw } from 'lucide-react';
import { getFeed } from '../api/articles';
import { usePagedArticles } from '../hooks/usePagedArticles';
import { useNewsRefresh } from '../hooks/useNewsRefresh';
import { ArticleGrid } from '../components/ArticleGrid';
import { EmptyState } from '../components/ui/states';
import { Button } from '../components/ui/Button';

export default function Home() {
  const fetcher = useCallback((page) => getFeed({ page, size: 12 }), []);
  const state = usePagedArticles(fetcher, []);

  // Refresh is a backend-owned operation: it keeps running if the user leaves,
  // and reconnects on return. When it completes, reload the feed to show results.
  const { operation, isActive, error: refreshError, start } = useNewsRefresh({
    onCompleted: () => state.reload(),
  });

  const handleBookmarkChange = (id, value) =>
    state.setItems((items) => items.map((a) => (a.id === id ? { ...a, bookmarked: value } : a)));

  return (
    <section>
      <div className="mb-6 flex items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-ink">Your feed</h1>
          <p className="mt-1 text-sm text-muted">
            Personalized news based on what you read and follow.
          </p>
          {isActive && (
            <p className="mt-1 text-sm text-muted" role="status">
              {operation.currentStep || 'Updating your news'}… {operation.progress}%
            </p>
          )}
          {!isActive && operation?.status === 'FAILED' && (
            <p className="mt-1 text-sm text-red-600">{operation.error || 'Update failed.'}</p>
          )}
          {refreshError && <p className="mt-1 text-sm text-red-600">{refreshError}</p>}
        </div>
        <Button variant="secondary" size="sm" onClick={() => start()} loading={isActive}>
          <RefreshCw className="h-4 w-4" aria-hidden="true" /> Refresh
        </Button>
      </div>

      <ArticleGrid
        state={state}
        onBookmarkChange={handleBookmarkChange}
        empty={
          <EmptyState
            icon={Newspaper}
            title="Your feed is empty"
            message="Pick favorite categories in your preferences, or fetch the latest news to get started."
            action={
              <Button size="sm" onClick={() => start()} loading={isActive}>
                Fetch latest news
              </Button>
            }
          />
        }
      />
    </section>
  );
}
