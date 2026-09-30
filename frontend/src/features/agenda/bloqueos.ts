import type { Bloqueo } from '../../types/agenda'
import type { OpcionesRegistro } from '../../types/registroCorte'

const UN_DIA_MS = 24 * 60 * 60 * 1000


export function esDiaCompleto(bloqueo: Bloqueo): boolean {
  return new Date(bloqueo.fin).getTime() - new Date(bloqueo.inicio).getTime() >= UN_DIA_MS
}

export function partesEnZona(iso: string, zona: string): { fecha: string; hora: string } {
  const partes = new Intl.DateTimeFormat('en-CA', {
    timeZone: zona,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(iso))
  const valor = (tipo: string) => partes.find((p) => p.type === tipo)?.value ?? '00'
  return {
    fecha: valor('year') + '-' + valor('month') + '-' + valor('day'),
    hora: valor('hour') + ':' + valor('minute'),
  }
}

export interface ValoresBloqueo {
  barberoId: string | null
  diaCompleto: boolean
  fecha: string
  desde: string
  hasta: string
  motivo: string
}

export function valoresIniciales(
  bloqueo: Bloqueo | null,
  fechaInicial: string,
  opciones: OpcionesRegistro,
): ValoresBloqueo {
  if (bloqueo) {
    const inicio = partesEnZona(bloqueo.inicio, opciones.zonaHoraria)
    const fin = partesEnZona(bloqueo.fin, opciones.zonaHoraria)
    const completo = esDiaCompleto(bloqueo)
    return {
      barberoId: bloqueo.barberoId,
      diaCompleto: completo,
      fecha: inicio.fecha,
      desde: completo ? '' : inicio.hora,
      hasta: completo ? '' : fin.hora,
      motivo: bloqueo.motivo ?? '',
    }
  }
  
  const barberoId = opciones.barberoPropioId ?? (opciones.barberos.length === 1 ? opciones.barberos[0].id : null)
  return { barberoId, diaCompleto: false, fecha: fechaInicial, desde: '', hasta: '', motivo: '' }
}