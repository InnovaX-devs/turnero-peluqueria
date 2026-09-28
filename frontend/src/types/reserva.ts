export interface SucursalPublica {
  id: string
  nombre: string
  direccion: string
  ciudad: string
  telefono: string | null
}

export interface BarberiaPublica {
  nombre: string
  slug: string
  logoUrl: string | null
  reservasOnline: boolean
  sucursales: SucursalPublica[]
}
export interface ServicioPublico {
  id: string
  nombre: string
  duracionMin: number
  /** En centavos */
  precio: number
}

export interface BarberoPublico {
  id: string
  nombre: string
  apellido: string
  fotoUrl: string | null
  /** yyyy-mm-dd, o null si no tiene horarios libres dentro de la anticipación máxima */
  proximoDiaDisponible: string | null
}
