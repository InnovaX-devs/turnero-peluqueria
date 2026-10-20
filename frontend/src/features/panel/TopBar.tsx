import { useRef, useState, type ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { ChevronDown, CircleUser, LogOut, Plus, Scissors } from 'lucide-react'
import { useCerrarAfuera } from '../../hooks/useCerrarAfuera'
import type { Usuario } from '../../types/auth'
import { ITEMS_GESTION, ITEMS_NEGOCIO, ITEMS_PRINCIPALES, ITEM_CUENTA, visibles, type ItemNav } from './navegacion'

interface Props {
  usuario: Usuario | null
  onRegistrar: () => void
  onCerrarSesion: () => void
}

// Pills sobre la barra de marca; el activo lleva un fondo negro con texto blanco.
const ITEM_BASE =
  'flex cursor-pointer items-center gap-2 rounded-full px-3.5 py-2 text-[13px] font-medium transition-colors'
const ITEM_ACTIVO = 'bg-acento text-sobre-acento'
const ITEM_INACTIVO = 'text-barra-texto/75 hover:bg-barra-texto/[0.08] hover:text-barra-texto'

const claseLink = ({ isActive }: { isActive: boolean }) => `${ITEM_BASE} ${isActive ? ITEM_ACTIVO : ITEM_INACTIVO}`

/** Logo de la barbería si cargó uno; si no (o si la imagen no carga), lo que se pase como alternativa. */
function ImagenBarberia({ usuario, className, alternativa }: { usuario: Usuario | null; className: string; alternativa: ReactNode }) {
  const url = usuario?.barberiaLogoUrl ?? null
  const [rota, setRota] = useState<string | null>(null)
  if (url === null || rota === url) return <>{alternativa}</>
  return <img src={url} alt="" className={className} onError={() => setRota(url)} />
}

function Logo({ usuario }: { usuario: Usuario | null }) {
  return (
    <NavLink to="/panel" className="flex shrink-0 items-center gap-2.5" aria-label="Turnea, ir al inicio">
      <ImagenBarberia
        usuario={usuario}
        className="size-8 rounded-[9px] bg-white object-cover"
        alternativa={
          <span className="bg-grad grid size-8 place-items-center rounded-[9px] text-sobre-acento">
            <Scissors size={17} strokeWidth={2.2} aria-hidden="true" />
          </span>
        }
      />
      <span className="font-titulo text-[15.5px] font-bold tracking-[-0.02em] text-barra-texto">Turnea</span>
    </NavLink>
  )
}

function Enlace({ item }: { item: ItemNav }) {
  const Icono = item.icono
  return (
    <NavLink to={item.to} end={item.exacto} className={claseLink}>
      <Icono size={15} strokeWidth={1.8} aria-hidden="true" />
      {item.etiqueta}
    </NavLink>
  )
}

/** Menú desplegable de la top bar (mismo color que la barra). */
function Desplegable({
  etiqueta,
  icono,
  alineado = 'izquierda',
  claseBoton,
  children,
}: {
  etiqueta: string
  icono?: React.ReactNode
  alineado?: 'izquierda' | 'derecha'
  claseBoton: string
  children: (cerrar: () => void) => React.ReactNode
}) {
  const [abierto, setAbierto] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const cerrar = () => setAbierto(false)
  useCerrarAfuera(ref, abierto, cerrar)

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        className={claseBoton}
        aria-haspopup="menu"
        aria-expanded={abierto}
        onClick={() => setAbierto((v) => !v)}
      >
        {icono}
        {etiqueta}
        <ChevronDown size={12} strokeWidth={2} aria-hidden="true" className={abierto ? 'rotate-180 transition' : 'transition'} />
      </button>
      {abierto && (
        <div
          role="menu"
          className={[
            'absolute top-[calc(100%+6px)] z-30 min-w-52 rounded-xl border border-barra-texto/15 bg-barra p-1.5 shadow-xl',
            alineado === 'derecha' ? 'right-0' : 'left-0',
          ].join(' ')}
        >
          {children(cerrar)}
        </div>
      )}
    </div>
  )
}

