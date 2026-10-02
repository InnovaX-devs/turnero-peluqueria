import { api } from './client'
import type { ClienteListado, FichaCliente } from '../types/cliente'

export async function listarClientes(q: string): Promise<ClienteListado[]> {
  const { data } = await api.get<ClienteListado[]>('/api/clientes/listado', { params: { q } })
  return data
}

export async function obtenerFichaCliente(id: string): Promise<FichaCliente> {
  const { data } = await api.get<FichaCliente>(`/api/clientes/${id}`)
  return data
}

export async function actualizarNotasCliente(id: string, notas: string): Promise<FichaCliente> {
  const { data } = await api.patch<FichaCliente>(`/api/clientes/${id}/notas`, { notas })
  return data
}