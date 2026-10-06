import { api } from './client'
import type { LoginResponse, Usuario } from '../types/auth'

export async function login(email: string, password: string): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/api/auth/login', { email, password })
  return data
}

export async function obtenerUsuarioActual(): Promise<Usuario> {
  const { data } = await api.get<Usuario>('/api/auth/yo')
  return data
}

export async function solicitarRecuperacion(email: string): Promise<string> {
  const { data } = await api.post<{ mensaje: string }>('/api/auth/recuperar', { email })
  return data.mensaje
}

export async function restablecerPassword(token: string, password: string): Promise<void> {
  await api.post('/api/auth/restablecer', { token, password })
}
/** "Mi cuenta": el usuario logueado cambia su contraseña. Pide la actual. */
export async function cambiarPassword(actual: string, nueva: string): Promise<void> {
  await api.put('/api/auth/password', { actual, nueva })
}
