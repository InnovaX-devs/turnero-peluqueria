export interface Profesional {
  id: string
  nombre: string
  apellido: string
  fotoUrl: string | null
  /** Rubro salud. null si no tiene */
  matricula: string | null
  activo: boolean
  sucursalIds: string[]
  servicioIds: string[]
  /** Rubro salud */
  especialidadIds: string[]
  usuarioEmail: string | null
}

export interface Especialidad {
  id: string
  nombre: string
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

export interface OpcionesProfesional {
  sucursales: SucursalOpcion[]
  servicios: ServicioOpcion[]
  /** Todas las del negocio (rubro salud) */
  especialidades: Especialidad[]
}

/** diaSemana: 1 = lunes ... 7 = domingo. Horas en formato "HH:mm". */
export interface Horario {
  sucursalId: string
  diaSemana: number
  horaInicio: string
  horaFin: string
}

/** Solo el email: la contraseña la elige el barbero desde el link que le llega por mail. */
export interface CrearUsuarioProfesional {
  email: string
}

export interface DatosProfesional {
  nombre: string
  apellido: string
  sucursalIds: string[]
  servicioIds: string[]
  /** Rubro salud. Sin mandar = no se toca; "" = se quita */
  matricula?: string
  /** Rubro salud. Sin mandar = no se tocan; [] = se quitan todas */
  especialidadIds?: string[]
}

export interface CrearProfesional extends DatosProfesional {
  usuario?: CrearUsuarioProfesional
}