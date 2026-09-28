// Fechas "de calendario" (yyyy-mm-dd) en hora local del navegador, sin líos de zona horaria.

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

export function aFechaLocal(fechaISO: string): Date {
  const [y, m, d] = fechaISO.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function aFechaISO(fecha: Date): string {
  return `${fecha.getFullYear()}-${pad(fecha.getMonth() + 1)}-${pad(fecha.getDate())}`
}

export function hoyISO(): string {
  return aFechaISO(new Date())
}

export function sumarDias(fechaISO: string, dias: number): string {
  const fecha = aFechaLocal(fechaISO)
  fecha.setDate(fecha.getDate() + dias)
  return aFechaISO(fecha)
}

/** Cuántos días van de `desde` a `hasta` (negativo si hasta es anterior). */
export function diasEntre(desde: string, hasta: string): number {
  return Math.round((aFechaLocal(hasta).getTime() - aFechaLocal(desde).getTime()) / 86_400_000)
}

function capitalizar(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

/** "Lun", "Mar", "Mié"… */
export function diaSemanaCorto(fechaISO: string): string {
  return capitalizar(aFechaLocal(fechaISO).toLocaleDateString('es-AR', { weekday: 'short' }).replace('.', ''))
}

/** "Septiembre 2026" */
export function mesYAnio(fechaISO: string): string {
  const fecha = aFechaLocal(fechaISO)
  return `${capitalizar(fecha.toLocaleDateString('es-AR', { month: 'long' }))} ${fecha.getFullYear()}`
}

/** "miércoles, 23 de septiembre" */
export function fechaLarga(fechaISO: string): string {
  return aFechaLocal(fechaISO).toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' })
}

/** "10:00:00" o "10:00" → "10:00" */
export function horaCorta(hora: string): string {
  return hora.slice(0, 5)
}
