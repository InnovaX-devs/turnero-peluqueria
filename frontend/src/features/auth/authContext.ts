import { createContext } from 'react'
import type { Usuario } from '../../types/auth'

export interface AuthContextValue {
  usuario: Usuario | null
  /** true mientras se recupera la sesión al cargar la página */
  cargando: boolean
  iniciarSesion: (email: string, password: string) => Promise<void>
  cerrarSesion: () => void
  /** Vuelve a pedir los datos del usuario (ej. después de cambiar el logo de la barbería) */
  refrescarUsuario: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
