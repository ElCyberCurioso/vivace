# Vivace · contexto del proyecto

Documento de traspaso: qué es el proyecto, cómo está montado hoy, **por qué** se
tomaron ciertas decisiones (para no deshacerlas sin querer) y qué queda
pendiente.

Última actualización: 2026-08-26 · Rama `master`.

---

## 1. Qué es Vivace

Sistema para tocar la guitarra con partituras (letra + acordes), con tres piezas
que comparten datos:

| Pieza | Dónde vive | Qué hace |
|---|---|---|
| **App Android** | `app/` | Kotlin + Jetpack Compose. Repertorio, visor, herramientas y entrenamiento. |
| **Worker + API** | `worker/` | Cloudflare Worker: API multiusuario, almacén (D1 + R2) y la propia web. |
| **Web Vivace** | `worker/src/web-html.js` | Servida por el Worker en `/`: catálogo, visor y editor desde el navegador. |

El **formato de partitura es el mismo en todas partes**: cabeceras `#clave: valor`,
separador `---` y cuerpo con acordes entre llaves.

```
#title: Ejemplo
#artist: Autor
#capo: 2
#locked: true
---
{Am} Casa   {C} árbol
```

---

## 2. Arquitectura actual

### App Android
- **minSdk 26 · target/compileSdk 35 · Kotlin 2.0.21 · JVM 17 · Gradle 8.7 / AGP 8.6.1**
- Compose BOM 2024.12.01, Room 2.6.1 (KSP), kotlinx.serialization, security-crypto.
- WorkManager 2.9.1 para la sincronización en segundo plano.
- **Base de datos Room en la versión 17**, con `exportSchema = true` (los esquemas
  van a `app/schemas/`). Migraciones acumuladas relevantes:
  - v11 `songs.created_at` · v13 `songs.locked` · v14 `songs.deleted_at` (papelera)
  - v15 índice **único** en `songs.remote_key` (con deduplicación defensiva previa)
  - v16 `songs.remote_id` + `songs.visibility` (sincronización con cuenta)
  - v17 `songs.remote_rev`; `remote_id`/`dirty`/`deleted_at` en `playlists` y
    `song_versions`; tabla `pending_deletes` (cola de borrados)
- Paquetes: `data/` (Room, orden y filtros), `sync/` (cliente, motor y worker),
  `chords/` (diccionario, transposición, audio), `training/` (lógica pura del
  entrenamiento), `tuner/`, `metronome/`, `print/`, `update/`, `ui/`.
- Piezas de sincronización: `sync/SyncPlan.kt` (reglas puras), `sync/SyncEngine.kt`
  (pull → aplicar → push por lotes), `sync/SyncWorker.kt` (WorkManager) y
  `sync/ChordSyncManager.kt` (blob de acordes, aparte). `R2Client`, `SyncManager`,
  `SyncPolicy` y `AccountSyncManager` **se han borrado**.

### Worker (módulos ES, `"type": "module"`)
| Fichero | Responsabilidad |
|---|---|
| `src/index.js` | Router y recursos estáticos (con ETag). |
| `src/api.js` | `/auth/*` y `/api/*` (API multiusuario). |
| `src/sync.js` | `/api/sync/*` (feed de cambios y subida por lotes) y `/api/playlists*`. |
| `src/limits.js` | Topes de entrada y límite de intentos (decisión pura + D1). |
| `src/auth.js` | PBKDF2 + JWT. |
| `src/permissions.js` | Reglas de acceso (lógica pura). |
| `src/db.js` | Consultas a D1. |
| `src/migrate.js` | Indexado de lo que ya existía en R2, con backfill de carpetas/favoritos. |
| `src/web-html.js` | Web de Vivace: `WEB_HTML`, `WEB_CSS` y `WEB_APP_JS`. |
| `src/client-lib.js` | JS de navegador compartido, servido en `/static/vivace.js`. |

`src/admin-html.js` **se ha borrado** junto con las rutas de token compartido.

