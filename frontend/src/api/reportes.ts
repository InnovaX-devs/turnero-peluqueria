import { api } from './client'
import type { ClientesInactivos, Reportes } from '../types/reportes'

export async function obtenerReportes(desde: string, hasta: string, sucursalId: string | null): Promise<Reportes> {
  const { data } = await api.get<Reportes>('/api/reportes', {
    params: { desde, hasta, ...(sucursalId ? { sucursal: sucursalId } : {}) },
  })
  return data
}

export async function obtenerClientesInactivos(dias: number): Promise<ClientesInactivos> {
  const { data } = await api.get<ClientesInactivos>('/api/reportes/clientes-inactivos', { params: { dias } })
  return data
}
