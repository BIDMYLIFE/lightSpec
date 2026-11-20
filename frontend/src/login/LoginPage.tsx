import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AuthLayout } from '../ui/AuthLayout'
import { FormField } from '../ui/FormField'
import { SubmitButton } from '../ui/SubmitButton'
import { useLoginViewModel } from './useLoginViewModel'

export function LoginPage() {
  const vm = useLoginViewModel()

  function submit(event: FormEvent) {
    event.preventDefault()
    void vm.submit()
  }

  return (
    <AuthLayout
      title="登入 LightSpec"
      subtitle="使用您的帳號進入安全工作區。"
      footer={<>還沒有帳號？ <Link className="font-semibold text-cyan-300 hover:text-cyan-200" to="/register">建立帳號</Link></>}
    >
      <form className="space-y-5" onSubmit={submit} noValidate>
        {vm.apiError && <p role="alert" className="rounded-xl border border-rose-400/40 bg-rose-950/50 p-3 text-sm text-rose-200">{vm.apiError}</p>}
        <FormField label="使用者名稱" autoComplete="username" value={vm.username} error={vm.errors.username} onChange={(event) => vm.setUsername(event.target.value)} />
        <FormField label="密碼" type="password" autoComplete="current-password" value={vm.password} error={vm.errors.password} onChange={(event) => vm.setPassword(event.target.value)} />
        <SubmitButton loading={vm.loading} loadingLabel="正在登入…">登入</SubmitButton>
      </form>
    </AuthLayout>
  )
}
