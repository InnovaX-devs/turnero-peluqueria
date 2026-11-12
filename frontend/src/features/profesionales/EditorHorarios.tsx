import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { guardarHorarios, obtenerHorarios } from '../../api/profesionales'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import { Selector } from '../../components/Selector'
import type { ConsultorioOpcion, Profesional, Horario, SucursalOpcion } from '../../types/profesional'
import { DIAS, normalizarHorarios, ordenarHorarios, validarHorarios } from './profesionales'

interface Props {
  profesional: Profesional
  sucursales: SucursalOpcion[] // todas las opciones, para resolver nombres
  /** Consultorios activos (rubro salud, issue #69). Vacío = no se muestra el campo */
  consultorios?: ConsultorioOpcion[]
}

/** Valor del selector para "sin consultorio" (el Selector trabaja con strings). */
const SIN_CONSULTORIO = ''

export function EditorHorarios({ profesional, sucursales, consultorios = [] }: Props) {
  const { data, isPending, error, refetch } = useQuery({
    queryKey: ['horarios', profesional.id],
    queryFn: () => obtenerHorarios(profesional.id),
  })

  if (profesional.sucursalIds.length === 0) {
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

  const propias = profesional.sucursalIds.map((id) => ({
    id,
    nombre: sucursales.find((s) => s.id === id)?.nombre ?? 'Sucursal',
  }))

  return <Editor profesional={profesional} inicial={data} sucursales={propias} consultorios={consultorios} />
}

function Editor({
  profesional,
  inicial,
  sucursales,
  consultorios,
}: {
  profesional: Profesional
  inicial: Horario[]
  sucursales: { id: string; nombre: string }[]
  consultorios: ConsultorioOpcion[]
}) {
  const queryClient = useQueryClient()
  const [tramos, setTramos] = useState<Horario[]>(() => normalizarHorarios(inicial))
  const [base, setBase] = useState(() => JSON.stringify(normalizarHorarios(inicial)))
  const [error, setError] = useState<string | null>(null)
  const [guardado, setGuardado] = useState(false)

  const guardar = useMutation({
    mutationFn: (t: Horario[]) => guardarHorarios(profesional.id, t),
    onSuccess: (res) => {
      const normalizados = normalizarHorarios(res)
      queryClient.setQueryData(['horarios', profesional.id], res)
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

  /** Los consultorios son de una sucursal: al cambiarla, el consultorio elegido deja de valer. */
  function cambiarSucursal(indice: number, sucursalId: string) {
    setTramos((ts) =>
      ts.map((t, i) => {
        if (i !== indice) return t
        const sigueValiendo = consultorios.some((c) => c.id === t.consultorioId && c.sucursalId === sucursalId)
        return { ...t, sucursalId, consultorioId: sigueValiendo ? t.consultorioId : null }
      }),
    )
    tocar()
  }

  function consultoriosDe(sucursalId: string): ConsultorioOpcion[] {
    return consultorios.filter((c) => c.sucursalId === sucursalId)
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
        consultorioId: ultimo?.consultorioId ?? null,
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
                    <Selector
                      variante="compacto"
                      etiqueta={`Sucursal, ${dia.nombre}`}
                      valor={t.sucursalId}
                      onChange={(v) => cambiarSucursal(i, v)}
                      opciones={sucursales.map((s) => ({ valor: s.id, etiqueta: s.nombre }))}
                    />
                  )}
                  {consultoriosDe(t.sucursalId).length > 0 && (
                    <Selector
                      variante="compacto"
                      etiqueta={`Consultorio, ${dia.nombre}`}
                      valor={t.consultorioId ?? SIN_CONSULTORIO}
                      onChange={(v) => modificar(i, { consultorioId: v === SIN_CONSULTORIO ? null : v })}
                      opciones={[
                        { valor: SIN_CONSULTORIO, etiqueta: 'Sin consultorio' },
                        ...consultoriosDe(t.sucursalId).map((c) => ({ valor: c.id, etiqueta: c.nombre })),
                      ]}
                    />
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