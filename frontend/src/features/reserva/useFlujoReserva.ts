import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import type { SucursalPublica } from '../../types/reserva'
import { useTerminos } from '../terminos/useTerminos'
import { SELECCIONES_VACIAS, aplicarEleccion, esPaso, pasoAlcanzable, pasosDelFlujo, type Selecciones } from './flujo'

/** {@code porEspecialidad}: rubro salud con especialidades (especialidad → profesional → prestación → horario). */
export function useFlujoReserva(slug: string, sucursales: SucursalPublica[], porEspecialidad = false) {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const location = useLocation()
  const terminos = useTerminos()
  const clave = `reserva.${slug}`

  const [selecciones, setSelecciones] = useState<Selecciones>(() =>
    normalizar(leerGuardadas(clave), sucursales, porEspecialidad),
  )

  useEffect(() => {
    try {
      sessionStorage.setItem(clave, JSON.stringify(selecciones))
    } catch {
      // incógnito o storage lleno: el flujo anda igual, solo no sobrevive a un reload
    }
  }, [clave, selecciones])

  const pasos = useMemo(() => pasosDelFlujo(sucursales.length, porEspecialidad), [sucursales.length, porEspecialidad])
  // Los links viejos decían ?paso=barbero
  const pasoEnUrl = searchParams.get('paso') === 'barbero' ? 'profesional' : searchParams.get('paso')
  const indice = pasoAlcanzable(pasos, esPaso(pasoEnUrl) ? pasoEnUrl : null, selecciones)
  const actual = pasos[indice]

  // Link directo a un paso no alcanzable (ej. ?paso=horario sin servicio): se corrige sin sumar historial
  useEffect(() => {
    if (pasoEnUrl !== null && pasoEnUrl !== actual.paso) {
      setSearchParams({ paso: actual.paso }, { replace: true, state: location.state })
    }
  }, [pasoEnUrl, actual.paso, setSearchParams, location.state])

  const avanzar = useCallback(
    (cambios: Partial<Selecciones>) => {
      setSelecciones((prev) => aplicarEleccion(prev, cambios, porEspecialidad))
      const siguiente = pasos[indice + 1]
      if (siguiente) {
        navigate({ search: `?paso=${siguiente.paso}` }, { state: { dentroDelFlujo: true } })
        window.scrollTo(0, 0)
      }
    },
    [indice, navigate, pasos, porEspecialidad],
  )

  /** Si se llegó navegando el flujo, igual que el atrás del navegador; si se entró por link, reemplaza la URL. */
  const volver = useCallback(() => {
    if (indice === 0) return
    if ((location.state as { dentroDelFlujo?: boolean } | null)?.dentroDelFlujo) navigate(-1)
    else setSearchParams({ paso: pasos[indice - 1].paso }, { replace: true })
  }, [indice, location.state, navigate, pasos, setSearchParams])

  /** Después de confirmar: borra lo elegido para que un reload no muestre el turno ya reservado. */
  const reiniciar = useCallback(() => {
    setSelecciones(normalizar({}, sucursales, porEspecialidad))
  }, [sucursales, porEspecialidad])

  return { pasos, indice, paso: actual.paso, titulo: actual.titulo(terminos), selecciones, avanzar, volver, reiniciar }
}

function leerGuardadas(clave: string): Partial<Selecciones> {
  try {
    const valor: unknown = JSON.parse(sessionStorage.getItem(clave) ?? 'null')
    return valor && typeof valor === 'object' ? (valor as Partial<Selecciones>) : {}
  } catch {
    return {}
  }
}

/** Descarta lo guardado que ya no aplica y autoelige la sucursal si hay una sola. */
function normalizar(guardadas: Partial<Selecciones>, sucursales: SucursalPublica[], porEspecialidad: boolean): Selecciones {
  const sel: Selecciones = { ...SELECCIONES_VACIAS }
  for (const campo of Object.keys(SELECCIONES_VACIAS) as (keyof Selecciones)[]) {
    const v = guardadas[campo]
    if (typeof v === 'string') sel[campo] = v
  }
  // Si el negocio dejó de reservar por especialidad, una guardada de antes ya no aplica
  if (!porEspecialidad) sel.especialidadId = null
  if (sucursales.length === 1) return aplicarEleccion(sel, { sucursalId: sucursales[0].id }, porEspecialidad)
  if (sel.sucursalId !== null && !sucursales.some((s) => s.id === sel.sucursalId)) return { ...SELECCIONES_VACIAS }
  return sel
}