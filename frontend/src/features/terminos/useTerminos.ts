import { useContext } from 'react'
import { TerminosContext } from './terminosContext'

/** Los términos del rubro del negocio: `t.profesional.S` → "Barbero" / "Profesional", `t.cliente.p` → "clientes" / "pacientes". */
export function useTerminos() {
  return useContext(TerminosContext)
}
