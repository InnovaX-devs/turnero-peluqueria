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

export async function subirLogoBarberia(archivo: File): Promise<Barberia> {
  const form = new FormData()
  form.append('archivo', archivo)
  const { data } = await api.post<Barberia>('/api/barberia/logo', form)
  return data
}

export async function quitarLogoBarberia(): Promise<Barberia> {
  const { data } = await api.delete<Barberia>('/api/barberia/logo')
  return data
}