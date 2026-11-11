import { useEffect, useState } from 'react'
import { keepPreviousData, useInfiniteQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listarClientes } from '../../api/clientes'
import { formatearDni } from '../../lib/dni'
import { mensajeDeError } from '../../lib/errores'
import type { OrdenClientes } from '../../types/cliente'
import { textoUltimaVisita } from './ultimaVisita'
import './clientes.css'
import { useTerminos } from '../terminos/useTerminos'

export function ClientesPage() {
  const t = useTerminos()
  const [texto, setTexto] = useState('')
  const [buscado, setBuscado] = useState('')
  const [orden, setOrden] = useState<OrdenClientes>('nombre')

  // Espera a que se deje de tipear para no consultar en cada letra
  useEffect(() => {
    const timer = setTimeout(() => setBuscado(texto.trim()), 250)
    return () => clearTimeout(timer)
  }, [texto])

  const consulta = useInfiniteQuery({
    queryKey: ['clientes', 'listado', buscado, orden],
    queryFn: ({ pageParam }) => listarClientes(buscado, pageParam, orden),
    initialPageParam: 0,
    getNextPageParam: (ultima) => (ultima.hayMas ? ultima.pagina + 1 : undefined),
    // Mientras se carga la búsqueda nueva se sigue mostrando la lista anterior
    placeholderData: keepPreviousData,
  })

  const clientes = consulta.data?.pages.flatMap((p) => p.clientes) ?? []
  const total = consulta.data?.pages[0]?.total ?? 0

  return (
    <section className="clientes">
      <h1 className="clientes-titulo">{t.cliente.P}</h1>

      <div className="clientes-filtros">
        <input
          className="clientes-buscador"
          type="search"
          placeholder={t.pideDni ? 'Buscar por nombre o DNI' : 'Buscar por nombre o teléfono'}
          aria-label={`Buscar ${t.cliente.s} por nombre o ${t.pideDni ? 'DNI' : 'teléfono'}`}
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
        />
        <label className="clientes-orden">
          <span>Ordenar por</span>
          <select value={orden} onChange={(e) => setOrden(e.target.value as OrdenClientes)}>
            <option value="nombre">Nombre (A-Z)</option>
            <option value="ultimaVisita">Última visita</option>
          </select>
        </label>
      </div>

      {consulta.isPending && <p className="clientes-estado">Cargando…</p>}

      {consulta.isError && (
        <p className="clientes-estado clientes-error" role="alert">
          {mensajeDeError(consulta.error)}
        </p>
      )}

      {consulta.isSuccess && clientes.length === 0 && (
        <p className="clientes-estado">
          {buscado ? `No se encontró ningún ${t.cliente.s} con esa búsqueda.` : `Todavía no hay ${t.cliente.p} cargados.`}
        </p>
      )}

      {consulta.isSuccess && clientes.length > 0 && (
        <>
          <p className="clientes-total" aria-live="polite">
            {buscado
              ? `${total} ${total === 1 ? 'resultado' : 'resultados'}`
              : `${total} ${t.cliente.cantidad(total)}`}
          </p>

          <ul className="clientes-lista">
            {clientes.map((c) => (
              <li key={c.id}>
                <Link to={`/panel/clientes/${c.id}`} className="clientes-fila">
                  <span className="clientes-fila-datos">
                    <strong>{c.nombre}</strong>
                    <small>{c.dni ? `DNI ${formatearDni(c.dni)} · ${c.telefono}` : c.telefono}</small>
                    <small className="clientes-fila-visita">{textoUltimaVisita(c.ultimaVisita)}</small>
                  </span>
                  {c.tieneNotas && <span className="clientes-etiqueta-nota">Con nota</span>}
                </Link>
              </li>
            ))}
          </ul>

          {consulta.hasNextPage && (
            <button
              type="button"
              className="boton-secundario clientes-ver-mas"
              onClick={() => consulta.fetchNextPage()}
              disabled={consulta.isFetchingNextPage}
            >
              {consulta.isFetchingNextPage ? 'Cargando…' : `Ver más (${total - clientes.length} restantes)`}
            </button>
          )}
        </>
      )}
    </section>
  )
}
