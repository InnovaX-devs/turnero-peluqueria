import { useState } from 'react'

interface Props {
  nombre: string
  logoUrl: string | null
}

export function EncabezadoBarberia({ nombre, logoUrl }: Props) {
  // Si la URL del logo está rota, se muestran las iniciales en vez de una imagen rota
  const [logoRoto, setLogoRoto] = useState(false)
  const mostrarLogo = logoUrl !== null && !logoRoto

  return (
    <header className="reserva-encabezado">
      {mostrarLogo ? (
        <img
          className="reserva-logo"
          src={logoUrl}
          alt={'Logo de ' + nombre}
          onError={() => setLogoRoto(true)}
        />
      ) : (
        <div className="reserva-logo reserva-logo-iniciales" aria-hidden="true">
          {iniciales(nombre)}
        </div>
      )}
      <h1 className="reserva-nombre">{nombre}</h1>
    </header>
  )
}

function iniciales(nombre: string): string {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((palabra) => palabra[0].toUpperCase())
    .join('')
}