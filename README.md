# Sistema de gestión para barberías

Monorepo con backend en Spring Boot y frontend en React.

## Requisitos
- Java 21
- Node 20+
- Docker

## Levantar en local

1. Copiar variables de entorno
   ```bash
   cp .env.example .env
   ```
2. Levantar la base de datos
   ```bash
   docker compose up -d
   ```
3. Backend (http://localhost:8080)
   ```bash
   cd backend
   mvn clean spring-boot:run
   ```
4. Frontend (http://localhost:5173)
   ```bash
   cd frontend
   npm run dev
   ```

## Estructura
- `backend/`: API REST, organizada por feature (`barbero/`, `atencion/`, etc.)
- `frontend/`: app React. `/b/:slug` es la reserva pública, `/panel/*` el panel interno
- `docs/`: decisiones técnicas y modelo de datos
