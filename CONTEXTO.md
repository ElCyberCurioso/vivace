# Vivace · contexto del proyecto

Documento de traspaso: qué es el proyecto, cómo está montado hoy, qué se hizo en
la última tanda de trabajo, **por qué** se tomaron ciertas decisiones (para no
deshacerlas sin querer) y qué queda pendiente.

Última actualización: 2026-07-02 · Rama `master` · **nada de esto está commiteado
todavía** (52 ficheros modificados + 33 nuevos).

---

## 1. Qué es Vivace

Sistema para tocar la guitarra con partituras (letra + acordes), con tres piezas
que comparten datos:

| Pieza | Dónde vive | Qué hace |
|---|---|---|
| **App Android** | `app/` | Kotlin + Jetpack Compose. Repertorio, visor, herramientas y entrenamiento. |
| **Worker + API** | `worker/` | Cloudflare Worker: API multiusuario, almacén (D1 + R2), web y panel de administración. |
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
- **Base de datos Room en la versión 16.** Migraciones acumuladas relevantes:
  - v11 `songs.created_at` · v13 `songs.locked` · v14 `songs.deleted_at` (papelera)
  - v15 índice **único** en `songs.remote_key` (con deduplicación defensiva previa)
  - v16 `songs.remote_id` + `songs.visibility` (sincronización con cuenta)
- Paquetes: `data/` (Room, orden y filtros), `sync/` (clientes y sincronización),
  `chords/` (diccionario, transposición, audio), `training/` (lógica pura del
  entrenamiento), `tuner/`, `metronome/`, `print/`, `update/`, `ui/`.

### Worker (módulos ES, `"type": "module"`)
| Fichero | Responsabilidad |
|---|---|
| `src/index.js` | Router (~300 líneas). |
| `src/api.js` | `/auth/*` y `/api/*` (API multiusuario). |
| `src/auth.js` | PBKDF2 + JWT. |
| `src/permissions.js` | Reglas de acceso (lógica pura). |
| `src/db.js` | Consultas a D1. |
| `src/migrate.js` | Indexado de lo que ya existía en R2. |
| `src/web-html.js` | Web pública de Vivace. |
| `src/admin-html.js` | Panel de administración (heredado, token compartido). |
| `src/client-lib.js` | JS de navegador compartido, servido en `/static/vivace.js`. |

### Rutas del Worker
| Ruta | Acceso |
|---|---|
| `GET /` | Web Vivace (pública) |
| `GET /admin` | Panel de administración — **ojo: antes estaba en `/`** |
| `GET /static/vivace.js` | Librería de cliente |
| `POST /auth/register`, `POST /auth/login` | Público |
| `GET /auth/me` | Sesión |
| `GET /api/songs/public[?owner=<id>\|all]` | Público (por defecto, lo del admin) |
| `GET`/`POST /api/songs` | Sesión |
| `GET /api/songs/:id` | Público si la partitura lo es; si no, dueño o admin |
| `PUT`/`DELETE /api/songs/:id` | Dueño o admin |
| `GET`/`PUT /api/chords` | Sesión (blob de acordes por usuario) |
| `POST /admin/migrate?visibility=public` | Solo admin |
| `GET /update`, `GET /update/apk` | Público (auto-actualización de la app) |
| `/list`, `/object`, `/bodies`, `/delete` | **Heredadas**, token compartido |

### Almacenamiento
- **D1** (`vivace`): usuarios, metadatos de partituras y permisos.
- **R2** (`guitarchords`): el **texto** de cada partitura (`songs/*.txt`), el blob
  de acordes por usuario (`users/<id>/chords.json`) y el APK (`app/`).
- `songs.r2_key` apunta a la clave original: **la migración no mueve ficheros**.

---

## 3. Trabajo realizado en la última sesión

### 3.1 Auditoría y corrección de errores (fase 1)
Se auditó todo el proyecto y se corrigieron **bugs reales**:
- **Una partitura en la papelera podía resucitar al sincronizar**: `getByRemoteKey`
  no filtraba la papelera. Las reglas se extrajeron a `sync/SyncPolicy.kt`
  (función pura testeada): una canción en papelera se salta por completo, pero
  conserva su clave remota para que tampoco se reimporte como nueva.
- **Canciones huérfanas**: si se borraba el fichero en el servidor, la copia local
  quedaba marcada como sincronizada para siempre. Ahora pierden el enlace remoto.
  Salvaguarda: si la lista remota llega **vacía**, no se desenlaza nada (protege
  contra una URL mal configurada).
