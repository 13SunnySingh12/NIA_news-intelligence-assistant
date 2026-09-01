import { AlertTriangle } from 'lucide-react';
import { Button } from './Button';
import { Spinner } from './Spinner';

/** Centered loading indicator with an explicit message. */
export function LoadingState({ message = 'Loading…' }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-20 text-muted">
      <Spinner className="h-6 w-6" />
      <p className="text-sm">{message}</p>
    </div>
  );
}

/** Friendly empty state with an optional call to action. */
export function EmptyState({ icon: Icon, title, message, action }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed
      border-hair bg-surface/60 px-6 py-16 text-center">
      {Icon && (
        <div className="mb-4 rounded-2xl bg-surface-2 p-3 text-muted">
          <Icon className="h-6 w-6" aria-hidden="true" />
        </div>
      )}
      {/* Sits directly under the page h1, so h2 is the next level, not h3. */}
      <h2 className="text-base font-semibold text-ink">{title}</h2>
      {message && <p className="mt-1 max-w-sm text-sm text-muted">{message}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

/** Error state with an optional retry action. */
export function ErrorState({ message = 'Something went wrong.', onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-hair
      bg-surface px-6 py-16 text-center">
      <div className="mb-4 rounded-2xl bg-red-50 p-3 text-red-500">
        <AlertTriangle className="h-6 w-6" aria-hidden="true" />
      </div>
      <p className="max-w-sm text-sm text-muted">{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry} className="mt-5">
          Try again
        </Button>
      )}
    </div>
  );
}
