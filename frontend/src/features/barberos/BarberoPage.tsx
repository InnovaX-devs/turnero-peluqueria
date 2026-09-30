import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { obtenerBarbero, obtenerOpcionesBarbero } from '../../api/barberos'
import { mensajeDeError } from '../../lib/errores'
import { AccesoBarbero } from './AccesoBarbero'
import { EditorHorarios } from './EditorHorarios'
import { FormularioBarbero } from './FormularioBarbero'
import { FotoBarbero } from './FotoBarbero'
import '../admin/configuracion.css'
import './barberos.css'

function useOpciones() {
  return useQuery({ queryKey: ['barberos', 'opciones'], queryFn: obtenerOpcionesBarbero })
}

export function BarberoNuevoPage() {
  const navigate = useNavigate()
  const opciones = useOpciones()

  return (
    <section className="config barbero-detalle">
      <Link to="/panel/barberos" className="barbero-volver">← Barberos</Link>
      <h1>Nuevo barbero</h1>
      {opciones.isPending && <p className="config-estado">Cargando…</p>}
      {opciones.error && <p className="config-error" role="alert">{mensajeDeError(opciones.error)}</p>}
      {opciones.data && (
        <FormularioBarbero
          opciones={opciones.data}
          // Después de crearlo se pasa a su ficha para cargar foto y horarios
          onGuardado={(b) => navigate(`/panel/barberos/${b.id}`, { replace: true })}
        />
      )}
    </section>
  )
}

export function BarberoEditarPage() {
  const { id = '' } = useParams()
  const barbero = useQuery({ queryKey: ['barbero', id], queryFn: () => obtenerBarbero(id), enabled: id !== '' })
  const opciones = useOpciones()

  const error = barbero.error ?? opciones.error
  const b = barbero.data
  const o = opciones.data

  return (
    <section className="config barbero-detalle">
      <Link to="/panel/barberos" className="barbero-volver">← Barberos</Link>

      {(barbero.isPending || opciones.isPending) && !error && <p className="config-estado">Cargando…</p>}
      {error && (
        <div className="config-error" role="alert">
          <p>{mensajeDeError(error)}</p>
        </div>
      )}

      {b && o && (
        <>
          <h1>
            {b.nombre} {b.apellido}
            {!b.activo && <span className="badge badge-inactivo">Inactivo</span>}
          </h1>

          <div className="config-grupo barbero-bloque">
            <h2>Foto</h2>
            <FotoBarbero barbero={b} />
          </div>

          <div className="barbero-bloque">
            <h2>Datos y asignaciones</h2>
            <FormularioBarbero key={b.id} barbero={b} opciones={o} />
          </div>

          <div className="config-grupo barbero-bloque">
            <h2>Acceso al panel</h2>
            <AccesoBarbero barbero={b} />
          </div>

          <div className="config-grupo barbero-bloque">
            <h2>Horario semanal</h2>
            <EditorHorarios barbero={b} sucursales={o.sucursales} />
          </div>
        </>
      )}
    </section>
  )
}