import { Spinner } from './Spinner';

const VARIANTS = {
  primary:
    'bg-brand text-white hover:bg-brand-strong shadow-sm disabled:hover:bg-brand',
  secondary:
    'bg-surface text-ink border border-hair hover:bg-surface-2',
  ghost: 'text-muted hover:text-ink hover:bg-surface-2',
  outline: 'border border-hair text-ink hover:bg-surface-2 bg-transparent',
  danger: 'bg-red-600 text-white hover:bg-red-700',
};

const SIZES = {
  sm: 'h-9 px-3 text-sm',
  md: 'h-11 px-4 text-sm',
  lg: 'h-12 px-5 text-base',
};

/**
 * Consistent button with variants, sizes, and a built-in loading state.
 * Renders a real <button> for full keyboard and screen-reader support.
 */
export function Button({
  children,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  className = '',
  type = 'button',
  ...props
}) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-xl font-medium
        transition-colors duration-150 disabled:cursor-not-allowed disabled:opacity-60
        ${VARIANTS[variant]} ${SIZES[size]} ${className}`}
      {...props}
    >
      {loading && <Spinner className="h-4 w-4" />}
      {children}
    </button>
  );
}

export default Button;
