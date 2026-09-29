import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { registrarCorte } from '../../api/registroCorte'
import { mensajeDeError } from '../../lib/errores'
import type { ClienteResumen, MetodoPago, OpcionesRegistro } from '../../types/registroCorte'
import { BuscadorCliente } from './BuscadorCliente'
import {
  METODOS_PAGO,
  ahoraEnZona,
  centavosAPesos,
  formatearPrecio,
  guardarPreferencias,
  pesosACentavos,
  valoresIniciales,
} from './registroCorte'
import { useOpcionesRegistro } from './useOpcionesRegistro'
import './registro.css'

interface Props {
  onCerrar: () => void
  onRegistrado: () => void
}

export function RegistrarCorte({ onCerrar, onRegistrado }: Props) {
  const ref = useRef<HTMLDialogElement>(null)
  const { data, isPending, error, refetch } = useOpcionesRegistro()

  useEffect(() => {
    const dialogo = ref.current
    if (dialogo && !dialogo.open) dialogo.showModal()
  }, [])

  return (
    <dialog
      ref={ref}
      className="registro"
      aria-labelledby="registro-titulo"
      onCancel={(e) => {
        e.preventDefault()
        onCerrar()
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onCerrar()
      }}
    >
      <div className="registro-contenido">
        <header className="registro-encabezado">
          <h2 id="registro-titulo">Registrar corte</h2>
          <button type="button" className="boton-texto" onClick={onCerrar}>
            Cerrar
          </button>
        </header>

        {isPending && <p className="registro-estado">Cargando…</p>}
        {error && (
          <div role="alert">
            <p className="registro-error">{mensajeDeError(error)}</p>
            <button type="button" className="boton-secundario" onClick={() => refetch()}>
              Reintentar
            </button>
          </div>
        )}
        {data && data.servicios.length === 0 && (
          <p className="registro-estado">Todavía no hay servicios cargados.</p>
        )}
        {data && data.servicios.length > 0 && <FormularioCorte opciones={data} onRegistrado={onRegistrado} />}
      </div>
    </dialog>
  )
}

interface FormularioProps {
  opciones: OpcionesRegistro
  onRegistrado: () => void
}