### Rutas del Worker
| Ruta | Acceso |
|---|---|
| `GET /` | Web Vivace (pública) |
| `GET /static/vivace.css`, `/static/vivace-app.js`, `/static/vivace.js` | Público, cacheado con ETag |
| `POST /auth/register`, `POST /auth/login` | Público (con límite de intentos) |
| `GET /auth/me` | Sesión |
| `GET /api/songs/public[?owner=<id>\|all]` | Público (por defecto, lo del admin) |
| `GET`/`POST /api/songs` | Sesión (`?trash=1` para la papelera) |
| `GET /api/songs/:id` | Público si la partitura lo es; si no, dueño o admin |
| `PUT`/`DELETE /api/songs/:id` | Dueño, editor si es pública, o admin |
| `DELETE /api/songs/:id?hard=1` | Dueño, y solo desde la papelera |
| `PUT /api/songs/:id/favorite`, `POST /api/songs/:id/restore` | Dueño |
| `GET /api/sync/changes`, `POST /api/sync/push` | Sesión |
| `GET`/`POST /api/playlists`, `PUT`/`DELETE /api/playlists/:id` | Sesión |
| `GET`/`PUT /api/chords` | Sesión (blob de acordes por usuario) |
| `GET /api/chords/global` | Público · `PUT` y `seed`: editor o admin |
| `POST /admin/migrate?visibility=public[&backfill=1]` | Solo admin |
| `GET /update`, `GET /update/apk` | Público (auto-actualización de la app) |

Las rutas heredadas con token compartido (`/list`, `/object`, `/bodies`,
`/delete`) y el panel `/admin` **ya no existen**.

### Almacenamiento
- **D1** (`vivace`): usuarios, metadatos de partituras y permisos.
- **R2** (`guitarchords`): el **texto** de cada partitura (`songs/*.txt`), el blob
  de acordes por usuario (`users/<id>/chords.json`) y el APK (`app/`).
- `songs.r2_key` apunta a la clave original: **la migración no mueve ficheros**.

---

## 3. Trabajo realizado en la última sesión

Objetivo: que editar una partitura propia funcione igual desde el móvil y desde
la web, que administrar el catálogo se haga desde la web con la sesión, y que la
sincronización deje de depender de que alguien pulse un botón.

### 3.1 Seguridad
- **`AUTH_SECRET` deja de caer a `SYNC_TOKEN`.** Ese respaldo iba en todas las
  instalaciones antiguas de la app: quien lo tuviera podía firmar un JWT con el
  `sub` de cualquiera, administrador incluido. Si falta el secreto, la API
  responde 503 en todo lo que emita o acepte sesión (una lectura pública anónima
  sigue funcionando, y a quien llama sin token le toca 401, como antes).
- **Retiradas las rutas de token compartido** (`/list`, `/object`, `/bodies`,
  `/delete`) y el panel `/admin`. Se saltaban el modelo de permisos entero.
- **Límite de intentos** en `/auth/login` y `/auth/register` (10 por email+IP cada
  15 minutos, tabla `auth_attempts`). La decisión vive en `limits.js/rateDecision`,
  que es pura y está testeada.
- **Topes de entrada**: contenido ≤ 512 KB, campos ≤ 200 caracteres, blob de
  acordes ≤ 1 MB y validado como JSON (antes se guardaba `request.text()` crudo).
- **CORS** abierto solo en las lecturas públicas; el resto, al propio origen.
- `approveProposal` pasa a `db.batch()`: el efecto y el «resuelta» ya no pueden
  quedar a medias.

### 3.2 Esquema y API de sincronización
- D1: tabla `playlists`; `songs` gana `rev`, `favorite`, `position` y
  `playlist_id`; `song_versions` gana `rev`. Índices por `(owner_id, updated_at, id)`.
- **`rev` lo incrementa el servidor** en cada escritura. Es lo que permite
  detectar un conflicto: `updated_at` no sirve porque lo pisa cualquiera y dos
  relojes distintos no se pueden comparar.
