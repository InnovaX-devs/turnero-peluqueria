import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { login, obtenerUsuarioActual } from '../../api/auth'
import { registrarAlNoAutorizado, TOKEN_KEY } from '../../api/client'
import type { Usuario } from '../../types/auth'
import { AuthContext, type AuthContextValue } from './authContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  // Si hay un token guardado, arrancamos "cargando" hasta validarlo con el backend
  const [cargando, setCargando] = useState(() => localStorage.getItem(TOKEN_KEY) !== null)

  const cerrarSesion = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setUsuario(null)
    queryClient.clear() // borra datos cacheados del usuario anterior
  }, [queryClient])

  // Cualquier 401 de la API cierra la sesión
  useEffect(() => {
    registrarAlNoAutorizado(cerrarSesion)
    return () => registrarAlNoAutorizado(null)
  }, [cerrarSesion])

  // Al recargar la página, recupera la sesión con el token guardado
  useEffect(() => {
    if (localStorage.getItem(TOKEN_KEY) === null) return
    obtenerUsuarioActual()
      .then(setUsuario)
      .catch(() => cerrarSesion())
      .finally(() => setCargando(false))
  }, [cerrarSesion])

  const iniciarSesion = useCallback(async (email: string, password: string) => {
    const respuesta = await login(email, password)
    localStorage.setItem(TOKEN_KEY, respuesta.token)
    setUsuario(respuesta.usuario)
  }, [])

  const refrescarUsuario = useCallback(() => {
    obtenerUsuarioActual()
      .then(setUsuario)
      .catch(() => {
        // Si falla no pasa nada: la barra se actualiza la próxima vez que se cargue la página
      })
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ usuario, cargando, iniciarSesion, cerrarSesion, refrescarUsuario }),
    [usuario, cargando, iniciarSesion, cerrarSesion, refrescarUsuario],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
