import { useCallback, useState } from 'react';
import { Search as SearchIcon } from 'lucide-react';
import { searchArticles } from '../api/articles';
import { usePagedArticles } from '../hooks/usePagedArticles';
import { useDebounce } from '../hooks/useDebounce';
import { ArticleGrid } from '../components/ArticleGrid';
import { EmptyState } from '../components/ui/states';
import { Input } from '../components/ui/Input';

const MODES = [
  { key: 'keyword', label: 'Keyword' },
  { key: 'semantic', label: 'Smart' },
];

export default function Search() {
  const [query, setQuery] = useState('');
  const [mode, setMode] = useState('keyword');
  const debounced = useDebounce(query.trim(), 450);
  const hasQuery = debounced.length >= 2;

  const fetcher = useCallback(
    (page) => {
      if (!hasQuery) return Promise.resolve({ content: [], hasNext: false });
      return searchArticles(debounced, { mode, page, size: 12 });
    },
    [debounced, mode, hasQuery],
  );
  const state = usePagedArticles(fetcher, [debounced, mode]);

  const handleBookmarkChange = (id, value) =>
    state.setItems((items) => items.map((a) => (a.id === id ? { ...a, bookmarked: value } : a)));

  return (
    <section>
      <div className="mb-5">
        <h1 className="text-2xl font-bold tracking-tight text-ink">Search</h1>
        <p className="mt-1 text-sm text-muted">
          {mode === 'semantic'
            ? 'Smart search understands meaning, not just exact words.'
            : 'Find articles by keyword.'}
        </p>
      </div>

      <div className="mb-5 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <SearchIcon
            className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted"
            aria-hidden="true"
          />
          <Input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search the news…"
            aria-label="Search the news"
            className="pl-9"
          />
        </div>
        <div
          role="tablist"
          aria-label="Search mode"
          className="inline-flex rounded-xl border border-hair bg-surface p-1"
        >
          {MODES.map((item) => (
            <button
              key={item.key}
              role="tab"
              aria-selected={mode === item.key}
              onClick={() => setMode(item.key)}
              className={`rounded-lg px-4 py-1.5 text-sm font-medium transition-colors
                ${mode === item.key ? 'bg-brand text-white' : 'text-muted hover:text-ink'}`}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {!hasQuery ? (
        <EmptyState
          icon={SearchIcon}
          title="Search the news"
          message="Type at least two characters to find articles by keyword or meaning."
        />
      ) : (
        <ArticleGrid
          state={state}
          onBookmarkChange={handleBookmarkChange}
          empty={
            <EmptyState
              icon={SearchIcon}
              title="No results"
              message={`We couldn’t find anything for “${debounced}”. Try different words.`}
            />
          }
        />
      )}
    </section>
  );
}
