import type { SucursalPublica } from '../../types/reserva'

interface Props {
  sucursales: SucursalPublica[]
  elegida: string | null
  onElegir: (id: string) => void
}

export function PasoSucursal({ sucursales, elegida, onElegir }: Props) {
  return (
    <ul className="reserva-opciones">
      {sucursales.map((s) => (
        <li key={s.id}>
          <button type="button" className="reserva-opcion" aria-pressed={s.id === elegida} onClick={() => onElegir(s.id)}>
            <strong>{s.nombre}</strong>
            <span>{s.direccion}, {s.ciudad}</span>
          </button>
        </li>
      ))}
    </ul>
  )
}