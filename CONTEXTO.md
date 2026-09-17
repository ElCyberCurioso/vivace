# Accordio · contexto del proyecto

Documento de traspaso: qué es el proyecto, cómo está montado hoy, **por qué** se
tomaron ciertas decisiones (para no deshacerlas sin querer) y qué queda
pendiente.

Última actualización: 2026-09-17 · Rama `master` · último commit `03317c6`.

> Todo lo que describe este documento está **commiteado y subido** a
> `origin/master`, y el servidor está **desplegado** (versión `84a93ad9`). Lo
> único sin publicar es la app: sigue en `versionCode 2`, y no debe publicarse
> sin probarla antes en un móvil de verdad (§7).

> **El proyecto se llama Accordio**, y desde el renombrado también por dentro:
> código, comentarios, documentación, tokens de estilo, rutas de los estáticos y
> claves de almacenamiento. «Vivace» era el nombre anterior.
>
> Las claves SÍ se renombraron, pero **con mudanza**: `vivace_*` → `accordio_*`
> en `localStorage` y `guitarchords_sync` → `accordio_sync` en el móvil, con
> código que copia lo viejo a lo nuevo al arrancar. Sin esa mudanza, el cambio
> de nombre habría cerrado la sesión a todo el mundo.
>
> **Lo que sigue diciendo el nombre viejo, y por qué no se toca:**
>
> | Sigue igual | Motivo |
> |---|---|
> | `applicationId` y paquete `com.guitarchords.app` | Cambiarlo publica OTRA app: desinstala la instalada, se lleva sus datos y rompe la actualización. |
> | Clave de firma `vivace-release.jks`, alias `vivace` | Es la clave con la que está firmada la app publicada. |
> | Base D1 `vivace`, Worker `guitarchords-sync`, bucket `guitarchords` | Son nombres reales de recursos en Cloudflare: cambiarlos en `wrangler.toml` no los renombra, apuntan a otra cosa. Renombrarlos de verdad es migrar datos con corte de servicio. |
> | `/static/vivace.*` | Se mantienen como **alias** de `/static/accordio.*` mientras caduca la caché de quien tenga la página vieja abierta. |
> | El prefijo `v` de los ids del visor (`vTitle`, `vBody`…) | Ahí la `v` es de **visor**, no de Vivace. La librería de cliente sí pasó de `v*` a `ac*` (`acRenderSong`, `acChordSvg`…). |

---

## 1. Qué es Accordio

Sistema para tocar la guitarra con partituras (letra + acordes), con tres piezas
que comparten datos:

| Pieza | Dónde vive | Qué hace |
|---|---|---|
| **App Android** | `app/` | Kotlin + Jetpack Compose. Repertorio, visor, herramientas y entrenamiento. |
| **Worker + API** | `worker/` | Cloudflare Worker: API multiusuario, almacén (D1 + R2) y la propia web. |
| **Web Accordio** | `worker/src/web-html.js` | Servida por el Worker en `/`: catálogo, visor y editor desde el navegador. |

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
- Paquetes: `data/` (Room, orden y filtros), `sync/` (cliente `AccordioClient`,
  motor y worker), `chords/` (diccionario, instrumentos —`Instrument.kt`—,
  transposición, audio), `training/` (lógica pura del entrenamiento), `tuner/`
  (detector de tono y catálogo de afinaciones —`Tunings.kt`—), `metronome/`,
  `print/` (PDF idéntico al de la web), `update/`, `ui/`.
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
| `src/web-html.js` | Web de Accordio: `WEB_HTML`, `WEB_CSS` y `WEB_APP_JS`. |
| `src/client-lib.js` | JS de navegador compartido, servido en `/static/accordio.js`. |
| `src/chords.js` | Diccionarios globales por instrumento (guitarra/ukelele) y variantes de digitación por partitura. |
| `src/chords-db.js` | Biblioteca de acordes (generada; ver `tools/generar-chords-db.mjs`). |
| `src/chords-seed.js` | Semilla curada de 348 acordes, anterior a la biblioteca. |
| `eslint.config.mjs` | Reglas de lint (pocas y elegidas; `no-undef` es la que importa). |

`src/admin-html.js` **se ha borrado** junto con las rutas de token compartido.

