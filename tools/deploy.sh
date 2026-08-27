#!/usr/bin/env bash
#
# Vivace · despliegue a producción (Linux/macOS).
#
#   ./tools/deploy.sh preflight        comprueba TODO sin tocar nada
#   ./tools/deploy.sh backend          primera vez: bucket R2, base D1 y secreto
#   ./tools/deploy.sh release          el despliegue completo del servidor
#   ./tools/deploy.sh catalog          indexa lo que ya estaba en R2 (una vez)
#   ./tools/deploy.sh app <apk>        sube un APK firmado y publica la versión
#   ./tools/deploy.sh verify           comprueba lo que hay publicado
#   ./tools/deploy.sh rollback         vuelve a la versión anterior del Worker
#
# Opciones:  --yes  no preguntar   ·   --url <base>  URL del Worker
#            --skip-tests  (desaconsejado: los tests son la única red)
#
# ORDEN, que aquí importa más que en ningún otro sitio: el esquema se aplica
# ANTES de publicar el código. La API nueva cuenta con columnas que la base
# vieja no tiene (rev, favorite, position, playlist_id), así que desplegar
# primero el Worker deja la web devolviendo errores hasta que llegue el ALTER.
# Por eso `release` hace las dos cosas y en ese orden: no se pueden separar.
#
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKER="$RAIZ/worker"
BASE_URL=""
ASUMIR_SI=0
SALTAR_TESTS=0

# ------------------------------------------------------------------ salida --

if [ -t 1 ]; then
  C_INFO=$'\033[36m'; C_OK=$'\033[32m'; C_ERR=$'\033[31m'
  C_WARN=$'\033[33m'; C_TIT=$'\033[1m'; C_OFF=$'\033[0m'
else
  C_INFO=""; C_OK=""; C_ERR=""; C_WARN=""; C_TIT=""; C_OFF=""
fi

titulo() { printf '\n%s── %s ──%s\n' "$C_TIT" "$*" "$C_OFF"; }
nota()   { printf '%s→ %s%s\n' "$C_INFO" "$*" "$C_OFF"; }
ok()     { printf '%s✓ %s%s\n' "$C_OK" "$*" "$C_OFF"; }
aviso()  { printf '%s! %s%s\n' "$C_WARN" "$*" "$C_OFF"; }
error()  { printf '%s✗ %s%s\n' "$C_ERR" "$*" "$C_OFF" >&2; exit 1; }

# Lee del terminal si lo hay, y si no de la entrada estándar: así el script
# sirve igual a mano que con la entrada redirigida (pruebas, tuberías, CI).
leer() {
  local destino="$1" oculto="${2:-}"
  # No basta con que /dev/tty exista: sin terminal de control, abrirlo da ENXIO.
  # La única comprobación fiable es intentar abrirlo.
  if { : < /dev/tty; } 2>/dev/null; then
    if [ -n "$oculto" ]; then
      read -r -s "$destino" < /dev/tty || eval "$destino=''"
      printf '\n'
    else
      read -r "$destino" < /dev/tty || eval "$destino=''"
    fi
  else
    read -r "$destino" || eval "$destino=''"
  fi
}

confirmar() {
  [ "$ASUMIR_SI" = "1" ] && return 0
  local respuesta=""
  printf '%s? %s [s/N] %s' "$C_WARN" "$1" "$C_OFF"
  leer respuesta
  case "$respuesta" in [sS]|[sS][iI]|[yY]|[yY][eE][sS]) return 0 ;; *) return 1 ;; esac
}

necesita() { command -v "$1" >/dev/null 2>&1 || error "falta '$1' en el PATH"; }

# wrangler vive en las dependencias del worker: no hace falta instalarlo global.
wr() { (cd "$WORKER" && npx --no-install wrangler "$@"); }

# --------------------------------------------------------------- preflight --

deps_worker() {
  necesita node
  necesita npm
  if [ ! -d "$WORKER/node_modules" ]; then
    nota "instalando dependencias del worker…"
    (cd "$WORKER" && npm install --no-audit --no-fund)
  fi
}

