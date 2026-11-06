import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { crearTurno, obtenerHorariosLibres } from '../../api/agenda'
import { anotarListaEspera } from '../../api/listaEspera'
import { mensajeDeError } from '../../lib/errores'
import { veTodoElNegocio } from '../../lib/roles'
import type { ClienteResumen, OpcionesRegistro } from '../../types/registroCorte'
import { BuscadorCliente } from '../atenciones/BuscadorCliente'
import { formatearPrecio } from '../atenciones/registroCorte'
import { useOpcionesRegistro } from '../atenciones/useOpcionesRegistro'
import { useAuth } from '../auth/useAuth'
import { formatearFechaLarga, hoyISO } from './agenda'
import '../atenciones/registro.css'

interface Props {
  /** Día que se está viendo en la agenda */
  fechaInicial: string
  /** Barbero filtrado en la agenda, si hay */
  profesionalInicialId: string | null
  onCerrar: () => void
}

/**
 * Dar turno desde el panel (issue #71): por teléfono o en el mostrador. Si no hay lugar, el mismo
 * formulario anota al cliente en la lista de espera. Admin y recepción pueden dar sobreturnos.
 */
export function NuevoTurno({ fechaInicial, profesionalInicialId, onCerrar }: Props) {
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
      aria-labelledby="nuevo-turno-titulo"
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
          <h2 id="nuevo-turno-titulo">Nuevo turno</h2>
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
        {data && data.servicios.length === 0 && <p className="registro-estado">Todavía no hay servicios cargados.</p>}
        {data && data.servicios.length > 0 && (
          <Formulario
            opciones={data}
            fechaInicial={fechaInicial < hoyISO() ? hoyISO() : fechaInicial}
            profesionalInicialId={profesionalInicialId}
            onListo={onCerrar}
          />
        )}
      </div>
    </dialog>
  )
}

interface FormularioProps {
  opciones: OpcionesRegistro
  fechaInicial: string
  profesionalInicialId: string | null
  onListo: () => void
}

/** "17:00:00" -> "17:00" */
function hhmm(hora: string): string {
  return hora.slice(0, 5)
}

