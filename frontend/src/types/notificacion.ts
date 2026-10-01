export type TipoNotificacion = 'CONFIRMACION' | 'RECORDATORIO'
export type EstadoNotificacion = 'PENDIENTE' | 'ENVIADA' | 'FALLIDA' | 'OMITIDA'

export interface Notificacion {
  id: string
  atencionId: string
  tipo: TipoNotificacion
  canal: 'WHATSAPP_LINK' | 'EMAIL'
  estado: EstadoNotificacion
  clienteNombre: string | null
  destinatario: string
  barberoNombre: string | null
  turnoInicio: string
  mensaje: string
  linkWhatsApp: string | null
  programadaPara: string
  enviadaEn: string | null
  error: string | null
}