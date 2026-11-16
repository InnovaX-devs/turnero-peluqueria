import type { EstadoAtencion } from './agenda'
import type { TipoPago } from './cobertura'

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
  /** Issue #67. Puede faltar si el backend es anterior */
  tipoPago?: TipoPago
  /** Obra social con la que se atendió. null = particular */
  coberturaNombre?: string | null
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
  /** Obra social / prepaga (issue #67, salud). null = particular */
  coberturaId?: string | null
  coberturaNombre?: string | null
  numeroAfiliado?: string | null
  plan?: string | null
}

export interface DatosCoberturaCliente {
  /** null = particular */
  coberturaId: string | null
  numeroAfiliado: string
  plan: string
}