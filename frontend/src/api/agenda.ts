import { api } from './client'
import type { Agenda } from '../types/agenda'

export async function obtenerAgenda(desde: string, hasta: string, barberoId?: string): Promise<Agenda> {
  const { data } = await api.get<Agenda>('/api/agenda', { params: { desde, hasta, barberoId } })
  return data
}