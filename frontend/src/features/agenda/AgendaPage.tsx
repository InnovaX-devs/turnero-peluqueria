import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { obtenerAgenda } from '../../api/agenda'
import { mensajeDeError } from '../../lib/errores'
import { veTodoElNegocio } from '../../lib/roles'
import { Selector } from '../../components/Selector'
import type { Bloqueo, Turno } from '../../types/agenda'
import { useAuth } from '../auth/useAuth'
import { BloqueoForm } from './BloqueoForm'
import { ResumenDia } from './ResumenDia'
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
  // Admin y recepción ven la agenda de todos y pueden filtrar; el barbero, solo la suya.
  const veTodo = veTodoElNegocio(usuario?.rol)

  const [fecha, setFecha] = useState(() => hoyISO())
  const [vista, setVista] = useState<Vista>('dia')
  const [profesionalId, setProfesionalId] = useState<string | null>(null)
  const [seleccionado, setSeleccionado] = useState<Turno | null>(null)
  // null = cerrado; { bloqueo: null } = bloqueo nuevo; { bloqueo } = editar ese bloqueo
 const [formBloqueo, setFormBloqueo] = useState<{ bloqueo: Bloqueo | null } | null>(null)
  const { desde, hasta } = rangoDeVista(vista, fecha)
  const filtroProfesional = veTodo ? (profesionalId ?? undefined) : undefined

  const { data, isPending, error, refetch } = useQuery({
    queryKey: ['agenda', desde, hasta, filtroProfesional],
    queryFn: () => obtenerAgenda(desde, hasta, filtroProfesional),
  })

  const dias = useMemo(() => diasDelRango(desde, hasta), [desde, hasta])
  const porDia = useMemo(() => agruparPorDia(data ? combinarItems(data) : [], dias), [data, dias])

  // Si se ven varios barberos a la vez, hay que aclarar de quién es cada turno.
  const mostrarProfesional = veTodo && profesionalId === null

  function moverFecha(pasos: number) {
    setFecha((f) => sumarDias(f, vista === 'dia' ? pasos : pasos * 7))
  }

  return (
    <section className="agenda">
      <h1 className="agenda-titulo">Agenda</h1>

      {vista === 'dia' && data && <ResumenDia turnos={data.turnos} fecha={fecha} esAdmin={esAdmin} />}

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

          {veTodo && data && data.profesionales.length > 0 && (
            <Selector
              variante="pill"
              alinear="derecha"
              etiqueta="Filtrar por barbero"
              valor={profesionalId ?? ''}
              onChange={(v) => setProfesionalId(v === '' ? null : v)}
              opciones={[
                { valor: '', etiqueta: 'Todos los barberos' },
                ...data.profesionales.map((b) => ({ valor: b.id, etiqueta: b.nombre })),
              ]}
            />
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
                              <TarjetaTurno turno={turno} mostrarProfesional={mostrarProfesional} />
                            </button>
                          ) : bloqueo ? (
                            <button
                              type="button"
                              className="agenda-item agenda-item-bloqueo"
                              onClick={() => setFormBloqueo({ bloqueo })}
                            >
                              <TarjetaBloqueo bloqueo={bloqueo} mostrarProfesional={mostrarProfesional} />
                            </button>
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
          profesionalInicialId={filtroProfesional ?? null}
          onCerrar={() => setFormBloqueo(null)}
        />
      )}
    </section>
  )
}

function TarjetaTurno({ turno, mostrarProfesional }: { turno: Turno; mostrarProfesional: boolean }) {
  return (
    <>
      <div className="agenda-item-hora">
        {formatearHora(turno.inicio)}
        <span className="agenda-item-hora-fin">–{formatearHora(turno.fin)}</span>
      </div>
      <div className="agenda-item-cuerpo">
        <strong>{turno.clienteNombre ?? 'Sin cliente'}</strong>
        <span className="agenda-item-servicio">{turno.servicioNombre}</span>
        {mostrarProfesional && <span className="agenda-item-profesional">{turno.profesionalNombre}</span>}
      </div>
      <div className="agenda-item-badges">
        <span className={`agenda-badge estado-${turno.estado.toLowerCase()}`}>{ETIQUETAS_ESTADO[turno.estado]}</span>
        <span className={`agenda-badge pago-${turno.estadoPago.toLowerCase()}`}>{ETIQUETAS_PAGO[turno.estadoPago]}</span>
      </div>
    </>
  )
}

function TarjetaBloqueo({ bloqueo, mostrarProfesional }: { bloqueo: Bloqueo; mostrarProfesional: boolean }) {
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
        {mostrarProfesional && <span className="agenda-item-profesional">{bloqueo.profesionalNombre}</span>}
      </div>
    </>
  )
}