import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { actualizarNotasTurno } from '../../api/agenda'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { Rubro } from '../../lib/terminos'
import type { Turno } from '../../types/agenda'
import { formatearFechaHora } from './agenda'

const MAXIMO = 2000

interface Props {
  turno: Turno
  rubro: Rubro | undefined
  /** Recibe el turno ya actualizado, para que el detalle refleje la nota y la auditoría sin cerrarse. */
  onActualizado: (turno: Turno) => void
}

/**
 * Notas de consulta (issue #72): las ven y editan solo el profesional que atendió y el admin. Este
 * componente no se monta para recepción (y el backend tampoco le manda la nota). En salud lleva el
 * aviso de que no reemplazan la historia clínica.
 */
export function NotasConsulta({ turno, rubro, onActualizado }: Props) {
  const queryClient = useQueryClient()
  const guardadas = turno.notas ?? ''
  const [borrador, setBorrador] = useState(guardadas)
  const [guardado, setGuardado] = useState(false)
  const esSalud = rubro === 'SALUD'

  const guardar = useMutation({
    mutationFn: () => actualizarNotasTurno(turno.id, borrador),
    onSuccess: (actualizado) => {
      // La agenda ya cargada tiene la nota vieja: se refresca en segundo plano
      queryClient.invalidateQueries({ queryKey: ['agenda'] })
      setBorrador(actualizado.notas ?? '')
      setGuardado(true)
      onActualizado(actualizado)
    },
  })

  const hayCambios = borrador.trim() !== guardadas.trim()

  return (
    <section className="agenda-notas" aria-labelledby={`notas-${turno.id}`}>
      <h3 id={`notas-${turno.id}`} className="agenda-notas-titulo">
        {esSalud ? 'Notas de consulta' : 'Notas del turno'}
      </h3>
      {esSalud && (
        <p className="agenda-notas-aviso" role="note">
          Estas notas no reemplazan la historia clínica
        </p>
      )}

      <textarea
        className="agenda-notas-texto"
        rows={4}
        maxLength={MAXIMO}
        aria-labelledby={`notas-${turno.id}`}
        value={borrador}
        onChange={(e) => {
          setBorrador(e.target.value)
          setGuardado(false)
          guardar.reset()
        }}
      />

      {guardar.isError && (
        <p className="agenda-modal-error" role="alert">
          {erroresDeCampos(guardar.error).notas ?? mensajeDeError(guardar.error)}
        </p>
      )}

      <div className="agenda-notas-pie">
        <small className="agenda-notas-contador">
          {borrador.length}/{MAXIMO}
        </small>
        {guardado && !hayCambios && (
          <span className="agenda-notas-guardado" role="status">
            Guardado
          </span>
        )}
        <button
          type="button"
          className="boton-secundario agenda-notas-guardar"
          disabled={!hayCambios || guardar.isPending}
          onClick={() => guardar.mutate()}
        >
          {guardar.isPending ? 'Guardando…' : 'Guardar notas'}
        </button>
      </div>

      {turno.notas && turno.notasEditadasPorEmail && turno.notasEditadasEn && (
        <p className="agenda-notas-auditoria">
          Última edición: {turno.notasEditadasPorEmail} · {formatearFechaHora(turno.notasEditadasEn)}
        </p>
      )}
      <p className="agenda-notas-visibilidad">Solo las ven quien atendió y el admin.</p>
    </section>
  )
}
