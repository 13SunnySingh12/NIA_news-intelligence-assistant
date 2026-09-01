import { GradientBackground } from '../ui/pipo';
import { Brand } from '../ui/Brand';

/** Shared auth layout so Login and Signup share one visual language. */
export function AuthShell({ title, subtitle, children, footer }) {
  return (
    <div className="relative flex min-h-screen items-center justify-center px-4 py-10">
      <GradientBackground />

      <div className="w-full max-w-md animate-fade-in">
        <div className="mb-6 flex flex-col items-center text-center">
          <Brand size={40} textClassName="text-xl" className="mb-4" />
          <h1 className="text-2xl font-bold tracking-tight text-ink">{title}</h1>
          {subtitle && <p className="mt-1.5 text-sm text-muted">{subtitle}</p>}
        </div>

        <div className="rounded-2xl border border-hair bg-surface/90 p-6 shadow-pop backdrop-blur-sm sm:p-7">
          {children}
        </div>

        {footer && <div className="mt-6 text-center text-sm text-muted">{footer}</div>}
      </div>
    </div>
  );
}

export default AuthShell;
