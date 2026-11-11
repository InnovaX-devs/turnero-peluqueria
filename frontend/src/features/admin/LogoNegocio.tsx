import { useState } from 'react'
import { ElegirImagen } from '../../components/ElegirImagen'
import { imagenAMostrar, type CambioImagen } from '../../components/imagenPendiente'
import { useTerminos } from '../terminos/useTerminos'

interface Props {
  /** El logo guardado (null = sin logo) */
  logoUrl: string | null
  cambio: CambioImagen
  onCambio: (cambio: CambioImagen) => void
  deshabilitado: boolean
}

/** Logo del negocio. No guarda nada: el cambio se sube con "Guardar cambios" del formulario. */
export function LogoNegocio({ logoUrl, cambio, onCambio, deshabilitado }: Props) {
  const t = useTerminos()
  // Si la imagen no carga se muestra el recuadro vacío en vez de una imagen rota
  const [logoRoto, setLogoRoto] = useState<string | null>(null)
  const url = imagenAMostrar(logoUrl, cambio)
  const mostrarLogo = url !== null && logoRoto !== url

  return (
    <div className="config-logo-subida">
      {mostrarLogo ? (
        <img className="config-logo" src={url} alt={`Logo ${t.negocio.del}`} onError={() => setLogoRoto(url)} />
      ) : (
        <div className="config-logo-vacio" aria-hidden="true">
          Sin logo
        </div>
      )}
      <ElegirImagen
        guardada={logoUrl}
        cambio={cambio}
        onCambio={onCambio}
        sustantivo="logo"
        deshabilitado={deshabilitado}
        className="config-logo-acciones"
        botonGuardar="Guardar cambios"
      />
    </div>
  )
}
