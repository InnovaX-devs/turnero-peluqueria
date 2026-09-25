import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { useParams } from 'react-router-dom'
import { obtenerBarberiaPublica } from '../../api/reservaPublica'
import { mensajeDeError } from '../../lib/errores'
import { EncabezadoBarberia } from './EncabezadoBarberia'
import { FlujoReserva } from './FlujoReserva'
import { SinReservasOnline } from './SinReservasOnline'
import './reserva.css'

const SLUG_VALIDO = /^[a-z0-9]+(-[a-z0-9]+)*$/i
const esNoEncontrado = (e: unknown) => axios.isAxiosError(e) && e.response?.status === 404

export function ReservaPage() {
  const { slug = '' } = useParams()
  const slugValido = SLUG_VALIDO.test(slug)

  const { data, isPending, error, refetch } = useQuery({
    queryKey: ['barberia-publica', slug],
    queryFn: () => obtenerBarberiaPublica(slug),
    enabled: slugValido, // un slug con caracteres inválidos no puede existir: 404 sin ir al backend
    retry: (intentos, err) => !esNoEncontrado(err) && intentos < 1,
    staleTime: 5 * 60 * 1000,
  })

  useEffect(() => {
    if (!data) return
    const anterior = document.title
    document.title = `Reservá en ${data.nombre}`
    return () => {
      document.title = anterior
    }
  }, [data])

  if (!slugValido || esNoEncontrado(error)) {
    return (
      <main className="reserva reserva-no-encontrada">
        <p className="reserva-404">404</p>
        <h1>No encontramos esta barbería</h1>
        <p>Revisá que el link esté bien escrito o pedile a la barbería que te lo vuelva a pasar.</p>
      </main>
    )
  }

  if (isPending) return <div className="pantalla-centrada">Cargando…</div>

  if (error) {
    return (
      <main className="reserva">
        <div className="reserva-error" role="alert">
          <p>{mensajeDeError(error)}</p>
          <button type="button" className="boton-principal" onClick={() => refetch()}>Reintentar</button>
        </div>
      </main>
    )
  }

  return (
    <main className="reserva">
      <EncabezadoBarberia key={data.logoUrl} nombre={data.nombre} logoUrl={data.logoUrl} />
      {data.reservasOnline ? (
        <FlujoReserva key={data.slug} barberia={data} />
      ) : (
        <SinReservasOnline sucursales={data.sucursales} />
      )}
    </main>
  )
}