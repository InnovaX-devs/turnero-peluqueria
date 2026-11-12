import type { Rubro } from '../lib/terminos'
import type { Sexo } from './cliente'

export interface SucursalPublica {
  id: string
  nombre: string
  direccion: string
  ciudad: string
  telefono: string | null
}

export interface NegocioPublico {
  nombre: string
  slug: string
  logoUrl: string | null
  /** Cómo se nombran las cosas en la página pública (barbero / profesional, cliente / paciente...) */
  rubro: Rubro
  reservasOnline: boolean
  /** Hasta cuántos días adelante se puede reservar */
  anticipacionMaximaDias: number
  /**
   * Rubro salud con especialidades cargadas: la reserva arranca por la especialidad
   * (especialidad → profesional o cualquiera → prestación → horario).
   */
  reservaPorEspecialidad: boolean
  sucursales: SucursalPublica[]
}

export interface EspecialidadPublica {
  id: string
  nombre: string
}

export interface ServicioPublico {
  id: string
  nombre: string
  duracionMin: number
  /** En centavos */
  precio: number
}

export interface ProfesionalPublico {
  id: string
  nombre: string
  apellido: string
  fotoUrl: string | null
  /** Rubro salud: se muestra en la tarjeta. null si no tiene */
  matricula: string | null
  /** Nombres de sus especialidades (rubro salud), ordenados */
  especialidades: string[]
  /** yyyy-mm-dd, o null si no tiene horarios libres dentro de la anticipación máxima */
  proximoDiaDisponible: string | null
}

export interface SlotDisponible {
  /** "HH:mm" o "HH:mm:ss" según cómo lo serialice el backend */
  hora: string
  /** Barberos libres en ese horario (con "cualquiera" puede haber varios) */
  profesionales: string[]
}

export interface DisponibilidadDia {
  fecha: string
  slots: SlotDisponible[]
}

export interface CrearReservaRequest {
  sucursalId: string
  servicioId: string
  profesionalId: string
  fecha: string
  hora: string
  clienteNombre: string
  /** E.164, ej. +5493584111111 */
  clienteTelefono: string
  /** Obligatorio en salud (solo números). En los demás rubros no se manda. */
  clienteDni?: string
  /** "AAAA-MM-DD". Solo salud, opcional. */
  clienteFechaNacimiento?: string
  /** Solo salud, opcional. */
  clienteSexo?: Sexo
  notas: string | null
}

export interface ReservaCreada {
  id: string
  inicio: string
  fin: string
  estado: string
  profesionalId: string
  profesionalNombre: string
  servicioId: string
  servicioNombre: string
  /** En centavos */
  precioFinal: number
  tokenGestion: string
}
