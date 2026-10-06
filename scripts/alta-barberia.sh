#!/usr/bin/env bash
# Da de alta una barbería nueva: la barbería, su primera sucursal y su usuario admin.
# Uso (desde la raíz del repo):  ./scripts/alta-barberia.sh
#
# Todo lo demás (barberos, servicios, horarios, logo) lo carga el admin desde el panel.
# La barbería arranca con la reserva online apagada; el admin la prende cuando termina.
# La contraseña la elegís vos; el admin la cambia después desde "Mi cuenta".
set -euo pipefail

cd "$(dirname "$0")/../backend"

echo "Alta de una barbería nueva"
echo "--------------------------"
echo "¿En qué base la creo?"
echo "  1) Local (Docker)"
echo "  2) Supabase (perfil dev)"
read -rp "Elegí 1 o 2: " BASE
case "$BASE" in
  1) export SPRING_PROFILES_ACTIVE=local ;;
  2) export SPRING_PROFILES_ACTIVE=dev ;;
  *) echo "Opción inválida"; exit 1 ;;
esac

echo
read -rp "Nombre de la barbería: " ALTA_NOMBRE
read -rp "Slug para el link (Enter para armarlo con el nombre): " ALTA_SLUG
read -rp "Nombre de la sucursal (Enter para usar el de la barbería): " ALTA_SUCURSAL
read -rp "Dirección de la sucursal: " ALTA_DIRECCION
read -rp "Ciudad: " ALTA_CIUDAD
read -rp "Teléfono o WhatsApp de la sucursal (opcional, ej. +5493584123456): " ALTA_TELEFONO
read -rp "Zona horaria (Enter para America/Argentina/Cordoba): " ALTA_ZONA_HORARIA
read -rp "Email del admin (con este entra al panel): " ALTA_EMAIL

# La contraseña no se muestra mientras la escribís
while true; do
  read -rsp "Contraseña del admin (mínimo 8 caracteres): " ALTA_PASSWORD; echo
  if [ "${#ALTA_PASSWORD}" -lt 8 ]; then
    echo "Tiene que tener al menos 8 caracteres."
    continue
  fi
  read -rsp "Repetí la contraseña: " REPETIDA; echo
  if [ "$ALTA_PASSWORD" != "$REPETIDA" ]; then
    echo "No coinciden, probá de nuevo."
    continue
  fi
  break
done
unset REPETIDA

echo
echo "Voy a crear \"$ALTA_NOMBRE\" con el admin $ALTA_EMAIL en la base $( [ "$BASE" = 1 ] && echo local || echo Supabase )."
read -rp "¿Confirmás? (s/n): " OK
[ "$OK" = "s" ] || { echo "Cancelado."; exit 0; }

export ALTA_EJECUTAR=true ALTA_NOMBRE ALTA_SLUG ALTA_SUCURSAL ALTA_DIRECCION ALTA_CIUDAD \
       ALTA_TELEFONO ALTA_ZONA_HORARIA ALTA_EMAIL ALTA_PASSWORD

echo "Arrancando (tarda unos segundos)..."
# Puerto al azar para no chocar con el backend si está levantado; hace el alta y termina
./mvnw -q spring-boot:run -Dspring-boot.run.arguments="--server.port=0"
