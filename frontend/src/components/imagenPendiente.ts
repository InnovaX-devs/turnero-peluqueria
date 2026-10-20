/**
 * Un cambio de imagen (logo, foto) que todavía no se guardó: se elige, se ve la vista previa y
 * recién se sube cuando la persona confirma. null = sin cambios.
 */
export type CambioImagen = { tipo: 'subir'; archivo: File; vistaPrevia: string } | { tipo: 'quitar' } | null

/** Lo que hay que mostrar: la vista previa si eligió una nueva, nada si la quitó, o la guardada. */
export function imagenAMostrar(guardada: string | null, cambio: CambioImagen): string | null {
  if (cambio?.tipo === 'subir') return cambio.vistaPrevia
  if (cambio?.tipo === 'quitar') return null
  return guardada
}
