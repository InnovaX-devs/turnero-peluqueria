import { createContext } from 'react'
import type { Usuario } from '../../types/auth'

export interface AuthContextValue {
  usuario: Usuario | null
  /** true mientras se recupera la sesión al cargar la página */
  cargando: boolean
  iniciarSesion: (email: string, password: string) => Promise<void>
  cerrarSesion: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
