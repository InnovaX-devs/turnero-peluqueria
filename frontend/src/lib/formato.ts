/** Los precios vienen en centavos desde la API ($15.000,00 = 1500000). */
export function formatearPrecio(centavos: number): string {
  return (centavos / 100).toLocaleString('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 })
}

/** 30 → "30 min", 60 → "1 h", 90 → "1 h 30 min". */
export function formatearDuracion(minutos: number): string {
  const horas = Math.floor(minutos / 60)
  const resto = minutos % 60
  if (horas === 0) return `${resto} min`
  return resto === 0 ? `${horas} h` : `${horas} h ${resto} min`
}

function aFechaLocal(fechaISO: string): Date {
  const [y, m, d] = fechaISO.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/** "hoy", "mañana", "el jueves" (dentro de la semana) o "el 12/10". */
export function formatearProximoDia(fechaISO: string, hoy: Date = new Date()): string {
  const inicioHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate())
  const dias = Math.round((aFechaLocal(fechaISO).getTime() - inicioHoy.getTime()) / 86_400_000)
  if (dias <= 0) return 'hoy'
  if (dias === 1) return 'mañana'
  const fecha = aFechaLocal(fechaISO)
  if (dias < 7) return `el ${fecha.toLocaleDateString('es-AR', { weekday: 'long' })}`
  return `el ${fecha.getDate()}/${fecha.getMonth() + 1}`
}