### Rutas del Worker
| Ruta | Acceso |
|---|---|
| `GET /` | Web Accordio (pública) |
| `GET /static/accordio.css`, `/static/accordio-app.js`, `/static/accordio.js` (y los alias `/static/vivace.*`) | Público, cacheado con ETag |
| `POST /auth/register`, `POST /auth/login` | Público (con límite de intentos) |
| `GET /auth/me` | Sesión |
| `GET /api/songs/public[?owner=<id>\|all]` | Público (por defecto, lo del admin) |
| `GET`/`POST /api/songs` | Sesión (`?trash=1` para la papelera) |
| `GET /api/songs/:id` | Público si la partitura lo es; si no, dueño o admin |
| `GET /api/songs/:id/related` | Como la de origen (devuelve solo catálogo público) |
| `PUT`/`DELETE /api/songs/:id` | Dueño, editor si es pública, o admin |
| `DELETE /api/songs/:id?hard=1` | Dueño, y solo desde la papelera |
| `PUT /api/songs/:id/favorite`, `POST /api/songs/:id/restore` | Dueño |
| `GET /api/sync/changes`, `POST /api/sync/push` | Sesión |
| `GET`/`POST /api/playlists`, `PUT`/`DELETE /api/playlists/:id` | Sesión |
| `GET`/`PUT /api/chords` | Sesión (blob de acordes por usuario) |
| `GET /api/chords/global[?instrument=guitarra\|ukelele]` | Público · `PUT` y `seed`: editor o admin |
| `POST /admin/migrate?visibility=public[&backfill=1]` | Solo admin |
| `GET /api/settings` | Público (hoy solo `registrationOpen`) |
| `PUT /api/settings` | **Solo administrador** |
| `GET /update`, `GET /update/apk` | Público (auto-actualización de la app) |

Los tres listados de partituras (`/api/songs/public`, `/api/songs` y
`/api/songs?trash=1`) aceptan `?limit=` (tope 500), `?offset=`, `?q=`, `?sort=`
(`title`|`recent`|`old`) y `?genre=`. Los propios y la papelera aceptan además
`?visibility=private|public`, `?favorite=1` y `?playlist=<id>|none`: **todos los
filtros se resuelven en SQL**, nunca en el navegador (ver §4). `POST` y `PUT
/api/songs` aceptan `chordVariants`; en `PUT`, si no viene, se conserva.

Las rutas heredadas con token compartido (`/list`, `/object`, `/bodies`,
`/delete`) y el panel `/admin` **ya no existen**.

