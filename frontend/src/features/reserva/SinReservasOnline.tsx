import type { SucursalPublica } from '../../types/reserva'

interface Props {
  sucursales: SucursalPublica[]
}

function linkMapa(s: SucursalPublica): string {
  const direccion = encodeURIComponent(s.direccion + ', ' + s.ciudad)
  return 'https://www.google.com/maps/search/?api=1&query=' + direccion
}

export function SinReservasOnline({ sucursales }: Props) {
  return (
    <section className="reserva-cerrada">
      <h2>Esta barbería no toma reservas online</h2>
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