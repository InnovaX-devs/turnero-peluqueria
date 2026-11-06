/** Usuario de recepción / secretaría (issue #70). No está vinculado a un barbero. */
export interface UsuarioRecepcion {
  id: string
  email: string
  activo: boolean
  creadoEn: string
}
