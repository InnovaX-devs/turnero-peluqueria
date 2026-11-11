export type MetodoPago = 'EFECTIVO' | 'TRANSFERENCIA' | 'MERCADOPAGO' | 'TARJETA'

export interface ServicioOpcion {
  id: string
  nombre: string
  duracionMin: number
  /** En centavos */
  precio: number
}

export interface ProfesionalOpcion {
  id: string
  nombre: string
  sucursalIds: string[]
}

export interface SucursalOpcion {
  id: string
  nombre: string
}

export interface OpcionesRegistro {
  zonaHoraria: string
  profesionalPropioId: string | null
  servicios: ServicioOpcion[]
  profesionales: ProfesionalOpcion[]
  sucursales: SucursalOpcion[]
}

export interface ClienteResumen {
  id: string
  nombre: string
  telefono: string
  /** Solo en salud; null en los demás rubros. */
  dni: string | null
}

export interface DatosNuevoCliente {
  nombre: string
  /** E.164, ej. +5493584111111 */
  telefono: string
  /** Obligatorio en salud. */
  dni?: string
}

export interface RegistrarCorteDatos {
  clienteId: string | null
  servicioId: string
  profesionalId: string | null
  sucursalId: string | null
  /** En centavos */
  precioFinal: number
  metodoPago: MetodoPago
  notas: string | null
  /** null = ahora */
  fecha: string | null
  hora: string | null
}