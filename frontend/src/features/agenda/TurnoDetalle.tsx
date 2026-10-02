import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { cambiarEstadoTurno } from '../../api/agenda'
import { mensajeDeError } from '../../lib/errores'
import type { CambiarEstadoTurno, MetodoPago, Turno } from '../../types/agenda'
import {
  ETIQUETAS_ESTADO,
  ETIQUETAS_METODO_PAGO,
  ETIQUETAS_PAGO,
  formatearFechaHora,
  formatearFechaISO,
  formatearFechaLarga,
  formatearHora,
  formatearPrecio,
  linkWhatsApp,
} from './agenda'

/** Estados a los que se puede pasar un turno desde el detalle (RESERVADA no es un destino). */
type AccionEstado = 'COMPLETADA' | 'AUSENTE' | 'CANCELADA'

const METODOS_PAGO: MetodoPago[] = ['EFECTIVO', 'TRANSFERENCIA', 'MERCADOPAGO', 'TARJETA']

export function TurnoDetalle({ turno, onCerrar }: { turno: Turno; onCerrar: () => void }) {
  const queryClient = useQueryClient()

  // Arranca con lo que vino del listado y se actualiza al confirmar un cambio de estado,
  // así el modal refleja el resultado sin tener que cerrarlo.
  const [turnoActual, setTurnoActual] = useState(turno)
  const [accion, setAccion] = useState<AccionEstado | null>(null)
  const [precioInput, setPrecioInput] = useState(() => String(turno.precioFinal / 100))
  const [metodoPago, setMetodoPago] = useState<MetodoPago | ''>(turno.metodoPago ?? '')
  const [errorValidacion, setErrorValidacion] = useState<string | null>(null)

  // Cerrar con Escape, como cualquier modal
  useEffect(() => {
    function alTeclear(e: KeyboardEvent) {
      if (e.key === 'Escape') onCerrar()
    }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [onCerrar])

  const cambiar = useMutation({
    mutationFn: (datos: CambiarEstadoTurno) => cambiarEstadoTurno(turnoActual.id, datos),
    onSuccess: (actualizado) => {
      // La agenda ya cargada puede tener este turno con el estado viejo: se refresca en segundo plano.
      queryClient.invalidateQueries({ queryKey: ['agenda'] })
      setTurnoActual(actualizado)
      setAccion(null)
      setErrorValidacion(null)
    },
  })

  const fechaISO = formatearFechaISO(new Date(turnoActual.inicio))
  const puedeCambiarEstado = turnoActual.estado === 'RESERVADA'

  function abrirAccion(nueva: AccionEstado) {
    setAccion(nueva)
    setErrorValidacion(null)
    cambiar.reset()
  }

  function confirmarSimple(estado: 'AUSENTE' | 'CANCELADA') {
    cambiar.mutate({ estado })
  }

  function confirmarCompletar() {
    const precio = Number(precioInput.replace(',', '.'))
    if (precioInput.trim() === '' || Number.isNaN(precio) || precio < 0) {
      setErrorValidacion('Ingresá un precio válido.')
      return
    }
    if (!metodoPago) {
      setErrorValidacion('Elegí cómo pagó.')
      return
    }
    setErrorValidacion(null)
    cambiar.mutate({ estado: 'COMPLETADA', precioFinal: Math.round(precio * 100), metodoPago })
  }

  function generarMensajeWhatsApp() {
    const nombre = turnoActual.clienteNombre ?? 'cliente'
    const fecha = formatearFechaLarga(fechaISO)
    const horaInicio = formatearHora(turnoActual.inicio)
    const horaFin = formatearHora(turnoActual.fin)
    const precio = formatearPrecio(turnoActual.precioFinal)

    const mensaje = `¡Hola, ${nombre}! Te compartimos el detalle de tu turno:

    *Detalle del turno*

    Servicio: ${turnoActual.servicioNombre}
    Fecha: ${fecha}
    Horario: ${horaInicio} a ${horaFin}
    Profesional: ${turnoActual.barberoNombre}
    Precio: ${precio}

    ¡Muchas gracias por elegirnos!`

    return `${linkWhatsApp(turnoActual.clienteTelefono!)}?text=${encodeURIComponent(mensaje)}`
  }

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
          {turnoActual.clienteNombre ?? 'Sin cliente'}
        </h2>
        <p className="agenda-modal-fecha">
          {formatearFechaLarga(fechaISO)} · {formatearHora(turnoActual.inicio)}–{formatearHora(turnoActual.fin)}
        </p>

        {turnoActual.clienteNotas && (
          <div className="agenda-modal-nota-cliente">
            <span className="agenda-modal-nota-cliente-etiqueta">Nota del cliente</span>
            <p>{turnoActual.clienteNotas}</p>
          </div>
        )}

        <dl className="agenda-modal-detalle">
          <div>
            <dt>Servicio</dt>
            <dd>{turnoActual.servicioNombre}</dd>
          </div>
          <div>
            <dt>Barbero</dt>
            <dd>{turnoActual.barberoNombre}</dd>
          </div>
          <div>
            <dt>Estado</dt>
            <dd>{ETIQUETAS_ESTADO[turnoActual.estado]}</dd>
          </div>
          <div>
            <dt>Pago</dt>
            <dd>
              {ETIQUETAS_PAGO[turnoActual.estadoPago]}
              {turnoActual.metodoPago && ` · ${ETIQUETAS_METODO_PAGO[turnoActual.metodoPago]}`}
            </dd>
          </div>
          <div>
            <dt>Precio</dt>
            <dd>{formatearPrecio(turnoActual.precioFinal)}</dd>
          </div>
          {turnoActual.notas && (
            <div>
              <dt>Notas del turno</dt>
              <dd>{turnoActual.notas}</dd>
            </div>
          )}
          {turnoActual.estadoCambiadoPorEmail && turnoActual.estadoCambiadoEn && (
            <div>
              <dt>Actualizado</dt>
              <dd>
                {turnoActual.estadoCambiadoPorEmail} · {formatearFechaHora(turnoActual.estadoCambiadoEn)}
              </dd>
            </div>
          )}
        </dl>

        {puedeCambiarEstado && (
          <div className="agenda-modal-estado">
            {accion === null && (
              <div className="agenda-modal-acciones">
                <button type="button" className="boton-secundario" onClick={() => abrirAccion('COMPLETADA')}>
                  Marcar completado
                </button>
                <button type="button" className="boton-secundario" onClick={() => abrirAccion('AUSENTE')}>
                  Marcar ausente
                </button>
                <button
                  type="button"
                  className="boton-secundario agenda-modal-accion-cancelar"
                  onClick={() => abrirAccion('CANCELADA')}
                >
                  Cancelar turno
                </button>
              </div>
            )}

            {accion === 'COMPLETADA' && (
              <div className="agenda-modal-formulario">
                <label className="agenda-modal-campo">
                  <span className="agenda-modal-campo-etiqueta">Precio final</span>
                  <input
                    type="number"
                    inputMode="decimal"
                    min={0}
                    step="1"
                    value={precioInput}
                    onChange={(e) => setPrecioInput(e.target.value)}
                  />
                </label>
                <label className="agenda-modal-campo">
                  <span className="agenda-modal-campo-etiqueta">Método de pago</span>
                  <select value={metodoPago} onChange={(e) => setMetodoPago(e.target.value as MetodoPago)}>
                    <option value="" disabled>
                      Elegí una opción
                    </option>
                    {METODOS_PAGO.map((m) => (
                      <option key={m} value={m}>
                        {ETIQUETAS_METODO_PAGO[m]}
                      </option>
                    ))}
                  </select>
                </label>

                {(errorValidacion || cambiar.isError) && (
                  <p className="agenda-modal-error" role="alert">
                    {errorValidacion ?? mensajeDeError(cambiar.error)}
                  </p>
                )}

                <div className="agenda-modal-confirmar">
                  <button type="button" className="boton-secundario" onClick={() => setAccion(null)}>
                    Volver
                  </button>
                  <button
                    type="button"
                    className="boton-principal"
                    onClick={confirmarCompletar}
                    disabled={cambiar.isPending}
                  >
                    {cambiar.isPending ? 'Guardando…' : 'Confirmar'}
                  </button>
                </div>
              </div>
            )}

            {(accion === 'AUSENTE' || accion === 'CANCELADA') && (
              <div className="agenda-modal-formulario">
                <p className="agenda-modal-confirmacion-texto">
                  {accion === 'AUSENTE' ? '¿Marcar este turno como ausente?' : '¿Cancelar este turno?'}
                </p>

                {cambiar.isError && (
                  <p className="agenda-modal-error" role="alert">
                    {mensajeDeError(cambiar.error)}
                  </p>
                )}

                <div className="agenda-modal-confirmar">
                  <button type="button" className="boton-secundario" onClick={() => setAccion(null)}>
                    Volver
                  </button>
                  <button
                    type="button"
                    className="boton-principal"
                    onClick={() => confirmarSimple(accion)}
                    disabled={cambiar.isPending}
                  >
                    {cambiar.isPending ? 'Guardando…' : 'Confirmar'}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {turnoActual.clienteId && (
          <Link className="boton-secundario agenda-modal-ficha" to={`/panel/clientes/${turnoActual.clienteId}`}>
            Ver ficha del cliente
          </Link>
        )}

        {turnoActual.clienteTelefono ? (
          <a
            className="boton-principal agenda-modal-whatsapp"
            href={generarMensajeWhatsApp()}
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