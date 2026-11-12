import { api } from './client'
import type { Especialidad } from '../types/profesional'

/** Especialidades del negocio (rubro salud). Solo admin. */
export async function listarEspecialidades(): Promise<Especialidad[]> {
  const { data } = await api.get<Especialidad[]>('/api/especialidades')
  return data
}

export async function crearEspecialidad(nombre: string): Promise<Especialidad> {
  const { data } = await api.post<Especialidad>('/api/especialidades', { nombre })
  return data
}

export async function renombrarEspecialidad(id: string, nombre: string): Promise<Especialidad> {
  const { data } = await api.put<Especialidad>(`/api/especialidades/${id}`, { nombre })
  return data
}

/** Se la quita también a los profesionales que la tenían. */
export async function borrarEspecialidad(id: string): Promise<void> {
  await api.delete(`/api/especialidades/${id}`)
}
