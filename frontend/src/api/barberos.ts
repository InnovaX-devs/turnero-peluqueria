import { api } from './client'
import type {
  Barbero,
  CrearBarbero,
  CrearUsuarioBarbero,
  DatosBarbero,
  Horario,
  OpcionesBarbero,
} from '../types/barbero'

export async function listarBarberos(incluirInactivos = true): Promise<Barbero[]> {
  const { data } = await api.get<Barbero[]>('/api/barberos', { params: { incluirInactivos } })
  return data
}

export async function obtenerOpcionesBarbero(): Promise<OpcionesBarbero> {
  const { data } = await api.get<OpcionesBarbero>('/api/barberos/opciones')
  return data
}

export async function obtenerBarbero(id: string): Promise<Barbero> {
  const { data } = await api.get<Barbero>(`/api/barberos/${id}`)
  return data
}

export async function crearBarbero(datos: CrearBarbero): Promise<Barbero> {
  const { data } = await api.post<Barbero>('/api/barberos', datos)
  return data
}

export async function actualizarBarbero(id: string, datos: DatosBarbero): Promise<Barbero> {
  const { data } = await api.put<Barbero>(`/api/barberos/${id}`, datos)
  return data
}

export async function cambiarActivoBarbero(id: string, activo: boolean): Promise<Barbero> {
  const { data } = await api.patch<Barbero>(`/api/barberos/${id}/activo`, { activo })
  return data
}

/** Crea el usuario del barbero y el backend le manda la invitación por email. */
export async function crearUsuarioBarbero(id: string, datos: CrearUsuarioBarbero): Promise<Barbero> {
  const { data } = await api.post<Barbero>(`/api/barberos/${id}/usuario`, datos)
  return data
}

/** Manda un link nuevo al barbero (los anteriores dejan de valer). */
export async function reenviarInvitacionBarbero(id: string): Promise<void> {
  await api.post(`/api/barberos/${id}/invitacion`)
}

export async function subirFotoBarbero(id: string, archivo: File): Promise<Barbero> {
  const form = new FormData()
  form.append('archivo', archivo)
  const { data } = await api.post<Barbero>(`/api/barberos/${id}/foto`, form)
  return data
}

export async function quitarFotoBarbero(id: string): Promise<Barbero> {
  const { data } = await api.delete<Barbero>(`/api/barberos/${id}/foto`)
  return data
}

export async function obtenerHorarios(id: string): Promise<Horario[]> {
  const { data } = await api.get<Horario[]>(`/api/barberos/${id}/horarios`)
  return data
}

export async function guardarHorarios(id: string, horarios: Horario[]): Promise<Horario[]> {
  const { data } = await api.put<Horario[]>(`/api/barberos/${id}/horarios`, { horarios })
  return data
}