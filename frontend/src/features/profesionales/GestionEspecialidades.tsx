import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { borrarEspecialidad, crearEspecialidad, listarEspecialidades, renombrarEspecialidad } from '../../api/especialidades'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import { useTerminos } from '../terminos/useTerminos'

/**
 * Lista de especialidades del negocio (rubro salud, issue #68): alta, renombre y baja.
 * Se asignan a cada profesional desde su ficha.
 */
export function GestionEspecialidades() {
  const t = useTerminos()
  const queryClient = useQueryClient()
  const [nueva, setNueva] = useState('')
  const [editando, setEditando] = useState<{ id: string; nombre: string } | null>(null)
  const [confirmandoBorrar, setConfirmandoBorrar] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data, isPending, error: errorCarga } = useQuery({
    queryKey: ['especialidades'],
    queryFn: listarEspecialidades,
  })

  /** Las opciones del formulario y la lista de profesionales muestran los nombres: se refrescan. */
  function refrescar() {
    setError(null)
    queryClient.invalidateQueries({ queryKey: ['especialidades'] })
    queryClient.invalidateQueries({ queryKey: ['barberos'] })
    queryClient.invalidateQueries({ queryKey: ['barbero'] })
  }

  function mostrarError(err: unknown) {
    setError(erroresDeCampos(err).nombre ?? mensajeDeError(err))
  }

  const crear = useMutation({
    mutationFn: (nombre: string) => crearEspecialidad(nombre),
    onSuccess: () => {
      setNueva('')
      refrescar()
    },
    onError: mostrarError,
  })

  const renombrar = useMutation({
    mutationFn: ({ id, nombre }: { id: string; nombre: string }) => renombrarEspecialidad(id, nombre),
    onSuccess: () => {
      setEditando(null)
      refrescar()
    },
    onError: mostrarError,
  })

  const borrar = useMutation({
    mutationFn: (id: string) => borrarEspecialidad(id),
    onSuccess: () => {
      setConfirmandoBorrar(null)
      refrescar()
    },
    onError: mostrarError,
  })

  const ocupado = crear.isPending || renombrar.isPending || borrar.isPending

  function agregar() {
    if (nueva.trim() === '') {
      setError('Escribí el nombre de la especialidad')
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
    <section className="especialidades-gestion" aria-labelledby="especialidades-titulo">
      <h2 id="especialidades-titulo">Especialidades</h2>
      <p className="config-ayuda">
        Los {t.cliente.p} pueden reservar eligiendo una especialidad, sin conocer el nombre {t.profesional.del}.
        Asignalas desde la ficha de cada {t.profesional.s}.
      </p>

      <div className="especialidad-nueva">
        <input
          type="text"
          value={nueva}
          maxLength={80}
          placeholder="Nueva especialidad (ej. Kinesiología)"
          aria-label="Nueva especialidad"
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
      {data && data.length === 0 && <p className="config-estado">Todavía no hay especialidades cargadas.</p>}

      {data && data.length > 0 && (
        <ul className="especialidades-lista">
          {data.map((e) => (
            <li key={e.id}>
              {editando?.id === e.id ? (
                <>
                  <input
                    type="text"
                    value={editando.nombre}
                    maxLength={80}
                    aria-label={`Nuevo nombre para ${e.nombre}`}
                    autoFocus
                    onChange={(ev) => setEditando({ id: e.id, nombre: ev.target.value })}
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
              ) : confirmandoBorrar === e.id ? (
                <>
                  <span role="alert">¿Borrar «{e.nombre}»? Se les quita a quienes la tengan.</span>
                  <button
                    type="button"
                    className="boton-secundario"
                    disabled={ocupado}
                    onClick={() => borrar.mutate(e.id)}
                  >
                    Sí, borrar
                  </button>
                  <button type="button" className="boton-texto" onClick={() => setConfirmandoBorrar(null)}>
                    Cancelar
                  </button>
                </>
              ) : (
                <>
                  <span>{e.nombre}</span>
                  <button
                    type="button"
                    className="boton-texto"
                    onClick={() => {
                      setError(null)
                      setConfirmandoBorrar(null)
                      setEditando({ id: e.id, nombre: e.nombre })
                    }}
                  >
                    Renombrar
                  </button>
                  <button
                    type="button"
                    className="boton-texto"
                    onClick={() => {
                      setError(null)
                      setEditando(null)
                      setConfirmandoBorrar(e.id)
                    }}
                  >
                    Borrar
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
