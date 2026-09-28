import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  obtenerBarberosPublicos,
  obtenerDiasDisponibles,
  obtenerDisponibilidad,
  obtenerServiciosPublicos,
} from '../../api/reservaPublica'
import { mensajeDeError } from '../../lib/errores'
import { diaSemanaCorto, diasEntre, fechaLarga, horaCorta, hoyISO, mesYAnio, sumarDias } from '../../lib/fechas'
import { formatearDuracion } from '../../lib/formato'
import { BARBERO_CUALQUIERA } from './flujo'

const DIAS_POR_SEMANA = 7

export interface EleccionHorario {
  fecha: string
  hora: string
  barberoAsignadoId: string
}

interface Props {
  slug: string
  anticipacionMaximaDias: number
  sucursalId: string
  servicioId: string
  /** Un id o BARBERO_CUALQUIERA */
  barberoId: string
  fechaElegida: string | null
  horaElegida: string | null
  onElegir: (eleccion: EleccionHorario) => void
}

export function PasoHorario(props: Props) {
  const { slug, anticipacionMaximaDias, sucursalId, servicioId, barberoId, fechaElegida, horaElegida, onElegir } = props
  const esCualquiera = barberoId === BARBERO_CUALQUIERA
  const consulta = { sucursalId, servicioId, barberoId: esCualquiera ? null : barberoId }

  const hoy = hoyISO()
  const ultimoDia = sumarDias(hoy, anticipacionMaximaDias)

  // Las semanas arrancan siempre desde hoy (hoy, hoy+7, hoy+14…). Si ya había una fecha elegida, se abre esa semana.
  const [semana, setSemana] = useState(() => {
    if (!fechaElegida || fechaElegida < hoy) return 0
    return Math.floor(diasEntre(hoy, fechaElegida) / DIAS_POR_SEMANA)
  })
  const [diaTocado, setDiaTocado] = useState<string | null>(fechaElegida)

  const inicio = sumarDias(hoy, semana * DIAS_POR_SEMANA)
  const fin = [sumarDias(inicio, DIAS_POR_SEMANA - 1), ultimoDia].sort()[0]
  const diasDeLaSemana = Array.from({ length: DIAS_POR_SEMANA }, (_, i) => sumarDias(inicio, i))
  const hayAnterior = semana > 0
  const haySiguiente = sumarDias(inicio, DIAS_POR_SEMANA) <= ultimoDia

  const qDias = useQuery({
    queryKey: ['dias-disponibles', sucursalId, servicioId, consulta.barberoId, inicio, fin],
    queryFn: () => obtenerDiasDisponibles(consulta, inicio, fin),
    staleTime: 30 * 1000,
  })
  const diasLibres = qDias.data ?? []

  // Día activo: el que tocó el cliente si sigue libre; si no, el primero libre de la semana
  const diaActivo = diaTocado && diasLibres.includes(diaTocado) ? diaTocado : (diasLibres[0] ?? null)

  const qSlots = useQuery({
    queryKey: ['disponibilidad', sucursalId, servicioId, consulta.barberoId, diaActivo],
    queryFn: () => obtenerDisponibilidad(consulta, diaActivo as string),
    enabled: diaActivo !== null,
    staleTime: 30 * 1000,
  })

  // Datos para el resumen (ya están en caché de los pasos anteriores)
  const { data: servicios } = useQuery({
    queryKey: ['servicios-publicos', slug, sucursalId],
    queryFn: () => obtenerServiciosPublicos(slug, sucursalId),
    staleTime: 5 * 60 * 1000,
  })
  const { data: barberos } = useQuery({
    queryKey: ['barberos-publicos', slug, sucursalId, servicioId],
    queryFn: () => obtenerBarberosPublicos(slug, sucursalId, servicioId),
    staleTime: 60 * 1000,
  })
  const servicio = servicios?.find((s) => s.id === servicioId)
  const barbero = barberos?.find((b) => b.id === barberoId)
  const nombreBarbero = esCualquiera ? 'Cualquier barbero' : barbero ? `${barbero.nombre} ${barbero.apellido}` : ''
  const iniciales = esCualquiera ? '✦' : barbero ? `${barbero.nombre[0]}${barbero.apellido[0]}`.toUpperCase() : ''

  function cambiarSemana(delta: number) {
    setSemana((s) => s + delta)
    setDiaTocado(null)
  }

  return (
    <div className="reserva-horario">
      <div className="reserva-calendario-encabezado">
        <strong>{mesYAnio(diaActivo ?? inicio)}</strong>
      </div>

      <div className="reserva-semana">
        <button
          type="button"
          className="reserva-flecha"
          onClick={() => cambiarSemana(-1)}
          disabled={!hayAnterior}
          aria-label="Semana anterior"
        >
          ‹
        </button>
        <ul className="reserva-dias">
          {diasDeLaSemana.map((dia) => {
            const libre = diasLibres.includes(dia)
            return (
              <li key={dia}>
                <button
                  type="button"
                  className="reserva-dia"
                  aria-pressed={dia === diaActivo}
                  aria-label={fechaLarga(dia)}
                  disabled={!libre}
                  onClick={() => setDiaTocado(dia)}
                >
                  <span className="reserva-dia-numero">{Number(dia.slice(8))}</span>
                  <span className="reserva-dia-nombre">{diaSemanaCorto(dia)}</span>
                </button>
              </li>
            )
          })}
        </ul>
        <button
          type="button"
          className="reserva-flecha"
          onClick={() => cambiarSemana(1)}
          disabled={!haySiguiente}
          aria-label="Semana siguiente"
        >
          ›
        </button>
      </div>

      {qDias.isPending && <p className="reserva-cargando">Buscando días disponibles…</p>}

      {qDias.error && (
        <div className="reserva-error reserva-error-inline" role="alert">
          <p>{mensajeDeError(qDias.error)}</p>
          <button type="button" className="boton-principal" onClick={() => qDias.refetch()}>
            Reintentar
          </button>
        </div>
      )}

      {qDias.isSuccess && diaActivo === null && (
        <p className="reserva-vacio">
          No hay turnos disponibles estos días.{haySiguiente && ' Probá con la semana siguiente.'}
        </p>
      )}

      {diaActivo && (
        <>
          <h3 className="reserva-fecha-titulo">{fechaLarga(diaActivo)}</h3>

          <div className="reserva-resumen-barbero">
            {!esCualquiera && barbero?.fotoUrl ? (
              <img className="reserva-avatar reserva-avatar-chico" src={barbero.fotoUrl} alt="" />
            ) : (
              <span className="reserva-avatar reserva-avatar-chico" aria-hidden="true">{iniciales}</span>
            )}
            <div>
              <strong>{nombreBarbero}</strong>
              {servicio && <span>Duración: {formatearDuracion(servicio.duracionMin)}</span>}
            </div>
          </div>

          {qSlots.isPending && <p className="reserva-cargando">Cargando horarios…</p>}

          {qSlots.error && (
            <div className="reserva-error reserva-error-inline" role="alert">
              <p>{mensajeDeError(qSlots.error)}</p>
              <button type="button" className="boton-principal" onClick={() => qSlots.refetch()}>
                Reintentar
              </button>
            </div>
          )}

          {qSlots.data && qSlots.data.slots.length === 0 && (
            <p className="reserva-vacio">Ya no quedan horarios para este día. Elegí otro.</p>
          )}

          {qSlots.data && qSlots.data.slots.length > 0 && (
            <ul className="reserva-horas">
              {qSlots.data.slots.map((slot) => {
                const hora = horaCorta(slot.hora)
                return (
                  <li key={hora}>
                    <button
                      type="button"
                      className="reserva-hora"
                      aria-pressed={fechaElegida === diaActivo && horaElegida === hora}
                      onClick={() =>
                        onElegir({
                          fecha: diaActivo,
                          hora,
                          // Con "cualquiera", el turno queda para el primer barbero libre en ese horario
                          barberoAsignadoId: esCualquiera ? slot.barberos[0] : barberoId,
                        })
                      }
                    >
                      {hora} hs
                    </button>
                  </li>
                )
              })}
            </ul>
          )}
        </>
      )}
    </div>
  )
}
