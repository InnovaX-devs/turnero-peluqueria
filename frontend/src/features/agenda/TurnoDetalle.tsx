import { useEffect } from 'react'
import type { Turno } from '../../types/agenda'
import {
  ETIQUETAS_ESTADO,
  ETIQUETAS_METODO_PAGO,
  ETIQUETAS_PAGO,
  formatearFechaISO,
  formatearFechaLarga,
  formatearHora,
  formatearPrecio,
  linkWhatsApp,
} from './agenda'

export function TurnoDetalle({ turno, onCerrar }: { turno: Turno; onCerrar: () => void }) {
  // Cerrar con Escape, como cualquier modal
  useEffect(() => {
    function alTeclear(e: KeyboardEvent) {
      if (e.key === 'Escape') onCerrar()
    }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [onCerrar])

  const fechaISO = formatearFechaISO(new Date(turno.inicio))

  return (
    <div className="agenda-modal-fondo" role="presentation" onClick={onCerrar}>
      <div
        className="agenda-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="agenda-modal-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <button type="button" className="agenda-modal-cerrar" onClick={onCerrar} aria-label="Cerrar">
          ✕
        </button>

        <h2 id="agenda-modal-titulo" className="agenda-modal-titulo">
          {turno.clienteNombre ?? 'Sin cliente'}
        </h2>
        <p className="agenda-modal-fecha">
          {formatearFechaLarga(fechaISO)} · {formatearHora(turno.inicio)}–{formatearHora(turno.fin)}
        </p>

        <dl className="agenda-modal-detalle">
          <div>
            <dt>Servicio</dt>
            <dd>{turno.servicioNombre}</dd>
          </div>
          <div>
            <dt>Barbero</dt>
            <dd>{turno.barberoNombre}</dd>
          </div>
          <div>
            <dt>Estado</dt>
            <dd>{ETIQUETAS_ESTADO[turno.estado]}</dd>
          </div>
          <div>
            <dt>Pago</dt>
            <dd>
              {ETIQUETAS_PAGO[turno.estadoPago]}
              {turno.metodoPago && ` · ${ETIQUETAS_METODO_PAGO[turno.metodoPago]}`}
            </dd>
          </div>
          <div>
            <dt>Precio</dt>
            <dd>{formatearPrecio(turno.precioFinal)}</dd>
          </div>
          {turno.notas && (
            <div>
              <dt>Notas</dt>
              <dd>{turno.notas}</dd>
            </div>
          )}
        </dl>

        {turno.clienteTelefono ? (
          <a
            className="boton-principal agenda-modal-whatsapp"
            href={linkWhatsApp(turno.clienteTelefono)}
            target="_blank"
            rel="noreferrer"
          >
            Escribir por WhatsApp
          </a>
        ) : (
          <p className="agenda-modal-sin-telefono">Este turno no tiene un teléfono de cliente cargado.</p>
        )}
      </div>
    </div>
  )
}