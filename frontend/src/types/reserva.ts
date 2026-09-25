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