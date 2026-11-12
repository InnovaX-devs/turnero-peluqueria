import { useState } from 'react'
import type { NegocioPublico, ReservaCreada } from '../../types/reserva'
import { PasoProfesional } from './PasoProfesional'
import { PasoDatos } from './PasoDatos'
import { PasoEspecialidad } from './PasoEspecialidad'
import { PasoHorario } from './PasoHorario'
import { PasoServicio } from './PasoServicio'
import { PasoSucursal } from './PasoSucursal'
import { ReservaConfirmada } from './ReservaConfirmada'
import { PROFESIONAL_CUALQUIERA } from './flujo'
import { useFlujoReserva } from './useFlujoReserva'

export function FlujoReserva({ negocio }: { negocio: NegocioPublico }) {
  // Rubro salud con especialidades: especialidad → profesional (o cualquiera) → prestación → horario
  const porEspecialidad = negocio.reservaPorEspecialidad
  const { pasos, indice, paso, titulo, selecciones, avanzar, volver, reiniciar } = useFlujoReserva(
    negocio.slug,
    negocio.sucursales,
    porEspecialidad,
  )
  const [confirmada, setConfirmada] = useState<{ reserva: ReservaCreada; sucursalId: string } | null>(null)

  if (confirmada) {
    return (
      <ReservaConfirmada
        reserva={confirmada.reserva}
        sucursal={negocio.sucursales.find((s) => s.id === confirmada.sucursalId)}
        negocioNombre={negocio.nombre}
        onOtroTurno={() => setConfirmada(null)}
      />
    )
  }

  const sucursal = negocio.sucursales.find((s) => s.id === selecciones.sucursalId)

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
          sucursales={negocio.sucursales}
          elegida={selecciones.sucursalId}
          onElegir={(sucursalId) => avanzar({ sucursalId })}
        />
      )}
      {paso === 'especialidad' && selecciones.sucursalId && (
        <PasoEspecialidad
          slug={negocio.slug}
          sucursalId={selecciones.sucursalId}
          elegida={selecciones.especialidadId}
          onElegir={(especialidadId) => avanzar({ especialidadId })}
        />
      )}
      {paso === 'servicio' && selecciones.sucursalId && !porEspecialidad && (
        <PasoServicio
          slug={negocio.slug}
          sucursalId={selecciones.sucursalId}
          elegido={selecciones.servicioId}
          onElegir={(servicioId) => avanzar({ servicioId })}
        />
      )}
      {paso === 'servicio' && selecciones.sucursalId && porEspecialidad
        && selecciones.especialidadId && selecciones.profesionalId && (
        <PasoServicio
          slug={negocio.slug}
          sucursalId={selecciones.sucursalId}
          filtro={
            selecciones.profesionalId === PROFESIONAL_CUALQUIERA
              ? { especialidadId: selecciones.especialidadId }
              : { profesionalId: selecciones.profesionalId }
          }
          elegido={selecciones.servicioId}
          onElegir={(servicioId) => avanzar({ servicioId })}
        />
      )}
      {paso === 'profesional' && selecciones.sucursalId && !porEspecialidad && selecciones.servicioId && (
        <PasoProfesional
          slug={negocio.slug}
          sucursalId={selecciones.sucursalId}
          servicioId={selecciones.servicioId}
          elegido={selecciones.profesionalId}
          onElegir={(profesionalId) => avanzar({ profesionalId })}
        />
      )}
      {paso === 'profesional' && selecciones.sucursalId && porEspecialidad && selecciones.especialidadId && (
        <PasoProfesional
          slug={negocio.slug}
          sucursalId={selecciones.sucursalId}
          servicioId={null}
          especialidadId={selecciones.especialidadId}
          elegido={selecciones.profesionalId}
          onElegir={(profesionalId) => avanzar({ profesionalId })}
        />
      )}
      {paso === 'horario' && selecciones.sucursalId && selecciones.servicioId && selecciones.profesionalId && (
        <PasoHorario
          slug={negocio.slug}
          anticipacionMaximaDias={negocio.anticipacionMaximaDias}
          sucursalId={selecciones.sucursalId}
          servicioId={selecciones.servicioId}
          profesionalId={selecciones.profesionalId}
          especialidadId={porEspecialidad ? selecciones.especialidadId : null}
          fechaElegida={selecciones.fecha}
          horaElegida={selecciones.hora}
          onElegir={(eleccion) => avanzar(eleccion)}
        />
      )}
      {paso === 'datos' && sucursal && selecciones.servicioId && selecciones.profesionalAsignadoId
        && selecciones.fecha && selecciones.hora && (
        <PasoDatos
          slug={negocio.slug}
          sucursal={sucursal}
          servicioId={selecciones.servicioId}
          profesionalId={selecciones.profesionalAsignadoId}
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