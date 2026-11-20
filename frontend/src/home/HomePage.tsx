import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function HomePage() {
  const navigate = useNavigate()
  const { state, setAnonymous } = useAuth()
  const [loggingOut, setLoggingOut] = useState(false)

  if (state.status !== 'authenticated') return null

  async function logout() {
    setLoggingOut(true)
    try {
      await apiClient.post('/auth/logout')
    } finally {
      setAnonymous()
      navigate('/login', { replace: true })
    }
  }

  return (
    <main className="min-h-dvh bg-slate-950 text-slate-100">
      <header className="border-b border-white/10 bg-slate-900/70 px-4 py-4 backdrop-blur sm:px-8">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4">
          <p className="font-bold tracking-[0.22em] text-cyan-300">LIGHTSPEC</p>
          <button
            type="button"
            disabled={loggingOut}
            onClick={() => void logout()}
            className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold hover:border-cyan-400 hover:text-cyan-200 focus:outline-none focus:ring-2 focus:ring-cyan-300 disabled:opacity-60"
          >
            {loggingOut ? '正在登出…' : '登出'}
          </button>
        </div>
      </header>
      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-8">
        <p className="text-sm font-semibold uppercase tracking-[0.24em] text-cyan-300">安全工作區</p>
        <h1 className="mt-4 text-4xl font-bold tracking-tight sm:text-5xl">
          歡迎回來，<span className="text-cyan-300">{state.user.username}</span>
        </h1>
        <p className="mt-5 max-w-2xl text-lg leading-8 text-slate-400">您已透過安全的 Cookie 工作階段登入 LightSpec。</p>
      </section>
    </main>
  )
}
