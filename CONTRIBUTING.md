# Cómo trabajamos

## Flujo

1. Tomá un issue y asignátelo en GitHub.
2. Actualizá `main` y creá una rama para ese issue:
   ```bash
   git checkout main
   git pull
   git checkout -b feature/9-disponibilidad-horarios
   ```
3. Trabajá y commiteá en tu rama.
4. Subí la rama y abrí un Pull Request hacia `main`, con `Closes #N` en la descripción.
5. Esperá que el CI pase y que otra persona del equipo lo apruebe.
6. Mergeá y borrá la rama.

`main` está protegida: nadie pushea directo, todo entra por PR.

## Nombres de ramas

Formato: `tipo/numero-de-issue-descripcion-corta`, en minúscula y con guiones.

| Tipo | Para qué | Ejemplo |
|---|---|---|
| `feature/` | Funcionalidad nueva | `feature/9-disponibilidad-horarios` |
| `fix/` | Corregir un bug | `fix/15-doble-reserva` |
| `chore/` | Configuración, dependencias, infra | `chore/1-setup-inicial` |
| `docs/` | Documentación | `docs/2-modelo-de-datos` |
| `refactor/` | Mejorar código sin cambiar comportamiento | `refactor/16-agenda-componentes` |

Una rama = un issue. Si en el medio aparece otra cosa, va en otra rama.

## Mensajes de commit

Usamos [Conventional Commits](https://www.conventionalcommits.org/es/): `tipo: descripción corta en presente`.

```
feat: agrega endpoint de disponibilidad
fix: evita reservas superpuestas del mismo barbero
chore: configura CI del frontend
docs: documenta modelo de datos
refactor: separa lógica de slots en su propio servicio
test: agrega tests de disponibilidad
```

## Base de datos y migraciones

- El esquema se maneja **solo con Flyway**, nunca con `ddl-auto=update`.
- Las migraciones van en `backend/src/main/resources/db/migration/`.
- Nombre: `V{número}__{descripcion}.sql`, por ejemplo `V2__crea_tabla_servicio.sql`.
- **Nunca edites una migración que ya está en `main`.** Si hay que cambiar algo, creá una nueva.
- Si dos personas crean la misma versión (ej. dos `V3`), el que mergea segundo renumera la suya.

## Antes de abrir un PR

- Backend: `cd backend && ./mvnw verify` (necesita la base levantada con `docker compose up -d`)
- Frontend: `cd frontend && npm run lint && npm run build`
