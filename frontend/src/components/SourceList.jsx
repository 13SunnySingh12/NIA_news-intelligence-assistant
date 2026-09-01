import { Link } from 'react-router-dom';

/**
 * Trusted supporting articles for an assistant answer. Each links to the
 * internal article page (so reading history is captured), never the raw URL.
 */
export function SourceList({ sources }) {
  if (!sources || sources.length === 0) return null;

  return (
    <div className="mt-3 border-t border-hair pt-3">
      <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">Sources</p>
      <ol className="space-y-1.5">
        {sources.map((source, index) => (
          <li key={source.id} className="flex gap-2 text-sm">
            <span className="text-muted">{index + 1}.</span>
            <Link to={`/article/${source.id}`} className="text-brand-strong hover:underline">
              {source.title}
              {source.source && <span className="text-muted"> — {source.source}</span>}
            </Link>
          </li>
        ))}
      </ol>
    </div>
  );
}

export default SourceList;
