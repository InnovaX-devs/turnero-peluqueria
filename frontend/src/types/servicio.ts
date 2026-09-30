/** Servicio del ABM de admin. {@code precio} en centavos ($15.000 = 1500000). */
export interface Servicio {
  id: string
  nombre: string
  duracionMin: number
  precio: number
  activo: boolean
  orden: number
}

export interface DatosServicio {
  nombre: string
  duracionMin: number
  precio: number
}