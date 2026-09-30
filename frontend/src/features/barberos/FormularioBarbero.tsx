import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { actualizarBarbero, crearBarbero } from '../../api/barberos'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { Barbero, OpcionesBarbero } from '../../types/barbero'
import { mismoConjunto } from './barberos'

interface Form {
  nombre: string
  apellido: string
  sucursalIds: string[]
  servicioIds: string[]
  conUsuario: boolean
  email: string
  password: string
}

interface Props {
  barbero?: Barbero
  opciones: OpcionesBarbero
  onGuardado?: (b: Barbero) => void
}

function alternar(lista: string[], id: string): string[] {
  return lista.includes(id) ? lista.filter((x) => x !== id) : [...lista, id]
}

export function FormularioBarbero({ barbero, opciones, onGuardado }: Props) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<Form>({
    nombre: barbero?.nombre ?? '',
    apellido: barbero?.apellido ?? '',
    sucursalIds: barbero?.sucursalIds ?? [],
    servicioIds: barbero?.servicioIds ?? [],
    conUsuario: false,
    email: '',
    password: '',
  })
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const [guardado, setGuardado] = useState(false)

  const guardar = useMutation({
    mutationFn: (): Promise<Barbero> => {
      const datos = {
        nombre: form.nombre.trim(),
        apellido: form.apellido.trim(),
        sucursalIds: form.sucursalIds,
        servicioIds: form.servicioIds,
      }
      if (barbero) return actualizarBarbero(barbero.id, datos)
      return crearBarbero({
        ...datos,
        ...(form.conUsuario ? { usuario: { email: form.email.trim(), password: form.password } } : {}),
      })
    },
    onSuccess: (res) => {
      queryClient.setQueryData(['barbero', res.id], res)
      queryClient.invalidateQueries({ queryKey: ['barberos', 'lista'] })
      // Si le sacaron una sucursal, el backend borró sus horarios ahí: hay que volver a pedirlos
      if (barbero && !mismoConjunto(barbero.sucursalIds, res.sucursalIds)) {
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
    if (!barbero && form.conUsuario) {
      if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) e.email = 'Ingresá un email válido'
      if (form.password.length < 8 || form.password.length > 72) {
        e.password = 'La contraseña tiene que tener entre 8 y 72 caracteres'
      }
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
    !!barbero &&
    form.nombre.trim() === barbero.nombre &&
    form.apellido.trim() === barbero.apellido &&
    mismoConjunto(form.sucursalIds, barbero.sucursalIds) &&
    mismoConjunto(form.servicioIds, barbero.servicioIds)

  const errEmail = errores['usuario.email'] ?? errores.email
  const errPassword = errores['usuario.password'] ?? errores.password

  return (
    <form className="config-form" onSubmit={handleSubmit} noValidate>
      <div className="config-fila barbero-fila-nombre">
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

      {!barbero && (
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
              <small>Con esto el barbero puede entrar a su panel. También se puede hacer después.</small>
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
              <label className="config-campo">
                <span className="config-etiqueta">Contraseña (mínimo 8)</span>
                <input
                  type="text"
                  autoComplete="off"
                  value={form.password}
                  onChange={(e) => cambiar('password', e.target.value)}
                  aria-invalid={!!errPassword}
                />
                {errPassword && <span className="config-campo-error" role="alert">{errPassword}</span>}
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
        {guardar.isPending ? 'Guardando…' : barbero ? 'Guardar cambios' : 'Crear barbero'}
      </button>
    </form>
  )
}