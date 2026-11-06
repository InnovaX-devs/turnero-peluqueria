import { api } from './client'
import type { AnotarListaEspera, EntradaListaEspera } from '../types/listaEspera'

export async function obtenerListaEspera(fecha: string): Promise<EntradaListaEspera[]> {
  const { data } = await api.get<EntradaListaEspera[]>('/api/lista-espera', { params: { fecha } })
  return data
}

export async function anotarListaEspera(datos: AnotarListaEspera): Promise<EntradaListaEspera> {
  const { data } = await api.post<EntradaListaEspera>('/api/lista-espera', datos)
  return data
}

export async function resolverListaEspera(id: string): Promise<void> {
  await api.patch(`/api/lista-espera/${id}/resuelto`)
}
