import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiClientError, apiClient } from '../api/client'
import { showSuccess } from '../ui/feedback'

interface RegistrationFields {
  username: string
  password: string
  passwordConfirmation: string
}

export function useRegistrationViewModel() {
  const navigate = useNavigate()
  const [fields, setFields] = useState<RegistrationFields>({ username: '', password: '', passwordConfirmation: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(false)

  function update(field: keyof RegistrationFields, value: string) {
    setFields((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: '' }))
  }

  function validate() {
    const next: Record<string, string> = {}
    if (!fields.username.trim()) next.username = '請輸入使用者名稱'
    if (fields.password.length < 8) next.password = '密碼至少需要 8 個字元'
    if (fields.passwordConfirmation !== fields.password) next.passwordConfirmation = '兩次輸入的密碼不一致'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function submit() {
    setApiError('')
    if (!validate()) return
    setLoading(true)
    try {
      await apiClient.post('/auth/register', fields)
      await showSuccess('帳號建立完成', '請使用新帳號登入。')
      navigate('/login', { replace: true })
    } catch (error) {
      if (error instanceof ApiClientError) {
        setErrors(error.problem.errors ?? {})
        setApiError(error.message)
      } else {
        setApiError('無法建立帳號，請稍後再試')
      }
    } finally {
      setLoading(false)
    }
  }

  return { fields, errors, apiError, loading, update, submit }
}
