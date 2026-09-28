import { useQuery } from '@tanstack/react-query'
import { obtenerBarberosPublicos } from '../../api/reservaPublica'
import { mensajeDeError } from '../../lib/errores'
import { formatearProximoDia } from '../../lib/formato'
import type { BarberoPublico } from '../../types/reserva'
import { BARBERO_CUALQUIERA } from './flujo'

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

function iniciales(b: BarberoPublico): string {
  return `${b.nombre.charAt(0)}${b.apellido.charAt(0)}`.toUpperCase()
}

export function PasoBarbero({ slug, sucursalId, servicioId, elegido, onElegir }: Props) {
  const { data: barberos, isPending, error, refetch } = useQuery({
    queryKey: ['barberos-publicos', slug, sucursalId, servicioId],
    queryFn: () => obtenerBarberosPublicos(slug, sucursalId, servicioId),
    staleTime: 60 * 1000, // la disponibilidad cambia: se refresca más seguido que los servicios
  })

  if (isPending) {
    return <p className="reserva-cargando">Cargando barberos…</p>
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

  const conTurnos = barberos.filter((b) => b.proximoDiaDisponible !== null)

  if (conTurnos.length === 0) {
    return (
      <p className="reserva-vacio">
        Por ahora no hay turnos disponibles para este servicio. Probá con otro servicio o volvé más tarde.
      </p>
    )
  }

  // "Cualquiera" está disponible desde el primer día en que alguno de los barberos lo está
  const primerDia = conTurnos
    .map((b) => b.proximoDiaDisponible as string)
    .sort()[0]

  return (
    <ul className="reserva-barberos">
      {conTurnos.length > 1 && (
        <li>
          <button
            type="button"
            className="reserva-barbero"
            aria-pressed={elegido === BARBERO_CUALQUIERA}
            onClick={() => onElegir(BARBERO_CUALQUIERA)}
          >
            <span className="reserva-avatar reserva-avatar-cualquiera" aria-hidden="true">✦</span>
            <strong>Cualquier barbero</strong>
            <span className="reserva-barbero-dispo">{disponibilidad(primerDia)}</span>
          </button>
        </li>
      )}
      {barberos.map((b) => {
        const sinTurnos = b.proximoDiaDisponible === null
        return (
          <li key={b.id}>
            <button
              type="button"
              className="reserva-barbero"
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
              <span className="reserva-barbero-dispo">{disponibilidad(b.proximoDiaDisponible)}</span>
            </button>
          </li>
        )
      })}
    </ul>
  )
}
