import { useEffect, useState } from 'react'
import { formatearPrecio } from '../../lib/formato'
import type { Turno } from '../../types/agenda'
import { formatearHora, hoyISO } from './agenda'

interface Props {
  /** Turnos del día que se está viendo */
  turnos: Turno[]
  fecha: string
  /** La plata del día solo la ve un admin */
  esAdmin: boolean
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: string | number }) {
  return (
    <div>
      <p className="etiqueta-mono m-0 text-barra-texto/60">{etiqueta}</p>
      <p className="m-0 mt-1 font-mono text-xl font-medium text-barra-texto">{valor}</p>
    </div>
  )
}

/** Tarjeta de marca con lo que importa del día: qué sigue y cómo viene la jornada. */
export function ResumenDia({ turnos, fecha, esAdmin }: Props) {
  const [ahora, setAhora] = useState(() => Date.now())
  useEffect(() => {
    const t = setInterval(() => setAhora(Date.now()), 60_000)
    return () => clearInterval(t)
  }, [])

  const esHoy = fecha === hoyISO()
  const activos = turnos.filter((t) => t.estado !== 'CANCELADA')
  const completados = activos.filter((t) => t.estado === 'COMPLETADA')
  const cobrado = completados
    .filter((t) => t.estadoPago === 'PAGADO')
    .reduce((suma, t) => suma + t.precioFinal, 0)

  const pendientes = activos
    .filter((t) => t.estado === 'RESERVADA')
    .sort((a, b) => a.inicio.localeCompare(b.inicio))
  // Hoy: el que está en curso o el siguiente. Otro día: el primero de la jornada.
  const proximo = esHoy ? pendientes.find((t) => new Date(t.fin).getTime() > ahora) : pendientes[0]
  const enCurso = proximo !== undefined && esHoy && new Date(proximo.inicio).getTime() <= ahora

  const etiqueta = enCurso ? 'En curso' : esHoy ? 'Próximo turno' : 'Primer turno'

  return (
    <section
      className="halo-acento grid gap-5 rounded-2xl bg-barra p-5 text-barra-texto sm:grid-cols-[1fr_auto] sm:items-center sm:p-6"
      aria-label="Resumen del día"
    >
      <div className="relative min-w-0">
        <p className="etiqueta-mono m-0 text-barra-texto/60">{etiqueta}</p>
        {proximo ? (
          <>
            <p className="m-0 mt-1.5 font-titulo text-4xl font-bold leading-none tracking-[-0.03em]">
              {formatearHora(proximo.inicio)}
            </p>
            <p className="m-0 mt-2 truncate text-[15px] font-medium">
              {proximo.clienteNombre ?? 'Sin cliente'}
              <span className="font-normal text-barra-texto/60"> · {proximo.servicioNombre}</span>
            </p>
          </>
        ) : (
          <p className="m-0 mt-1.5 font-titulo text-2xl font-bold tracking-[-0.02em]">
            {esHoy ? 'No quedan turnos por atender' : 'Sin turnos reservados'}
          </p>
        )}
      </div>

      <div className="relative flex gap-7 border-t border-barra-texto/15 pt-4 sm:border-l sm:border-t-0 sm:pl-7 sm:pt-0">
        <Dato etiqueta="Turnos" valor={activos.length} />
        <Dato etiqueta="Atendidos" valor={completados.length} />
        {esAdmin && <Dato etiqueta="Cobrado" valor={formatearPrecio(cobrado)} />}
      </div>
    </section>
  )
}