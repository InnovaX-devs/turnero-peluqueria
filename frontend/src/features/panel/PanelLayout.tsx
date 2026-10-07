import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { RegistrarCorte } from '../atenciones/RegistrarCorte'
import { useOpcionesRegistro } from '../atenciones/useOpcionesRegistro'
import '../cuenta/cuenta.css'
import { BottomBar } from './BottomBar'
import { TopBar } from './TopBar'
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
      <TopBar usuario={usuario} onRegistrar={() => setRegistrando(true)} onCerrarSesion={cerrarSesion} />
      <main className="panel-contenido">
        <Outlet />
      </main>

      <BottomBar usuario={usuario} onRegistrar={() => setRegistrando(true)} onCerrarSesion={cerrarSesion} />

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