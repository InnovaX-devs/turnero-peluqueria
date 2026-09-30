export interface Barbero {
  id: string
  nombre: string
  apellido: string
  fotoUrl: string | null
  activo: boolean
  sucursalIds: string[]
  servicioIds: string[]
  usuarioEmail: string | null
}

export interface SucursalOpcion {
  id: string
  nombre: string
  direccion: string | null
}

export interface ServicioOpcion {
  id: string
  nombre: string
  duracionMin: number
}

export interface OpcionesBarbero {
  sucursales: SucursalOpcion[]
  servicios: ServicioOpcion[]
}

/** diaSemana: 1 = lunes ... 7 = domingo. Horas en formato "HH:mm". */
export interface Horario {
  sucursalId: string
  diaSemana: number
  horaInicio: string
  horaFin: string
}

export interface CrearUsuarioBarbero {
  email: string
  password: string
}

export interface DatosBarbero {
  nombre: string
  apellido: string
  sucursalIds: string[]
  servicioIds: string[]
}

export interface CrearBarbero extends DatosBarbero {
  usuario?: CrearUsuarioBarbero
}