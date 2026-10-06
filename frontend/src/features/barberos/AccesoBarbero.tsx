import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { crearUsuarioBarbero, reenviarInvitacionBarbero } from '../../api/barberos'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { Barbero } from '../../types/barbero'

export function AccesoBarbero({ barbero }: { barbero: Barbero }) {
  const queryClient = useQueryClient()
  const [email, setEmail] = useState('')
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  const crear = useMutation({
    mutationFn: () => crearUsuarioBarbero(barbero.id, { email: email.trim() }),
    onSuccess: (b) => {
      queryClient.setQueryData(['barbero', b.id], b)
      queryClient.invalidateQueries({ queryKey: ['barberos', 'lista'] })
      setAviso(
        b.activo
          ? `Le mandamos la invitación a ${b.usuarioEmail}.`
          : 'Usuario creado. Como el barbero está inactivo no se mandó la invitación: reactivalo y reenviála.',
      )
    },
    onError: (err) => {
      const deCampos = erroresDeCampos(err)
      setErrores(deCampos)
      setErrorGeneral(Object.keys(deCampos).length === 0 ? mensajeDeError(err) : null)
    },
  })

  const reenviar = useMutation({
    mutationFn: () => reenviarInvitacionBarbero(barbero.id),
    onSuccess: () => {
      setErrorGeneral(null)
      setAviso(`Le mandamos un link nuevo a ${barbero.usuarioEmail}. Los anteriores dejan de valer.`)
    },
    onError: (err) => {
      setAviso(null)
      setErrorGeneral(erroresDeCampos(err).usuario ?? mensajeDeError(err))
    },
  })

  if (barbero.usuarioEmail) {
    return (
      <div className="config-form">
        <p>
          Puede entrar al panel con <strong>{barbero.usuarioEmail}</strong>.
          {!barbero.activo && ' Mientras el barbero esté inactivo, no puede iniciar sesión.'}
        </p>
        <p className="config-ayuda">
          Si todavía no eligió su contraseña, o la olvidó, podés mandarle un link nuevo. El link vence en 3 días.
        </p>
        {aviso && <p className="config-ok" role="status">{aviso}</p>}
        {errorGeneral && <p className="config-error" role="alert">{errorGeneral}</p>}
        <div>
          <button
            type="button"
            className="boton-secundario"
            disabled={!barbero.activo || reenviar.isPending}
            onClick={() => {
              setAviso(null)
              setErrorGeneral(null)
              reenviar.mutate()
            }}
          >
            {reenviar.isPending ? 'Enviando…' : 'Reenviar invitación'}
          </button>
        </div>
      </div>
    )
  }

  function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const locales: Record<string, string> = {}
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) locales.email = 'Ingresá un email válido'
    setErrores(locales)
    setErrorGeneral(null)
    if (Object.keys(locales).length === 0) crear.mutate()
  }

  return (
    <form className="config-form" onSubmit={handleSubmit} noValidate>
      <p className="config-ayuda">
        Este barbero todavía no tiene usuario para entrar al panel. Al crearlo le llega un email para que elija su
        contraseña.
      </p>
      <div className="config-fila">
        <label className="config-campo">
          <span className="config-etiqueta">Email</span>
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
      </div>
      {errorGeneral && <p className="config-error" role="alert">{errorGeneral}</p>}
      <button type="submit" className="boton-principal config-guardar" disabled={crear.isPending}>
        {crear.isPending ? 'Creando…' : 'Crear usuario y enviar invitación'}
      </button>
    </form>
  )
}