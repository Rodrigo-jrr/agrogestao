import axios from 'axios'

export const apiClient = axios.create({
  baseURL: '/api',
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('agro.token')
  const propertyId = localStorage.getItem('agro.propertyId')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (propertyId) {
    config.headers['X-Property-Id'] = propertyId
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('agro.token')
      localStorage.removeItem('agro.propertyId')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error) && error.response?.data && typeof error.response.data === 'object') {
    const data = error.response.data as { message?: string }
    if (data.message) return data.message
  }
  return 'Ocorreu um erro inesperado'
}
