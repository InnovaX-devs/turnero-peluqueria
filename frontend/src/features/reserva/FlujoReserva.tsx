import { useState } from 'react'
import type { BarberiaPublica, ReservaCreada } from '../../types/reserva'
import { PasoBarbero } from './PasoBarbero'
import { PasoDatos } from './PasoDatos'
import { PasoHorario } from './PasoHorario'
import { PasoServicio } from './PasoServicio'
import { PasoSucursal } from './PasoSucursal'
import { ReservaConfirmada } from './ReservaConfirmada'
import { useFlujoReserva } from './useFlujoReserva'

export function FlujoReserva({ barberia }: { barberia: BarberiaPublica }) {
  const { pasos, indice, paso, titulo, selecciones, avanzar, volver, reiniciar } = useFlujoReserva(
    barberia.slug,
    barberia.sucursales,
  )
  const [confirmada, setConfirmada] = useState<{ reserva: ReservaCreada; sucursalId: string } | null>(null)

  if (confirmada) {
    return (
      <ReservaConfirmada
        reserva={confirmada.reserva}
        sucursal={barberia.sucursales.find((s) => s.id === confirmada.sucursalId)}
        onOtroTurno={() => setConfirmada(null)}
      />
    )
  }

  const sucursal = barberia.sucursales.find((s) => s.id === selecciones.sucursalId)

  function cancelar() {
    if (window.confirm('¿Cancelar la reserva? Se va a borrar lo que elegiste.')) {
      reiniciar() // al borrar lo elegido, el flujo vuelve solo al primer paso
      window.scrollTo(0, 0)
    }
  }

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
        <div className="reserva-progreso-derecha">
          <span className="reserva-contador">Paso {indice + 1} de {pasos.length}</span>
          {indice > 0 && (
            <button type="button" className="reserva-cancelar" onClick={cancelar}>
              Cancelar
            </button>
          )}
        </div>
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
      {paso === 'horario' && selecciones.sucursalId && selecciones.servicioId && selecciones.barberoId && (
        <PasoHorario
          slug={barberia.slug}
          anticipacionMaximaDias={barberia.anticipacionMaximaDias}
          sucursalId={selecciones.sucursalId}
          servicioId={selecciones.servicioId}
          barberoId={selecciones.barberoId}
          fechaElegida={selecciones.fecha}
          horaElegida={selecciones.hora}
          onElegir={(eleccion) => avanzar(eleccion)}
        />
      )}
      {paso === 'datos' && sucursal && selecciones.servicioId && selecciones.barberoAsignadoId
        && selecciones.fecha && selecciones.hora && (
        <PasoDatos
          slug={barberia.slug}
          sucursal={sucursal}
          servicioId={selecciones.servicioId}
          barberoId={selecciones.barberoAsignadoId}
          fecha={selecciones.fecha}
          hora={selecciones.hora}
          onConfirmada={(reserva) => {
            setConfirmada({ reserva, sucursalId: sucursal.id })
            reiniciar()
            window.scrollTo(0, 0)
          }}
          onElegirOtroHorario={volver}
        />
      )}
    </section>
  )
}