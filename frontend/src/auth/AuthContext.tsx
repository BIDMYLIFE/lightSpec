import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiClientError, apiClient, setUnauthorizedHandler } from '../api/client'

export interface CurrentUser {
  id: number
  username: string
  role: 'ADMIN' | 'USER'
}

export type AuthState =
  | { status: 'checking' }
  | { status: 'setup-required' }
  | { status: 'anonymous' }
  | { status: 'authenticated'; user: CurrentUser }

interface AuthViewModel {
  state: AuthState
  refresh(): Promise<void>
  markSetupComplete(): Promise<void>
  setAuthenticated(user: CurrentUser): void
  setAnonymous(): void
}

const AuthContext = createContext<AuthViewModel | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: 'checking' })

  const refresh = useCallback(async () => {
    setState({ status: 'checking' })
    const setup = await apiClient.get<{ setupRequired: boolean }>('/setup/status')
    if (setup.data.setupRequired) {
      setState({ status: 'setup-required' })
      return
    }
    try {
      const me = await apiClient.get<CurrentUser>('/auth/me')
      setState({ status: 'authenticated', user: me.data })
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 401) {
        setState({ status: 'anonymous' })
        return
      }
      throw error
    }
  }, [])

  useEffect(() => {
    let active = true
    setUnauthorizedHandler(() => active && setState({ status: 'anonymous' }))
    refresh().catch(() => active && setState({ status: 'anonymous' }))
    return () => {
      active = false
      setUnauthorizedHandler(() => undefined)
    }
  }, [refresh])

  const value = useMemo<AuthViewModel>(
    () => ({
      state,
      refresh,
      markSetupComplete: refresh,
      setAuthenticated: (user) => setState({ status: 'authenticated', user }),
      setAnonymous: () => setState({ status: 'anonymous' }),
    }),
    [refresh, state],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth 必須在 AuthProvider 內使用')
  return context
}
