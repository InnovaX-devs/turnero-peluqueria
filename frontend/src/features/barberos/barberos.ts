import type { Barbero, Horario } from '../../types/barbero'

export const DIAS = [
  { n: 1, nombre: 'Lunes' },
  { n: 2, nombre: 'Martes' },
  { n: 3, nombre: 'Miércoles' },
  { n: 4, nombre: 'Jueves' },
  { n: 5, nombre: 'Viernes' },
  { n: 6, nombre: 'Sábado' },
  { n: 7, nombre: 'Domingo' },
]

/** El backend puede devolver "10:00:00"; los <input type="time"> usan "10:00". */
export function hhmm(hora: string): string {
  return hora.slice(0, 5)
}

export function iniciales(b: Pick<Barbero, 'nombre' | 'apellido'>): string {
  return (b.nombre.charAt(0) + b.apellido.charAt(0)).toUpperCase()
}

export function normalizarHorarios(horarios: Horario[]): Horario[] {
  return ordenarHorarios(
    horarios.map((h) => ({ ...h, horaInicio: hhmm(h.horaInicio), horaFin: hhmm(h.horaFin) })),
  )
}

export function ordenarHorarios(horarios: Horario[]): Horario[] {
  return [...horarios].sort(
    (a, b) => a.diaSemana - b.diaSemana || a.horaInicio.localeCompare(b.horaInicio),
  )
}

export function mismoConjunto(a: string[], b: string[]): boolean {
  return a.length === b.length && a.every((x) => b.includes(x))
}

/** Mismas reglas que el backend. Devuelve el mensaje de error o null si está todo bien. */
export function validarHorarios(tramos: Horario[]): string | null {
  for (const t of tramos) {
    if (!t.horaInicio || !t.horaFin) return 'Completá la hora de inicio y de fin de todos los tramos'
  }
  for (const dia of DIAS) {
    const delDia = tramos
      .filter((t) => t.diaSemana === dia.n)
      .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio))
    let finMaximo = ''
    for (const t of delDia) {
      if (t.horaFin <= t.horaInicio) {
        return `${dia.nombre}: la hora de fin tiene que ser posterior a la de inicio`
      }
      if (finMaximo !== '' && t.horaInicio < finMaximo) {
        return `${dia.nombre}: hay tramos que se superponen`
      }
      if (t.horaFin > finMaximo) finMaximo = t.horaFin
    }
  }
  return null
}