import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cambiarActivoProfesional, listarProfesionales, obtenerOpcionesProfesional } from '../../api/profesionales'
import { mensajeDeError } from '../../lib/errores'
import { Avatar } from './FotoProfesional'
import { GestionEspecialidades } from './GestionEspecialidades'
import { GestionConsultorios } from './GestionConsultorios'
import '../admin/configuracion.css'
import './profesionales.css'
import { useTerminos } from '../terminos/useTerminos'

export function ProfesionalesPage() {
  const t = useTerminos()
  const queryClient = useQueryClient()
  const [verInactivos, setVerInactivos] = useState(false)
  const [confirmandoBaja, setConfirmandoBaja] = useState<string | null>(null)
  const [errorAccion, setErrorAccion] = useState<string | null>(null)

  const { data, isPending, error, refetch } = useQuery({
    queryKey: ['barberos', 'lista'],
    queryFn: () => listarProfesionales(true),
  })

  // Rubro salud (issue #68): matrícula y especialidades en la tarjeta. La lista trae ids: los nombres salen de las opciones
  const esSalud = t.rubro === 'SALUD'
  const opciones = useQuery({
    queryKey: ['barberos', 'opciones'],
    queryFn: obtenerOpcionesProfesional,
    enabled: esSalud,
  })
  function nombresEspecialidades(ids: string[]): string {
    return (opciones.data?.especialidades ?? [])
      .filter((e) => ids.includes(e.id))
      .map((e) => e.nombre)
      .join(' · ')
  }

  const cambiarActivo = useMutation({
    mutationFn: ({ id, activo }: { id: string; activo: boolean }) => cambiarActivoProfesional(id, activo),
    onSuccess: (b) => {
      setErrorAccion(null)
      queryClient.setQueryData(['barbero', b.id], b)
      queryClient.invalidateQueries({ queryKey: ['barberos', 'lista'] })
    },
    onError: (err) => setErrorAccion(mensajeDeError(err)),
  })

  function confirmarBaja(id: string) {
    setConfirmandoBaja(null)
    cambiarActivo.mutate({ id, activo: false })
  }

  const visibles = (data ?? []).filter((b) => verInactivos || b.activo)
  const hayInactivos = (data ?? []).some((b) => !b.activo)

  return (
    <section className="profesionales">
      <div className="profesionales-cabecera">
        <h1>{t.profesional.P}</h1>
        <Link to="/panel/profesionales/nuevo" className="boton-secundario">
          + Nuev{t.profesional.o} {t.profesional.s}
        </Link>
      </div>

      {hayInactivos && (
        <label className="profesionales-filtro">
          <input type="checkbox" checked={verInactivos} onChange={(e) => setVerInactivos(e.target.checked)} />
          Mostrar inactivos
        </label>
      )}

      {isPending && <p className="config-estado">Cargando…</p>}
      {error && (
        <div className="config-error" role="alert">
          <p>{mensajeDeError(error)}</p>
          <button type="button" className="boton-secundario" onClick={() => refetch()}>
            Reintentar
          </button>
        </div>
      )}
      {errorAccion && <p className="config-error" role="alert">{errorAccion}</p>}

      {data && visibles.length === 0 && <p className="config-estado">Todavía no hay {t.profesional.p} cargad{t.profesional.o}s.</p>}

      <ul className="profesionales-lista">
        {visibles.map((b) => (
          <li
            key={b.id}
            className={
              b.activo
                ? 'profesional-tarjeta profesional-confirmable'
                : 'profesional-tarjeta profesional-confirmable profesional-inactivo'
            }
          >
            <Avatar profesional={b} />
            <div className="profesional-info">
              <strong>
                {b.nombre} {b.apellido}
              </strong>
              <span className="profesional-detalle-linea">
                {b.sucursalIds.length} sucursal(es) · {b.servicioIds.length} {t.servicio.cantidad(b.servicioIds.length)}
                {b.usuarioEmail ? ' · con acceso' : ''}
              </span>
              {esSalud && (b.matricula || nombresEspecialidades(b.especialidadIds)) && (
                <span className="profesional-detalle-linea">
                  {[nombresEspecialidades(b.especialidadIds), b.matricula && `Matrícula ${b.matricula}`]
                    .filter(Boolean)
                    .join(' · ')}
                </span>
              )}
              {!b.activo && <span className="badge badge-inactivo">Inactivo</span>}
            </div>
            {confirmandoBaja !== b.id && (
              <div className="profesional-acciones">
                <Link to={`/panel/profesionales/${b.id}`} className="boton-secundario">
                  Editar
                </Link>
                {b.activo ? (
                  <button
                    type="button"
                    className="boton-texto"
                    disabled={cambiarActivo.isPending}
                    onClick={() => setConfirmandoBaja(b.id)}
                  >
                    Desactivar
                  </button>
                ) : (
                  <button
                    type="button"
                    className="boton-texto"
                    disabled={cambiarActivo.isPending}
                    onClick={() => cambiarActivo.mutate({ id: b.id, activo: true })}
                  >
                    Reactivar
                  </button>
                )}
              </div>
            )}
            {confirmandoBaja === b.id && (
              <div className="profesional-confirmacion" role="alert">
                <span>
                  {b.usuarioEmail ? 'También va a dejar de poder entrar al panel. ' : ''}Su historial se conserva.
                </span>
                <div className="profesional-acciones">
                  <button
                    type="button"
                    className="boton-secundario profesional-baja-confirmar"
                    disabled={cambiarActivo.isPending}
                    onClick={() => confirmarBaja(b.id)}
                  >
                    Sí, desactivar
                  </button>
                  <button type="button" className="boton-texto" onClick={() => setConfirmandoBaja(null)}>
                    Cancelar
                  </button>
                </div>
              </div>
            )}
          </li>
        ))}
      </ul>

      {esSalud && <GestionEspecialidades />}
      {esSalud && opciones.data && <GestionConsultorios sucursales={opciones.data.sucursales} />}
    </section>
  )
}