import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { quitarFotoProfesional, subirFotoProfesional } from '../../api/profesionales'
import { ElegirImagen } from '../../components/ElegirImagen'
import { imagenAMostrar, type CambioImagen } from '../../components/imagenPendiente'
import { mensajeDeError } from '../../lib/errores'
import type { Profesional } from '../../types/profesional'
import { iniciales } from './profesionales'

export function Avatar({ profesional, grande = false }: { profesional: Profesional; grande?: boolean }) {
  const clase = grande ? 'avatar avatar-grande' : 'avatar'
  return profesional.fotoUrl ? (
    <img className={clase} src={profesional.fotoUrl} alt={`Foto de ${profesional.nombre}`} />
  ) : (
    <span className={clase} aria-hidden="true">
      {iniciales(profesional)}
    </span>
  )
}

/** Foto del barbero. La foto elegida se ve al instante, pero se sube recién con "Guardar foto". */
export function FotoProfesional({ profesional }: { profesional: Profesional }) {
  const queryClient = useQueryClient()
  const [cambio, setCambio] = useState<CambioImagen>(null)
  const [error, setError] = useState<string | null>(null)
  const [guardada, setGuardada] = useState(false)

  const guardar = useMutation({
    mutationFn: (c: NonNullable<CambioImagen>) =>
      c.tipo === 'subir' ? subirFotoProfesional(profesional.id, c.archivo) : quitarFotoProfesional(profesional.id),
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
    <div className="foto-profesional">
      <Avatar profesional={{ ...profesional, fotoUrl: imagenAMostrar(profesional.fotoUrl, cambio) }} grande />
      <ElegirImagen
        guardada={profesional.fotoUrl}
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
