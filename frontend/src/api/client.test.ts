import { AxiosError, AxiosHeaders, type AxiosAdapter, type AxiosResponse } from 'axios'
import { createApiClient } from './client'

function response(config: Parameters<AxiosAdapter>[0], status: number, data: unknown): AxiosResponse {
  return { config, status, data, statusText: String(status), headers: new AxiosHeaders() }
}

describe('apiClient', () => {
  it('uses credentials and adds the server CSRF header to mutations', async () => {
    const requests: Parameters<AxiosAdapter>[0][] = []
    const adapter: AxiosAdapter = async (config) => {
      requests.push(config)
      if (config.url === '/security/csrf') {
        return response(config, 200, { headerName: 'X-XSRF-TOKEN', token: 'csrf-token' })
      }
      return response(config, 201, { ok: true })
    }
    const client = createApiClient({ adapter })

    await client.post('/auth/register', { username: 'member' })

    expect(requests).toHaveLength(2)
    expect(requests[0].withCredentials).toBe(true)
    expect(requests[1].headers.get('X-XSRF-TOKEN')).toBe('csrf-token')
    expect(requests[1].withCredentials).toBe(true)
  })

  it('normalizes problem details and invokes the 401 handler', async () => {
    const onUnauthorized = vi.fn()
    const adapter: AxiosAdapter = async (config) => {
      throw new AxiosError(
        'Unauthorized',
        'ERR_BAD_REQUEST',
        config,
        undefined,
        response(config, 401, {
          type: 'urn:lightspec:problem:invalid-login',
          title: '登入失敗',
          detail: '使用者名稱或密碼錯誤',
        }),
      )
    }
    const client = createApiClient({ adapter, onUnauthorized })

    await expect(client.get('/auth/me')).rejects.toMatchObject({
      name: 'ApiClientError',
      status: 401,
      message: '使用者名稱或密碼錯誤',
    })
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('does not request CSRF for safe reads', async () => {
    const adapter = vi.fn<AxiosAdapter>(async (config) => response(config, 200, { setupRequired: true }))
    const client = createApiClient({ adapter })

    await client.get('/setup/status')

    expect(adapter).toHaveBeenCalledOnce()
  })
})
