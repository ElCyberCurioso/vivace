# Accordio · contexto del proyecto

Documento de traspaso: qué es el proyecto, cómo está montado hoy, **por qué** se
tomaron ciertas decisiones (para no deshacerlas sin querer) y qué queda
pendiente.

Última actualización: 2026-09-02 · Rama `master` · último commit `8e550df`.

> **El proyecto se llama Accordio** en todo lo que ve el usuario: la web, el
> dominio (`accordio.site`) y la app. «Vivace» era el nombre anterior y sobrevive
> en nombres internos que no ve nadie —`VivaceClient`, `GuitarChordsTheme`, las
> claves `vivace_*` de `localStorage`, el directorio del proyecto—. **No se
> renombran**: las claves de almacenamiento cerrarían la sesión a todo el mundo y
> el `applicationId` (`com.guitarchords.app`) desinstalaría la app.

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
| `src/web-html.js` | Web de Accordio: `WEB_HTML`, `WEB_CSS` y `WEB_APP_JS`. |
| `src/client-lib.js` | JS de navegador compartido, servido en `/static/vivace.js`. |
| `src/chords-db.js` | Biblioteca de acordes (generada; ver `tools/generar-chords-db.mjs`). |
| `src/chords-seed.js` | Semilla curada de 348 acordes, anterior a la biblioteca. |

`src/admin-html.js` **se ha borrado** junto con las rutas de token compartido.

### Rutas del Worker
| Ruta | Acceso |
|---|---|
| `GET /` | Web Accordio (pública) |
| `GET /static/vivace.css`, `/static/vivace-app.js`, `/static/vivace.js` | Público, cacheado con ETag |
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
| `GET /api/chords/global` | Público · `PUT` y `seed`: editor o admin |
| `POST /admin/migrate?visibility=public[&backfill=1]` | Solo admin |
| `GET /api/settings` | Público (hoy solo `registrationOpen`) |
| `PUT /api/settings` | **Solo administrador** |
| `GET /update`, `GET /update/apk` | Público (auto-actualización de la app) |

Los tres listados de partituras (`/api/songs/public`, `/api/songs` y
`/api/songs?trash=1`) aceptan `?limit=` (tope 500), `?offset=` y `?q=`.

Las rutas heredadas con token compartido (`/list`, `/object`, `/bodies`,
`/delete`) y el panel `/admin` **ya no existen**.

**Transporte**: el Worker atiende `accordio.site` y `www.accordio.site` como
dominios propios (`custom_domain` en `wrangler.toml`). Antes de mirar la ruta,
`canonicalRedirect` manda todo a HTTPS y al apex —301 en GET/HEAD, 308 en el
resto— y `conHsts` añade HSTS de un año a lo que salga por HTTPS. `localhost`
está exento de las dos cosas.

### Almacenamiento
- **D1** (`vivace`): usuarios, metadatos de partituras, permisos y la tabla
  `settings` (clave→valor; hoy solo `registration_open`).
- **R2** (`guitarchords`): el **texto** de cada partitura (`songs/*.txt`), el blob
  de acordes por usuario (`users/<id>/chords.json`) y el APK (`app/`).
- `songs.r2_key` apunta a la clave original: **la migración no mueve ficheros**.

---

## 3. Trabajo realizado en la última sesión

Objetivo: dominio propio, la marca Accordio en la web y en la app, y que buscar,
recomendar e imprimir dejen de ser aproximaciones.

**Nada de esto está commiteado**: 58 ficheros modificados y ~19 sin seguir. El
mensaje de commit ya redactado está en `COMMIT_MESSAGE.txt`.

### 3.1 Dominio propio y transporte
- `accordio.site` como dominio del Worker (apex y `www`), declarado con
  `custom_domain`: wrangler crea el DNS y el certificado al desplegar. La zona ya
  estaba activa en la misma cuenta de Cloudflare.
- Un solo host y siempre cifrado (ver §2, «Transporte»). El `www` y el HTTP se
  arreglan **en el mismo salto**.
- Conviene activar además **Always Use HTTPS** en la zona (SSL/TLS → Edge
  Certificates): corta en el borde sin llegar a ejecutar el Worker.
- La app apunta a `https://accordio.site` de fábrica. **Ojo**: la 1.0 que había
  publicada se compiló con `UPDATE_BASE_URL` vacío, así que consulta la URL que
  cada uno tenga en Sincronización; quien no la tenga configurada no verá la
  actualización y tendrá que instalar la 2.0 a mano una vez.

