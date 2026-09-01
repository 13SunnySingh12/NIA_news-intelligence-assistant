import { Loader2 } from 'lucide-react';

export function Spinner({ className = '', label = 'Loading' }) {
  return (
    <span role="status" aria-label={label} className="inline-flex">
      <Loader2 className={`animate-spin ${className}`} aria-hidden="true" />
    </span>
  );
}

export default Spinner;
