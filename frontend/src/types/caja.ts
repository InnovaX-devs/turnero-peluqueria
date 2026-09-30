/** Todos los montos en centavos. */
export interface GrupoCaja {
  id: string
  nombre: string
  cantidad: number
  total: number
}

export interface GrupoMetodoPago {
  /** null = se completó sin informar el método */
  metodo: string | null
  cantidad: number
  total: number
}

export interface DiaCaja {
  fecha: string
  cantidad: number
  total: number
}

export interface Caja {
  desde: string
  hasta: string
  total: number
  cantidad: number
  ticketPromedio: number
  porBarbero: GrupoCaja[]
  porServicio: GrupoCaja[]
  porMetodoPago: GrupoMetodoPago[]
  porDia: DiaCaja[]
}

export interface ConsultaCaja {
  desde: string
  hasta: string
  /** null = todas las sucursales */
  sucursalId: string | null
}
