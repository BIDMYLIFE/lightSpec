import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import App from '../App'
import { ApiClientError, apiClient } from '../api/client'
import { showSuccess } from '../ui/feedback'

vi.mock('../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../api/client')>()
  return { ...original, apiClient: { get: vi.fn(), post: vi.fn() }, setUnauthorizedHandler: vi.fn() }
})
vi.mock('../ui/feedback', () => ({ showSuccess: vi.fn(() => Promise.resolve()) }))

const get = vi.mocked(apiClient.get)
const post = vi.mocked(apiClient.post)

describe('RegistrationPage', () => {
  beforeEach(() => {
    get.mockReset().mockResolvedValueOnce({ data: { setupRequired: false } })
      .mockRejectedValueOnce(new ApiClientError({ detail: '未登入' }, 401))
    post.mockReset()
    vi.mocked(showSuccess).mockClear()
  })

  it('is available after admin setup and validates input', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(await screen.findByRole('button', { name: '註冊' }))
    expect(screen.getByText('請輸入使用者名稱')).toBeInTheDocument()
    expect(screen.getByText('密碼至少需要 8 個字元')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })

  it('shows a duplicate-name problem beside the username', async () => {
    const user = userEvent.setup()
    post.mockRejectedValueOnce(new ApiClientError({ detail: '使用者名稱已被使用', errors: { username: '請使用其他名稱' } }, 409))
    renderPage()
    await fill(user)
    await user.click(screen.getByRole('button', { name: '註冊' }))
    expect(await screen.findByText('使用者名稱已被使用')).toBeInTheDocument()
    expect(screen.getByText('請使用其他名稱')).toBeInTheDocument()
  })

  it('navigates to login after successful registration', async () => {
    const user = userEvent.setup()
    post.mockResolvedValueOnce({ data: { username: 'member', role: 'USER' } })
    renderPage()
    await fill(user)
    await user.click(screen.getByRole('button', { name: '註冊' }))
    expect(await screen.findByRole('heading', { name: '登入 LightSpec' })).toBeInTheDocument()
    expect(showSuccess).toHaveBeenCalledOnce()
  })
})

function renderPage() {
  render(<MemoryRouter initialEntries={['/register']}><App /></MemoryRouter>)
}

async function fill(user: ReturnType<typeof userEvent.setup>) {
  await user.type(await screen.findByLabelText('使用者名稱'), 'member')
  await user.type(screen.getByLabelText('密碼'), 'safe-password-123')
  await user.type(screen.getByLabelText('確認密碼'), 'safe-password-123')
}
