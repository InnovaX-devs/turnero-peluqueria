import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { solicitarRecuperacion } from '../../api/auth'
import { mensajeDeError } from '../../lib/errores'
import './auth.css'

/** "Olvidé mi contraseña": pide el email y manda el link para restablecerla. */
export function RecuperarPage() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [confirmacion, setConfirmacion] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    // Se lee del formulario (y no del estado) para que funcione el autocompletado del navegador
    const emailIngresado = String(new FormData(e.currentTarget).get('email') ?? '').trim()
    if (!emailIngresado) {
      setError('Ingresá tu email.')
      return
    }

    setError(null)
    setEnviando(true)
    try {
      setConfirmacion(await solicitarRecuperacion(emailIngresado))
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setEnviando(false)
    }
  }

  if (confirmacion) {
    return (
      <main className="auth-pagina">
        <section className="auth-tarjeta">
          <h1 className="auth-titulo">Revisá tu email</h1>
          <p className="auth-subtitulo">{confirmacion}</p>
          <p className="auth-nota">
            El link vence en 1 hora. Si no te llega en unos minutos, revisá la carpeta de spam o
            volvé a pedirlo.
          </p>
          <button type="button" className="boton-principal" onClick={() => setConfirmacion(null)}>
            Pedir otro link
          </button>
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
        <h1 className="auth-titulo">Olvidé mi contraseña</h1>
        <p className="auth-subtitulo">
          Ingresá el email con el que entrás al panel y te mandamos un link para elegir una
          contraseña nueva.
        </p>

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

          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}

          <button type="submit" className="boton-principal" disabled={enviando}>
            {enviando ? 'Enviando…' : 'Enviar link'}
          </button>
        </form>

        <Link to="/panel/login" className="auth-link">
          Volver al login
        </Link>
      </section>
    </main>
  )
}