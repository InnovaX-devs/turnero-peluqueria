import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import axios from 'axios'
import { obtenerFichaCliente } from '../../api/clientes'
import { formatearDni } from '../../lib/dni'
import { mensajeDeError } from '../../lib/errores'
import { formatearPrecio } from '../../lib/formato'
import { ETIQUETAS_ESTADO, linkWhatsApp } from '../agenda/agenda'
import { ETIQUETA_SEXO } from '../../types/cliente'
import { NotasCliente } from './NotasCliente'
import './clientes.css'
import { useTerminos } from '../terminos/useTerminos'

/** "1990-05-17" → "17/05/1990" (sin pasar por Date, para que la zona horaria no corra el día). */
function fechaNacimientoLegible(iso: string): string {
  const [anio, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${anio}`
}

function formatearFecha(iso: string): string {
  return new Date(iso).toLocaleString('es-AR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function ClienteFichaPage() {
  const t = useTerminos()
  const { id = '' } = useParams()

  const ficha = useQuery({
    queryKey: ['clientes', 'ficha', id],
    queryFn: () => obtenerFichaCliente(id),
    enabled: id !== '',
    // Un cliente que no existe (404) no se arregla reintentando
    retry: (intentos, error) => !(axios.isAxiosError(error) && error.response?.status === 404) && intentos < 2,
  })

  const volver = (
    <Link to="/panel/clientes" className="ficha-volver">
      ← {t.cliente.P}
    </Link>
  )

  if (ficha.isPending) {
    return (
      <section className="clientes">
        {volver}
        <p className="clientes-estado">Cargando…</p>
      </section>
    )
  }

  if (ficha.isError) {
    const noExiste = axios.isAxiosError(ficha.error) && ficha.error.response?.status === 404
    return (
      <section className="clientes">
        {volver}
        <p className="clientes-estado clientes-error" role="alert">
          {noExiste ? `No encontramos a ese ${t.cliente.s}.` : mensajeDeError(ficha.error)}
        </p>
      </section>
    )
  }

  const cliente = ficha.data

  return (
    <section className="clientes">
      {volver}

      <header className="ficha-encabezado">
        <h1 className="clientes-titulo">{cliente.nombre}</h1>
        <p className="ficha-telefono">{cliente.telefono}</p>
        {cliente.dni && <p className="ficha-dato">DNI {formatearDni(cliente.dni)}</p>}
        {(cliente.fechaNacimiento || cliente.sexo) && (
          <p className="ficha-dato">
            {[
              cliente.fechaNacimiento && `Nació el ${fechaNacimientoLegible(cliente.fechaNacimiento)}`,
              cliente.sexo && ETIQUETA_SEXO[cliente.sexo],
            ]
              .filter(Boolean)
              .join(' · ')}
          </p>
        )}
        <p className="ficha-dato">
          {t.cliente.S} desde{' '}
          {new Date(cliente.creadoEn).toLocaleDateString('es-AR', { month: 'long', year: 'numeric' })}
        </p>
        <a
          className="boton-principal ficha-whatsapp"
          href={linkWhatsApp(cliente.telefono)}
          target="_blank"
          rel="noreferrer"
        >
          Escribir por WhatsApp
        </a>
      </header>

      <div className="ficha-resumen">
        <div>
          <strong>{cliente.historial.filter((a) => a.estado === 'COMPLETADA').length}</strong>
          <span>{t.atencion.P}</span>
        </div>
        <div className={cliente.ausencias > 0 ? 'ficha-resumen-alerta' : undefined}>
          <strong>{cliente.ausencias}</strong>
          <span>{cliente.ausencias === 1 ? 'Ausencia' : 'Ausencias'}</span>
        </div>
      </div>

      <NotasCliente cliente={cliente} />

      <section className="ficha-seccion">
        <h2>Historial</h2>
        {cliente.historial.length === 0 ? (
          <p className="clientes-estado">Todavía no tiene atenciones.</p>
        ) : (
          <ul className="ficha-historial">
            {cliente.historial.map((a) => (
              <li key={a.id}>
                <div className="ficha-historial-principal">
                  <strong>{a.servicioNombre}</strong>
                  <span>{formatearPrecio(a.precioFinal)}</span>
                </div>
                <div className="ficha-historial-detalle">
                  <span>{formatearFecha(a.inicio)}</span>
                  <span>{a.profesionalNombre}</span>
                  <span className={`ficha-estado ficha-estado-${a.estado.toLowerCase()}`}>
                    {ETIQUETAS_ESTADO[a.estado]}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </section>
  )
}