- **`"Sin título"` hardcodeado en tres sitios**, uno comparado dentro del sync:
  ahora es `SongTextFormat.UNTITLED`, documentado como valor **persistido** (no se
  traduce, porque el sync lo compara).
- **Operaciones en lote no atómicas** (borrar/mover/reordenar/favoritas): pasan a
  una sola sentencia `WHERE id IN (:ids)` o a una transacción Room.
- **`remote_key` sin unicidad**: índice único + deduplicación previa.
- **El candado se saltaba en las versiones**: `VersionEditorScreen` ya pide
  confirmación como el editor principal.

### 3.2 Limpieza (fase 2)
- **`ui/components/SongListItem.kt`**: una sola fila de canción para las cuatro
  listas (carpeta, sin lista, favoritas, búsqueda), con huecos `leading`/`trailing`.
- **`MoveSongDialog`** unifica los dos diálogos de mover casi idénticos.
- **Cero warnings de deprecación**: `LinkAnnotation.Clickable` en vez de
  `ClickableText`, `MenuBook` AutoMirrored, `IntentCompat.getParcelableExtra`.
- Strings muertas fuera, `"Compartir lista"` a recurso, el editor deja de saltarse
  el Repository, ~30 imports sin uso eliminados.
- `CAMBIOS_PENDIENTES.md` borrado (obsoleto) y **README reescrito**.
- El worker gana `npm run check` y `npm test`; se **arregló `test-detect.mjs`**,
  que llevaba roto sin que nadie lo ejecutara.

### 3.3 UX (fase 3)
- **Snackbar con «Deshacer»** (la app no tenía ninguno) al mandar a la papelera
  (individual y en lote) y al sacar de una carpeta con el swipe.
  *Limitación conocida*: al deshacer un swipe la canción vuelve al final de la lista,
  no a su posición original.
- **Estado «no disponible»** en el visor (antes: spinner infinito si la canción no existía).
- **Favoritas** gana buscador y orden; **Búsqueda global** gana orden.
- Corregido el texto que decía «no se puede deshacer» cuando en realidad va a la papelera.

### 3.4 Funcionalidad nueva (fase 4)
- **Auto-comprobación de actualizaciones** al arrancar (una vez al día) + banner
  descartable en el Home.
- **Practicar los acordes de una canción** desde el visor: extrae los acordes
  reales (`chords/SongChords.kt`) y los empareja contra el metrónomo.
  **No da XP a propósito**: meter ejercicios arbitrarios en el curriculum rompería
  los ids estables y falsearía las estadísticas.
- **Modo concierto**: flechas ◀ ▶ en el visor para encadenar las canciones de una
  carpeta. **No navega**: cambia la canción en la misma pantalla, así se conservan
  tamaño de letra, velocidad y tono. Se implementó con botones y no con gesto para
  no chocar con el scroll horizontal de las tablaturas.
- **Área de oído (EAR)**: `training/ToneEngine.kt` (síntesis con AudioTrack),
  tres modos (intervalo, mayor/menor, dirección) y **9 ejercicios**. El área se
  activa sola porque el dashboard deriva «Próximamente» de si hay contenido.
- **Escuchar acordes**: `chords/ChordAudio.kt` (digitación → MIDI) y
  `ui/components/ChordPlayer.kt`. Rasgueo en el modal de acorde y en el buscador,
  y **cuerdas sueltas** al tocar el diagrama (`ChordDiagram` acepta `onStringTap`
  opcional, de modo que en los ejercicios sigue siendo informativo).

### 3.5 Sistema multiusuario (tres fases)
**Decisiones acordadas**: D1 + R2 · email y contraseña · todo pasa al admin y la
app usa login · por fases.

- **Backend**: usuarios, sesiones y permisos. Contraseñas con **PBKDF2-SHA256**
  (100k iteraciones, sal por usuario) y sesión **JWT HS256** firmada con
  `AUTH_SECRET`. **El primer usuario que se registra queda como administrador.**
  Login con mensaje único para email inexistente y contraseña incorrecta (no
  revela qué correos existen). `verifyToken` **nunca lanza**: basura → 401, no 500.
- **Web Vivace**: login/registro, catálogo público, «Mis partituras», **visor con
  auto-scroll, metrónomo (Web Audio), transposición ±11 con bemoles, tamaño de
  letra y capo**, y editor con conmutador **privada ↔ pública**.
- **App al login**: `sync/VivaceClient.kt` + `sync/AccountSyncManager.kt`
  sustituyen al token compartido. **Re-enlace automático**: la API devuelve la
  clave de R2 **solo al dueño**, y en el primer sync la app casa sus partituras
  con los ids nuevos → no se duplica nada.

---

## 4. Decisiones de diseño que conviene respetar

