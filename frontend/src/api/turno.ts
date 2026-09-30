import { api } from './client'
import type { TurnoPublico } from '../types/turno'

export async function obtenerTurno(token: string): Promise<TurnoPublico> {
  const { data } = await api.get<TurnoPublico>(`/api/public/turnos/${token}`)
  return data
}

export async function cancelarTurno(token: string): Promise<TurnoPublico> {
  const { data } = await api.post<TurnoPublico>(`/api/public/turnos/${token}/cancelar`)
  return data
}

/** fecha "YYYY-MM-DD" y hora "HH:mm", en la hora de la barbería (igual que al reservar). */
export async function reprogramarTurno(token: string, fecha: string, hora: string): Promise<TurnoPublico> {
  const { data } = await api.put<TurnoPublico>(`/api/public/turnos/${token}/reprogramar`, { fecha, hora })
  return data
}