# ¿Hay sesión de Cloudflare?
#
# No basta con mirar el código de salida: `wrangler whoami` termina en 0 aunque
# no haya sesión, y se limita a decirlo por pantalla. Hay que leer la respuesta.
sesion_cloudflare() {
  local salida
  salida="$(wr whoami 2>&1 || true)"
  if printf '%s' "$salida" | grep -qi "You are logged in\|associated with the email"; then
    return 0
  fi
  printf '%s\n' "$salida" | tail -3 >&2
  error "sin sesión de Cloudflare. Entra con:  cd worker && npx wrangler login"
}

# El database_id de wrangler.toml tiene que existir de verdad en la cuenta.
comprobar_d1() {
  local id
  id="$(sed -n 's/^database_id = "\(.*\)"/\1/p' "$WORKER/wrangler.toml" | head -1)"
  [ -n "$id" ] || error "wrangler.toml no tiene database_id"
  case "$id" in
    *REEMPLAZAR*) error "wrangler.toml sigue con el database_id de ejemplo; lanza: $0 backend" ;;
  esac
  if wr d1 list --json 2>/dev/null | grep -q "$id"; then
    ok "base D1 encontrada ($id)"
  else
    error "el database_id de wrangler.toml ($id) no existe en esta cuenta de Cloudflare"
  fi
}

# AUTH_SECRET es obligatorio: sin él la API responde 503 y no autentica a nadie.
comprobar_secreto() {
  if wr secret list 2>/dev/null | grep -q "AUTH_SECRET"; then
    ok "AUTH_SECRET configurado"
  else
    error "falta el secreto AUTH_SECRET. Ponlo con:  $0 backend"
  fi
}

comprobar_git() {
  command -v git >/dev/null 2>&1 || return 0
  (cd "$RAIZ" && git rev-parse --git-dir >/dev/null 2>&1) || return 0
  if [ -n "$(cd "$RAIZ" && git status --porcelain)" ]; then
    aviso "hay cambios sin commitear: se desplegará el árbol de trabajo tal cual"
  else
    ok "árbol de trabajo limpio ($(cd "$RAIZ" && git rev-parse --short HEAD))"
  fi
}

preflight() {
  titulo "Comprobaciones previas"
  deps_worker
  ok "node $(node --version) · npm $(npm --version)"
  sesion_cloudflare
  ok "sesión de Cloudflare activa"
  comprobar_d1
  comprobar_secreto
  comprobar_git
  titulo "Sintaxis y tests"
  (cd "$WORKER" && npm run check)
  if [ "$SALTAR_TESTS" = "1" ]; then
    aviso "tests saltados por --skip-tests"
  else
    (cd "$WORKER" && npm test >/dev/null) && ok "tests del worker en verde"
  fi
  ok "todo listo para publicar"
}

# ----------------------------------------------------------------- backend --

backend() {
  titulo "Preparar el backend (solo la primera vez)"
  deps_worker
  sesion_cloudflare

  if confirmar "¿Crear el bucket R2 'guitarchords'? (si ya existe, di que no)"; then
    wr r2 bucket create guitarchords || aviso "el bucket ya existía"
  fi
  if confirmar "¿Crear la base D1 'vivace'? (si ya existe, di que no)"; then
    wr d1 create vivace || aviso "la base ya existía"
    aviso "copia el database_id que ha impreso wrangler en worker/wrangler.toml"
    confirmar "¿Ya está el database_id en wrangler.toml?" || error "pega el database_id y repite"
  fi
  comprobar_d1
  aplicar_esquema

  if wr secret list 2>/dev/null | grep -q "AUTH_SECRET"; then
    ok "AUTH_SECRET ya estaba configurado"
  else
    nota "AUTH_SECRET firma las sesiones. Una cadena larga y aleatoria, por ejemplo:"
    nota "  $(head -c 32 /dev/urandom | base64 | tr -d '\n/+=' | head -c 40)"
    wr secret put AUTH_SECRET
  fi
  ok "backend preparado. El PRIMER usuario que se registre queda como administrador."
}

# ------------------------------------------------------------------ esquema --

