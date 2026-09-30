import { useState } from 'react'
import { formatearPrecio } from '../../lib/formato'
import { linkDelTurno, linkWhatsAppParaGuardar } from '../turno/linkTurno'
import '../turno/turno.css'
import type { ReservaCreada, SucursalPublica } from '../../types/reserva'

interface Props {
  reserva: ReservaCreada
  sucursal: SucursalPublica | undefined
  barberiaNombre: string
  onOtroTurno: () => void
}

export function ReservaConfirmada({ reserva, sucursal, barberiaNombre, onOtroTurno }: Props) {
  const [copiado, setCopiado] = useState(false)
  const link = linkDelTurno(reserva.tokenGestion)

  async function copiar() {
    try {
      await navigator.clipboard.writeText(link)
      setCopiado(true)
    } catch {
      // Sin permiso para el portapapeles: el link igual queda a la vista para copiarlo a mano
      setCopiado(false)
    }
  }

  const inicio = new Date(reserva.inicio)
  const cuando = inicio.toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' })
  const hora = inicio.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false })

  return (
    <section className="reserva-confirmada" aria-live="polite">
      <div className="reserva-confirmada-icono" aria-hidden="true">✓</div>
      <h2>¡Listo, tu turno está reservado!</h2>
      <p className="reserva-confirmada-sub">Te esperamos. Si no podés venir, cancelalo así el horario queda libre.</p>

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

      <div className="reserva-link-turno">
        <p>
          <strong>Guardá este link:</strong> desde ahí podés cancelar o cambiar tu turno.
        </p>
        <p className="turno-limite">
          <a href={link}>{link}</a>
        </p>
        <div className="turno-acciones-fila">
          <button type="button" className="boton-secundario" onClick={copiar}>
            {copiado ? '¡Copiado!' : 'Copiar link'}
          </button>
          <a
            className="boton-secundario"
            href={linkWhatsAppParaGuardar(link, barberiaNombre)}
            target="_blank"
            rel="noreferrer"
          >
            Guardarlo en WhatsApp
          </a>
        </div>
      </div>

      <button type="button" className="boton-secundario reserva-otro-turno" onClick={onOtroTurno}>
        Sacar otro turno
      </button>
    </section>
  )
}
