import { api } from './client'
import type { ClienteResumen, OpcionesRegistro, RegistrarCorteDatos } from '../types/registroCorte'

export async function obtenerOpcionesRegistro(): Promise<OpcionesRegistro> {
  const { data } = await api.get<OpcionesRegistro>('/api/atenciones/opciones')
  return data
}

export async function buscarClientes(q: string): Promise<ClienteResumen[]> {
  const { data } = await api.get<ClienteResumen[]>('/api/clientes', { params: { q } })
  return data
}

export async function crearCliente(nombre: string, telefono: string): Promise<ClienteResumen> {
  const { data } = await api.post<ClienteResumen>('/api/clientes', { nombre, telefono })
  return data
}

export async function registrarCorte(datos: RegistrarCorteDatos): Promise<void> {
  await api.post('/api/atenciones', datos)
}