- `GET /api/sync/changes`: un cursor por flujo (`<updated_at>:<id>`), lápidas
  incluidas y texto incrustado. `POST /api/sync/push`: lote con `baseRev` por
  partitura; lo que choca vuelve marcado como conflicto con la copia del servidor.
- Papelera de verdad en la API: `?trash=1`, `/restore` y `?hard=1`.
- Listados **paginados** (`limit`/`offset`, `hasMore`).
- `migrate.js` gana `backfill=1`: rescata `#playlist:`/`#favorite:` de las
  cabeceras y los pasa a columnas.

### 3.3 App: cola offline y sincronización automática
- **Todas** las mutaciones marcan `dirty`. Antes, favorito, mover de carpeta,
  reordenar y papelera escribían directas en el DAO y **no se subían nunca**.
- **`pending_deletes`**: el borrado definitivo deja lápida antes de que la fila
  desaparezca. Sin eso, la partitura seguía viva en el servidor y volvía a bajar
  como nueva. La purga de la papelera a 90 días solo toca lo ya confirmado.
- **`SyncWorker`** (WorkManager) hace el trabajo: sobrevive a cerrar la app y al
  reinicio. Disparadores: cambio local (agrupado 5 s), reconexión, arranque y
  repaso cada 6 h. Se usa `ExistingWorkPolicy.KEEP`, no `REPLACE`: REPLACE
  cancelaría también la pasada en curso. El hueco que deja KEEP lo cierra
  `doWork`, que pide otra vuelta si al terminar sigue habiendo pendientes.
- **Conflicto = versión.** Gana el servidor como Original y lo local se guarda
  como «Conflicto · fecha», marcado para subir. Con sync en segundo plano no hay
  a quién preguntar, y perder una edición en silencio no es aceptable.
- **`ChordSyncManager`, dos fallos de pérdida de datos corregidos**: (1) un error
  de red al leer el blob se trataba como «remoto vacío» y el push posterior
  borraba del servidor los acordes de los demás; (2) `clearDirtyAll()` limpiaba
  también lo editado *durante* la pasada, que se perdía sin subirse.
- `SongTextFormat.encode()` deja de escribir `#playlist:` y `#favorite:` (ya son
  campos); `decode()` los sigue leyendo por los ficheros antiguos.

### 3.4 Web
- **Listas, favoritos y papelera**, que solo existían en el móvil.
- Pestaña **Administración** (editor/admin): copia y restauración en ZIP
  —portadas del panel retirado—, «sin vídeo» y categorías automáticas.
- **Impresión** en el visor, con el tono y la cejilla que se están viendo.
- **Conmutador de tema**: los tokens de `[data-theme=light]` existían y no había
  forma de llegar a ellos.
- **Accesibilidad**: `role="dialog"` + `aria-modal` + trampa de foco y devolución
  del foco en los cuatro modales; `<label>` real en los filtros;
  `autocomplete="new-password"` al registrarse; `minlength` en la contraseña.
- **Errores visibles**: versiones y comentarios ya no se tragan el fallo en
  silencio dejando la sección vacía.
- **CSS y JS fuera del HTML** (`/static/vivace.css`, `/static/vivace-app.js`) con
  ETag y caché: la página pasa de ~114 KB sin cachear a 16 KB.
- **Visor, tres arreglos medidos en navegador**:
  - El scroll se heredaba entre partituras: al abrir la segunda se entraba por
    el final de la primera. `openSong` sí hacía `vBody.scrollTop = 0`, pero
    ANTES de mostrar el visor; con `#viewer` en `display:none` el elemento no
    tiene caja y esa asignación no hace nada. Movido a después de `.add("on")`.
  - **Ancho estándar** para todas: `.sheet` era `width:max-content` con
    `margin:0 auto`, así que cada partitura medía lo que midiera su línea más
    larga y las cortas salían centradas. Ahora hay un token `--vv-sheet:68ch`.
    En `ch` a propósito: al cambiar el tamaño de letra la hoja mantiene el
    mismo número de columnas.
  - `scrollbar-gutter:stable` en `#vBody` y desplazamiento lateral DENTRO de
    `.sheet`: sin lo primero el borde izquierdo bailaba 8 px entre una canción
    con barra y otra sin ella; sin lo segundo, una tablatura ancha movía de
    lado también los comentarios.

