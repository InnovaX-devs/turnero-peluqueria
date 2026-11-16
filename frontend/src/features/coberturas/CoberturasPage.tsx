import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cambiarCoberturaActiva,
  cargarCoberturasComunes,
  crearCobertura,
  listarCoberturas,
  renombrarCobertura,
  reporteCoberturas,
} from '../../api/coberturas'
import { Selector } from '../../components/Selector'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import { formatearPrecio } from '../../lib/formato'
import { ETIQUETA_TIPO_PAGO, type Cobertura, type ReporteCoberturas } from '../../types/cobertura'
import { periodoDe, type Periodo } from '../caja/periodos'
import { useTerminos } from '../terminos/useTerminos'
import '../caja/caja.css'
import '../profesionales/profesionales.css'
import './coberturas.css'

/**
 * Obras sociales y prepagas (issue #67, rubro salud). Solo admin.
 * Arriba el ABM (con la carga de las más comunes); abajo el reporte para facturar a mano a cada una.
 */
export function CoberturasPage() {
  return (
    <section className="coberturas">
      <h1>Obras sociales y prepagas</h1>
      <GestionCoberturas />
      <ReporteFacturacion />
    </section>
  )
}

function GestionCoberturas() {
  const t = useTerminos()
  const queryClient = useQueryClient()
  const [nueva, setNueva] = useState('')
  const [editando, setEditando] = useState<{ id: string; nombre: string } | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data, isPending, error: errorCarga } = useQuery({
    queryKey: ['coberturas'],
    queryFn: () => listarCoberturas(),
  })

  /** Las opciones de los profesionales y las fichas de pacientes muestran los nombres: se refrescan. */
  function refrescar() {
    setError(null)
    queryClient.invalidateQueries({ queryKey: ['coberturas'] })
    queryClient.invalidateQueries({ queryKey: ['barberos'] })
    queryClient.invalidateQueries({ queryKey: ['barbero'] })
  }

  function mostrarError(err: unknown) {
    setError(erroresDeCampos(err).nombre ?? mensajeDeError(err))
  }

  const crear = useMutation({
    mutationFn: (nombre: string) => crearCobertura(nombre),
    onSuccess: () => {
      setNueva('')
      refrescar()
    },
    onError: mostrarError,
  })

  const renombrar = useMutation({
    mutationFn: ({ id, nombre }: { id: string; nombre: string }) => renombrarCobertura(id, nombre),
    onSuccess: () => {
      setEditando(null)
      refrescar()
    },
    onError: mostrarError,
  })

  const activar = useMutation({
    mutationFn: ({ id, activa }: { id: string; activa: boolean }) => cambiarCoberturaActiva(id, activa),
    onSuccess: refrescar,
    onError: mostrarError,
  })

  const comunes = useMutation({
    mutationFn: cargarCoberturasComunes,
    onSuccess: refrescar,
    onError: mostrarError,
  })

  const ocupado = crear.isPending || renombrar.isPending || activar.isPending || comunes.isPending

  function agregar() {
    if (nueva.trim() === '') {
      setError('Escribí el nombre de la obra social o prepaga')
      return
    }
    crear.mutate(nueva.trim())
  }

  function guardarNombre() {
    if (!editando) return
    if (editando.nombre.trim() === '') {
      setError('El nombre es obligatorio')
      return
    }
    renombrar.mutate({ id: editando.id, nombre: editando.nombre.trim() })
  }

  return (
    <section className="coberturas-bloque" aria-labelledby="coberturas-lista-titulo">
      <div className="coberturas-cabecera">
        <h2 id="coberturas-lista-titulo">Las que trabajás</h2>
        <button type="button" className="boton-secundario" disabled={ocupado} onClick={() => comunes.mutate()}>
          {comunes.isPending ? 'Cargando…' : 'Cargar las más comunes'}
        </button>
      </div>
      <p className="config-ayuda">
        Los {t.cliente.p} eligen su obra social al reservar y solo ven a quienes la atienden. Asignalas desde la ficha
        de cada {t.profesional.s}. Las que dejes de trabajar, desactivalas: no se borran para no perder el historial.
      </p>

      <div className="especialidad-nueva">
        <input
          type="text"
          value={nueva}
          maxLength={80}
          placeholder="Nueva obra social o prepaga (ej. OSDE)"
          aria-label="Nueva obra social o prepaga"
          onChange={(e) => {
            setNueva(e.target.value)
            setError(null)
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              agregar()
            }
          }}
        />
        <button type="button" className="boton-secundario" disabled={ocupado} onClick={agregar}>
          Agregar
        </button>
      </div>

      {error && <p className="config-error" role="alert">{error}</p>}
      {isPending && <p className="config-estado">Cargando…</p>}
      {errorCarga && <p className="config-error" role="alert">{mensajeDeError(errorCarga)}</p>}
      {data && data.length === 0 && (
        <p className="config-estado">Todavía no cargaste ninguna. Probá con «Cargar las más comunes».</p>
      )}

      {data && data.length > 0 && (
        <ul className="especialidades-lista">
          {data.map((c) => (
            <li key={c.id} className={c.activa ? undefined : 'cobertura-inactiva'}>
              {editando?.id === c.id ? (
                <>
                  <input
                    type="text"
                    value={editando.nombre}
                    maxLength={80}
                    aria-label={`Nuevo nombre para ${c.nombre}`}
                    autoFocus
                    onChange={(ev) => setEditando({ id: c.id, nombre: ev.target.value })}
                    onKeyDown={(ev) => {
                      if (ev.key === 'Enter') {
                        ev.preventDefault()
                        guardarNombre()
                      } else if (ev.key === 'Escape') {
                        setEditando(null)
                      }
                    }}
                  />
                  <button type="button" className="boton-secundario" disabled={ocupado} onClick={guardarNombre}>
                    Guardar
                  </button>
                  <button type="button" className="boton-texto" onClick={() => setEditando(null)}>
                    Cancelar
                  </button>
                </>
              ) : (
                <>
                  <span>
                    {c.nombre}
                    {!c.activa && <small className="cobertura-etiqueta">Desactivada</small>}
                  </span>
                  <button
                    type="button"
                    className="boton-texto"
                    onClick={() => {
                      setError(null)
                      setEditando({ id: c.id, nombre: c.nombre })
                    }}
                  >
                    Renombrar
                  </button>
                  <button
                    type="button"
                    className="boton-texto"
                    disabled={ocupado}
                    onClick={() => activar.mutate({ id: c.id, activa: !c.activa })}
                  >
                    {c.activa ? 'Desactivar' : 'Activar'}
                  </button>
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function ReporteFacturacion() {
  const [periodo, setPeriodo] = useState<Periodo>(() => periodoDe('mes'))
  const [coberturaId, setCoberturaId] = useState('')

  const { data: coberturas } = useQuery({ queryKey: ['coberturas'], queryFn: () => listarCoberturas() })
  const valido = periodo.desde !== '' && periodo.hasta !== '' && periodo.desde <= periodo.hasta

  const { data, isPending, isFetching, error, refetch } = useQuery({
    queryKey: ['coberturas', 'reporte', periodo, coberturaId],
    queryFn: () => reporteCoberturas(periodo.desde, periodo.hasta, coberturaId || undefined),
    enabled: valido,
    placeholderData: keepPreviousData,
  })

  return (
    <section className="coberturas-bloque" aria-labelledby="coberturas-reporte-titulo">
      <div className="coberturas-cabecera">
        <h2 id="coberturas-reporte-titulo">Para facturar</h2>
        <button
          type="button"
          className="boton-secundario"
          disabled={!data || data.cantidad === 0}
          onClick={() => data && descargarCsv(data)}
        >
          Descargar Excel (CSV)
        </button>
      </div>
      <p className="config-ayuda">Atenciones completadas por obra social o prepaga en el período.</p>

      <div className="caja-rango">
        <label>
          Desde
          <input
            type="date"
            value={periodo.desde}
            onChange={(e) => setPeriodo((p) => ({ ...p, desde: e.target.value }))}
          />
        </label>
        <label>
          Hasta
          <input
            type="date"
            value={periodo.hasta}
            onChange={(e) => setPeriodo((p) => ({ ...p, hasta: e.target.value }))}
          />
        </label>
        <Selector
          variante="pill"
          etiqueta="Obra social"
          valor={coberturaId}
          onChange={setCoberturaId}
          opciones={[
            { valor: '', etiqueta: 'Todas' },
            ...(coberturas ?? []).map((c: Cobertura) => ({ valor: c.id, etiqueta: c.nombre })),
          ]}
        />
        {!valido && <p className="caja-error">La fecha "hasta" tiene que ser igual o posterior a "desde".</p>}
      </div>

      {valido && isPending && <p className="caja-estado">Cargando…</p>}
      {error && (
        <div className="caja-error" role="alert">
          <p>{mensajeDeError(error)}</p>
          <button type="button" className="boton-secundario" onClick={() => refetch()}>Reintentar</button>
        </div>
      )}
      {data && <Reporte reporte={data} actualizando={isFetching} />}
    </section>
  )
}

function Reporte({ reporte, actualizando }: { reporte: ReporteCoberturas; actualizando: boolean }) {
  if (reporte.cantidad === 0) {
    return <p className="caja-estado">No hay atenciones por obra social en este período.</p>
  }
  return (
    <div className={actualizando ? 'caja-resumen caja-actualizando' : 'caja-resumen'}>
      <div className="caja-totales">
        <div className="caja-total-principal">
          <span>Atenciones</span>
          <strong>{reporte.cantidad}</strong>
        </div>
        <div>
          <span>Valor de las prestaciones</span>
          <strong>{formatearPrecio(reporte.totalPrestaciones)}</strong>
        </div>
        <div>
          <span>Copagos cobrados</span>
          <strong>{formatearPrecio(reporte.totalCopagos)}</strong>
        </div>
      </div>

      <div className="coberturas-tabla-envoltorio">
        <table className="coberturas-tabla">
          <caption>Por obra social</caption>
          <thead>
            <tr>
              <th scope="col">Obra social</th>
              <th scope="col" className="numero">Atenciones</th>
              <th scope="col" className="numero">Prestaciones</th>
              <th scope="col" className="numero">Copagos</th>
            </tr>
          </thead>
          <tbody>
            {reporte.porCobertura.map((r) => (
              <tr key={r.coberturaId}>
                <th scope="row">{r.nombre}</th>
                <td className="numero">{r.cantidad}</td>
                <td className="numero">{formatearPrecio(r.totalPrestaciones)}</td>
                <td className="numero">{formatearPrecio(r.totalCopagos)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="coberturas-tabla-envoltorio">
        <table className="coberturas-tabla">
          <caption>Detalle</caption>
          <thead>
            <tr>
              <th scope="col">Fecha</th>
              <th scope="col">Obra social</th>
              <th scope="col">Paciente</th>
              <th scope="col">Afiliado</th>
              <th scope="col">Profesional</th>
              <th scope="col">Prestación</th>
              <th scope="col">Autorización</th>
              <th scope="col" className="numero">Copago</th>
            </tr>
          </thead>
          <tbody>
            {reporte.detalle.map((d) => (
              <tr key={d.atencionId}>
                <td>{fechaHora(d.fecha)}</td>
                <td>{d.coberturaNombre}</td>
                <td>
                  {d.pacienteNombre ?? '—'}
                  {d.pacienteDni && <small> · DNI {d.pacienteDni}</small>}
                </td>
                <td>
                  {d.numeroAfiliado ?? '—'}
                  {d.plan && <small> · {d.plan}</small>}
                </td>
                <td>
                  {d.profesional}
                  {d.matricula && <small> · {d.matricula}</small>}
                </td>
                <td>{d.prestacion}</td>
                <td>{d.numeroAutorizacion ?? '—'}</td>
                <td className="numero">{d.copago != null ? formatearPrecio(d.copago) : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

/** "2026-11-17T10:00:00" → "17/11 10:00" */
function fechaHora(iso: string): string {
  const [fecha, hora] = iso.split('T')
  const [, mes, dia] = fecha.split('-')
  return `${dia}/${mes} ${hora.slice(0, 5)}`
}

/** CSV con ; (lo abre bien Excel en español), con BOM para los acentos. */
function descargarCsv(r: ReporteCoberturas) {
  const pesos = (c: number | null) => (c == null ? '' : (c / 100).toFixed(2).replace('.', ','))
  const celda = (v: string | null) => `"${(v ?? '').replaceAll('"', '""')}"`
  const filas = [
    ['Fecha', 'Obra social', 'Tipo de pago', 'Paciente', 'DNI', 'Nº afiliado', 'Plan', 'Profesional', 'Matrícula',
      'Prestación', 'Nº autorización', 'Valor prestación', 'Copago'].map(celda).join(';'),
    ...r.detalle.map((d) =>
      [
        celda(d.fecha.replace('T', ' ').slice(0, 16)),
        celda(d.coberturaNombre),
        celda(ETIQUETA_TIPO_PAGO[d.tipoPago]),
        celda(d.pacienteNombre),
        celda(d.pacienteDni),
        celda(d.numeroAfiliado),
        celda(d.plan),
        celda(d.profesional),
        celda(d.matricula),
        celda(d.prestacion),
        celda(d.numeroAutorizacion),
        pesos(d.precio),
        pesos(d.copago),
      ].join(';'),
    ),
  ]
  const blob = new Blob(['﻿' + filas.join('\r\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `obras-sociales_${r.desde}_${r.hasta}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
