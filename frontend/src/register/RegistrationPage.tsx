import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AuthLayout } from '../ui/AuthLayout'
import { FormField } from '../ui/FormField'
import { SubmitButton } from '../ui/SubmitButton'
import { useRegistrationViewModel } from './useRegistrationViewModel'

export function RegistrationPage() {
  const vm = useRegistrationViewModel()

  function submit(event: FormEvent) {
    event.preventDefault()
    void vm.submit()
  }

  return (
    <AuthLayout
      title="建立帳號"
      subtitle="註冊一般使用者帳號，完成後即可安全登入。"
      footer={<>已經有帳號？ <Link className="font-semibold text-cyan-300 hover:text-cyan-200" to="/login">前往登入</Link></>}
    >
      <form className="space-y-5" onSubmit={submit} noValidate>
        {vm.apiError && <p role="alert" className="rounded-xl border border-rose-400/40 bg-rose-950/50 p-3 text-sm text-rose-200">{vm.apiError}</p>}
        <FormField label="使用者名稱" autoComplete="username" value={vm.fields.username} error={vm.errors.username} onChange={(event) => vm.update('username', event.target.value)} />
        <FormField label="密碼" type="password" autoComplete="new-password" value={vm.fields.password} error={vm.errors.password} onChange={(event) => vm.update('password', event.target.value)} />
        <FormField label="確認密碼" type="password" autoComplete="new-password" value={vm.fields.passwordConfirmation} error={vm.errors.passwordConfirmation} onChange={(event) => vm.update('passwordConfirmation', event.target.value)} />
        <SubmitButton loading={vm.loading} loadingLabel="正在建立帳號…">註冊</SubmitButton>
      </form>
    </AuthLayout>
  )
}
