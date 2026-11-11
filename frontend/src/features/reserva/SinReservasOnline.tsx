import type { SucursalPublica } from '../../types/reserva'
import { useTerminos } from '../terminos/useTerminos'

interface Props {
  sucursales: SucursalPublica[]
}

function linkMapa(s: SucursalPublica): string {
  const direccion = encodeURIComponent(s.direccion + ', ' + s.ciudad)
  return 'https://www.google.com/maps/search/?api=1&query=' + direccion
}

export function SinReservasOnline({ sucursales }: Props) {
  const t = useTerminos()
  return (
    <section className="reserva-cerrada">
      <h2>{t.negocio.Este} no toma reservas online</h2>
      {sucursales.length > 0 && <p>Podés pedir tu turno directamente:</p>}

      <ul className="reserva-contactos">
        {sucursales.map((s) => (
          <li key={s.id} className="reserva-contacto">
            {sucursales.length > 1 && <strong>{s.nombre}</strong>}
            <a href={linkMapa(s)} target="_blank" rel="noreferrer">
              {s.direccion}, {s.ciudad}
            </a>
            {s.telefono && (
              <a className="boton-principal reserva-llamar" href={'tel:' + s.telefono}>
                Llamar al {s.telefono}
              </a>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}