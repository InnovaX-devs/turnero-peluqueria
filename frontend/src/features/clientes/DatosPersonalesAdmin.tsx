import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { anonimizarCliente, exportarDatosCliente, obtenerAccesosCliente } from '../../api/clientes'
import { mensajeDeError } from '../../lib/errores'
import type { AccionPaciente, FichaCliente } from '../../types/cliente'
import { useTerminos } from '../terminos/useTerminos'

const ETIQUETA_ACCION: Record<AccionPaciente, string> = {
  VER_FICHA: 'Abrió la ficha',
  EDITAR_NOTAS: 'Editó las notas',
  EDITAR_COBERTURA: 'Editó la obra social',
  EXPORTAR: 'Exportó los datos',
  ANONIMIZAR: 'Borró los datos personales',
}

function formatearFecha(iso: string): string {
  return new Date(iso).toLocaleString('es-AR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** Baja el JSON como archivo, sin pasar por el navegador a otra URL (el token va en el header, no en un link). */
function descargarJson(nombreArchivo: string, datos: unknown) {
  const blob = new Blob([JSON.stringify(datos, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombreArchivo
  enlace.click()
  URL.revokeObjectURL(url)
}

/**
 * Issue #73 (solo admin): derechos del titular de los datos (Ley 25.326) y registro de accesos a la ficha.
 * Exportar = derecho de acceso. Borrar datos = derecho de supresión: se anonimiza, los turnos quedan sin identificar.
 */
export function DatosPersonalesAdmin({ cliente }: { cliente: FichaCliente }) {
  const t = useTerminos()
  const queryClient = useQueryClient()
  const [verAccesos, setVerAccesos] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const anonimizado = Boolean(cliente.anonimizadoEn)

  const accesos = useQuery({
    queryKey: ['clientes', 'accesos', cliente.id],
    queryFn: () => obtenerAccesosCliente(cliente.id),
    enabled: verAccesos,
  })

  const exportar = useMutation({
    mutationFn: () => exportarDatosCliente(cliente.id),
    onSuccess: (datos) => {
      descargarJson(`datos-${t.cliente.s}-${cliente.id}.json`, datos)
      queryClient.invalidateQueries({ queryKey: ['clientes', 'accesos', cliente.id] })
    },
  })

  const borrar = useMutation({
    mutationFn: () => anonimizarCliente(cliente.id),
    onSuccess: () => {
      setConfirmando(false)
      queryClient.invalidateQueries({ queryKey: ['clientes'] })
      queryClient.invalidateQueries({ queryKey: ['agenda'] })
    },
  })

  return (
    <section className="ficha-seccion ficha-privacidad">
      <h2>Datos personales</h2>

      {anonimizado ? (
        <p className="clientes-estado">
          Los datos personales de {t.cliente.el} se borraron el {formatearFecha(cliente.anonimizadoEn as string)}.
          Sus turnos y cobros se conservan sin identificarlo.
        </p>
      ) : (
        <div className="ficha-privacidad-acciones">
          <button
            type="button"
            className="boton-secundario"
            disabled={exportar.isPending}
            onClick={() => exportar.mutate()}
          >
            {exportar.isPending ? 'Preparando…' : 'Exportar sus datos'}
          </button>

          {!confirmando ? (
            <button type="button" className="boton-secundario ficha-peligro" onClick={() => setConfirmando(true)}>
              Borrar sus datos personales
            </button>
          ) : (
            <div className="ficha-privacidad-confirmar" role="alert">
              <p>
                Se borran el nombre, el teléfono, el DNI, la obra social y todas las notas de {t.cliente.el}. Sus
                turnos y cobros quedan en la caja pero sin identificarlo.{' '}
                <strong>No se puede deshacer.</strong>
              </p>
              <div className="ficha-privacidad-botones">
                <button
                  type="button"
                  className="boton-secundario ficha-peligro"
                  disabled={borrar.isPending}
                  onClick={() => borrar.mutate()}
                >
                  {borrar.isPending ? 'Borrando…' : 'Sí, borrar los datos'}
                </button>
                <button
                  type="button"
                  className="boton-texto"
                  disabled={borrar.isPending}
                  onClick={() => {
                    setConfirmando(false)
                    borrar.reset()
                  }}
                >
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {exportar.isError && (
        <p className="clientes-error" role="alert">{mensajeDeError(exportar.error)}</p>
      )}
      {borrar.isError && (
        <p className="clientes-error" role="alert">{mensajeDeError(borrar.error)}</p>
      )}

      <button type="button" className="boton-texto ficha-privacidad-toggle" onClick={() => setVerAccesos((v) => !v)}>
        {verAccesos ? 'Ocultar quién vio esta ficha' : 'Ver quién vio esta ficha'}
      </button>

      {verAccesos && (
        <>
          {accesos.isPending && <p className="clientes-estado">Cargando…</p>}
          {accesos.isError && <p className="clientes-error" role="alert">{mensajeDeError(accesos.error)}</p>}
          {accesos.data && accesos.data.length === 0 && <p className="clientes-estado">Todavía no hay accesos registrados.</p>}
          {accesos.data && accesos.data.length > 0 && (
            <ul className="ficha-accesos">
              {accesos.data.map((a, i) => (
                <li key={`${a.fecha}-${i}`}>
                  <span>{ETIQUETA_ACCION[a.accion]}</span>
                  <small>{a.usuarioEmail} · {formatearFecha(a.fecha)}</small>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}
