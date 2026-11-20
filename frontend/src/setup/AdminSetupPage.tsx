import type { FormEvent } from 'react'
import { AuthLayout } from '../ui/AuthLayout'
import { FormField } from '../ui/FormField'
import { SubmitButton } from '../ui/SubmitButton'
import { useAdminSetupViewModel } from './useAdminSetupViewModel'

export function AdminSetupPage() {
  const vm = useAdminSetupViewModel()

  function submit(event: FormEvent) {
    event.preventDefault()
    void vm.submit()
  }

  return (
    <AuthLayout title="建立第一位管理員" subtitle="設定管理帳號後，初始化入口將永久關閉。">
      <aside role="alert" className="mb-6 rounded-2xl border border-amber-300/40 bg-amber-950/40 p-4 text-sm leading-6 text-amber-100">
        <strong className="block text-amber-200">公開服務初始化警示</strong>
        第一位完成此表單的人將成為系統管理員；建立完成後，本頁與初始化 API 將不可再次使用。
      </aside>
      <form className="space-y-5" onSubmit={submit} noValidate>
        {vm.apiError && <p role="alert" className="rounded-xl border border-rose-400/40 bg-rose-950/50 p-3 text-sm text-rose-200">{vm.apiError}</p>}
        <FormField
          label="使用者名稱"
          name="username"
          autoComplete="username"
          value={vm.fields.username}
          error={vm.errors.username}
          onChange={(event) => vm.update('username', event.target.value)}
        />
        <FormField
          label="密碼"
          name="password"
          type="password"
          autoComplete="new-password"
          value={vm.fields.password}
          error={vm.errors.password}
          onChange={(event) => vm.update('password', event.target.value)}
        />
        <FormField
          label="確認密碼"
          name="passwordConfirmation"
          type="password"
          autoComplete="new-password"
          value={vm.fields.passwordConfirmation}
          error={vm.errors.passwordConfirmation}
          onChange={(event) => vm.update('passwordConfirmation', event.target.value)}
        />
        <SubmitButton loading={vm.loading} loadingLabel="正在建立管理員…">建立管理員</SubmitButton>
      </form>
    </AuthLayout>
  )
}
