import { useState } from 'react'
import type { Cobertura } from '../../types/cobertura'
import { COBERTURA_PARTICULAR } from './flujo'

interface Props {
  coberturas: Cobertura[]
  elegida: string | null
  onElegir: (id: string) => void
}

/** Con más obras sociales que esto se muestra un buscador arriba de la lista. */
const MINIMO_PARA_BUSCADOR = 6

/** "Médifé" y "medife" son lo mismo para el buscador. */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()
}

/**
 * Rubro salud (issue #67): particular u obra social / prepaga. Con obra social, los pasos siguientes
 * muestran solo a los profesionales que la atienden.
 */
export function PasoCobertura({ coberturas, elegida, onElegir }: Props) {
  const [busqueda, setBusqueda] = useState('')
  const filtro = normalizar(busqueda)
  const visibles = filtro === '' ? coberturas : coberturas.filter((c) => normalizar(c.nombre).includes(filtro))

  return (
    <>
      <ul className="reserva-opciones">
        <li>
          <button
            type="button"
            className="reserva-opcion"
            aria-pressed={elegida === COBERTURA_PARTICULAR}
            onClick={() => onElegir(COBERTURA_PARTICULAR)}
          >
            <strong>Particular</strong>
            <span>Sin obra social ni prepaga</span>
          </button>
        </li>
      </ul>

      <h3 className="reserva-subtitulo">Con obra social o prepaga</h3>
      {coberturas.length > MINIMO_PARA_BUSCADOR && (
        <div className="reserva-buscador">
          <input
            type="search"
            aria-label="Buscar obra social o prepaga"
            placeholder="Buscar (ej. OSDE)"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>
      )}
      {visibles.length === 0 ? (
        <p className="reserva-vacio">
          No encontramos «{busqueda.trim()}». Si no está en la lista, elegí Particular.
        </p>
      ) : (
        <ul className="reserva-opciones">
          {visibles.map((c) => (
            <li key={c.id}>
              <button
                type="button"
                className="reserva-opcion"
                aria-pressed={c.id === elegida}
                onClick={() => onElegir(c.id)}
              >
                <strong>{c.nombre}</strong>
              </button>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
