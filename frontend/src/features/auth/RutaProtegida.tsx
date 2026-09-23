import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'

/** Envuelve las rutas del panel: sin sesión, manda al login. */
export function RutaProtegida() {
  const { usuario, cargando } = useAuth()
  const location = useLocation()

  if (cargando) {
    return <div className="pantalla-centrada">Cargando…</div>
  }

  if (!usuario) {
    // Guardamos a dónde quería ir para volver ahí después del login
    return <Navigate to="/panel/login" replace state={{ desde: location.pathname }} />
  }

  return <Outlet />
}
