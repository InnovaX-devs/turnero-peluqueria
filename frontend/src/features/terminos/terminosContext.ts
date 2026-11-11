import { createContext } from 'react'
import { terminosDe, type Terminos } from '../../lib/terminos'

/** Sin proveedor arriba, se habla como una barbería (lo que había antes de los rubros). */
export const TerminosContext = createContext<Terminos>(terminosDe(null))
