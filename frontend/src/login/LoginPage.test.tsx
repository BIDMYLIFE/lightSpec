import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import App from '../App'
import { ApiClientError, apiClient } from '../api/client'

vi.mock('../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../api/client')>()
  return { ...original, apiClient: { get: vi.fn(), post: vi.fn() }, setUnauthorizedHandler: vi.fn() }
})

const get = vi.mocked(apiClient.get)
const post = vi.mocked(apiClient.post)

describe('LoginPage', () => {
  beforeEach(() => {
    get.mockReset().mockResolvedValueOnce({ data: { setupRequired: false } })
      .mockRejectedValueOnce(new ApiClientError({ detail: '未登入' }, 401))
    post.mockReset()
  })

  it('shows a uniform invalid-credentials error', async () => {
    const user = userEvent.setup()
    post.mockRejectedValueOnce(new ApiClientError({ detail: '使用者名稱或密碼錯誤' }, 401))
    renderPage()
    await fill(user)
    await user.click(screen.getByRole('button', { name: '登入' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('使用者名稱或密碼錯誤')
  })

  it('disables the form while login is pending', async () => {
    const user = userEvent.setup()
    post.mockImplementationOnce(() => new Promise(() => undefined))
    renderPage()
    await fill(user)
    await user.click(screen.getByRole('button', { name: '登入' }))
    expect(screen.getByRole('button', { name: '正在登入…' })).toBeDisabled()
  })

  it('calls me after login and restores the authenticated state', async () => {
    const user = userEvent.setup()
    post.mockResolvedValueOnce({ data: {} })
    get.mockResolvedValueOnce({ data: { id: 9, username: 'Display Name', role: 'USER' } })
    renderPage()
    await fill(user)
    await user.click(screen.getByRole('button', { name: '登入' }))
    expect(await screen.findByRole('heading', { name: /歡迎回來，Display Name/ })).toBeInTheDocument()
    expect(get).toHaveBeenLastCalledWith('/auth/me')
  })

  it('restores an existing session on refresh without showing login', async () => {
    get.mockReset().mockResolvedValueOnce({ data: { setupRequired: false } })
      .mockResolvedValueOnce({ data: { id: 9, username: 'member', role: 'USER' } })
    renderPage()
    expect(await screen.findByRole('heading', { name: /歡迎回來，member/ })).toBeInTheDocument()
  })
})

function renderPage() {
  render(<MemoryRouter initialEntries={['/login']}><App /></MemoryRouter>)
}

async function fill(user: ReturnType<typeof userEvent.setup>) {
  await user.type(await screen.findByLabelText('使用者名稱'), 'member')
  await user.type(screen.getByLabelText('密碼'), 'safe-password-123')
}
