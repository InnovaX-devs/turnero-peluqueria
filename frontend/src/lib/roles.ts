import type { Rol } from '../types/auth'

/** Cómo se muestra el rol en el panel (barra, menú, Mi cuenta). */
export function etiquetaRol(rol: Rol | undefined): string {
  switch (rol) {
    case 'ADMIN':
      return 'Admin'
    case 'RECEPCION':
      return 'Recepción'
    default:
      return 'Barbero'
  }
}

/**
 * Admin y recepción operan sobre la agenda de todos los profesionales (filtro "Todos").
 * Un profesional, solo sobre la suya. Mismo criterio que UsuarioActual.veTodoElNegocio() del backend.
 */
export function veTodoElNegocio(rol: Rol | undefined): boolean {
  return rol === 'ADMIN' || rol === 'RECEPCION'
}
