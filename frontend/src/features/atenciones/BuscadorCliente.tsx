import { useEffect, useState, type KeyboardEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { buscarClientes, crearCliente } from '../../api/registroCorte'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { ClienteResumen } from '../../types/registroCorte'
import { normalizarTelefono } from './registroCorte'

interface Props {
  seleccionado: ClienteResumen | null
  onSeleccionar: (cliente: ClienteResumen | null) => void
}

/** Enter en estos inputs no tiene que registrar el corte (están dentro del formulario). */
function evitarEnter(e: KeyboardEvent<HTMLInputElement>) {
  if (e.key === 'Enter') e.preventDefault()
}

export function BuscadorCliente({ seleccionado, onSeleccionar }: Props) {
  const [texto, setTexto] = useState('')
  const [buscado, setBuscado] = useState('')
  const [creando, setCreando] = useState(false)
  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')

  // Espera a que el usuario deje de tipear para no buscar en cada letra
  useEffect(() => {
    const timer = setTimeout(() => setBuscado(texto.trim()), 250)
    return () => clearTimeout(timer)
  }, [texto])

  const busqueda = useQuery({
    queryKey: ['clientes', buscado],
    queryFn: () => buscarClientes(buscado),
    enabled: buscado.length >= 2,
    staleTime: 30_000,
  })

  const creacion = useMutation({
    mutationFn: () => crearCliente(nombre.trim(), normalizarTelefono(telefono)),
    onSuccess: (nuevo) => {
      onSeleccionar(nuevo)
      setCreando(false)
      setTexto('')
      setNombre('')
      setTelefono('')
    },
  })

  function empezarACrear() {
    // Lo que ya tipeó en el buscador se usa como nombre o como teléfono
    const tieneNumeros = /\d/.test(texto)
    setNombre(tieneNumeros ? '' : texto.trim())
    setTelefono(tieneNumeros ? texto.trim() : '')
    setCreando(true)
  }

  if (seleccionado) {
    return (
      <div className="cliente-elegido">
        <div>
          <strong>{seleccionado.nombre}</strong>
          <span>{seleccionado.telefono}</span>
        </div>
        <button type="button" className="boton-texto" onClick={() => onSeleccionar(null)}>
          Quitar
        </button>
      </div>
    )
  }

  if (creando) {
    const errores = erroresDeCampos(creacion.error)
    return (
      <div className="cliente-nuevo">
        <input
          className="registro-input"
          placeholder="Nombre"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          onKeyDown={evitarEnter}
          autoFocus
        />
        <input
          className="registro-input"
          placeholder="Teléfono, ej: 358 4111111"
          inputMode="tel"
          value={telefono}
          onChange={(e) => setTelefono(e.target.value)}
          onKeyDown={evitarEnter}
        />
        {creacion.error && (
          <p className="registro-error" role="alert">
            {errores.telefono ?? errores.nombre ?? mensajeDeError(creacion.error)}
          </p>
        )}
        <div className="cliente-nuevo-acciones">
          <button type="button" className="boton-secundario" onClick={() => setCreando(false)}>
            Cancelar
          </button>
          <button
            type="button"
            className="boton-principal"
            disabled={creacion.isPending || nombre.trim() === '' || telefono.trim() === ''}
            onClick={() => creacion.mutate()}
          >
            {creacion.isPending ? 'Guardando…' : 'Crear cliente'}
          </button>
        </div>
      </div>
    )
  }

  const resultados = buscado.length >= 2 ? (busqueda.data ?? []) : []

  return (
    <div>
      <input
        className="registro-input"
        type="search"
        placeholder="Buscar por nombre o teléfono"
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
        onKeyDown={evitarEnter}
      />
      {resultados.length > 0 && (
        <ul className="cliente-resultados">
          {resultados.map((c) => (
            <li key={c.id}>
              <button
                type="button"
                className="registro-opcion"
                onClick={() => {
                  onSeleccionar(c)
                  setTexto('')
                }}
              >
                <strong>{c.nombre}</strong>
                <small>{c.telefono}</small>
              </button>
            </li>
          ))}
        </ul>
      )}
      {buscado.length >= 2 && busqueda.isSuccess && resultados.length === 0 && (
        <p className="registro-estado">No se encontró ningún cliente.</p>
      )}
      <button type="button" className="boton-texto" onClick={empezarACrear}>
        + Crear cliente nuevo
      </button>
    </div>
  )
}