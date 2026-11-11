/** Solo los números del DNI: saca puntos, espacios y lo demás. "12.345.678" → "12345678". */
export function soloDigitosDni(texto: string): string {
  return texto.replace(/\D/g, '')
}

/** Mensaje de error si el DNI no tiene 7 u 8 números (puntos y espacios se aceptan). undefined si está bien. */
export function errorDeDni(texto: string): string | undefined {
  const t = texto.trim()
  if (t === '') return 'Ingresá el DNI.'
  if (/[^\d.\s]/.test(t) || !/^\d{7,8}$/.test(soloDigitosDni(t))) {
    return 'El DNI tiene que tener 7 u 8 números.'
  }
  return undefined
}

/** "12345678" → "12.345.678". Si no tiene 7 u 8 dígitos lo devuelve como vino. */
export function formatearDni(dni: string): string {
  return /^\d{7,8}$/.test(dni) ? dni.replace(/\B(?=(\d{3})+(?!\d))/g, '.') : dni
}
