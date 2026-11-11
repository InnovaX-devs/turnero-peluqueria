import { useMemo, type ReactNode } from 'react'
import { useAuth } from '../auth/useAuth'
import { terminosDe, type Rubro } from '../../lib/terminos'
import { TerminosContext } from './terminosContext'

/** Páginas públicas (reserva, gestión del turno): el rubro viene en la respuesta del negocio. */
export function TerminosProvider({ rubro, children }: { rubro: Rubro | null | undefined; children: ReactNode }) {
  const terminos = useMemo(() => terminosDe(rubro), [rubro])
  return <TerminosContext.Provider value={terminos}>{children}</TerminosContext.Provider>
}

/** Panel: el rubro es el del negocio del usuario logueado. Antes de loguearse (login, recuperar) se usan los términos por defecto. */
export function TerminosDelUsuario({ children }: { children: ReactNode }) {
  const { usuario } = useAuth()
  return <TerminosProvider rubro={usuario?.rubro}>{children}</TerminosProvider>
}
