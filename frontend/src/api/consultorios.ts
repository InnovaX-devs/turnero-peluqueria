import { api } from './client'
import type { Consultorio } from '../types/profesional'

/** Consultorios del negocio, de todas las sucursales (rubro salud, issue #69). Solo admin. */
export async function listarConsultorios(incluirInactivos = true): Promise<Consultorio[]> {
  const { data } = await api.get<Consultorio[]>('/api/consultorios', { params: { incluirInactivos } })
  return data
}

export async function crearConsultorio(sucursalId: string, nombre: string): Promise<Consultorio> {
  const { data } = await api.post<Consultorio>('/api/consultorios', { sucursalId, nombre })
  return data
}

export async function renombrarConsultorio(id: string, nombre: string): Promise<Consultorio> {
  const { data } = await api.put<Consultorio>(`/api/consultorios/${id}`, { nombre })
  return data
}

/** No se puede desactivar mientras esté asignado en algún horario (el backend responde 409). */
export async function cambiarActivoConsultorio(id: string, activo: boolean): Promise<Consultorio> {
  const { data } = await api.patch<Consultorio>(`/api/consultorios/${id}/activo`, { activo })
  return data
}
