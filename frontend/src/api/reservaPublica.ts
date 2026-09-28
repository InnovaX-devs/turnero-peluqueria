import { api } from './client'
import type { BarberiaPublica, BarberoPublico, ServicioPublico } from '../types/reserva'

export async function obtenerBarberiaPublica(slug: string): Promise<BarberiaPublica> {
  const { data } = await api.get<BarberiaPublica>(`/api/public/barberias/${encodeURIComponent(slug)}`)
  return data
}

/** Solo los servicios que se pueden reservar en esa sucursal. */
export async function obtenerServiciosPublicos(slug: string, sucursalId: string): Promise<ServicioPublico[]> {
  const { data } = await api.get<ServicioPublico[]>(`/api/public/barberias/${encodeURIComponent(slug)}/servicios`, {
    params: { sucursal: sucursalId },
  })
  return data
}

/** Barberos que hacen ese servicio en esa sucursal, con su próximo día libre. */
export async function obtenerBarberosPublicos(
  slug: string,
  sucursalId: string,
  servicioId: string,
): Promise<BarberoPublico[]> {
  const { data } = await api.get<BarberoPublico[]>(`/api/public/barberias/${encodeURIComponent(slug)}/barberos`, {
    params: { sucursal: sucursalId, servicio: servicioId },
  })
  return data
}
