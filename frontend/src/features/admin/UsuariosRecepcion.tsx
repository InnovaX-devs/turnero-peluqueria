import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cambiarActivoRecepcion,
  crearUsuarioRecepcion,
  listarUsuariosRecepcion,
  reenviarInvitacionRecepcion,
} from '../../api/recepcion'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { UsuarioRecepcion } from '../../types/recepcion'

const CLAVE = ['recepcion', 'usuarios']

/**
 * Usuarios de recepción / secretaría (issue #70), dentro de Configuración (solo admin).
 * Recepción da y mueve turnos de todos los barberos y registra clientes y cobros, pero no ve
 * configuración, caja ni reportes.
 */
export function UsuariosRecepcion() {
  const { data, isPending, error } = useQuery({ queryKey: CLAVE, queryFn: listarUsuariosRecepcion })

  return (
    <section className="config-grupo config-recepcion" aria-labelledby="titulo-recepcion">
      <h2 id="titulo-recepcion" className="config-recepcion-titulo">Recepción</h2>
      <p className="config-ayuda">
        Usuarios para la persona que atiende el mostrador o el teléfono: da turnos para todos los barberos y registra
        clientes y cobros. No ve la configuración, la caja ni los reportes.
      </p>

      {isPending && <p className="config-estado">Cargando…</p>}
      {error && <p className="config-error" role="alert">{mensajeDeError(error)}</p>}

      {data && data.length > 0 && (
        <ul className="config-recepcion-lista">
          {data.map((u) => (
            <FilaUsuario key={u.id} usuario={u} />
          ))}
        </ul>
      )}

      <NuevoUsuario />
    </section>
  )
}

function FilaUsuario({ usuario }: { usuario: UsuarioRecepcion }) {
  const queryClient = useQueryClient()
  const [aviso, setAviso] = useState<string | null>(null)
  const [errorFila, setErrorFila] = useState<string | null>(null)

  const reenviar = useMutation({
    mutationFn: () => reenviarInvitacionRecepcion(usuario.id),
    onSuccess: () => {
      setErrorFila(null)
      setAviso(`Le mandamos un link nuevo a ${usuario.email}. Los anteriores dejan de valer.`)
    },
    onError: (err) => {
      setAviso(null)
      setErrorFila(erroresDeCampos(err).usuario ?? mensajeDeError(err))
    },
  })

  const activar = useMutation({
    mutationFn: () => cambiarActivoRecepcion(usuario.id, !usuario.activo),
    onSuccess: () => {
      setAviso(null)
      setErrorFila(null)
      queryClient.invalidateQueries({ queryKey: CLAVE })
    },
    onError: (err) => setErrorFila(mensajeDeError(err)),
  })

  return (
    <li className="config-recepcion-fila">
      <div className="config-recepcion-datos">
        <span className="config-recepcion-email">{usuario.email}</span>
        {!usuario.activo && <span className="config-aviso">Inactivo: no puede iniciar sesión</span>}
      </div>
      <div className="config-recepcion-acciones">
        <button
          type="button"
          className="boton-secundario"
          disabled={!usuario.activo || reenviar.isPending}
          onClick={() => reenviar.mutate()}
        >
          {reenviar.isPending ? 'Enviando…' : 'Reenviar invitación'}
        </button>
        <button type="button" className="boton-secundario" disabled={activar.isPending} onClick={() => activar.mutate()}>
          {usuario.activo ? 'Desactivar' : 'Reactivar'}
        </button>
      </div>
      {aviso && <p className="config-ok" role="status">{aviso}</p>}
      {errorFila && <p className="config-error" role="alert">{errorFila}</p>}
    </li>
  )
}

function NuevoUsuario() {
  const queryClient = useQueryClient()
  const [email, setEmail] = useState('')
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  const crear = useMutation({
    mutationFn: () => crearUsuarioRecepcion(email.trim()),
    onSuccess: (u) => {
      setEmail('')
      setAviso(`Le mandamos la invitación a ${u.email} para que elija su contraseña.`)
      queryClient.invalidateQueries({ queryKey: CLAVE })
    },
    onError: (err) => {
      const deCampos = erroresDeCampos(err)
      setErrores(deCampos)
      setErrorGeneral(Object.keys(deCampos).length === 0 ? mensajeDeError(err) : null)
    },
  })

  function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const locales: Record<string, string> = {}
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) locales.email = 'Ingresá un email válido'
    setErrores(locales)
    setErrorGeneral(null)
    setAviso(null)
    if (Object.keys(locales).length === 0) crear.mutate()
  }

  return (
    <form className="config-recepcion-nuevo" onSubmit={handleSubmit} noValidate>
      <label className="config-campo">
        <span className="config-etiqueta">Email del nuevo usuario</span>
        <input
          type="text"
          inputMode="email"
          autoCapitalize="none"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          aria-invalid={!!errores.email}
        />
        {errores.email && <span className="config-campo-error" role="alert">{errores.email}</span>}
      </label>
      {aviso && <p className="config-ok" role="status">{aviso}</p>}
      {errorGeneral && <p className="config-error" role="alert">{errorGeneral}</p>}
      <button type="submit" className="boton-principal config-guardar" disabled={crear.isPending}>
        {crear.isPending ? 'Creando…' : 'Crear usuario y enviar invitación'}
      </button>
    </form>
  )
}
