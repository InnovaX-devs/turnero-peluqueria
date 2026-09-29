import { aFechaLocal, hoyISO, sumarDias } from '../../lib/fechas'

export type Preset = 'hoy' | 'semana' | 'mes' | 'personalizado'

export interface Periodo {
  desde: string
  hasta: string
}

/** Hoy, esta semana (desde el lunes) o este mes (desde el día 1), siempre hasta hoy. */
export function periodoDe(preset: Exclude<Preset, 'personalizado'>, hoy: string = hoyISO()): Periodo {
  if (preset === 'hoy') return { desde: hoy, hasta: hoy }
  if (preset === 'semana') {
    const diasDesdeLunes = (aFechaLocal(hoy).getDay() + 6) % 7
    return { desde: sumarDias(hoy, -diasDesdeLunes), hasta: hoy }
  }
  return { desde: `${hoy.slice(0, 8)}01`, hasta: hoy }
}

const NOMBRES_METODO: Record<string, string> = {
  EFECTIVO: 'Efectivo',
  TRANSFERENCIA: 'Transferencia',
  MERCADOPAGO: 'Mercado Pago',
  TARJETA: 'Tarjeta',
}

export function nombreMetodo(metodo: string | null): string {
  return metodo ? (NOMBRES_METODO[metodo] ?? metodo) : 'Sin informar'
}
