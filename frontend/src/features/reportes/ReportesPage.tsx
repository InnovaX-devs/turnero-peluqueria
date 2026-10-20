import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { obtenerClientesInactivos, obtenerReportes } from '../../api/reportes'
import { mensajeDeError } from '../../lib/errores'
import { Selector } from '../../components/Selector'
import { hoyISO } from '../../lib/fechas'
import { formatearPrecio } from '../../lib/formato'
import type { Ausentismo, BarberoReporte, ClienteFrecuente, Demanda } from '../../types/reportes'
import { useAuth } from '../auth/useAuth'
import { useOpcionesRegistro } from '../atenciones/useOpcionesRegistro'
import { periodoDe, type Periodo, type Preset } from '../caja/periodos'
import { DIAS_CORTOS, DIAS_SEMANA, fechaCorta, linkWhatsApp, tasaAusentismo } from './formatos'
import '../caja/caja.css'
import './reportes.css'

const PRESETS: { valor: Exclude<Preset, 'hoy'>; etiqueta: string }[] = [
  { valor: 'semana', etiqueta: 'Esta semana' },
  { valor: 'mes', etiqueta: 'Este mes' },
  { valor: 'personalizado', etiqueta: 'Otro período' },
]

const OPCIONES_DIAS = [30, 45, 60, 90]

export function ReportesPage() {
  // El barbero ve solo sus números (el backend los filtra con su token); acá solo cambian los títulos.
  const { usuario } = useAuth()
  const propio = usuario?.rol === 'BARBERO'
  const [preset, setPreset] = useState<Exclude<Preset, 'hoy'>>('mes')
  const [personalizado, setPersonalizado] = useState<Periodo>(() => periodoDe('mes'))
  const [sucursalId, setSucursalId] = useState<string | null>(null)

  const { data: opciones } = useOpcionesRegistro()
  const sucursales = opciones?.sucursales ?? []

  const periodo = preset === 'personalizado' ? personalizado : periodoDe(preset)
  const periodoValido = periodo.desde !== '' && periodo.hasta !== '' && periodo.desde <= periodo.hasta

  const { data, isPending, isFetching, error, refetch } = useQuery({
    queryKey: ['reportes', periodo.desde, periodo.hasta, sucursalId],
    queryFn: () => obtenerReportes(periodo.desde, periodo.hasta, sucursalId),
    enabled: periodoValido,
    placeholderData: keepPreviousData,
  })

  return (
    <section className="caja">
      <div className="caja-cabecera">
        <h1>{propio ? 'Mis reportes' : 'Reportes'}</h1>
      </div>

      <div className="caja-filtros">
        <div className="caja-presets" role="group" aria-label="Período">
          {PRESETS.map((p) => (
            <button key={p.valor} type="button" aria-pressed={preset === p.valor} onClick={() => setPreset(p.valor)}>
              {p.etiqueta}
            </button>
          ))}
        </div>
        {sucursales.length > 1 && (
          <Selector
            variante="pill"
            alinear="derecha"
            etiqueta="Sucursal"
            valor={sucursalId ?? ''}
            onChange={(v) => setSucursalId(v || null)}
            opciones={[
              { valor: '', etiqueta: 'Todas las sucursales' },
              ...sucursales.map((s) => ({ valor: s.id, etiqueta: s.nombre })),
            ]}
          />
        )}
      </div>

      {preset === 'personalizado' && (
        <div className="caja-rango">
          <label>
            Desde
            <input
              type="date"
              value={personalizado.desde}
              max={hoyISO()}
              onChange={(e) => setPersonalizado((p) => ({ ...p, desde: e.target.value }))}
            />
          </label>
          <label>
            Hasta
            <input
              type="date"
              value={personalizado.hasta}
              onChange={(e) => setPersonalizado((p) => ({ ...p, hasta: e.target.value }))}
            />
          </label>
          {!periodoValido && <p className="caja-error">La fecha "hasta" tiene que ser igual o posterior a "desde".</p>}
        </div>
      )}

      {periodoValido && isPending && <p className="caja-estado">Cargando…</p>}
      {error && (
        <div className="caja-error" role="alert">
          <p>{mensajeDeError(error)}</p>
          <button type="button" className="boton-secundario" onClick={() => refetch()}>Reintentar</button>
        </div>
      )}

      {data && (
        <div className={isFetching ? 'reportes caja-actualizando' : 'reportes'}>
          <TarjetaCobrado barberos={data.porBarbero} propio={propio} />
          <div className="reportes-fila">
            <TarjetaAusentismo ausentismo={data.ausentismo} />
            <TarjetaBarberos barberos={data.porBarbero} propio={propio} />
          </div>
          <TarjetaDemanda demanda={data.demanda} />
          <TarjetaFrecuentes clientes={data.clientesFrecuentes} propio={propio} />
        </div>
      )}

      {/* No depende del período: mira la última visita de cada cliente */}
      <ClientesQueNoVuelven propio={propio} />
    </section>
  )
}

