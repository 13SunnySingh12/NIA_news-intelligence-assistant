import { Link } from 'react-router-dom';
import { ArticleImage } from './ArticleImage';
import { BookmarkButton } from './BookmarkButton';
import { labelForCategory } from '../lib/categories';
import { timeAgo } from '../lib/format';

/**
 * Scannable article card: image, category, headline, source + time, and a
 * bookmark action. Kept intentionally light so the feed reads quickly.
 */
export function ArticleCard({ article, onBookmarkChange }) {
  return (
    <article className="group animate-fade-in overflow-hidden rounded-2xl border border-hair
      bg-surface shadow-card transition-shadow duration-200 hover:shadow-card-hover">
      {/*
        The headline link below points at the same article and carries the
        accessible name, so this image link is decorative: exposing it too made
        every card announce its headline twice and cost keyboard users an extra
        tab stop per card. Worse, a card with no image had no name here at all —
        the placeholder is an aria-hidden icon — so it announced as just "link".
        Hidden from assistive tech and taken out of the tab order; still clickable.
      */}
      <Link
        to={`/article/${article.id}`}
        className="block"
        aria-hidden="true"
        tabIndex={-1}
      >
        <ArticleImage
          src={article.imageUrl}
          alt={article.title}
          className="aspect-[16/9] w-full"
        />
      </Link>

      <div className="flex flex-col gap-2.5 p-4">
        <div className="flex items-center gap-2 text-xs">
          <Link
            to={`/category/${article.category}`}
            className="rounded-full bg-brand-soft px-2.5 py-1 font-medium text-brand-strong
              transition-colors hover:bg-brand hover:text-white"
          >
            {labelForCategory(article.category)}
          </Link>
          <span className="text-muted">{timeAgo(article.publishedAt)}</span>
        </div>

        <Link to={`/article/${article.id}`} className="block">
          <h2 className="line-clamp-3 text-[15px] font-semibold leading-snug text-ink
            transition-colors group-hover:text-brand-strong">
            {article.title}
          </h2>
        </Link>

        {article.description && (
          <p className="line-clamp-2 text-sm text-muted">{article.description}</p>
        )}

        <div className="mt-1 flex items-center justify-between border-t border-hair pt-2.5">
          <span className="truncate pr-2 text-xs font-medium text-muted">{article.source}</span>
          <BookmarkButton
            articleId={article.id}
            bookmarked={article.bookmarked}
            onChange={(value) => onBookmarkChange?.(article.id, value)}
          />
        </div>
      </div>
    </article>
  );
}

export default ArticleCard;
