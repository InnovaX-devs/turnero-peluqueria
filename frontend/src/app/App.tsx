import { Navigate, Route, Routes } from 'react-router-dom'
import { ConfiguracionBarberiaPage } from '../features/admin/ConfiguracionBarberiaPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RecuperarPage } from '../features/auth/RecuperarPage'
import { RutaAdmin } from '../features/auth/RutaAdmin'
import { RutaProtegida } from '../features/auth/RutaProtegida'
import { PanelInicio } from '../features/panel/PanelInicio'
import { PanelLayout } from '../features/panel/PanelLayout'
import { ReservaPage } from '../features/reserva/ReservaPage'
import { BarberoEditarPage, BarberoNuevoPage } from '../features/barberos/BarberoPage'
import { BarberosPage } from '../features/barberos/BarberosPage'

export function App() {
  return (
    <Routes>
      {/* Panel: públicas */}
      <Route path="/panel/login" element={<LoginPage />} />
      <Route path="/panel/recuperar" element={<RecuperarPage />} />

      {/* Panel: requieren sesión */}
      <Route element={<RutaProtegida />}>
        <Route path="/panel" element={<PanelLayout />}>
          <Route index element={<PanelInicio />} />

          {/* Solo admin */}
          <Route element={<RutaAdmin />}>
            <Route path="configuracion" element={<ConfiguracionBarberiaPage />} />
            <Route path="barberos" element={<BarberosPage />} />
            <Route path="barberos/nuevo" element={<BarberoNuevoPage />} />
            <Route path="barberos/:id" element={<BarberoEditarPage />} />
          </Route>
        </Route>
      </Route>

      {/* Reserva pública: sin login */}
      <Route path="/b/:slug" element={<ReservaPage />} />

      <Route path="*" element={<Navigate to="/panel" replace />} />
    </Routes>
  )
}