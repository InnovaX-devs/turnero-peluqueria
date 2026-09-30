import { useEffect, useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { RegistrarCorte } from '../atenciones/RegistrarCorte'
import { useOpcionesRegistro } from '../atenciones/useOpcionesRegistro'
import './panel.css'

export function PanelLayout() {
  const { usuario, cerrarSesion } = useAuth()
  const [registrando, setRegistrando] = useState(false)
  const [aviso, setAviso] = useState<string | null>(null)

  // Precarga servicios y barberos: así el formulario abre al instante
  useOpcionesRegistro()

  useEffect(() => {
    if (!aviso) return
    const timer = setTimeout(() => setAviso(null), 3000)
    return () => clearTimeout(timer)
  }, [aviso])

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
        {usuario?.rol === 'ADMIN' && <NavLink to="/panel/caja">Caja</NavLink>}
        {usuario?.rol === 'ADMIN' && <NavLink to="/panel/reportes">Reportes</NavLink>}
        {usuario?.rol === 'ADMIN' && <NavLink to="/panel/barberos">Barberos</NavLink>}
        {usuario?.rol === 'ADMIN' && <NavLink to="/panel/servicios">Servicios</NavLink>}
        {usuario?.rol === 'ADMIN' && <NavLink to="/panel/configuracion">Configuración</NavLink>}
      </nav>
      <main className="panel-contenido">
        <Outlet />
      </main>

      {/* Siempre visible, en cualquier pantalla del panel */}
      <button type="button" className="boton-registrar" onClick={() => setRegistrando(true)}>
        + Registrar corte
      </button>

      {registrando && (
        <RegistrarCorte
          onCerrar={() => setRegistrando(false)}
          onRegistrado={() => {
            setRegistrando(false)
            setAviso('Corte registrado')
          }}
        />
      )}

      {aviso && (
        <div className="panel-aviso" role="status">
          {aviso}
        </div>
      )}
    </div>
  )
}
