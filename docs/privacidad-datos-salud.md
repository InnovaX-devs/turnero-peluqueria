# Términos y política de privacidad para negocios de salud (borrador)

> **BORRADOR — no es asesoramiento legal.** Revisarlo con un abogado antes del primer cliente de salud
> (issue #73). Los puntos marcados con ⚠️ son los que más conviene que mire.

Este documento define cómo se reparten las responsabilidades sobre los datos de pacientes entre el negocio que
usa el turnero y Innovax. Aplica a los negocios con rubro **Salud** (consultorios, clínicas). La política que ve el
paciente al reservar está en el frontend (`/b/:slug/privacidad`, `PrivacidadSaludPage.tsx`).

## 1. Roles (Ley 25.326)

| Parte | Rol | Qué significa |
|---|---|---|
| **El negocio** (consultorio, clínica) | Responsable de la base de datos | Decide para qué se usan los datos de sus pacientes y responde ante ellos y ante la autoridad. |
| **Innovax** | Encargado del tratamiento | Guarda y procesa los datos **solo por cuenta y orden del negocio**, para prestar el servicio de turnos. No los usa para fines propios. |

⚠️ La Ley 25.326 trata como **datos sensibles** los que revelan información de salud (art. 2 y 7): solo se pueden
tratar con consentimiento expreso o por razones de interés general autorizadas por ley.

## 2. Obligaciones del negocio

- Informar a sus pacientes para qué se usan sus datos y obtener su consentimiento (el turnero lo registra al reservar online: fecha y versión de los términos).
- Atender los pedidos de acceso, rectificación y supresión de sus pacientes. El panel permite **exportar** los datos de un paciente y **borrar sus datos personales** (solo el admin del negocio).
- ⚠️ Inscribir su base de datos ante la Agencia de Acceso a la Información Pública (AAIP) si la normativa vigente lo exige. Confirmar con el abogado quién y cuándo.
- Cargar solo los datos necesarios. El turnero **no es una historia clínica**: las notas de consulta son un apoyo operativo. Si el negocio debe conservar historia clínica (Ley 26.529), lo hace en su propio sistema.
- Dar acceso al panel solo a personal autorizado y mantener sus contraseñas en secreto.

## 3. Obligaciones de Innovax

- Tratar los datos solo siguiendo las instrucciones del negocio y para prestar el servicio.
- Guardar confidencialidad sobre los datos y limitar el acceso interno a quien lo necesite para dar soporte.
- Mantener medidas de seguridad. Hoy implementadas en el sistema:
  - Aislamiento entre negocios: todas las consultas del panel filtran por el negocio del usuario logueado.
  - Roles: las notas de consulta solo las ven el admin y el profesional que atendió; recepción no.
  - Registro de accesos a la ficha del paciente (usuario, acción, fecha), consultable por el admin.
  - Los logs de la aplicación no incluyen DNI, cobertura ni notas.
  - Contraseñas con hash (BCrypt) y sesiones con token de vencimiento corto.
- Avisar al negocio sin demora indebida si detecta un incidente de seguridad que afecte datos de sus pacientes. ⚠️ Definir plazo.
- Al terminar el servicio, devolver o borrar los datos del negocio cuando éste lo pida. ⚠️ Definir plazo y formato.
- No ceder los datos a terceros, salvo a los proveedores de infraestructura indicados abajo.

## 4. Proveedores de infraestructura (subencargados) ⚠️

Completar y mantener actualizada esta lista con los proveedores reales en producción. Según la configuración del
repositorio:

| Proveedor | Para qué |
|---|---|
| Supabase | Base de datos PostgreSQL y almacenamiento de fotos de profesionales |
| Resend | Envío de emails (recuperación de contraseña e invitaciones) |
| Hosting del backend y del frontend | Ejecutar la aplicación |

⚠️ Si alguno de estos servidores está fuera de Argentina, hay una **transferencia internacional de datos**
(art. 12 de la Ley 25.326): consultar con el abogado qué condiciones hay que cumplir (por ejemplo, cláusulas
contractuales o consentimiento).

## 5. Derechos de los pacientes

El paciente puede pedirle **al negocio** acceso, rectificación, actualización y supresión de sus datos. El negocio
responde usando el panel. Innovax colabora si el negocio lo necesita.

**Supresión = anonimización.** Se borran nombre, teléfono, DNI, fecha de nacimiento, sexo, obra social, afiliado
y todas las notas (ficha, consultas y lista de espera). Los turnos y cobros se conservan sin identificar a la
persona, porque forman parte de la caja y la contabilidad del negocio. ⚠️ Confirmar que esto cumple con las
obligaciones de conservación del negocio.

## 6. Vigencia y cambios

Cuando cambie el texto de la política que ve el paciente, subir `TerminosSalud.VERSION` en el backend: así queda
registrado con qué versión aceptó cada paciente.
