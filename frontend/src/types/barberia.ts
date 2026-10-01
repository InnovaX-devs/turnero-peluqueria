export interface Barberia {
  id: string
  nombre: string
  slug: string
  logoUrl: string | null
  reservasOnline: boolean
  anticipacionMinimaMin: number
  anticipacionMaximaDias: number
  intervaloSlotsMin: number
  /** Hasta cuántos minutos antes del turno el cliente puede cancelarlo o cambiarlo desde su link */
  anticipacionCancelacionMin: number
  /** Cuántas horas antes del turno se le avisa al cliente con un recordatorio */
  recordatorioHorasAntes: number
}

export type ActualizarBarberia = Omit<Barberia, 'id'>