aplicar_esquema() {
  titulo "Esquema de la base de producción"
  nota "schema.sql solo CREA lo que falta; no borra ni reescribe datos."
  (cd "$WORKER" && npx --no-install wrangler d1 execute vivace --remote --yes --file=schema.sql)
  ok "schema.sql aplicado"

  # migrations.sql son ALTER TABLE, y SQLite no tiene "ADD COLUMN IF NOT EXISTS".
  #
  # Van UNA A UNA a propósito. Pasarle el fichero entero a D1 parece equivalente
  # y no lo es: en cuanto un ALTER falla —y el primero falla siempre, porque su
  # columna ya está desde la tanda anterior— se aborta el fichero completo y los
  # de abajo no llegan a ejecutarse. Con el fichero entero, "duplicate column
  # name" era indistinguible de "ya estaba todo aplicado", y el despliegue
  # seguía adelante dejando la base a medias con el código nuevo ya publicado.
  nota "migrations.sql (ALTER sobre tablas que ya existen)…"
  local aplicadas=0 existentes=0 sentencia salida codigo
  while IFS= read -r sentencia; do
    [ -n "$sentencia" ] || continue
    set +e
    salida="$( (cd "$WORKER" && npx --no-install wrangler d1 execute vivace \
                  --remote --yes --command "$sentencia") 2>&1 )"
    codigo=$?
    set -e
    if [ $codigo -eq 0 ]; then
      aplicadas=$((aplicadas + 1))
      nota "  aplicada: $(printf '%s' "$sentencia" | cut -c1-70)"
    elif printf '%s' "$salida" | grep -qi "duplicate column name"; then
      existentes=$((existentes + 1))
    else
      printf '%s\n' "$salida" >&2
      error "falló una migración por algo que NO es 'duplicate column name': $sentencia"
    fi
  done <<EOF_MIGRACIONES
$(sentencias_de "$WORKER/migrations.sql")
EOF_MIGRACIONES
  ok "migrations.sql: $aplicadas nueva(s), $existentes ya estaba(n)"
  comprobar_columnas
}

# Comprobar, no suponer.
#
# Este es el guardia que faltaba: la primera vez, migrations.sql abortó en su
# primer ALTER y el script lo dio por bueno, así que se publicó el código nuevo
# contra una base sin las columnas nuevas y dejó de poder escribirse ninguna
# partitura. Ahora, después de aplicar, se pregunta a la base si están de
# verdad; las que hay que buscar salen del propio migrations.sql, así que esto
# no hay que mantenerlo a mano.
comprobar_columnas() {
  local tabla columna faltan=""
  while IFS='|' read -r tabla columna; do
    [ -n "$tabla" ] || continue
    if ! (cd "$WORKER" && npx --no-install wrangler d1 execute vivace --remote --yes \
            --command "SELECT name FROM pragma_table_info('$tabla')" 2>/dev/null) \
          | grep -q "\"$columna\""; then
      faltan="$faltan $tabla.$columna"
    fi
  done <<EOF_COLUMNAS
$(sentencias_de "$WORKER/migrations.sql" \
   | sed -n 's/^ALTER TABLE \([a-z_]*\) ADD COLUMN \([a-z_]*\).*/\1|\2/p')
EOF_COLUMNAS
  if [ -n "$faltan" ]; then
    error "la base NO tiene estas columnas tras migrar:$faltan · no se publica código que las necesita"
  fi
  ok "la base tiene todas las columnas que el código espera"
}

# Parte un .sql en sentencias de una línea: quita comentarios y líneas en blanco
# y junta lo que quede hasta cada ';'.
sentencias_de() {
  sed 's/--.*$//' "$1" | tr '\n' ' ' | tr ';' '\n' \
    | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | grep -v '^$'
}

# ------------------------------------------------------------------ release --

