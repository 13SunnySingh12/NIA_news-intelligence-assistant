import { ArticleCard } from './ArticleCard';
import { Button } from './ui/Button';
import { ErrorState } from './ui/states';

function CardSkeleton() {
  return (
    <div className="overflow-hidden rounded-2xl border border-hair bg-surface shadow-card">
      <div className="aspect-[16/9] w-full animate-pulse bg-surface-2" />
      <div className="space-y-3 p-4">
        <div className="h-3 w-24 animate-pulse rounded bg-surface-2" />
        <div className="h-4 w-full animate-pulse rounded bg-surface-2" />
        <div className="h-4 w-2/3 animate-pulse rounded bg-surface-2" />
      </div>
    </div>
  );
}

export function ArticleGridSkeleton({ count = 6 }) {
  return (
    <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: count }).map((_, index) => (
        <CardSkeleton key={index} />
      ))}
    </div>
  );
}

/**
 * Renders a paged article list (from usePagedArticles) with the full set of
 * states: loading skeletons, error+retry, empty, results, and "load more".
 */
export function ArticleGrid({ state, onBookmarkChange, empty }) {
  const { items, loading, loadingMore, error, hasMore, loadMore, reload } = state;

  if (loading) return <ArticleGridSkeleton />;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  if (!items.length) return empty;

  return (
    <>
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((article) => (
          <ArticleCard key={article.id} article={article} onBookmarkChange={onBookmarkChange} />
        ))}
      </div>
      {hasMore && (
        <div className="mt-8 flex justify-center">
          <Button variant="secondary" onClick={loadMore} loading={loadingMore}>
            Load more
          </Button>
        </div>
      )}
    </>
  );
}

export default ArticleGrid;
