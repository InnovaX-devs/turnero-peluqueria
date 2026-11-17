import { api } from './client'
import type { AccesoPaciente, DatosCoberturaCliente, FichaCliente, OrdenClientes, PaginaClientes } from '../types/cliente'

export const CLIENTES_POR_PAGINA = 50

/** pagina arranca en 0. */
export async function listarClientes(q: string, pagina: number, orden: OrdenClientes): Promise<PaginaClientes> {
  const { data } = await api.get<PaginaClientes>('/api/clientes/listado', {
    params: { q, pagina, orden, tamano: CLIENTES_POR_PAGINA },
  })
  return data
}

export async function obtenerFichaCliente(id: string): Promise<FichaCliente> {
  const { data } = await api.get<FichaCliente>(`/api/clientes/${id}`)
  return data
}

/** Obra social / prepaga del paciente (issue #67). coberturaId null = particular. */
export async function actualizarCoberturaCliente(id: string, datos: DatosCoberturaCliente): Promise<FichaCliente> {
  const { data } = await api.patch<FichaCliente>(`/api/clientes/${id}/cobertura`, datos)
  return data
}

export async function actualizarNotasCliente(id: string, notas: string): Promise<FichaCliente> {
  const { data } = await api.patch<FichaCliente>(`/api/clientes/${id}/notas`, { notas })
  return data
}

/** Issue #73 (solo admin): quién vio o tocó los datos de este paciente, lo más reciente primero. */
export async function obtenerAccesosCliente(id: string): Promise<AccesoPaciente[]> {
  const { data } = await api.get<AccesoPaciente[]>(`/api/clientes/${id}/accesos`)
  return data
}

/** Issue #73 (solo admin): todos los datos del paciente (derecho de acceso). Queda registrado en los accesos. */
export async function exportarDatosCliente(id: string): Promise<unknown> {
  const { data } = await api.get<unknown>(`/api/clientes/${id}/exportar`)
  return data
}

/** Issue #73 (solo admin): borra los datos personales del paciente (derecho de supresión). No se puede deshacer. */
export async function anonimizarCliente(id: string): Promise<void> {
  await api.post(`/api/clientes/${id}/anonimizar`)
}
