import { Logo } from './Logo';

/**
 * Brand lockup: the NIA logo with the "NIA" name beside it. Used wherever the
 * brand is shown (navbar, auth screens, splash) so the logo and name always
 * appear together and stay consistent. The logo is decorative here (alt="")
 * because the adjacent text provides the accessible name.
 */
export function Brand({ size = 32, textClassName = 'text-lg', className = '' }) {
  return (
    <span className={`inline-flex items-center gap-2 ${className}`}>
      <Logo size={size} alt="" />
      <span className={`font-bold tracking-tight text-ink ${textClassName}`}>NIA</span>
    </span>
  );
}

export default Brand;
