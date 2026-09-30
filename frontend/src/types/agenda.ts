export type EstadoAtencion = 'RESERVADA' | 'COMPLETADA' | 'CANCELADA' | 'AUSENTE'
export type OrigenAtencion = 'ONLINE' | 'MANUAL'
export type EstadoPago = 'PAGADO' | 'PENDIENTE'
export type MetodoPago = 'EFECTIVO' | 'TRANSFERENCIA' | 'MERCADOPAGO' | 'TARJETA'

export interface Turno {
  id: string
  inicio: string
  fin: string
  estado: EstadoAtencion
  origen: OrigenAtencion
  estadoPago: EstadoPago
  metodoPago: MetodoPago | null
  barberoId: string
  barberoNombre: string
  servicioId: string
  servicioNombre: string
  precioFinal: number
  /** Null = corte anónimo (cargado a mano sin cliente asociado). */
  clienteNombre: string | null
  clienteTelefono: string | null
  notas: string | null
  /** Quién marcó el turno como completado/ausente/cancelado. Null = nunca se cambió. */
  estadoCambiadoPorEmail: string | null
  estadoCambiadoEn: string | null
}


export interface CambiarEstadoTurno {
  estado: EstadoAtencion
  precioFinal?: number
  metodoPago?: MetodoPago
}

export interface Bloqueo {
  id: string
  inicio: string
  fin: string
  motivo: string | null
  barberoId: string
  barberoNombre: string
}

export interface BarberoResumen {
  id: string
  nombre: string
}

export interface Agenda {
  desde: string
  hasta: string
  barberos: BarberoResumen[]
  turnos: Turno[]
  bloqueos: Bloqueo[]
}

export interface BloqueoDatos {
  barberoIds: string[]
  fecha: string
  horaDesde: string | null
  horaHasta: string | null
  motivo: string | null
}