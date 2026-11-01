#!/usr/bin/env bash
# S10 · Puesta en marcha y verificación de Firebase para DriverApp.
#
#   ./scripts/firebase_s10.sh setup    # crea proyecto, registra la app, escribe
#                                      # local.properties, habilita Auth y despliega reglas
#   ./scripts/firebase_s10.sh verify   # prueba Auth + Firestore + Storage por REST
#
# Requisitos: firebase-tools y google-cloud-cli instalados y con sesión iniciada
#   npm install -g firebase-tools && firebase login
#   yay -S google-cloud-cli && gcloud auth login
set -euo pipefail

PROJECT_ID="${PROJECT_ID:-driverapp-sumaq}"
PACKAGE_NAME="com.driverapp.repartidor"
DISPLAY_NAME="DriverApp SUMAQ"
LOCATION="${LOCATION:-southamerica-east1}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_PROPS="$ROOT/local.properties"

say() { printf '\n\033[1m▸ %s\033[0m\n' "$*"; }
ok()  { printf '  \033[32m✓\033[0m %s\n' "$*"; }
die() { printf '  \033[31m✗\033[0m %s\n' "$*" >&2; exit 1; }

json_get() { # json_get <json> <dotted.path>
  python3 -c "
import json,sys
d=json.load(sys.stdin)
for k in sys.argv[1].split('.'):
    if isinstance(d,list): d=d[int(k)]
    else: d=d.get(k) if isinstance(d,dict) else None
    if d is None: break
print('' if d is None else (d if not isinstance(d,(dict,list)) else json.dumps(d)))" "$2"
}

require_tools() {
  command -v firebase >/dev/null || die "falta firebase-tools →  npm install -g firebase-tools && firebase login"
  command -v gcloud   >/dev/null || die "falta google-cloud-cli →  yay -S google-cloud-cli && gcloud auth login"
  firebase projects:list >/dev/null 2>&1 || die "firebase sin sesión →  firebase login"
  gcloud auth print-access-token >/dev/null 2>&1 || die "gcloud sin sesión →  gcloud auth login"
}

cmd_setup() {
  require_tools

  say "Proyecto $PROJECT_ID"
  if firebase projects:list --json | grep -q "\"projectId\": \"$PROJECT_ID\""; then
    ok "ya existe"
  else
    firebase projects:create "$PROJECT_ID" --display-name "$DISPLAY_NAME" >/dev/null \
      && ok "creado" \
      || die "no se pudo crear (¿la organización bloquea la creación? créalo en la consola y vuelve a correr setup)"
  fi

  say "App Android $PACKAGE_NAME"
  APP_ID="$(firebase apps:list ANDROID --project "$PROJECT_ID" --json \
            | python3 -c "
import json,sys
apps=json.load(sys.stdin).get('result',[])
for a in apps:
    if a.get('packageName')=='$PACKAGE_NAME': print(a['appId']); break")"
  if [ -z "${APP_ID:-}" ]; then
    APP_ID="$(firebase apps:create ANDROID "$DISPLAY_NAME" --package-name "$PACKAGE_NAME" \
              --project "$PROJECT_ID" --json | json_get - result.appId)"
    ok "registrada → $APP_ID"
  else
    ok "ya registrada → $APP_ID"
  fi

  say "Credenciales → local.properties"
  SDK_JSON="$(firebase apps:sdkconfig ANDROID "$APP_ID" --project "$PROJECT_ID" --json)"
  API_KEY="$(printf '%s' "$SDK_JSON" | json_get - result.client.api_key.current_key)"
  APP_ID_CFG="$(printf '%s' "$SDK_JSON" | json_get - result.client.client_info.mobilesdk_app_id)"
  PROJ="$(printf '%s' "$SDK_JSON" | json_get - result.project_info.project_id)"
  BUCKET="$(printf '%s' "$SDK_JSON" | json_get - result.project_info.storage_bucket)"

  touch "$LOCAL_PROPS"
  grep -vE '^(FIREBASE_API_KEY|FIREBASE_APP_ID|FIREBASE_PROJECT_ID|FIREBASE_STORAGE_BUCKET)=' "$LOCAL_PROPS" > "$LOCAL_PROPS.tmp" || true
  {
    echo "FIREBASE_API_KEY=${API_KEY:-}"
    echo "FIREBASE_APP_ID=${APP_ID_CFG:-$APP_ID}"
    echo "FIREBASE_PROJECT_ID=${PROJ:-$PROJECT_ID}"
    echo "FIREBASE_STORAGE_BUCKET=${BUCKET:-}"
  } >> "$LOCAL_PROPS.tmp"
  mv "$LOCAL_PROPS.tmp" "$LOCAL_PROPS"
  ok "escritas (api key ${API_KEY:0:8}…, proyecto ${PROJ:-$PROJECT_ID})"

  say "Auth: Email/Password"
  TOKEN="$(gcloud auth print-access-token)"
  curl -s -o /dev/null -w '  HTTP %{http_code}\n' -X PATCH \
    "https://identitytoolkit.googleapis.com/admin/v2/projects/$PROJECT_ID/config?updateMask=signIn" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d '{"signIn":{"email":{"enabled":true,"passwordRequired":true}}}' \
    | sed 's/^/  habilita email: /'

  say "Firestore"
  if gcloud firestore databases describe --project "$PROJECT_ID" >/dev/null 2>&1; then
    ok "base de datos ya existe"
  else
    gcloud firestore databases create --project "$PROJECT_ID" \
      --location="$LOCATION" --type=firestore-native >/dev/null && ok "creada en $LOCATION" \
      || die "no se pudo crear Firestore"
  fi

  say "Reglas"
  firebase deploy --only firestore:rules --project "$PROJECT_ID" --non-interactive >/dev/null \
    && ok "firestore.rules desplegadas"
  if gcloud storage buckets describe "gs://${PROJECT_ID}.firebasestorage.app" >/dev/null 2>&1; then
    firebase deploy --only storage --project "$PROJECT_ID" --non-interactive >/dev/null \
      && ok "storage.rules desplegadas"
  else
    ok "Storage aún sin bucket → habilítalo en la consola (Storage → Get started) y corre:"
    echo "      ./scripts/firebase_s10.sh setup"
  fi

  say "Listo. Compila y verifica:"
  echo "    ./gradlew :app:assembleDebug"
  echo "    ./scripts/firebase_s10.sh verify"
}

