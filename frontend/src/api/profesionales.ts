import { api } from './client'
import type {
  Profesional,
  CrearProfesional,
  CrearUsuarioProfesional,
  DatosProfesional,
  Horario,
  OpcionesProfesional,
} from '../types/profesional'

export async function listarProfesionales(incluirInactivos = true): Promise<Profesional[]> {
  const { data } = await api.get<Profesional[]>('/api/profesionales', { params: { incluirInactivos } })
  return data
}

export async function obtenerOpcionesProfesional(): Promise<OpcionesProfesional> {
  const { data } = await api.get<OpcionesProfesional>('/api/profesionales/opciones')
  return data
}

export async function obtenerProfesional(id: string): Promise<Profesional> {
  const { data } = await api.get<Profesional>(`/api/profesionales/${id}`)
  return data
}

export async function crearProfesional(datos: CrearProfesional): Promise<Profesional> {
  const { data } = await api.post<Profesional>('/api/profesionales', datos)
  return data
}

export async function actualizarProfesional(id: string, datos: DatosProfesional): Promise<Profesional> {
  const { data } = await api.put<Profesional>(`/api/profesionales/${id}`, datos)
  return data
}

export async function cambiarActivoProfesional(id: string, activo: boolean): Promise<Profesional> {
  const { data } = await api.patch<Profesional>(`/api/profesionales/${id}/activo`, { activo })
  return data
}

/** Crea el usuario del barbero y el backend le manda la invitación por email. */
export async function crearUsuarioProfesional(id: string, datos: CrearUsuarioProfesional): Promise<Profesional> {
  const { data } = await api.post<Profesional>(`/api/profesionales/${id}/usuario`, datos)
  return data
}

/** Manda un link nuevo al barbero (los anteriores dejan de valer). */
export async function reenviarInvitacionProfesional(id: string): Promise<void> {
  await api.post(`/api/profesionales/${id}/invitacion`)
}

export async function subirFotoProfesional(id: string, archivo: File): Promise<Profesional> {
  const form = new FormData()
  form.append('archivo', archivo)
  const { data } = await api.post<Profesional>(`/api/profesionales/${id}/foto`, form)
  return data
}

export async function quitarFotoProfesional(id: string): Promise<Profesional> {
  const { data } = await api.delete<Profesional>(`/api/profesionales/${id}/foto`)
  return data
}

export async function obtenerHorarios(id: string): Promise<Horario[]> {
  const { data } = await api.get<Horario[]>(`/api/profesionales/${id}/horarios`)
  return data
}

export async function guardarHorarios(id: string, horarios: Horario[]): Promise<Horario[]> {
  const { data } = await api.put<Horario[]>(`/api/profesionales/${id}/horarios`, { horarios })
  return data
}