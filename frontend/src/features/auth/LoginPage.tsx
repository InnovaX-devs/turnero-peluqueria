import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { mensajeDeError } from '../../lib/errores'
import { useAuth } from './useAuth'
import './auth.css'

export function LoginPage() {
  const { usuario, iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const desde = (location.state as { desde?: string } | null)?.desde ?? '/panel'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [verPassword, setVerPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  // Si ya está logueado, no tiene sentido mostrar el login
  if (usuario) {
    return <Navigate to={desde} replace />
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    // Se leen del formulario (y no del estado) para que funcione el autocompletado del navegador
    const datos = new FormData(e.currentTarget)
    const emailIngresado = String(datos.get('email') ?? '').trim()
    const passwordIngresada = String(datos.get('password') ?? '')

    if (!emailIngresado || !passwordIngresada) {
      setError('Completá el email y la contraseña.')
      return
    }

    setError(null)
    setEnviando(true)
    try {
      await iniciarSesion(emailIngresado, passwordIngresada)
      navigate(desde, { replace: true })
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="auth-pagina">
      <section className="auth-tarjeta">
        <h1 className="auth-titulo">Ingresá al panel</h1>
        <p className="auth-subtitulo">Accedé con el email y la contraseña de tu barbería.</p>

        <form onSubmit={handleSubmit} noValidate>
          <label className="campo">
            <span className="campo-etiqueta">Email</span>
            <input
              type="email"
              name="email"
              autoComplete="email"
              inputMode="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="tu@email.com"
              autoFocus
            />
          </label>

          <label className="campo">
            <span className="campo-etiqueta">Contraseña</span>
            <div className="campo-password">
              <input
                type={verPassword ? 'text' : 'password'}
                name="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
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

          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}

          <button type="submit" className="boton-principal" disabled={enviando}>
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>

        <Link to="/panel/recuperar" className="auth-link">
          Olvidé mi contraseña
        </Link>
      </section>
    </main>
  )
}