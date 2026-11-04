import { useQuery } from '@tanstack/react-query'
import { obtenerNegocio } from '../../api/negocio'
import { mensajeDeError } from '../../lib/errores'
import { FormularioNegocio } from './FormularioNegocio'
import './configuracion.css'

/** Pantalla de configuración de la barbería (solo admin). */
export function ConfiguracionNegocioPage() {
  const { data, isPending, error, refetch } = useQuery({
    queryKey: ['barberia'],
    queryFn: obtenerNegocio,
  })

  return (
    <section className="config">
      <h1>Configuración de la barbería</h1>

      {isPending && <p className="config-estado">Cargando…</p>}

      {error && (
        <div className="config-error" role="alert">
          <p>{mensajeDeError(error)}</p>
          <button type="button" className="boton-secundario" onClick={() => refetch()}>
            Reintentar
          </button>
        </div>
      )}

      {data && <FormularioNegocio negocio={data} />}
    </section>
  )
}