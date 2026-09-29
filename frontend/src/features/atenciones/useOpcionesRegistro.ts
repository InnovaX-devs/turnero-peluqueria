import { useQuery } from '@tanstack/react-query'
import { obtenerOpcionesRegistro } from '../../api/registroCorte'

/** Servicios, barberos y sucursales para el formulario. Se precarga desde el panel. */
export function useOpcionesRegistro() {
  return useQuery({
    queryKey: ['registro-corte-opciones'],
    queryFn: obtenerOpcionesRegistro,
    staleTime: 5 * 60 * 1000,
  })
}