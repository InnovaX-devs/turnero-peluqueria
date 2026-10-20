import { api } from './client'
import type { FichaCliente, OrdenClientes, PaginaClientes } from '../types/cliente'

export const CLIENTES_POR_PAGINA = 50

/** pagina arranca en 0. */
export async function listarClientes(q: string, pagina: number, orden: OrdenClientes): Promise<PaginaClientes> {
  const { data } = await api.get<PaginaClientes>('/api/clientes/listado', {
    params: { q, pagina, orden, tamano: CLIENTES_POR_PAGINA },
  })
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