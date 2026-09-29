import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { descargarCaja, obtenerCaja } from '../../api/caja'
import { mensajeDeError } from '../../lib/errores'
import { aFechaLocal, hoyISO } from '../../lib/fechas'
import { formatearPrecio } from '../../lib/formato'
import type { Caja, DiaCaja } from '../../types/caja'
import { useOpcionesRegistro } from '../atenciones/useOpcionesRegistro'
import { nombreMetodo, periodoDe, type Periodo, type Preset } from './periodos'
import './caja.css'

const PRESETS: { valor: Preset; etiqueta: string }[] = [
  { valor: 'hoy', etiqueta: 'Hoy' },
  { valor: 'semana', etiqueta: 'Esta semana' },
  { valor: 'mes', etiqueta: 'Este mes' },
  { valor: 'personalizado', etiqueta: 'Otro período' },
]

export function CajaPage() {
  const [preset, setPreset] = useState<Preset>('hoy')
  const [personalizado, setPersonalizado] = useState<Periodo>(() => periodoDe('mes'))
  const [sucursalId, setSucursalId] = useState<string | null>(null)
  const [exportando, setExportando] = useState(false)
  const [errorExportar, setErrorExportar] = useState<string | null>(null)

  const { data: opciones } = useOpcionesRegistro()
  const sucursales = opciones?.sucursales ?? []

  const periodo = preset === 'personalizado' ? personalizado : periodoDe(preset)
  const periodoValido = periodo.desde !== '' && periodo.hasta !== '' && periodo.desde <= periodo.hasta
  const consulta = { ...periodo, sucursalId }

  const { data, isPending, isFetching, error, refetch } = useQuery({
    queryKey: ['caja', consulta],
    queryFn: () => obtenerCaja(consulta),
    enabled: periodoValido,
    placeholderData: keepPreviousData, // al cambiar de período no parpadea la pantalla
  })

  async function exportar() {
    setErrorExportar(null)
    setExportando(true)
    try {
      await descargarCaja(consulta)
    } catch {
      setErrorExportar('No se pudo descargar el archivo. Probá de nuevo.')
    } finally {
      setExportando(false)
    }
  }

  return (
    <section className="caja">
      <div className="caja-cabecera">
        <h1>Caja</h1>
        <button
          type="button"
          className="boton-secundario"
          onClick={exportar}
          disabled={!periodoValido || exportando || data?.cantidad === 0}
        >
          {exportando ? 'Descargando…' : 'Descargar Excel (CSV)'}
        </button>
      </div>
      {errorExportar && <p className="caja-error" role="alert">{errorExportar}</p>}

      <div className="caja-filtros">
        <div className="caja-presets" role="group" aria-label="Período">
          {PRESETS.map((p) => (
            <button
              key={p.valor}
              type="button"
              aria-pressed={preset === p.valor}
              onClick={() => setPreset(p.valor)}
            >
              {p.etiqueta}
            </button>
          ))}
        </div>

        {sucursales.length > 1 && (
          <select
            aria-label="Sucursal"
            value={sucursalId ?? ''}
            onChange={(e) => setSucursalId(e.target.value || null)}
          >
            <option value="">Todas las sucursales</option>
            {sucursales.map((s) => (
              <option key={s.id} value={s.id}>{s.nombre}</option>
            ))}
          </select>
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

      {data && <ResumenCaja caja={data} actualizando={isFetching} />}
    </section>
  )
}

function ResumenCaja({ caja, actualizando }: { caja: Caja; actualizando: boolean }) {
  if (caja.cantidad === 0) {
    return <p className="caja-estado">No hay cortes cobrados en este período.</p>
  }

  return (
    <div className={actualizando ? 'caja-resumen caja-actualizando' : 'caja-resumen'}>
      <div className="caja-totales">
        <div className="caja-total-principal">
          <span>Total facturado</span>
          <strong>{formatearPrecio(caja.total)}</strong>
        </div>
        <div>
          <span>Cortes</span>
          <strong>{caja.cantidad}</strong>
        </div>
        <div>
          <span>Ticket promedio</span>
          <strong>{formatearPrecio(caja.ticketPromedio)}</strong>
        </div>
      </div>

      {caja.porDia.length > 1 && caja.porDia.length <= 62 && <GraficoDias dias={caja.porDia} />}

      <div className="caja-desgloses">
        <Desglose
          titulo="Por barbero"
          total={caja.total}
          filas={caja.porBarbero.map((g) => ({ clave: g.id, nombre: g.nombre, cantidad: g.cantidad, total: g.total }))}
        />
        <Desglose
          titulo="Por método de pago"
          total={caja.total}
          filas={caja.porMetodoPago.map((g) => ({
            clave: g.metodo ?? 'sin-informar',
            nombre: nombreMetodo(g.metodo),
            cantidad: g.cantidad,
            total: g.total,
          }))}
        />
        <Desglose
          titulo="Por servicio"
          total={caja.total}
          filas={caja.porServicio.map((g) => ({ clave: g.id, nombre: g.nombre, cantidad: g.cantidad, total: g.total }))}
        />
      </div>
    </div>
  )
}

interface FilaDesglose {
  clave: string
  nombre: string
  cantidad: number
  total: number
}

function Desglose({ titulo, filas, total }: { titulo: string; filas: FilaDesglose[]; total: number }) {
  return (
    <div className="caja-desglose">
      <h2>{titulo}</h2>
      <ul>
        {filas.map((f) => {
          const porcentaje = total === 0 ? 0 : Math.round((f.total / total) * 100)
          return (
            <li key={f.clave}>
              <div className="caja-fila">
                <span>
                  {f.nombre} <small>· {f.cantidad} {f.cantidad === 1 ? 'corte' : 'cortes'}</small>
                </span>
                <strong>{formatearPrecio(f.total)}</strong>
              </div>
              <div className="caja-barra" aria-hidden="true">
                <div style={{ width: `${porcentaje}%` }} />
              </div>
            </li>
          )
        })}
      </ul>
    </div>
  )
}

function GraficoDias({ dias }: { dias: DiaCaja[] }) {
  const maximo = Math.max(...dias.map((d) => d.total), 1)
  return (
    <div className="caja-grafico">
      <h2>Por día</h2>
      <div className="caja-grafico-barras" role="img" aria-label="Facturación por día">
        {dias.map((d) => {
          const fecha = aFechaLocal(d.fecha)
          const etiqueta = fecha.toLocaleDateString('es-AR', { weekday: 'short', day: 'numeric' })
          return (
            <div
              key={d.fecha}
              className="caja-grafico-dia"
              title={`${etiqueta}: ${formatearPrecio(d.total)} (${d.cantidad} ${d.cantidad === 1 ? 'corte' : 'cortes'})`}
            >
              <div className="caja-grafico-columna">
                <div style={{ height: `${(d.total / maximo) * 100}%` }} />
              </div>
              <span>{fecha.getDate()}</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
