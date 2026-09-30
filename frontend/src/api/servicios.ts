import { api } from './client'
import type { DatosServicio, Servicio } from '../types/servicio'

export async function listarServicios(incluirInactivos = true): Promise<Servicio[]> {
  const { data } = await api.get<Servicio[]>('/api/servicios', { params: { incluirInactivos } })
  return data
}

export async function crearServicio(datos: DatosServicio): Promise<Servicio> {
  const { data } = await api.post<Servicio>('/api/servicios', datos)
  return data
}

export async function actualizarServicio(id: string, datos: DatosServicio): Promise<Servicio> {
  const { data } = await api.put<Servicio>(`/api/servicios/${id}`, datos)
  return data
}

export async function cambiarActivoServicio(id: string, activo: boolean): Promise<Servicio> {
  const { data } = await api.patch<Servicio>(`/api/servicios/${id}/activo`, { activo })
  return data
}

/** Recibe TODOS los servicios activos en el orden nuevo. Devuelve la lista completa actualizada. */
export async function reordenarServicios(ids: string[]): Promise<Servicio[]> {
  const { data } = await api.put<Servicio[]>('/api/servicios/orden', { ids })
  return data
}