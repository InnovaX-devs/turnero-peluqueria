import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { quitarFotoBarbero, subirFotoBarbero } from '../../api/barberos'
import { ElegirImagen } from '../../components/ElegirImagen'
import { imagenAMostrar, type CambioImagen } from '../../components/imagenPendiente'
import { mensajeDeError } from '../../lib/errores'
import type { Barbero } from '../../types/barbero'
import { iniciales } from './barberos'

export function Avatar({ barbero, grande = false }: { barbero: Barbero; grande?: boolean }) {
  const clase = grande ? 'avatar avatar-grande' : 'avatar'
  return barbero.fotoUrl ? (
    <img className={clase} src={barbero.fotoUrl} alt={`Foto de ${barbero.nombre}`} />
  ) : (
    <span className={clase} aria-hidden="true">
      {iniciales(barbero)}
    </span>
  )
}

/** Foto del barbero. La foto elegida se ve al instante, pero se sube recién con "Guardar foto". */
export function FotoBarbero({ barbero }: { barbero: Barbero }) {
  const queryClient = useQueryClient()
  const [cambio, setCambio] = useState<CambioImagen>(null)
  const [error, setError] = useState<string | null>(null)
  const [guardada, setGuardada] = useState(false)

  const guardar = useMutation({
    mutationFn: (c: NonNullable<CambioImagen>) =>
      c.tipo === 'subir' ? subirFotoBarbero(barbero.id, c.archivo) : quitarFotoBarbero(barbero.id),
    onSuccess: (b) => {
      queryClient.setQueryData(['barbero', b.id], b)
      queryClient.invalidateQueries({ queryKey: ['barberos', 'lista'] })
      setCambio(null)
      setError(null)
      setGuardada(true)
    },
    onError: (err) => setError(mensajeDeError(err)),
  })

  function cambiar(c: CambioImagen) {
    setCambio(c)
    setError(null)
    setGuardada(false)
  }

  return (
    <div className="foto-barbero">
      <Avatar barbero={{ ...barbero, fotoUrl: imagenAMostrar(barbero.fotoUrl, cambio) }} grande />
      <ElegirImagen
        guardada={barbero.fotoUrl}
        cambio={cambio}
        onCambio={cambiar}
        sustantivo="foto"
        deshabilitado={guardar.isPending}
        className="foto-acciones"
        botonGuardar="Guardar foto"
      >
        {cambio !== null && (
          <button
            type="button"
            className="boton-principal foto-guardar"
            disabled={guardar.isPending}
            onClick={() => guardar.mutate(cambio)}
          >
            {guardar.isPending ? 'Guardando…' : 'Guardar foto'}
          </button>
        )}
      </ElegirImagen>
      {guardada && (
        <p className="config-ok foto-estado" role="status">
          Foto guardada.
        </p>
      )}
      {error && (
        <p className="config-campo-error foto-estado" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
