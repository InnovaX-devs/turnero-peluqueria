# Checklist de datos sensibles — issue #73

Criterio del issue: **este checklist lo revisa el equipo antes del primer cliente de salud.**

Estado: ✅ resuelto en código · 🔎 hay que verificarlo a mano (fuera del código) · ⚠️ decisión o texto pendiente del equipo

## Tareas del issue

| # | Tarea | Estado | Dónde / qué hacer |
|---|---|---|---|
| 1 | DNI, cobertura y notas no salen en logs | ✅ | Ningún `log.*` imprime esos datos. `LogEmailSender` ya no vuelca el cuerpo del email (solo destinatario enmascarado y asunto; el cuerpo solo con `app.mail.log-contenido=true`, activo únicamente en el perfil `local`). `GlobalExceptionHandler` captura `DataIntegrityViolationException` y loguea solo clase y SQLSTATE: el mensaje de Postgres trae el DNI. `application.yml` deja explícito que Hibernate no loguea SQL ni parámetros. |
| 2 | Auditoría de acceso a la ficha | ✅ | Tabla `acceso_paciente` (V18). Se registra abrir la ficha, editar notas, editar cobertura, exportar y anonimizar. El admin ve el registro en la ficha ("Ver quién vio esta ficha"). |
| 3 | Términos y política de privacidad | ⚠️ | Borrador de términos negocio–Innovax en [`privacidad-datos-salud.md`](privacidad-datos-salud.md). Política para el paciente en `/b/:slug/privacidad`. **Los dos textos los tiene que revisar un abogado.** |
| 4 | Checkbox de consentimiento en la reserva pública | ✅ | Solo rubro salud. El backend lo exige (400 si falta) y guarda fecha y versión en `cliente` (V19). Los pacientes cargados a mano desde el panel no tienen consentimiento registrado. |
| 5 | Exportar y borrar datos de un paciente | ✅ | Ficha del paciente → "Datos personales" (solo admin). Exportar baja un JSON. Borrar anonimiza (no se puede deshacer) y se bloquea si el paciente tiene turnos reservados. |
| 6 | Backups y cifrado en reposo en Supabase | 🔎 | Ver la sección de abajo. |

## 6. Qué verificar en Supabase (a mano)

No se puede comprobar desde el repositorio. Mirar en el dashboard del proyecto de producción y anotar acá el resultado:

- [ ] **Backups:** qué plan tiene el proyecto y si hay backups automáticos, con qué retención. Probar al menos una vez que se puede restaurar. (La documentación de Supabase indica que los backups diarios dependen del plan y que Point-in-Time Recovery es un agregado: confirmarlo en la documentación vigente, cambia.)
- [ ] **Cifrado en reposo:** confirmar en la documentación y los términos de Supabase que la base y el storage están cifrados en reposo, y dejar el link acá.
- [ ] **Cifrado en tránsito:** la conexión JDBC usa SSL y está activado "Enforce SSL on incoming connections".
- [ ] **API de datos (PostgREST) y RLS:** las tablas del esquema `public` quedan expuestas por la API REST de Supabase con la clave `anon` si no tienen Row Level Security. La aplicación se conecta directo por JDBC, así que no usa esa API: o se **desactiva la Data API**, o se activa `ENABLE ROW LEVEL SECURITY` en todas las tablas (sin políticas bloquea a `anon`; el usuario dueño de las tablas, que es el que usa la app y Flyway, no se ve afectado). Esto es lo más importante de verificar.
- [ ] **Secretos:** `DB_PASSWORD`, `SUPABASE_SERVICE_KEY`, `JWT_SECRET` y `RESEND_API_KEY` solo están en las variables de entorno del hosting, no en el repo, y se rotaron si alguna vez se compartieron.
- [ ] **Accesos al proyecto:** quién tiene acceso al dashboard de Supabase y al hosting, con 2FA activado.

## Riesgos que encontré al revisar y NO están resueltos ⚠️

Quedaron fuera del alcance del issue; conviene decidirlos antes del primer cliente de salud:

1. **La nota de la ficha (`cliente.notas`) la ven todos los roles**, incluida recepción, y viaja en cada turno de la agenda. Las notas de consulta (#72) sí están restringidas. Si en salud la nota de la ficha puede tener datos clínicos, hay que aplicarle la misma regla.
2. **Cualquier profesional puede abrir la ficha de cualquier paciente del negocio**, aunque nunca lo haya atendido. Ahora queda registrado, pero no está impedido.
3. **La reserva pública con DNI actualiza los datos de un paciente existente** (nombre, teléfono, fecha de nacimiento, sexo) con lo que mande quien reserva. Quien conozca un DNI puede cambiar los datos de ese paciente. Hay un test que fija ese comportamiento a propósito (`elMismoDniEsElMismoPacienteAunqueCambieElTelefono`), así que cambiarlo es una decisión de producto.
4. **La auditoría no cubre la agenda ni los listados**: el DNI, el teléfono y la cobertura aparecen ahí sin registrar quién los vio. Se auditó la ficha, que es lo que pide el issue.
5. **No hay límite de intentos en `/api/public/**`** (reserva, disponibilidad). No lo revisé a fondo.
