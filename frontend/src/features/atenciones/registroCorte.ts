import type { MetodoPago, OpcionesRegistro } from '../../types/registroCorte'

export const METODOS_PAGO: { valor: MetodoPago; etiqueta: string }[] = [
  { valor: 'EFECTIVO', etiqueta: 'Efectivo' },
  { valor: 'TRANSFERENCIA', etiqueta: 'Transferencia' },
  { valor: 'MERCADOPAGO', etiqueta: 'Mercado Pago' },
  { valor: 'TARJETA', etiqueta: 'Tarjeta' },
]

// ---------- Preferencias: el último servicio, pago y barbero usados quedan preelegidos ----------

const CLAVE_PREFERENCIAS = 'registroCorte.preferencias'

interface Preferencias {
  servicioId?: string
  metodoPago?: MetodoPago
  barberoId?: string
}

function leerPreferencias(): Preferencias {
  try {
    const valor: unknown = JSON.parse(localStorage.getItem(CLAVE_PREFERENCIAS) ?? '{}')
    return valor && typeof valor === 'object' ? (valor as Preferencias) : {}
  } catch {
    return {}
  }
}

export function guardarPreferencias(preferencias: Preferencias): void {
  try {
    localStorage.setItem(CLAVE_PREFERENCIAS, JSON.stringify(preferencias))
  } catch {
    // sin storage igual funciona, solo no recuerda la última elección
  }
}

export interface ValoresIniciales {
  servicioId: string | null
  precio: string
  metodoPago: MetodoPago
  barberoId: string | null
}

export function valoresIniciales(opciones: OpcionesRegistro): ValoresIniciales {
  const prefs = leerPreferencias()
  const servicio = opciones.servicios.find((s) => s.id === prefs.servicioId) ?? opciones.servicios[0]
  const barbero =
    opciones.barberos.find((b) => b.id === opciones.barberoPropioId) ??
    opciones.barberos.find((b) => b.id === prefs.barberoId) ??
    (opciones.barberos.length === 1 ? opciones.barberos[0] : undefined)
  return {
    servicioId: servicio ? servicio.id : null,
    precio: servicio ? centavosAPesos(servicio.precio) : '',
    metodoPago: prefs.metodoPago ?? 'EFECTIVO',
    barberoId: barbero ? barbero.id : null,
  }
}

// ---------- Precio ----------

/** 1500000 -> "15000" (lo que se muestra en el input) */
export function centavosAPesos(centavos: number): string {
  return String(centavos / 100).replace('.', ',')
}

/** "15.000" o "15000,50" -> centavos. null si no es un número válido. */
export function pesosACentavos(texto: string): number | null {
  const limpio = texto.trim().replace(/\./g, '').replace(',', '.')
  if (limpio === '') return null
  const numero = Number(limpio)
  if (!Number.isFinite(numero) || numero < 0) return null
  return Math.round(numero * 100)
}

export function formatearPrecio(centavos: number): string {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    maximumFractionDigits: 2,
  }).format(centavos / 100)
}

// ---------- Fecha y teléfono ----------

/** Ahora en la zona de la barbería, en el formato del input datetime-local: "2026-09-29T18:30" */
export function ahoraEnZona(zona: string): string {
  const partes = new Intl.DateTimeFormat('en-CA', {
    timeZone: zona,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date())
  const parte = (tipo: string) => partes.find((p) => p.type === tipo)?.value ?? '00'
  return parte('year') + '-' + parte('month') + '-' + parte('day') + 'T' + parte('hour') + ':' + parte('minute')
}

/**
 * Lo que tipea el usuario -> formato E.164 que pide la base.
 * "358 4111111" (celular con código de área, sin 0 ni 15) -> "+5493584111111"
 */
export function normalizarTelefono(texto: string): string {
  const digitos = texto.replace(/\D/g, '')
  if (texto.trim().startsWith('+') || digitos.startsWith('54')) return '+' + digitos
  if (digitos.length === 10) return '+549' + digitos
  return '+' + digitos
}