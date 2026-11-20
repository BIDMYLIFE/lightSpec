import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiClientError, apiClient } from '../api/client'
import { type CurrentUser, useAuth } from '../auth/AuthContext'

export function useLoginViewModel() {
  const navigate = useNavigate()
  const { setAuthenticated } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(false)

  function validate() {
    const next: Record<string, string> = {}
    if (!username.trim()) next.username = '請輸入使用者名稱'
    if (!password) next.password = '請輸入密碼'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function submit() {
    setApiError('')
    if (!validate()) return
    setLoading(true)
    try {
      await apiClient.post('/auth/login', { username, password })
      const me = await apiClient.get<CurrentUser>('/auth/me')
      setAuthenticated(me.data)
      navigate('/home', { replace: true })
    } catch (error) {
      setApiError(error instanceof ApiClientError ? error.message : '無法登入，請稍後再試')
    } finally {
      setLoading(false)
    }
  }

  return {
    username,
    password,
    errors,
    apiError,
    loading,
    setUsername: (value: string) => { setUsername(value); setErrors((current) => ({ ...current, username: '' })) },
    setPassword: (value: string) => { setPassword(value); setErrors((current) => ({ ...current, password: '' })) },
    submit,
  }
}
