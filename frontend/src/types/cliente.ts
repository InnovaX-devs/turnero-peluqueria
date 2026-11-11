import type { EstadoAtencion } from './agenda'

export type Sexo = 'FEMENINO' | 'MASCULINO' | 'OTRO'

export const ETIQUETA_SEXO: Record<Sexo, string> = {
  FEMENINO: 'Femenino',
  MASCULINO: 'Masculino',
  OTRO: 'Otro',
}

export interface ClienteListado {
  id: string
  nombre: string
  telefono: string
  /** Solo en salud; null en los demás rubros. */
  dni: string | null
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
  profesionalNombre: string
  servicioNombre: string
  /** En centavos */
  precioFinal: number
}

export interface FichaCliente {
  id: string
  nombre: string
  telefono: string
  /** Solo en salud; null en los demás rubros. */
  dni: string | null
  /** "AAAA-MM-DD" */
  fechaNacimiento: string | null
  sexo: Sexo | null
  notas: string | null
  creadoEn: string
  ausencias: number
  /** De la más reciente a la más antigua. */
  historial: AtencionHistorial[]
}