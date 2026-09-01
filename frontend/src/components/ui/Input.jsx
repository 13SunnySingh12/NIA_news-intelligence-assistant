export function Input({ id, describedBy, invalid, className = '', ...props }) {
  return (
    <input
      id={id}
      aria-describedby={describedBy}
      aria-invalid={invalid || undefined}
      className={`h-11 w-full rounded-xl border bg-surface px-3.5 text-sm text-ink
        placeholder:text-muted/70 transition-colors duration-150
        focus:border-brand ${invalid ? 'border-red-400' : 'border-hair'} ${className}`}
      {...props}
    />
  );
}

export default Input;
