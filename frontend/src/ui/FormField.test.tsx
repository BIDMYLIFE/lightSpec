import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthLayout } from './AuthLayout'
import { FormField } from './FormField'
import { SubmitButton } from './SubmitButton'

describe('shared authentication UI', () => {
  it('keeps a responsive full-width card without a fixed mobile width', () => {
    window.innerWidth = 360
    render(<AuthLayout title="登入" subtitle="安全登入"><p>表單</p></AuthLayout>)

    const section = screen.getByRole('region')
    expect(section).toHaveClass('w-full', 'max-w-md')
    expect(document.querySelector('main')).toHaveClass('px-4', 'sm:px-6')
  })

  it('associates labels and errors and supports keyboard focus order', async () => {
    const user = userEvent.setup()
    render(
      <form>
        <FormField label="使用者名稱" name="username" error="使用者名稱不可空白" />
        <SubmitButton>繼續</SubmitButton>
      </form>,
    )

    const input = screen.getByLabelText('使用者名稱')
    expect(input).toHaveAccessibleDescription('使用者名稱不可空白')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('使用者名稱不可空白')

    await user.tab()
    expect(input).toHaveFocus()
    await user.tab()
    expect(screen.getByRole('button', { name: '繼續' })).toHaveFocus()
  })
})
