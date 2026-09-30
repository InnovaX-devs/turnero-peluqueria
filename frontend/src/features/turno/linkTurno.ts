/** Link que el cliente guarda para cancelar o cambiar su turno. */
export function linkDelTurno(token: string, origen: string = window.location.origin): string {
  return `${origen}/turno/${token}`
}

/** Abre WhatsApp para mandarse el link a uno mismo (o a quien quiera). */
export function linkWhatsAppParaGuardar(link: string, barberia: string): string {
  const texto = `Mi turno en ${barberia}. Para cancelarlo o cambiarlo: ${link}`
  return `https://wa.me/?text=${encodeURIComponent(texto)}`
}

/** WhatsApp de la sucursal, para cuando ya no se puede cambiar online. */
export function linkWhatsAppSucursal(telefono: string): string {
  return `https://wa.me/${telefono.replace(/\D/g, '')}`
}

/** 120 → "2 horas", 90 → "1 h 30 min", 30 → "30 minutos". Igual que el mensaje del backend. */
export function textoAnticipacion(minutos: number): string {
  const horas = Math.floor(minutos / 60)
  const resto = minutos % 60
  if (horas === 0) return `${resto} ${resto === 1 ? 'minuto' : 'minutos'}`
  if (resto === 0) return `${horas} ${horas === 1 ? 'hora' : 'horas'}`
  return `${horas} h ${resto} min`
}
