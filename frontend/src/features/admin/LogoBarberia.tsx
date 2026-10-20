import { useState } from 'react'
import { ElegirImagen } from '../../components/ElegirImagen'
import { imagenAMostrar, type CambioImagen } from '../../components/imagenPendiente'

interface Props {
  /** El logo guardado (null = sin logo) */
  logoUrl: string | null
  cambio: CambioImagen
  onCambio: (cambio: CambioImagen) => void
  deshabilitado: boolean
}

/** Logo de la barbería. No guarda nada: el cambio se sube con "Guardar cambios" del formulario. */
export function LogoBarberia({ logoUrl, cambio, onCambio, deshabilitado }: Props) {
  // Si la imagen no carga se muestra el recuadro vacío en vez de una imagen rota
  const [logoRoto, setLogoRoto] = useState<string | null>(null)
  const url = imagenAMostrar(logoUrl, cambio)
  const mostrarLogo = url !== null && logoRoto !== url

  return (
    <div className="config-logo-subida">
      {mostrarLogo ? (
        <img className="config-logo" src={url} alt="Logo de la barbería" onError={() => setLogoRoto(url)} />
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
