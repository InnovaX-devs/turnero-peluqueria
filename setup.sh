#!/usr/bin/env bash
# Genera la estructura base del monorepo (Spring Boot + React).
# Uso: parado en la raíz del repo clonado ->  bash setup.sh
set -euo pipefail

# ====== Configuración (cambiá esto si querés) ======
GROUP_ID="com.innovax"
ARTIFACT_ID="barberia-backend"
PACKAGE="com.innovax.barberia"
JAVA_VERSION="21"
# ===================================================

PACKAGE_PATH="${PACKAGE//.//}"

# --- Chequeos previos ---
for cmd in curl unzip node npm java; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: falta '$cmd'. Instalalo y volvé a correr el script."
    exit 1
  fi
done

if [ -d backend ] || [ -d frontend ]; then
  echo "ERROR: ya existe 'backend/' o 'frontend/'. Borralas o corré el script en un repo limpio."
  exit 1
fi

keep() { mkdir -p "$1" && touch "$1/.gitkeep"; }

echo "==> Carpetas raíz"
keep docs
keep .github/workflows
keep .github/ISSUE_TEMPLATE

# ================= BACKEND =================
echo "==> Generando backend con Spring Initializr"
curl -fsSL https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d javaVersion="$JAVA_VERSION" \
  -d groupId="$GROUP_ID" \
  -d artifactId="$ARTIFACT_ID" \
  -d name=barberia \
  -d packageName="$PACKAGE" \
  -d dependencies=web,data-jpa,postgresql,flyway,security,validation,lombok,actuator,devtools \
  -o backend.zip
unzip -q backend.zip -d backend
rm backend.zip

JAVA_BASE="backend/src/main/java/$PACKAGE_PATH"

echo "==> Paquetes por feature"
for f in config common; do
  keep "$JAVA_BASE/$f"
done
for f in auth barberia sucursal barbero servicio horario cliente atencion pago notificacion; do
  keep "$JAVA_BASE/$f"
  keep "$JAVA_BASE/$f/dto"
done
keep "$JAVA_BASE/disponibilidad"

echo "==> Configuración de Spring"
RES="backend/src/main/resources"
rm -f "$RES/application.properties"
keep "$RES/db/migration"

cat > "$RES/application.yml" <<'EOF'
spring:
  application:
    name: barberia
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate   # el esquema lo maneja Flyway, nunca Hibernate
  flyway:
    enabled: true

server:
  port: 8080
EOF

cat > "$RES/application-dev.yml" <<'EOF'
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/${DB_NAME:barberia}
    username: ${DB_USER:barberia}
    password: ${DB_PASSWORD:barberia}
  jpa:
    show-sql: true
EOF

cat > "$RES/application-prod.yml" <<'EOF'
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
EOF

# ================= FRONTEND =================
echo "==> Generando frontend con Vite (React + TypeScript)"
echo "    Si te hace alguna pregunta, respondé 'No' a instalar/iniciar ahora."
npm create vite@latest frontend -- --template react-ts

echo "==> Instalando dependencias del frontend"
(
  cd frontend
  npm install
  npm install react-router-dom @tanstack/react-query axios
)

echo "==> Carpetas del frontend"
SRC="frontend/src"
for d in app api components hooks lib types; do
  keep "$SRC/$d"
done
for d in reserva auth agenda atenciones clientes admin caja; do
  keep "$SRC/features/$d"
done

# ================= ARCHIVOS RAÍZ =================
echo "==> Archivos de la raíz"

cat > .gitignore <<'EOF'
# Entorno
.env

# Backend
backend/target/
*.log

# Frontend
node_modules/
frontend/dist/

# IDEs / SO
.idea/
.vscode/
*.iml
.DS_Store
Thumbs.db
EOF

cat > .env.example <<'EOF'
# Copiá este archivo como .env y completá los valores
DB_NAME=barberia
DB_USER=barberia
DB_PASSWORD=barberia
SPRING_PROFILES_ACTIVE=dev
EOF

cat > docker-compose.yml <<'EOF'
services:
  db:
    image: postgres:16
    container_name: barberia-db
    environment:
      POSTGRES_DB: ${DB_NAME:-barberia}
      POSTGRES_USER: ${DB_USER:-barberia}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-barberia}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
EOF

cat > README.md <<'EOF'
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
   ./mvnw spring-boot:run
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
EOF

echo ""
echo "Listo. Revisá los cambios y commiteá:"
echo "  git add ."
echo "  git commit -m \"chore: estructura inicial del monorepo\""
echo "  git push"
