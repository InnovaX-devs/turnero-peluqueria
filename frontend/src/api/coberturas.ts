import { api } from './client'
import type { Cobertura, ReporteCoberturas } from '../types/cobertura'

/** Obras sociales / prepagas del negocio (issue #67). Listar lo puede cualquiera del panel; el resto, solo admin. */
export async function listarCoberturas(soloActivas = false): Promise<Cobertura[]> {
  const { data } = await api.get<Cobertura[]>('/api/coberturas', { params: soloActivas ? { activas: true } : {} })
  return data
}

export async function crearCobertura(nombre: string): Promise<Cobertura> {
  const { data } = await api.post<Cobertura>('/api/coberturas', { nombre })
  return data
}

export async function renombrarCobertura(id: string, nombre: string): Promise<Cobertura> {
  const { data } = await api.put<Cobertura>(`/api/coberturas/${id}`, { nombre })
  return data
}

export async function cambiarCoberturaActiva(id: string, activa: boolean): Promise<Cobertura> {
  const { data } = await api.patch<Cobertura>(`/api/coberturas/${id}/activa`, { activa })
  return data
}

/** Carga PAMI, OSDE, Swiss Medical... sin duplicar las que ya están. Devuelve la lista completa. */
export async function cargarCoberturasComunes(): Promise<Cobertura[]> {
  const { data } = await api.post<Cobertura[]>('/api/coberturas/comunes')
  return data
}

/** Atenciones completadas por obra social en el rango (fechas "YYYY-MM-DD", ambas incluidas). */
export async function reporteCoberturas(desde: string, hasta: string, coberturaId?: string): Promise<ReporteCoberturas> {
  const { data } = await api.get<ReporteCoberturas>('/api/coberturas/reporte', {
    params: { desde, hasta, ...(coberturaId ? { cobertura: coberturaId } : {}) },
  })
  return data
}
