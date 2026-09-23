import axios from 'axios'
import type { ApiError } from '../types/auth'

/** Convierte cualquier error de la API en un mensaje para mostrarle al usuario. */
export function mensajeDeError(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (!error.response) {
      return 'No se pudo conectar con el servidor. Revisá tu conexión.'
    }
    return error.response.data?.mensaje ?? 'Ocurrió un error inesperado.'
  }
  return 'Ocurrió un error inesperado.'
}

/** Errores por campo que devuelve la API (ej. { slug: 'Ese slug ya está en uso' }). Vacío si no hay. */
export function erroresDeCampos(error: unknown): Record<string, string> {
  if (axios.isAxiosError<ApiError>(error)) {
    return error.response?.data?.errores ?? {}
  }
  return {}
}