### 3.5 Herramientas
- `tools/deploy.sh` para Linux/macOS (antes solo había PowerShell): `preflight`,
  `backend`, `release`, `catalog`, `app`, `verify` y `rollback`, con verificación
  del despliegue incluida.
- `tools/deploy.ps1`: `-Backfill`, aplica `migrations.sql` y ya no pide `SYNC_TOKEN`.
  Además el esquema se aplica DENTRO de `Publish-Worker`: antes `-All` publicaba
  el Worker sin migrar, y ninguna combinación de parámetros debe poder dejar el
  código nuevo contra una base vieja.

**Tests: 149 en el worker (eran 111)** y **106 JVM en la app**, incluidos 14 de
recorrido completo por `src/index.js` (registro, edición web, sync móvil,
conflicto, borrado, lápida, papelera, idempotencia de la migración) y un D1 de
mentira con estado en `test/fake-d1.mjs`.

### 3.6 Despliegue a producción (hecho)

Está **desplegado y verificado** en
`https://guitarchords-sync.elcybercurioso.workers.dev`.

Salieron dos cosas por el camino, las dos ya arregladas:

1. **Falsos negativos por propagación.** `verify` corría justo después de
   `wrangler deploy` y pillaba la versión anterior: 5 comprobaciones en rojo con
   el despliegue correcto. Ahora espera hasta un minuto a que la URL responda
   con el código nuevo. Las dos rutas que lo distinguen sin sesión son
   `/api/sync/changes` (antes 404, ahora 401) y `/list` (antes 401, ahora 404).
2. **`migrations.sql` se aplicó a medias, y eso sí rompió producción.** El
   fichero empieza por el `ALTER` de `youtube_url`, que ya existía; D1 aborta el
   fichero entero al primer error, así que los 5 `ALTER` nuevos no llegaron a
   ejecutarse. El script vio «duplicate column name», lo dio por «ya estaba
   todo» y publicó igual: con el código nuevo en vivo y sin `rev`/`favorite`/
   `position`/`playlist_id`, **dejó de poder escribirse ninguna partitura**.
   Ahora las migraciones van una a una y, después, se le pregunta a la base si
   las columnas están de verdad; si falta alguna, no se publica.

También se corrigió que `r2 object put` recibía `--remote`, que no existe en
wrangler 3 (allí lo remoto es lo predeterminado; en la 4 hay que pedirlo). El
script mira la ayuda en vez de suponerlo.

**La app está publicada**: `versionCode 1 / versionName 1.0`, 13,3 MB, firmada
con la clave de release (no la de depuración) y servida en `/update/apk`. Se
comprobó que lo que baja el cliente es idéntico byte a byte al APK local.

## 4. Decisiones de diseño que conviene respetar

1. **El texto de las partituras vive en R2, no en D1.** `songs.r2_key` apunta a la
   clave original para que la migración no mueva ni reescriba objetos.
2. **`SongTextFormat.UNTITLED` no se traduce**: es un valor persistido que el sync
   compara.
3. **La papelera manda**: nada de lo borrado localmente resucita al sincronizar.
4. **El borrado permanente solo existe en la papelera** y siempre con confirmación.
   Borrar una lista **no** borra sus partituras (pasan a «Sin lista»).
5. **El candado (`locked`) es un seguro contra el despiste, no un permiso.** Se
   pone desde el editor de la web y tanto la app como la web lo respetan pidiendo
   confirmación. El servidor NO lo comprueba: si hiciera falta impedir de verdad
   una edición, el sitio es `permissions.js`.
6. **La práctica desde una canción no puntúa**; el progreso es solo del curriculum.
7. **`dynamic color` está desactivado** a propósito: la paleta propia garantiza el
   contraste de acordes y texto en claro y oscuro.
