import { useEffect, useState } from 'react'
import type { Turno } from '../../types/agenda'
import { estaEnSala, formatearHora, tiempoEsperando } from './agenda'
import { useTerminos } from '../terminos/useTerminos'

interface Props {
  /** Turnos del día (ya filtrados por permisos en el backend) */
  turnos: Turno[]
  mostrarProfesional: boolean
  onElegir: (turno: Turno) => void
}

/**
 * Sala de espera del día (issue #71): quién llegó, hace cuánto espera y con quién tiene turno.
 * Solo aparece si hay alguien esperando, así no cambia nada para quien no la usa.
 */
export function SalaDeEspera({ turnos, mostrarProfesional, onElegir }: Props) {
  const terminos = useTerminos()
  const [ahora, setAhora] = useState(() => Date.now())
  useEffect(() => {
    const t = setInterval(() => setAhora(Date.now()), 30_000)
    return () => clearInterval(t)
  }, [])

  const esperando = turnos
    .filter(estaEnSala)
    .sort((a, b) => (a.llegadaEn ?? '').localeCompare(b.llegadaEn ?? ''))

  if (esperando.length === 0) return null

  return (
    <section className="sala" aria-labelledby="sala-titulo">
      <h2 id="sala-titulo" className="sala-titulo">
        Sala de espera <span className="sala-cantidad">{esperando.length}</span>
      </h2>
      <ul className="sala-lista">
        {esperando.map((t) => (
          <li key={t.id}>
            <button type="button" className="sala-item" onClick={() => onElegir(t)}>
              <span className="sala-espera">{tiempoEsperando(t.llegadaEn!, ahora)}</span>
              <span className="sala-cuerpo">
                <strong>{t.clienteNombre ?? `Sin ${terminos.cliente.s}`}</strong>
                <span className="sala-detalle">
                  Turno {formatearHora(t.inicio)} · {t.servicioNombre}
                  {mostrarProfesional && ` · ${t.profesionalNombre}`}
                </span>
              </span>
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
