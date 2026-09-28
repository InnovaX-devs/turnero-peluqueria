import { formatearPrecio } from '../../lib/formato'
import type { ReservaCreada, SucursalPublica } from '../../types/reserva'

interface Props {
  reserva: ReservaCreada
  sucursal: SucursalPublica | undefined
  onOtroTurno: () => void
}

export function ReservaConfirmada({ reserva, sucursal, onOtroTurno }: Props) {
  const inicio = new Date(reserva.inicio)
  const cuando = inicio.toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' })
  const hora = inicio.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false })

  return (
    <section className="reserva-confirmada" aria-live="polite">
      <div className="reserva-confirmada-icono" aria-hidden="true">✓</div>
      <h2>¡Listo, tu turno está reservado!</h2>
      <p className="reserva-confirmada-sub">Te esperamos. Si no podés venir, avisale a la barbería.</p>

      <dl className="reserva-resumen">
        <div>
          <dt>Servicio</dt>
          <dd>{reserva.servicioNombre}</dd>
        </div>
        <div>
          <dt>Barbero</dt>
          <dd>{reserva.barberoNombre}</dd>
        </div>
        <div>
          <dt>Cuándo</dt>
          <dd>{cuando} · {hora} hs</dd>
        </div>
        {sucursal && (
          <div>
            <dt>Dónde</dt>
            <dd>{sucursal.nombre} · {sucursal.direccion}, {sucursal.ciudad}</dd>
          </div>
        )}
        <div className="reserva-resumen-total">
          <dt>Total (se paga en la barbería)</dt>
          <dd>{formatearPrecio(reserva.precioFinal)}</dd>
        </div>
      </dl>

      <button type="button" className="boton-secundario reserva-otro-turno" onClick={onOtroTurno}>
        Sacar otro turno
      </button>
    </section>
  )
}