### 3.2 Buscar, recomendar y altas de cuenta
- **Buscar es del servidor** (`?q=` en los tres listados). Antes solo se filtraba
  lo ya descargado —la primera página de 60—, así que una partitura más atrás no
  existía para el buscador hasta pulsar «Cargar más» varias veces. Sin distinguir
  mayúsculas ni tildes por los dos lados, y con los comodines de `LIKE` escapados.
- **Recomendadas** al pie del visor: primero del mismo artista y, si no hay, del
  mismo estilo. Dos consultas y no un `OR`, porque es una prioridad, no un filtro.
- **Interruptor de altas** en Administración, solo administrador. El corte está
  en `/auth/register`; la instalación vacía es la excepción a propósito.

### 3.3 Diccionario de acordes
- 8.669 acordes (~14.400 nombres con alias en bemoles) de
  **guitar-chords-db-json** (MIT), en `src/chords-db.js`, sembrados con «Importar
  diccionario base». Se regenera con `node tools/generar-chords-db.mjs <repo> 4`.
- El sembrado **solo añade**: no pisa la semilla curada ni lo editado a mano.
- Destapó un fallo de fondo: el origen da los trastes **absolutos** y el
  diccionario los quiere **relativos** al traste base. El dibujante de la web los
  trataba como absolutos, así que todo acorde con traste base > 1 salía mal.

### 3.4 Marca Accordio en la web
- Paquete `accordio_claro_oscuro.zip` (claro + oscuro). Los tokens `--ac-*` son
  la única fuente de color y medida; encima, la capa `--vv-*` cuelga de los
  **roles** del kit, que son los que cambian con el tema.
- Montserrat + Poppins; JetBrains Mono se queda para la hoja y las cifras.
- Iconos del kit como `<symbol>`; avisos y diálogos propios (se retiraron
  `alert`/`confirm`/`prompt`, que bloquean la página y desentonan).
- Lo que se usa del kit está copiado en `worker/brand/`, con un README que dice
  qué se cogió y qué no. **El Worker no lee esa carpeta**: los recursos van
  embebidos en `web-html.js`.

### 3.5 Móvil y editor
- Visor: cabecera de una línea con menú de acciones, mandos en panel que sube
  desde abajo, versiones al final de la partitura. Se **mueven los mismos nodos**
  según el ancho, no se duplican.
- La partitura entra de ancho al abrirla (mide la línea más larga y el ancho real
  de un carácter). Suelo de 13 px: es de legibilidad, no de encaje.
- Editor a tres columnas —escribir, ver, la ficha—, alineadas porque comparten
  estructura. La ficha hace su propio scroll y es columna hasta 1000 px.

### 3.6 Impresión
- El PDF pasa a ser un **documento A4**: marca, título y artista arriba,
  «Página N de M» abajo, repetidos en cada hoja.
- Las páginas **se reparten midiendo** desde JS. En Chrome un `position:fixed` no
  se repite por hoja y `counter(page)` devuelve `0` fuera de un margen con
  nombre, que no está implementado.
- El capo **no salía nunca**: la expresión que lo leía del rótulo vivía en un
  literal de plantilla, donde `\D` no es escape válido. Ahora sale del dato.

### 3.7 App Android con la misma imagen (2.0, publicada)
- Paleta, tipografías, formas e iconos del kit; barra teal en las 23 pantallas
  vía `accordioTopBarColors()`. Color dinámico apagado.
- Los iconos se construyen en Kotlin con los colores del tema (`ui/icons/`): los
  del kit tienen dos tonos y el coral cambia entre claro y oscuro.
