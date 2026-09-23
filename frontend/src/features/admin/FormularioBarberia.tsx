import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { actualizarBarberia } from '../../api/barberia'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { Barberia } from '../../types/barberia'
import {
  ANTICIPACIONES_MINIMAS,
  INTERVALOS,
  aDatos,
  etiquetaMinutos,
  normalizarSlug,
  sonIguales,
  validar,
  type Formulario,
} from './configuracion'

function aFormulario(b: Barberia): Formulario {
  return {
    nombre: b.nombre,
    slug: b.slug,
    logoUrl: b.logoUrl ?? '',
    reservasOnline: b.reservasOnline,
    anticipacionMinimaMin: b.anticipacionMinimaMin,
    anticipacionMaximaDias: String(b.anticipacionMaximaDias),
    intervaloSlotsMin: b.intervaloSlotsMin,
  }
}

export function FormularioBarberia({ barberia }: { barberia: Barberia }) {
  const queryClient = useQueryClient()
  // El estado del form arranca con lo guardado; después lo maneja el usuario
  const [form, setForm] = useState<Formulario>(() => aFormulario(barberia))
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const [guardado, setGuardado] = useState(false)
  const [logoRoto, setLogoRoto] = useState<string | null>(null)

  const guardar = useMutation({
    mutationFn: actualizarBarberia,
    onSuccess: (actualizada) => {
      queryClient.setQueryData(['barberia'], actualizada)
      setForm(aFormulario(actualizada)) // refleja lo que quedó guardado (ej. nombre sin espacios sobrantes)
      setGuardado(true)
    },
    onError: (err) => {
      const deCampos = erroresDeCampos(err)
      setErrores(deCampos)
      // si el error es de un campo puntual ya se ve debajo del campo; si no, va arriba del botón
      setErrorGeneral(Object.keys(deCampos).length === 0 ? mensajeDeError(err) : null)
    },
  })

  function cambiar<K extends keyof Formulario>(campo: K, valor: Formulario[K]) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setErrores((e) => {
      const copia = { ...e }
      delete copia[campo]
      return copia
    })
    setGuardado(false)
    setErrorGeneral(null)
  }

  const datos = aDatos(form)
  const hayCambios = !sonIguales(datos, barberia)
  const slugCambio = datos.slug !== barberia.slug
  const urlPublica = `${window.location.origin}/b/${datos.slug || 'tu-barberia'}`
  const mostrarLogo = datos.logoUrl !== null && /^https?:\/\/\S+$/.test(datos.logoUrl) && logoRoto !== datos.logoUrl

  function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const erroresLocales = validar(form)
    if (Object.keys(erroresLocales).length > 0) {
      setErrores(erroresLocales)
      return
    }
    setErrores({})
    setErrorGeneral(null)
    setGuardado(false)
    guardar.mutate(datos)
  }

  return (
    <form className="config-form" onSubmit={handleSubmit} noValidate>
      {/* ---------- Datos básicos ---------- */}
      <fieldset className="config-grupo">
        <legend>Datos de la barbería</legend>

        <label className="config-campo">
          <span className="config-etiqueta">Nombre</span>
          <input
            type="text"
            value={form.nombre}
            maxLength={120}
            onChange={(e) => cambiar('nombre', e.target.value)}
            aria-invalid={!!errores.nombre}
          />
          {errores.nombre && <span className="config-campo-error" role="alert">{errores.nombre}</span>}
        </label>

        <label className="config-campo">
          <span className="config-etiqueta">Slug (dirección de tu página)</span>
          <input
            type="text"
            value={form.slug}
            maxLength={80}
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            onChange={(e) => cambiar('slug', normalizarSlug(e.target.value))}
            aria-invalid={!!errores.slug}
          />
          <span className="config-ayuda">Tus clientes reservan en: {urlPublica}</span>
          {slugCambio && (
            <span className="config-aviso">
              Si cambiás el slug, el link anterior (/b/{barberia.slug}) deja de funcionar.
            </span>
          )}
          {errores.slug && <span className="config-campo-error" role="alert">{errores.slug}</span>}
        </label>

        <label className="config-campo">
          <span className="config-etiqueta">Logo (URL de la imagen)</span>
          <input
            type="url"
            inputMode="url"
            value={form.logoUrl}
            maxLength={500}
            placeholder="https://…"
            onChange={(e) => cambiar('logoUrl', e.target.value)}
            aria-invalid={!!errores.logoUrl}
          />
          {errores.logoUrl && <span className="config-campo-error" role="alert">{errores.logoUrl}</span>}
          {mostrarLogo && (
            <img
              className="config-logo"
              src={datos.logoUrl ?? undefined}
              alt="Vista previa del logo"
              onError={() => setLogoRoto(datos.logoUrl)}
            />
          )}
        </label>
      </fieldset>

      {/* ---------- Reservas online ---------- */}
      <fieldset className="config-grupo">
        <legend>Reservas online</legend>

        <label className="interruptor">
          <input
            type="checkbox"
            role="switch"
            checked={form.reservasOnline}
            onChange={(e) => cambiar('reservasOnline', e.target.checked)}
          />
          <span className="interruptor-pista" aria-hidden="true" />
          <span>
            <strong>Permitir reservas online</strong>
            <small>
              Si lo desactivás, los clientes no pueden reservar desde tu página, pero los turnos ya
              reservados se mantienen. Vos y tus barberos siguen pudiendo cargar turnos a mano.
            </small>
          </span>
        </label>

        {form.reservasOnline && (
          <div className="config-fila">
            <label className="config-campo">
              <span className="config-etiqueta">Anticipación mínima</span>
              <select
                value={form.anticipacionMinimaMin}
                onChange={(e) => cambiar('anticipacionMinimaMin', Number(e.target.value))}
                aria-invalid={!!errores.anticipacionMinimaMin}
              >
                {opcionesAnticipacion(form.anticipacionMinimaMin).map((min) => (
                  <option key={min} value={min}>
                    {min === 0 ? 'Sin anticipación' : etiquetaMinutos(min)}
                  </option>
                ))}
              </select>
              <span className="config-ayuda">Con menos tiempo que este no se puede reservar.</span>
              {errores.anticipacionMinimaMin && (
                <span className="config-campo-error" role="alert">{errores.anticipacionMinimaMin}</span>
              )}
            </label>

            <label className="config-campo">
              <span className="config-etiqueta">Anticipación máxima (días)</span>
              <input
                type="number"
                inputMode="numeric"
                min={1}
                max={365}
                value={form.anticipacionMaximaDias}
                onChange={(e) => cambiar('anticipacionMaximaDias', e.target.value)}
                aria-invalid={!!errores.anticipacionMaximaDias}
              />
              <span className="config-ayuda">Hasta cuántos días adelante se puede reservar.</span>
              {errores.anticipacionMaximaDias && (
                <span className="config-campo-error" role="alert">{errores.anticipacionMaximaDias}</span>
              )}
            </label>
          </div>
        )}
      </fieldset>

      {/* ---------- Agenda ---------- */}
      <fieldset className="config-grupo">
        <legend>Agenda</legend>

        <label className="config-campo">
          <span className="config-etiqueta">Intervalo de horarios</span>
          <select
            value={form.intervaloSlotsMin}
            onChange={(e) => cambiar('intervaloSlotsMin', Number(e.target.value))}
            aria-invalid={!!errores.intervaloSlotsMin}
          >
            {INTERVALOS.map((min) => (
              <option key={min} value={min}>
                Cada {min} minutos
              </option>
            ))}
          </select>
          <span className="config-ayuda">Cada cuánto arranca un horario disponible en la agenda.</span>
          {errores.intervaloSlotsMin && (
            <span className="config-campo-error" role="alert">{errores.intervaloSlotsMin}</span>
          )}
        </label>
      </fieldset>

      {errorGeneral && (
        <p className="config-error" role="alert">
          {errorGeneral}
        </p>
      )}
      {guardado && (
        <p className="config-ok" role="status">
          Cambios guardados.
        </p>
      )}

      <button type="submit" className="boton-principal config-guardar" disabled={!hayCambios || guardar.isPending}>
        {guardar.isPending ? 'Guardando…' : 'Guardar cambios'}
      </button>
    </form>
  )
}

/** Las opciones habituales, más el valor actual si es uno personalizado (para no perderlo). */
function opcionesAnticipacion(actual: number): number[] {
  return ANTICIPACIONES_MINIMAS.includes(actual)
    ? ANTICIPACIONES_MINIMAS
    : [...ANTICIPACIONES_MINIMAS, actual].sort((a, b) => a - b)
}