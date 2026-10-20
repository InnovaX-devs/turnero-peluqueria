import { useEffect, useRef, useState, type ReactNode } from 'react'
import type { CambioImagen } from './imagenPendiente'

const MAX_BYTES = 5 * 1024 * 1024
const TIPOS = ['image/jpeg', 'image/png', 'image/webp']

interface Props {
  /** La imagen que está guardada ahora (null = no hay) */
  guardada: string | null
  cambio: CambioImagen
  onCambio: (cambio: CambioImagen) => void
  /** "logo" o "foto": arma los textos de los botones y los errores */
  sustantivo: 'logo' | 'foto'
  deshabilitado?: boolean
  /** Clase del contenedor de botones (cada pantalla tiene su estilo) */
  className: string
  /** El botón que guarda el cambio, para el aviso: "Guardar cambios", "Guardar foto" */
  botonGuardar: string
  /** Botones extra, por ejemplo "Guardar foto" */
  children?: ReactNode
}

/**
 * Botones para elegir, cambiar o quitar una imagen sin guardarla todavía. La imagen elegida queda
 * como "cambio pendiente" hasta que la pantalla la guarde.
 */
export function ElegirImagen({
  guardada,
  cambio,
  onCambio,
  sustantivo,
  deshabilitado,
  className,
  botonGuardar,
  children,
}: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)
  const articulo = sustantivo === 'logo' ? 'El' : 'La'
  const nuevo = sustantivo === 'logo' ? 'nuevo' : 'nueva'

  // Libera la vista previa anterior cuando cambia o cuando se sale de la pantalla
  useEffect(() => {
    return () => {
      if (cambio?.tipo === 'subir') URL.revokeObjectURL(cambio.vistaPrevia)
    }
  }, [cambio])

  function elegir(e: React.ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0]
    e.target.value = '' // permite volver a elegir el mismo archivo
    if (!archivo) return
    if (!TIPOS.includes(archivo.type)) return setError('Solo se aceptan imágenes JPG, PNG o WEBP')
    if (archivo.size > MAX_BYTES) return setError(`${articulo} ${sustantivo} no puede pesar más de 5 MB`)
    setError(null)
    onCambio({ tipo: 'subir', archivo, vistaPrevia: URL.createObjectURL(archivo) })
  }

  const hayImagen = cambio?.tipo === 'subir' || (cambio === null && guardada !== null)

  return (
    <div className={className}>
      <input ref={inputRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={elegir} />
      <button
        type="button"
        className="boton-secundario"
        disabled={deshabilitado}
        onClick={() => inputRef.current?.click()}
      >
        {hayImagen ? `Cambiar ${sustantivo}` : `Subir ${sustantivo}`}
      </button>
      {hayImagen && (
        <button
          type="button"
          className="boton-texto"
          disabled={deshabilitado}
          // Si la imagen elegida todavía no se guardó, "Quitar" solo la descarta
          onClick={() => onCambio(guardada === null ? null : { tipo: 'quitar' })}
        >
          Quitar
        </button>
      )}
      {cambio !== null && (
        <button type="button" className="boton-texto" disabled={deshabilitado} onClick={() => onCambio(null)}>
          Deshacer
        </button>
      )}
      {children}
      {cambio !== null ? (
        <span className="config-aviso" role="status">
          {cambio.tipo === 'subir'
            ? `${articulo} ${sustantivo} ${nuevo} se guarda cuando toques «${botonGuardar}».`
            : `${articulo} ${sustantivo} se quita cuando toques «${botonGuardar}».`}
        </span>
      ) : (
        <span className="config-ayuda">JPG, PNG o WEBP, hasta 5 MB.</span>
      )}
      {error && (
        <span className="config-campo-error" role="alert">
          {error}
        </span>
      )}
    </div>
  )
}
