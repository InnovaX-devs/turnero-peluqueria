import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listarClientes } from '../../api/clientes'
import { mensajeDeError } from '../../lib/errores'
import './clientes.css'

export function ClientesPage() {
  const [texto, setTexto] = useState('')
  const [buscado, setBuscado] = useState('')

  // Espera a que se deje de tipear para no consultar en cada letra
  useEffect(() => {
    const timer = setTimeout(() => setBuscado(texto.trim()), 250)
    return () => clearTimeout(timer)
  }, [texto])

  const clientes = useQuery({
    queryKey: ['clientes', 'listado', buscado],
    queryFn: () => listarClientes(buscado),
    // Mientras se carga la búsqueda nueva se sigue mostrando la lista anterior
    placeholderData: (anterior) => anterior,
  })

  return (
    <section className="clientes">
      <h1 className="clientes-titulo">Clientes</h1>

      <input
        className="clientes-buscador"
        type="search"
        placeholder="Buscar por nombre o teléfono"
        aria-label="Buscar cliente por nombre o teléfono"
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
      />

      {clientes.isPending && <p className="clientes-estado">Cargando…</p>}

      {clientes.isError && (
        <p className="clientes-estado clientes-error" role="alert">
          {mensajeDeError(clientes.error)}
        </p>
      )}

      {clientes.isSuccess && clientes.data.length === 0 && (
        <p className="clientes-estado">
          {buscado ? 'No se encontró ningún cliente con esa búsqueda.' : 'Todavía no hay clientes cargados.'}
        </p>
      )}

      {clientes.isSuccess && clientes.data.length > 0 && (
        <ul className="clientes-lista">
          {clientes.data.map((c) => (
            <li key={c.id}>
              <Link to={`/panel/clientes/${c.id}`} className="clientes-fila">
                <span className="clientes-fila-datos">
                  <strong>{c.nombre}</strong>
                  <small>{c.telefono}</small>
                </span>
                {c.tieneNotas && <span className="clientes-etiqueta-nota">Con nota</span>}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}