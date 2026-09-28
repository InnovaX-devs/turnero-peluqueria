// Arma el teléfono en formato E.164 (+5493584111111), que es lo que pide el backend
// y lo que sirve para escribirle al cliente por WhatsApp.

export interface Pais {
  codigo: string
  nombre: string
  bandera: string
  prefijo: string
  ejemplo: string
}

export const PAISES: Pais[] = [
  { codigo: 'AR', nombre: 'Argentina', bandera: '🇦🇷', prefijo: '54', ejemplo: '358 4111111' },
  { codigo: 'UY', nombre: 'Uruguay', bandera: '🇺🇾', prefijo: '598', ejemplo: '94 123 456' },
  { codigo: 'CL', nombre: 'Chile', bandera: '🇨🇱', prefijo: '56', ejemplo: '9 1234 5678' },
  { codigo: 'PY', nombre: 'Paraguay', bandera: '🇵🇾', prefijo: '595', ejemplo: '981 123456' },
  { codigo: 'BO', nombre: 'Bolivia', bandera: '🇧🇴', prefijo: '591', ejemplo: '71234567' },
  { codigo: 'BR', nombre: 'Brasil', bandera: '🇧🇷', prefijo: '55', ejemplo: '11 91234 5678' },
]

export type ResultadoTelefono = { ok: true; e164: string } | { ok: false; error: string }

/**
 * Argentina: acepta el número como lo escriba la gente ("0358 15 411-1111", "3584111111",
 * "+54 9 358 4111111") y lo normaliza a +549 + código de área + número (10 dígitos).
 */
function argentina(digitos: string): ResultadoTelefono {
  let d = digitos
  if (d.startsWith('549') && d.length === 13) d = d.slice(3)
  else if (d.startsWith('54') && d.length === 12) d = d.slice(2)
  if (d.startsWith('0')) d = d.slice(1)
  if (d.startsWith('9') && d.length === 11) d = d.slice(1)

  // "358 15 4111111": el 15 va después del código de área (de 2 a 4 dígitos)
  if (d.length === 12) {
    for (const largoArea of [2, 3, 4]) {
      if (d.slice(largoArea, largoArea + 2) === '15') {
        d = d.slice(0, largoArea) + d.slice(largoArea + 2)
        break
      }
    }
  }

  if (d.length !== 10) {
    return { ok: false, error: 'Revisá el número: código de área sin el 0 y número sin el 15 (10 números en total).' }
  }
  return { ok: true, e164: `+549${d}` }
}

export function armarTelefono(pais: Pais, entrada: string): ResultadoTelefono {
  const digitos = entrada.replace(/\D/g, '')
  if (digitos === '') return { ok: false, error: 'Ingresá tu número de teléfono.' }

  if (pais.codigo === 'AR') return argentina(digitos)

  const numero = digitos.replace(/^0+/, '')
  const completo = pais.prefijo + numero
  if (completo.length < 8 || completo.length > 15) {
    return { ok: false, error: 'Revisá el número de teléfono.' }
  }
  return { ok: true, e164: `+${completo}` }
}
