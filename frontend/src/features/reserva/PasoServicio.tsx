import { useQuery } from '@tanstack/react-query'
import { obtenerServiciosPublicos, type FiltroServicios } from '../../api/reservaPublica'
import { mensajeDeError } from '../../lib/errores'
import { formatearDuracion, formatearPrecio } from '../../lib/formato'
import { useTerminos } from '../terminos/useTerminos'

interface Props {
  slug: string
  sucursalId: string
  /** Flujo por especialidad (rubro salud): las prestaciones del profesional elegido o de la especialidad */
  filtro?: FiltroServicios
  elegido: string | null
  onElegir: (id: string) => void
}

export function PasoServicio({ slug, sucursalId, filtro, elegido, onElegir }: Props) {
  const t = useTerminos()
  const { data: servicios, isPending, error, refetch } = useQuery({
    // Sin filtro, la misma clave que usan el resumen del horario y de los datos (comparten caché)
    queryKey: filtro
      ? ['servicios-publicos', slug, sucursalId, filtro.profesionalId ?? null, filtro.especialidadId ?? null]
      : ['servicios-publicos', slug, sucursalId],
    queryFn: () => obtenerServiciosPublicos(slug, sucursalId, filtro),
    staleTime: 5 * 60 * 1000,
  })

  if (isPending) {
    return <p className="reserva-cargando">Cargando {t.servicio.p}…</p>
  }

  if (error) {
    return (
      <div className="reserva-error reserva-error-inline" role="alert">
        <p>{mensajeDeError(error)}</p>
        <button type="button" className="boton-principal" onClick={() => refetch()}>
          Reintentar
        </button>
      </div>
    )
  }

  if (servicios.length === 0) {
    return (
      <p className="reserva-vacio">
        {filtro
          ? <>Por ahora no hay {t.servicio.p} para reservar con {filtro.profesionalId ? t.profesional.este : 'esta especialidad'}.</>
          : <>Por ahora no hay {t.servicio.p} para reservar en esta sucursal.</>}
      </p>
    )
  }

  return (
    <ul className="reserva-opciones">
      {servicios.map((s) => (
        <li key={s.id}>
          <button
            type="button"
            className="reserva-opcion reserva-opcion-servicio"
            aria-pressed={s.id === elegido}
            onClick={() => onElegir(s.id)}
          >
            <span className="reserva-servicio-info">
              <strong>{s.nombre}</strong>
              <span>Duración: {formatearDuracion(s.duracionMin)}</span>
            </span>
            <strong className="reserva-servicio-precio">{formatearPrecio(s.precio)}</strong>
          </button>
        </li>
      ))}
    </ul>
  )
}