8. **La piel es el paquete de marca Vivace («Nocturno»)** y los tokens se llaman
   igual en los tres sitios: `--vv-*` en la web (`worker/src/web-html.js`),
   `vv_*` en `app/src/main/res/values*/colors.xml` y los mismos valores en
   `ui/theme/Theme.kt`. Reglas que no hay que romper: el ámbar cambia de tono
   entre modos (#E8B04B oscuro / #B8791F claro, nunca el claro sobre fondo
   claro), el verde `beat` es solo tempo y estado positivo (entra por
   `ExtendedColors.success`, no como adorno), y las cifras (BPM, hercios,
   cents) van en JetBrains Mono vía `VivaceMono`. Las dos fuentes van
   empaquetadas en `res/font/` en su versión variable, con la OFL en
   `app/licenses/`.
9. **Migraciones Room**: columna con `NOT NULL DEFAULT` en la migración y **sin**
   `@ColumnInfo(defaultValue=…)` en la entidad (patrón usado en todo el proyecto).
10. **La sincronización no pregunta nada.** Corre en segundo plano: no hay nadie
    al otro lado para resolver un conflicto. Ante un choque se conservan las dos
    versiones y se avisa después. Cualquier cambio que devuelva un diálogo
    bloqueante al flujo de sync rompe esa premisa.
11. **`rev` lo pone el servidor; el cliente solo lo devuelve.** Es la única forma
    de detectar un choque. No sustituirlo por `updated_at`: lo pisa cualquiera y
    los relojes de dos aparatos no se pueden comparar.
12. **Nada se borra en local sin haberlo comunicado antes.** Ese fue el bug de las
    partituras que resucitaban: si desaparece la fila y el servidor no se ha
    enterado, vuelve a bajar como nueva. Por eso existe `pending_deletes` y por eso
    la purga de la papelera exige `dirty = 0`.
13. **Publicar es un acto editorial.** Ni la app ni el dueño marcan la casilla:
    se propone. El servidor ignora en silencio un `visibility` de quien no es
    editor, en vez de rechazar la edición entera y perder el trabajo.
14. **Carpeta y favorito son campos de la API, no cabeceras del texto.** Volver a
    esconderlos en el `.txt` deja a la web sin poder enseñarlos.

---

## 5. Cómo verificar

```bash
# Worker · no hace falta cuenta de Cloudflare
cd worker
npm run check     # sintaxis de los módulos y del JS que se sirve
npm test          # 149 tests
```

```bash
# App Android · Linux
export JAVA_HOME=$HOME/jdks/jdk-17.0.20.1+1
export ANDROID_HOME=$HOME/Android/Sdk ANDROID_SDK_ROOT=$HOME/Android/Sdk
./gradlew testDebugUnitTest     # 106 tests
./gradlew assembleDebug         # APK en app/build/outputs/apk/debug/
./gradlew assembleRelease       # APK firmado (necesita keystore.properties)
```

**El entorno de compilación ya está montado en esta máquina** (ver §9). El JDK
del sistema NO sirve: es solo JRE y le falta `jlink`, que AGP necesita para
`core-for-system-modules.jar`. Da un error que no menciona el JDK por ningún
lado.

Cobertura de tests (lógica pura): transposición, biblioteca de acordes, audio de
acordes, extracción de acordes de una canción, orden y filtrado de listas, formato
de texto, **reglas de sincronización (`SyncPlan`)**, fusión de acordes,
entrenamiento completo (gamificación, ritmo, oído, teoría, curriculum, test de
nivel) y, en el worker, autenticación, permisos, enrutado, topes y límite de
intentos, feed de cambios, subida por lotes con conflicto, papelera, paginación,
idempotencia de la migración y un **recorrido completo** por `src/index.js`
(`test/flow.test.mjs`).

### Prueba manual del requisito offline

