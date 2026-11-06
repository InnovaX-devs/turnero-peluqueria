/** RECEPCION: secretaría, da turnos para todos los profesionales (issue #70). */
export type Rol = 'ADMIN' | 'PROFESIONAL' | 'RECEPCION'

export interface Usuario {
  id: string
  email: string
  rol: Rol
  negocioId: string
  /** null si el usuario no está vinculado a un barbero (ej. un admin que no corta, o recepción) */
  profesionalId: string | null
  negocioNombre: string
  /** null si la barbería no cargó logo */
  negocioLogoUrl: string | null
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
