import { Navigate, Route, Routes } from 'react-router-dom'
import { ConfiguracionBarberiaPage } from '../features/admin/ConfiguracionBarberiaPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RecuperarPage } from '../features/auth/RecuperarPage'
import { RutaAdmin } from '../features/auth/RutaAdmin'
import { RutaProtegida } from '../features/auth/RutaProtegida'
import { ClienteFichaPage } from '../features/clientes/ClienteFichaPage'
import { ClientesPage } from '../features/clientes/ClientesPage'
import { CuentaPage } from '../features/cuenta/CuentaPage'
import { PanelInicio } from '../features/panel/PanelInicio'
import { PanelLayout } from '../features/panel/PanelLayout'
import { ReservaPage } from '../features/reserva/ReservaPage'
import { GestionTurnoPage } from '../features/turno/GestionTurnoPage'
import { CajaPage } from '../features/caja/CajaPage'
import { ReportesPage } from '../features/reportes/ReportesPage'
import { BarberoEditarPage, BarberoNuevoPage } from '../features/barberos/BarberoPage'
import { BarberosPage } from '../features/barberos/BarberosPage'
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

          {/* Solo admin */}
          <Route element={<RutaAdmin />}>
            <Route path="configuracion" element={<ConfiguracionBarberiaPage />} />
            <Route path="barberos" element={<BarberosPage />} />
            <Route path="barberos/nuevo" element={<BarberoNuevoPage />} />
            <Route path="barberos/:id" element={<BarberoEditarPage />} />
            <Route path="caja" element={<CajaPage />} />
            <Route path="reportes" element={<ReportesPage />} />
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