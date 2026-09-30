import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cambiarActivoBarbero, listarBarberos } from '../../api/barberos'
import { mensajeDeError } from '../../lib/errores'
import type { Barbero } from '../../types/barbero'
import { Avatar } from './FotoBarbero'
import '../admin/configuracion.css'
import './barberos.css'

export function BarberosPage() {
  const queryClient = useQueryClient()
  const [verInactivos, setVerInactivos] = useState(false)
  const [errorAccion, setErrorAccion] = useState<string | null>(null)

  const { data, isPending, error, refetch } = useQuery({
    queryKey: ['barberos', 'lista'],
    queryFn: () => listarBarberos(true),
  })

  const cambiarActivo = useMutation({
    mutationFn: ({ id, activo }: { id: string; activo: boolean }) => cambiarActivoBarbero(id, activo),
    onSuccess: (b) => {
      setErrorAccion(null)
      queryClient.setQueryData(['barbero', b.id], b)
      queryClient.invalidateQueries({ queryKey: ['barberos', 'lista'] })
    },
    onError: (err) => setErrorAccion(mensajeDeError(err)),
  })

  function desactivar(b: Barbero) {
    const aviso = b.usuarioEmail ? ' También va a dejar de poder entrar al panel.' : ''
    if (window.confirm(`¿Desactivar a ${b.nombre} ${b.apellido}?${aviso} Su historial se conserva.`)) {
      cambiarActivo.mutate({ id: b.id, activo: false })
    }
  }

  const visibles = (data ?? []).filter((b) => verInactivos || b.activo)
  const hayInactivos = (data ?? []).some((b) => !b.activo)

  return (
    <section className="barberos">
      <div className="barberos-cabecera">
        <h1>Barberos</h1>
        <Link to="/panel/barberos/nuevo" className="boton-secundario">
          + Nuevo barbero
        </Link>
      </div>

      {hayInactivos && (
        <label className="barberos-filtro">
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

      {data && visibles.length === 0 && <p className="config-estado">Todavía no hay barberos cargados.</p>}

      <ul className="barberos-lista">
        {visibles.map((b) => (
          <li key={b.id} className={b.activo ? 'barbero-tarjeta' : 'barbero-tarjeta barbero-inactivo'}>
            <Avatar barbero={b} />
            <div className="barbero-info">
              <strong>
                {b.nombre} {b.apellido}
              </strong>
              <span className="barbero-detalle-linea">
                {b.sucursalIds.length} sucursal(es) · {b.servicioIds.length} servicio(s)
                {b.usuarioEmail ? ' · con acceso' : ''}
              </span>
              {!b.activo && <span className="badge badge-inactivo">Inactivo</span>}
            </div>
            <div className="barbero-acciones">
              <Link to={`/panel/barberos/${b.id}`} className="boton-secundario">
                Editar
              </Link>
              {b.activo ? (
                <button type="button" className="boton-texto" disabled={cambiarActivo.isPending} onClick={() => desactivar(b)}>
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
          </li>
        ))}
      </ul>
    </section>
  )
}