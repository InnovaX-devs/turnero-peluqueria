import type { BarberiaPublica } from '../../types/reserva'
import { PasoBarbero } from './PasoBarbero'
import { PasoServicio } from './PasoServicio'
import { PasoSucursal } from './PasoSucursal'
import { useFlujoReserva } from './useFlujoReserva'

export function FlujoReserva({ barberia }: { barberia: BarberiaPublica }) {
  const { pasos, indice, paso, titulo, selecciones, avanzar, volver } = useFlujoReserva(
    barberia.slug,
    barberia.sucursales,
  )

  return (
    <section className="reserva-flujo" aria-labelledby="reserva-titulo-paso">
      <div className="reserva-progreso">
        {indice > 0 ? (
          <button type="button" className="reserva-atras" onClick={volver}>
            <span aria-hidden="true">‹</span> Atrás
          </button>
        ) : (
          <span />
        )}
        <span className="reserva-contador">Paso {indice + 1} de {pasos.length}</span>
      </div>
      <div className="reserva-barra" role="progressbar" aria-label="Progreso de la reserva"
        aria-valuemin={1} aria-valuemax={pasos.length} aria-valuenow={indice + 1}>
        <div style={{ width: `${((indice + 1) / pasos.length) * 100}%` }} />
      </div>

      <h2 id="reserva-titulo-paso" className="reserva-titulo-paso">{titulo}</h2>

      {paso === 'sucursal' && (
        <PasoSucursal
          sucursales={barberia.sucursales}
          elegida={selecciones.sucursalId}
          onElegir={(sucursalId) => avanzar({ sucursalId })}
        />
      )}
      {paso === 'servicio' && selecciones.sucursalId && (
        <PasoServicio
          slug={barberia.slug}
          sucursalId={selecciones.sucursalId}
          elegido={selecciones.servicioId}
          onElegir={(servicioId) => avanzar({ servicioId })}
        />
      )}
      {paso === 'barbero' && selecciones.sucursalId && selecciones.servicioId && (
        <PasoBarbero
          slug={barberia.slug}
          sucursalId={selecciones.sucursalId}
          servicioId={selecciones.servicioId}
          elegido={selecciones.barberoId}
          onElegir={(barberoId) => avanzar({ barberoId })}
        />
      )}
      {/* Los pasos siguientes se implementan en sus propios issues */}
      {paso !== 'sucursal' && paso !== 'servicio' && paso !== 'barbero' && (
        <p className="reserva-pendiente">Este paso todavía no está disponible.</p>
      )}
    </section>
  )
}