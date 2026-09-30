import { aFechaLocal } from '../../lib/fechas'

export const DIAS_SEMANA = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']
export const DIAS_CORTOS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

/** "2020-03-17" → "17/03/2020" */
export function fechaCorta(iso: string): string {
  return aFechaLocal(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

/** Ausentes sobre turnos que ya pasaron (atendidos + ausentes). null si no hubo ninguno. */
export function tasaAusentismo(atendidos: number, ausentes: number): number | null {
  const total = atendidos + ausentes
  return total === 0 ? null : Math.round((ausentes / total) * 100)
}

/** Link de WhatsApp con un mensaje ya escrito. El teléfono viene en formato +549... */
export function linkWhatsApp(telefono: string, nombre: string, barberia: string): string {
  const primerNombre = nombre.trim().split(/\s+/)[0]
  const mensaje =
    `¡Hola ${primerNombre}! ¿Cómo andás? Hace un tiempo que no te vemos por ${barberia}. ` +
    '¿Querés que te reservemos un turno?'
  return `https://wa.me/${telefono.replace(/\D/g, '')}?text=${encodeURIComponent(mensaje)}`
}