Modo avión → editar una partitura, moverla de carpeta y marcarla favorita →
**cerrar la app del todo** → recuperar la conexión → los tres cambios tienen que
subir solos, sin abrir la pantalla de sincronización.

**Esto es lo único importante que NO se ha probado todavía**: la app compila y
sus tests pasan, pero nadie la ha ejecutado en un dispositivo.

---

## 6. Despliegue

Ya está hecho. Para las siguientes veces, desde Linux/macOS:

```bash
./tools/deploy.sh preflight     # comprueba todo sin tocar nada
./tools/deploy.sh release       # esquema → deploy → verificación
./tools/deploy.sh app <apk> --url <base>    # publica una versión de la app
./tools/deploy.sh verify --url <base>
./tools/deploy.sh rollback      # vuelve al código anterior
```

`release` hace, EN ESTE ORDEN y sin poder separarse: comprobaciones previas
(sesión de Cloudflare, que el `database_id` exista, que `AUTH_SECRET` esté),
`npm run check`, tests, `schema.sql`, `migrations.sql` una a una, comprobación de
que las columnas están, `wrangler deploy` y verificación de lo publicado.

Desde Windows, lo equivalente es `tools/deploy.ps1` (además prepara la cadena de
compilación de Android y firma el APK).

Estado actual de producción:

| | |
|---|---|
| URL | `https://guitarchords-sync.elcybercurioso.workers.dev` |
| D1 | `vivace` · `1830ca34-43f6-429a-812d-5156287e90f1` |
| R2 | `guitarchords` |
| Secreto | `AUTH_SECRET` puesto (obligatorio; sin él la API responde 503) |
| Datos | 1 usuario (admin) · 426 partituras |
| App | `versionCode 1` / `1.0` publicada en `/update` |

---

## 7. Pendiente

**Lo primero de todo**
- [ ] **COMMIT.** Hay ~53 ficheros modificados y sin commitear. Todo lo descrito
      en §3 vive solo en el árbol de trabajo: si se pierde, se pierde entero.
      El último commit del repositorio sigue siendo `d38bbca`.
- [ ] **Copia de seguridad de la clave de firma** fuera de esta máquina (§9).
      Sin ella no se puede volver a actualizar la app nunca.
- [ ] **Probar la app en un dispositivo real.** Compila y sus 106 tests pasan,
      pero no se ha instalado ni ejecutado. Lo más importante que hay que mirar:
      la migración de Room 16 → 17 sobre una base con datos, y la prueba manual
      del modo avión (§5).

**Mejoras identificadas y no abordadas**
- [ ] La web no cambia de URL al navegar (`showView` alterna `.hidden`): no se
      puede enlazar a una partitura ni usar el botón «atrás».
- [ ] Sincronizar el progreso del entrenamiento (mismo patrón que los acordes).
- [ ] Niveles 4-5 del curriculum en el resto de áreas.
- [ ] Diagramas de acorde en la web (hoy solo en la app).
- [ ] El parser de cabeceras está tres veces: `SongTextFormat.kt`, `vParseSong`
      y `migrate.js`. El de YouTube, dos (cliente y servidor), con un test que
      compara ambas para que no se separen.
- [ ] CSP: ya no queda JS de la aplicación en línea, pero sigue habiendo un
      `<script>` mínimo en `<head>` para aplicar el tema sin parpadeo.
- [ ] i18n de `ui/dictionary/TheoryGuide.kt` (contenido largo en español).
- [ ] Layout de tablet tipo lista-detalle.
- [ ] `wrangler` está en la 3.114 y hay 4.x. Actualizar en su momento, no justo
      antes de un despliegue.

---

## 8. Trampas conocidas

- **No editar ficheros `.kt` con scripts de PowerShell** (`Get-Content -Raw` +
  `-replace`) ni con `perl` sin cuidado: rompen la codificación UTF-8 y dejan
  mojibake en los acentos. Usar herramientas de edición o escribir bytes UTF-8
  explícitos. Comprobación rápida:
  `find app/src -name "*.kt" | while read f; do iconv -f UTF-8 -t UTF-8 "$f" >/dev/null || echo "$f"; done`