/** Plata cobrada en el período. Para el barbero llega solo lo suyo; para el admin, todos los barberos sumados. */
function TarjetaCobrado({ barberos, propio }: { barberos: BarberoReporte[]; propio: boolean }) {
  const cobrado = barberos.reduce((suma, b) => suma + b.facturado, 0)
  const cortes = barberos.reduce((suma, b) => suma + b.atendidos, 0)
  const ticketPromedio = cortes === 0 ? 0 : Math.round(cobrado / cortes)
  return (
    <div className="reporte-tarjeta reporte-cobrado">
      <h2>{propio ? 'Lo que cobraste' : 'Total facturado'}</h2>
      {cortes === 0 ? (
        <p className="reporte-cobrado-vacio">Todavía no hay cortes cobrados en este período.</p>
      ) : (
        <>
          <p className="reporte-numero">{formatearPrecio(cobrado)}</p>
          <p className="reporte-cobrado-detalle">
            {cortes} {cortes === 1 ? 'corte' : 'cortes'} · ticket promedio {formatearPrecio(ticketPromedio)}
          </p>
        </>
      )}
    </div>
  )
}

function TarjetaAusentismo({ ausentismo }: { ausentismo: Ausentismo }) {
  const tasa = tasaAusentismo(ausentismo.atendidos, ausentismo.ausentes)
  const turnos = ausentismo.atendidos + ausentismo.ausentes
  return (
    <div className="reporte-tarjeta">
      <h2>Ausentismo</h2>
      {tasa === null ? (
        <p className="caja-estado">No hubo turnos online atendidos en este período.</p>
      ) : (
        <>
          <p className="reporte-numero">{tasa}%</p>
          <p className="reporte-detalle">
            {ausentismo.ausentes} de {turnos} {turnos === 1 ? 'turno online' : 'turnos online'} no se presentaron
            {ausentismo.cancelados > 0 &&
              ` · ${ausentismo.cancelados} ${ausentismo.cancelados === 1 ? 'canceló' : 'cancelaron'} a tiempo`}
          </p>
        </>
      )}
      {ausentismo.sinMarcar > 0 && (
        <p className="reporte-aviso">
          Hay {ausentismo.sinMarcar} {ausentismo.sinMarcar === 1 ? 'turno pasado' : 'turnos pasados'} sin marcar como
          atendido o ausente. Marcalos desde la agenda para que este número sea real.
        </p>
      )}
    </div>
  )
}

