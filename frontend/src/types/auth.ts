import type { Rubro } from '../lib/terminos'

/** RECEPCION: secretaría, da turnos para todos los profesionales (issue #70). */
export type Rol = 'ADMIN' | 'PROFESIONAL' | 'RECEPCION'

export interface Usuario {
  id: string
  email: string
  rol: Rol
  negocioId: string
  /** null si el usuario no está vinculado a un profesional (ej. un admin que no atiende, o recepción) */
  profesionalId: string | null
  negocioNombre: string
  /** null si el negocio no cargó logo */
  negocioLogoUrl: string | null
  /** Cómo se nombran las cosas en el panel (barbero / profesional, cliente / paciente...) */
  rubro: Rubro
}

export interface LoginResponse {
  token: string
  expiraEn: string
  usuario: Usuario
}

/** Formato de error que devuelve el backend (ApiError.java) */
export interface ApiError {
  status: number
  mensaje: string
  errores: Record<string, string>
}