const claseItemMenu = ({ isActive }: { isActive: boolean }) =>
  [
    'flex w-full cursor-pointer items-center gap-2.5 rounded-lg px-3 py-2 text-left text-[13px] transition-colors',
    isActive ? 'bg-acento text-sobre-acento' : 'text-barra-texto/85 hover:bg-barra-texto/[0.08] hover:text-barra-texto',
  ].join(' ')

export function TopBar({ usuario, onRegistrar, onCerrarSesion }: Props) {
  const esAdmin = usuario?.rol === 'ADMIN'
  const { pathname } = useLocation()
  const gestionActiva = ITEMS_GESTION.some((i) => pathname.startsWith(i.to))

  return (
    <>
      {/* Celu: solo la marca; la navegación va abajo */}
      <header className="nav-reset sticky top-0 z-10 flex h-14 items-center justify-between bg-barra px-4 md:hidden">
        <Logo usuario={usuario} />
        <span className="etiqueta-mono text-barra-texto/60">{esAdmin ? 'Admin' : 'Barbero'}</span>
      </header>

      <header className="nav-reset sticky top-0 z-10 hidden h-[68px] items-center gap-6 bg-barra px-7 md:flex">
        <Logo usuario={usuario} />

        <nav aria-label="Secciones del panel" className="flex flex-1 items-center gap-1">
          {visibles(ITEMS_PRINCIPALES, esAdmin).map((i) => (
            <Enlace key={i.to} item={i} />
          ))}
          {visibles(ITEMS_NEGOCIO, esAdmin).map((i) => (
            <Enlace key={i.to} item={i} />
          ))}
          {esAdmin && (
            <Desplegable
              etiqueta="Gestión"
              claseBoton={`${ITEM_BASE} ${gestionActiva ? ITEM_ACTIVO : ITEM_INACTIVO}`}
            >
              {(cerrar) =>
                ITEMS_GESTION.map((i) => (
                  <NavLink key={i.to} to={i.to} role="menuitem" className={claseItemMenu} onClick={cerrar}>
                    <i.icono size={14} strokeWidth={1.75} aria-hidden="true" />
                    {i.etiqueta}
                  </NavLink>
                ))
              }
            </Desplegable>
          )}
        </nav>

        <button
          type="button"
          onClick={onRegistrar}
          className="bg-grad shadow-marca flex cursor-pointer items-center gap-1.5 rounded-full px-5 py-2.5 text-sm font-semibold text-sobre-acento
            transition-transform hover:-translate-y-0.5"
        >
          <Plus size={16} strokeWidth={2.5} aria-hidden="true" />
          Registrar corte
        </button>

        <Desplegable
          alineado="derecha"
          etiqueta=""
          icono={
            <ImagenBarberia
              usuario={usuario}
              className="size-9 rounded-full bg-white object-cover"
              alternativa={
                <span className="bg-grad grid size-9 place-items-center rounded-full text-sm font-bold uppercase text-sobre-acento">
                  {usuario?.email.charAt(0) ?? '?'}
                </span>
              }
            />
          }
          claseBoton="flex cursor-pointer items-center gap-1.5 rounded-full py-1 pl-1 pr-2 text-barra-texto/70 hover:text-barra-texto"
        >
          {(cerrar) => (
            <>
              <div className="border-b border-barra-texto/15 px-3 pb-2.5 pt-2">
                {usuario?.barberiaNombre && (
                  <p className="m-0 truncate text-[13px] font-semibold text-barra-texto">{usuario.barberiaNombre}</p>
                )}
                <p className="m-0 truncate text-[13px] font-medium text-barra-texto">{usuario?.email}</p>
                <p className="etiqueta-mono m-0 mt-0.5 text-barra-texto/60">{esAdmin ? 'Admin' : 'Barbero'}</p>
              </div>
              <NavLink to={ITEM_CUENTA.to} role="menuitem" className={(s) => `${claseItemMenu(s)} mt-1`} onClick={cerrar}>
                <CircleUser size={14} strokeWidth={1.75} aria-hidden="true" />
                Mi cuenta
              </NavLink>
              <button
                type="button"
                role="menuitem"
                className={claseItemMenu({ isActive: false })}
                onClick={onCerrarSesion}
              >
                <LogOut size={14} strokeWidth={1.75} aria-hidden="true" />
                Cerrar sesión
              </button>
            </>
          )}
        </Desplegable>
      </header>
    </>
  )
}