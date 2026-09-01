import { useState } from 'react';
import { Newspaper } from 'lucide-react';

/**
 * Article image with graceful fallbacks: a broken or missing image collapses to
 * a neutral placeholder instead of breaking the layout.
 */
export function ArticleImage({ src, alt = '', className = '' }) {
  const [failed, setFailed] = useState(false);
  const showImage = Boolean(src) && !failed;

  return (
    <div className={`relative overflow-hidden bg-surface-2 ${className}`}>
      {showImage ? (
        <img
          src={src}
          alt={alt}
          loading="lazy"
          onError={() => setFailed(true)}
          className="h-full w-full object-cover transition-transform duration-500 ease-out"
        />
      ) : (
        <div className="flex h-full w-full items-center justify-center bg-gradient-to-br
          from-surface-2 to-brand-soft">
          <Newspaper className="h-8 w-8 text-muted/40" aria-hidden="true" />
        </div>
      )}
    </div>
  );
}

export default ArticleImage;
