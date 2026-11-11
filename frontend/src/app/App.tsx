import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { ConfiguracionNegocioPage } from '../features/admin/ConfiguracionNegocioPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RecuperarPage } from '../features/auth/RecuperarPage'
import { RutaAdmin } from '../features/auth/RutaAdmin'
import { RutaProtegida } from '../features/auth/RutaProtegida'
import { RutaRoles } from '../features/auth/RutaRoles'
import { ClienteFichaPage } from '../features/clientes/ClienteFichaPage'
import { ClientesPage } from '../features/clientes/ClientesPage'
import { CuentaPage } from '../features/cuenta/CuentaPage'
import { PanelInicio } from '../features/panel/PanelInicio'
import { PanelLayout } from '../features/panel/PanelLayout'
import { ReservaPage } from '../features/reserva/ReservaPage'
import { GestionTurnoPage } from '../features/turno/GestionTurnoPage'
import { CajaPage } from '../features/caja/CajaPage'
import { ReportesPage } from '../features/reportes/ReportesPage'
import { ProfesionalEditarPage, ProfesionalNuevoPage } from '../features/profesionales/ProfesionalPage'
import { ProfesionalesPage } from '../features/profesionales/ProfesionalesPage'
import { ServiciosPage } from '../features/servicios/ServiciosPage'
import { NotificacionesPage } from '../features/notificaciones/NotificacionesPage'
import { RestablecerPage } from '../features/auth/RestablecerPage'

export function App() {
  return (
    <Routes>
      {/* Panel: públicas */}
      <Route path="/panel/login" element={<LoginPage />} />
      <Route path="/panel/recuperar" element={<RecuperarPage />} />
      <Route path="/panel/restablecer" element={<RestablecerPage />} />

      {/* Panel: requieren sesión */}
      <Route element={<RutaProtegida />}>
        <Route path="/panel" element={<PanelLayout />}>
          <Route index element={<PanelInicio />} />
          <Route path="mensajes" element={<NotificacionesPage />} />
          <Route path="clientes" element={<ClientesPage />} />
          <Route path="clientes/:id" element={<ClienteFichaPage />} />
          <Route path="cuenta" element={<CuentaPage />} />
          {/* Admin: todo el negocio. Profesional: solo lo suyo (lo filtra el backend). Recepción no. */}
          <Route element={<RutaRoles roles={['ADMIN', 'PROFESIONAL']} />}>
            <Route path="reportes" element={<ReportesPage />} />
          </Route>

          {/* Solo admin */}
          <Route element={<RutaAdmin />}>
            <Route path="configuracion" element={<ConfiguracionNegocioPage />} />
            <Route path="profesionales" element={<ProfesionalesPage />} />
            <Route path="profesionales/nuevo" element={<ProfesionalNuevoPage />} />
            <Route path="profesionales/:id" element={<ProfesionalEditarPage />} />
            {/* Rutas anteriores al issue #64: redirigen a las nuevas para no romper marcadores */}
            <Route path="barberos" element={<Navigate to="/panel/profesionales" replace />} />
            <Route path="barberos/nuevo" element={<Navigate to="/panel/profesionales/nuevo" replace />} />
            <Route path="barberos/:id" element={<RedireccionProfesional />} />
            <Route path="caja" element={<CajaPage />} />
            <Route path="servicios" element={<ServiciosPage />} />
          </Route>
        </Route>
      </Route>

      {/* Reserva pública: sin login */}
      <Route path="/b/:slug" element={<ReservaPage />} />
      <Route path="/turno/:token" element={<GestionTurnoPage />} />

      <Route path="*" element={<Navigate to="/panel" replace />} />
    </Routes>
  )
}

/** /panel/barberos/:id (ruta anterior al issue #64) -> /panel/profesionales/:id */
function RedireccionProfesional() {
  const { id = '' } = useParams()
  return <Navigate to={'/panel/profesionales/' + id} replace />
}
