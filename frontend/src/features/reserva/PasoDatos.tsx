import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { crearReserva, obtenerProfesionalesPublicos, obtenerServiciosPublicos } from '../../api/reservaPublica'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import { Selector } from '../../components/Selector'
import { fechaLarga } from '../../lib/fechas'
import { formatearDuracion, formatearPrecio } from '../../lib/formato'
import { errorDeDni, soloDigitosDni } from '../../lib/dni'
import { armarTelefono, PAISES } from '../../lib/telefono'
import { ETIQUETA_SEXO, type Sexo } from '../../types/cliente'
import type { ReservaCreada, SucursalPublica } from '../../types/reserva'
import { useTerminos } from '../terminos/useTerminos'

interface Props {
  slug: string
  sucursal: SucursalPublica
  servicioId: string
  profesionalId: string
  fecha: string
  hora: string
  onConfirmada: (reserva: ReservaCreada) => void
  onElegirOtroHorario: () => void
}

const CAMPOS_DEL_FORMULARIO = ['clienteNombre', 'clienteTelefono', 'clienteDni', 'clienteFechaNacimiento']

const esHorarioOcupado = (e: unknown) => axios.isAxiosError(e) && e.response?.status === 409

export function PasoDatos({ slug, sucursal, servicioId, profesionalId, fecha, hora, onConfirmada, onElegirOtroHorario }: Props) {
  const t = useTerminos()
  const queryClient = useQueryClient()
  const [nombre, setNombre] = useState('')
  const [codigoPais, setCodigoPais] = useState('AR')
  const [telefono, setTelefono] = useState('')
  const [dni, setDni] = useState('')
  const [fechaNacimiento, setFechaNacimiento] = useState('')
  const [sexo, setSexo] = useState<Sexo | ''>('')
  const [notas, setNotas] = useState('')
  const [errores, setErrores] = useState<{ nombre?: string; telefono?: string; dni?: string; fechaNacimiento?: string }>({})

  const pais = PAISES.find((p) => p.codigo === codigoPais) ?? PAISES[0]

  // Para el resumen (ya están en caché de los pasos anteriores)
  const { data: servicios } = useQuery({
    queryKey: ['servicios-publicos', slug, sucursal.id],
    queryFn: () => obtenerServiciosPublicos(slug, sucursal.id),
    staleTime: 5 * 60 * 1000,
  })
  const servicio = servicios?.find((s) => s.id === servicioId)

  const mutacion = useMutation({
    mutationFn: crearReserva,
    onSuccess: (reserva) => {
      // La disponibilidad cambió: que los próximos turnos se consulten de nuevo
      queryClient.invalidateQueries({ queryKey: ['disponibilidad'] })
      queryClient.invalidateQueries({ queryKey: ['dias-disponibles'] })
      queryClient.invalidateQueries({ queryKey: ['profesionales-publicos'] })
      onConfirmada(reserva)
    },
    onError: (error) => {
      if (esHorarioOcupado(error)) {
        queryClient.invalidateQueries({ queryKey: ['disponibilidad'] })
        queryClient.invalidateQueries({ queryKey: ['dias-disponibles'] })
        return
      }
      const campos = erroresDeCampos(error)
      setErrores({
        nombre: campos.clienteNombre,
        telefono: campos.clienteTelefono,
        dni: campos.clienteDni,
        fechaNacimiento: campos.clienteFechaNacimiento,
      })
    },
  })

  // El profesional puede venir de "cualquiera": se busca por el id asignado
  const { data: profesionales } = useQuery({
    queryKey: ['profesionales-publicos', slug, sucursal.id, servicioId],
    queryFn: () => obtenerProfesionalesPublicos(slug, sucursal.id, servicioId),
    staleTime: 60 * 1000,
  })
  const profesional = profesionales?.find((b) => b.id === profesionalId)

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const nombreLimpio = nombre.trim()
    const tel = armarTelefono(pais, telefono)
    const nuevosErrores = {
      nombre: nombreLimpio.length < 2 ? 'Ingresá tu nombre y apellido.' : undefined,
      telefono: tel.ok ? undefined : tel.error,
      dni: t.pideDni ? errorDeDni(dni) : undefined,
      fechaNacimiento: undefined,
    }
    setErrores(nuevosErrores)
    if (nuevosErrores.nombre || nuevosErrores.dni || !tel.ok) return

    mutacion.mutate({
      sucursalId: sucursal.id,
      servicioId,
      profesionalId,
      fecha,
      hora,
      clienteNombre: nombreLimpio,
      clienteTelefono: tel.e164,
      // El DNI, la fecha de nacimiento y el sexo se piden solo en salud
      ...(t.pideDni && {
        clienteDni: soloDigitosDni(dni),
        clienteFechaNacimiento: fechaNacimiento || undefined,
        clienteSexo: sexo || undefined,
      }),
      notas: notas.trim() || null,
    })
  }

  const ocupado = mutacion.isError && esHorarioOcupado(mutacion.error)
  // Errores que no son de un campo del formulario (ej. la barbería apagó las reservas en el medio)
  const campos = mutacion.isError ? erroresDeCampos(mutacion.error) : {}
  const otros = Object.entries(campos)
    .filter(([campo]) => !CAMPOS_DEL_FORMULARIO.includes(campo))
    .map(([, mensaje]) => mensaje)
  const errorGeneral =
    mutacion.isError && !ocupado && (otros.length > 0 || Object.keys(campos).length === 0)
      ? (otros[0] ?? mensajeDeError(mutacion.error))
      : null

  return (
    <div className="reserva-datos">
      <dl className="reserva-resumen">
        <div>
          <dt>{t.servicio.S}</dt>
          <dd>
            {servicio ? `${servicio.nombre} · ${formatearDuracion(servicio.duracionMin)}` : '…'}
          </dd>
        </div>
        <div>
          <dt>{t.profesional.S}</dt>
          <dd>{profesional ? `${profesional.nombre} ${profesional.apellido}` : '…'}</dd>
        </div>
        <div>
          <dt>Cuándo</dt>
          <dd>{fechaLarga(fecha)} · {hora} hs</dd>
        </div>
        <div>
          <dt>Dónde</dt>
          <dd>{sucursal.nombre} · {sucursal.direccion}, {sucursal.ciudad}</dd>
        </div>
        {servicio && (
          <div className="reserva-resumen-total">
            <dt>Total</dt>
            <dd>{formatearPrecio(servicio.precio)}</dd>
          </div>
        )}
      </dl>

      {ocupado ? (
        <div className="reserva-aviso" role="alert">
          <p><strong>Ese horario se acaba de ocupar.</strong> Alguien lo reservó mientras completabas tus datos.</p>
          <button type="button" className="boton-principal" onClick={onElegirOtroHorario}>
            Elegir otro horario
          </button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          <label className="reserva-campo">
            <span>Nombre y apellido</span>
            <input
              type="text"
              autoComplete="name"
              maxLength={120}
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              aria-invalid={errores.nombre ? true : undefined}
            />
            {errores.nombre && <small className="reserva-campo-error">{errores.nombre}</small>}
          </label>

          <div className="reserva-campo">
            <label htmlFor="reserva-telefono">Teléfono (WhatsApp)</label>
            <div className="reserva-telefono">
              <Selector
                variante="compacto"
                etiqueta="País"
                valor={codigoPais}
                onChange={setCodigoPais}
                opciones={PAISES.map((p) => ({ valor: p.codigo, etiqueta: `${p.codigo} +${p.prefijo}` }))}
              />
              <input
                id="reserva-telefono"
                type="tel"
                inputMode="tel"
                autoComplete="tel-national"
                placeholder={pais.ejemplo}
                value={telefono}
                onChange={(e) => setTelefono(e.target.value)}
                aria-invalid={errores.telefono ? true : undefined}
              />
            </div>
            {errores.telefono ? (
              <small className="reserva-campo-error">{errores.telefono}</small>
            ) : (
              pais.codigo === 'AR' && <small>Código de área sin el 0 y número sin el 15.</small>
            )}
          </div>

          {t.pideDni && (
            <>
              <label className="reserva-campo">
                <span>DNI</span>
                <input
                  type="text"
                  inputMode="numeric"
                  autoComplete="off"
                  maxLength={10}
                  placeholder="Sin puntos, ej: 30123456"
                  value={dni}
                  onChange={(e) => setDni(e.target.value)}
                  aria-invalid={errores.dni ? true : undefined}
                />
                {errores.dni ? (
                  <small className="reserva-campo-error">{errores.dni}</small>
                ) : (
                  <small>Con el DNI te identificamos: si compartís teléfono con un familiar, cada uno pone el suyo.</small>
                )}
              </label>

              <label className="reserva-campo">
                <span>Fecha de nacimiento (opcional)</span>
                <input
                  type="date"
                  autoComplete="bday"
                  max={new Date().toISOString().slice(0, 10)}
                  value={fechaNacimiento}
                  onChange={(e) => setFechaNacimiento(e.target.value)}
                  aria-invalid={errores.fechaNacimiento ? true : undefined}
                />
                {errores.fechaNacimiento && <small className="reserva-campo-error">{errores.fechaNacimiento}</small>}
              </label>

              <label className="reserva-campo">
                <span>Sexo (opcional)</span>
                <select value={sexo} onChange={(e) => setSexo(e.target.value as Sexo | '')}>
                  <option value="">Prefiero no decirlo</option>
                  {(Object.keys(ETIQUETA_SEXO) as Sexo[]).map((s) => (
                    <option key={s} value={s}>{ETIQUETA_SEXO[s]}</option>
                  ))}
                </select>
              </label>
            </>
          )}

          <label className="reserva-campo">
            <span>Notas para {t.profesional.el} (opcional)</span>
            <textarea
              rows={2}
              maxLength={500}
              value={notas}
              onChange={(e) => setNotas(e.target.value)}
              placeholder={t.ejemploNota}
            />
          </label>

          <p className="reserva-pago">💵 El pago se hace en {t.negocio.el}.</p>

          {errorGeneral && (
            <p className="reserva-campo-error" role="alert">{errorGeneral}</p>
          )}

          <button type="submit" className="boton-principal" disabled={mutacion.isPending}>
            {mutacion.isPending ? 'Reservando…' : 'Confirmar turno'}
          </button>

          <p className="reserva-legal">
            Al confirmar, aceptás que {t.negocio.el} use tus datos para gestionar este turno.
          </p>
        </form>
      )}
    </div>
  )
}