import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../features/auth/LoginPage'
import { RecuperarPage } from '../features/auth/RecuperarPage'
import { RutaProtegida } from '../features/auth/RutaProtegida'
import { PanelInicio } from '../features/panel/PanelInicio'
import { PanelLayout } from '../features/panel/PanelLayout'

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
        </Route>
      </Route>

      {/* La reserva pública (/b/:slug) se agrega en la épica 1 */}
      <Route path="*" element={<Navigate to="/panel" replace />} />
    </Routes>
  )
}
