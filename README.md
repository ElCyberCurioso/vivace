# Vivace

Aplicación Android para guitarristas: gestiona partituras con letra y acordes, las sincroniza con tu propio servidor y añade herramientas de práctica (afinador, metrónomo, diccionario y un sistema de entrenamiento gamificado).

Interfaz en español e inglés, 100 % Jetpack Compose + Material 3, con tema claro/oscuro seleccionable.

## Características

### Partituras y listas
- Listas (carpetas) con orden manual por arrastre o por título, artista, fecha de creación o de modificación.
- Buscador dentro de cada carpeta (título, artista y género) y búsqueda global por contenido.
- Visor con letra y acordes alineados, **auto-scroll** de velocidad variable (mantiene la pantalla encendida), tamaño de fuente, **transposición ±11 semitonos** con opción de bemoles, capo, versiones alternativas por canción y **metrónomo integrado**.
- Editor con inserción de acordes `{Am}` y bloques de tablatura `{tab}…{/tab}`.
- Favoritas, selección múltiple (mover, borrar, marcar) e impresión/PDF.
- **Papelera**: lo borrado se conserva 90 días y se puede restaurar; el borrado definitivo solo ocurre ahí, con confirmación.
- **Bloqueo de partituras**: las marcadas desde el panel web piden confirmación antes de editarse.
- Compartir/importar listas como ZIP `.gtrlist`.

### Sincronización (Cloudflare Worker + R2)
- Sincronización diferencial estilo git: la descarga es automática, la subida se confirma y los conflictos se resuelven canción a canción.
- Etiqueta por partitura: sincronizada con el servidor o solo local.
- Los **acordes personalizados** se sincronizan aparte, de forma automática y convergente.
- **Panel web de administración** servido por el propio Worker: editor con vista previa, modo visualización con auto-scroll y transposición, detección automática de acordes (individual y en lote), borrado masivo, backup/restauración en ZIP y bloqueo de partituras.
- **Auto-actualización**: la app consulta la versión publicada, descarga el APK y lanza el instalador (ver `worker/` más abajo).

### Herramientas
- **Diccionario** de acordes (chords-db) con digitaciones propias editables que tienen prioridad sobre las de la base.
- **Buscador inverso**: marca trastes en el mástil y la app identifica el acorde (incluidos slash chords).
- **Afinador** por micrófono (algoritmo YIN) y **metrónomo** con acento de compás.
- **Entrenamiento**: test de nivel inicial y ~57 ejercicios en 3 niveles sobre acordes, cambios, ritmo, escalas, técnica y teoría, con XP, niveles, racha diaria y logros.

## Stack

- Kotlin 2.0.21 (plugin Compose), JVM 17
- Jetpack Compose BOM 2024.12.01 + Material 3
- Navigation-Compose 2.8.5, Lifecycle 2.8.7
- Room 2.6.1 con KSP 2.0.21-1.0.28
- kotlinx.serialization 1.7.3, Coroutines 1.9.0
- security-crypto (token de sincronización cifrado), reorderable 2.4.2
- Gradle 8.7, Android Gradle Plugin 8.6.1

## Requisitos

- **minSdk** 26 (Android 8.0) · **target/compileSdk** 35 · JDK 17
- Permisos: `INTERNET`, `ACCESS_NETWORK_STATE`, `RECORD_AUDIO` (solo afinador y ejercicios con micrófono) y `REQUEST_INSTALL_PACKAGES` (auto-actualización).

## Compilación

```bash
./gradlew :app:assembleDebug        # APK de depuración
./gradlew testDebugUnitTest         # tests JVM
```