- Nombre visible «Accordio». `versionCode 2` / `2.0`, firmada con la clave de
  siempre y **ya publicada** en `/update`.

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
   (`--vv-*` en la web, `ExtendedColors` en la app).

   Lo que **no** hay que romper:
   - **Los `--vv-*` cuelgan de los ROLES del kit** (`--ac-action`, `--ac-active`,
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

---

## 5. Cómo verificar

```bash
# Worker · no hace falta cuenta de Cloudflare
cd worker
npm run check     # sintaxis de los módulos y del JS que se sirve
npm test          # 187 tests
npm run dev       # servidor local en https (ver §8: adopta el host del dominio)
```

```bash
# App Android · Linux (el JDK del sistema ya vale: se instaló openjdk-21-jdk)
export ANDROID_HOME=$HOME/Android/Sdk ANDROID_SDK_ROOT=$HOME/Android/Sdk
./gradlew testDebugUnitTest     # 106 tests
./gradlew assembleDebug         # APK en app/build/outputs/apk/debug/
./gradlew assembleRelease       # APK firmado (necesita keystore.properties)
```

**El entorno de compilación ya está montado en esta máquina** (ver §9). Durante
un tiempo solo hubo JRE y la app no compilaba: AGP necesita `jlink`, y el error
que da no menciona el JDK por ningún lado. Se instaló `openjdk-21-jdk` y quedó
resuelto (ver §8 si tras actualizar Java falla AAPT2).

Añadidos en la última sesión: dominio y HSTS, búsqueda, recomendadas, altas de
cuenta y la biblioteca de acordes —incluida la paridad entre el navegador y el
servidor al normalizar y al dibujar—.

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

**El próximo despliegue crea la tabla `settings`**, así que la base va antes que
el código: `release` ya lo hace en ese orden. Un `wrangler deploy` a secas
publicaría el código sin la tabla; el interruptor de altas cae a «abiertas» y no
rompe nada, pero conviene no dejarlo así.

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
| App | `versionCode 2` / `2.0` publicada en `/update`, firmada con la clave de siempre (SHA-256 `e6a53587…`) |
| Diccionario | biblioteca completa sembrada en el diccionario global (14.424 nombres) |
| Estilo web | paquete de marca Accordio (claro + oscuro); fuentes en `worker/brand/` |
| Estilo app | mismo paquete: paleta, Montserrat+Poppins, formas, barra teal e iconos del kit |
| Altas de cuenta | interruptor en Administración (solo admin); estado en la tabla `settings` |

---

## 7. Pendiente

**Lo primero de todo**
- [ ] **COMMIT.** 58 ficheros modificados y ~19 sin seguir. Todo lo de §3 vive
      solo en el árbol de trabajo. El mensaje está escrito en
      `COMMIT_MESSAGE.txt`; el último commit del repositorio es `8e550df`.
- [ ] **Desplegar la web.** Lo desplegado llega hasta «el papel como documento».
      Sin subir: el capo en el PDF, el editor a tres columnas y su alineación, el
      scroll del panel lateral y el logo en la cabecera del papel.
      `cd worker && npx wrangler deploy`.
- [ ] **Copia de seguridad de la clave de firma** fuera de esta máquina (§9).
      Sin ella no se puede volver a actualizar la app nunca.
- [ ] **Probar la app 2.0 en un dispositivo real.** Compila y sus 106 tests
      pasan, pero **no se ha visto ejecutándose**: aquí no hay emulador. Mirar la
      migración de Room 16 → 17 sobre una base con datos, la prueba del modo
      avión (§5) y que la imagen nueva se vea bien.
- [ ] Decidir qué hacer con `accordio_claro_oscuro.zip` (2,4 MB, sin seguir en la
      raíz): ignorarlo o guardarlo. Lo que se usa ya está en `worker/brand/`.

**Convendría, sin prisa**
- [ ] Activar **Always Use HTTPS** y **Minimum TLS 1.2** en la zona de Cloudflare
      (SSL/TLS → Edge Certificates). El Worker ya redirige, pero eso corta antes.
- [ ] El token OAuth de wrangler caducó una vez a mitad de sesión (`9109 Invalid
      access token`): si falla algo de Cloudflare, `npx wrangler login`.
- [ ] Sembrar el diccionario en producción si no se ha hecho: pestaña Acordes →
      **Importar diccionario base** (una vez; solo añade).

**Mejoras identificadas y no abordadas**
- [ ] Sincronizar el progreso del entrenamiento (mismo patrón que los acordes).
- [ ] Niveles 4-5 del curriculum en el resto de áreas.
- [ ] El parser de cabeceras está tres veces: `SongTextFormat.kt`, `vParseSong`
      y `migrate.js`. El de YouTube, dos (cliente y servidor), con un test que
      compara ambas para que no se separen.
- [ ] CSP: ya no queda JS de la aplicación en línea, pero sigue habiendo un
      `<script>` mínimo en `<head>` para aplicar el tema sin parpadeo.
- [ ] i18n de `ui/dictionary/TheoryGuide.kt` (contenido largo en español).
- [ ] Layout de tablet tipo lista-detalle.
- [ ] Quedan emoji en la web (`🔒` de bloqueada, `♩`, `♦`, `▶`): el kit tiene
      iconos para casi todos.
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
