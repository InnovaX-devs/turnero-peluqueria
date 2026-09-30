import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { actualizarBloqueo, crearBloqueo, eliminarBloqueo, verificarConflictos } from '../../api/bloqueos'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { Bloqueo, BloqueoDatos, Turno } from '../../types/agenda'
import type { OpcionesRegistro } from '../../types/registroCorte'
import { useOpcionesRegistro } from '../atenciones/useOpcionesRegistro'
import { formatearHora, linkWhatsApp } from './agenda'
import { valoresIniciales } from './bloqueos'
import '../atenciones/registro.css'
import './bloqueo.css'

interface Props {
  /** null = bloqueo nuevo */
  bloqueo: Bloqueo | null
  /** Día que se está viendo en la agenda: es el que se propone para un bloqueo nuevo */
  fechaInicial: string
  onCerrar: () => void
}

export function BloqueoForm({ bloqueo, fechaInicial, onCerrar }: Props) {
  const ref = useRef<HTMLDialogElement>(null)
  const { data, isPending, error, refetch } = useOpcionesRegistro()

  useEffect(() => {
    const dialogo = ref.current
    if (dialogo && !dialogo.open) dialogo.showModal()
  }, [])

  return (
    <dialog
      ref={ref}
      className="registro"
      aria-labelledby="bloqueo-titulo"
      onCancel={(e) => {
        e.preventDefault()
        onCerrar()
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onCerrar()
      }}
    >
      <div className="registro-contenido">
        <header className="registro-encabezado">
          <h2 id="bloqueo-titulo">{bloqueo ? 'Editar bloqueo' : 'Bloquear horario'}</h2>
          <button type="button" className="boton-texto" onClick={onCerrar}>
            Cerrar
          </button>
        </header>

        {isPending && <p className="registro-estado">Cargando…</p>}
        {error && (
          <div role="alert">
            <p className="registro-error">{mensajeDeError(error)}</p>
            <button type="button" className="boton-secundario" onClick={() => refetch()}>
              Reintentar
            </button>
          </div>
        )}
        {data && <Formulario bloqueo={bloqueo} fechaInicial={fechaInicial} opciones={data} onCerrar={onCerrar} />}
      </div>
    </dialog>
  )
}

/** Si la API devolvió errores por campo, se muestra el primero; si no, el mensaje general. */
function textoDeError(error: unknown): string {
  const campos = Object.values(erroresDeCampos(error))
  return campos.length > 0 ? campos[0] : mensajeDeError(error)
}

interface FormularioProps {
  bloqueo: Bloqueo | null
  fechaInicial: string
  opciones: OpcionesRegistro
  onCerrar: () => void
}

