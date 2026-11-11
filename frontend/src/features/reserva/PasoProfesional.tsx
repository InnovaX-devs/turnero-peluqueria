import { useQuery } from '@tanstack/react-query'
import { obtenerProfesionalesPublicos } from '../../api/reservaPublica'
import { mensajeDeError } from '../../lib/errores'
import { formatearProximoDia } from '../../lib/formato'
import type { ProfesionalPublico } from '../../types/reserva'
import { PROFESIONAL_CUALQUIERA } from './flujo'
import { useTerminos } from '../terminos/useTerminos'

interface Props {
  slug: string
  sucursalId: string
  servicioId: string
  elegido: string | null
  onElegir: (id: string) => void
}

function disponibilidad(fecha: string | null): string {
  return fecha ? `Disponible ${formatearProximoDia(fecha)}` : 'Sin turnos por ahora'
}

function iniciales(b: ProfesionalPublico): string {
  return `${b.nombre.charAt(0)}${b.apellido.charAt(0)}`.toUpperCase()
}

export function PasoProfesional({ slug, sucursalId, servicioId, elegido, onElegir }: Props) {
  const t = useTerminos()
  const { data: profesionales, isPending, error, refetch } = useQuery({
    queryKey: ['profesionales-publicos', slug, sucursalId, servicioId],
    queryFn: () => obtenerProfesionalesPublicos(slug, sucursalId, servicioId),
    staleTime: 60 * 1000, // la disponibilidad cambia: se refresca más seguido que los servicios
  })

  if (isPending) {
    return <p className="reserva-cargando">Cargando {t.profesional.p}…</p>
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

  const conTurnos = profesionales.filter((b) => b.proximoDiaDisponible !== null)

  if (conTurnos.length === 0) {
    return (
      <p className="reserva-vacio">
        Por ahora no hay turnos disponibles para {t.servicio.este}. Probá con {t.servicio.otro} o volvé más tarde.
      </p>
    )
  }

  // "Cualquiera" está disponible desde el primer día en que alguno de los profesionales lo está
  const primerDia = conTurnos
    .map((b) => b.proximoDiaDisponible as string)
    .sort()[0]

  return (
    <ul className="reserva-profesionales">
      {conTurnos.length > 1 && (
        <li>
          <button
            type="button"
            className="reserva-profesional"
            aria-pressed={elegido === PROFESIONAL_CUALQUIERA}
            onClick={() => onElegir(PROFESIONAL_CUALQUIERA)}
          >
            <span className="reserva-avatar reserva-avatar-cualquiera" aria-hidden="true">✦</span>
            <strong>Cualquier {t.profesional.s}</strong>
            <span className="reserva-profesional-dispo">{disponibilidad(primerDia)}</span>
          </button>
        </li>
      )}
      {profesionales.map((b) => {
        const sinTurnos = b.proximoDiaDisponible === null
        return (
          <li key={b.id}>
            <button
              type="button"
              className="reserva-profesional"
              aria-pressed={b.id === elegido}
              disabled={sinTurnos}
              onClick={() => onElegir(b.id)}
            >
              {b.fotoUrl ? (
                <img className="reserva-avatar" src={b.fotoUrl} alt="" />
              ) : (
                <span className="reserva-avatar" aria-hidden="true">{iniciales(b)}</span>
              )}
              <strong>{b.nombre} {b.apellido}</strong>
              <span className="reserva-profesional-dispo">{disponibilidad(b.proximoDiaDisponible)}</span>
            </button>
          </li>
        )
      })}
    </ul>
  )
}
