import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { crearReserva, obtenerBarberosPublicos, obtenerServiciosPublicos } from '../../api/reservaPublica'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import { fechaLarga } from '../../lib/fechas'
import { formatearDuracion, formatearPrecio } from '../../lib/formato'
import { armarTelefono, PAISES } from '../../lib/telefono'
import type { ReservaCreada, SucursalPublica } from '../../types/reserva'

interface Props {
  slug: string
  sucursal: SucursalPublica
  servicioId: string
  barberoId: string
  fecha: string
  hora: string
  onConfirmada: (reserva: ReservaCreada) => void
  onElegirOtroHorario: () => void
}

const esHorarioOcupado = (e: unknown) => axios.isAxiosError(e) && e.response?.status === 409

export function PasoDatos({ slug, sucursal, servicioId, barberoId, fecha, hora, onConfirmada, onElegirOtroHorario }: Props) {
  const queryClient = useQueryClient()
  const [nombre, setNombre] = useState('')
  const [codigoPais, setCodigoPais] = useState('AR')
  const [telefono, setTelefono] = useState('')
  const [notas, setNotas] = useState('')
  const [errores, setErrores] = useState<{ nombre?: string; telefono?: string }>({})

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
      queryClient.invalidateQueries({ queryKey: ['barberos-publicos'] })
      onConfirmada(reserva)
    },
    onError: (error) => {
      if (esHorarioOcupado(error)) {
        queryClient.invalidateQueries({ queryKey: ['disponibilidad'] })
        queryClient.invalidateQueries({ queryKey: ['dias-disponibles'] })
        return
      }
      const campos = erroresDeCampos(error)
      setErrores({ nombre: campos.clienteNombre, telefono: campos.clienteTelefono })
    },
  })

  // El barbero puede venir de "cualquiera": se busca por el id asignado
  const { data: barberos } = useQuery({
    queryKey: ['barberos-publicos', slug, sucursal.id, servicioId],
    queryFn: () => obtenerBarberosPublicos(slug, sucursal.id, servicioId),
    staleTime: 60 * 1000,
  })
  const barbero = barberos?.find((b) => b.id === barberoId)

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const nombreLimpio = nombre.trim()
    const tel = armarTelefono(pais, telefono)
    const nuevosErrores = {
      nombre: nombreLimpio.length < 2 ? 'Ingresá tu nombre y apellido.' : undefined,
      telefono: tel.ok ? undefined : tel.error,
    }
    setErrores(nuevosErrores)
    if (nuevosErrores.nombre || !tel.ok) return

    mutacion.mutate({
      sucursalId: sucursal.id,
      servicioId,
      barberoId,
      fecha,
      hora,
      clienteNombre: nombreLimpio,
      clienteTelefono: tel.e164,
      notas: notas.trim() || null,
    })
  }

  const ocupado = mutacion.isError && esHorarioOcupado(mutacion.error)
  // Errores que no son de un campo del formulario (ej. la barbería apagó las reservas en el medio)
  const campos = mutacion.isError ? erroresDeCampos(mutacion.error) : {}
  const otros = Object.entries(campos)
    .filter(([campo]) => campo !== 'clienteNombre' && campo !== 'clienteTelefono')
    .map(([, mensaje]) => mensaje)
  const errorGeneral =
    mutacion.isError && !ocupado && (otros.length > 0 || Object.keys(campos).length === 0)
      ? (otros[0] ?? mensajeDeError(mutacion.error))
      : null

  return (
    <div className="reserva-datos">
      <dl className="reserva-resumen">
        <div>
          <dt>Servicio</dt>
          <dd>
            {servicio ? `${servicio.nombre} · ${formatearDuracion(servicio.duracionMin)}` : '…'}
          </dd>
        </div>
        <div>
          <dt>Barbero</dt>
          <dd>{barbero ? `${barbero.nombre} ${barbero.apellido}` : '…'}</dd>
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
              <select
                aria-label="País"
                value={codigoPais}
                onChange={(e) => setCodigoPais(e.target.value)}
              >
                {PAISES.map((p) => (
                  <option key={p.codigo} value={p.codigo}>
                    {p.codigo} +{p.prefijo}
                  </option>
                ))}
              </select>
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

          <label className="reserva-campo">
            <span>Notas para el barbero (opcional)</span>
            <textarea
              rows={2}
              maxLength={500}
              value={notas}
              onChange={(e) => setNotas(e.target.value)}
              placeholder="Ej: quiero el mismo corte que la última vez"
            />
          </label>

          <p className="reserva-pago">💵 El pago se hace en la barbería.</p>

          {errorGeneral && (
            <p className="reserva-campo-error" role="alert">{errorGeneral}</p>
          )}

          <button type="submit" className="boton-principal" disabled={mutacion.isPending}>
            {mutacion.isPending ? 'Reservando…' : 'Confirmar turno'}
          </button>

          <p className="reserva-legal">
            Al confirmar, aceptás que la barbería use tu nombre y teléfono para gestionar este turno.
          </p>
        </form>
      )}
    </div>
  )
}