function Formulario({ bloqueo, fechaInicial, opciones, onCerrar }: FormularioProps) {
  const queryClient = useQueryClient()
  const [inicial] = useState(() => valoresIniciales(bloqueo, fechaInicial, opciones))

  const [barberoId, setBarberoId] = useState<string | null>(inicial.barberoId)
  const [diaCompleto, setDiaCompleto] = useState(inicial.diaCompleto)
  const [fecha, setFecha] = useState(inicial.fecha)
  const [desde, setDesde] = useState(inicial.desde)
  const [hasta, setHasta] = useState(inicial.hasta)
  const [motivo, setMotivo] = useState(inicial.motivo)
  const [conflictos, setConflictos] = useState<Turno[] | null>(null)
  const [confirmandoBorrado, setConfirmandoBorrado] = useState(false)

  const horasInvalidas = !diaCompleto && desde !== '' && hasta !== '' && hasta <= desde
  const listo =
    barberoId !== null && fecha !== '' && (diaCompleto || (desde !== '' && hasta !== '' && !horasInvalidas))

  const datos: BloqueoDatos = {
    barberoId,
    fecha,
    horaDesde: diaCompleto ? null : desde,
    horaHasta: diaCompleto ? null : hasta,
    motivo: motivo.trim() === '' ? null : motivo.trim(),
  }

  function alTerminar() {
    void queryClient.invalidateQueries({ queryKey: ['agenda'] })
    onCerrar()
  }

  // Primero se fija si el bloqueo tapa turnos reservados: si es así, se muestran y se pide confirmar.
  const guardar = useMutation({
    mutationFn: async (confirmado: boolean): Promise<Turno[]> => {
      if (!confirmado) {
        const turnos = await verificarConflictos(datos)
        if (turnos.length > 0) return turnos
      }
      if (bloqueo) await actualizarBloqueo(bloqueo.id, datos)
      else await crearBloqueo(datos)
      return []
    },
    onSuccess: (turnos) => {
      if (turnos.length > 0) setConflictos(turnos)
      else alTerminar()
    },
  })

  const borrar = useMutation({
    mutationFn: () => eliminarBloqueo(bloqueo?.id ?? ''),
    onSuccess: alTerminar,
  })

  function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (listo) guardar.mutate(false)
  }

  // ---------- Aviso: el bloqueo se superpone con turnos reservados ----------
  if (conflictos) {
    const mostrarBarbero = opciones.barberos.length > 1
    return (
      <div className="registro-form">
        <div className="bloqueo-aviso" role="alert">
          <strong>
            {conflictos.length === 1
              ? 'Hay 1 turno reservado en ese horario'
              : 'Hay ' + conflictos.length + ' turnos reservados en ese horario'}
          </strong>
          <p>Si bloqueás igual, los turnos no se cancelan: siguen reservados. Avisales a los clientes.</p>
        </div>

        <ul className="bloqueo-conflictos">
          {conflictos.map((t) => (
            <li key={t.id}>
              <strong>
                {formatearHora(t.inicio)}–{formatearHora(t.fin)} · {t.clienteNombre ?? 'Sin cliente'}
              </strong>
              <small>
                {t.servicioNombre}
                {mostrarBarbero ? ' · ' + t.barberoNombre : ''}
              </small>
              {t.clienteTelefono && (
                <a href={linkWhatsApp(t.clienteTelefono)} target="_blank" rel="noreferrer">
                  Escribir por WhatsApp
                </a>
              )}
            </li>
          ))}
        </ul>

        <div className="registro-pie">
          {guardar.error && (
            <p className="registro-error" role="alert">
              {textoDeError(guardar.error)}
            </p>
          )}
          <div className="bloqueo-acciones">
            <button type="button" className="boton-secundario" onClick={() => setConflictos(null)}>
              Volver
            </button>
            <button
              type="button"
              className="boton-principal"
              disabled={guardar.isPending}
              onClick={() => guardar.mutate(true)}
            >
              {guardar.isPending ? 'Guardando…' : bloqueo ? 'Guardar igual' : 'Bloquear igual'}
            </button>
          </div>
        </div>
      </div>
    )
  }

  // ---------- Formulario ----------
  return (
    <form className="registro-form" onSubmit={enviar}>
      {opciones.barberos.length > 1 && (
        <fieldset className="registro-grupo">
          <legend>Barbero</legend>
          <div className="registro-opciones">
            {opciones.barberos.map((b) => (
              <button
                key={b.id}
                type="button"
                className="registro-opcion"
                aria-pressed={b.id === barberoId}
                onClick={() => setBarberoId(b.id)}
              >
                {b.nombre}
              </button>
            ))}
          </div>
        </fieldset>
      )}

      <fieldset className="registro-grupo">
        <legend>Qué querés bloquear</legend>
        <div className="bloqueo-dos-columnas">
          <button
            type="button"
            className="registro-opcion registro-opcion-centrada"
            aria-pressed={!diaCompleto}
            onClick={() => setDiaCompleto(false)}
          >
            Un horario
          </button>
          <button
            type="button"
            className="registro-opcion registro-opcion-centrada"
            aria-pressed={diaCompleto}
            onClick={() => setDiaCompleto(true)}
          >
            Día completo
          </button>
        </div>
      </fieldset>

      <label className="registro-grupo">
        <span className="registro-etiqueta">Día</span>
        <input className="registro-input" type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} />
      </label>

      {!diaCompleto && (
        <div className="registro-grupo">
          <div className="bloqueo-dos-columnas">
            <label>
              <span className="registro-etiqueta">Desde</span>
              <input
                className="registro-input"
                type="time"
                value={desde}
                onChange={(e) => setDesde(e.target.value)}
              />
            </label>
            <label>
              <span className="registro-etiqueta">Hasta</span>
              <input
                className="registro-input"
                type="time"
                value={hasta}
                onChange={(e) => setHasta(e.target.value)}
              />
            </label>
          </div>
          {horasInvalidas && <p className="bloqueo-ayuda">La hora final tiene que ser posterior a la inicial.</p>}
        </div>
      )}

      <label className="registro-grupo">
        <span className="registro-etiqueta">Motivo (opcional)</span>
        <input
          className="registro-input"
          value={motivo}
          maxLength={200}
          placeholder="Ej: Almuerzo, franco, trámite"
          onChange={(e) => setMotivo(e.target.value)}
        />
      </label>

      {bloqueo && (
        <div className="bloqueo-eliminar">
          {confirmandoBorrado ? (
            <div className="bloqueo-confirmar-borrado">
              <p>¿Eliminar este bloqueo? Los horarios vuelven a estar disponibles.</p>
              {borrar.error && (
                <p className="registro-error" role="alert">
                  {textoDeError(borrar.error)}
                </p>
              )}
              <div className="bloqueo-acciones">
                <button type="button" className="boton-secundario" onClick={() => setConfirmandoBorrado(false)}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="boton-principal bloqueo-boton-peligro"
                  disabled={borrar.isPending}
                  onClick={() => borrar.mutate()}
                >
                  {borrar.isPending ? 'Eliminando…' : 'Sí, eliminar'}
                </button>
              </div>
            </div>
          ) : (
            <button type="button" className="boton-texto bloqueo-eliminar-boton" onClick={() => setConfirmandoBorrado(true)}>
              Eliminar bloqueo
            </button>
          )}
        </div>
      )}

      <div className="registro-pie">
        {guardar.error && (
          <p className="registro-error" role="alert">
            {textoDeError(guardar.error)}
          </p>
        )}
        <button type="submit" className="boton-principal" disabled={!listo || guardar.isPending}>
          {guardar.isPending ? 'Guardando…' : bloqueo ? 'Guardar cambios' : 'Bloquear'}
        </button>
      </div>
    </form>
  )
}