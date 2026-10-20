import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { actualizarBarberia } from '../../api/barberia'
import { LogoBarberia } from './LogoBarberia'
import { Selector } from '../../components/Selector'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { Barberia } from '../../types/barberia'
import {
  ANTICIPACIONES_CANCELACION,
  ANTICIPACIONES_MINIMAS,
  HORAS_RECORDATORIO,
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
    anticipacionCancelacionMin: b.anticipacionCancelacionMin,
    recordatorioHorasAntes: b.recordatorioHorasAntes,
  }
}

export function FormularioBarberia({ barberia }: { barberia: Barberia }) {
  const queryClient = useQueryClient()
  // El estado del form arranca con lo guardado; después lo maneja el usuario
  const [form, setForm] = useState<Formulario>(() => aFormulario(barberia))
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const [guardado, setGuardado] = useState(false)

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

  // El logo se sube/quita al instante (como la foto del barbero). Se sincroniza el form para que
  // "Guardar cambios" no pise el logo nuevo con el valor viejo.
  function alCambiarLogo(actualizada: Barberia) {
    queryClient.setQueryData(['barberia'], actualizada)
    setForm((f) => ({ ...f, logoUrl: actualizada.logoUrl ?? '' }))
  }

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

        <div className="config-campo">
          <span className="config-etiqueta">Logo</span>
          <LogoBarberia logoUrl={barberia.logoUrl} onCambio={alCambiarLogo} />
        </div>
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
              <Selector
                etiqueta="Anticipación mínima"
                valor={String(form.anticipacionMinimaMin)}
                onChange={(v) => cambiar('anticipacionMinimaMin', Number(v))}
                invalido={!!errores.anticipacionMinimaMin}
                opciones={opcionesAnticipacion(form.anticipacionMinimaMin).map((min) => ({
                  valor: String(min),
                  etiqueta: min === 0 ? 'Sin anticipación' : etiquetaMinutos(min),
                }))}
              />
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

        {form.reservasOnline && (
          <label className="config-campo">
            <span className="config-etiqueta">Cancelar o cambiar el turno</span>
            <Selector
              etiqueta="Cancelar o cambiar el turno"
              valor={String(form.anticipacionCancelacionMin)}
              onChange={(v) => cambiar('anticipacionCancelacionMin', Number(v))}
              invalido={!!errores.anticipacionCancelacionMin}
              opciones={opcionesAnticipacion(form.anticipacionCancelacionMin, ANTICIPACIONES_CANCELACION).map((min) => ({
                valor: String(min),
                etiqueta: min === 0 ? 'Hasta el horario del turno' : `Hasta ${etiquetaMinutos(min)} antes`,
              }))}
            />
            <span className="config-ayuda">
              Hasta cuándo el cliente puede cancelar o cambiar su turno desde el link que recibe. Después
              tiene que comunicarse con vos.
            </span>
            {errores.anticipacionCancelacionMin && (
              <span className="config-campo-error" role="alert">{errores.anticipacionCancelacionMin}</span>
            )}
          </label>
        )}
      </fieldset>

      {/* ---------- Avisos al cliente ---------- */}
      <fieldset className="config-grupo">
        <legend>Avisos al cliente</legend>

        <label className="config-campo">
          <span className="config-etiqueta">Recordatorio del turno</span>
          <Selector
            etiqueta="Recordatorio del turno"
            valor={String(form.recordatorioHorasAntes)}
            onChange={(v) => cambiar('recordatorioHorasAntes', Number(v))}
            invalido={!!errores.recordatorioHorasAntes}
            opciones={opcionesHoras(form.recordatorioHorasAntes).map((h) => ({
              valor: String(h),
              etiqueta: h === 24 ? '1 día antes' : h % 24 === 0 ? `${h / 24} días antes` : `${h} horas antes`,
            }))}
          />
          <span className="config-ayuda">
            Con cuánta anticipación aparece el recordatorio en Mensajes para enviarle al cliente.
          </span>
          {errores.recordatorioHorasAntes && (
            <span className="config-campo-error" role="alert">{errores.recordatorioHorasAntes}</span>
          )}
        </label>
      </fieldset>

      {/* ---------- Agenda ---------- */}
      <fieldset className="config-grupo">
        <legend>Agenda</legend>

        <label className="config-campo">
          <span className="config-etiqueta">Intervalo de horarios</span>
          <Selector
            etiqueta="Intervalo de horarios"
            valor={String(form.intervaloSlotsMin)}
            onChange={(v) => cambiar('intervaloSlotsMin', Number(v))}
            invalido={!!errores.intervaloSlotsMin}
            opciones={INTERVALOS.map((min) => ({ valor: String(min), etiqueta: `Cada ${min} minutos` }))}
          />
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

/** Las opciones fijas, más el valor actual si alguien lo guardó con otro número. */
function opcionesAnticipacion(actual: number, opciones: number[] = ANTICIPACIONES_MINIMAS): number[] {
  return opciones.includes(actual) ? opciones : [...opciones, actual].sort((a, b) => a - b)
}

/** Las opciones fijas de recordatorio, más el valor actual si estuviera guardado con otro número. */
function opcionesHoras(actual: number): number[] {
  return HORAS_RECORDATORIO.includes(actual) ? HORAS_RECORDATORIO : [...HORAS_RECORDATORIO, actual].sort((a, b) => a - b)
}