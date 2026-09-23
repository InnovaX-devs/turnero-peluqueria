import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './useAuth'

/** Dentro de RutaProtegida: deja pasar solo a admins. Los barberos vuelven al inicio del panel. */
export function RutaAdmin() {
  const { usuario } = useAuth()
  return usuario?.rol === 'ADMIN' ? <Outlet /> : <Navigate to="/panel" replace />
}