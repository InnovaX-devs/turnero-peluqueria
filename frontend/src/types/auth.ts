export type Rol = 'ADMIN' | 'BARBERO'

export interface Usuario {
  id: string
  email: string
  rol: Rol
  barberiaId: string
  /** null si el usuario no está vinculado a un barbero (ej. un admin que no corta) */
  barberoId: string | null
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