**Transporte**: el Worker atiende `accordio.site` y `www.accordio.site` como
dominios propios (`custom_domain` en `wrangler.toml`). Antes de mirar la ruta,
`canonicalRedirect` manda todo a HTTPS y al apex —301 en GET/HEAD, 308 en el
resto— y `conCabecerasSeguras` añade HSTS de un año a lo que salga por HTTPS,
más `nosniff`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy` y la
CSP en modo informe. `localhost` está exento del HSTS y de los redirects, no de
las demás cabeceras: así un fallo de política se ve mientras se desarrolla.

### Almacenamiento
- **D1** (`vivace`, nombre real del recurso; ver la nota del principio): usuarios,
  metadatos de partituras, permisos y la tabla
  `settings` (clave→valor; hoy solo `registration_open`).
- **R2** (`guitarchords`): el **texto** de cada partitura (`songs/*.txt`), el blob
  de acordes por usuario (`users/<id>/chords.json`), los diccionarios globales
  —`chords/global-chords.json` (guitarra) y `chords/global-chords-ukelele.json`
  (ukelele, **vacío hasta que se importe**; el fichero para subirlo se genera con
  `tools/generar-ukelele.mjs`, ver §7)— y el APK (`app/`).
- `songs.r2_key` apunta a la clave original: **la migración no mueve ficheros**.

---

## 3. Trabajo realizado en las últimas sesiones

### 3.0 Sesión 16–17/09/2026 · siete commits, `b4f8fd0` … `03317c6`

Todo commiteado, subido y —lo del servidor— desplegado. 38 ficheros,
+2.864/−52. Orden de los commits:

| Commit | Qué |
|---|---|
| `b4f8fd0` | Cabeceras de seguridad, errores sin filtrar, freno de escritura, despliegue del esquema |
| `af41fd2` | CI en GitHub Actions, ESLint y más casos en el test de URLs |
| `d2ee0a7` | El esquema se prueba contra SQLite de verdad |
| `c732e69` | Diccionario de ukelele |
| `a1d9489` | No versionar la salida del generador |
| `e1c40e9` | `chordVariants` en la app (y el fallo de sync que destapó) |
| `03317c6` | Subir de verdad la base a la versión 18 |

**Dos fallos que estaban en producción y ya no están**

1. **El despliegue no podía aplicar el esquema.** `wrangler d1 execute --file`
   no ejecuta el SQL: lo sube y luego pide a la API que lo importe
   (`POST /d1/database/<id>/import`), y ese paso falla desde esta máquina con un
   «fetch failed» que parece falta de red. Reproducido con un fichero de una
   línea, y también con wrangler 4, así que actualizar no arregla nada.
   `aplicar_esquema` manda ahora `schema.sql` por `--command` en una sola
   llamada, con repaso sentencia a sentencia de reserva para señalar la culpable.
2. **El push de sincronización borraba las variantes de acorde.**
   `stmtUpdateSongMeta` reescribe la fila entera con `meta.chord_variants || ""`,
   y `sync.js` no nombraba la columna: cada sincronización desde el móvil se
   llevaba por delante lo elegido en la web. La ruta `PUT /api/songs/:id` sí se
   protegía; esta se quedó fuera y sin prueba que lo viera. Ahora se conservan
   salvo que el cliente diga otra cosa, y `null` cuenta como «no las toques»
   (la app serializa con `encodeDefaults`, así que un campo vacío viaja como
   null explícito). Dos pruebas lo fijan. **Lo ya perdido no vuelve.**

**Servidor**
- `conHsts` pasa a `conCabecerasSeguras`: `nosniff`, `X-Frame-Options: DENY`,
  `Referrer-Policy`, `Permissions-Policy` y CSP **en modo informe**
  (`Content-Security-Policy-Report-Only`). Para hacerla obligatoria, ver §7.
- El catch global ya no serializa `err.message` al cliente —daba nombres de
  tablas y columnas—: registra el detalle con un id corto y devuelve ese id.
- Freno de escritura (60/min por usuario) en crear partitura, comentario y
  propuesta; `rateLimit` acepta tope y ventana propios y `auth_attempts` se
  barre sola pasadas 24 h.
- El registro deja de confirmar si un email ya tiene cuenta.
- `listProposals` paginada; el administrador del catálogo se recuerda 5 minutos
  en vez de preguntarlo a D1 en cada carga de la portada.
- `[observability]` en `wrangler.toml`: antes un fallo no dejaba rastro ninguno.
- La pestaña del navegador dice `Título · Autor · Accordio` al abrir partitura.

**Calidad**
- **CI** (`.github/workflows/ci.yml`): worker (`npm ci`, `check`, `test`) y app
  (`./gradlew test`), en push, PR y a mano.
- **ESLint** encadenado a `npm run check`; nada más ponerlo cazó dos imports
  muertos. Los `catch (e)` que no miran la excepción pasan a `catch` a secas.
- `test/esquema.test.mjs`: `schema.sql` y `migrations.sql` contra SQLite real
  (`node:sqlite`), incluida la invariante de la que vive el despliegue —repetirlo
  no cambia el esquema—. Worker: **212 pruebas**.
- `npm run deploy` **falla a propósito** y remite a `tools/deploy.sh`; queda
  `deploy:raw` para emergencias.

**Ukelele y digitaciones**
- `app/src/main/assets/chords/ukulele.json` (chords-db, MIT): 552 acordes,
  2.114 digitaciones. `tools/generar-ukelele.mjs` produce el diccionario del
  servidor **desde ese mismo fichero** —una sola copia en el repositorio— y
  añade los alias en sostenidos, porque chords-db escribe el ukelele en bemoles
  y quien teclee `{C#m}` no encontraría nada. Salen 777 nombres.
- `chords/global-chords-ukelele.json` **sigue vacío**: falta importarlo (§7).
- La app lee y escribe `chordVariants`: `ChordVariants` (puro, 8 pruebas),
  columna `chord_variants`, **migración de Room 17 → 18**, baja y sube por
  sincronización, y el modal de acorde abre por la postura elegida y deja fijar
  otra. Sin partitura delante (diccionario, buscador) el botón no aparece.

> **Cuidado con Room.** `version = 18` se quedó sin aplicar en un primer intento
> y Room, sin quejarse, exportó el esquema con la columna nueva **llamándolo
> 17.json**. En un móvil con datos eso cierra la app al arrancar («Room cannot
> verify the data integrity»). Corregido en `03317c6`: versión 18, `17.json`
> restaurado y `18.json` exportado. Al tocar entidades, **comprobar siempre que
> aparece el `<versión>.json` nuevo**.

### 3.1 Web: cada pantalla es una ruta
- Antes las pantallas se abrían llamándose entre ellas y se apilaban: al editor
  se llegaba **sin sesión** y «atrás» con el ratón llevaba a sitios que no
  tocaban. Ahora hay rutas en el fragmento (`PANTALLAS` en `web-html.js`) y
  `navegar()` / `aplicarUrl()` son la única puerta; ver §4.18.
- Guardia al resolver la URL: sin sesión → `#/entrar` recordando el destino; sin
  rango → catálogo con aviso; `#/editar/<id>` carga la ficha y exige dueño o
  admin. Cerrar sesión con el editor abierto saca de él.
- Guardar/borrar hacen `replaceState`; cerrar el visor o el editor vuelve atrás
  solo si la entrada la puso la aplicación (`history.state.propia`).
- `openSong()` ya **no** toca el historial: lo ejecuta la ruta.

### 3.2 Las privadas no salían: filtros al servidor
- «Solo privadas» (activo por defecto), favoritas, carpeta, categoría y orden se
  aplicaban en el navegador sobre la página descargada. Van a SQL
  (`filtrosPropios` y `ordenDe` en `db.js`, `filtrosDe` en `api.js`).

### 3.3 Digitación elegida por partitura
- `songs.chord_variants` (migración en `migrations.sql`), `chordVariants` en la
  API, botón **Digitaciones** en el editor (modal `#varModal`) y uso en visor,
  tira de acordes, hover y ficha del acorde. Ver §4.20.

### 3.4 Instrumentos: ukelele
- **Worker**: `INSTRUMENTOS` en `chords.js`, blob propio por instrumento,
  `?instrument=` en `/api/chords/global`. El de ukelele **está vacío**.
- **Web**: selector en el visor (`#vInstr`) y en Acordes (`#chordInstr`),
  «Importar JSON…», `acChordSvg` dibuja las cuerdas que traiga la digitación.
  La preferencia es del lector (`accordio_instrument`), no de la partitura.
- **App**: `Instrument` + `InstrumentPrefs`; `ChordDb`, `ChordLibrary`,
  `ChordDiagram`, `ChordAudio` y `ChordPlayer` por instrumento. Selector en el
  diccionario y en `ChordModal`; la práctica de cambios sigue el instrumento y
  los ejercicios de entrenamiento siguen siendo de guitarra. Ver §4.21–23.

### 3.5 Afinador de la app
- `tuner/Tunings.kt`: 9 afinaciones de guitarra y 4 de ukelele, frecuencias
  calculadas desde la nota, cuerda más cercana en semitonos, elección guardada
  (`TunerPrefs`). En `TunerScreen`: pastillas de instrumento + desplegable.

### 3.6 Editor web
- Escribir/ver a todo el ancho; la ficha es una barra superior (rejilla de
  6→4→3→2→1 columnas por ancho). Desde 901 px la página queda fija al alto de la
  ventana y solo hacen scroll los dos paneles (`#editSplit{flex:1 1 0}`).
- Capo como carrusel −/+ (`#eCapoStep`, 0–12 con vuelta), barras de scroll con
  los colores de la marca y hoja del visor que crece hasta evitar el scroll
  lateral (`width:max-content`).

### 3.7 Papel
- Marca de agua en cada hoja del PDF web: logo y «Accordio» en diagonal, 7 %.
- `print/PrintAdapter.kt` reescrito para dar **el mismo documento** que la web
  (ver §4.26). `PrintAdapter.print()` recibe `semitones` para la píldora de tono.

### 3.8 Renombrado Vivace → Accordio y carpeta
- Ver la nota del principio. La carpeta del proyecto pasó de
  `~/Desktop/projects/vivace` a **`~/Desktop/projects/accordio`**; hay que abrir
  Android Studio desde la ruta nueva.

---

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
7. **`dynamic color` está desactivado** a propósito: el color dinámico de Android
   12+ pinta la app con el fondo de pantalla del móvil y deja de ser Accordio,
   que es justo lo que se quiere reconocer entre la web y el teléfono.
8. **La piel es el paquete de marca Accordio**, el mismo en la web y en la app.
   Los valores salen de `tokens.css` / `tokens.dark.css` (copia en
   `worker/brand/`) y viven en tres sitios: `--ac-*` en `web-html.js`, `ac_*` en
   `res/values*/colors.xml` y `ui/theme/Theme.kt`. Encima va una capa semántica
   (`--ui-*` en la web (antes `--vv-*`), `ExtendedColors` en la app).

   Lo que **no** hay que romper:
   - **Los `--ui-*` cuelgan de los ROLES del kit** (`--ac-action`, `--ac-active`,
     `--ac-highlight`, `--ac-pending`, `--ac-nav-*`), no de las rampas. Son los
     roles los que cambian con el tema: la marca no se invierte, lo que cambia es
     quién hace de acción. En oscuro el teal no contrasta y pasa a titular; la
     acción la toma el turquesa.
   - **Un solo acento por bloque**: coral llama a la acción, turquesa indica
     estado. **El amarillo es solo estado** (capo, valoración, pendiente), nunca
     adorno.
   - **Texto corrido sobre coral o amarillo, jamás**: para leer van las rampas
     `coral-700` / `yellow-900`. El coral de marca sobre crema da 2,4:1.
   - Dos contrastes suben respecto al kit, y en los dos casos sube el **rol**, no
     el token: el texto secundario sobre tarjeta clara (`#7B8E92` da 3,2:1) y
     sobre oscura (`#8CA6AA`, 3,6:1).
   - **Montserrat** titulares, **Poppins** texto, y **JetBrains Mono** para la
     hoja y las cifras: el kit no trae monoespaciada y sin ancho fijo los acordes
     dejan de caer sobre su sílaba. Empaquetadas en `res/font/`, con la OFL en
     `app/licenses/`.
   - **El papel va siempre en claro** aunque se lea en oscuro, y sin el mosaico.
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
15. **Buscar y recomendar los resuelve el servidor.** Filtrar en el navegador
    solo alcanza a lo ya descargado, que es de donde venía el buscador roto.
