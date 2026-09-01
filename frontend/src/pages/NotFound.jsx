import { Link } from 'react-router-dom';
import { Button } from '../components/ui/Button';

export default function NotFound() {
  return (
    <div className="grid min-h-screen place-items-center bg-page px-4 text-center">
      <div>
        <p className="text-sm font-semibold uppercase tracking-wide text-brand">404</p>
        <h1 className="mt-2 text-3xl font-bold tracking-tight text-ink">Page not found</h1>
        <p className="mt-2 text-sm text-muted">The page you’re looking for doesn’t exist.</p>
        <Link to="/" className="mt-6 inline-block">
          <Button>Back to NIA</Button>
        </Link>
      </div>
    </div>
  );
}