function FormularioCorte({ opciones, onRegistrado }: FormularioProps) {
  const queryClient = useQueryClient()
  const [inicial] = useState(() => valoresIniciales(opciones))

  const [servicioId, setServicioId] = useState<string | null>(inicial.servicioId)
  const [precio, setPrecio] = useState(inicial.precio)
  const [metodoPago, setMetodoPago] = useState<MetodoPago>(inicial.metodoPago)
  const [barberoId, setBarberoId] = useState<string | null>(inicial.barberoId)
  const [sucursalId, setSucursalId] = useState<string | null>(null)
  const [cliente, setCliente] = useState<ClienteResumen | null>(null)
  const [notas, setNotas] = useState('')
  const [fechaHora, setFechaHora] = useState(() => ahoraEnZona(opciones.zonaHoraria))
  const [fechaEditada, setFechaEditada] = useState(false)

  const barbero = opciones.barberos.find((b) => b.id === barberoId)
  const sucursalesDelBarbero = opciones.sucursales.filter(
    (s) => barbero !== undefined && barbero.sucursalIds.includes(s.id),
  )
  // Con una sola sucursal no se pregunta
  const sucursalFinal = sucursalesDelBarbero.length === 1 ? sucursalesDelBarbero[0].id : sucursalId
  const precioCentavos = pesosACentavos(precio)

  const listo =
    servicioId !== null &&
    barberoId !== null &&
    precioCentavos !== null &&
    fechaHora !== '' &&
    (sucursalesDelBarbero.length <= 1 || sucursalId !== null)

  const registro = useMutation({
    mutationFn: registrarCorte,
    onSuccess: () => {
      guardarPreferencias({
        servicioId: servicioId ?? undefined,
        metodoPago,
        barberoId: barberoId ?? undefined,
      })
      void queryClient.invalidateQueries({ queryKey: ['agenda'] }) // si la agenda está abierta, se actualiza
      onRegistrado()
    },
  })

  function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (servicioId === null || barberoId === null || precioCentavos === null || fechaHora === '') return
    const [fecha, hora] = fechaHora.split('T')
    registro.mutate({
      clienteId: cliente ? cliente.id : null,
      servicioId,
      barberoId,
      sucursalId: sucursalFinal,
      precioFinal: precioCentavos,
      metodoPago,
      notas: notas.trim() === '' ? null : notas.trim(),
      // Si no la tocó, que la ponga el servidor: "ahora" exacto
      fecha: fechaEditada ? fecha : null,
      hora: fechaEditada ? hora : null,
    })
  }

  return (
    <form className="registro-form" onSubmit={enviar}>
      <fieldset className="registro-grupo">
        <legend>Servicio</legend>
        <div className="registro-opciones">
          {opciones.servicios.map((s) => (
            <button
              key={s.id}
              type="button"
              className="registro-opcion"
              aria-pressed={s.id === servicioId}
              onClick={() => {
                setServicioId(s.id)
                setPrecio(centavosAPesos(s.precio))
              }}
            >
              <strong>{s.nombre}</strong>
              <small>{formatearPrecio(s.precio)}</small>
            </button>
          ))}
        </div>
      </fieldset>

      <label className="registro-grupo">
        <span className="registro-etiqueta">Precio cobrado</span>
        <span className="registro-precio">
          <span className="registro-precio-signo" aria-hidden="true">$</span>
          <input
            className="registro-input"
            inputMode="decimal"
            value={precio}
            onChange={(e) => setPrecio(e.target.value)}
          />
        </span>
      </label>

      <fieldset className="registro-grupo">
        <legend>Método de pago</legend>
        <div className="registro-pagos">
          {METODOS_PAGO.map((m) => (
            <button
              key={m.valor}
              type="button"
              className="registro-opcion registro-opcion-centrada"
              aria-pressed={m.valor === metodoPago}
              onClick={() => setMetodoPago(m.valor)}
            >
              {m.etiqueta}
            </button>
          ))}
        </div>
      </fieldset>

      {opciones.barberos.length > 1 && (
        <fieldset className="registro-grupo">
          <legend>Barbero</legend>
          <div className="registro-opciones">
            {opciones.barberos.map((b) => (
              <button
                key={b.id}
                type="button"
                className="registro-opcion"
                aria-pressed={b.id === barberoId}
                onClick={() => {
                  setBarberoId(b.id)
                  setSucursalId(null)
                }}
              >
                {b.nombre}
              </button>
            ))}
          </div>
        </fieldset>
      )}

      {sucursalesDelBarbero.length > 1 && (
        <fieldset className="registro-grupo">
          <legend>Sucursal</legend>
          <div className="registro-opciones">
            {sucursalesDelBarbero.map((s) => (
              <button
                key={s.id}
                type="button"
                className="registro-opcion"
                aria-pressed={s.id === sucursalId}
                onClick={() => setSucursalId(s.id)}
              >
                {s.nombre}
              </button>
            ))}
          </div>
        </fieldset>
      )}

      <fieldset className="registro-grupo">
        <legend>Cliente (opcional)</legend>
        <BuscadorCliente seleccionado={cliente} onSeleccionar={setCliente} />
      </fieldset>

      <details className="registro-mas">
        <summary>
          {fechaEditada ? 'Fecha: ' + fechaHora.replace('T', ' ') : 'Fecha y hora: ahora'}
          {notas.trim() !== '' ? ' · con notas' : ''}
        </summary>
        <label className="registro-grupo">
          <span className="registro-etiqueta">Fecha y hora</span>
          <input
            className="registro-input"
            type="datetime-local"
            value={fechaHora}
            max={ahoraEnZona(opciones.zonaHoraria)}
            onChange={(e) => {
              setFechaHora(e.target.value)
              setFechaEditada(true)
            }}
          />
        </label>
        <label className="registro-grupo">
          <span className="registro-etiqueta">Notas (opcional)</span>
          <textarea
            className="registro-input"
            value={notas}
            maxLength={2000}
            placeholder="Ej: degradé en 2, arriba tijera"
            onChange={(e) => setNotas(e.target.value)}
          />
        </label>
      </details>

      <div className="registro-pie">
        {registro.error && (
          <p className="registro-error" role="alert">
            {mensajeDeError(registro.error)}
          </p>
        )}
        <button type="submit" className="boton-principal" disabled={!listo || registro.isPending}>
          {registro.isPending ? 'Registrando…' : 'Registrar corte · ' + formatearPrecio(precioCentavos ?? 0)}
        </button>
      </div>
    </form>
  )
}