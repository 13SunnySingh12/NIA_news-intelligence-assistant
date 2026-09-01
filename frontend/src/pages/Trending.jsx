import { useEffect, useState } from 'react';
import { TrendingUp } from 'lucide-react';
import { getTrending } from '../api/trending';
import { ArticleCard } from '../components/ArticleCard';
import { ArticleGridSkeleton } from '../components/ArticleGrid';
import { EmptyState, ErrorState } from '../components/ui/states';
import { CATEGORIES } from '../lib/categories';

const FILTERS = [{ slug: '', label: 'All' }, ...CATEGORIES];

export default function Trending() {
  const [category, setCategory] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    getTrending(category || undefined, { signal: controller.signal })
      .then((data) => {
        if (!active) return;
        setItems(data || []);
        setLoading(false);
      })
      .catch((err) => {
        if (!active || err.name === 'AbortError') return;
        setError(err.message || 'Could not load trending stories.');
        setLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [category, reloadKey]);

  const handleBookmarkChange = (id, value) =>
    setItems((current) => current.map((a) => (a.id === id ? { ...a, bookmarked: value } : a)));

  return (
    <section>
      <div className="mb-5">
        <h1 className="text-2xl font-bold tracking-tight text-ink">Trending</h1>
        <p className="mt-1 text-sm text-muted">The most-read stories in the last 24 hours.</p>
      </div>

      <div className="mb-6 flex gap-2 overflow-x-auto pb-1
        [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {FILTERS.map((filter) => (
          <button
            key={filter.slug || 'all'}
            onClick={() => setCategory(filter.slug)}
            aria-pressed={category === filter.slug}
            className={`whitespace-nowrap rounded-full px-3.5 py-1.5 text-sm font-medium transition-colors
              ${category === filter.slug ? 'bg-ink text-white' : 'bg-surface-2 text-muted hover:text-ink'}`}
          >
            {filter.label}
          </button>
        ))}
      </div>

      {loading ? (
        <ArticleGridSkeleton />
      ) : error ? (
        <ErrorState message={error} onRetry={() => setReloadKey((key) => key + 1)} />
      ) : items.length === 0 ? (
        <EmptyState
          icon={TrendingUp}
          title="Nothing trending yet"
          message="As people read articles, the most popular ones will show up here."
        />
      ) : (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((article) => (
            <ArticleCard key={article.id} article={article} onBookmarkChange={handleBookmarkChange} />
          ))}
        </div>
      )}
    </section>
  );
}
