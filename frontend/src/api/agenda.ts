import { api } from './client'
import type { Agenda, CambiarEstadoTurno, Turno } from '../types/agenda'

export async function obtenerAgenda(desde: string, hasta: string, profesionalId?: string): Promise<Agenda> {
  const { data } = await api.get<Agenda>('/api/agenda', { params: { desde, hasta, profesionalId } })
  return data
}

export async function cambiarEstadoTurno(id: string, datos: CambiarEstadoTurno): Promise<Turno> {
  const { data } = await api.patch<Turno>(`/api/agenda/turnos/${id}/estado`, datos)
  return data
}