16. **Los diagramas usan trastes RELATIVOS al traste base**, la convención de
    chords-db, en la app y en la web. Si algún día se mezclan con absolutos,
    todo acorde con cejilla saldrá con los puntos corridos.
17. **El diccionario global solo se amplía**: sembrar añade lo que falta y nunca
    pisa lo editado a mano ni la semilla curada.
18. **En la web, cada pantalla es una ruta y la ruta es la única puerta.**
    `#/`, `#/mias`, `#/papelera`, `#/propuestas`, `#/acordes`, `#/usuarios`,
    `#/admin`, `#/entrar`, `#/cancion/<id>`, `#/nueva` y `#/editar/<id>`. Cada
    una declara el permiso que pide (`sesion`, `editor`, `admin`) y el guardia se
    aplica **al resolver la URL**: al arrancar, al navegar, al volver con
    «atrás» y al cerrar sesión. Nada de abrir una pantalla llamando a su función:
    así es como se llegaba al editor sin sesión y como el historial acababa
    llevando a sitios que no correspondían. Editar comprueba además la ficha
    (dueño o admin), porque tener sesión no es tener permiso sobre ESA partitura.
19. **Los filtros del listado se resuelven en SQL, no en el navegador.** El
    listado viene por páginas: filtrar solo lo descargado hacía que «Solo
    privadas» o una carpeta salieran vacías hasta pulsar «Cargar más» varias
    veces. En el navegador se queda **solo** el recorte por el texto del
    buscador, que tiene que responder tecla a tecla.
