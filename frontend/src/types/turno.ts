/** Lo que ve el cliente en /turno/:token. Fechas ISO en UTC; precio en centavos. */
export interface TurnoPublico {
  token: string
  estado: 'RESERVADA' | 'COMPLETADA' | 'CANCELADA' | 'AUSENTE'
  inicio: string
  fin: string
  clienteNombre: string | null
  precio: number
  barberia: { nombre: string; slug: string }
  sucursal: { id: string; nombre: string; direccion: string; ciudad: string; telefono: string | null }
  servicio: { id: string; nombre: string; duracionMin: number }
  barbero: { id: string; nombre: string }
  anticipacionCancelacionMin: number
  /** Hasta cuándo se puede cancelar o cambiar online */
  limiteModificacion: string
  puedeCancelar: boolean
  puedeReprogramar: boolean
}
