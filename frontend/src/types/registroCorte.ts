export type MetodoPago = 'EFECTIVO' | 'TRANSFERENCIA' | 'MERCADOPAGO' | 'TARJETA'

export interface ServicioOpcion {
  id: string
  nombre: string
  duracionMin: number
  /** En centavos */
  precio: number
}

export interface BarberoOpcion {
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
  barberoPropioId: string | null
  servicios: ServicioOpcion[]
  barberos: BarberoOpcion[]
  sucursales: SucursalOpcion[]
}

export interface ClienteResumen {
  id: string
  nombre: string
  telefono: string
}

export interface RegistrarCorteDatos {
  clienteId: string | null
  servicioId: string
  barberoId: string | null
  sucursalId: string | null
  /** En centavos */
  precioFinal: number
  metodoPago: MetodoPago
  notas: string | null
  /** null = ahora */
  fecha: string | null
  hora: string | null
}