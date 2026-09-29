import { useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { quitarFotoBarbero, subirFotoBarbero } from '../../api/barberos'
import { mensajeDeError } from '../../lib/errores'
import type { Barbero } from '../../types/barbero'
import { iniciales } from './barberos'

const MAX_BYTES = 5 * 1024 * 1024
const TIPOS = ['image/jpeg', 'image/png', 'image/webp']

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

export function FotoBarbero({ barbero }: { barbero: Barbero }) {
  const queryClient = useQueryClient()
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)

  function alActualizar(b: Barbero) {
    queryClient.setQueryData(['barbero', b.id], b)
    queryClient.invalidateQueries({ queryKey: ['barberos', 'lista'] })
    setError(null)
  }

  const subir = useMutation({
    mutationFn: (archivo: File) => subirFotoBarbero(barbero.id, archivo),
    onSuccess: alActualizar,
    onError: (err) => setError(mensajeDeError(err)),
  })

  const quitar = useMutation({
    mutationFn: () => quitarFotoBarbero(barbero.id),
    onSuccess: alActualizar,
    onError: (err) => setError(mensajeDeError(err)),
  })

  function elegir(e: React.ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0]
    e.target.value = '' // permite volver a elegir el mismo archivo
    if (!archivo) return
    if (!TIPOS.includes(archivo.type)) return setError('Solo se aceptan imágenes JPG, PNG o WEBP')
    if (archivo.size > MAX_BYTES) return setError('La foto no puede pesar más de 5 MB')
    setError(null)
    subir.mutate(archivo)
  }

  const ocupado = subir.isPending || quitar.isPending

  return (
    <div className="foto-barbero">
      <Avatar barbero={barbero} grande />
      <div className="foto-acciones">
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          hidden
          onChange={elegir}
        />
        <button
          type="button"
          className="boton-secundario"
          disabled={ocupado}
          onClick={() => inputRef.current?.click()}
        >
          {subir.isPending ? 'Subiendo…' : barbero.fotoUrl ? 'Cambiar foto' : 'Subir foto'}
        </button>
        {barbero.fotoUrl && (
          <button type="button" className="boton-texto" disabled={ocupado} onClick={() => quitar.mutate()}>
            Quitar
          </button>
        )}
        <span className="config-ayuda">JPG, PNG o WEBP, hasta 5 MB.</span>
        {error && (
          <span className="config-campo-error" role="alert">
            {error}
          </span>
        )}
      </div>
    </div>
  )
}