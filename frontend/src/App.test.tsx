import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from './App'
import { ApiClientError, apiClient } from './api/client'

vi.mock('./api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('./api/client')>()
  return {
    ...original,
    apiClient: { get: vi.fn() },
    setUnauthorizedHandler: vi.fn(),
  }
})

const get = vi.mocked(apiClient.get)

describe('Auth route guard', () => {
  beforeEach(() => get.mockReset())

  it('redirects every normal route to setup when no admin exists', async () => {
    get.mockResolvedValueOnce({ data: { setupRequired: true } })
    render(
      <MemoryRouter initialEntries={['/register']}>
        <App />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('heading', { name: '建立第一位管理員' })).toBeInTheDocument()
  })

  it('redirects the setup page to login after an admin exists', async () => {
    get.mockResolvedValueOnce({ data: { setupRequired: false } })
    get.mockRejectedValueOnce(new ApiClientError({ detail: '未登入' }, 401))
    render(
      <MemoryRouter initialEntries={['/setup/admin']}>
        <App />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('heading', { name: '登入 LightSpec' })).toBeInTheDocument()
  })

  it('restores an authenticated user while checking startup state', async () => {
    get.mockResolvedValueOnce({ data: { setupRequired: false } })
    get.mockResolvedValueOnce({ data: { id: 7, username: 'member', role: 'USER' } })
    render(
      <MemoryRouter initialEntries={['/home']}>
        <App />
      </MemoryRouter>,
    )
    expect(await screen.findByRole('heading', { name: /歡迎回來，member/ })).toBeInTheDocument()
  })
})