release() {
  preflight
  titulo "Despliegue a producción"
  aviso "Esto publica la web y la API para todo el mundo."
  aviso "Se retiran las rutas heredadas (/list, /object, /bodies, /delete) y el panel /admin:"
  aviso "cualquier app antigua que aún use el token compartido dejará de sincronizar."
  confirmar "¿Seguir?" || { nota "cancelado, no se ha tocado nada"; exit 0; }

  # 1) La base primero: el código nuevo cuenta con columnas nuevas.
  aplicar_esquema

  # 2) Y ahora el código.
  titulo "Publicando el Worker"
  local salida
  salida="$( (cd "$WORKER" && npx --no-install wrangler deploy) 2>&1 | tee /dev/stderr )"
  local publicada
  publicada="$(printf '%s' "$salida" | grep -oE 'https://[a-zA-Z0-9._-]+\.workers\.dev' | head -1)"
  if [ -z "$BASE_URL" ]; then
    BASE_URL="$publicada"
    [ -n "$BASE_URL" ] && nota "URL publicada: $BASE_URL"
  elif [ -n "$publicada" ] && [ "$publicada" != "$BASE_URL" ]; then
    # Pasar --url con una dirección antigua es la forma más fácil de acabar
    # comprobando un Worker que no es el que se acaba de tocar.
    aviso "has pasado --url $BASE_URL, pero se ha publicado en $publicada"
    aviso "voy a comprobar la que has pasado; si no es la correcta, esto dará el código viejo"
  fi
  ok "Worker publicado"

  if [ -n "$BASE_URL" ]; then
    ESPERAR_PROPAGACION=1 verify
  else
    aviso "no he podido deducir la URL; comprueba a mano:  $0 verify --url https://…"
  fi

  titulo "Siguientes pasos"
  nota "1. Si es la primera vez, regístrate en ${BASE_URL:-la URL del Worker}: esa cuenta será la administradora."
  nota "2. Para indexar lo que ya estaba en R2:  $0 catalog --url ${BASE_URL:-<base>}"
  nota "3. Para publicar el APK:                 $0 app <ruta-al-apk> --url ${BASE_URL:-<base>}"
  nota "Si algo va mal:                          $0 rollback"
}

# ------------------------------------------------------------------- verify --

# Un GET que devuelve solo el código, sin reventar el script si falla.
codigo_de() {
  curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$1" 2>/dev/null || echo "000"
}

comprobacion() {
  local etiqueta="$1" esperado="$2" real="$3"
  if [ "$real" = "$esperado" ]; then
    ok "$etiqueta ($real)"
  else
    aviso "$etiqueta: esperado $esperado, recibido $real"
    FALLOS=$((FALLOS + 1))
  fi
}

# Dos rutas distinguen el código nuevo del anterior, sin necesidad de sesión:
#   /api/sync/changes  no existía antes  → antes 404, ahora 401
#   /list              ya no existe      → antes 401, ahora 404
# Si las dos dan lo de antes, esa URL está sirviendo el código VIEJO.
version_nueva() {
  [ "$(codigo_de "$BASE_URL/api/sync/changes")" = "401" ] &&
  [ "$(codigo_de "$BASE_URL/list")" = "404" ]
}

# Un despliegue tarda unos segundos en propagar por toda la red de Cloudflare.
# Comprobar al instante da falsos negativos, así que se espera un poco.
esperar_propagacion() {
  local intentos=12 i=1
  version_nueva && return 0
  while [ "$i" -le "$intentos" ]; do
    printf '\r  esperando a que propague el despliegue… %ss ' "$((i * 5))"
    sleep 5
    if version_nueva; then printf '\r%*s\r' 50 ""; return 0; fi
    i=$((i + 1))
  done
  printf '\r%*s\r' 50 ""
  return 1
}

# Cuando lo publicado es el código anterior, el problema no es "una comprobación
# que no cuadra": es que se está mirando otra cosa. Decirlo con todas las letras.
diagnostico_codigo_viejo() {
  printf '\n'
  aviso "esa URL está sirviendo el CÓDIGO ANTERIOR, no el que se acaba de publicar."
  aviso "Lo delata /list: responde 401, y ese guardia de token compartido ya no existe"
  aviso "en el código nuevo (respondería 404). Causas, por probabilidad:"
  aviso ""
  aviso "  1. La URL no es la del Worker desplegado. Comprueba cuál se ha publicado:"
  aviso "       cd worker && npx wrangler deployments list | tail -8"
  aviso "     y compárala con la que estás verificando: $BASE_URL"
  aviso "  2. Hay un dominio propio o una ruta apuntando a OTRO Worker (uno antiguo)."
  aviso "       Panel de Cloudflare → Workers → el servicio → Settings → Domains & Routes"
  aviso "  3. El despliegue no llegó a hacerse. Míralo con:"
  aviso "       cd worker && npx wrangler deployments list"
}

