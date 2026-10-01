import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { restablecerPassword } from '../../api/auth'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import './auth.css'

const MINIMO = 8

/** "Nueva contraseña": se llega desde el link del email (/panel/restablecer?token=...). */
export function RestablecerPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''

  const [password, setPassword] = useState('')
  const [repetida, setRepetida] = useState('')
  const [verPassword, setVerPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // El link no sirve más (vencido, usado o inválido): hay que pedir otro
  const [linkInvalido, setLinkInvalido] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [listo, setListo] = useState(false)

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (password.length < MINIMO) {
      setError(`La contraseña tiene que tener al menos ${MINIMO} caracteres.`)
      return
    }
    if (password !== repetida) {
      setError('Las contraseñas no coinciden.')
      return
    }

    setError(null)
    setEnviando(true)
    try {
      await restablecerPassword(token, password)
      setListo(true)
    } catch (err) {
      const campos = erroresDeCampos(err)
      if (campos.token) {
        setLinkInvalido(campos.token)
      } else {
        setError(campos.password ?? mensajeDeError(err))
      }
    } finally {
      setEnviando(false)
    }
  }

  if (listo) {
    return (
      <main className="auth-pagina">
        <section className="auth-tarjeta">
          <h1 className="auth-titulo">Contraseña actualizada</h1>
          <p className="auth-subtitulo">Ya podés ingresar al panel con tu contraseña nueva.</p>
          <Link to="/panel/login" replace className="boton-principal boton-link">
            Ir al login
          </Link>
        </section>
      </main>
    )
  }

  if (!token || linkInvalido) {
    return (
      <main className="auth-pagina">
        <section className="auth-tarjeta">
          <h1 className="auth-titulo">El link no sirve</h1>
          <p className="auth-subtitulo">
            {linkInvalido ?? 'El link está incompleto. Copialo entero desde el email o pedí uno nuevo.'}
          </p>
          <Link to="/panel/recuperar" className="boton-principal boton-link">
            Pedir un link nuevo
          </Link>
          <Link to="/panel/login" className="auth-link">
            Volver al login
          </Link>
        </section>
      </main>
    )
  }

  return (
    <main className="auth-pagina">
      <section className="auth-tarjeta">
        <h1 className="auth-titulo">Nueva contraseña</h1>
        <p className="auth-subtitulo">Elegí una contraseña de al menos {MINIMO} caracteres.</p>

        <form onSubmit={handleSubmit} noValidate>
          <label className="campo">
            <span className="campo-etiqueta">Contraseña nueva</span>
            <div className="campo-password">
              <input
                type={verPassword ? 'text' : 'password'}
                name="password"
                autoComplete="new-password"
                minLength={MINIMO}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoFocus
              />
              <button
                type="button"
                className="boton-texto"
                onClick={() => setVerPassword((v) => !v)}
                aria-label={verPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                {verPassword ? 'Ocultar' : 'Ver'}
              </button>
            </div>
          </label>

          <label className="campo">
            <span className="campo-etiqueta">Repetí la contraseña</span>
            <input
              type={verPassword ? 'text' : 'password'}
              name="repetida"
              autoComplete="new-password"
              value={repetida}
              onChange={(e) => setRepetida(e.target.value)}
            />
          </label>

          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}

          <button type="submit" className="boton-principal" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar contraseña'}
          </button>
        </form>
      </section>
    </main>
  )
}