import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { cambiarEstadoTurno, marcarLlegadaTurno } from '../../api/agenda'
import { formatearDni } from '../../lib/dni'
import { mensajeDeError } from '../../lib/errores'
import { Selector } from '../../components/Selector'
import type { CambiarEstadoTurno, MetodoPago, Turno } from '../../types/agenda'
import { ETIQUETA_TIPO_PAGO, type TipoPago } from '../../types/cobertura'
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
import { useTerminos } from '../terminos/useTerminos'
import { useAuth } from '../auth/useAuth'
import { NotasConsulta } from './NotasConsulta'

/** Estados a los que se puede pasar un turno desde el detalle (RESERVADA no es un destino). */
type AccionEstado = 'COMPLETADA' | 'AUSENTE' | 'CANCELADA'

const METODOS_PAGO: MetodoPago[] = ['EFECTIVO', 'TRANSFERENCIA', 'MERCADOPAGO', 'TARJETA']

export function TurnoDetalle({ turno, onCerrar }: { turno: Turno; onCerrar: () => void }) {
  const t = useTerminos()
  const { usuario } = useAuth()
  const queryClient = useQueryClient()

  // Arranca con lo que vino del listado y se actualiza al confirmar un cambio de estado,
  // así el modal refleja el resultado sin tener que cerrarlo.
  const [turnoActual, setTurnoActual] = useState(turno)
  const [accion, setAccion] = useState<AccionEstado | null>(null)
  const [precioInput, setPrecioInput] = useState(() => String(turno.precioFinal / 100))
  const [metodoPago, setMetodoPago] = useState<MetodoPago | ''>(turno.metodoPago ?? '')
  const [errorValidacion, setErrorValidacion] = useState<string | null>(null)
  // Obras sociales (issue #67, solo salud)
  const esSalud = t.rubro === 'SALUD'
  const [tipoPago, setTipoPago] = useState<TipoPago>(turno.tipoPago ?? 'PARTICULAR')
  const [copagoInput, setCopagoInput] = useState('')
  const [autorizacion, setAutorizacion] = useState(turno.numeroAutorizacion ?? '')

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
      // Si se canceló, la lista de espera puede tener un aviso nuevo
      queryClient.invalidateQueries({ queryKey: ['lista-espera'] })
      setTurnoActual(actualizado)
      setAccion(null)
      setErrorValidacion(null)
    },
  })

  // Sala de espera (issue #71): marcar que el cliente llegó, o desmarcarlo si se tocó por error.
  const llegada = useMutation({
    mutationFn: (llego: boolean) => marcarLlegadaTurno(turnoActual.id, llego),
    onSuccess: (actualizado) => {
      queryClient.invalidateQueries({ queryKey: ['agenda'] })
      setTurnoActual(actualizado)
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

  /** Obra social con la que se puede cobrar: la del turno o, si se reservó como particular, la del paciente. */
  const coberturaCobrable = turnoActual.coberturaNombre ?? turnoActual.clienteCoberturaNombre ?? null
  const eligeTipoPago = esSalud && coberturaCobrable !== null
  const pagaAlgo = !eligeTipoPago || tipoPago !== 'COBERTURA'

  function confirmarCompletar() {
    const precio = Number(precioInput.replace(',', '.'))
    if (precioInput.trim() === '' || Number.isNaN(precio) || precio < 0) {
      setErrorValidacion('Ingresá un precio válido.')
      return
    }
    if (!eligeTipoPago) {
      if (!metodoPago) {
        setErrorValidacion('Elegí cómo pagó.')
        return
      }
      setErrorValidacion(null)
      cambiar.mutate({ estado: 'COMPLETADA', precioFinal: Math.round(precio * 100), metodoPago })
      return
    }

    let copago: number | undefined
    if (tipoPago === 'COBERTURA_CON_COPAGO') {
      copago = Number(copagoInput.replace(',', '.'))
      if (copagoInput.trim() === '' || Number.isNaN(copago) || copago < 0) {
        setErrorValidacion('Ingresá cuánto pagó de copago.')
        return
      }
    }
    if (pagaAlgo && !metodoPago) {
      setErrorValidacion(tipoPago === 'COBERTURA_CON_COPAGO' ? 'Elegí cómo pagó el copago.' : 'Elegí cómo pagó.')
      return
    }
    setErrorValidacion(null)
    cambiar.mutate({
      estado: 'COMPLETADA',
      precioFinal: Math.round(precio * 100),
      tipoPago,
      ...(pagaAlgo && metodoPago ? { metodoPago } : {}),
      ...(copago !== undefined ? { copago: Math.round(copago * 100) } : {}),
      ...(tipoPago !== 'PARTICULAR' && autorizacion.trim() ? { numeroAutorizacion: autorizacion.trim() } : {}),
    })
  }

  function generarMensajeWhatsApp() {
    const nombre = turnoActual.clienteNombre ?? t.cliente.s
    const fecha = formatearFechaLarga(fechaISO)
    const horaInicio = formatearHora(turnoActual.inicio)
    const horaFin = formatearHora(turnoActual.fin)
    const precio = formatearPrecio(turnoActual.precioFinal)

    const mensaje = `¡Hola, ${nombre}! Te compartimos el detalle de tu turno:

    *Detalle del turno*

    ${t.servicio.S}: ${turnoActual.servicioNombre}
    Fecha: ${fecha}
    Horario: ${horaInicio} a ${horaFin}
    ${t.profesional.S}: ${turnoActual.profesionalNombre}
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
          {turnoActual.clienteNombre ?? `Sin ${t.cliente.s}`}
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
          {turnoActual.clienteDni && (
            <div>
              <dt>DNI</dt>
              <dd>{formatearDni(turnoActual.clienteDni)}</dd>
            </div>
          )}
          <div>
            <dt>{t.servicio.S}</dt>
            <dd>{turnoActual.servicioNombre}</dd>
          </div>
          <div>
            <dt>{t.profesional.S}</dt>
            <dd>{turnoActual.profesionalNombre}</dd>
          </div>
          {turnoActual.consultorioNombre && (
            <div>
              <dt>Consultorio</dt>
              <dd>{turnoActual.consultorioNombre}</dd>
            </div>
          )}
          <div>
            <dt>Estado</dt>
            <dd>
              {ETIQUETAS_ESTADO[turnoActual.estado]}
              {turnoActual.sobreturno && ' · Sobreturno'}
            </dd>
          </div>
          {esSalud && (
            <div>
              <dt>Obra social</dt>
              <dd>
                {turnoActual.coberturaNombre
                  ? [
                      turnoActual.coberturaNombre,
                      turnoActual.clienteNumeroAfiliado && `Afiliado ${turnoActual.clienteNumeroAfiliado}`,
                      turnoActual.clientePlan && `Plan ${turnoActual.clientePlan}`,
                    ]
                      .filter(Boolean)
                      .join(' · ')
                  : turnoActual.clienteCoberturaNombre
                    ? `Particular (tiene ${turnoActual.clienteCoberturaNombre})`
                    : 'Particular'}
              </dd>
            </div>
          )}
          <div>
            <dt>Pago</dt>
            <dd>
              {ETIQUETAS_PAGO[turnoActual.estadoPago]}
              {esSalud && turnoActual.estado === 'COMPLETADA' && turnoActual.tipoPago && turnoActual.tipoPago !== 'PARTICULAR' &&
                ` · ${ETIQUETA_TIPO_PAGO[turnoActual.tipoPago]}`}
              {turnoActual.copago != null && ` (${formatearPrecio(turnoActual.copago)})`}
              {turnoActual.metodoPago && ` · ${ETIQUETAS_METODO_PAGO[turnoActual.metodoPago]}`}
              {turnoActual.numeroAutorizacion && ` · Autorización ${turnoActual.numeroAutorizacion}`}
            </dd>
          </div>
          <div>
            <dt>Precio</dt>
            <dd>{formatearPrecio(turnoActual.precioFinal)}</dd>
          </div>
          {turnoActual.estadoCambiadoPorEmail && turnoActual.estadoCambiadoEn && (
            <div>
              <dt>Actualizado</dt>
              <dd>
                {turnoActual.estadoCambiadoPorEmail} · {formatearFechaHora(turnoActual.estadoCambiadoEn)}
              </dd>
            </div>
          )}
        </dl>

        {/* Notas de consulta (issue #72): recepción no las ve ni las edita; el backend tampoco se las manda */}
        {usuario?.rol !== 'RECEPCION' && (
          <NotasConsulta
            turno={turnoActual}
            rubro={usuario?.rubro}
            onActualizado={(actualizado) => setTurnoActual(actualizado)}
          />
        )}

        {puedeCambiarEstado && (
          <div className="agenda-modal-llegada">
            {turnoActual.llegadaEn ? (
              <>
                <p>
                  <strong>En sala</strong> desde las {formatearHora(turnoActual.llegadaEn)}
                </p>
                <button
                  type="button"
                  className="boton-secundario"
                  disabled={llegada.isPending}
                  onClick={() => llegada.mutate(false)}
                >
                  Todavía no llegó
                </button>
              </>
            ) : (
              <>
                <p>¿Ya llegó?</p>
                <button
                  type="button"
                  className="boton-principal"
                  disabled={llegada.isPending}
                  onClick={() => llegada.mutate(true)}
                >
                  {llegada.isPending ? 'Guardando…' : 'Llegó'}
                </button>
              </>
            )}
            {llegada.isError && (
              <p className="agenda-modal-error" role="alert">
                {mensajeDeError(llegada.error)}
              </p>
            )}
          </div>
        )}

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
                {eligeTipoPago && (
                  <label className="agenda-modal-campo">
                    <span className="agenda-modal-campo-etiqueta">Cómo paga</span>
                    <Selector
                      etiqueta="Cómo paga"
                      valor={tipoPago}
                      onChange={(v) => setTipoPago(v as TipoPago)}
                      opciones={[
                        { valor: 'COBERTURA', etiqueta: `${coberturaCobrable} (sin cargo)` },
                        { valor: 'COBERTURA_CON_COPAGO', etiqueta: `${coberturaCobrable} con copago` },
                        { valor: 'PARTICULAR', etiqueta: 'Particular' },
                      ]}
                    />
                  </label>
                )}
                {eligeTipoPago && tipoPago === 'COBERTURA_CON_COPAGO' && (
                  <label className="agenda-modal-campo">
                    <span className="agenda-modal-campo-etiqueta">Copago</span>
                    <input
                      type="number"
                      inputMode="decimal"
                      min={0}
                      step="1"
                      value={copagoInput}
                      onChange={(e) => setCopagoInput(e.target.value)}
                    />
                  </label>
                )}
                {eligeTipoPago && tipoPago !== 'PARTICULAR' && (
                  <label className="agenda-modal-campo">
                    <span className="agenda-modal-campo-etiqueta">Nº de autorización (opcional)</span>
                    <input
                      type="text"
                      maxLength={60}
                      value={autorizacion}
                      onChange={(e) => setAutorizacion(e.target.value)}
                    />
                  </label>
                )}
                {pagaAlgo && (
                  <label className="agenda-modal-campo">
                    <span className="agenda-modal-campo-etiqueta">
                      {eligeTipoPago && tipoPago === 'COBERTURA_CON_COPAGO' ? 'Cómo pagó el copago' : 'Método de pago'}
                    </span>
                    <Selector
                      etiqueta="Método de pago"
                      valor={metodoPago}
                      onChange={(v) => setMetodoPago(v as MetodoPago)}
                      opciones={METODOS_PAGO.map((m) => ({ valor: m, etiqueta: ETIQUETAS_METODO_PAGO[m] }))}
                    />
                  </label>
                )}

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
            Ver ficha {t.cliente.del}
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