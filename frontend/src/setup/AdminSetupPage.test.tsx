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

describe('AdminSetupPage', () => {
  beforeEach(() => {
    get.mockReset().mockResolvedValueOnce({ data: { setupRequired: true } })
    post.mockReset()
    vi.mocked(showSuccess).mockClear()
  })

  it('shows client-side field validation without calling the API', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter initialEntries={['/setup/admin']}><App /></MemoryRouter>)

    await user.click(await screen.findByRole('button', { name: '建立管理員' }))

    expect(screen.getByText('請輸入使用者名稱')).toBeInTheDocument()
    expect(screen.getByText('密碼至少需要 8 個字元')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })

  it('shows the first-visitor takeover warning before submission', async () => {
    render(<MemoryRouter initialEntries={['/setup/admin']}><App /></MemoryRouter>)

    const warning = await screen.findByRole('alert')
    expect(warning).toHaveTextContent('第一位完成此表單的人將成為系統管理員')
    expect(warning).toHaveTextContent('初始化 API 將不可再次使用')
    expect(screen.getByRole('button', { name: '建立管理員' })).toBeInTheDocument()
  })

  it('maps API problem details back to the form', async () => {
    const user = userEvent.setup()
    post.mockRejectedValueOnce(new ApiClientError({ detail: '名稱已被使用', errors: { username: '請使用其他名稱' } }, 409))
    render(<MemoryRouter initialEntries={['/setup/admin']}><App /></MemoryRouter>)

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: '建立管理員' }))

    expect(await screen.findByText('名稱已被使用')).toBeInTheDocument()
    expect(screen.getByText('請使用其他名稱')).toBeInTheDocument()
  })

  it('shows loading then refreshes setup state and navigates to login', async () => {
    const user = userEvent.setup()
    let finish!: () => void
    post.mockImplementationOnce(() => new Promise((resolve) => { finish = () => resolve({ data: {} }) }))
    get.mockResolvedValueOnce({ data: { setupRequired: false } })
    get.mockRejectedValueOnce(new ApiClientError({ detail: '未登入' }, 401))
    render(<MemoryRouter initialEntries={['/setup/admin']}><App /></MemoryRouter>)

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: '建立管理員' }))
    expect(screen.getByRole('button', { name: '正在建立管理員…' })).toBeDisabled()
    finish()

    expect(await screen.findByRole('heading', { name: '登入 LightSpec' })).toBeInTheDocument()
    expect(showSuccess).toHaveBeenCalledOnce()
  })
})

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(await screen.findByLabelText('使用者名稱'), 'administrator')
  await user.type(screen.getByLabelText('密碼'), 'safe-password-123')
  await user.type(screen.getByLabelText('確認密碼'), 'safe-password-123')
}
