import { useEffect, type RefObject } from 'react'

/** Llama a `onCerrar` al hacer click fuera del elemento o al apretar Escape. */
export function useCerrarAfuera(ref: RefObject<HTMLElement | null>, activo: boolean, onCerrar: () => void) {
  useEffect(() => {
    if (!activo) return

    function alClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) onCerrar()
    }
    function alTecla(e: KeyboardEvent) {
      if (e.key === 'Escape') onCerrar()
    }

    document.addEventListener('mousedown', alClick)
    document.addEventListener('keydown', alTecla)
    return () => {
      document.removeEventListener('mousedown', alClick)
      document.removeEventListener('keydown', alTecla)
    }
  }, [ref, activo, onCerrar])
}