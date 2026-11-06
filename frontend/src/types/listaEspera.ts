/** Cliente anotado para un día en que no había lugar (issue #71). */
export interface EntradaListaEspera {
  id: string
  clienteId: string
  clienteNombre: string
  clienteTelefono: string
  /** null = con cualquier barbero */
  profesionalId: string | null
  profesionalNombre: string | null
  fecha: string
  nota: string | null
  /** Cuándo se liberó un lugar que le sirve. null = todavía no. */
  avisoEn: string | null
  avisoDetalle: string | null
  creadoEn: string
}

export interface AnotarListaEspera {
  clienteId: string
  profesionalId: string | null
  fecha: string
  nota: string | null
}
