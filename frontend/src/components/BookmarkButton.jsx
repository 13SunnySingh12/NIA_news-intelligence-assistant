import { useEffect, useState } from 'react';
import { Bookmark } from 'lucide-react';
import { addBookmark, removeBookmark } from '../api/bookmarks';

/**
 * Toggles a bookmark with an optimistic update, reverting on failure.
 * Guards against duplicate clicks while a request is in flight.
 */
export function BookmarkButton({ articleId, bookmarked: initial = false, onChange, showLabel = false }) {
  const [bookmarked, setBookmarked] = useState(initial);
  const [pending, setPending] = useState(false);

  useEffect(() => setBookmarked(initial), [initial]);

  const toggle = async (event) => {
    event.preventDefault();
    event.stopPropagation();
    if (pending) return;

    const next = !bookmarked;
    setPending(true);
    setBookmarked(next);
    try {
      if (next) {
        await addBookmark(articleId);
      } else {
        await removeBookmark(articleId);
      }
      onChange?.(next);
    } catch {
      setBookmarked(!next); // revert on failure
    } finally {
      setPending(false);
    }
  };

  return (
    <button
      type="button"
      onClick={toggle}
      disabled={pending}
      aria-pressed={bookmarked}
      aria-label={bookmarked ? 'Remove bookmark' : 'Save article'}
      className={`inline-flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-sm transition-colors
        ${bookmarked ? 'text-brand' : 'text-muted hover:text-ink'} disabled:opacity-60`}
    >
      <Bookmark className={`h-[18px] w-[18px] ${bookmarked ? 'fill-current' : ''}`} aria-hidden="true" />
      {showLabel && <span>{bookmarked ? 'Saved' : 'Save'}</span>}
    </button>
  );
}

export default BookmarkButton;
