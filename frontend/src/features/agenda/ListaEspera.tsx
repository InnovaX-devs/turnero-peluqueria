import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { obtenerListaEspera, resolverListaEspera } from '../../api/listaEspera'
import { mensajeDeError } from '../../lib/errores'
import type { EntradaListaEspera } from '../../types/listaEspera'
import { linkWhatsApp } from './agenda'

/**
 * Lista de espera del día (issue #71). Solo aparece si hay alguien anotado. Cuando se cancela o se
 * mueve un turno que le sirve a alguien de la lista, esa entrada se resalta con el lugar que se liberó.
 */
export function ListaEspera({ fecha }: { fecha: string }) {
  const { data, error } = useQuery({
    queryKey: ['lista-espera', fecha],
    queryFn: () => obtenerListaEspera(fecha),
    refetchInterval: 60_000,
  })

  if (error) return <p className="agenda-estado">Lista de espera: {mensajeDeError(error)}</p>
  if (!data || data.length === 0) return null

  const conAviso = data.filter((e) => e.avisoEn !== null).length

  return (
    <section className="espera" aria-labelledby="espera-titulo">
      <h2 id="espera-titulo" className="sala-titulo">
        Lista de espera <span className="sala-cantidad">{data.length}</span>
        {conAviso > 0 && (
          <span className="espera-aviso-cantidad">
            {conAviso === 1 ? '1 con lugar liberado' : `${conAviso} con lugar liberado`}
          </span>
        )}
      </h2>
      <ul className="sala-lista">
        {data.map((e) => (
          <Entrada key={e.id} entrada={e} />
        ))}
      </ul>
    </section>
  )
}

function Entrada({ entrada }: { entrada: EntradaListaEspera }) {
  const queryClient = useQueryClient()
  const resolver = useMutation({
    mutationFn: () => resolverListaEspera(entrada.id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['lista-espera'] }),
  })

  return (
    <li className={`espera-item${entrada.avisoEn ? ' con-aviso' : ''}`}>
      <div className="sala-cuerpo">
        <strong>{entrada.clienteNombre}</strong>
        <span className="sala-detalle">
          {entrada.profesionalNombre ?? 'Con cualquiera'}
          {entrada.nota && ` · ${entrada.nota}`}
        </span>
        {entrada.avisoDetalle && <span className="espera-aviso">{entrada.avisoDetalle}</span>}
        {resolver.error && <span className="agenda-modal-error">{mensajeDeError(resolver.error)}</span>}
      </div>
      <div className="espera-acciones">
        <a
          className="boton-secundario"
          href={linkWhatsApp(entrada.clienteTelefono)}
          target="_blank"
          rel="noreferrer"
        >
          WhatsApp
        </a>
        <button
          type="button"
          className="boton-secundario"
          disabled={resolver.isPending}
          onClick={() => resolver.mutate()}
          title="Ya se le dio turno o no hace falta más"
        >
          Resuelto
        </button>
      </div>
    </li>
  )
}