verify() {
  [ -n "$BASE_URL" ] || error "no sé contra qué URL comprobar; pásala con --url https://…"
  necesita curl
  titulo "Comprobando lo publicado en $BASE_URL"
  FALLOS=0

  # Se espera solo si venimos de publicar; en una comprobación suelta no tiene
  # sentido quedarse un minuto esperando algo que no se acaba de tocar.
  if [ "${ESPERAR_PROPAGACION:-0}" = "1" ] && ! esperar_propagacion; then
    diagnostico_codigo_viejo
    error "no sigo: lo que responde en esa URL no es lo que se ha publicado."
  fi

  comprobacion "la web responde"            200 "$(codigo_de "$BASE_URL/")"
  comprobacion "hoja de estilos"            200 "$(codigo_de "$BASE_URL/static/vivace.css")"
  comprobacion "JS de la aplicación"        200 "$(codigo_de "$BASE_URL/static/vivace-app.js")"
  comprobacion "catálogo público"           200 "$(codigo_de "$BASE_URL/api/songs/public")"

  # Estas dos dicen si de verdad está el código NUEVO desplegado: /api/sync/*
  # no existía antes (daría 404 en vez de 401) y /list ya no existe.
  comprobacion "API de sincronización viva" 401 "$(codigo_de "$BASE_URL/api/sync/changes")"
  comprobacion "ruta heredada /list fuera"  404 "$(codigo_de "$BASE_URL/list")"

  # 401 = el secreto está puesto y rechaza credenciales falsas.
  # 503 = falta AUTH_SECRET; la API no autenticaría a nadie.
  local login
  login="$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 -X POST \
    -H 'Content-Type: application/json' \
    -d '{"email":"comprobacion@vivace.invalid","password":"contrasena-de-prueba"}' \
    "$BASE_URL/auth/login" 2>/dev/null || echo "000")"
  if [ "$login" = "503" ]; then
    aviso "AUTH_SECRET no está configurado en producción: la API no autentica a nadie"
    FALLOS=$((FALLOS + 1))
  else
    comprobacion "sesiones firmadas" 401 "$login"
  fi

  # La caché de los estáticos: revalidar con la ETag tiene que dar 304.
  local etag rev
  etag="$(curl -s -D - -o /dev/null --max-time 20 "$BASE_URL/static/vivace.css" 2>/dev/null \
          | tr -d '\r' | sed -n 's/^[Ee][Tt]ag: //p' | head -1)"
  if [ -n "$etag" ]; then
    rev="$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 -H "If-None-Match: $etag" \
           "$BASE_URL/static/vivace.css" 2>/dev/null || echo "000")"
    comprobacion "caché de estáticos" 304 "$rev"
  else
    aviso "los estáticos no traen ETag"
    FALLOS=$((FALLOS + 1))
  fi

  if [ "$FALLOS" -eq 0 ]; then
    ok "todo responde como debe"
  else
    if ! version_nueva; then diagnostico_codigo_viejo; fi
    error "$FALLOS comprobación(es) no cuadran. Revisa antes de dar por bueno el despliegue."
  fi
}

# ----------------------------------------------------------------- catálogo --

# Lee un campo de un JSON por stdin sin depender de jq.
json_campo() {
  node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{try{const v=JSON.parse(s)[process.argv[1]];console.log(v===undefined||v===null?"":v)}catch(e){console.log("")}})' "$1"
}

