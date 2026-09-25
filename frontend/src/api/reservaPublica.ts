import { api } from './client'
import type { BarberiaPublica } from '../types/reserva'

export async function obtenerBarberiaPublica(slug: string): Promise<BarberiaPublica> {
  const { data } = await api.get<BarberiaPublica>(`/api/public/barberias/${encodeURIComponent(slug)}`)
  return data
}