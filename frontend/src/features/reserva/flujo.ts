import type { Terminos } from '../../lib/terminos'

export type Paso = 'sucursal' | 'servicio' | 'profesional' | 'horario' | 'datos'

/** Valor de profesionalId cuando el cliente elige "Cualquier profesional". */
export const PROFESIONAL_CUALQUIERA = 'cualquiera'

export interface Selecciones {
  sucursalId: string | null
  servicioId: string | null
  profesionalId: string | null // 'cualquiera' = primer profesional disponible
  fecha: string | null // YYYY-MM-DD
  hora: string | null // HH:mm
  /** El profesional real del turno: igual a profesionalId, o el que tocó en el horario si eligió "cualquiera" */
  profesionalAsignadoId: string | null
}

export const SELECCIONES_VACIAS: Selecciones = {
  sucursalId: null, servicioId: null, profesionalId: null, fecha: null, hora: null, profesionalAsignadoId: null,
}

interface DefinicionPaso {
  paso: Paso
  /** Recibe los términos del rubro: "Elegí el barbero" / "Elegí el profesional". */
  titulo: (t: Terminos) => string
  campos: (keyof Selecciones)[]
}

const TODOS_LOS_PASOS: DefinicionPaso[] = [
  { paso: 'sucursal', titulo: () => 'Elegí la sucursal', campos: ['sucursalId'] },
  { paso: 'servicio', titulo: (t) => `Elegí ${t.servicio.el}`, campos: ['servicioId'] },
  { paso: 'profesional', titulo: (t) => `Elegí ${t.profesional.el}`, campos: ['profesionalId'] },
  { paso: 'horario', titulo: () => 'Elegí día y horario', campos: ['fecha', 'hora', 'profesionalAsignadoId'] },
  { paso: 'datos', titulo: () => 'Tus datos', campos: [] },
]

/** Con una sola sucursal, el paso se saltea y se elige sola. */
export function pasosDelFlujo(cantidadSucursales: number): DefinicionPaso[] {
  return cantidadSucursales <= 1 ? TODOS_LOS_PASOS.filter((p) => p.paso !== 'sucursal') : TODOS_LOS_PASOS
}

export function esPaso(valor: string | null): valor is Paso {
  return TODOS_LOS_PASOS.some((p) => p.paso === valor)
}

/** El paso pedido en la URL, salvo que le falte algo de un paso anterior: entonces, el primero incompleto. */
export function pasoAlcanzable(pasos: DefinicionPaso[], pedido: Paso | null, sel: Selecciones): number {
  const i = pasos.findIndex((p) => p.paso === pedido)
  const objetivo = i === -1 ? 0 : i
  for (let j = 0; j < objetivo; j++) {
    if (pasos[j].campos.some((c) => sel[c] === null)) return j
  }
  return objetivo
}

/** Si el cliente vuelve y cambia algo, se borra lo elegido después. Si elige lo mismo, se conserva todo. */
export function aplicarEleccion(sel: Selecciones, cambios: Partial<Selecciones>): Selecciones {
  const cambiados = (Object.keys(cambios) as (keyof Selecciones)[]).filter((c) => cambios[c] !== sel[c])
  if (cambiados.length === 0) return sel
  const orden = TODOS_LOS_PASOS.flatMap((p) => p.campos)
  const primero = Math.min(...cambiados.map((c) => orden.indexOf(c)))
  const resultado = { ...sel, ...cambios }
  for (const campo of orden.slice(primero + 1)) {
    if (!(campo in cambios)) resultado[campo] = null
  }
  return resultado
}