20. **La digitación de un acorde es de la partitura, no del diccionario.**
    `songs.chord_variants` guarda, por instrumento, qué posición usa cada acorde
    en esa canción (`{"guitarra":{"F":2}}`); el índice 0 no se guarda porque es
    el valor por defecto. La API **solo la toca si el cliente la manda**: la app
    Android envía la ficha entera y no la conoce, así que sin esa condición
    guardar desde el móvil borraría lo elegido en la web.
21. **El instrumento es un dato, no un ajuste de pintado.** En la app,
    `Instrument` (guitarra 6 cuerdas / ukelele 4) lleva la afinación al aire, el
    MIDI de cada cuerda y el nombre del diccionario; de ahí salen el diagrama,
    el sonido y las digitaciones. Una `ChordShape` dice a qué instrumento
    pertenece por cuántos trastes trae, así que un diagrama suelto se pinta y
    suena bien sin arrastrar el instrumento por media aplicación.
22. **En el ukelele no se exige la fundamental en el bajo.** Es una regla de
    guitarra: la afinación estándar del ukelele es *reentrante* (la 4.ª cuerda
    suena más aguda que la 3.ª) y no hay bajo que valga. Con la regla puesta, el
    generador devolvía posturas altas en vez del Do al aire; sin ella salen las
    de siempre (C 0003, Am 2000, F 2010, G 0232), y hay tests que las fijan.
23. **Las digitaciones propias del usuario son de guitarra.** Se guardan con seis
    trastes y sin instrumento, así que el editor de digitaciones solo se ofrece
    en guitarra en vez de guardar algo que después nadie sabría leer.
24. **Las afinaciones del afinador se calculan, no se copian.** `Tunings` parte
    del nombre de la nota (La4 = 440 Hz): una tabla de decimales escritos a mano
    es justo donde se cuela el error que hace que el afinador diga que estás
    afinado cuando no lo estás. La cuerda más parecida se busca en SEMITONOS y
    no en hercios, o el afinador se va siempre a las agudas.
25. **Cada instrumento tiene su diccionario, con su blob y sus cuerdas.**
    Guitarra son 6 valores por digitación y ukelele 4; mezclarlos obligaría a
    adivinar de qué instrumento es cada posición. El de ukelele **existe y está
    vacío**: la estructura está montada (blob propio, validación a 4 cuerdas,
    selector en el visor y en el diccionario, importador de JSON) y lo único que
    falta es el contenido.
26. **La hoja impresa es la misma en la web y en la app.** La referencia es
    `ESTILO_IMPRESION` (web); `PrintAdapter.kt` la reproduce en un lienzo de
    `PrintedPdfDocument` (puntos a 72 ppp: px CSS × 0,75 y mm × 2,8346). Si se
    cambia una, se cambia la otra. Montserrat y JetBrains Mono son fuentes
    **variables**: en `Paint` hay que pedir el peso con
    `setFontVariationSettings("'wght' 700")` o Montserrat sale en Thin.

---

## 5. Cómo verificar

```bash
# Worker · no hace falta cuenta de Cloudflare
cd worker
npm run check     # sintaxis de los módulos y del JS que se sirve
npm test          # 205 tests
npm run dev       # servidor local en https (ver §8: adopta el host del dominio)
```

```bash
# App Android · Linux (el JDK del sistema ya vale: se instaló openjdk-21-jdk)
export ANDROID_HOME=$HOME/Android/Sdk ANDROID_SDK_ROOT=$HOME/Android/Sdk
./gradlew testDebugUnitTest     # 121 tests
./gradlew assembleDebug         # APK en app/build/outputs/apk/debug/
./gradlew assembleRelease       # APK firmado (necesita keystore.properties)
```

