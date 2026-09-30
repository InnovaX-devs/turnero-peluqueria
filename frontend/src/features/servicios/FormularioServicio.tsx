import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { actualizarServicio, crearServicio } from '../../api/servicios'
import { erroresDeCampos, mensajeDeError } from '../../lib/errores'
import type { Servicio } from '../../types/servicio'
import { centavosAPesosTexto, pesosACentavos, useInvalidarServicios } from './servicios'

interface Props {
  /** Sin servicio = alta. */
  servicio?: Servicio
  onGuardado: () => void
  onCancelar: () => void
}

export function FormularioServicio({ servicio, onGuardado, onCancelar }: Props) {
  const invalidar = useInvalidarServicios()
  const [nombre, setNombre] = useState(servicio?.nombre ?? '')
  const [duracion, setDuracion] = useState(servicio ? String(servicio.duracionMin) : '')
  const [precio, setPrecio] = useState(servicio ? centavosAPesosTexto(servicio.precio) : '')
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)

  const guardar = useMutation({
    mutationFn: (datos: { nombre: string; duracionMin: number; precio: number }) =>
      servicio ? actualizarServicio(servicio.id, datos) : crearServicio(datos),
    onSuccess: () => {
      invalidar()
      onGuardado()
    },
    onError: (err) => {
      const deCampos = erroresDeCampos(err)
      setErrores(deCampos)
      setErrorGeneral(Object.keys(deCampos).length === 0 ? mensajeDeError(err) : null)
    },
  })

  function limpiarErrores() {
    setErrores({})
    setErrorGeneral(null)
  }

  function handleSubmit(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault()
    const e: Record<string, string> = {}
    const minutos = Number(duracion)
    const centavos = pesosACentavos(precio)

    if (nombre.trim() === '') e.nombre = 'El nombre es obligatorio'
    if (duracion.trim() === '' || !Number.isInteger(minutos) || minutos < 1 || minutos > 480) {
      e.duracionMin = 'Ingresá una duración entera entre 1 y 480 minutos'
    }
    if (centavos === null) e.precio = 'Ingresá un precio válido (ej. 15000 o 15000,50)'

    if (Object.keys(e).length > 0 || centavos === null) {
      setErrores(e)
      return
    }
    limpiarErrores()
    guardar.mutate({ nombre: nombre.trim(), duracionMin: minutos, precio: centavos })
  }

  const sinCambios =
    !!servicio &&
    nombre.trim() === servicio.nombre &&
    Number(duracion) === servicio.duracionMin &&
    pesosACentavos(precio) === servicio.precio

  return (
    <form className="config-form servicio-form config-grupo" onSubmit={handleSubmit} noValidate>
      <h2>{servicio ? 'Editar servicio' : 'Nuevo servicio'}</h2>

      <label className="config-campo">
        <span className="config-etiqueta">Nombre</span>
        <input
          type="text"
          value={nombre}
          maxLength={120}
          autoFocus
          onChange={(e) => {
            setNombre(e.target.value)
            limpiarErrores()
          }}
          aria-invalid={!!errores.nombre}
        />
        {errores.nombre && <span className="config-campo-error" role="alert">{errores.nombre}</span>}
      </label>

      <div className="config-fila servicio-fila">
        <label className="config-campo">
          <span className="config-etiqueta">Duración (minutos)</span>
          <input
            type="text"
            inputMode="numeric"
            value={duracion}
            onChange={(e) => {
              setDuracion(e.target.value)
              limpiarErrores()
            }}
            aria-invalid={!!errores.duracionMin}
          />
          {errores.duracionMin && <span className="config-campo-error" role="alert">{errores.duracionMin}</span>}
        </label>
        <label className="config-campo">
          <span className="config-etiqueta">Precio (pesos)</span>
          <input
            type="text"
            inputMode="decimal"
            value={precio}
            onChange={(e) => {
              setPrecio(e.target.value)
              limpiarErrores()
            }}
            aria-invalid={!!errores.precio}
          />
          {errores.precio && <span className="config-campo-error" role="alert">{errores.precio}</span>}
        </label>
      </div>

      {servicio && (
        <p className="config-ayuda">
          Cambiar el precio o la duración solo afecta a las atenciones nuevas. Las ya registradas conservan su precio.
        </p>
      )}

      {errorGeneral && <p className="config-error" role="alert">{errorGeneral}</p>}

      <div className="servicio-form-acciones">
        <button type="submit" className="boton-principal config-guardar" disabled={sinCambios || guardar.isPending}>
          {guardar.isPending ? 'Guardando…' : servicio ? 'Guardar cambios' : 'Crear servicio'}
        </button>
        <button type="button" className="boton-secundario" onClick={onCancelar}>
          Cancelar
        </button>
      </div>
    </form>
  )
}