function Formulario({ opciones, fechaInicial, profesionalInicialId, onListo }: FormularioProps) {
  const queryClient = useQueryClient()
  const { usuario } = useAuth()
  const puedeSobreturno = veTodoElNegocio(usuario?.rol)

  const inicialProfesional =
    (profesionalInicialId && opciones.profesionales.some((p) => p.id === profesionalInicialId)
      ? profesionalInicialId
      : null) ??
    opciones.profesionalPropioId ??
    (opciones.profesionales.length === 1 ? opciones.profesionales[0].id : null)

  const [modo, setModo] = useState<'turno' | 'espera'>('turno')
  const [cliente, setCliente] = useState<ClienteResumen | null>(null)
  const [servicioId, setServicioId] = useState<string>(opciones.servicios[0].id)
  const [profesionalId, setProfesionalId] = useState<string | null>(inicialProfesional)
  const [sucursalId, setSucursalId] = useState<string | null>(null)
  const [fecha, setFecha] = useState(fechaInicial)
  const [hora, setHora] = useState<string | null>(null)
  const [sobreturno, setSobreturno] = useState(false)
  const [horaSobreturno, setHoraSobreturno] = useState('')
  const [notas, setNotas] = useState('')

  const profesional = opciones.profesionales.find((p) => p.id === profesionalId)
  const sucursalesDelProfesional = opciones.sucursales.filter(
    (s) => profesional !== undefined && profesional.sucursalIds.includes(s.id),
  )
  const sucursalFinal = sucursalesDelProfesional.length === 1 ? sucursalesDelProfesional[0].id : sucursalId
  const faltaSucursal = sucursalesDelProfesional.length > 1 && sucursalId === null
  const servicio = opciones.servicios.find((s) => s.id === servicioId)

  const libres = useQuery({
    queryKey: ['horarios-libres', profesionalId, servicioId, sucursalFinal, fecha],
    queryFn: () => obtenerHorariosLibres(profesionalId!, servicioId, sucursalFinal, fecha),
    enabled: modo === 'turno' && profesionalId !== null && !faltaSucursal && fecha !== '',
  })

  const darTurno = useMutation({
    mutationFn: crearTurno,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['agenda'] })
      void queryClient.invalidateQueries({ queryKey: ['horarios-libres'] })
      onListo()
    },
  })

  const anotar = useMutation({
    mutationFn: anotarListaEspera,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['lista-espera'] })
      onListo()
    },
  })

  // Si cambia el barbero, el servicio o el día, la hora elegida vale solo si sigue libre en la grilla nueva
  const horaElegida = hora !== null && libres.data?.horas.includes(hora) ? hora : null
  const horaFinal = sobreturno ? horaSobreturno : horaElegida
  const listoTurno = cliente !== null && profesionalId !== null && !faltaSucursal && fecha !== '' && !!horaFinal
  const listoEspera = cliente !== null && fecha !== '' && (puedeSobreturno || profesionalId !== null)
  const mutacion = modo === 'turno' ? darTurno : anotar

  function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (cliente === null) return
    const nota = notas.trim() === '' ? null : notas.trim()
    if (modo === 'espera') {
      anotar.mutate({ clienteId: cliente.id, profesionalId, fecha, nota })
      return
    }
    if (!listoTurno || profesionalId === null || !horaFinal) return
    darTurno.mutate({
      clienteId: cliente.id,
      servicioId,
      profesionalId,
      sucursalId: sucursalFinal,
      fecha,
      hora: hhmm(horaFinal),
      notas: nota,
      sobreturno,
    })
  }

  return (
    <form className="registro-form" onSubmit={enviar}>
      <div className="registro-opciones" role="group" aria-label="Qué querés hacer">
        <button
          type="button"
          className="registro-opcion registro-opcion-centrada"
          aria-pressed={modo === 'turno'}
          onClick={() => setModo('turno')}
        >
          Dar turno
        </button>
        <button
          type="button"
          className="registro-opcion registro-opcion-centrada"
          aria-pressed={modo === 'espera'}
          onClick={() => setModo('espera')}
        >
          Lista de espera
        </button>
      </div>

      <fieldset className="registro-grupo">
        <legend>Cliente</legend>
        <BuscadorCliente seleccionado={cliente} onSeleccionar={setCliente} />
      </fieldset>

      {modo === 'turno' && (
        <fieldset className="registro-grupo">
          <legend>Servicio</legend>
          <div className="registro-opciones">
            {opciones.servicios.map((s) => (
              <button
                key={s.id}
                type="button"
                className="registro-opcion"
                aria-pressed={s.id === servicioId}
                onClick={() => setServicioId(s.id)}
              >
                <strong>{s.nombre}</strong>
                <small>
                  {s.duracionMin} min · {formatearPrecio(s.precio)}
                </small>
              </button>
            ))}
          </div>
        </fieldset>
      )}

      {(opciones.profesionales.length > 1 || (modo === 'espera' && puedeSobreturno)) && (
        <fieldset className="registro-grupo">
          <legend>Barbero</legend>
          <div className="registro-opciones">
            {modo === 'espera' && puedeSobreturno && (
              <button
                type="button"
                className="registro-opcion"
                aria-pressed={profesionalId === null}
                onClick={() => setProfesionalId(null)}
              >
                Cualquiera
              </button>
            )}
            {opciones.profesionales.map((p) => (
              <button
                key={p.id}
                type="button"
                className="registro-opcion"
                aria-pressed={p.id === profesionalId}
                onClick={() => {
                  setProfesionalId(p.id)
                  setSucursalId(null)
                }}
              >
                {p.nombre}
              </button>
            ))}
          </div>
        </fieldset>
      )}

      {modo === 'turno' && sucursalesDelProfesional.length > 1 && (
        <fieldset className="registro-grupo">
          <legend>Sucursal</legend>
          <div className="registro-opciones">
            {sucursalesDelProfesional.map((s) => (
              <button
                key={s.id}
                type="button"
                className="registro-opcion"
                aria-pressed={s.id === sucursalId}
                onClick={() => setSucursalId(s.id)}
              >
                {s.nombre}
              </button>
            ))}
          </div>
        </fieldset>
      )}

      <label className="registro-grupo">
        <span className="registro-etiqueta">Día</span>
        <input
          className="registro-input"
          type="date"
          min={hoyISO()}
          value={fecha}
          onChange={(e) => setFecha(e.target.value)}
        />
      </label>

      {modo === 'turno' && (
        <fieldset className="registro-grupo">
          <legend>Horario</legend>

          {!sobreturno && profesionalId === null && <p className="registro-estado">Elegí el barbero.</p>}
          {!sobreturno && faltaSucursal && <p className="registro-estado">Elegí la sucursal.</p>}
          {!sobreturno && libres.isPending && libres.fetchStatus !== 'idle' && (
            <p className="registro-estado">Buscando horarios…</p>
          )}
          {!sobreturno && libres.error && <p className="registro-error">{mensajeDeError(libres.error)}</p>}
          {!sobreturno && libres.data && libres.data.horas.length === 0 && (
            <p className="registro-estado">
              No hay horarios libres el {formatearFechaLarga(fecha).toLowerCase()}.{' '}
              <button type="button" className="boton-texto" onClick={() => setModo('espera')}>
                Anotar en lista de espera
              </button>
            </p>
          )}
          {!sobreturno && libres.data && libres.data.horas.length > 0 && (
            <div className="registro-horas">
              {libres.data.horas.map((h) => (
                <button
                  key={h}
                  type="button"
                  className="registro-opcion registro-opcion-centrada"
                  aria-pressed={h === horaElegida}
                  onClick={() => setHora(h)}
                >
                  {hhmm(h)}
                </button>
              ))}
            </div>
          )}

          {puedeSobreturno && (
            <label className="registro-sobreturno">
              <input type="checkbox" checked={sobreturno} onChange={(e) => setSobreturno(e.target.checked)} />
              <span>
                <strong>Sobreturno</strong>
                <small>Encima de otro turno o fuera del horario del barbero. No se ofrece en la reserva online.</small>
              </span>
            </label>
          )}
          {sobreturno && (
            <input
              className="registro-input"
              type="time"
              aria-label="Hora del sobreturno"
              value={horaSobreturno}
              onChange={(e) => setHoraSobreturno(e.target.value)}
            />
          )}
        </fieldset>
      )}

      <label className="registro-grupo">
        <span className="registro-etiqueta">{modo === 'turno' ? 'Notas (opcional)' : 'Nota (opcional)'}</span>
        <textarea
          className="registro-input"
          value={notas}
          maxLength={modo === 'turno' ? 2000 : 500}
          placeholder={modo === 'turno' ? 'Ej: viene con el hijo' : 'Ej: puede después de las 18'}
          onChange={(e) => setNotas(e.target.value)}
        />
      </label>

      <div className="registro-pie">
        {mutacion.error && (
          <p className="registro-error" role="alert">
            {mensajeDeError(mutacion.error)}
          </p>
        )}
        {modo === 'turno' ? (
          <button type="submit" className="boton-principal" disabled={!listoTurno || darTurno.isPending}>
            {darTurno.isPending
              ? 'Guardando…'
              : cliente === null
                ? 'Elegí el cliente'
                : horaFinal
                ? `${sobreturno ? 'Dar sobreturno' : 'Dar turno'} · ${hhmm(horaFinal)}${servicio ? ' · ' + servicio.nombre : ''}`
                : 'Elegí un horario'}
          </button>
        ) : (
          <button type="submit" className="boton-principal" disabled={!listoEspera || anotar.isPending}>
            {anotar.isPending ? 'Guardando…' : cliente === null ? 'Elegí el cliente' : 'Anotar en lista de espera'}
          </button>
        )}
      </div>
    </form>
  )
}
