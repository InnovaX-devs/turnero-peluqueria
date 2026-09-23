import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import './panel.css'

export function PanelLayout() {
  const { usuario, cerrarSesion } = useAuth()

  return (
    <div className="panel">
      <header className="panel-header">
        <div>
          <strong>{usuario?.email}</strong>
          <span className="panel-rol">{usuario?.rol === 'ADMIN' ? 'Admin' : 'Barbero'}</span>
        </div>
        <button type="button" className="boton-secundario" onClick={cerrarSesion}>
          Cerrar sesión
        </button>
      </header>
      <nav className="panel-nav" aria-label="Secciones del panel">
        <NavLink to="/panel" end>
          Inicio
        </NavLink>
        {usuario?.rol === 'ADMIN' && <NavLink to="/panel/configuracion">Configuración</NavLink>}
      </nav>
      <main className="panel-contenido">
        <Outlet />
      </main>
    </div>
  )
}