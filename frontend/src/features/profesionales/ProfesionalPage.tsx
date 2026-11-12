import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { obtenerProfesional, obtenerOpcionesProfesional } from '../../api/profesionales'
import { mensajeDeError } from '../../lib/errores'
import { AccesoProfesional } from './AccesoProfesional'
import { EditorHorarios } from './EditorHorarios'
import { FormularioProfesional } from './FormularioProfesional'
import { FotoProfesional } from './FotoProfesional'
import '../admin/configuracion.css'
import './profesionales.css'
import { useTerminos } from '../terminos/useTerminos'

function useOpciones() {
  return useQuery({ queryKey: ['barberos', 'opciones'], queryFn: obtenerOpcionesProfesional })
}

export function ProfesionalNuevoPage() {
  const t = useTerminos()
  const navigate = useNavigate()
  const opciones = useOpciones()

  return (
    <section className="config profesional-detalle">
      <Link to="/panel/profesionales" className="profesional-volver">← {t.profesional.P}</Link>
      <h1>Nuev{t.profesional.o} {t.profesional.s}</h1>
      {opciones.isPending && <p className="config-estado">Cargando…</p>}
      {opciones.error && <p className="config-error" role="alert">{mensajeDeError(opciones.error)}</p>}
      {opciones.data && (
        <FormularioProfesional
          opciones={opciones.data}
          // Después de crearlo se pasa a su ficha para cargar foto y horarios
          onGuardado={(b) => navigate(`/panel/profesionales/${b.id}`, { replace: true })}
        />
      )}
    </section>
  )
}

export function ProfesionalEditarPage() {
  const t = useTerminos()
  const { id = '' } = useParams()
  const profesional = useQuery({ queryKey: ['barbero', id], queryFn: () => obtenerProfesional(id), enabled: id !== '' })
  const opciones = useOpciones()

  const error = profesional.error ?? opciones.error
  const b = profesional.data
  const o = opciones.data

  return (
    <section className="config profesional-detalle">
      <Link to="/panel/profesionales" className="profesional-volver">← {t.profesional.P}</Link>

      {(profesional.isPending || opciones.isPending) && !error && <p className="config-estado">Cargando…</p>}
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

          <div className="config-grupo profesional-bloque">
            <h2>Foto</h2>
            <FotoProfesional profesional={b} />
          </div>

          <div className="profesional-bloque">
            <h2>Datos y asignaciones</h2>
            <FormularioProfesional key={b.id} profesional={b} opciones={o} />
          </div>

          <div className="config-grupo profesional-bloque">
            <h2>Acceso al panel</h2>
            <AccesoProfesional profesional={b} />
          </div>

          <div className="config-grupo profesional-bloque">
            <h2>Horario semanal</h2>
            <EditorHorarios profesional={b} sucursales={o.sucursales} consultorios={o.consultorios ?? []} />
          </div>
        </>
      )}
    </section>
  )
}