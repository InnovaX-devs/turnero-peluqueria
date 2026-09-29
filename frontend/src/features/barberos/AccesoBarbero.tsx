import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { crearUsuarioBarbero } from '../../api/barberos'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { Barbero } from '../../types/barbero'

export function AccesoBarbero({ barbero }: { barbero: Barbero }) {
  const queryClient = useQueryClient()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)

  const crear = useMutation({
    mutationFn: () => crearUsuarioBarbero(barbero.id, { email: email.trim(), password }),
    onSuccess: (b) => {
      queryClient.setQueryData(['barbero', b.id], b)
      queryClient.invalidateQueries({ queryKey: ['barberos', 'lista'] })
    },
    onError: (err) => {
      const deCampos = erroresDeCampos(err)
      setErrores(deCampos)
      setErrorGeneral(Object.keys(deCampos).length === 0 ? mensajeDeError(err) : null)
    },
  })

  if (barbero.usuarioEmail) {
    return (
      <p>
        Puede entrar al panel con <strong>{barbero.usuarioEmail}</strong>.
        {!barbero.activo && ' Mientras el barbero esté inactivo, no puede iniciar sesión.'}
      </p>
    )
  }

  function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const locales: Record<string, string> = {}
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) locales.email = 'Ingresá un email válido'
    if (password.length < 8 || password.length > 72) {
      locales.password = 'La contraseña tiene que tener entre 8 y 72 caracteres'
    }
    setErrores(locales)
    setErrorGeneral(null)
    if (Object.keys(locales).length === 0) crear.mutate()
  }

  return (
    <form className="config-form" onSubmit={handleSubmit} noValidate>
      <p className="config-ayuda">Este barbero todavía no tiene usuario para entrar al panel.</p>
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
        <label className="config-campo">
          <span className="config-etiqueta">Contraseña (mínimo 8)</span>
          <input
            type="text"
            autoComplete="off"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            aria-invalid={!!errores.password}
          />
          {errores.password && <span className="config-campo-error" role="alert">{errores.password}</span>}
        </label>
      </div>
      {errorGeneral && <p className="config-error" role="alert">{errorGeneral}</p>}
      <button type="submit" className="boton-principal config-guardar" disabled={crear.isPending}>
        {crear.isPending ? 'Creando…' : 'Crear usuario'}
      </button>
    </form>
  )
}