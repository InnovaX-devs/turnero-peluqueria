import { useQueryClient } from '@tanstack/react-query'

export const CLAVE_LISTA_SERVICIOS = ['servicios', 'lista'] as const

/** Máximo que acepta el backend: 1.000.000.000 de centavos. */
const PRECIO_MAXIMO_PESOS = 10_000_000

/** 1500000 → "15000"; 155050 → "1550.50". Para mostrar en el input. */
export function centavosAPesosTexto(centavos: number): string {
  const pesos = centavos / 100
  return Number.isInteger(pesos) ? String(pesos) : pesos.toFixed(2)
}

/** "15000" / "15000,50" / "15000.5" → centavos. Null si no es un importe válido. */
export function pesosACentavos(texto: string): number | null {
  const limpio = texto.trim().replace(',', '.')
  if (!/^\d+(\.\d{1,2})?$/.test(limpio)) return null
  const pesos = Number(limpio)
  if (pesos > PRECIO_MAXIMO_PESOS) return null
  return Math.round(pesos * 100)
}

/** Cuando cambia algo de los servicios, también hay que refrescar los selectores que los usan. */
export function useInvalidarServicios() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: CLAVE_LISTA_SERVICIOS })
    queryClient.invalidateQueries({ queryKey: ['barberos', 'opciones'] })
    queryClient.invalidateQueries({ queryKey: ['registro-corte-opciones'] })
  }
}