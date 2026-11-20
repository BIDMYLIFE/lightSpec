import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { AdminSetupPage } from './setup/AdminSetupPage'
import { RegistrationPage } from './register/RegistrationPage'
import { LoginPage } from './login/LoginPage'
import { HomePage } from './home/HomePage'

function Placeholder({ title }: { title: string }) {
  return (
    <main className="grid min-h-screen place-items-center bg-slate-950 p-6 text-slate-100">
      <h1 className="text-3xl font-bold">{title}</h1>
    </main>
  )
}

function AuthRoutes() {
  const { state } = useAuth()
  const location = useLocation()

  if (state.status === 'checking') return <Placeholder title="正在確認系統狀態" />
  if (state.status === 'setup-required' && location.pathname !== '/setup/admin') {
    return <Navigate to="/setup/admin" replace />
  }
  if (state.status !== 'setup-required' && location.pathname === '/setup/admin') {
    return <Navigate to="/login" replace />
  }
  if (state.status === 'authenticated' && ['/login', '/register'].includes(location.pathname)) {
    return <Navigate to="/home" replace />
  }
  if (state.status === 'anonymous' && location.pathname === '/home') {
    return <Navigate to="/login" replace />
  }

  return (
    <Routes>
      <Route path="/setup/admin" element={<AdminSetupPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegistrationPage />} />
      <Route path="/home" element={<HomePage />} />
      <Route
        path="*"
        element={<Navigate to={state.status === 'authenticated' ? '/home' : '/login'} replace />}
      />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AuthRoutes />
    </AuthProvider>
  )
}
