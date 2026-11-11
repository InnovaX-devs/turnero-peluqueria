import { useEffect, useState, type KeyboardEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { buscarClientes, crearCliente } from '../../api/registroCorte'
import { errorDeDni, formatearDni, soloDigitosDni } from '../../lib/dni'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { ClienteResumen } from '../../types/registroCorte'
import { normalizarTelefono } from './registroCorte'
import { useTerminos } from '../terminos/useTerminos'

interface Props {
  seleccionado: ClienteResumen | null
  onSeleccionar: (cliente: ClienteResumen | null) => void
}

/** Enter en estos inputs no tiene que registrar el corte (están dentro del formulario). */
function evitarEnter(e: KeyboardEvent<HTMLInputElement>) {
  if (e.key === 'Enter') e.preventDefault()
}

/** Teléfono y, si lo tiene (salud), el DNI. */
function detalle(c: ClienteResumen): string {
  return c.dni ? `DNI ${formatearDni(c.dni)} · ${c.telefono}` : c.telefono
}

export function BuscadorCliente({ seleccionado, onSeleccionar }: Props) {
  const t = useTerminos()
  const [texto, setTexto] = useState('')
  const [buscado, setBuscado] = useState('')
  const [creando, setCreando] = useState(false)
  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [dni, setDni] = useState('')
  const [errorDniLocal, setErrorDniLocal] = useState<string | undefined>()

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
    mutationFn: () =>
      crearCliente({
        nombre: nombre.trim(),
        telefono: normalizarTelefono(telefono),
        ...(t.pideDni && { dni: soloDigitosDni(dni) }),
      }),
    onSuccess: (nuevo) => {
      onSeleccionar(nuevo)
      setCreando(false)
      setTexto('')
      setNombre('')
      setTelefono('')
      setDni('')
    },
  })

  function crear() {
    if (t.pideDni) {
      const error = errorDeDni(dni)
      setErrorDniLocal(error)
      if (error) return
    }
    creacion.mutate()
  }

  function empezarACrear() {
    // Lo que ya tipeó en el buscador se usa como nombre o, si son números, como DNI (salud) o teléfono
    const tieneNumeros = /\d/.test(texto)
    setNombre(tieneNumeros ? '' : texto.trim())
    setDni(tieneNumeros && t.pideDni ? soloDigitosDni(texto) : '')
    setTelefono(tieneNumeros && !t.pideDni ? texto.trim() : '')
    setErrorDniLocal(undefined)
    setCreando(true)
  }

  if (seleccionado) {
    return (
      <div className="cliente-elegido">
        <div>
          <strong>{seleccionado.nombre}</strong>
          <span>{detalle(seleccionado)}</span>
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
        {t.pideDni && (
          <input
            className="registro-input"
            placeholder="DNI, sin puntos"
            inputMode="numeric"
            maxLength={10}
            value={dni}
            onChange={(e) => setDni(e.target.value)}
            onKeyDown={evitarEnter}
            aria-invalid={errorDniLocal || errores.dni ? true : undefined}
          />
        )}
        <input
          className="registro-input"
          placeholder="Teléfono, ej: 358 4111111"
          inputMode="tel"
          value={telefono}
          onChange={(e) => setTelefono(e.target.value)}
          onKeyDown={evitarEnter}
        />
        {(errorDniLocal || creacion.error) && (
          <p className="registro-error" role="alert">
            {errorDniLocal ?? errores.dni ?? errores.telefono ?? errores.nombre ?? mensajeDeError(creacion.error)}
          </p>
        )}
        <div className="cliente-nuevo-acciones">
          <button type="button" className="boton-secundario" onClick={() => setCreando(false)}>
            Cancelar
          </button>
          <button
            type="button"
            className="boton-principal"
            disabled={creacion.isPending || nombre.trim() === '' || telefono.trim() === '' || (t.pideDni && dni.trim() === '')}
            onClick={crear}
          >
            {creacion.isPending ? 'Guardando…' : `Crear ${t.cliente.s}`}
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
        placeholder={t.pideDni ? 'Buscar por nombre o DNI' : 'Buscar por nombre o teléfono'}
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
                <small>{detalle(c)}</small>
              </button>
            </li>
          ))}
        </ul>
      )}
      {buscado.length >= 2 && busqueda.isSuccess && resultados.length === 0 && (
        <p className="registro-estado">No se encontró ningún {t.cliente.s}.</p>
      )}
      <button type="button" className="boton-texto" onClick={empezarACrear}>
        + Crear {t.cliente.s} nuev{t.cliente.o}
      </button>
    </div>
  )
}