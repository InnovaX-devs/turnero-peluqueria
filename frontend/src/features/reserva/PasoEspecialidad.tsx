import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { obtenerEspecialidadesPublicas } from '../../api/reservaPublica'
import { mensajeDeError } from '../../lib/errores'

interface Props {
  slug: string
  sucursalId: string
  elegida: string | null
  onElegir: (id: string) => void
}

/** Con más especialidades que esto se muestra un buscador arriba de la lista. */
const MINIMO_PARA_BUSCADOR = 6

/** "Kinesiología" y "kinesiologia" son lo mismo para el buscador. */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()
}

/** Rubro salud (issue #68): el paciente elige qué necesita sin tener que conocer al profesional. */
export function PasoEspecialidad({ slug, sucursalId, elegida, onElegir }: Props) {
  const [busqueda, setBusqueda] = useState('')
  const { data: especialidades, isPending, error, refetch } = useQuery({
    queryKey: ['especialidades-publicas', slug, sucursalId],
    queryFn: () => obtenerEspecialidadesPublicas(slug, sucursalId),
    staleTime: 5 * 60 * 1000,
  })

  if (isPending) {
    return <p className="reserva-cargando">Cargando especialidades…</p>
  }

  if (error) {
    return (
      <div className="reserva-error reserva-error-inline" role="alert">
        <p>{mensajeDeError(error)}</p>
        <button type="button" className="boton-principal" onClick={() => refetch()}>
          Reintentar
        </button>
      </div>
    )
  }

  if (especialidades.length === 0) {
    return <p className="reserva-vacio">Por ahora no hay especialidades para reservar en esta sucursal.</p>
  }

  const filtro = normalizar(busqueda)
  const visibles = filtro === '' ? especialidades : especialidades.filter((e) => normalizar(e.nombre).includes(filtro))

  return (
    <>
      {especialidades.length > MINIMO_PARA_BUSCADOR && (
        <div className="reserva-buscador">
          <input
            type="search"
            aria-label="Buscar especialidad"
            placeholder="Buscar especialidad (ej. Kinesiología)"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>
      )}
      {visibles.length === 0 ? (
        <p className="reserva-vacio">No encontramos «{busqueda.trim()}». Probá con otra palabra.</p>
      ) : (
        <ul className="reserva-opciones">
          {visibles.map((e) => (
            <li key={e.id}>
              <button
                type="button"
                className="reserva-opcion"
                aria-pressed={e.id === elegida}
                onClick={() => onElegir(e.id)}
              >
                <strong>{e.nombre}</strong>
              </button>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
