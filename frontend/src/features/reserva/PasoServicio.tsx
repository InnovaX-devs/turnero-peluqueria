import { useQuery } from '@tanstack/react-query'
import { obtenerServiciosPublicos } from '../../api/reservaPublica'
import { mensajeDeError } from '../../lib/errores'
import { formatearDuracion, formatearPrecio } from '../../lib/formato'

interface Props {
  slug: string
  sucursalId: string
  elegido: string | null
  onElegir: (id: string) => void
}

export function PasoServicio({ slug, sucursalId, elegido, onElegir }: Props) {
  const { data: servicios, isPending, error, refetch } = useQuery({
    queryKey: ['servicios-publicos', slug, sucursalId],
    queryFn: () => obtenerServiciosPublicos(slug, sucursalId),
    staleTime: 5 * 60 * 1000,
  })

  if (isPending) {
    return <p className="reserva-cargando">Cargando servicios…</p>
  }

  if (error) {
    return (
      <div className="reserva-error" role="alert">
        <p>{mensajeDeError(error)}</p>
        <button type="button" className="boton-principal" onClick={() => refetch()}>
          Reintentar
        </button>
      </div>
    )
  }

  if (servicios.length === 0) {
    return <p className="reserva-vacio">Por ahora no hay servicios para reservar en esta sucursal.</p>
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
