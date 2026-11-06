import { Navigate, Outlet } from 'react-router-dom'
import type { Rol } from '../../types/auth'
import { useAuth } from './useAuth'

/** Dentro de RutaProtegida: deja pasar solo a los roles indicados. El resto vuelve al inicio del panel. */
export function RutaRoles({ roles }: { roles: Rol[] }) {
  const { usuario } = useAuth()
  return usuario && roles.includes(usuario.rol) ? <Outlet /> : <Navigate to="/panel" replace />
}