En Windows sin Gradle en el PATH:

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
```

### Versión de release y firma

Para distribuir actualizaciones hay que firmar **siempre con la misma clave**. Crea `keystore.properties` en la raíz (está en `.gitignore`):

```properties
storeFile=../vivace-release.jks
storePassword=…
keyAlias=vivace
keyPassword=…
```

Sin ese fichero, `assembleRelease` firma con la clave de depuración. Sube `versionCode` en cada publicación.

## Despliegue

`tools/deploy.ps1` (Windows PowerShell) prepara el entorno y publica. Sin parámetros solo **configura**: no publica nada por accidente.

```powershell
.\tools\deploy.ps1                 # comprueba/instala JDK 17+, Node LTS, SDK 35, deps del worker y sesión de Cloudflare
.\tools\deploy.ps1 -InitBackend    # solo la primera vez: bucket R2, base D1, schema.sql y secretos
.\tools\deploy.ps1 -Worker         # check + tests + wrangler deploy
.\tools\deploy.ps1 -App -Notes "Novedades"   # APK firmado + subida a R2 con su latest.json
.\tools\deploy.ps1 -All            # todo seguido
.\tools\deploy.ps1 -PublishCatalog # indexa en la base las partituras que ya estaban en R2
```

Detalles que conviene saber:

- **Pregunta antes de cada publicación**; `-Force` se las salta.
- **Se niega a publicar un APK firmado con la clave de depuración** (`-AllowDebugSigning` lo permite para pruebas): si la firma cambia, las actualizaciones no se instalan encima.
- Con `-BaseUrl` consulta `/update` y **aborta si el `versionCode` no sube**.
- `latest.json` se genera solo a partir de `versionCode`/`versionName` de `app/build.gradle.kts`.
- Los secretos se piden por `wrangler secret put`: no se escriben en el repositorio ni se muestran.

### El catálogo que ya estaba en R2

Las partituras anteriores al multiusuario viven en R2 pero **no están en la base de datos**, y la web lista desde la base: por eso el catálogo aparece vacío tras desplegar. `-PublishCatalog` pide la cuenta de administrador, inicia sesión y llama a `POST /admin/migrate`, que las indexa a su nombre **sin mover ni reescribir ningún fichero** (`songs.r2_key` sigue apuntando a la clave original). Es idempotente: repetirlo no duplica nada. La migración va **por tandas** (`?limit=`, por defecto 40, y `?cursor=`) porque un Worker tiene un tope de subpeticiones por invocación y un catálogo grande lo revienta; el script repite hasta que la respuesta trae `done: true`.

Con `-CatalogVisibility public` (por defecto) quedan visibles para cualquiera que entre en la web; con `private`, solo en «Mis partituras» del administrador.

**Si no tienes la cuenta de administrador** (`/admin/migrate` solo la acepta a ella), se arregla desde D1 con tu acceso de Cloudflare, sin contraseñas de nadie:

```powershell
.\tools\deploy.ps1 -ListUsers                              # quién existe y quién es admin
.\tools\deploy.ps1 -PromoteAdmin -AdminEmail tu@correo.com # date el rol a ti mismo
```

Ojo: el catálogo público es el del **administrador más antiguo**. Si promocionas tu cuenta y queda otro admin anterior, la portada seguirá mostrando la suya; degrada al viejo a `user` o migra con esa cuenta.

## Estructura

```
app/src/main/kotlin/com/guitarchords/app/
├── data/         Room (entidades, DAOs, migraciones), orden y filtros de listas
├── sync/         Cliente R2, sincronización de partituras y de acordes, reglas puras
├── update/       Comprobación/descarga/instalación de nuevas versiones
├── chords/       Diccionario, parser, transposición, reconocimiento, teoría
├── tuner/        Detección de tono (YIN)
├── metronome/    Motor de metrónomo
├── training/     Curriculum, ejercicios, gamificación (lógica pura)
├── print/        Impresión/PDF
└── ui/           Pantallas Compose por área + componentes compartidos
worker/           Cloudflare Worker (API R2 + panel de administración)
```

### Formato de partitura

Cabeceras `#clave: valor` (title, artist, genre, capo, url, favorite, locked, playlist), separador `---` y cuerpo con acordes entre llaves:

```
#title: Ejemplo
#artist: Autor
---
{Am} Casa   {C} árbol
```

Es el mismo formato en el dispositivo y en el bucket, así que los ficheros son legibles y editables desde el panel web.

## Web

La misma URL del Worker sirve **Vivace web** en `/`: catálogo publicado (visible sin cuenta), registro e inicio de sesión, tus partituras, y un **visor** con desplazamiento automático a velocidad regulable, metrónomo, transposición ±11 con bemoles, tamaño de letra y aviso de capo — las mismas herramientas que en el móvil. Desde el visor puedes editar y **publicar** tus partituras (privada ↔ pública).

