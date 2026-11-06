import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { actualizarProfesional, crearProfesional } from '../../api/profesionales'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { Profesional, OpcionesProfesional } from '../../types/profesional'
import { mismoConjunto } from './profesionales'

interface Form {
  nombre: string
  apellido: string
  sucursalIds: string[]
  servicioIds: string[]
  conUsuario: boolean
  email: string
}

interface Props {
  profesional?: Profesional
  opciones: OpcionesProfesional
  onGuardado?: (b: Profesional) => void
}

function alternar(lista: string[], id: string): string[] {
  return lista.includes(id) ? lista.filter((x) => x !== id) : [...lista, id]
}

export function FormularioProfesional({ profesional, opciones, onGuardado }: Props) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<Form>({
    nombre: profesional?.nombre ?? '',
    apellido: profesional?.apellido ?? '',
    sucursalIds: profesional?.sucursalIds ?? [],
    servicioIds: profesional?.servicioIds ?? [],
    conUsuario: false,
    email: '',
  })
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const [guardado, setGuardado] = useState(false)

  const guardar = useMutation({
    mutationFn: (): Promise<Profesional> => {
      const datos = {
        nombre: form.nombre.trim(),
        apellido: form.apellido.trim(),
        sucursalIds: form.sucursalIds,
        servicioIds: form.servicioIds,
      }
      if (profesional) return actualizarProfesional(profesional.id, datos)
      return crearProfesional({
        ...datos,
        ...(form.conUsuario ? { usuario: { email: form.email.trim() } } : {}),
      })
    },
    onSuccess: (res) => {
      queryClient.setQueryData(['barbero', res.id], res)
      queryClient.invalidateQueries({ queryKey: ['barberos', 'lista'] })
      // Si le sacaron una sucursal, el backend borró sus horarios ahí: hay que volver a pedirlos
      if (profesional && !mismoConjunto(profesional.sucursalIds, res.sucursalIds)) {
        queryClient.removeQueries({ queryKey: ['horarios', res.id] })
      }
      setForm((f) => ({ ...f, nombre: res.nombre, apellido: res.apellido }))
      setGuardado(true)
      onGuardado?.(res)
    },
    onError: (err) => {
      const deCampos = erroresDeCampos(err)
      setErrores(deCampos)
      setErrorGeneral(Object.keys(deCampos).length === 0 ? mensajeDeError(err) : null)
    },
  })

  function cambiar<K extends keyof Form>(campo: K, valor: Form[K]) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setErrores({})
    setErrorGeneral(null)
    setGuardado(false)
  }

  function validar(): Record<string, string> {
    const e: Record<string, string> = {}
    if (form.nombre.trim() === '') e.nombre = 'El nombre es obligatorio'
    if (form.apellido.trim() === '') e.apellido = 'El apellido es obligatorio'
    if (!profesional && form.conUsuario) {
      if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) e.email = 'Ingresá un email válido'
    }
    return e
  }

  function handleSubmit(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault()
    const locales = validar()
    if (Object.keys(locales).length > 0) {
      setErrores(locales)
      return
    }
    setErrores({})
    setErrorGeneral(null)
    setGuardado(false)
    guardar.mutate()
  }

  const sinCambios =
    !!profesional &&
    form.nombre.trim() === profesional.nombre &&
    form.apellido.trim() === profesional.apellido &&
    mismoConjunto(form.sucursalIds, profesional.sucursalIds) &&
    mismoConjunto(form.servicioIds, profesional.servicioIds)

  const errEmail = errores['usuario.email'] ?? errores.email

  return (
    <form className="config-form" onSubmit={handleSubmit} noValidate>
      <div className="config-fila profesional-fila-nombre">
        <label className="config-campo">
          <span className="config-etiqueta">Nombre</span>
          <input
            type="text"
            value={form.nombre}
            maxLength={80}
            onChange={(e) => cambiar('nombre', e.target.value)}
            aria-invalid={!!errores.nombre}
          />
          {errores.nombre && <span className="config-campo-error" role="alert">{errores.nombre}</span>}
        </label>
        <label className="config-campo">
          <span className="config-etiqueta">Apellido</span>
          <input
            type="text"
            value={form.apellido}
            maxLength={80}
            onChange={(e) => cambiar('apellido', e.target.value)}
            aria-invalid={!!errores.apellido}
          />
          {errores.apellido && <span className="config-campo-error" role="alert">{errores.apellido}</span>}
        </label>
      </div>

      <fieldset className="config-grupo">
        <legend>Sucursales donde trabaja</legend>
        {opciones.sucursales.length === 0 && (
          <p className="config-ayuda">No hay sucursales activas para asignar.</p>
        )}
        <div className="checks">
          {opciones.sucursales.map((s) => (
            <label key={s.id} className="check">
              <input
                type="checkbox"
                checked={form.sucursalIds.includes(s.id)}
                onChange={() => cambiar('sucursalIds', alternar(form.sucursalIds, s.id))}
              />
              <span>
                {s.nombre}
                {s.direccion && <small>{s.direccion}</small>}
              </span>
            </label>
          ))}
        </div>
        {errores.sucursalIds && <span className="config-campo-error" role="alert">{errores.sucursalIds}</span>}
      </fieldset>

      <fieldset className="config-grupo">
        <legend>Servicios que realiza</legend>
        {opciones.servicios.length === 0 && (
          <p className="config-ayuda">No hay servicios activos para asignar.</p>
        )}
        <div className="checks">
          {opciones.servicios.map((s) => (
            <label key={s.id} className="check">
              <input
                type="checkbox"
                checked={form.servicioIds.includes(s.id)}
                onChange={() => cambiar('servicioIds', alternar(form.servicioIds, s.id))}
              />
              <span>
                {s.nombre}
                <small>{s.duracionMin} min</small>
              </span>
            </label>
          ))}
        </div>
        {errores.servicioIds && <span className="config-campo-error" role="alert">{errores.servicioIds}</span>}
      </fieldset>

      {!profesional && (
        <fieldset className="config-grupo">
          <legend>Acceso al panel</legend>
          <label className="interruptor">
            <input
              type="checkbox"
              role="switch"
              checked={form.conUsuario}
              onChange={(e) => cambiar('conUsuario', e.target.checked)}
            />
            <span className="interruptor-pista" aria-hidden="true" />
            <span>
              <strong>Crearle un usuario</strong>
              <small>
                Le llega un email para que elija su contraseña y entre a su panel. También se puede hacer después.
              </small>
            </span>
          </label>
          {form.conUsuario && (
            <div className="config-fila">
              <label className="config-campo">
                <span className="config-etiqueta">Email</span>
                <input
                  type="text"
                  inputMode="email"
                  autoCapitalize="none"
                  value={form.email}
                  onChange={(e) => cambiar('email', e.target.value)}
                  aria-invalid={!!errEmail}
                />
                {errEmail && <span className="config-campo-error" role="alert">{errEmail}</span>}
              </label>
            </div>
          )}
        </fieldset>
      )}

      {errorGeneral && <p className="config-error" role="alert">{errorGeneral}</p>}
      {guardado && <p className="config-ok" role="status">Cambios guardados.</p>}

      <button
        type="submit"
        className="boton-principal config-guardar"
        disabled={sinCambios || guardar.isPending}
      >
        {guardar.isPending ? 'Guardando…' : profesional ? 'Guardar cambios' : 'Crear barbero'}
      </button>
    </form>
  )
}