- **El panel `/admin` ya no existe**; su sitio es la pestaña «Administración» de
  la web, con sesión de editor o administrador.
- **Los estáticos se cachean una hora** (`/static/*`), aunque revalidan por ETag:
  tras desplegar, un Ctrl+F5 evita sorpresas.
- **El JS de la web va dentro de un template literal** (`WEB_APP_JS`). Un acento
  grave o un `${` sin escapar rompe el módulo entero; `npm run check` lo detecta.
- **La app no compila sin SDK de Android**, y tampoco con el JDK del sistema si
  es solo JRE: AGP necesita `jlink` y el error que da no menciona el JDK.
- **Tras `wrangler deploy`, la propagación tarda unos segundos.** Verificar al
  instante da falsos negativos que parecen un despliegue roto.
- **`wrangler r2 object put` no acepta `--remote` en la 3.x** (allí lo remoto es
  lo predeterminado); en la 4 sí. El script mira la ayuda antes de pasarlo.
- **`wrangler whoami` termina en 0 aunque no haya sesión.** Hay que leer la
  respuesta, no el código de salida.
- **En bash, `local x="$(cmd)"` se traga el fallo de `cmd`**: el código de salida
  es el de `local`. Declarar y asignar en líneas separadas.
- **En PowerShell, la salida de un programa cae en la tubería de la función**,
  así que devolver un booleano desde una función que llama a `Invoke-Tool` llega
  mezclado con ella. Por eso `Invoke-ApplySchema` usa `$script:EsquemaAplicado`.
- **`migrations.sql` hay que aplicarlo SENTENCIA A SENTENCIA.** Si se le pasa el
  fichero entero a D1, el primer `ALTER` que falle —y el primero falla siempre,
  porque su columna ya está de la tanda anterior— aborta el fichero completo y
  los de abajo no se ejecutan. Esto ya pasó una vez: se publicó el código nuevo
  contra una base sin `rev`/`favorite`/`position`/`playlist_id` y dejó de poder
  escribirse ninguna partitura. Los dos guiones de despliegue lo hacen de una en
  una y además **comprueban después** que las columnas están de verdad.
- **Tras desplegar, Cloudflare tarda unos segundos en propagar.** Verificar al
  instante da falsos negativos; `deploy.sh` espera hasta un minuto a que la URL
  responda con el código nuevo antes de dar nada por malo.
- El APK de depuración **no sirve** para probar la auto-actualización (va firmado
  con la clave de debug).
- `wrangler.toml` tiene un `database_id` de marcador que **hay que reemplazar**.

---

## 9. Entorno de compilación y clave de firma

Montado en esta máquina durante la última sesión (no estaba nada):

| Qué | Dónde |
|---|---|
| SDK de Android | `~/Android/Sdk` (platform 35, build-tools 35.0.0, platform-tools) |
| JDK 17 | `~/jdks/jdk-17.0.20.1+1` (Temurin) |
| `local.properties` | en la raíz, con `sdk.dir` · ignorado por git |
| Clave de release | `~/vivace-release.jks` · alias `vivace` · RSA 4096, 10000 días |
| `keystore.properties` | en la raíz · ignorado por git · **la contraseña está ahí** |

**La contraseña no se escribe en este documento a propósito**: `CONTEXTO.md` sí
va a git. Está en `keystore.properties`, que no.

Sobre la clave: es la **primera** publicación, así que esa clave es la
definitiva. Android solo instala una actualización si va firmada con la misma.
Si se pierde el `.jks` o su contraseña, no se puede volver a actualizar la app:
habría que desinstalar y reinstalar, y cada usuario perdería sus datos locales.
Cópiala fuera de esta máquina.

Como la contraseña llegó a aparecer en una conversación, si preocupa se puede
cambiar todavía con `keytool -storepasswd` y actualizar `keystore.properties`:
solo hay un APK publicado y nadie lo ha instalado.