catalogo() {
  [ -n "$BASE_URL" ] || error "hace falta la URL:  $0 catalog --url https://…"
  necesita curl
  titulo "Indexar en la base lo que ya estaba en R2"
  nota "No mueve ni reescribe ningún fichero; solo los registra a nombre del administrador."
  nota "Además rescata carpetas y favoritos de las cabeceras antiguas del texto."

  local email="" clave="" token
  printf 'Email de la cuenta administradora: '; leer email
  printf 'Contraseña: '; leer clave oculto

  token="$(node -e 'process.stdout.write(JSON.stringify({email:process.argv[1],password:process.argv[2]}))' "$email" "$clave" \
    | curl -s --max-time 30 -X POST "$BASE_URL/auth/login" \
        -H 'Content-Type: application/json' --data-binary @- | json_campo token)"
  [ -n "$token" ] || error "no se ha podido iniciar sesión"
  ok "sesión iniciada"

  local cursor="" total=0 saltadas=0 rescatadas=0 tanda=0 url resp hecho fallo
  while :; do
    tanda=$((tanda + 1))
    url="$BASE_URL/admin/migrate?visibility=public&backfill=1"
    [ -n "$cursor" ] && url="$url&cursor=$(node -e 'process.stdout.write(encodeURIComponent(process.argv[1]))' "$cursor")"
    resp="$(curl -s --max-time 60 -X POST "$url" -H "Authorization: Bearer $token")" \
      || error "se cortó en la tanda $tanda; vuelve a lanzarlo y sigue por donde iba"
    fallo="$(printf '%s' "$resp" | json_campo error)"
    [ -n "$fallo" ] && error "$fallo"
    total=$((total + $(printf '%s' "$resp" | json_campo imported)))
    saltadas=$((saltadas + $(printf '%s' "$resp" | json_campo skipped)))
    rescatadas=$((rescatadas + $(printf '%s' "$resp" | json_campo backfilled)))
    printf '\r  tanda %s · nuevas %s · ya estaban %s · rescatadas %s' "$tanda" "$total" "$saltadas" "$rescatadas"
    hecho="$(printf '%s' "$resp" | json_campo done)"
    [ "$hecho" = "true" ] && break
    cursor="$(printf '%s' "$resp" | json_campo cursor)"
    [ -n "$cursor" ] || break
    if [ "$tanda" -ge 500 ]; then
      printf '\n'; aviso "demasiadas tandas; vuelve a lanzarlo para continuar"; break
    fi
  done
  printf '\n'
  ok "indexadas $total · ya registradas $saltadas · carpetas/favoritos rescatados $rescatadas"
}

# ---------------------------------------------------------------------- app --

