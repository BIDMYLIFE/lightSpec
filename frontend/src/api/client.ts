import axios, { AxiosError, AxiosHeaders, type AxiosAdapter, type AxiosInstance } from 'axios'

export interface ApiProblem {
  type?: string
  title?: string
  status?: number
  detail?: string
  errors?: Record<string, string>
}

export class ApiClientError extends Error {
  readonly problem: ApiProblem
  readonly status?: number

  constructor(problem: ApiProblem, status?: number) {
    super(problem.detail ?? problem.title ?? '請求失敗，請稍後再試')
    this.name = 'ApiClientError'
    this.problem = problem
    this.status = status
  }
}

type UnauthorizedHandler = () => void

let unauthorizedHandler: UnauthorizedHandler = () => undefined

export function setUnauthorizedHandler(handler: UnauthorizedHandler) {
  unauthorizedHandler = handler
}

interface ClientOptions {
  adapter?: AxiosAdapter
  onUnauthorized?: UnauthorizedHandler
}

export function createApiClient(options: ClientOptions = {}): AxiosInstance {
  const config = {
    baseURL: '/api',
    withCredentials: true,
    // Preserve the masked token from /security/csrf when Axios sends the request.
    withXSRFToken: false,
    adapter: options.adapter,
  }
  const client = axios.create(config)
  const csrfClient = axios.create(config)
  let csrfToken: Promise<{ headerName: string; token: string }> | undefined

  async function getCsrfToken() {
    csrfToken ??= csrfClient
      .get<{ headerName: string; token: string }>('/security/csrf')
      .then((response) => response.data)
      .catch((error) => {
        csrfToken = undefined
        throw error
      })
    return csrfToken
  }

  client.interceptors.request.use(async (request) => {
    const method = request.method?.toUpperCase() ?? 'GET'
    if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
      const csrf = await getCsrfToken()
      request.headers = AxiosHeaders.from(request.headers)
      request.headers.set(csrf.headerName, csrf.token)
    }
    return request
  })

  client.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
      if (!(error instanceof AxiosError)) {
        return Promise.reject(error)
      }
      if (error.response?.status === 401) {
        ;(options.onUnauthorized ?? unauthorizedHandler)()
      }
      if (error.response?.data && typeof error.response.data === 'object') {
        return Promise.reject(new ApiClientError(error.response.data as ApiProblem, error.response.status))
      }
      return Promise.reject(error)
    },
  )

  return client
}

export const apiClient = createApiClient()
