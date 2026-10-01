import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listarNotificaciones, marcarNotificacionEnviada } from '../../api/notificaciones'
import { mensajeDeError } from '../../lib/errores'
import type { EstadoNotificacion, Notificacion } from '../../types/notificacion'
import './notificaciones.css'

const CLAVE = ['notificaciones']

const FILTROS: { valor: EstadoNotificacion; etiqueta: string }[] = [
  { valor: 'PENDIENTE', etiqueta: 'Por enviar' },
  { valor: 'ENVIADA', etiqueta: 'Enviados' },
  { valor: 'FALLIDA', etiqueta: 'Con error' },
  { valor: 'OMITIDA', etiqueta: 'Omitidos' },
]

const ETIQUETA_TIPO = { CONFIRMACION: 'Confirmación', RECORDATORIO: 'Recordatorio' } as const

function fechaTurno(iso: string) {
  return new Date(iso).toLocaleString('es-AR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function NotificacionesPage() {
  const queryClient = useQueryClient()
  const [estado, setEstado] = useState<EstadoNotificacion>('PENDIENTE')
  const [errorAccion, setErrorAccion] = useState<string | null>(null)

  const { data, isPending, error, refetch } = useQuery({
    queryKey: [...CLAVE, estado],
    queryFn: () => listarNotificaciones(estado),
    // Los recordatorios los genera el job: se refresca solo cada minuto
    refetchInterval: 60_000,
  })

  const marcar = useMutation({
    mutationFn: marcarNotificacionEnviada,
    onSuccess: () => {
      setErrorAccion(null)
      queryClient.invalidateQueries({ queryKey: CLAVE })
    },
    onError: (err) => {
      setErrorAccion(mensajeDeError(err))
      queryClient.invalidateQueries({ queryKey: CLAVE })
    },
  })

  return (
    <section className="notificaciones">
      <h1>Mensajes para enviar</h1>
      <p className="notificaciones-ayuda">
        Tocá <strong>Abrir WhatsApp</strong>, mandá el mensaje y después marcalo como enviado.
      </p>

      <div className="notificaciones-filtros" role="group" aria-label="Estado">
        {FILTROS.map((f) => (
          <button key={f.valor} type="button" aria-pressed={estado === f.valor} onClick={() => setEstado(f.valor)}>
            {f.etiqueta}
          </button>
        ))}
      </div>

      {errorAccion && (
        <p className="notificaciones-error" role="alert">
          {errorAccion}
        </p>
      )}

      {isPending && <p>Cargando…</p>}

      {error && (
        <div role="alert">
          <p>{mensajeDeError(error)}</p>
          <button type="button" className="boton-secundario" onClick={() => refetch()}>
            Reintentar
          </button>
        </div>
      )}

      {data && data.length === 0 && <p>No hay mensajes en esta lista.</p>}

      <ul className="notificaciones-lista">
        {data?.map((n) => (
          <TarjetaNotificacion
            key={n.id}
            n={n}
            ocupado={marcar.isPending}
            onMarcar={() => marcar.mutate(n.id)}
          />
        ))}
      </ul>
    </section>
  )
}

function TarjetaNotificacion({
  n,
  ocupado,
  onMarcar,
}: {
  n: Notificacion
  ocupado: boolean
  onMarcar: () => void
}) {
  const pendiente = n.estado === 'PENDIENTE'

  return (
    <li className="notificacion">
      <div className="notificacion-cabecera">
        <span className={`notificacion-tipo notificacion-tipo-${n.tipo.toLowerCase()}`}>{ETIQUETA_TIPO[n.tipo]}</span>
        <strong>{n.clienteNombre ?? n.destinatario}</strong>
      </div>
      <p className="notificacion-turno">
        Turno: {fechaTurno(n.turnoInicio)}
        {n.barberoNombre ? ` · ${n.barberoNombre}` : ''}
      </p>
      <p className="notificacion-telefono">{n.destinatario}</p>
      <details>
        <summary>Ver mensaje</summary>
        <pre className="notificacion-mensaje">{n.mensaje}</pre>
      </details>

      {n.error && <p className="notificaciones-error">{n.error}</p>}
      {n.enviadaEn && <p className="notificacion-turno">Enviado: {fechaTurno(n.enviadaEn)}</p>}

      {pendiente && (
        <div className="notificacion-acciones">
          {n.linkWhatsApp && (
            <a className="boton-principal" href={n.linkWhatsApp} target="_blank" rel="noopener noreferrer">
              Abrir WhatsApp
            </a>
          )}
          <button type="button" className="boton-secundario" disabled={ocupado} onClick={onMarcar}>
            Ya lo envié
          </button>
        </div>
      )}
    </li>
  )
}