import { useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Newspaper } from 'lucide-react';
import { getByCategory } from '../api/articles';
import { usePagedArticles } from '../hooks/usePagedArticles';
import { ArticleGrid } from '../components/ArticleGrid';
import { EmptyState } from '../components/ui/states';
import { labelForCategory, CATEGORY_LABELS } from '../lib/categories';

export default function Category() {
  const { name } = useParams();
  const isKnown = Boolean(CATEGORY_LABELS[name]);

  const fetcher = useCallback((page) => getByCategory(name, { page, size: 12 }), [name]);
  const state = usePagedArticles(fetcher, [name]);

  const handleBookmarkChange = (id, value) =>
    state.setItems((items) => items.map((a) => (a.id === id ? { ...a, bookmarked: value } : a)));

  if (!isKnown) {
    return (
      <EmptyState
        icon={Newspaper}
        title="Unknown category"
        message="That category doesn’t exist. Choose one from the bar above."
      />
    );
  }

  return (
    <section>
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-ink">{labelForCategory(name)}</h1>
        <p className="mt-1 text-sm text-muted">The latest {labelForCategory(name).toLowerCase()} stories.</p>
      </div>

      <ArticleGrid
        state={state}
        onBookmarkChange={handleBookmarkChange}
        empty={
          <EmptyState
            icon={Newspaper}
            title="No articles yet"
            message="We don’t have stories in this category right now. Check back soon."
          />
        }
      />
    </section>
  );
}
