import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { obtenerNegocioPublico } from '../../api/reservaPublica'
import './privacidad.css'

/**
 * Política de privacidad para pacientes (issue #73, Ley 25.326). Es el texto que acepta el paciente en el
 * checkbox de la reserva online del rubro salud.
 *
 * IMPORTANTE: es un texto base. Revisalo con un abogado antes del primer cliente de salud y, cada vez que
 * lo cambies, subí TerminosSalud.VERSION en el backend: ahí se guarda con qué versión aceptó cada paciente.
 */
export function PrivacidadSaludPage() {
  const { slug = '' } = useParams()
  const negocio = useQuery({
    queryKey: ['negocio-publico', slug],
    queryFn: () => obtenerNegocioPublico(slug),
    enabled: slug !== '',
    staleTime: 5 * 60 * 1000,
  })

  const nombre = negocio.data?.nombre ?? 'el establecimiento'

  return (
    <main className="privacidad">
      <h1>Política de privacidad</h1>
      <p className="privacidad-sub">Tratamiento de datos personales de pacientes · Ley 25.326</p>

      <h2>Quién es responsable de tus datos</h2>
      <p>
        <strong>{nombre}</strong> es el responsable de la base de datos donde se guardan tus datos personales y
        decide para qué se usan. El sistema de turnos es provisto por Innovax, que trata los datos únicamente por
        cuenta y orden de {nombre} (encargado del tratamiento) y no los usa para fines propios.
      </p>

      <h2>Qué datos se piden</h2>
      <p>
        Nombre y apellido, teléfono, DNI y, de forma opcional, fecha de nacimiento y sexo. Si te atendés con obra
        social o prepaga, también su nombre, tu número de afiliado y tu plan. Podés agregar notas para el
        profesional. Algunos de estos datos son <strong>datos sensibles</strong> porque se relacionan con tu salud:
        solo se tratan con tu consentimiento.
      </p>

      <h2>Para qué se usan</h2>
      <p>
        Para gestionar tus turnos, enviarte la confirmación y los recordatorios, identificarte cuando te atendés y
        facturar la atención a tu obra social o prepaga. No se usan con fines publicitarios.
      </p>

      <h2>Quién puede ver tus datos</h2>
      <p>
        El personal autorizado de {nombre} y el profesional que te atiende. Cada vez que alguien abre tu ficha
        queda registrado quién fue y cuándo. Tus datos no se ceden a terceros, salvo a tu obra social o prepaga
        cuando hace falta para facturar, o cuando lo exige la ley.
      </p>

      <h2>Tus derechos</h2>
      <p>
        Podés pedir en cualquier momento a {nombre} que te muestre los datos que tiene sobre vos, que los corrija
        o actualice, o que los suprima. Para hacerlo, comunicate directamente con {nombre}.
      </p>

      <h2>Cuánto tiempo se guardan</h2>
      <p>
        Hasta que pidas su supresión. Si una ley obliga a {nombre} a conservar cierta información (por ejemplo,
        la historia clínica), la conserva por el plazo que corresponda.
      </p>

      <h2>Autoridad de control</h2>
      <p className="privacidad-legal">
        El titular de los datos personales tiene la facultad de ejercer el derecho de acceso a los mismos en forma
        gratuita a intervalos no inferiores a seis meses, salvo que se acredite un interés legítimo al efecto
        conforme lo establecido en el artículo 14, inciso 3 de la Ley Nº 25.326. La Agencia de Acceso a la
        Información Pública, en su carácter de Órgano de Control de la Ley Nº 25.326, tiene la atribución de atender
        las denuncias y reclamos que interpongan quienes resulten afectados en sus derechos por incumplimiento de
        las normas vigentes en materia de protección de datos personales.
      </p>
    </main>
  )
}
