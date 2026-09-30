import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { obtenerAgenda } from '../../api/agenda'
import { mensajeDeError } from '../../lib/errores'
import type { Bloqueo, Turno } from '../../types/agenda'
import { useAuth } from '../auth/useAuth'
import { BloqueoForm } from './BloqueoForm'
import { TurnoDetalle } from './TurnoDetalle'
import {
  ETIQUETAS_ESTADO,
  ETIQUETAS_PAGO,
  type Vista,
  agruparPorDia,
  combinarItems,
  diasDelRango,
  formatearFechaCorta,
  formatearFechaLarga,
  formatearHora,
  hoyISO,
  rangoDeVista,
  sumarDias,
} from './agenda'
import { esDiaCompleto } from './bloqueos'
import './agenda.css'

export function AgendaPage() {
  const { usuario } = useAuth()
  const esAdmin = usuario?.rol === 'ADMIN'

  const [fecha, setFecha] = useState(() => hoyISO())
  const [vista, setVista] = useState<Vista>('dia')
  const [barberoId, setBarberoId] = useState<string | null>(null)
  const [seleccionado, setSeleccionado] = useState<Turno | null>(null)
  // null = cerrado; { bloqueo: null } = bloqueo nuevo; { bloqueo } = editar ese bloqueo
 const [formBloqueo, setFormBloqueo] = useState<{ bloqueo: Bloqueo | null } | null>(null)
  const { desde, hasta } = rangoDeVista(vista, fecha)
  const filtroBarbero = esAdmin ? (barberoId ?? undefined) : undefined

  const { data, isPending, error, refetch } = useQuery({
    queryKey: ['agenda', desde, hasta, filtroBarbero],
    queryFn: () => obtenerAgenda(desde, hasta, filtroBarbero),
  })

  const dias = useMemo(() => diasDelRango(desde, hasta), [desde, hasta])
  const porDia = useMemo(() => agruparPorDia(data ? combinarItems(data) : [], dias), [data, dias])

  // Si el admin ve varios barberos a la vez, hay que aclarar de quién es cada turno.
  const mostrarBarbero = esAdmin && barberoId === null

  function moverFecha(pasos: number) {
    setFecha((f) => sumarDias(f, vista === 'dia' ? pasos : pasos * 7))
  }

  return (
    <section className="agenda">
      <h1 className="agenda-titulo">Agenda</h1>

      <header className="agenda-controles">
        <div className="agenda-navegacion">
          <button type="button" className="boton-secundario" onClick={() => moverFecha(-1)} aria-label="Anterior">
            ‹
          </button>
          <button type="button" className="boton-secundario" onClick={() => setFecha(hoyISO())}>
            Hoy
          </button>
          <button type="button" className="boton-secundario" onClick={() => moverFecha(1)} aria-label="Siguiente">
            ›
          </button>
          <strong className="agenda-fecha">
            {vista === 'dia' ? formatearFechaLarga(fecha) : `${formatearFechaCorta(desde)} – ${formatearFechaCorta(hasta)}`}
          </strong>
        </div>

        <div className="agenda-filtros">
          <div className="agenda-vista-toggle" role="group" aria-label="Vista">
            <button type="button" className={vista === 'dia' ? 'activo' : ''} onClick={() => setVista('dia')}>
              Día
            </button>
            <button type="button" className={vista === 'semana' ? 'activo' : ''} onClick={() => setVista('semana')}>
              Semana
            </button>
          </div>

          <button type="button" className="boton-secundario" onClick={() => setFormBloqueo({ bloqueo: null })}>
            + Bloquear horario
          </button>

          {esAdmin && data && data.barberos.length > 0 && (
            <select
              className="agenda-select-barbero"
              value={barberoId ?? ''}
              onChange={(e) => setBarberoId(e.target.value === '' ? null : e.target.value)}
              aria-label="Filtrar por barbero"
            >
              <option value="">Todos los barberos</option>
              {data.barberos.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.nombre}
                </option>
              ))}
            </select>
          )}
        </div>
      </header>

      {isPending && <p className="agenda-estado">Cargando…</p>}

      {error && (
        <div className="agenda-error" role="alert">
          <p>{mensajeDeError(error)}</p>
          <button type="button" className="boton-secundario" onClick={() => refetch()}>
            Reintentar
          </button>
        </div>
      )}

      {data && (
        <div className="agenda-dias">
          {dias.map((dia) => {
            const itemsDelDia = porDia.get(dia) ?? []
            return (
              <div key={dia} className="agenda-dia">
                {vista === 'semana' && <h2 className="agenda-dia-titulo">{formatearFechaCorta(dia)}</h2>}

                {itemsDelDia.length === 0 && <p className="agenda-dia-vacio">Sin turnos</p>}

                {itemsDelDia.length > 0 && (
                  <ul className="agenda-lista">
                    {itemsDelDia.map((item) => {
                      const { turno, bloqueo } = item
                      return (
                        <li key={`${item.tipo}-${turno?.id ?? bloqueo?.id}`}>
                          {turno ? (
                            <button
                              type="button"
                              className="agenda-item agenda-item-turno"
                              onClick={() => setSeleccionado(turno)}
                            >
                              <TarjetaTurno turno={turno} mostrarBarbero={mostrarBarbero} />
                            </button>
                          ) : bloqueo ? (
                            <button
                              type="button"
                              className="agenda-item agenda-item-bloqueo"
                              onClick={() => setFormBloqueo({ bloqueo })}
                            >
                              <TarjetaBloqueo bloqueo={bloqueo} mostrarBarbero={mostrarBarbero} />
+                            </button>
                          ) : null}
                        </li>
                      )
                    })}
                  </ul>
                )}
              </div>
            )
          })}
        </div>
      )}

      {seleccionado && <TurnoDetalle turno={seleccionado} onCerrar={() => setSeleccionado(null)} />}

      {formBloqueo && (
        <BloqueoForm
          bloqueo={formBloqueo.bloqueo}
          fechaInicial={fecha}
          barberoInicialId={filtroBarbero ?? null}
          onCerrar={() => setFormBloqueo(null)}
        />
      )}
    </section>
  )
}

