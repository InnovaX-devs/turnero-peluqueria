import { useState, type FormEvent } from 'react'
import { cambiarPassword } from '../../api/auth'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import { useAuth } from '../auth/useAuth'
import '../auth/auth.css'
import './cuenta.css'

const MINIMO = 8

/** "Mi cuenta": datos del usuario y cambio de contraseña. La ven admin y barberos. */
export function CuentaPage() {
  const { usuario } = useAuth()
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [repetida, setRepetida] = useState('')
  const [ver, setVer] = useState(false)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [listo, setListo] = useState(false)

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setListo(false)
    setError(null)

    const locales: Record<string, string> = {}
    if (!actual) locales.actual = 'Ingresá tu contraseña actual.'
    if (nueva.length < MINIMO) locales.nueva = `Tiene que tener al menos ${MINIMO} caracteres.`
    else if (nueva !== repetida) locales.repetida = 'Las contraseñas no coinciden.'
    setErrores(locales)
    if (Object.keys(locales).length > 0) return

    setEnviando(true)
    try {
      await cambiarPassword(actual, nueva)
      setActual('')
      setNueva('')
      setRepetida('')
      setListo(true)
    } catch (err) {
      const campos = erroresDeCampos(err)
      if (Object.keys(campos).length > 0) setErrores(campos)
      else setError(mensajeDeError(err))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="cuenta">
      <h1 className="cuenta-titulo">Mi cuenta</h1>

      <dl className="cuenta-datos">
        <div>
          <dt>Email</dt>
          <dd>{usuario?.email}</dd>
        </div>
        <div>
          <dt>Rol</dt>
          <dd>{usuario?.rol === 'ADMIN' ? 'Admin' : 'Barbero'}</dd>
        </div>
      </dl>

      <form className="cuenta-form" onSubmit={handleSubmit} noValidate>
        <h2 className="cuenta-subtitulo">Cambiar contraseña</h2>

        <label className="campo">
          <span className="campo-etiqueta">Contraseña actual</span>
          <div className="campo-password">
            <input
              type={ver ? 'text' : 'password'}
              name="actual"
              autoComplete="current-password"
              value={actual}
              onChange={(e) => setActual(e.target.value)}
              aria-invalid={!!errores.actual}
            />
            <button
              type="button"
              className="boton-texto"
              onClick={() => setVer((v) => !v)}
              aria-label={ver ? 'Ocultar contraseñas' : 'Mostrar contraseñas'}
            >
              {ver ? 'Ocultar' : 'Ver'}
            </button>
          </div>
          {errores.actual && <span className="cuenta-error" role="alert">{errores.actual}</span>}
        </label>

        <label className="campo">
          <span className="campo-etiqueta">Contraseña nueva</span>
          <input
            type={ver ? 'text' : 'password'}
            name="nueva"
            autoComplete="new-password"
            value={nueva}
            onChange={(e) => setNueva(e.target.value)}
            aria-invalid={!!errores.nueva}
          />
          {errores.nueva && <span className="cuenta-error" role="alert">{errores.nueva}</span>}
        </label>

        <label className="campo">
          <span className="campo-etiqueta">Repetí la contraseña nueva</span>
          <input
            type={ver ? 'text' : 'password'}
            name="repetida"
            autoComplete="new-password"
            value={repetida}
            onChange={(e) => setRepetida(e.target.value)}
            aria-invalid={!!errores.repetida}
          />
          {errores.repetida && <span className="cuenta-error" role="alert">{errores.repetida}</span>}
        </label>

        {error && <p className="auth-error" role="alert">{error}</p>}
        {listo && (
          <p className="cuenta-listo" role="status">
            Listo, cambiaste tu contraseña. La próxima vez entrá con la nueva.
          </p>
        )}

        <button type="submit" className="boton-principal" disabled={enviando}>
          {enviando ? 'Guardando…' : 'Cambiar contraseña'}
        </button>
      </form>
    </section>
  )
}