function TarjetaBarberos({ barberos, propio }: { barberos: BarberoReporte[]; propio: boolean }) {
  return (
    <div className="reporte-tarjeta">
      <h2>{propio ? 'Mis cortes' : 'Cortes por barbero'}</h2>
      {barberos.length === 0 ? (
        <p className="caja-estado">Sin turnos en este período.</p>
      ) : (
        <div className="reporte-tabla-scroll">
          <table className="reporte-tabla">
            <thead>
              <tr>
                {!propio && <th>Barbero</th>}
                <th>Atendidos</th>
                {!propio && <th>Cobrado</th>}
                <th>Ausentes</th>
                <th>Cancelados</th>
              </tr>
            </thead>
            <tbody>
              {barberos.map((b) => (
                <tr key={b.id}>
                  {!propio && <td>{b.nombre}</td>}
                  <td><strong>{b.atendidos}</strong></td>
                  {!propio && <td>{formatearPrecio(b.facturado)}</td>}
                  <td>{b.ausentes}</td>
                  <td>{b.cancelados}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function TarjetaDemanda({ demanda }: { demanda: Demanda[] }) {
  if (demanda.length === 0) {
    return (
      <div className="reporte-tarjeta">
        <h2>Días y horarios con más demanda</h2>
        <p className="caja-estado">Sin turnos en este período.</p>
      </div>
    )
  }

  const horas = demanda.map((d) => d.hora)
  const desde = Math.min(9, ...horas)
  const hasta = Math.max(20, ...horas)
  const maximo = demanda[0].cantidad
  const celdas = new Map(demanda.map((d) => [`${d.diaSemana}-${d.hora}`, d.cantidad]))
  const columnas = Array.from({ length: hasta - desde + 1 }, (_, i) => desde + i)

  return (
    <div className="reporte-tarjeta">
      <h2>Días y horarios con más demanda</h2>
      <ol className="reporte-top">
        {demanda.slice(0, 3).map((d) => (
          <li key={`${d.diaSemana}-${d.hora}`}>
            <strong>{DIAS_SEMANA[d.diaSemana - 1]} de {d.hora} a {d.hora + 1} h</strong>
            <span> · {d.cantidad} {d.cantidad === 1 ? 'turno' : 'turnos'}</span>
          </li>
        ))}
      </ol>

      <div className="reporte-tabla-scroll">
        <table className="reporte-mapa" aria-label="Turnos por día y hora">
          <thead>
            <tr>
              <th />
              {columnas.map((h) => (
                <th key={h} scope="col">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {DIAS_CORTOS.map((dia, i) => (
              <tr key={dia}>
                <th scope="row">{dia}</th>
                {columnas.map((h) => {
                  const cantidad = celdas.get(`${i + 1}-${h}`) ?? 0
                  const intensidad = cantidad === 0 ? 0 : Math.max(15, Math.round((cantidad / maximo) * 100))
                  return (
                    <td
                      key={h}
                      title={`${DIAS_SEMANA[i]} ${h} h: ${cantidad} ${cantidad === 1 ? 'turno' : 'turnos'}`}
                      style={{ background: `color-mix(in srgb, var(--acento) ${intensidad}%, transparent)` }}
                    >
                      {cantidad > 0 ? cantidad : ''}
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function TarjetaFrecuentes({ clientes, propio }: { clientes: ClienteFrecuente[]; propio: boolean }) {
  return (
    <div className="reporte-tarjeta">
      <h2>{propio ? 'Mis clientes más frecuentes' : 'Clientes más frecuentes'}</h2>
      {clientes.length === 0 ? (
        <p className="caja-estado">Todavía no hay clientes con visitas registradas en este período.</p>
      ) : (
        <ul className="reporte-lista">
          {clientes.map((c, i) => (
            <li key={c.id}>
              <span className="reporte-posicion">{i + 1}</span>
              <div>
                <strong>{c.nombre}</strong>
                <small>
                  {c.visitas} {c.visitas === 1 ? 'visita' : 'visitas'} · última el {fechaCorta(c.ultimaVisita)}
                </small>
              </div>
              <strong>{formatearPrecio(c.total)}</strong>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function ClientesQueNoVuelven({ propio }: { propio: boolean }) {
  const [dias, setDias] = useState(45)
  const { data, isPending, error } = useQuery({
    queryKey: ['reportes', 'clientes-inactivos', dias],
    queryFn: () => obtenerClientesInactivos(dias),
    placeholderData: keepPreviousData,
  })

  return (
    <div className="reporte-tarjeta reporte-inactivos">
      <div className="reporte-cabecera">
        <h2>{propio ? 'Mis clientes que no vuelven' : 'Clientes que no vuelven'}</h2>
        <label>
          Sin venir hace más de
          <Selector
            variante="compacto"
            alinear="derecha"
            etiqueta="Días sin venir"
            valor={String(dias)}
            onChange={(v) => setDias(Number(v))}
            opciones={OPCIONES_DIAS.map((d) => ({ valor: String(d), etiqueta: `${d} días` }))}
          />
        </label>
      </div>
      <p className="reporte-detalle">
        No aparecen los que ya tienen un turno reservado. Los que más veces vinieron, primero.
      </p>

      {isPending && <p className="caja-estado">Cargando…</p>}
      {error && <p className="caja-error">{mensajeDeError(error)}</p>}
      {data && data.clientes.length === 0 && (
        <p className="caja-estado">Nadie dejó de venir hace más de {dias} días. ¡Bien ahí!</p>
      )}
      {data && data.clientes.length > 0 && (
        <ul className="reporte-lista">
          {data.clientes.map((c) => (
            <li key={c.id}>
              <div>
                <strong>{c.nombre}</strong>
                <small>
                  Hace {c.diasSinVenir} días ({fechaCorta(c.ultimaVisita)}) · {c.visitas}{' '}
                  {c.visitas === 1 ? 'visita' : 'visitas'}
                </small>
              </div>
              <a
                className="boton-secundario reporte-whatsapp"
                href={linkWhatsApp(c.telefono, c.nombre, data.barberiaNombre)}
                target="_blank"
                rel="noreferrer"
              >
                Escribir por WhatsApp
              </a>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}