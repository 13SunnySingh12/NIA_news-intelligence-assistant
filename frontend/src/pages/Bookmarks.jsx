import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Bookmark } from 'lucide-react';
import { listBookmarks } from '../api/bookmarks';
import { usePagedArticles } from '../hooks/usePagedArticles';
import { ArticleGrid } from '../components/ArticleGrid';
import { EmptyState } from '../components/ui/states';
import { Button } from '../components/ui/Button';

export default function Bookmarks() {
  const fetcher = useCallback((page) => listBookmarks({ page, size: 12 }), []);
  const state = usePagedArticles(fetcher, []);

  // Removing a bookmark here should drop it from the list immediately.
  const handleBookmarkChange = (id, value) => {
    if (!value) {
      state.setItems((items) => items.filter((a) => a.id !== id));
    }
  };

  return (
    <section>
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-ink">Bookmarks</h1>
        <p className="mt-1 text-sm text-muted">Articles you’ve saved to read later.</p>
      </div>

      <ArticleGrid
        state={state}
        onBookmarkChange={handleBookmarkChange}
        empty={
          <EmptyState
            icon={Bookmark}
            title="No bookmarks yet"
            message="Tap the bookmark icon on any article to save it here."
            action={
              <Link to="/">
                <Button size="sm">Browse the feed</Button>
              </Link>
            }
          />
        }
      />
    </section>
  );
}
