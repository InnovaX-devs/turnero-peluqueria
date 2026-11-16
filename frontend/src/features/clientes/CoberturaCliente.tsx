import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { actualizarCoberturaCliente } from '../../api/clientes'
import { listarCoberturas } from '../../api/coberturas'
import { Selector } from '../../components/Selector'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { FichaCliente } from '../../types/cliente'

/**
 * Obra social / prepaga del paciente (issue #67, solo salud): cobertura, número de afiliado y plan.
 * Sin cobertura = particular.
 */
export function CoberturaCliente({ cliente }: { cliente: FichaCliente }) {
  const queryClient = useQueryClient()
  const original = {
    coberturaId: cliente.coberturaId ?? '',
    numeroAfiliado: cliente.numeroAfiliado ?? '',
    plan: cliente.plan ?? '',
  }
  const [form, setForm] = useState(original)
  const [guardado, setGuardado] = useState(false)

  const { data: coberturas } = useQuery({
    queryKey: ['coberturas', 'activas'],
    queryFn: () => listarCoberturas(true),
  })

  const guardar = useMutation({
    mutationFn: () =>
      actualizarCoberturaCliente(cliente.id, {
        coberturaId: form.coberturaId || null,
        numeroAfiliado: form.numeroAfiliado.trim(),
        plan: form.plan.trim(),
      }),
    onSuccess: (actualizada) => {
      queryClient.setQueryData(['clientes', 'ficha', cliente.id], actualizada)
      queryClient.invalidateQueries({ queryKey: ['agenda'] })
      setForm({
        coberturaId: actualizada.coberturaId ?? '',
        numeroAfiliado: actualizada.numeroAfiliado ?? '',
        plan: actualizada.plan ?? '',
      })
      setGuardado(true)
    },
  })

  function cambiar(campo: keyof typeof form, valor: string) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setGuardado(false)
    guardar.reset()
  }

  // Si tiene una cobertura que después se desactivó, igual se muestra como opción
  const opciones = [
    { valor: '', etiqueta: 'Particular (sin obra social)' },
    ...(coberturas ?? []).map((c) => ({ valor: c.id, etiqueta: c.nombre })),
    ...(cliente.coberturaId && !(coberturas ?? []).some((c) => c.id === cliente.coberturaId)
      ? [{ valor: cliente.coberturaId, etiqueta: `${cliente.coberturaNombre ?? 'Obra social'} (desactivada)` }]
      : []),
  ]

  const hayCambios =
    form.coberturaId !== original.coberturaId ||
    form.numeroAfiliado.trim() !== original.numeroAfiliado ||
    form.plan.trim() !== original.plan
  const errores = guardar.isError ? erroresDeCampos(guardar.error) : {}

  return (
    <section className="ficha-seccion">
      <h2>Obra social</h2>
      <div className="ficha-cobertura">
        <div className="ficha-cobertura-campo">
          <span>Obra social o prepaga</span>
          <Selector
            etiqueta="Obra social o prepaga"
            valor={form.coberturaId}
            onChange={(v) => cambiar('coberturaId', v)}
            opciones={opciones}
            invalido={!!errores.coberturaId}
          />
        </div>
        {form.coberturaId && (
          <>
            <label className="ficha-cobertura-campo">
              <span>Nº de afiliado</span>
              <input
                type="text"
                value={form.numeroAfiliado}
                maxLength={40}
                onChange={(e) => cambiar('numeroAfiliado', e.target.value)}
              />
            </label>
            <label className="ficha-cobertura-campo">
              <span>Plan (opcional)</span>
              <input
                type="text"
                value={form.plan}
                maxLength={60}
                placeholder="Ej. 210"
                onChange={(e) => cambiar('plan', e.target.value)}
              />
            </label>
          </>
        )}
      </div>

      {guardar.isError && (
        <p className="clientes-error" role="alert">
          {errores.coberturaId ?? errores.numeroAfiliado ?? errores.plan ?? mensajeDeError(guardar.error)}
        </p>
      )}

      <div className="ficha-notas-pie">
        <span />
        {guardado && !hayCambios && (
          <span className="ficha-guardado" role="status">
            Guardado
          </span>
        )}
        <button
          type="button"
          className="boton-principal ficha-guardar"
          disabled={!hayCambios || guardar.isPending}
          onClick={() => guardar.mutate()}
        >
          {guardar.isPending ? 'Guardando…' : 'Guardar obra social'}
        </button>
      </div>
    </section>
  )
}
