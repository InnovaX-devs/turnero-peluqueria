import { api } from './client'
import type { EstadoNotificacion, Notificacion } from '../types/notificacion'

export async function listarNotificaciones(estado: EstadoNotificacion = 'PENDIENTE'): Promise<Notificacion[]> {
  const { data } = await api.get<Notificacion[]>('/api/notificaciones', { params: { estado } })
  return data
}

export async function marcarNotificacionEnviada(id: string): Promise<Notificacion> {
  const { data } = await api.post<Notificacion>(`/api/notificaciones/${id}/enviada`)
  return data
}