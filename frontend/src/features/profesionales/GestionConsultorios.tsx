import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cambiarActivoConsultorio,
  crearConsultorio,
  listarConsultorios,
  renombrarConsultorio,
} from '../../api/consultorios'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import { Selector } from '../../components/Selector'
import type { SucursalOpcion } from '../../types/profesional'
import { useTerminos } from '../terminos/useTerminos'

interface Props {
  /** Sucursales activas del negocio */
  sucursales: SucursalOpcion[]
}

/**
 * Consultorios de cada sucursal (rubro salud, issue #69): alta, renombre, baja y reactivación.
 * Se asignan a los tramos del horario de cada profesional; dos profesionales no pueden usar
 * el mismo consultorio a la misma hora.
 */
export function GestionConsultorios({ sucursales }: Props) {
  const t = useTerminos()
  const queryClient = useQueryClient()
  const [nuevo, setNuevo] = useState('')
  const [sucursalNuevo, setSucursalNuevo] = useState(sucursales[0]?.id ?? '')
  const [editando, setEditando] = useState<{ id: string; nombre: string } | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data, isPending, error: errorCarga } = useQuery({
    queryKey: ['consultorios'],
    queryFn: () => listarConsultorios(true),
  })

  /** Las opciones del editor de horarios y los horarios muestran los consultorios: se refrescan. */
  function refrescar() {
    setError(null)
    queryClient.invalidateQueries({ queryKey: ['consultorios'] })
    queryClient.invalidateQueries({ queryKey: ['barberos'] })
    queryClient.invalidateQueries({ queryKey: ['horarios'] })
  }

  function mostrarError(err: unknown) {
    const campos = erroresDeCampos(err)
    setError(campos.nombre ?? campos.sucursalId ?? campos.activo ?? mensajeDeError(err))
  }

  const crear = useMutation({
    mutationFn: ({ sucursalId, nombre }: { sucursalId: string; nombre: string }) => crearConsultorio(sucursalId, nombre),
    onSuccess: () => {
      setNuevo('')
      refrescar()
    },
    onError: mostrarError,
  })

  const renombrar = useMutation({
    mutationFn: ({ id, nombre }: { id: string; nombre: string }) => renombrarConsultorio(id, nombre),
    onSuccess: () => {
      setEditando(null)
      refrescar()
    },
    onError: mostrarError,
  })

  const cambiarActivo = useMutation({
    mutationFn: ({ id, activo }: { id: string; activo: boolean }) => cambiarActivoConsultorio(id, activo),
    onSuccess: refrescar,
    onError: mostrarError,
  })

  const ocupado = crear.isPending || renombrar.isPending || cambiarActivo.isPending

  function agregar() {
    if (!sucursalNuevo) {
      setError('Elegí la sucursal')
      return
    }
    if (nuevo.trim() === '') {
      setError('Escribí el nombre del consultorio')
      return
    }
    crear.mutate({ sucursalId: sucursalNuevo, nombre: nuevo.trim() })
  }

  function guardarNombre() {
    if (!editando) return
    if (editando.nombre.trim() === '') {
      setError('El nombre es obligatorio')
      return
    }
    renombrar.mutate({ id: editando.id, nombre: editando.nombre.trim() })
  }

  function nombreSucursal(id: string): string {
    return sucursales.find((s) => s.id === id)?.nombre ?? 'Sucursal inactiva'
  }

  // Agrupados por sucursal solo si hay más de una; si no, una lista sola
  const variasSucursales = sucursales.length > 1
  const porSucursal = new Map<string, NonNullable<typeof data>>()
  for (const c of data ?? []) {
    porSucursal.set(c.sucursalId, [...(porSucursal.get(c.sucursalId) ?? []), c])
  }

  return (
    <section className="especialidades-gestion" aria-labelledby="consultorios-titulo">
      <h2 id="consultorios-titulo">Consultorios</h2>
      <p className="config-ayuda">
        Si varios {t.profesional.p} comparten consultorios, cargalos acá y asignalos en el horario de cada{' '}
        {t.profesional.s}. Así nunca se dan dos turnos a la misma hora en el mismo consultorio.
      </p>

      {sucursales.length === 0 ? (
        <p className="config-estado">No hay sucursales activas.</p>
      ) : (
        <div className="especialidad-nueva">
          {variasSucursales && (
            <Selector
              variante="compacto"
              etiqueta="Sucursal del consultorio nuevo"
              valor={sucursalNuevo}
              onChange={(v) => {
                setSucursalNuevo(v)
                setError(null)
              }}
              opciones={sucursales.map((s) => ({ valor: s.id, etiqueta: s.nombre }))}
            />
          )}
          <input
            type="text"
            value={nuevo}
            maxLength={80}
            placeholder="Nuevo consultorio (ej. Consultorio 2)"
            aria-label="Nuevo consultorio"
            onChange={(e) => {
              setNuevo(e.target.value)
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
      )}

      {error && <p className="config-error" role="alert">{error}</p>}
      {isPending && <p className="config-estado">Cargando…</p>}
      {errorCarga && <p className="config-error" role="alert">{mensajeDeError(errorCarga)}</p>}
      {data && data.length === 0 && <p className="config-estado">Todavía no hay consultorios cargados.</p>}

      {[...porSucursal.entries()].map(([sucursalId, lista]) => (
        <div key={sucursalId}>
          {variasSucursales && <h3 className="config-ayuda">{nombreSucursal(sucursalId)}</h3>}
          <ul className="especialidades-lista">
            {lista.map((c) => (
              <li key={c.id}>
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
                      {!c.activo && <span className="badge badge-inactivo">Inactivo</span>}
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
                      onClick={() => {
                        setError(null)
                        cambiarActivo.mutate({ id: c.id, activo: !c.activo })
                      }}
                    >
                      {c.activo ? 'Desactivar' : 'Reactivar'}
                    </button>
                  </>
                )}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </section>
  )
}
