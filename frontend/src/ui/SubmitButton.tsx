import type { ButtonHTMLAttributes } from 'react'

interface SubmitButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean
  loadingLabel?: string
}

export function SubmitButton({ loading, loadingLabel = '處理中…', children, disabled, ...props }: SubmitButtonProps) {
  return (
    <button
      {...props}
      type={props.type ?? 'submit'}
      disabled={disabled || loading}
      aria-busy={loading}
      className="inline-flex min-h-12 w-full items-center justify-center rounded-xl bg-cyan-400 px-5 py-3 font-bold text-slate-950 transition hover:bg-cyan-300 focus:outline-none focus:ring-2 focus:ring-cyan-200 focus:ring-offset-2 focus:ring-offset-slate-900 disabled:cursor-not-allowed disabled:opacity-60"
    >
      {loading ? loadingLabel : children}
    </button>
  )
}
