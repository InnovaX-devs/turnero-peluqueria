import type { Agenda, Bloqueo, EstadoAtencion, EstadoPago, MetodoPago, Turno } from '../../types/agenda'

export type Vista = 'dia' | 'semana'

/** Un turno o un bloqueo, ya normalizados para dibujarse juntos en la misma lista. */
export interface ItemAgenda {
  tipo: 'turno' | 'bloqueo'
  inicio: string
  fin: string
  /** Día local (yyyy-mm-dd) al que pertenece, para agrupar en la vista semana. */
  fechaISO: string
  turno?: Turno
  bloqueo?: Bloqueo
}

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

export function formatearFechaISO(fecha: Date): string {
  return `${fecha.getFullYear()}-${pad(fecha.getMonth() + 1)}-${pad(fecha.getDate())}`
}

export function hoyISO(): string {
  return formatearFechaISO(new Date())
}

function aFechaLocal(fechaISO: string): Date {
  const [y, m, d] = fechaISO.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function sumarDias(fechaISO: string, dias: number): string {
  const fecha = aFechaLocal(fechaISO)
  fecha.setDate(fecha.getDate() + dias)
  return formatearFechaISO(fecha)
}

/** Lunes de la semana que contiene fechaISO. */
export function inicioDeSemana(fechaISO: string): string {
  const fecha = aFechaLocal(fechaISO)
  const diaSemana = fecha.getDay() // 0 domingo .. 6 sábado
  const offset = diaSemana === 0 ? -6 : 1 - diaSemana
  fecha.setDate(fecha.getDate() + offset)
  return formatearFechaISO(fecha)
}

export function rangoDeVista(vista: Vista, fechaISO: string): { desde: string; hasta: string } {
  if (vista === 'dia') {
    return { desde: fechaISO, hasta: fechaISO }
  }
  const desde = inicioDeSemana(fechaISO)
  return { desde, hasta: sumarDias(desde, 6) }
}

export function diasDelRango(desde: string, hasta: string): string[] {
  const dias: string[] = []
  let actual = desde
  while (actual <= hasta) {
    dias.push(actual)
    actual = sumarDias(actual, 1)
  }
  return dias
}

export function formatearHora(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })
}

export function formatearFechaLarga(fechaISO: string): string {
  return aFechaLocal(fechaISO).toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' })
}

export function formatearFechaCorta(fechaISO: string): string {
  return aFechaLocal(fechaISO).toLocaleDateString('es-AR', { weekday: 'short', day: 'numeric', month: 'short' })
}

/** precioFinal viene en centavos ($15.000,00 = 1500000). */
export function formatearPrecio(centavos: number): string {
  return (centavos / 100).toLocaleString('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 })
}

export const ETIQUETAS_ESTADO: Record<EstadoAtencion, string> = {
  RESERVADA: 'Reservado',
  COMPLETADA: 'Completado',
  CANCELADA: 'Cancelado',
  AUSENTE: 'Ausente',
}

export const ETIQUETAS_PAGO: Record<EstadoPago, string> = {
  PAGADO: 'Pagado',
  PENDIENTE: 'Pendiente',
}

export const ETIQUETAS_METODO_PAGO: Record<MetodoPago, string> = {
  EFECTIVO: 'Efectivo',
  TRANSFERENCIA: 'Transferencia',
  MERCADOPAGO: 'Mercado Pago',
  TARJETA: 'Tarjeta',
}

/** Junta turnos y bloqueos en una sola lista, cada uno con el día local al que pertenece. */
export function combinarItems(agenda: Agenda): ItemAgenda[] {
  const turnos: ItemAgenda[] = agenda.turnos.map((t) => ({
    tipo: 'turno',
    inicio: t.inicio,
    fin: t.fin,
    fechaISO: formatearFechaISO(new Date(t.inicio)),
    turno: t,
  }))
  const bloqueos: ItemAgenda[] = agenda.bloqueos.map((b) => ({
    tipo: 'bloqueo',
    inicio: b.inicio,
    fin: b.fin,
    fechaISO: formatearFechaISO(new Date(b.inicio)),
    bloqueo: b,
  }))
  return [...turnos, ...bloqueos].sort((a, b) => a.inicio.localeCompare(b.inicio))
}

export function agruparPorDia(items: ItemAgenda[], dias: string[]): Map<string, ItemAgenda[]> {
  const mapa = new Map<string, ItemAgenda[]>()
  dias.forEach((dia) => mapa.set(dia, []))
  items.forEach((item) => {
    mapa.get(item.fechaISO)?.push(item)
  })
  return mapa
}

/** wa.me necesita el teléfono en dígitos, sin el "+" (el teléfono ya viene en formato E.164). */
export function linkWhatsApp(telefono: string): string {
  return `https://wa.me/${telefono.replace(/\D/g, '')}`
}