function TarjetaTurno({ turno, mostrarBarbero }: { turno: Turno; mostrarBarbero: boolean }) {
  return (
    <>
      <div className="agenda-item-hora">
        {formatearHora(turno.inicio)}
        <span className="agenda-item-hora-fin">–{formatearHora(turno.fin)}</span>
      </div>
      <div className="agenda-item-cuerpo">
        <strong>{turno.clienteNombre ?? 'Sin cliente'}</strong>
        <span className="agenda-item-servicio">{turno.servicioNombre}</span>
        {mostrarBarbero && <span className="agenda-item-barbero">{turno.barberoNombre}</span>}
      </div>
      <div className="agenda-item-badges">
        <span className={`agenda-badge estado-${turno.estado.toLowerCase()}`}>{ETIQUETAS_ESTADO[turno.estado]}</span>
        <span className={`agenda-badge pago-${turno.estadoPago.toLowerCase()}`}>{ETIQUETAS_PAGO[turno.estadoPago]}</span>
      </div>
    </>
  )
}

function TarjetaBloqueo({ bloqueo, mostrarBarbero }: { bloqueo: Bloqueo; mostrarBarbero: boolean }) {
  return (
    <>
      <div className="agenda-item-hora">
        {esDiaCompleto(bloqueo) ? (
          'Todo el día'
        ) : (
          <>
            {formatearHora(bloqueo.inicio)}
            <span className="agenda-item-hora-fin">–{formatearHora(bloqueo.fin)}</span>
          </>
        )}
      </div>
      <div className="agenda-item-cuerpo">
        <strong>{bloqueo.motivo ?? 'Bloqueado'}</strong>
        {mostrarBarbero && <span className="agenda-item-barbero">{bloqueo.barberoNombre}</span>}
      </div>
    </>
  )
}