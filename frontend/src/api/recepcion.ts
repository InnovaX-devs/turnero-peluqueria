import { api } from './client'
import type { UsuarioRecepcion } from '../types/recepcion'

export async function listarUsuariosRecepcion(): Promise<UsuarioRecepcion[]> {
  const { data } = await api.get<UsuarioRecepcion[]>('/api/recepcion/usuarios')
  return data
}

export async function crearUsuarioRecepcion(email: string): Promise<UsuarioRecepcion> {
  const { data } = await api.post<UsuarioRecepcion>('/api/recepcion/usuarios', { email })
  return data
}

export async function reenviarInvitacionRecepcion(id: string): Promise<void> {
  await api.post(`/api/recepcion/usuarios/${id}/invitacion`)
}

export async function cambiarActivoRecepcion(id: string, activo: boolean): Promise<UsuarioRecepcion> {
  const { data } = await api.patch<UsuarioRecepcion>(`/api/recepcion/usuarios/${id}/activo`, { activo })
  return data
}
