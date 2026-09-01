import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, ExternalLink } from 'lucide-react';
import { getArticle, markRead } from '../api/articles';
import { ArticleImage } from '../components/ArticleImage';
import { BookmarkButton } from '../components/BookmarkButton';
import { SummaryBox } from '../components/SummaryBox';
import { LoadingState, ErrorState } from '../components/ui/states';
import { labelForCategory } from '../lib/categories';
import { formatDate } from '../lib/format';

export default function Article() {
  const { id } = useParams();
  const [article, setArticle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    getArticle(id)
      .then((data) => {
        if (!active) return;
        setArticle(data);
        setLoading(false);
        // Record the read event (powers personalization + trending). Best-effort.
        markRead(id).catch(() => {});
      })
      .catch((err) => {
        if (!active) return;
        setError(err.message || 'Could not load this article.');
        setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [id]);

  if (loading) return <LoadingState message="Loading article…" />;
  if (error) return <ErrorState message={error} />;
  if (!article) return null;

  const paragraphs = (article.content || article.description || '')
    .split(/\n+/)
    .map((line) => line.trim())
    .filter(Boolean);

  return (
    <article className="mx-auto max-w-3xl animate-fade-in">
      <Link to="/" className="mb-5 inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink">
        <ArrowLeft className="h-4 w-4" aria-hidden="true" /> Back to feed
      </Link>

      <div className="mb-3 flex items-center gap-2 text-xs">
        <Link
          to={`/category/${article.category}`}
          className="rounded-full bg-brand-soft px-2.5 py-1 font-medium text-brand-strong"
        >
          {labelForCategory(article.category)}
        </Link>
        <span className="text-muted">{formatDate(article.publishedAt)}</span>
      </div>

      <h1 className="font-serif text-3xl font-semibold leading-tight text-ink sm:text-4xl">
        {article.title}
      </h1>

      <div className="mt-3 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted">
        <span className="font-medium text-ink">{article.source}</span>
        {article.author && <span>· {article.author}</span>}
      </div>

      {article.imageUrl && (
        <ArticleImage
          src={article.imageUrl}
          alt={article.title}
          className="mt-6 aspect-[16/9] w-full rounded-2xl"
        />
      )}

      <div className="mt-5 flex flex-wrap items-center gap-3">
        <BookmarkButton articleId={article.id} bookmarked={article.bookmarked} showLabel />
        {article.url && (
          <a
            href={article.url}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-sm text-muted
              transition-colors hover:text-ink"
          >
            <ExternalLink className="h-4 w-4" aria-hidden="true" /> Read original
          </a>
        )}
      </div>

      <div className="my-7">
        <SummaryBox articleId={article.id} />
      </div>

      {paragraphs.length > 0 ? (
        <div className="space-y-4 text-[17px] leading-relaxed text-ink/90">
          {paragraphs.map((paragraph, index) => (
            <p key={index}>{paragraph}</p>
          ))}
        </div>
      ) : (
        <p className="text-sm text-muted">
          The full text isn’t available here. Use “Read original” to view the complete article.
        </p>
      )}
    </article>
  );
}