cmd_verify() {
  [ -f "$LOCAL_PROPS" ] || die "no existe local.properties"
  API_KEY="$(grep -E '^FIREBASE_API_KEY=' "$LOCAL_PROPS" | cut -d= -f2- || true)"
  BUCKET="$(grep -E '^FIREBASE_STORAGE_BUCKET=' "$LOCAL_PROPS" | cut -d= -f2- || true)"
  PROJ="$(grep -E '^FIREBASE_PROJECT_ID=' "$LOCAL_PROPS" | cut -d= -f2- || true)"
  [ -n "${API_KEY:-}" ] || die "FIREBASE_API_KEY vacía: corre primero 'setup'"

  EMAIL="probe.$(date +%s)@driverapp.test"
  PASS="Probe-$(date +%s)"

  say "1/3 Auth — crear usuario de prueba ($EMAIL)"
  SIGNUP="$(curl -s -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$API_KEY" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"returnSecureToken\":true}")"
  ID_TOKEN="$(printf '%s' "$SIGNUP" | json_get - idToken)"
  if [ -z "${ID_TOKEN:-}" ]; then
    echo "  respuesta: $(printf '%s' "$SIGNUP" | head -c 300)"
    die "Auth falló (revisa que Email/Password esté habilitado y que el package esté registrado)"
  fi
  ok "auth OK (idToken de ${#ID_TOKEN} caracteres)"

  say "2/3 Firestore — escribir drivers/<uid>"
  UID_LOCAL="$(printf '%s' "$SIGNUP" | json_get - localId)"
  FS_CODE="$(curl -s -o /tmp/s10_fs.json -w '%{http_code}' -X PATCH \
    "https://firestore.googleapis.com/v1/projects/$PROJ/databases/(default)/documents/drivers/$UID_LOCAL" \
    -H "Authorization: Bearer $ID_TOKEN" -H 'Content-Type: application/json' \
    -d '{"fields":{"online":{"booleanValue":true},"lat":{"doubleValue":-12.1219},"lng":{"doubleValue":-77.0297}}}')"
  if [ "$FS_CODE" = "200" ]; then ok "firestore OK"; else echo "  HTTP $FS_CODE: $(head -c 300 /tmp/s10_fs.json)"; die "Firestore rechazó la escritura (¿reglas no desplegadas?)"; fi

  say "3/3 Storage — subir deliveries/$UID_LOCAL.jpg"
  if [ -z "${BUCKET:-}" ]; then
    ok "omitido: Storage no configurado (requiere plan Blaze; fuera de alcance)"
    say "S10 verificado en runtime (Auth + Firestore)"
    echo "  Auth ✅   Firestore ✅   Storage ⛔ fuera de alcance   ($PROJ)"
    exit 0
  fi

  printf 'probe' > /tmp/s10_probe.jpg
  ST_CODE="$(curl -s -o /tmp/s10_st.json -w '%{http_code}' -X POST \
    "https://storage.googleapis.com/upload/storage/v1/b/$BUCKET/o?uploadType=media&name=deliveries/$UID_LOCAL.jpg" \
    -H "Authorization: Bearer $ID_TOKEN" -H 'Content-Type: image/jpeg' \
    --data-binary @/tmp/s10_probe.jpg)"
  if [ "$ST_CODE" = "200" ]; then ok "storage OK"; else echo "  HTTP $ST_CODE: $(head -c 300 /tmp/s10_st.json)"; die "Storage rechazó la subida (¿bucket creado y storage.rules desplegadas?)"; fi

  say "S10 verificado en runtime"
  echo "  Auth ✅   Firestore ✅   Storage ✅   ($PROJ)"
}

case "${1:-}" in
  setup)  cmd_setup ;;
  verify) cmd_verify ;;
  *) echo "uso: $0 {setup|verify}"; exit 1 ;;
esac