**El entorno de compilación ya está montado en esta máquina** (ver §9). Durante
un tiempo solo hubo JRE y la app no compilaba: AGP necesita `jlink`, y el error
que da no menciona el JDK por ningún lado. Se instaló `openjdk-21-jdk` y quedó
resuelto (ver §8 si tras actualizar Java falla AAPT2).

Añadidos en la última sesión: filtros del listado en SQL
(`test/filtros.test.mjs`), instrumentos y variantes de digitación de punta a
punta (`test/instrumentos.test.mjs`); en la app, posturas y audio de ukelele
(`UkuleleChordsTest`) y afinaciones (`tuner/TuningsTest`).

**Prueba de la web en un navegador sin cabeza** (lo que usó la última sesión
para las rutas, los permisos, los filtros, las digitaciones y la mudanza de
claves; el arnés vivía fuera del repo y hay que rehacerlo):
1. Un script de Node importa `WEB_HTML`, `WEB_CSS`, `CLIENT_JS` y `WEB_APP_JS` y
   escribe una sola página: CSS en `<style>`, JS en `<script>` **cambiando
   `</script` por `<\/script`** (hay cadenas que lo contienen y cerrarían la
   etiqueta), y sin el CSS nada se oculta (`.hidden` vive ahí).
2. Antes de la aplicación, un `<script>` sustituye `window.fetch` por una API de
   mentira con respuestas fijas y pone (o no) `accordio_token`.
3. Otro `<script>` al final ejecuta los pasos con esperas y vuelca el resultado
   en un `<div>` fuera de pantalla.
4. `chromium --headless --no-sandbox --virtual-time-budget=10000 --dump-dom
   pagina.html | grep -o 'id="TEST"[^>]*>[^<]*'`. Con `--screenshot` y
   `--window-size=1440,900` se ve la maquetación.

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

**Esto sigue sin probarse**: la app compila y sus tests pasan, pero nadie la ha
ejecutado en un dispositivo. La 2.0 está publicada sin haberse visto correr.

La web sí se ha verificado a fondo en navegador (Chromium por CDP): capturas en
claro y oscuro, viewport de móvil con detección de solapes, y la impresión
comprobada generando el PDF de verdad y leyéndolo con `pypdf`.

---

## 6. Despliegue

Web y app están desplegadas, pero **hay cambios de web sin subir** (§7). Desde
Linux/macOS:

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
que las columnas **y las tablas** están, `wrangler deploy` y verificación de lo
publicado (que incluye el 301 de HTTP y la cabecera HSTS en un dominio propio).

**El próximo despliegue añade la columna `songs.chord_variants`** (y crea la
tabla `settings` si aún no se hizo), así que **la base va antes que el código**:
`release` ya lo hace en ese orden. Un `wrangler deploy` a secas publicaría un
código que escribe en una columna que no existe, y **guardar cualquier
partitura fallaría**.

Para publicar la app: `./tools/deploy.sh app <apk> --url https://accordio.site`.
Lee `versionCode`/`versionName` de `build.gradle.kts`, rechaza un APK firmado con
la clave de depuración y exige que el código suba respecto a lo publicado.

Desde Windows, lo equivalente es `tools/deploy.ps1` (además prepara la cadena de
compilación de Android y firma el APK).

Estado actual de producción:

| | |
|---|---|
| URL | `https://accordio.site` (canónica; `www` → 301 al apex) |
| URL anterior | `https://guitarchords-sync.elcybercurioso.workers.dev` (sigue viva) |
| D1 | `vivace` · `1830ca34-43f6-429a-812d-5156287e90f1` |
| R2 | `guitarchords` |
| Secreto | `AUTH_SECRET` puesto (obligatorio; sin él la API responde 503) |
| Datos | 1 usuario (admin) · 426 partituras |
| Worker | Versión `84a93ad9`, desplegada el 17/09/2026 con `./tools/deploy.sh release` |
| App | `versionCode 2` / `2.0` publicada en `/update`, firmada con la clave de siempre (SHA-256 `e6a53587…`). **Los cambios del 16–17/09 NO están publicados** |
| CI | GitHub Actions en cada push y PR; en verde |
| Diccionario | biblioteca completa sembrada en el diccionario global (14.424 nombres) |
| Estilo web | paquete de marca Accordio (claro + oscuro); fuentes en `worker/brand/` |
| Estilo app | mismo paquete: paleta, Montserrat+Poppins, formas, barra teal e iconos del kit |
| Altas de cuenta | interruptor en Administración (solo admin); estado en la tabla `settings` |

---

## 7. Pendiente

**Lo primero de todo**
- [ ] **Copia de seguridad de la clave de firma** fuera de esta máquina (§9).
      Sin ella no se puede volver a actualizar la app nunca. Sigue pendiente
      desde hace sesiones y es lo único irreversible de esta lista.