1. **El texto de las partituras vive en R2, no en D1.** `songs.r2_key` apunta a la
   clave original para que la migración no mueva ni reescriba objetos.
2. **`SongTextFormat.UNTITLED` no se traduce**: es un valor persistido que el sync
   compara.
3. **La papelera manda**: nada de lo borrado localmente resucita al sincronizar.
4. **El borrado permanente solo existe en la papelera** y siempre con confirmación.
   Borrar una lista **no** borra sus partituras (pasan a «Sin lista»).
5. **El candado (`locked`) solo se pone o quita desde el panel del worker**; la app
   y la web lo respetan pidiendo confirmación.
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

---

## 5. Cómo verificar

```bash
# App Android (Windows, sin Gradle en el PATH)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest     # 102 tests
.\gradlew.bat assembleDebug         # APK en app/build/outputs/apk/debug/

# Worker
cd worker
npm run check     # sintaxis de módulos y del JS embebido en las páginas
npm test          # 36 tests + detección de acordes
```

**Estado actual: 102 tests JVM + 36 del worker, todo en verde y sin warnings de compilación.**

Cobertura de tests (lógica pura): transposición, biblioteca de acordes, audio de
acordes, extracción de acordes de una canción, orden y filtrado de listas, formato
de texto, política de sincronización, fusión de acordes, entrenamiento completo
(gamificación, ritmo, oído, teoría, curriculum, test de nivel) y, en el worker,
autenticación, permisos, enrutado de la API y librería de cliente.

---

## 6. Puesta en marcha del servidor (pendiente de ejecutar)

```bash
cd worker
npx wrangler r2 bucket create guitarchords
npx wrangler d1 create vivace                     # pegar el id en wrangler.toml
npx wrangler d1 execute vivace --remote --file=schema.sql
npx wrangler secret put AUTH_SECRET
npx wrangler secret put SYNC_TOKEN                # heredado
npx wrangler deploy
```

Después: registrarse en la web (se queda como **admin**) →
`POST /admin/migrate?visibility=public` con el JWT → entrar en el móvil con la
misma cuenta y sincronizar (debe **re-enlazar sin duplicar**).

Para publicar una versión de la app hace falta un **keystore de release** y
`keystore.properties` (ver README). **La clave debe ser siempre la misma** o las
actualizaciones no se instalarán encima.

---

## 7. Pendiente

**Inmediato**
- [ ] **Estilo visual**: el usuario lo pasará; aplicarlo a la web (`web-html.js`,
      todo el color está en variables CSS de `:root`) y a la app (`ui/theme/`).
- [ ] Desplegar y probar el flujo multiusuario de punta a punta (no verificable sin
      la cuenta de Cloudflare).
- [ ] Commit: no hay nada commiteado de esta tanda.

**Cuando el multiusuario esté validado**
- [ ] Retirar las rutas heredadas con token compartido y borrar `sync/R2Client.kt`
      y `sync/SyncManager.kt` (ya no los usa la interfaz).
- [ ] Llevar el panel `/admin` a la sesión de administrador en vez del token.

**Mejoras identificadas y no abordadas**
- [ ] Sincronizar el progreso del entrenamiento (mismo patrón que los acordes).
- [ ] Niveles 4-5 del curriculum en el resto de áreas.
- [ ] Diagramas de acorde en la web (hoy solo en la app).
- [ ] El endpoint `/bodies` descarga todos los cuerpos: paginar si la colección crece.
- [ ] Duplicación entre `admin-html.js` y `client-lib.js` (el panel conserva su
      copia de transposición y render; se dejó así por no tocar lo que funciona).
- [ ] i18n de `ui/dictionary/TheoryGuide.kt` (contenido largo hardcodeado en español).
- [ ] Layout de tablet tipo lista-detalle.

---

## 8. Trampas conocidas

- **No editar ficheros `.kt` con scripts de PowerShell** (`Get-Content -Raw` +
  `-replace`) ni con `perl` sin cuidado: rompen la codificación UTF-8 y dejan
  mojibake en los acentos. Usar herramientas de edición o escribir bytes UTF-8
  explícitos. Comprobación rápida:
  `find app/src -name "*.kt" | while read f; do iconv -f UTF-8 -t UTF-8 "$f" >/dev/null || echo "$f"; done`
- **El panel de administración se movió de `/` a `/admin`.**
- **Tras desplegar el worker, forzar recarga (Ctrl+F5)**: el navegador cachea el HTML.
- El APK de depuración **no sirve** para probar la auto-actualización (va firmado
  con la clave de debug).
- `wrangler.toml` tiene un `database_id` de marcador que **hay que reemplazar**.
