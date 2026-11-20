import { useId, type InputHTMLAttributes } from 'react'

interface FormFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string
  error?: string
}

export function FormField({ label, error, className = '', ...inputProps }: FormFieldProps) {
  const id = useId()
  const errorId = `${id}-error`
  return (
    <div>
      <label htmlFor={id} className="mb-2 block text-sm font-semibold text-slate-200">
        {label}
      </label>
      <input
        {...inputProps}
        id={id}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : undefined}
        className={`w-full rounded-xl border bg-slate-950/70 px-4 py-3 text-base text-white outline-none transition placeholder:text-slate-600 focus:ring-2 focus:ring-cyan-400 ${
          error ? 'border-rose-400' : 'border-slate-700 focus:border-cyan-400'
        } ${className}`}
      />
      {error && (
        <p id={errorId} role="alert" className="mt-2 text-sm text-rose-300">
          {error}
        </p>
      )}
    </div>
  )
}
