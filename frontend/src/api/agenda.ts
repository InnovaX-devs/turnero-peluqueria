import { api } from './client'
import type { Agenda, CambiarEstadoTurno, Turno } from '../types/agenda'

export async function obtenerAgenda(desde: string, hasta: string, profesionalId?: string): Promise<Agenda> {
  const { data } = await api.get<Agenda>('/api/agenda', { params: { desde, hasta, profesionalId } })
  return data
}

/** Sala de espera: true cuando el cliente llega, false para desmarcarlo. */
export async function marcarLlegadaTurno(id: string, llego: boolean): Promise<Turno> {
  const { data } = await api.patch<Turno>(`/api/agenda/turnos/${id}/llegada`, { llego })
  return data
}

export async function cambiarEstadoTurno(id: string, datos: CambiarEstadoTurno): Promise<Turno> {
  const { data } = await api.patch<Turno>(`/api/agenda/turnos/${id}/estado`, datos)
  return data
}