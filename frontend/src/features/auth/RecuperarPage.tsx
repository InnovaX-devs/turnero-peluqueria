import { Link } from 'react-router-dom'
import './auth.css'

/** Placeholder: lo completa el issue "Recuperar contraseña por email". */
export function RecuperarPage() {
  return (
    <main className="auth-pagina">
      <section className="auth-tarjeta">
        <h1 className="auth-titulo">Recuperar contraseña</h1>
        <p className="auth-subtitulo">
          Todavía no está disponible. Pedile al encargado de la barbería que te cambie la contraseña.
        </p>
        <Link to="/panel/login" className="auth-link">
          Volver al login
        </Link>
      </section>
    </main>
  )
}
