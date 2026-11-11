import { useState, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../features/auth/AuthProvider'
import { TerminosDelUsuario } from '../features/terminos/TerminosProvider'

export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () => new QueryClient({ defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } } }),
  )

  return (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <TerminosDelUsuario>{children}</TerminosDelUsuario>
        </AuthProvider>
      </QueryClientProvider>
    </BrowserRouter>
  )
}
