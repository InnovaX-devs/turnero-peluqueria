import { api } from './client'
import type { Bloqueo, BloqueoDatos, Turno } from '../types/agenda'

/** Turnos reservados que el bloqueo taparía. No guarda nada. */
export async function verificarConflictos(datos: BloqueoDatos): Promise<Turno[]> {
  const { data } = await api.post<Turno[]>('/api/bloqueos/conflictos', datos)
  return data
}

export async function crearBloqueo(datos: BloqueoDatos): Promise<Bloqueo> {
  const { data } = await api.post<Bloqueo>('/api/bloqueos', datos)
  return data
}

export async function actualizarBloqueo(id: string, datos: BloqueoDatos): Promise<Bloqueo> {
  const { data } = await api.put<Bloqueo>('/api/bloqueos/' + id, datos)
  return data
}

export async function eliminarBloqueo(id: string): Promise<void> {
  await api.delete('/api/bloqueos/' + id)
}