- [ ] **Probar la app en un dispositivo real.** Compila y sus tests pasan, pero
      **no se ha visto ejecutándose**: aquí no hay emulador. Ahora pesa más que
      antes, porque la base cambió de versión. Mirar:
      - la **migración de Room 17 → 18** sobre una base con datos (§3.0);
      - que tras actualizar **se conserve la sesión** (mudanza de
        `guitarchords_sync` → `accordio_sync`);
      - los diagramas de ukelele, que ahora vienen del diccionario empaquetado
        y ya no de las posturas calculadas;
      - elegir una digitación en el visor, sincronizar y verla puesta en la web;
      - el modo avión (§5), el selector del afinador y el PDF impreso.
- [ ] **Importar el diccionario de ukelele en la web.** Generarlo con
      `node tools/generar-ukelele.mjs` (deja `worker/tools/ukelele-diccionario.json`,
      ~214 KB, ignorado por git) y subirlo desde **Acordes → Instrumento:
      Ukelele → Importar JSON…**. Hasta entonces la web sigue sin posturas de
      ukelele; la app ya las trae dentro.
- [ ] **Publicar la app** cuando lo anterior esté probado: subir
      `versionCode`/`versionName` y
      `./tools/deploy.sh app <apk> --url https://accordio.site`.
- [ ] **Pasar la CSP a obligatoria.** Hoy va como
      `Content-Security-Policy-Report-Only` porque no había navegador con el que
      probarla. Recorrer catálogo, visor con vídeo, editor y diccionario con la
      consola abierta; si no se queja, renombrar la cabecera en
      `conCabecerasSeguras` (`index.js`). Si protesta, ajustar la política, no
      quitarla.

**Convendría, sin prisa**
- [ ] Activar **Always Use HTTPS** y **Minimum TLS 1.2** en la zona de Cloudflare
      (SSL/TLS → Edge Certificates). El Worker ya redirige, pero eso corta antes.
- [ ] El token OAuth de wrangler caducó una vez a mitad de sesión (`9109 Invalid
      access token`): si falla algo de Cloudflare, `npx wrangler login`.
- [ ] Sembrar el diccionario en producción si no se ha hecho: pestaña Acordes →
      **Importar diccionario base** (una vez; solo añade).

**Ukelele y digitaciones: hecho en la sesión del 16–17/09**

Queda solo importar el blob en la web (arriba). El diccionario ya viaja en la
app, el generador existe y `chordVariants` funciona de punta a punta. Formato
del blob, por si hace falta subirlo a mano con
`PUT /api/chords/global?instrument=ukelele`:
`{"chords": {"Am": [{"frets":[2,0,0,0],"fingers":[2,0,0,0],"baseFret":1,"barres":[]}]}}`.
**Cuatro valores por digitación**, de la 4ª cuerda (Sol) a la 1ª (La); el
servidor rechaza los de seis. `seed` solo siembra guitarra.

**Mejoras identificadas y no abordadas**
- [ ] Sincronizar el progreso del entrenamiento (mismo patrón que los acordes).
- [ ] Niveles 4-5 del curriculum en el resto de áreas.
- [ ] El parser de cabeceras está tres veces: `SongTextFormat.kt`, `acParseSong`
      y `migrate.js`. El de YouTube, dos (cliente y servidor), con un test que
      compara ambas para que no se separen.
- [ ] El `<script>` mínimo del `<head>` (tema sin parpadeo) es lo que obliga a
      `'unsafe-inline'` en la CSP. Sacándolo se puede cambiar por un hash y
      cerrar ese hueco del todo.
- [ ] Tests de migración de Room con `MigrationTestHelper`: ya hay dos esquemas
      (`17.json` y `18.json`) con los que contrastar, pero no existe `androidTest`
      ni Robolectric en el repositorio, así que no hay dónde ejecutarlos.
- [ ] Plan por fases de mejora (fases 3 a 5): Google Play —variantes `play` y
      `direct`, R8, páginas de privacidad y términos, borrado y exportación de
      cuenta—, PWA, accesibilidad, layout de tablet, i18n de la web y FTS5 en la
      búsqueda cuando el catálogo lo pida.
- [ ] i18n de `ui/dictionary/TheoryGuide.kt` (contenido largo en español).
- [ ] Layout de tablet tipo lista-detalle.
- [ ] Quedan emoji en la web (`🔒` de bloqueada, `♩`, `♦`, `▶`): el kit tiene
      iconos para casi todos.
- [ ] `wrangler` está en la 3.114 y hay 4.x. Actualizar en su momento, no justo
      antes de un despliegue.
- [ ] Meter en el repo el arnés de pruebas de la web en navegador (§5): hoy hay
      que rehacerlo cada vez.
