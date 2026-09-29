import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { guardarHorarios, obtenerHorarios } from '../../api/barberos'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { Barbero, Horario, SucursalOpcion } from '../../types/barbero'
import { DIAS, normalizarHorarios, ordenarHorarios, validarHorarios } from './barberos'

interface Props {
  barbero: Barbero
  sucursales: SucursalOpcion[] // todas las opciones, para resolver nombres
}

export function EditorHorarios({ barbero, sucursales }: Props) {
  const { data, isPending, error, refetch } = useQuery({
    queryKey: ['horarios', barbero.id],
    queryFn: () => obtenerHorarios(barbero.id),
  })

  if (barbero.sucursalIds.length === 0) {
    return <p className="config-ayuda">Asignale al menos una sucursal para poder cargar su horario.</p>
  }
  if (isPending) return <p className="config-estado">Cargando…</p>
  if (error) {
    return (
      <div className="config-error" role="alert">
        <p>{mensajeDeError(error)}</p>
        <button type="button" className="boton-secundario" onClick={() => refetch()}>
          Reintentar
        </button>
      </div>
    )
  }

  const propias = barbero.sucursalIds.map((id) => ({
    id,
    nombre: sucursales.find((s) => s.id === id)?.nombre ?? 'Sucursal',
  }))

  return <Editor barbero={barbero} inicial={data} sucursales={propias} />
}

function Editor({
  barbero,
  inicial,
  sucursales,
}: {
  barbero: Barbero
  inicial: Horario[]
  sucursales: { id: string; nombre: string }[]
}) {
  const queryClient = useQueryClient()
  const [tramos, setTramos] = useState<Horario[]>(() => normalizarHorarios(inicial))
  const [base, setBase] = useState(() => JSON.stringify(normalizarHorarios(inicial)))
  const [error, setError] = useState<string | null>(null)
  const [guardado, setGuardado] = useState(false)

  const guardar = useMutation({
    mutationFn: (t: Horario[]) => guardarHorarios(barbero.id, t),
    onSuccess: (res) => {
      const normalizados = normalizarHorarios(res)
      queryClient.setQueryData(['horarios', barbero.id], res)
      setTramos(normalizados)
      setBase(JSON.stringify(normalizados))
      setGuardado(true)
    },
    onError: (err) => setError(erroresDeCampos(err).horarios ?? mensajeDeError(err)),
  })

  function tocar() {
    setError(null)
    setGuardado(false)
  }

  function modificar(indice: number, cambio: Partial<Horario>) {
    setTramos((ts) => ts.map((t, i) => (i === indice ? { ...t, ...cambio } : t)))
    tocar()
  }

  function quitar(indice: number) {
    setTramos((ts) => ts.filter((_, i) => i !== indice))
    tocar()
  }

  function agregar(dia: number) {
    const ultimo = tramos.filter((t) => t.diaSemana === dia).at(-1)
    setTramos((ts) => [
      ...ts,
      {
        sucursalId: ultimo?.sucursalId ?? sucursales[0].id,
        diaSemana: dia,
        horaInicio: ultimo ? '16:00' : '10:00',
        horaFin: ultimo ? '21:00' : '13:00',
      },
    ])
    tocar()
  }

  /** Copia los tramos de un día a los demás días de lunes a viernes. */
  function copiarALaSemana(dia: number) {
    const origen = tramos.filter((t) => t.diaSemana === dia)
    setTramos((ts) => [
      ...ts.filter((t) => t.diaSemana > 5 || t.diaSemana === dia),
      ...[1, 2, 3, 4, 5]
        .filter((d) => d !== dia)
        .flatMap((d) => origen.map((o) => ({ ...o, diaSemana: d }))),
    ])
    tocar()
  }

  function handleGuardar() {
    const mensaje = validarHorarios(tramos)
    if (mensaje) {
      setError(mensaje)
      return
    }
    setError(null)
    setGuardado(false)
    guardar.mutate(ordenarHorarios(tramos))
  }

  const hayCambios = JSON.stringify(ordenarHorarios(tramos)) !== base

  return (
    <div className="horarios">
      {DIAS.map((dia) => {
        const delDia = tramos.map((t, i) => ({ t, i })).filter(({ t }) => t.diaSemana === dia.n)
        return (
          <div key={dia.n} className="horario-dia">
            <strong className="horario-nombre">{dia.nombre}</strong>
            <div className="horario-tramos">
              {delDia.length === 0 && <span className="config-ayuda">No trabaja</span>}
              {delDia.map(({ t, i }) => (
                <div key={i} className="tramo">
                  {sucursales.length > 1 && (
                    <select
                      aria-label={`Sucursal, ${dia.nombre}`}
                      value={t.sucursalId}
                      onChange={(e) => modificar(i, { sucursalId: e.target.value })}
                    >
                      {sucursales.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.nombre}
                        </option>
                      ))}
                    </select>
                  )}
                  <input
                    type="time"
                    aria-label={`Desde, ${dia.nombre}`}
                    value={t.horaInicio}
                    onChange={(e) => modificar(i, { horaInicio: e.target.value })}
                  />
                  <span aria-hidden="true">a</span>
                  <input
                    type="time"
                    aria-label={`Hasta, ${dia.nombre}`}
                    value={t.horaFin}
                    onChange={(e) => modificar(i, { horaFin: e.target.value })}
                  />
                  <button type="button" className="boton-texto" onClick={() => quitar(i)}>
                    Quitar
                  </button>
                </div>
              ))}
              <div className="horario-acciones">
                <button type="button" className="boton-texto" onClick={() => agregar(dia.n)}>
                  + Agregar tramo
                </button>
                {dia.n <= 5 && delDia.length > 0 && (
                  <button type="button" className="boton-texto" onClick={() => copiarALaSemana(dia.n)}>
                    Copiar a lunes–viernes
                  </button>
                )}
              </div>
            </div>
          </div>
        )
      })}

      {error && <p className="config-error" role="alert">{error}</p>}
      {guardado && <p className="config-ok" role="status">Horario guardado.</p>}

      <button
        type="button"
        className="boton-principal config-guardar"
        disabled={!hayCambios || guardar.isPending}
        onClick={handleGuardar}
      >
        {guardar.isPending ? 'Guardando…' : 'Guardar horario'}
      </button>
    </div>
  )
}