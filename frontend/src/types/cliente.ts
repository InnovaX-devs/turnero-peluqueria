import type { EstadoAtencion } from './agenda'

export interface ClienteListado {
  id: string
  nombre: string
  telefono: string
  tieneNotas: boolean
  /** Último turno completado (ISO). null si nunca vino. */
  ultimaVisita: string | null
}

export type OrdenClientes = 'nombre' | 'ultimaVisita'

export interface PaginaClientes {
  clientes: ClienteListado[]
  /** Cuántos clientes coinciden con la búsqueda, en total */
  total: number
  pagina: number
  tamano: number
  hayMas: boolean
}

export interface AtencionHistorial {
  id: string
  inicio: string
  estado: EstadoAtencion
  barberoNombre: string
  servicioNombre: string
  /** En centavos */
  precioFinal: number
}

export interface FichaCliente {
  id: string
  nombre: string
  telefono: string
  notas: string | null
  creadoEn: string
  ausencias: number
  /** De la más reciente a la más antigua. */
  historial: AtencionHistorial[]
}