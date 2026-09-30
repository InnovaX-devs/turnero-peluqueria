import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { Link, useParams } from 'react-router-dom'
import { obtenerBarberiaPublica } from '../../api/reservaPublica'
import { cancelarTurno, obtenerTurno, reprogramarTurno } from '../../api/turno'
import { mensajeDeError } from '../../lib/errores'
import { fechaLarga, horaCorta } from '../../lib/fechas'
import { formatearDuracion, formatearPrecio } from '../../lib/formato'
import type { TurnoPublico } from '../../types/turno'
import { PasoHorario, type EleccionHorario } from '../reserva/PasoHorario'
import { linkWhatsAppSucursal, textoAnticipacion } from './linkTurno'
import '../reserva/reserva.css'
import './turno.css'

const TOKEN_VALIDO = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const esNoEncontrado = (e: unknown) => axios.isAxiosError(e) && e.response?.status === 404

function cuando(iso: string): string {
  const d = new Date(iso)
  const dia = d.toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' })
  const hora = d.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false })
  return `${dia} · ${hora} hs`
}

type Modo = 'ver' | 'confirmarCancelacion' | 'reprogramar'

export function GestionTurnoPage() {
  const { token = '' } = useParams()
  const tokenValido = TOKEN_VALIDO.test(token)
  const queryClient = useQueryClient()
  const [modo, setModo] = useState<Modo>('ver')
  const [eleccion, setEleccion] = useState<EleccionHorario | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  const { data: turno, isPending, error, refetch } = useQuery({
    queryKey: ['turno', token],
    queryFn: () => obtenerTurno(token),
    enabled: tokenValido,
    retry: (intentos, err) => !esNoEncontrado(err) && intentos < 1,
  })

  useEffect(() => {
    if (!turno) return
    const anterior = document.title
    document.title = `Tu turno en ${turno.barberia.nombre}`
    return () => {
      document.title = anterior
    }
  }, [turno])

  const actualizar = (t: TurnoPublico) => queryClient.setQueryData(['turno', token], t)

  const cancelar = useMutation({
    mutationFn: () => cancelarTurno(token),
    onSuccess: (t) => {
      actualizar(t)
      setModo('ver')
      setAviso('Listo, cancelaste el turno.')
    },
  })

  const reprogramar = useMutation({
    mutationFn: (e: EleccionHorario) => reprogramarTurno(token, e.fecha, e.hora),
    onSuccess: (t) => {
      actualizar(t)
      setModo('ver')
      setEleccion(null)
      setAviso(`Listo, tu turno ahora es el ${cuando(t.inicio)}.`)
    },
  })

  if (!tokenValido || esNoEncontrado(error)) {
    return (
      <main className="reserva reserva-no-encontrada">
        <p className="reserva-404">404</p>
        <h1>No encontramos este turno</h1>
        <p>Revisá que el link esté completo, tal como te llegó.</p>
      </main>
    )
  }

  if (isPending) return <div className="pantalla-centrada">Cargando…</div>

  if (error) {
    return (
      <main className="reserva">
        <div className="reserva-error" role="alert">
          <p>{mensajeDeError(error)}</p>
          <button type="button" className="boton-principal" onClick={() => refetch()}>Reintentar</button>
        </div>
      </main>
    )
  }

  if (modo === 'reprogramar') {
    return (
      <main className="reserva">
        <Encabezado turno={turno} />
        <Reprogramar
          turno={turno}
          eleccion={eleccion}
          onElegir={setEleccion}
          confirmando={reprogramar.isPending}
          error={reprogramar.error ? mensajeDeError(reprogramar.error) : null}
          onConfirmar={() => eleccion && reprogramar.mutate(eleccion)}
          onVolver={() => {
            setModo('ver')
            setEleccion(null)
            reprogramar.reset()
          }}
        />
      </main>
    )
  }

  return (
    <main className="reserva">
      <Encabezado turno={turno} />

      {aviso && (
        <p className="turno-aviso" role="status">
          {aviso}
        </p>
      )}

      <EstadoTurno turno={turno} />

      <dl className="reserva-resumen">
        <div>
          <dt>Servicio</dt>
          <dd>{turno.servicio.nombre} · {formatearDuracion(turno.servicio.duracionMin)}</dd>
        </div>
        <div>
          <dt>Barbero</dt>
          <dd>{turno.barbero.nombre}</dd>
        </div>
        <div>
          <dt>Cuándo</dt>
          <dd>{cuando(turno.inicio)}</dd>
        </div>
        <div>
          <dt>Dónde</dt>
          <dd>{turno.sucursal.nombre} · {turno.sucursal.direccion}, {turno.sucursal.ciudad}</dd>
        </div>
        <div className="reserva-resumen-total">
          <dt>Total (se paga en la barbería)</dt>
          <dd>{formatearPrecio(turno.precio)}</dd>
        </div>
      </dl>

      {turno.puedeCancelar && modo === 'ver' && (
        <div className="turno-acciones">
          {turno.puedeReprogramar && (
            <button type="button" className="boton-principal" onClick={() => { setAviso(null); setModo('reprogramar') }}>
              Cambiar día u horario
            </button>
          )}
          <button
            type="button"
            className="boton-secundario"
            onClick={() => { setAviso(null); cancelar.reset(); setModo('confirmarCancelacion') }}
          >
            Cancelar turno
          </button>
          <p className="turno-limite">
            Podés hacer cambios hasta el {cuando(turno.limiteModificacion)}.
            {!turno.puedeReprogramar && ' Para cambiar el horario, comunicate con la barbería.'}
          </p>
        </div>
      )}

      {modo === 'confirmarCancelacion' && (
        <div className="turno-confirmar" role="alertdialog" aria-labelledby="turno-confirmar-titulo">
          <p id="turno-confirmar-titulo">
            <strong>¿Seguro que querés cancelar el turno?</strong> El horario queda libre para otra persona.
          </p>
          {cancelar.error && <p className="turno-error" role="alert">{mensajeDeError(cancelar.error)}</p>}
          <div className="turno-acciones-fila">
            <button
              type="button"
              className="boton-peligro"
              onClick={() => cancelar.mutate()}
              disabled={cancelar.isPending}
            >
              {cancelar.isPending ? 'Cancelando…' : 'Sí, cancelar'}
            </button>
            <button type="button" className="boton-secundario" onClick={() => setModo('ver')} disabled={cancelar.isPending}>
              No, mantenerlo
            </button>
          </div>
        </div>
      )}
    </main>
  )
}

