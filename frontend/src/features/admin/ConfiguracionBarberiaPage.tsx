import { useQuery } from '@tanstack/react-query'
import { obtenerBarberia } from '../../api/barberia'
import { mensajeDeError } from '../../lib/errores'
import { FormularioBarberia } from './FormularioBarberia'
import './configuracion.css'

/** Pantalla de configuración de la barbería (solo admin). */
export function ConfiguracionBarberiaPage() {
  const { data, isPending, error, refetch } = useQuery({
    queryKey: ['barberia'],
    queryFn: obtenerBarberia,
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

      {data && <FormularioBarberia barberia={data} />}
    </section>
  )
}