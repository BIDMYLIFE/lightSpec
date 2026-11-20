import { useId, type ReactNode } from 'react'

interface AuthLayoutProps {
  eyebrow?: string
  title: string
  subtitle: string
  children: ReactNode
  footer?: ReactNode
}

export function AuthLayout({ eyebrow = 'LightSpec', title, subtitle, children, footer }: AuthLayoutProps) {
  const titleId = useId()
  return (
    <main className="relative grid min-h-dvh place-items-center overflow-hidden bg-slate-950 px-4 py-10 text-slate-100 sm:px-6">
      <div aria-hidden="true" className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,#164e63_0,transparent_42%),radial-gradient(circle_at_bottom_right,#312e81_0,transparent_38%)] opacity-70" />
      <section aria-labelledby={titleId} className="relative w-full max-w-md rounded-3xl border border-white/10 bg-slate-900/85 p-6 shadow-2xl shadow-cyan-950/40 backdrop-blur sm:p-9">
        <p className="text-xs font-bold uppercase tracking-[0.32em] text-cyan-300">{eyebrow}</p>
        <h1 id={titleId} className="mt-3 text-3xl font-bold tracking-tight text-white sm:text-4xl">{title}</h1>
        <p className="mt-3 text-sm leading-6 text-slate-300">{subtitle}</p>
        <div className="mt-8">{children}</div>
        {footer && <footer className="mt-7 border-t border-white/10 pt-5 text-center text-sm text-slate-400">{footer}</footer>}
      </section>
    </main>
  )
}
