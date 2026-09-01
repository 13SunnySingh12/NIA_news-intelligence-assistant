/**
 * Pipo vertical gradient background for the auth screens.
 * Layered radial gradients over a vertical base, softened with heavy blur and a
 * subtle grain overlay via blend modes. Decorative only: it sits behind content
 * (-z-10), ignores pointer events, and is hidden from assistive tech.
 */

const GRAIN =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E";

export function GradientBackground({ className = '' }) {
  return (
    <div
      aria-hidden="true"
      className={`pointer-events-none absolute inset-0 -z-10 overflow-hidden ${className}`}
    >
      {/* Vertical base gradient */}
      <div
        className="absolute inset-0"
        style={{ background: 'linear-gradient(180deg, #eef2ff 0%, #f6f7f9 46%, #ffffff 100%)' }}
      />

      {/* Radial color pools */}
      <div
        className="absolute left-1/2 top-[-22%] h-[72vmax] w-[72vmax] -translate-x-1/2 rounded-full blur-3xl"
        style={{
          background: 'radial-gradient(closest-side, rgba(99,102,241,0.34), rgba(99,102,241,0) 70%)',
          mixBlendMode: 'multiply',
        }}
      />
      <div
        className="absolute left-[-12%] top-[8%] h-[46vmax] w-[46vmax] rounded-full blur-3xl"
        style={{
          background: 'radial-gradient(closest-side, rgba(56,189,248,0.26), rgba(56,189,248,0) 70%)',
          mixBlendMode: 'multiply',
        }}
      />
      <div
        className="absolute bottom-[-18%] right-[-8%] h-[52vmax] w-[52vmax] rounded-full blur-3xl"
        style={{
          background: 'radial-gradient(closest-side, rgba(244,114,182,0.20), rgba(244,114,182,0) 70%)',
          mixBlendMode: 'multiply',
        }}
      />

      {/* Grain */}
      <div
        className="absolute inset-0 opacity-40"
        style={{ backgroundImage: `url("${GRAIN}")`, mixBlendMode: 'soft-light' }}
      />
    </div>
  );
}

export default GradientBackground;
