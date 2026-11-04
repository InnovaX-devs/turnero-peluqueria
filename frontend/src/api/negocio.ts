import { api } from './client'
import type { ActualizarNegocio, Negocio } from '../types/negocio'

export async function obtenerNegocio(): Promise<Negocio> {
  const { data } = await api.get<Negocio>('/api/negocio')
  return data
}

export async function actualizarNegocio(datos: ActualizarNegocio): Promise<Negocio> {
  const { data } = await api.put<Negocio>('/api/negocio', datos)
  return data
}

export async function subirLogoNegocio(archivo: File): Promise<Negocio> {
  const form = new FormData()
  form.append('archivo', archivo)
  const { data } = await api.post<Negocio>('/api/negocio/logo', form)
  return data
}

export async function quitarLogoNegocio(): Promise<Negocio> {
  const { data } = await api.delete<Negocio>('/api/negocio/logo')
  return data
}