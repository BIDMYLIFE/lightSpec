import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import App from '../App'
import { apiClient, setUnauthorizedHandler } from '../api/client'

vi.mock('../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../api/client')>()
  return {
    ...original,
    apiClient: { get: vi.fn(), post: vi.fn() },
    setUnauthorizedHandler: vi.fn(),
  }
})

const get = vi.mocked(apiClient.get)
const post = vi.mocked(apiClient.post)
const set401 = vi.mocked(setUnauthorizedHandler)

describe('HomePage', () => {
  beforeEach(() => {
    get.mockReset().mockResolvedValueOnce({ data: { setupRequired: false } })
      .mockResolvedValueOnce({ data: { id: 5, username: 'Display Name', role: 'USER' } })
    post.mockReset()
    set401.mockClear()
  })

  it('shows the current display name', async () => {
    renderHome()
    expect(await screen.findByRole('heading', { name: /歡迎回來，Display Name/ })).toBeInTheDocument()
  })

  it('logs out and navigates to login', async () => {
    const user = userEvent.setup()
    post.mockResolvedValueOnce({ data: undefined })
    renderHome()
    await user.click(await screen.findByRole('button', { name: '登出' }))
    expect(post).toHaveBeenCalledWith('/auth/logout')
    expect(await screen.findByRole('heading', { name: '登入 LightSpec' })).toBeInTheDocument()
  })

  it('returns to login when the global client reports an expired JWT', async () => {
    renderHome()
    await screen.findByText('Display Name')
    const handler = set401.mock.calls.find(([candidate]) => typeof candidate === 'function')?.[0]
    expect(handler).toBeTypeOf('function')

    act(() => handler?.())

    expect(await screen.findByRole('heading', { name: '登入 LightSpec' })).toBeInTheDocument()
  })
})

function renderHome() {
  render(<MemoryRouter initialEntries={['/home']}><App /></MemoryRouter>)
}
