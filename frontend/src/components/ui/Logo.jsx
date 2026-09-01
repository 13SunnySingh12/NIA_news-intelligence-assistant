/**
 * The official NIA logo. Every usage references the single /logo.svg asset, so it
 * is downloaded and cached once and stays identical across the app.
 *
 * The source SVG is square (viewBox 2048x2048) and declares
 * preserveAspectRatio="none", so it is always rendered in a square box (equal
 * width and height) to preserve its proportions. Explicit width/height reserve
 * space and prevent layout shift.
 *
 * Pass alt="" where a visible "NIA" wordmark already labels the logo (decorative).
 */
export function Logo({ size = 32, alt = 'NIA', className = '' }) {
  return (
    <img
      src="/logo.svg"
      alt={alt}
      width={size}
      height={size}
      style={{ width: size, height: size }}
      className={className}
      draggable="false"
    />
  );
}

export default Logo;
