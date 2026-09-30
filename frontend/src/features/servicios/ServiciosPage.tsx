import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cambiarActivoServicio, listarServicios, reordenarServicios } from '../../api/servicios'
import { mensajeDeError } from '../../lib/errores'
import { formatearDuracion, formatearPrecio } from '../../lib/formato'
import type { Servicio } from '../../types/servicio'
import { FormularioServicio } from './FormularioServicio'
import { CLAVE_LISTA_SERVICIOS, useInvalidarServicios } from './servicios'
import '../admin/configuracion.css'
import '../barberos/barberos.css'
import './servicios.css'

export function ServiciosPage() {
  const queryClient = useQueryClient()
  const invalidar = useInvalidarServicios()
  // 'nuevo' = formulario de alta; un id = editando ese servicio
  const [formulario, setFormulario] = useState<string | null>(null)
  const [verInactivos, setVerInactivos] = useState(false)
  const [errorAccion, setErrorAccion] = useState<string | null>(null)

  const { data, isPending, error, refetch } = useQuery({
    queryKey: CLAVE_LISTA_SERVICIOS,
    queryFn: () => listarServicios(true),
  })

  const cambiarActivo = useMutation({
    mutationFn: ({ id, activo }: { id: string; activo: boolean }) => cambiarActivoServicio(id, activo),
    onSuccess: () => {
      setErrorAccion(null)
      invalidar()
    },
    onError: (err) => setErrorAccion(mensajeDeError(err)),
  })

  const reordenar = useMutation({
    mutationFn: reordenarServicios,
    onSuccess: (lista) => {
      setErrorAccion(null)
      queryClient.setQueryData(CLAVE_LISTA_SERVICIOS, lista)
      invalidar()
    },
    onError: (err) => {
      setErrorAccion(mensajeDeError(err))
      invalidar()
    },
  })

  const activos = (data ?? []).filter((s) => s.activo)
  const inactivos = (data ?? []).filter((s) => !s.activo)
  const ocupado = cambiarActivo.isPending || reordenar.isPending

  function mover(indice: number, delta: -1 | 1) {
    const ids = activos.map((s) => s.id)
    const destino = indice + delta
    if (destino < 0 || destino >= ids.length) return
    ;[ids[indice], ids[destino]] = [ids[destino], ids[indice]]
    reordenar.mutate(ids)
  }

  function darDeBaja(s: Servicio) {
    if (
      window.confirm(
        `¿Dar de baja "${s.nombre}"? Deja de mostrarse para reservar y para registrar cortes. Las atenciones ya registradas se conservan.`,
      )
    ) {
      cambiarActivo.mutate({ id: s.id, activo: false })
    }
  }

  function datos(s: Servicio) {
    return (
      <span className="barbero-detalle-linea">
        {formatearDuracion(s.duracionMin)} · {formatearPrecio(s.precio)}
      </span>
    )
  }

  function tarjetaInactiva(s: Servicio) {
    if (formulario === s.id) {
      return (
        <li key={s.id}>
          <FormularioServicio servicio={s} onGuardado={() => setFormulario(null)} onCancelar={() => setFormulario(null)} />
        </li>
      )
    }
    return (
      <li key={s.id} className="barbero-tarjeta barbero-inactivo">
        <div className="barbero-info">
          <strong>
            {s.nombre}
            <span className="badge badge-inactivo">Inactivo</span>
          </strong>
          {datos(s)}
        </div>
        <div className="barbero-acciones">
          <button type="button" className="boton-secundario" onClick={() => setFormulario(s.id)}>
            Editar
          </button>
          <button
            type="button"
            className="boton-texto"
            disabled={ocupado}
            title="Vuelve a aparecer al final de la lista"
            onClick={() => cambiarActivo.mutate({ id: s.id, activo: true })}
          >
            Reactivar
          </button>
        </div>
      </li>
    )
  }

  return (
    <section className="barberos servicios">
      <div className="barberos-cabecera">
        <h1>Servicios</h1>
        {formulario !== 'nuevo' && (
          <button type="button" className="boton-secundario" onClick={() => setFormulario('nuevo')}>
            + Nuevo servicio
          </button>
        )}
      </div>

      {formulario === 'nuevo' && (
        <FormularioServicio onGuardado={() => setFormulario(null)} onCancelar={() => setFormulario(null)} />
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

      {data && activos.length === 0 && formulario !== 'nuevo' && (
        <p className="config-estado">Todavía no hay servicios activos.</p>
      )}

      {activos.length > 0 && (
        <>
          <p className="config-ayuda servicios-ayuda">
            Este es el orden en que se muestran en la página de reservas. Usá las flechas para cambiarlo.
          </p>
          <ul className="barberos-lista">
            {activos.map((s, i) =>
              formulario === s.id ? (
                <li key={s.id}>
                  <FormularioServicio
                    servicio={s}
                    onGuardado={() => setFormulario(null)}
                    onCancelar={() => setFormulario(null)}
                  />
                </li>
              ) : (
                <li key={s.id} className="barbero-tarjeta">
                  <div className="servicio-orden">
                    <button
                      type="button"
                      aria-label={`Subir ${s.nombre}`}
                      disabled={i === 0 || ocupado}
                      onClick={() => mover(i, -1)}
                    >
                      ▲
                    </button>
                    <button
                      type="button"
                      aria-label={`Bajar ${s.nombre}`}
                      disabled={i === activos.length - 1 || ocupado}
                      onClick={() => mover(i, 1)}
                    >
                      ▼
                    </button>
                  </div>
                  <div className="barbero-info">
                    <strong>{s.nombre}</strong>
                    {datos(s)}
                  </div>
                  <div className="barbero-acciones">
                    <button type="button" className="boton-secundario" onClick={() => setFormulario(s.id)}>
                      Editar
                    </button>
                    <button type="button" className="boton-texto" disabled={ocupado} onClick={() => darDeBaja(s)}>
                      Dar de baja
                    </button>
                  </div>
                </li>
              ),
            )}
          </ul>
        </>
      )}

      {inactivos.length > 0 && (
        <>
          <label className="barberos-filtro servicios-filtro">
            <input type="checkbox" checked={verInactivos} onChange={(e) => setVerInactivos(e.target.checked)} />
            Mostrar dados de baja ({inactivos.length})
          </label>
          {verInactivos && <ul className="barberos-lista">{inactivos.map(tarjetaInactiva)}</ul>}
        </>
      )}
    </section>
  )
}