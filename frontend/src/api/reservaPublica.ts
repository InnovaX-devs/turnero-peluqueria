import { api } from './client'
import type {
  EspecialidadPublica,
  NegocioPublico,
  ProfesionalPublico,
  CrearReservaRequest,
  DisponibilidadDia,
  ReservaCreada,
  ServicioPublico,
} from '../types/reserva'

export async function obtenerNegocioPublico(slug: string): Promise<NegocioPublico> {
  const { data } = await api.get<NegocioPublico>(`/api/public/negocios/${encodeURIComponent(slug)}`)
  return data
}

/** Rubro salud: especialidades que se pueden reservar en esa sucursal. */
export async function obtenerEspecialidadesPublicas(slug: string, sucursalId: string): Promise<EspecialidadPublica[]> {
  const { data } = await api.get<EspecialidadPublica[]>(
    `/api/public/negocios/${encodeURIComponent(slug)}/especialidades`,
    { params: { sucursal: sucursalId } },
  )
  return data
}

/** Rubro salud: la prestación se elige después del profesional (o de "cualquiera" de la especialidad). */
export interface FiltroServicios {
  profesionalId?: string
  especialidadId?: string
}

/** Solo los servicios que se pueden reservar en esa sucursal (y, si se indica, con ese profesional o especialidad). */
export async function obtenerServiciosPublicos(
  slug: string,
  sucursalId: string,
  filtro: FiltroServicios = {},
): Promise<ServicioPublico[]> {
  const { data } = await api.get<ServicioPublico[]>(`/api/public/negocios/${encodeURIComponent(slug)}/servicios`, {
    params: {
      sucursal: sucursalId,
      ...(filtro.profesionalId ? { profesional: filtro.profesionalId } : {}),
      ...(filtro.especialidadId ? { especialidad: filtro.especialidadId } : {}),
    },
  })
  return data
}

/**
 * Profesionales de esa sucursal con su próximo día libre: los que hacen el servicio, o (rubro salud,
 * servicioId null) los que tienen la especialidad.
 */
export async function obtenerProfesionalesPublicos(
  slug: string,
  sucursalId: string,
  servicioId: string | null,
  especialidadId?: string | null,
): Promise<ProfesionalPublico[]> {
  const { data } = await api.get<ProfesionalPublico[]>(`/api/public/negocios/${encodeURIComponent(slug)}/profesionales`, {
    params: {
      sucursal: sucursalId,
      ...(servicioId ? { servicio: servicioId } : {}),
      ...(especialidadId ? { especialidad: especialidadId } : {}),
    },
  })
  return data
}

interface ConsultaDisponibilidad {
  sucursalId: string
  servicioId: string
  /** null = cualquier barbero */
  profesionalId: string | null
  /** Rubro salud con "cualquier profesional": solo cuentan los de esta especialidad */
  especialidadId?: string | null
}

function paramsDe({ sucursalId, servicioId, profesionalId, especialidadId }: ConsultaDisponibilidad) {
  return {
    sucursal: sucursalId,
    servicio: servicioId,
    ...(profesionalId ? { profesional: profesionalId } : {}),
    ...(!profesionalId && especialidadId ? { especialidad: especialidadId } : {}),
  }
}

/** Días (yyyy-mm-dd) del rango que tienen al menos un horario libre. */
export async function obtenerDiasDisponibles(
  consulta: ConsultaDisponibilidad,
  desde: string,
  hasta: string,
): Promise<string[]> {
  const { data } = await api.get<{ dias: string[] }>('/api/public/disponibilidad/dias', {
    params: { ...paramsDe(consulta), desde, hasta },
  })
  return data.dias
}

/** Horarios libres de un día. */
export async function obtenerDisponibilidad(consulta: ConsultaDisponibilidad, fecha: string): Promise<DisponibilidadDia> {
  const { data } = await api.get<DisponibilidadDia>('/api/public/disponibilidad', {
    params: { ...paramsDe(consulta), fecha },
  })
  return data
}

/** Crea el turno. Si el horario se ocupó en el medio, responde 409. */
export async function crearReserva(request: CrearReservaRequest): Promise<ReservaCreada> {
  const { data } = await api.post<ReservaCreada>('/api/public/reservas', request)
  return data
}
