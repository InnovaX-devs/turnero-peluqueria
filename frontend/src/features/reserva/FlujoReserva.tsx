import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { obtenerCoberturasPublicas } from '../../api/reservaPublica'
import type { Cobertura } from '../../types/cobertura'
import type { NegocioPublico, ReservaCreada } from '../../types/reserva'
import { PasoCobertura } from './PasoCobertura'
import { PasoProfesional } from './PasoProfesional'
import { PasoDatos } from './PasoDatos'
import { PasoEspecialidad } from './PasoEspecialidad'
import { PasoHorario } from './PasoHorario'
import { PasoServicio } from './PasoServicio'
import { PasoSucursal } from './PasoSucursal'
import { ReservaConfirmada } from './ReservaConfirmada'
import { PROFESIONAL_CUALQUIERA, coberturaReal } from './flujo'
import { useFlujoReserva } from './useFlujoReserva'

/**
 * Rubro salud (issue #67): antes de armar el flujo se buscan las obras sociales del negocio; si tiene,
 * se agrega el paso "¿Cómo vas a pagar?". Si la consulta falla, se reserva igual (como particular).
 */
export function FlujoReserva({ negocio }: { negocio: NegocioPublico }) {
  const esSalud = negocio.rubro === 'SALUD'
  const { data, isPending } = useQuery({
    queryKey: ['coberturas-publicas', negocio.slug],
    queryFn: () => obtenerCoberturasPublicas(negocio.slug),
    enabled: esSalud,
    staleTime: 5 * 60 * 1000,
    retry: 1,
  })

  if (esSalud && isPending) {
    return <p className="reserva-cargando">Cargando…</p>
  }
  return <FlujoReservaPasos negocio={negocio} coberturas={esSalud ? (data ?? []) : []} />
}

function FlujoReservaPasos({ negocio, coberturas }: { negocio: NegocioPublico; coberturas: Cobertura[] }) {
  // Rubro salud con especialidades: especialidad → profesional (o cualquiera) → prestación → horario
  const porEspecialidad = negocio.reservaPorEspecialidad
  const coberturaIds = useMemo(() => coberturas.map((c) => c.id), [coberturas])
  const { pasos, indice, paso, titulo, selecciones, avanzar, volver, reiniciar } = useFlujoReserva(
    negocio.slug,
    negocio.sucursales,
    porEspecialidad,
    coberturaIds,
  )
  // Id real de la obra social elegida (undefined = particular o el negocio no trabaja con obras sociales)
  const coberturaId = coberturaReal(selecciones.coberturaId)
  const cobertura = coberturas.find((c) => c.id === coberturaId)
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
      {paso === 'cobertura' && (
        <PasoCobertura
          coberturas={coberturas}
          elegida={selecciones.coberturaId}
          onElegir={(id) => avanzar({ coberturaId: id })}
        />
      )}
      {paso === 'especialidad' && selecciones.sucursalId && (
        <PasoEspecialidad
          slug={negocio.slug}
          sucursalId={selecciones.sucursalId}
          coberturaId={coberturaId}
          elegida={selecciones.especialidadId}
          onElegir={(especialidadId) => avanzar({ especialidadId })}
        />
      )}
      {paso === 'servicio' && selecciones.sucursalId && !porEspecialidad && (
        <PasoServicio
          slug={negocio.slug}
          sucursalId={selecciones.sucursalId}
          filtro={coberturaId ? { coberturaId } : undefined}
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
              ? { especialidadId: selecciones.especialidadId, coberturaId }
              : { profesionalId: selecciones.profesionalId, coberturaId }
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
          coberturaId={coberturaId}
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
          coberturaId={coberturaId}
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
          coberturaId={coberturaId}
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
          cobertura={cobertura}
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