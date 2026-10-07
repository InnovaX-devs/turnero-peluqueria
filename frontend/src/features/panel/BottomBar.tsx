import { useRef, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { Ellipsis, LogOut, Plus, X } from 'lucide-react'
import { useCerrarAfuera } from '../../hooks/useCerrarAfuera'
import type { Usuario } from '../../types/auth'
import { ITEMS_GESTION, ITEMS_NEGOCIO, ITEMS_PRINCIPALES, ITEM_CUENTA, visibles, type ItemNav } from './navegacion'

interface Props {
  usuario: Usuario | null
  onRegistrar: () => void
  onCerrarSesion: () => void
}

const ETIQUETA_TAB = 'text-[0.7rem] font-semibold'

/** Ícono dentro de una pill: la activa va en negro con texto blanco. */
function IconoTab({ activo, children }: { activo: boolean; children: React.ReactNode }) {
  return (
    <span
      className={[
        'grid h-7 w-14 place-items-center rounded-full transition-colors',
        activo ? 'bg-acento text-sobre-acento' : 'text-barra-texto/60',
      ].join(' ')}
    >
      {children}
    </span>
  )
}

function Tab({ item }: { item: ItemNav }) {
  const Icono = item.icono
  return (
    <NavLink
      to={item.to}
      end={item.exacto}
      className={({ isActive }) =>
        `flex min-h-14 flex-1 flex-col items-center justify-center gap-0.5 no-underline ${ETIQUETA_TAB} ${
          isActive ? 'text-barra-texto' : 'text-barra-texto/60'
        }`
      }
    >
      {({ isActive }) => (
        <>
          <IconoTab activo={isActive}>
            <Icono size={21} strokeWidth={1.8} aria-hidden="true" />
          </IconoTab>
          {item.etiqueta}
        </>
      )}
    </NavLink>
  )
}

export function BottomBar({ usuario, onRegistrar, onCerrarSesion }: Props) {
  const esAdmin = usuario?.rol === 'ADMIN'
  const [masAbierto, setMasAbierto] = useState(false)
  const hojaRef = useRef<HTMLDivElement>(null)
  const { pathname } = useLocation()
  const cerrar = () => setMasAbierto(false)
  useCerrarAfuera(hojaRef, masAbierto, cerrar)

  const itemsMas = [
    ...visibles(ITEMS_NEGOCIO, esAdmin),
    ...visibles(ITEMS_GESTION, esAdmin),
    ITEM_CUENTA,
  ]
  const masActivo = itemsMas.some((i) => pathname.startsWith(i.to))
  const [inicio, ...resto] = ITEMS_PRINCIPALES

  return (
    <>
      <nav
        aria-label="Secciones del panel"
        className="nav-reset fixed inset-x-0 bottom-0 z-10 flex items-end bg-barra pb-[env(safe-area-inset-bottom)] shadow-[0_-4px_16px_rgb(0_0_0/0.15)] md:hidden"
      >
        <Tab item={inicio} />
        <Tab item={resto[0]} />

        {/* Acción principal, al centro y destacada */}
        <div className="flex flex-1 justify-center">
          <button
            type="button"
            onClick={onRegistrar}
            aria-label="Registrar corte"
            className="bg-grad shadow-marca -mt-6 mb-1.5 grid size-14 cursor-pointer place-items-center rounded-full border-4 border-fondo
              text-sobre-acento active:scale-95"
          >
            <Plus size={28} strokeWidth={2.5} aria-hidden="true" />
          </button>
        </div>

        <Tab item={resto[1]} />
        <button
          type="button"
          aria-haspopup="dialog"
          aria-expanded={masAbierto}
          onClick={() => setMasAbierto(true)}
          className={`flex min-h-14 flex-1 cursor-pointer flex-col items-center justify-center gap-0.5 ${ETIQUETA_TAB} ${
            masActivo ? 'text-barra-texto' : 'text-barra-texto/60'
          }`}
        >
          <IconoTab activo={masActivo}>
            <Ellipsis size={21} strokeWidth={1.8} aria-hidden="true" />
          </IconoTab>
          Más
        </button>
      </nav>

      {masAbierto && (
        <div className="nav-reset fixed inset-0 z-30 flex items-end bg-black/50 md:hidden">
          <div
            ref={hojaRef}
            role="dialog"
            aria-modal="true"
            aria-label="Más secciones"
            className="w-full rounded-t-3xl bg-tarjeta px-4 pb-[calc(16px+env(safe-area-inset-bottom))] pt-3 shadow-2xl"
          >
            <div className="mb-2 flex items-center justify-between">
              <div className="min-w-0">
                <p className="m-0 truncate text-sm font-semibold">{usuario?.email}</p>
                <p className="etiqueta-mono m-0 mt-0.5 text-texto-suave">{esAdmin ? 'Admin' : 'Barbero'}</p>
              </div>
              <button
                type="button"
                onClick={cerrar}
                aria-label="Cerrar"
                className="grid size-10 cursor-pointer place-items-center rounded-full text-texto-suave hover:bg-fondo"
              >
                <X size={20} aria-hidden="true" />
              </button>
            </div>

            <ul className="m-0 grid list-none grid-cols-3 gap-2 p-0">
              {itemsMas.map((i) => (
                <li key={i.to}>
                  <NavLink
                    to={i.to}
                    onClick={cerrar}
                    className={({ isActive }) =>
                      [
                        'flex min-h-20 flex-col items-center justify-center gap-1.5 rounded-2xl border px-1 text-center text-xs font-medium no-underline',
                        isActive
                          ? 'border-acento bg-acento-suave text-texto'
                          : 'border-borde bg-fondo text-texto',
                      ].join(' ')
                    }
                  >
                    <i.icono size={22} aria-hidden="true" />
                    {i.etiqueta}
                  </NavLink>
                </li>
              ))}
            </ul>

            <button
              type="button"
              onClick={onCerrarSesion}
              className="mt-3 flex min-h-12 w-full cursor-pointer items-center justify-center gap-2 rounded-xl border border-borde
                bg-tarjeta text-sm font-semibold text-error"
            >
              <LogOut size={18} aria-hidden="true" />
              Cerrar sesión
            </button>
          </div>
        </div>
      )}
    </>
  )
}