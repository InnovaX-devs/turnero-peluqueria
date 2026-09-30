import { useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { quitarLogoBarberia, subirLogoBarberia } from '../../api/barberia'
import { mensajeDeError } from '../../lib/errores'
import type { Barberia } from '../../types/barberia'

const MAX_BYTES = 5 * 1024 * 1024
const TIPOS = ['image/jpeg', 'image/png', 'image/webp']

interface Props {
  logoUrl: string | null
  onCambio: (barberia: Barberia) => void
}

export function LogoBarberia({ logoUrl, onCambio }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)
  // Si la imagen no carga se muestra el recuadro vacío en vez de una imagen rota
  const [logoRoto, setLogoRoto] = useState<string | null>(null)

  function alActualizar(b: Barberia) {
    setError(null)
    onCambio(b)
  }

  const subir = useMutation({
    mutationFn: subirLogoBarberia,
    onSuccess: alActualizar,
    onError: (err) => setError(mensajeDeError(err)),
  })

  const quitar = useMutation({
    mutationFn: quitarLogoBarberia,
    onSuccess: alActualizar,
    onError: (err) => setError(mensajeDeError(err)),
  })

  function elegir(e: React.ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0]
    e.target.value = '' // permite volver a elegir el mismo archivo
    if (!archivo) return
    if (!TIPOS.includes(archivo.type)) return setError('Solo se aceptan imágenes JPG, PNG o WEBP')
    if (archivo.size > MAX_BYTES) return setError('El logo no puede pesar más de 5 MB')
    setError(null)
    subir.mutate(archivo)
  }

  const ocupado = subir.isPending || quitar.isPending
  const mostrarLogo = logoUrl !== null && logoRoto !== logoUrl

  return (
    <div className="config-logo-subida">
      {mostrarLogo ? (
        <img
          className="config-logo"
          src={logoUrl}
          alt="Logo de la barbería"
          onError={() => setLogoRoto(logoUrl)}
        />
      ) : (
        <div className="config-logo-vacio" aria-hidden="true">
          Sin logo
        </div>
      )}
      <div className="config-logo-acciones">
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
          {subir.isPending ? 'Subiendo…' : logoUrl ? 'Cambiar logo' : 'Subir logo'}
        </button>
        {logoUrl && (
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