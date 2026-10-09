import {
  CalendarDays,
  ChartColumn,
  CircleUser,
  MessageSquare,
  Scissors,
  Settings,
  Tag,
  Users,
  Wallet,
  type LucideIcon,
} from 'lucide-react'

export interface ItemNav {
  to: string
  etiqueta: string
  icono: LucideIcon
  /** true = solo lo ve un admin */
  soloAdmin?: boolean
  /** true = NavLink exacto (para "/panel", que si no queda activo siempre) */
  exacto?: boolean
}

/** Secciones de uso diario: van directo en la top bar y en la barra inferior. */
export const ITEMS_PRINCIPALES: ItemNav[] = [
  { to: '/panel', etiqueta: 'Inicio', icono: CalendarDays, exacto: true },
  { to: '/panel/clientes', etiqueta: 'Clientes', icono: Users },
  { to: '/panel/mensajes', etiqueta: 'Mensajes', icono: MessageSquare },
]

/** Caja es solo admin; Reportes lo ven todos (el barbero ve únicamente los suyos). En desktop van en la top bar; en el celu, dentro de "Más". */
export const ITEMS_NEGOCIO: ItemNav[] = [
  { to: '/panel/caja', etiqueta: 'Caja', icono: Wallet, soloAdmin: true },
  { to: '/panel/reportes', etiqueta: 'Reportes', icono: ChartColumn },
]

/** Configuración del negocio: desplegable "Gestión" en desktop. */
export const ITEMS_GESTION: ItemNav[] = [
  { to: '/panel/barberos', etiqueta: 'Barberos', icono: Scissors, soloAdmin: true },
  { to: '/panel/servicios', etiqueta: 'Servicios', icono: Tag, soloAdmin: true },
  { to: '/panel/configuracion', etiqueta: 'Configuración', icono: Settings, soloAdmin: true },
]

export const ITEM_CUENTA: ItemNav = { to: '/panel/cuenta', etiqueta: 'Mi cuenta', icono: CircleUser }

export function visibles(items: ItemNav[], esAdmin: boolean) {
  return items.filter((i) => !i.soloAdmin || esAdmin)
}