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
  profesionalId: string
  profesionalNombre: string
  servicioId: string
  servicioNombre: string
  precioFinal: number
  /** Null = corte anónimo (cargado a mano sin cliente asociado). */
  clienteId: string | null
  clienteNombre: string | null
  clienteTelefono: string | null
  /** Nota de la ficha del cliente (ej. "degradé en 2, arriba tijera"). */
  clienteNotas: string | null
  /** Nota de este turno en particular (la que dejó quien reservó). */
  notas: string | null
  /** Quién marcó el turno como completado/ausente/cancelado. Null = nunca se cambió. */
  estadoCambiadoPorEmail: string | null
  estadoCambiadoEn: string | null
  /** Cuándo llegó el cliente (sala de espera). Null = todavía no llegó. */
  llegadaEn: string | null
  /** Cargado a propósito encima de otro turno o fuera de horario (solo admin/recepción). */
  sobreturno: boolean
}

/** Turno dado desde el panel. Fecha (YYYY-MM-DD) y hora (HH:mm) en la zona del negocio. */
export interface NuevoTurno {
  clienteId: string
  servicioId: string
  profesionalId: string
  sucursalId: string | null
  fecha: string
  hora: string
  notas: string | null
  sobreturno: boolean
}

export interface HorariosLibres {
  fecha: string
  /** "HH:mm" o "HH:mm:ss" */
  horas: string[]
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
  profesionalId: string
  profesionalNombre: string
}

export interface ProfesionalResumen {
  id: string
  nombre: string
}

export interface Agenda {
  desde: string
  hasta: string
  profesionales: ProfesionalResumen[]
  turnos: Turno[]
  bloqueos: Bloqueo[]
}

export interface BloqueoDatos {
  profesionalIds: string[]
  fecha: string
  horaDesde: string | null
  horaHasta: string | null
  motivo: string | null
}