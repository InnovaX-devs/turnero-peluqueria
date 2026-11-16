/** Obra social o prepaga (issue #67, rubro salud). */
export interface Cobertura {
  id: string
  nombre: string
  /** Las inactivas no se ofrecen en la reserva ni se pueden asignar, pero siguen en el reporte */
  activa: boolean
}

/** Cómo se paga una atención. Fuera de salud siempre es PARTICULAR. */
export type TipoPago = 'PARTICULAR' | 'COBERTURA' | 'COBERTURA_CON_COPAGO'

export const ETIQUETA_TIPO_PAGO: Record<TipoPago, string> = {
  PARTICULAR: 'Particular',
  COBERTURA: 'Obra social',
  COBERTURA_CON_COPAGO: 'Obra social con copago',
}

export interface ResumenCobertura {
  coberturaId: string
  nombre: string
  cantidad: number
  /** Centavos */
  totalPrestaciones: number
  /** Centavos */
  totalCopagos: number
}

export interface DetalleCobertura {
  atencionId: string
  /** Fecha y hora local del negocio, "2026-11-17T10:00:00" */
  fecha: string
  coberturaId: string
  coberturaNombre: string
  tipoPago: TipoPago
  pacienteNombre: string | null
  pacienteDni: string | null
  numeroAfiliado: string | null
  plan: string | null
  profesional: string
  matricula: string | null
  prestacion: string
  /** Centavos */
  precio: number
  /** Centavos */
  copago: number | null
  numeroAutorizacion: string | null
}

export interface ReporteCoberturas {
  desde: string
  hasta: string
  cantidad: number
  totalPrestaciones: number
  totalCopagos: number
  porCobertura: ResumenCobertura[]
  detalle: DetalleCobertura[]
}
