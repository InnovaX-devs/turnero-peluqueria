/** Montos en centavos. Fechas "YYYY-MM-DD". */
export interface BarberoReporte {
  id: string
  nombre: string
  atendidos: number
  ausentes: number
  cancelados: number
  /** Lo cobrado en los cortes atendidos del período, en centavos */
  facturado: number
}

export interface ClienteFrecuente {
  id: string
  nombre: string
  telefono: string
  visitas: number
  total: number
  ultimaVisita: string
}

export interface Ausentismo {
  atendidos: number
  ausentes: number
  cancelados: number
  /** Turnos que ya pasaron y siguen como reservados */
  sinMarcar: number
}

export interface Demanda {
  /** 1 = lunes … 7 = domingo */
  diaSemana: number
  hora: number
  cantidad: number
}

export interface Reportes {
  desde: string
  hasta: string
  porBarbero: BarberoReporte[]
  clientesFrecuentes: ClienteFrecuente[]
  ausentismo: Ausentismo
  /** Ordenada de mayor a menor demanda */
  demanda: Demanda[]
}

export interface ClienteInactivo {
  id: string
  nombre: string
  telefono: string
  ultimaVisita: string
  diasSinVenir: number
  visitas: number
}

export interface ClientesInactivos {
  barberiaNombre: string
  dias: number
  clientes: ClienteInactivo[]
}