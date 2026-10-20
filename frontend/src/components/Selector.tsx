import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react'
import { Check, ChevronDown } from 'lucide-react'
import { useCerrarAfuera } from '../hooks/useCerrarAfuera'
import './selector.css'

export interface OpcionSelector {
  valor: string
  etiqueta: string
}

interface Props {
  valor: string
  onChange: (valor: string) => void
  opciones: OpcionSelector[]
  /** Nombre del campo: lo leen los lectores de pantalla */
  etiqueta: string
  /** Texto cuando no hay ninguna opción elegida (valor vacío y sin opción con valor '') */
  placeholder?: string
  /** campo = ancho completo · pill = filtro redondeado · compacto = chico, para filas de filtros */
  variante?: 'campo' | 'pill' | 'compacto'
  /** De qué lado se alinea la lista respecto del campo */
  alinear?: 'izquierda' | 'derecha'
  invalido?: boolean
  deshabilitado?: boolean
}

/**
 * Reemplazo de <select>: abre una lista debajo del campo, igual en compu y en celu.
 * Siempre se ve igual en todos los navegadores.
 */
export function Selector({
  valor,
  onChange,
  opciones,
  etiqueta,
  placeholder = 'Elegí una opción',
  variante = 'campo',
  alinear = 'izquierda',
  invalido = false,
  deshabilitado = false,
}: Props) {
  const [abierto, setAbierto] = useState(false)
  const raiz = useRef<HTMLDivElement>(null)
  const boton = useRef<HTMLButtonElement>(null)
  const lista = useRef<HTMLDivElement>(null)
  const idLista = useId()

  const cerrar = () => setAbierto(false)
  useCerrarAfuera(raiz, abierto, cerrar)

  const actual = opciones.find((o) => o.valor === valor)
  const texto = actual?.etiqueta ?? placeholder

  // Si la lista se saldría de la pantalla por un lado, se alinea al lado contrario
  useLayoutEffect(() => {
    const el = lista.current
    if (!abierto || !el) return
    el.style.left = ''
    el.style.right = ''
    const { left, right } = el.getBoundingClientRect()
    if (left < 8) {
      el.style.left = '0'
      el.style.right = 'auto'
    } else if (right > window.innerWidth - 8) {
      el.style.right = '0'
      el.style.left = 'auto'
    }
  }, [abierto])

  // Al abrir, el foco va a la opción elegida (o a la primera) para poder usar el teclado
  useEffect(() => {
    if (!abierto) return
    const items = lista.current?.querySelectorAll<HTMLButtonElement>('[role="option"]')
    const elegido = lista.current?.querySelector<HTMLButtonElement>('[aria-selected="true"]')
    ;(elegido ?? items?.[0])?.focus()
  }, [abierto])

  function elegir(nuevo: string) {
    if (nuevo !== valor) onChange(nuevo)
    cerrar()
    boton.current?.focus()
  }

  function alTeclearLista(e: React.KeyboardEvent) {
    if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return
    e.preventDefault()
    const items = Array.from(lista.current?.querySelectorAll<HTMLButtonElement>('[role="option"]') ?? [])
    const i = items.indexOf(document.activeElement as HTMLButtonElement)
    const siguiente = e.key === 'ArrowDown' ? Math.min(i + 1, items.length - 1) : Math.max(i - 1, 0)
    items[siguiente]?.focus()
  }

  const clases = ['selector', `selector-${variante}`, invalido ? 'selector-invalido' : ''].filter(Boolean).join(' ')

  return (
    <div className={clases} ref={raiz}>
      <button
        type="button"
        ref={boton}
        className="selector-boton"
        aria-haspopup="listbox"
        aria-expanded={abierto}
        aria-controls={abierto ? idLista : undefined}
        aria-label={`${etiqueta}: ${texto}`}
        disabled={deshabilitado}
        onClick={() => setAbierto((v) => !v)}
        onKeyDown={(e) => {
          if (e.key === 'ArrowDown' && !abierto) {
            e.preventDefault()
            setAbierto(true)
          }
        }}
      >
        <span className={actual ? 'selector-valor' : 'selector-valor selector-placeholder'}>{texto}</span>
        <ChevronDown size={16} strokeWidth={2} aria-hidden="true" className="selector-flecha" />
      </button>

      {abierto && (
        <div
          id={idLista}
          ref={lista}
          role="listbox"
          aria-label={etiqueta}
          className={`selector-lista selector-${alinear}`}
          onKeyDown={alTeclearLista}
        >
          {opciones.map((o) => (
            <button
              key={o.valor}
              type="button"
              role="option"
              aria-selected={o.valor === valor}
              className="selector-opcion"
              onClick={() => elegir(o.valor)}
            >
              <span>{o.etiqueta}</span>
              {o.valor === valor && <Check size={16} strokeWidth={2.4} aria-hidden="true" />}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}