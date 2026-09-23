export interface Barberia {
  id: string
  nombre: string
  slug: string
  logoUrl: string | null
  reservasOnline: boolean
  anticipacionMinimaMin: number
  anticipacionMaximaDias: number
  intervaloSlotsMin: number
}

export type ActualizarBarberia = Omit<Barberia, 'id'>