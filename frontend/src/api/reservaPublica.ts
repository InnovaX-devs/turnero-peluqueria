import { api } from './client'
import type {
  BarberiaPublica,
  BarberoPublico,
  CrearReservaRequest,
  DisponibilidadDia,
  ReservaCreada,
  ServicioPublico,
} from '../types/reserva'

export async function obtenerBarberiaPublica(slug: string): Promise<BarberiaPublica> {
  const { data } = await api.get<BarberiaPublica>(`/api/public/barberias/${encodeURIComponent(slug)}`)
  return data
}

/** Solo los servicios que se pueden reservar en esa sucursal. */
export async function obtenerServiciosPublicos(slug: string, sucursalId: string): Promise<ServicioPublico[]> {
  const { data } = await api.get<ServicioPublico[]>(`/api/public/barberias/${encodeURIComponent(slug)}/servicios`, {
    params: { sucursal: sucursalId },
  })
  return data
}

/** Barberos que hacen ese servicio en esa sucursal, con su próximo día libre. */
export async function obtenerBarberosPublicos(
  slug: string,
  sucursalId: string,
  servicioId: string,
): Promise<BarberoPublico[]> {
  const { data } = await api.get<BarberoPublico[]>(`/api/public/barberias/${encodeURIComponent(slug)}/barberos`, {
    params: { sucursal: sucursalId, servicio: servicioId },
  })
  return data
}

interface ConsultaDisponibilidad {
  sucursalId: string
  servicioId: string
  /** null = cualquier barbero */
  barberoId: string | null
}

function paramsDe({ sucursalId, servicioId, barberoId }: ConsultaDisponibilidad) {
  return { sucursal: sucursalId, servicio: servicioId, ...(barberoId ? { barbero: barberoId } : {}) }
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