El panel de administración con token compartido sigue disponible en **`/admin`**.

## Worker y API

El Worker de Cloudflare es a la vez almacén (R2 + D1), API multiusuario, web y panel de administración.

```bash
cd worker
npm install
npx wrangler deploy         # publica
npm run check               # sintaxis de los módulos y de las páginas servidas
npm test                    # tests: auth, permisos, API, librería web y acordes
```

### Puesta en marcha (una vez)

```bash
npx wrangler r2 bucket create guitarchords
npx wrangler d1 create vivace                       # pega el id en wrangler.toml
npx wrangler d1 execute vivace --remote --file=schema.sql
npx wrangler secret put AUTH_SECRET                 # clave para firmar sesiones
npx wrangler secret put SYNC_TOKEN                  # token heredado (app actual)
```

**El primer usuario que se registra queda como administrador.** Sus partituras públicas son el catálogo que ve cualquiera al entrar en la web.

Para indexar en la base de datos las partituras que ya estaban en R2 (quedan a tu nombre, sin mover ficheros):

```bash
curl -X POST "$URL/admin/migrate?visibility=public" -H "Authorization: Bearer $JWT"
```

### API

| Ruta | Acceso |
|---|---|
| `POST /auth/register`, `POST /auth/login` | pública |
| `GET /auth/me` | sesión |
| `GET /api/songs/public[?owner=<id>\|all]` | pública (catálogo del admin por defecto) |
| `GET /api/songs`, `POST /api/songs` | sesión |
| `GET /api/songs/:id` | pública si la partitura es pública; si no, dueño o admin |
| `PUT`/`DELETE /api/songs/:id` | dueño o admin |
| `GET /api/chords/global` | pública (los diagramas se ven sin cuenta) |
| `PUT /api/chords/global`, `POST /api/chords/global/seed` | solo admin |

Cada partitura es `private` o `public`; publicarla la hace visible para todos, pero editarla sigue siendo cosa de su dueño.

### Acordes

Los diagramas salen de un **diccionario global** guardado en R2 (`chords/global-chords.json`): uno solo para toda la instalación, que lee cualquiera —también sin cuenta, porque ver la digitación es parte de leer la partitura— y que **solo edita el administrador**, desde la pestaña «Acordes» de la web.

En el visor, el botón **♦ Acordes** despliega los diagramas de los acordes de esa partitura, y al pulsar uno se ven todas sus digitaciones. Si transpones, los diagramas cambian con la canción.

«Importar diccionario base» carga los 348 acordes de `src/chords-seed.js` (generado desde [chords-db](https://github.com/tombatossals/chords-db), MIT, el mismo origen que usa la app) **sin pisar** los que ya hayas definido a mano.

No confundir con `/api/chords`, que es el blob de acordes **personales** de cada usuario y sigue funcionando aparte.

### La app móvil usa la misma cuenta

En **Ajustes → Sincronización** se introduce la dirección del servidor y se entra con email y contraseña (o se crea la cuenta desde ahí). A partir de ese momento la app sincroniza **tus** partituras y tus acordes personalizados con la API, con el mismo comportamiento de siempre: la descarga es automática, la subida se confirma y los conflictos se resuelven canción a canción.

La primera sincronización tras iniciar sesión **reconoce las partituras que ya estaban subidas** con el token compartido (las casa por su clave en R2), así que no se duplica nada. Las rutas antiguas (`/list`, `/object`…) siguen activas de momento para no romper instalaciones sin actualizar.

Para publicar una actualización de la app: sube el APK firmado y un `latest.json` al bucket.

```bash
wrangler r2 object put guitarchords/app/app-release.apk --file=app/build/outputs/apk/release/app-release.apk
wrangler r2 object put guitarchords/app/latest.json --file=latest.json
```

```json
{ "versionCode": 2, "versionName": "1.1", "notes": "Novedades…", "apkUrl": "/update/apk" }
```

## Licencia

Pendiente de definir. Los datos de acordes provienen de [chords-db](https://github.com/tombatossals/chords-db) (MIT); su licencia se incluye en `app/src/main/assets/chords/`.
