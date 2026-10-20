const MS_POR_DIA = 24 * 60 * 60 * 1000

/** "Vino hoy", "Vino ayer", "Vino hace 12 días", "Vino el 03/02/2026" o "Todavía no vino". */
export function textoUltimaVisita(iso: string | null, ahora: Date = new Date()): string {
  if (!iso) return 'Todavía no vino'
  const fecha = new Date(iso)
  const inicioDe = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const dias = Math.round((inicioDe(ahora) - inicioDe(fecha)) / MS_POR_DIA)
  if (dias <= 0) return 'Vino hoy'
  if (dias === 1) return 'Vino ayer'
  if (dias < 60) return `Vino hace ${dias} días`
  return `Vino el ${fecha.toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' })}`
}
