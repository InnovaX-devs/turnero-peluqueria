/**
 * Términos por rubro (issue #65): cómo se nombran las cosas en el panel, la página pública y los mensajes.
 * Un negocio de SALUD no tiene "barberos" ni "cortes": tiene profesionales, pacientes y atenciones.
 *
 * Tiene su espejo en el backend: Rubro.java (lo usan los mails y los mensajes de WhatsApp).
 * Si se cambia una palabra acá, hay que cambiarla allá también.
 */

export type Rubro = 'BARBERIA' | 'PELUQUERIA' | 'ESTETICA' | 'SALUD'

/** Los negocios anteriores a los rubros son barberías; también es lo que se usa mientras no se sabe el rubro. */
export const RUBRO_POR_DEFECTO: Rubro = 'BARBERIA'

/** Una palabra con su plural y su género, para armar frases con el artículo correcto. */
interface Palabra {
  singular: string
  plural: string
  femenino: boolean
}

/** Una palabra ya declinada: lo que se usa en las pantallas. */
export interface Termino {
  /** barbero */
  s: string
  /** barberos */
  p: string
  /** Barbero */
  S: string
  /** Barberos */
  P: string
  /** la barbería / el barbero */
  el: string
  /** La barbería / El barbero */
  El: string
  /** de la barbería / del barbero */
  del: string
  /** a la barbería / al barbero */
  al: string
  /** esta barbería / este barbero */
  este: string
  /** Esta barbería / Este barbero */
  Este: string
  /** esa prestación / ese servicio */
  ese: string
  /** otra prestación / otro servicio */
  otro: string
  /** un barbero / una barbería */
  un: string
  /** Vocal de género para concordar: "registrad" + o → registrado / registrada. */
  o: string
  /** Singular o plural según la cantidad: cantidad(1) → "corte", cantidad(3) → "cortes". */
  cantidad: (n: number) => string
}

export interface Terminos {
  rubro: Rubro
  /** El negocio: barbería, peluquería, centro de estética, consultorio. */
  negocio: Termino
  /** Quien atiende: barbero, estilista, esteticista, profesional. */
  profesional: Termino
  /** Quien se atiende: cliente o paciente. */
  cliente: Termino
  /** Lo que se reserva: servicio, tratamiento o prestación. */
  servicio: Termino
  /** Cada vez que se atiende a alguien: corte (barbería) o atención. */
  atencion: Termino
  /** Ejemplo para el campo de notas de la reserva. */
  ejemploNota: string
  /** Ejemplo para las notas internas de un cliente / paciente. */
  ejemploNotaFicha: string
}

type Diccionario = Record<'negocio' | 'profesional' | 'cliente' | 'servicio' | 'atencion', Palabra> & { ejemploNota: string; ejemploNotaFicha: string }

const DICCIONARIO: Record<Rubro, Diccionario> = {
  BARBERIA: {
    negocio: { singular: 'barbería', plural: 'barberías', femenino: true },
    profesional: { singular: 'barbero', plural: 'barberos', femenino: false },
    cliente: { singular: 'cliente', plural: 'clientes', femenino: false },
    servicio: { singular: 'servicio', plural: 'servicios', femenino: false },
    atencion: { singular: 'corte', plural: 'cortes', femenino: false },
    ejemploNota: 'Ej: quiero el mismo corte que la última vez',
    ejemploNotaFicha: 'Ej: degradé en 2, arriba tijera',
  },
  PELUQUERIA: {
    negocio: { singular: 'peluquería', plural: 'peluquerías', femenino: true },
    profesional: { singular: 'estilista', plural: 'estilistas', femenino: false },
    cliente: { singular: 'cliente', plural: 'clientes', femenino: false },
    servicio: { singular: 'servicio', plural: 'servicios', femenino: false },
    atencion: { singular: 'atención', plural: 'atenciones', femenino: true },
    ejemploNota: 'Ej: quiero el mismo color que la última vez',
    ejemploNotaFicha: 'Ej: tono 7.1, sin amoníaco',
  },
  ESTETICA: {
    negocio: { singular: 'centro de estética', plural: 'centros de estética', femenino: false },
    profesional: { singular: 'esteticista', plural: 'esteticistas', femenino: false },
    cliente: { singular: 'cliente', plural: 'clientes', femenino: false },
    servicio: { singular: 'tratamiento', plural: 'tratamientos', femenino: false },
    atencion: { singular: 'atención', plural: 'atenciones', femenino: true },
    ejemploNota: 'Ej: es la primera vez que me hago este tratamiento',
    ejemploNotaFicha: 'Ej: prefiere los turnos por la mañana',
  },
  SALUD: {
    negocio: { singular: 'consultorio', plural: 'consultorios', femenino: false },
    profesional: { singular: 'profesional', plural: 'profesionales', femenino: false },
    cliente: { singular: 'paciente', plural: 'pacientes', femenino: false },
    servicio: { singular: 'prestación', plural: 'prestaciones', femenino: true },
    atencion: { singular: 'atención', plural: 'atenciones', femenino: true },
    ejemploNota: 'Ej: es mi primera consulta',
    ejemploNotaFicha: 'Ej: prefiere los turnos por la mañana',
  },
}

const mayuscula = (texto: string) => texto.charAt(0).toUpperCase() + texto.slice(1)

function declinar({ singular, plural, femenino }: Palabra): Termino {
  const el = (femenino ? 'la ' : 'el ') + singular
  const este = (femenino ? 'esta ' : 'este ') + singular
  return {
    s: singular,
    p: plural,
    S: mayuscula(singular),
    P: mayuscula(plural),
    el,
    El: mayuscula(el),
    del: (femenino ? 'de la ' : 'del ') + singular,
    al: (femenino ? 'a la ' : 'al ') + singular,
    este,
    Este: mayuscula(este),
    ese: (femenino ? 'esa ' : 'ese ') + singular,
    otro: (femenino ? 'otra ' : 'otro ') + singular,
    un: (femenino ? 'una ' : 'un ') + singular,
    o: femenino ? 'a' : 'o',
    cantidad: (n) => (n === 1 ? singular : plural),
  }
}

const CACHE = new Map<Rubro, Terminos>()

/** Los términos del rubro. Si el rubro no se conoce (todavía no cargó, o un valor nuevo del backend), barbería. */
export function terminosDe(rubro: Rubro | null | undefined): Terminos {
  const clave: Rubro = rubro && rubro in DICCIONARIO ? rubro : RUBRO_POR_DEFECTO
  let terminos = CACHE.get(clave)
  if (!terminos) {
    const d = DICCIONARIO[clave]
    terminos = {
      rubro: clave,
      negocio: declinar(d.negocio),
      profesional: declinar(d.profesional),
      cliente: declinar(d.cliente),
      servicio: declinar(d.servicio),
      atencion: declinar(d.atencion),
      ejemploNota: d.ejemploNota,
      ejemploNotaFicha: d.ejemploNotaFicha,
    }
    CACHE.set(clave, terminos)
  }
  return terminos
}
