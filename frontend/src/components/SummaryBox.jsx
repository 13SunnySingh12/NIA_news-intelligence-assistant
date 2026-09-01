import { useEffect, useRef, useState } from 'react';
import { Sparkles, RotateCw } from 'lucide-react';
import { summarizeArticle } from '../api/assistant';
import { Button } from './ui/Button';
import { Spinner } from './ui/Spinner';

/** Give up rather than spin forever if the AI service stops responding. */
const SUMMARY_TIMEOUT_MS = 45000;

/**
 * On-demand AI summaries. Nothing is generated until the reader asks, and each
 * length is cached in component state so switching back is instant.
 *
 * Requests are tracked per length: switching from Short to Detailed mid-flight
 * must not let the first response clear the second one's loading state, and a
 * response that arrives after the reader moved on is discarded.
 */
export function SummaryBox({ articleId }) {
  const [summaries, setSummaries] = useState({});
  const [active, setActive] = useState(null);
  const [loading, setLoading] = useState(null);
  const [error, setError] = useState('');

  // Identifies the newest request so late/abandoned responses can be ignored.
  const requestId = useRef(0);
  const mounted = useRef(true);
  useEffect(() => {
    // Must be re-armed on every mount: StrictMode runs mount -> cleanup -> mount,
    // so setting this only in the cleanup would leave it false forever and every
    // response would be discarded as "unmounted".
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);

  // A new article means the old summaries no longer apply. This must NOT run on
  // mount: bumping requestId there would make the first response look superseded
  // and leave the reader on "Summarizing…" forever.
  const shownArticle = useRef(articleId);
  useEffect(() => {
    if (shownArticle.current === articleId) return;
    shownArticle.current = articleId;
    requestId.current += 1;
    setSummaries({});
    setActive(null);
    setLoading(null);
    setError('');
  }, [articleId]);

  const generate = async (length) => {
    setActive(length);
    setError('');
    if (summaries[length]) return; // already generated

    const id = ++requestId.current;
    setLoading(length);

    // Fail loudly instead of leaving the reader on "Summarizing…" forever.
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), SUMMARY_TIMEOUT_MS);

    try {
      const result = await summarizeArticle(articleId, length, { signal: controller.signal });
      if (!mounted.current || id !== requestId.current) return; // superseded
      setSummaries((current) => ({ ...current, [length]: result.text }));
      setLoading(null);
    } catch (err) {
      if (!mounted.current || id !== requestId.current) return; // superseded
      setError(
        err.name === 'AbortError'
          ? 'That took too long. Please try again.'
          : err.message || 'Could not generate a summary right now.',
      );
      setLoading(null);
    } finally {
      clearTimeout(timer);
    }
  };

  // Both start as null, and `null === null` is true — comparing them directly
  // rendered "Summarizing…" on an idle page with no request in flight. A spinner
  // requires an actually-selected length that is actually loading.
  const showSpinner = active !== null && loading === active;
  const showSummary = active !== null && !showSpinner && Boolean(summaries[active]);

  return (
    <div className="rounded-2xl border border-hair bg-surface p-5">
      <div className="mb-3 flex items-center gap-2">
        <Sparkles className="h-4 w-4 text-brand" aria-hidden="true" />
        <h2 className="text-sm font-semibold text-ink">AI summary</h2>
      </div>

      <div className="flex gap-2">
        <Button
          size="sm"
          variant={active === 'short' ? 'primary' : 'secondary'}
          onClick={() => generate('short')}
          loading={loading === 'short'}
        >
          Short
        </Button>
        <Button
          size="sm"
          variant={active === 'detailed' ? 'primary' : 'secondary'}
          onClick={() => generate('detailed')}
          loading={loading === 'detailed'}
        >
          Detailed
        </Button>
      </div>

      <div className="mt-4 min-h-[1.5rem] text-[15px] leading-relaxed">
        {error && (
          <div className="space-y-2" role="alert">
            <p className="text-sm text-red-600">{error}</p>
            <button
              type="button"
              onClick={() => generate(active || 'short')}
              className="inline-flex items-center gap-1.5 text-sm font-medium text-brand-strong hover:underline"
            >
              <RotateCw className="h-3.5 w-3.5" aria-hidden="true" /> Try again
            </button>
          </div>
        )}
        {!error && showSpinner && (
          <p className="flex items-center gap-2 text-sm text-muted">
            <Spinner className="h-4 w-4" /> Summarizing…
          </p>
        )}
        {!error && showSummary && (
          <p className="animate-fade-in text-ink/90">{summaries[active]}</p>
        )}
        {!error && !active && (
          <p className="text-sm text-muted">Generate a quick or detailed summary of this article.</p>
        )}
      </div>
    </div>
  );
}

export default SummaryBox;
