import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { actualizarNotasCliente } from '../../api/clientes'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { FichaCliente } from '../../types/cliente'

const MAXIMO = 2000

/** Campo de notas del cliente (ej. "degradé en 2, arriba tijera"). Se guarda con el botón. */
export function NotasCliente({ cliente }: { cliente: FichaCliente }) {
  const queryClient = useQueryClient()
  const guardadas = cliente.notas ?? ''
  const [borrador, setBorrador] = useState(guardadas)
  const [guardado, setGuardado] = useState(false)

  const guardar = useMutation({
    mutationFn: () => actualizarNotasCliente(cliente.id, borrador),
    onSuccess: (actualizada) => {
      queryClient.setQueryData(['clientes', 'ficha', cliente.id], actualizada)
      // El listado ("Con nota") y la agenda (nota en el detalle del turno) pueden tener la nota vieja
      queryClient.invalidateQueries({ queryKey: ['clientes', 'listado'] })
      queryClient.invalidateQueries({ queryKey: ['agenda'] })
      setBorrador(actualizada.notas ?? '')
      setGuardado(true)
    },
  })

  const hayCambios = borrador.trim() !== guardadas.trim()

  return (
    <section className="ficha-seccion">
      <h2>Notas</h2>
      <textarea
        className="ficha-notas"
        rows={4}
        maxLength={MAXIMO}
        placeholder="Ej: degradé en 2, arriba tijera"
        aria-label="Notas del cliente"
        value={borrador}
        onChange={(e) => {
          setBorrador(e.target.value)
          setGuardado(false)
          guardar.reset()
        }}
      />

      {guardar.isError && (
        <p className="clientes-error" role="alert">
          {erroresDeCampos(guardar.error).notas ?? mensajeDeError(guardar.error)}
        </p>
      )}

      <div className="ficha-notas-pie">
        <small>{borrador.length}/{MAXIMO}</small>
        {guardado && !hayCambios && (
          <span className="ficha-guardado" role="status">
            Guardado
          </span>
        )}
        <button
          type="button"
          className="boton-principal ficha-guardar"
          disabled={!hayCambios || guardar.isPending}
          onClick={() => guardar.mutate()}
        >
          {guardar.isPending ? 'Guardando…' : 'Guardar notas'}
        </button>
      </div>
    </section>
  )
}