# Sube un fichero a R2.
#
# El flag para escribir en el bucket de verdad cambia entre versiones de
# wrangler: en la 3 lo remoto es lo predeterminado y `--remote` ni existe (pasa
# a ser un argumento desconocido y el comando falla enseñando la ayuda); en la 4
# hay que pedirlo. Se mira la ayuda en vez de suponer una de las dos.
r2_put() {
  local clave="$1" fichero="$2" tipo="${3:-}"
  # Ruta absoluta: wrangler se ejecuta desde worker/, así que una relativa dada
  # por quien llama al script se resolvería contra el directorio equivocado.
  case "$fichero" in /*) ;; *) fichero="$PWD/$fichero" ;; esac
  [ -f "$fichero" ] || error "no existe el fichero a subir: $fichero"
  local extra=()
  if wr r2 object put --help 2>&1 | grep -q -- "--remote"; then
    extra+=(--remote)
  fi
  [ -n "$tipo" ] && extra+=(--content-type="$tipo")
  wr r2 object put "$clave" --file="$fichero" "${extra[@]}"
}

app() {
  local apk="${1:-}"
  [ -n "$apk" ] || error "uso: $0 app <ruta-al-apk-firmado> [--url https://…]"
  [ -f "$apk" ] || error "no existe el fichero: $apk"
  deps_worker
  sesion_cloudflare

  # Los datos de la versión salen de build.gradle.kts, que es la única fuente.
  local vcode vname
  vcode="$(sed -n 's/^ *versionCode *= *\([0-9]*\).*/\1/p' "$RAIZ/app/build.gradle.kts" | head -1)"
  vname="$(sed -n 's/^ *versionName *= *"\([^"]*\)".*/\1/p' "$RAIZ/app/build.gradle.kts" | head -1)"
  [ -n "$vcode" ] && [ -n "$vname" ] || error "no he podido leer versionCode/versionName de app/build.gradle.kts"
  titulo "Publicar la app · versión $vname (código $vcode)"

  # Un APK firmado con la clave de DEPURACIÓN no sirve: si la firma cambia, las
  # actualizaciones no se instalan encima de la que ya tiene la gente.
  if command -v unzip >/dev/null 2>&1; then
    if unzip -p "$apk" 'META-INF/*.RSA' 2>/dev/null | strings 2>/dev/null | grep -qi "Android Debug"; then
      error "ese APK está firmado con la clave de DEPURACIÓN. No lo publiques: las actualizaciones dejarían de instalarse."
    fi
  else
    aviso "sin 'unzip' no puedo comprobar la firma del APK; asegúrate de que es el de release"
  fi

  # Si ya hay una versión publicada, el código tiene que subir.
  if [ -n "$BASE_URL" ]; then
    local publicado
    publicado="$(curl -s --max-time 20 "$BASE_URL/update" 2>/dev/null | json_campo versionCode)"
    if [ -n "$publicado" ] && [ "$vcode" -le "$publicado" ] 2>/dev/null; then
      error "en producción ya hay versionCode $publicado y este APK es $vcode. Sube versionCode antes de publicar."
    fi
  fi

  local notas="${NOTAS:-}"
  if [ -z "$notas" ] && [ "$ASUMIR_SI" != "1" ]; then
    printf 'Novedades de esta versión (una línea, opcional): '
    leer notas
  fi

  confirmar "¿Publicar $apk como versión $vname para todos los dispositivos?" || { nota "cancelado"; exit 0; }

  local tmp
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' RETURN
  node -e '
    const [code, name, notes] = process.argv.slice(1);
    process.stdout.write(JSON.stringify({
      versionCode: Number(code), versionName: name, notes, apkUrl: "/update/apk"
    }, null, 2));
  ' "$vcode" "$vname" "$notas" > "$tmp/latest.json"

  r2_put "guitarchords/app/app-release.apk" "$apk"
  r2_put "guitarchords/app/latest.json" "$tmp/latest.json" "application/json"
  ok "APK y latest.json subidos"
  [ -n "$BASE_URL" ] && nota "compruébalo:  curl $BASE_URL/update"
}

# ----------------------------------------------------------------- rollback --

rollback() {
  deps_worker
  sesion_cloudflare
  titulo "Volver a la versión anterior del Worker"
  aviso "Esto revierte el CÓDIGO. Lo ya aplicado en la base de datos NO se deshace."
  aviso "Como todo el esquema son columnas y tablas AÑADIDAS, el código antiguo sigue"
  aviso "funcionando con ellas: simplemente las ignora. No hay que deshacer nada."
  confirmar "¿Seguir?" || { nota "cancelado"; exit 0; }
  wr rollback
  ok "revertido. Comprueba con:  $0 verify --url <base>"
}

# --------------------------------------------------------------------- main --

ayuda() { sed -n '3,16p' "$0" | sed 's/^#\{0,1\} \{0,1\}//'; }

ACCION="${1:-}"
[ $# -gt 0 ] && shift || true
ARGS=()
while [ $# -gt 0 ]; do
  case "$1" in
    --yes|-y)     ASUMIR_SI=1 ;;
    --skip-tests) SALTAR_TESTS=1 ;;
    --url)        shift; BASE_URL="${1:-}" ;;
    --url=*)      BASE_URL="${1#--url=}" ;;
    *)            ARGS+=("$1") ;;
  esac
  shift || true
done
BASE_URL="${BASE_URL%/}"

case "$ACCION" in
  preflight) preflight ;;
  backend)   backend ;;
  schema)    deps_worker; sesion_cloudflare; comprobar_d1; aplicar_esquema ;;
  release)   release ;;
  catalog)   catalogo ;;
  app)       app "${ARGS[0]:-}" ;;
  verify)    verify ;;
  rollback)  rollback ;;
  ""|-h|--help|help) ayuda ;;
  *) error "acción desconocida: $ACCION (usa --help)" ;;
esac
