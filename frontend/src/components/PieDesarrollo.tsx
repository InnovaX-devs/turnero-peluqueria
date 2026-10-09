import './pie-desarrollo.css'

const INSTAGRAM_INNOVAX = 'https://www.instagram.com/innovax.team/'

/** "© 2026 · Desarrollado por InnovaX". InnovaX lleva al Instagram del estudio. */
export function PieDesarrollo({ className = '' }: { className?: string }) {
  return (
    <footer className={`pie-desarrollo ${className}`.trim()}>
      <p>
        © {new Date().getFullYear()} · Desarrollado por{' '}
        <a href={INSTAGRAM_INNOVAX} target="_blank" rel="noopener noreferrer">
          InnovaX
        </a>
      </p>
    </footer>
  )
}