function Encabezado({ turno }: { turno: TurnoPublico }) {
  return (
    <header className="turno-encabezado">
      <p className="turno-barberia">{turno.barberia.nombre}</p>
      <h1>{turno.clienteNombre ? `${turno.clienteNombre.split(' ')[0]}, este es tu turno` : 'Tu turno'}</h1>
    </header>
  )
}

function EstadoTurno({ turno }: { turno: TurnoPublico }) {
  const otroTurno = (
    <Link className="boton-secundario turno-link" to={`/b/${turno.barberia.slug}`}>
      Sacar otro turno
    </Link>
  )

  if (turno.estado === 'CANCELADA') {
    return (
      <div className="turno-estado turno-estado-cancelado">
        <p><strong>Este turno está cancelado.</strong></p>
        {otroTurno}
      </div>
    )
  }
  if (turno.estado === 'COMPLETADA' || turno.estado === 'AUSENTE') {
    return (
      <div className="turno-estado">
        <p><strong>Este turno ya pasó.</strong></p>
        {otroTurno}
      </div>
    )
  }
  if (!turno.puedeCancelar) {
    // Reservado, pero ya no se puede cambiar online
    return (
      <div className="turno-estado">
        <p>
          <strong>Te esperamos.</strong> Los cambios online se aceptan hasta{' '}
          {textoAnticipacion(turno.anticipacionCancelacionMin)} antes del turno. Si necesitás cambiarlo,
          comunicate con la barbería.
        </p>
        {turno.sucursal.telefono && (
          <a
            className="boton-secundario turno-link"
            href={linkWhatsAppSucursal(turno.sucursal.telefono)}
            target="_blank"
            rel="noreferrer"
          >
            Escribir a la barbería
          </a>
        )}
      </div>
    )
  }
  return null
}

interface ReprogramarProps {
  turno: TurnoPublico
  eleccion: EleccionHorario | null
  onElegir: (e: EleccionHorario) => void
  confirmando: boolean
  error: string | null
  onConfirmar: () => void
  onVolver: () => void
}

function Reprogramar({ turno, eleccion, onElegir, confirmando, error, onConfirmar, onVolver }: ReprogramarProps) {
  // El calendario necesita la anticipación máxima de la barbería
  const { data: barberia, isPending, error: errorBarberia } = useQuery({
    queryKey: ['barberia-publica', turno.barberia.slug],
    queryFn: () => obtenerBarberiaPublica(turno.barberia.slug),
    staleTime: 5 * 60 * 1000,
  })

  return (
    <section>
      <div className="reserva-progreso">
        <button type="button" className="reserva-atras" onClick={onVolver} disabled={confirmando}>
          <span aria-hidden="true">‹</span> Volver
        </button>
      </div>
      <h2 className="reserva-titulo-paso">Elegí el nuevo día y horario</h2>
      <p className="turno-limite">
        Ahora: {cuando(turno.inicio)}, {turno.servicio.nombre} con {turno.barbero.nombre}.
      </p>

      {isPending && <p className="reserva-cargando">Cargando…</p>}
      {errorBarberia && <p className="turno-error" role="alert">{mensajeDeError(errorBarberia)}</p>}
      {barberia && (
        <PasoHorario
          slug={turno.barberia.slug}
          anticipacionMaximaDias={barberia.anticipacionMaximaDias}
          sucursalId={turno.sucursal.id}
          servicioId={turno.servicio.id}
          barberoId={turno.barbero.id}
          fechaElegida={eleccion?.fecha ?? null}
          horaElegida={eleccion?.hora ?? null}
          onElegir={onElegir}
        />
      )}

      {eleccion && (
        <div className="turno-confirmar">
          <p>
            Cambiar al <strong>{fechaLarga(eleccion.fecha)}, {horaCorta(eleccion.hora)} hs</strong>
          </p>
          {error && <p className="turno-error" role="alert">{error}</p>}
          <button type="button" className="boton-principal" onClick={onConfirmar} disabled={confirmando}>
            {confirmando ? 'Guardando…' : 'Confirmar cambio'}
          </button>
        </div>
      )}
    </section>
  )
}
