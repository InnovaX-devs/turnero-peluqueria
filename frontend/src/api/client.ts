import axios from 'axios'

export const TOKEN_KEY = 'barberia.token'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
})

// Agrega el token a cada request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

// Si el backend responde 401 (token vencido o inválido), se cierra la sesión.
// El login se excluye porque ahí un 401 significa "credenciales incorrectas".
let alNoAutorizado: (() => void) | null = null

export function registrarAlNoAutorizado(fn: (() => void) | null) {
  alNoAutorizado = fn
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const esLogin = error.config?.url?.includes('/api/auth/login')
    if (axios.isAxiosError(error) && error.response?.status === 401 && !esLogin) {
      alNoAutorizado?.()
    }
    return Promise.reject(error)
  },
)
