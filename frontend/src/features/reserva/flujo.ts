export type Paso = 'sucursal' | 'servicio' | 'barbero' | 'horario' | 'datos'

/** Valor de barberoId cuando el cliente elige "Cualquier barbero". */
export const BARBERO_CUALQUIERA = 'cualquiera'

export interface Selecciones {
  sucursalId: string | null
  servicioId: string | null
  barberoId: string | null // 'cualquiera' = primer barbero disponible
  fecha: string | null // YYYY-MM-DD
  hora: string | null // HH:mm
}

export const SELECCIONES_VACIAS: Selecciones = {
  sucursalId: null, servicioId: null, barberoId: null, fecha: null, hora: null,
}

interface DefinicionPaso {
  paso: Paso
  titulo: string
  campos: (keyof Selecciones)[]
}

const TODOS_LOS_PASOS: DefinicionPaso[] = [
  { paso: 'sucursal', titulo: 'Elegí la sucursal', campos: ['sucursalId'] },
  { paso: 'servicio', titulo: 'Elegí el servicio', campos: ['servicioId'] },
  { paso: 'barbero', titulo: 'Elegí el barbero', campos: ['barberoId'] },
  { paso: 'horario', titulo: 'Elegí día y horario', campos: ['fecha', 'hora'] },
  { paso: 'datos', titulo: 'Tus datos', campos: [] },
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