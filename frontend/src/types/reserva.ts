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
  /** Hasta cuántos días adelante se puede reservar */
  anticipacionMaximaDias: number
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

export interface SlotDisponible {
  /** "HH:mm" o "HH:mm:ss" según cómo lo serialice el backend */
  hora: string
  /** Barberos libres en ese horario (con "cualquiera" puede haber varios) */
  barberos: string[]
}

export interface DisponibilidadDia {
  fecha: string
  slots: SlotDisponible[]
}

export interface CrearReservaRequest {
  sucursalId: string
  servicioId: string
  barberoId: string
  fecha: string
  hora: string
  clienteNombre: string
  /** E.164, ej. +5493584111111 */
  clienteTelefono: string
  notas: string | null
}

export interface ReservaCreada {
  id: string
  inicio: string
  fin: string
  estado: string
  barberoId: string
  barberoNombre: string
  servicioId: string
  servicioNombre: string
  /** En centavos */
  precioFinal: number
  tokenGestion: string
}