- [ ] Retirar, pasado un tiempo, las mudanzas de claves (`mudarClaves` en la web,
      `mudar` en `SyncPrefs`), los alias `/static/vivace.*` y la cancelación de
      los trabajos `vivace-sync*` de WorkManager.

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
- **Y lo que `npm run check` NO detecta, del mismo literal: los escapes.** `\D`,
  `\n`, `\s`… no son escapes válidos de plantilla, así que la barra se pierde y
  al navegador le llega otra cosa. Esto ya rompió dos veces: `/\D+/` llegó como
  `/D+/` y el capo no salió nunca en el PDF, y un `join("\n")` llegó partido en
  dos líneas. **Hay que escribirlos dobles** (`\\D`) y, ante la duda, comparar el
  fuente con lo que sirve el Worker:
  `node --input-type=module -e 'const m=await import("./src/web-html.js"); …'`
- **La app no compila sin SDK de Android**, y tampoco con el JDK del sistema si
  es solo JRE: AGP necesita `jlink` y el error que da no menciona el JDK.
- **Si Java se actualiza con el daemon de Gradle vivo, AAPT2 deja de arrancar**:
  «Daemon startup failed» y, tres excepciones más abajo, «Failed to exec spawn
  helper». No es aapt2 ni los recursos —el binario arranca a mano—: es que
  `jspawnhelper` valida su versión contra la JVM en memoria. Se arregla con
  `./gradlew --stop` y matando los daemons.
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
- **En impresión, Chrome no repite un `position:fixed` por página** ni resuelve
  `counter(page)` fuera de un margen con nombre de `@page`, que no implementa. Si
  se toca la impresión, no volver a intentarlo por ahí: el reparto de páginas se
  hace midiendo desde JS (`GUION_PAGINAR`).
- **En un contenedor flex, un hijo con `overflow` necesita `min-height:0`** o no
  encoge por debajo de su contenido: la caja crece, el scroll no llega a
  activarse y acaba desplazándose la página entera. Pasó con la ficha del editor.
- **`wrangler dev` adopta el host del `custom_domain`**, así que en local también
  redirige a HTTPS. Por eso `npm run dev` usa `--local-protocol https`.
- **`node --test test/` falla en Node 22.23** con «Cannot find module …/test»:
  por eso `npm test` pasa el patrón `"test/*.test.mjs"`.
- **`test/fake-d1.mjs` asocia los valores POR POSICIÓN**: añadir una columna a un
  `INSERT`/`UPDATE` de `db.js` sin mover los índices del fake deja tests en rojo
  con valores cruzados (así salió `rev` 5 en vez de 6 al meter `chord_variants`).
- **Los acentos graves también rompen dentro de un COMENTARIO** de `WEB_APP_JS`:
  para el literal de plantilla un comentario no es nada especial.
- **El CSS del editor tiene trampas de orden**: una `@media` escrita ANTES de la
  regla base que quiere pisar pierde con la misma especificidad, y `#eSide label`
  gana a `#eLockedWrap` (hace falta `#eSide #eLockedWrap`). Y si `#editSplit`
  tiene `flex-basis:auto`, el alto de su contenido le quita sitio a la ficha:
  va con `flex:1 1 0`.
- **En Chromium con barras «Fluent», las flechas de la barra de scroll no se
  quitan**: con `scrollbar-color` puesto, `::-webkit-scrollbar-button` no aplica.
  El color y el grosor sí se respetan.
- **WorkManager guarda los trabajos únicos por NOMBRE en disco**: renombrar uno
  sin cancelar el viejo deja los dos corriendo para siempre.
- **El prefijo `v` de la web significa dos cosas**: los ids del visor (`vTitle`,
  `vBody`, `vChordBar`…, de *visor*) y, hasta el renombrado, la librería de
  cliente (hoy `ac*`). Un buscar-y-reemplazar de `v[A-Z]` se lleva el visor.

---

## 9. Entorno de compilación y clave de firma

Montado en esta máquina durante la última sesión (no estaba nada):

| Qué | Dónde |
|---|---|
| SDK de Android | `~/Android/Sdk` (platform 35, build-tools 34 y 35, platform-tools) |
| JDK | `openjdk-21-jdk` del sistema (antes solo estaba el JRE y no compilaba) |
| `local.properties` | en la raíz, con `sdk.dir` · ignorado por git |
| Clave de release | `~/vivace-release.jks` · alias `vivace` · RSA 4096, 10000 días |
| `keystore.properties` | en la raíz · ignorado por git · **la contraseña está ahí** |

**La contraseña no se escribe en este documento a propósito**: `CONTEXTO.md` sí
va a git. Está en `keystore.properties`, que no.

Sobre la clave: con ella van firmadas la 1.0 y la 2.0, y Android solo instala una
actualización si va firmada con la misma (SHA-256 `e6a5358757105ae231dae6c526d42d04…`).
Si se pierde el `.jks` o su contraseña, no se puede volver a actualizar la app:
habría que desinstalar y reinstalar, y cada usuario perdería sus datos locales.
**Sigue sin haber copia fuera de esta máquina** (§7).
Cópiala fuera de esta máquina.

Como la contraseña llegó a aparecer en una conversación, si preocupa se puede
cambiar todavía con `keytool -storepasswd` y actualizar `keystore.properties`:
solo hay un APK publicado y nadie lo ha instalado.
