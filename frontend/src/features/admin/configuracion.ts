import type { ActualizarBarberia } from '../../types/barberia'

export const INTERVALOS = [15, 30]
export const ANTICIPACIONES_MINIMAS = [0, 15, 30, 60, 120, 180, 360, 720, 1440, 2880]
export const ANTICIPACIONES_CANCELACION = [0, 30, 60, 120, 180, 360, 720, 1440, 2880]

/** Estado del formulario: los campos de texto/número se guardan como string mientras se editan. */
export interface Formulario {
  nombre: string
  slug: string
  logoUrl: string
  reservasOnline: boolean
  anticipacionMinimaMin: number
  anticipacionMaximaDias: string
  intervaloSlotsMin: number
  anticipacionCancelacionMin: number
}

const SLUG_VALIDO = /^[a-z0-9]+(-[a-z0-9]+)*$/
const MINUTOS_POR_DIA = 24 * 60

/** Convierte lo que tipea el usuario en un slug válido: "Mi Barbería" -> "mi-barberia". */
export function normalizarSlug(texto: string): string {
  return texto
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '') // saca tildes
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+/, '')
}

/** Lo que se manda a la API. */
export function aDatos(f: Formulario): ActualizarBarberia {
  return {
    nombre: f.nombre.trim(),
    slug: f.slug.replace(/-+$/, ''),
    logoUrl: f.logoUrl.trim() === '' ? null : f.logoUrl.trim(),
    reservasOnline: f.reservasOnline,
    anticipacionMinimaMin: f.anticipacionMinimaMin,
    anticipacionMaximaDias: Number(f.anticipacionMaximaDias),
    intervaloSlotsMin: f.intervaloSlotsMin,
    anticipacionCancelacionMin: f.anticipacionCancelacionMin,
  }
}

export function sonIguales(a: ActualizarBarberia, b: ActualizarBarberia): boolean {
  return (
    a.nombre === b.nombre &&
    a.slug === b.slug &&
    a.logoUrl === b.logoUrl &&
    a.reservasOnline === b.reservasOnline &&
    a.anticipacionMinimaMin === b.anticipacionMinimaMin &&
    a.anticipacionMaximaDias === b.anticipacionMaximaDias &&
    a.intervaloSlotsMin === b.intervaloSlotsMin &&
    a.anticipacionCancelacionMin === b.anticipacionCancelacionMin
  )
}

/** Mismas reglas que el backend, para avisar sin ir al servidor. El backend igual las valida. */
export function validar(f: Formulario): Record<string, string> {
  const d = aDatos(f)
  const errores: Record<string, string> = {}

  if (d.nombre === '') errores.nombre = 'El nombre es obligatorio'

  if (d.slug.length < 3) errores.slug = 'El slug tiene que tener al menos 3 caracteres'
  else if (!SLUG_VALIDO.test(d.slug)) errores.slug = 'Solo minúsculas, números y guiones, ej: mi-barberia'

  if (d.logoUrl !== null && !/^https?:\/\/\S+$/.test(d.logoUrl)) {
    errores.logoUrl = 'La URL del logo tiene que empezar con http:// o https://'
  }

  if (!Number.isInteger(d.anticipacionMaximaDias) || d.anticipacionMaximaDias < 1 || d.anticipacionMaximaDias > 365) {
    errores.anticipacionMaximaDias = 'Ingresá un número de días entre 1 y 365'
  } else if (d.anticipacionMinimaMin >= d.anticipacionMaximaDias * MINUTOS_POR_DIA) {
    errores.anticipacionMinimaMin = 'La anticipación mínima tiene que ser menor que la máxima'
  }

  return errores
}

/** 90 -> "1 h 30 min", 1440 -> "1 día". */
export function etiquetaMinutos(min: number): string {
  if (min < 60) return `${min} min`
  if (min % MINUTOS_POR_DIA === 0) {
    const dias = min / MINUTOS_POR_DIA
    return dias === 1 ? '1 día' : `${dias} días`
  }
  const horas = Math.floor(min / 60)
  const resto = min % 60
  return resto === 0 ? `${horas} h` : `${horas} h ${resto} min`
}

