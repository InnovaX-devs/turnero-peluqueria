import { api } from './client'
import type { ActualizarBarberia, Barberia } from '../types/barberia'

export async function obtenerBarberia(): Promise<Barberia> {
  const { data } = await api.get<Barberia>('/api/barberia')
  return data
}

export async function actualizarBarberia(datos: ActualizarBarberia): Promise<Barberia> {
  const { data } = await api.put<Barberia>('/api/barberia', datos)
  return data
}