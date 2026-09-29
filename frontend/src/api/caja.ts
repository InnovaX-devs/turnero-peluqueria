import { api } from './client'
import type { Caja, ConsultaCaja } from '../types/caja'

function paramsDe({ desde, hasta, sucursalId }: ConsultaCaja) {
  return { desde, hasta, ...(sucursalId ? { sucursal: sucursalId } : {}) }
}

export async function obtenerCaja(consulta: ConsultaCaja): Promise<Caja> {
  const { data } = await api.get<Caja>('/api/caja', { params: paramsDe(consulta) })
  return data
}

/** Descarga el detalle en CSV. Va por axios (y no con un link) porque necesita el token. */
export async function descargarCaja(consulta: ConsultaCaja): Promise<void> {
  const { data } = await api.get<Blob>('/api/caja/exportar', { params: paramsDe(consulta), responseType: 'blob' })
  const url = URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = url
  link.download = `caja_${consulta.desde}_${consulta.hasta}.csv`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
