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

### Sincronización (Cloudflare Worker + D1 + R2)
- **Automática y sin botones.** Lo que se edita se guarda siempre en el móvil; en cuanto hay conexión sube solo, aunque la app esté cerrada (WorkManager). Sin cobertura, los cambios esperan en el dispositivo.
- **Diferencial de verdad**: la app pide «qué ha cambiado desde la última vez» y recibe solo eso, con el texto incluido, en una llamada por tanda.
- **Los borrados viajan**: lo que se borra en un sitio desaparece en el otro y no vuelve a aparecer.
- **Conflictos sin preguntas**: si la misma partitura cambió en dos sitios, gana la del servidor y tu copia se guarda como versión alternativa. Nunca se pierde trabajo.
- **Carpetas, favoritos y orden** son datos de la cuenta: se ven y se editan igual desde el móvil y desde la web.
- Los **acordes personalizados** se sincronizan aparte, de forma automática y convergente.
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
- Base de datos Room en la **versión 17**. El esquema de cada versión se exporta a `app/schemas/` y se sube al repositorio: es la referencia contra la que comprobar que una migración deja la base igual que una instalación nueva.
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

Hay dos guiones equivalentes: `tools/deploy.sh` (Linux/macOS) y `tools/deploy.ps1`
(Windows PowerShell, que además prepara la cadena de compilación de Android y
firma el APK). Ninguno de los dos publica nada sin preguntar.

### Desde Linux o macOS

```bash
./tools/deploy.sh preflight        # comprueba TODO sin tocar nada
./tools/deploy.sh backend          # solo la primera vez: bucket R2, base D1 y AUTH_SECRET
./tools/deploy.sh release          # el despliegue completo del servidor
./tools/deploy.sh catalog --url https://…   # indexa lo que ya estaba en R2 (una vez)
./tools/deploy.sh app app-release.apk --url https://…   # publica una versión de la app
./tools/deploy.sh verify --url https://…    # comprueba lo que hay publicado
./tools/deploy.sh rollback         # vuelve a la versión anterior del Worker
```

`release` es el camino normal y hace, **en este orden**: comprobaciones previas
(sesión de Cloudflare, que el `database_id` exista de verdad, que `AUTH_SECRET`
esté puesto), `npm run check`, los tests, `schema.sql` + `migrations.sql`,
`wrangler deploy` y una verificación de lo publicado.

**El orden no es negociable**: el esquema va antes que el código. La API nueva
cuenta con columnas que la base vieja no tiene (`rev`, `favorite`, `position`,
`playlist_id`); publicar primero el Worker deja la web dando errores hasta que
llegue el `ALTER`. Por eso las dos cosas están dentro del mismo comando.

La verificación no se fía de que `wrangler deploy` haya dicho que sí: comprueba
que responden la web, los estáticos y el catálogo, que `/api/sync/changes`
contesta 401 (existe: si diera 404 seguiría el código viejo), que `/list` da 404
(las rutas heredadas se han ido de verdad), que un login falso da 401 y no 503
(o sea, que `AUTH_SECRET` está puesto) y que revalidar un estático con su `ETag`
devuelve 304.

### Desde Windows

```powershell
.\tools\deploy.ps1                 # comprueba/instala JDK 17+, Node LTS, SDK 35, deps del worker y sesión de Cloudflare
.\tools\deploy.ps1 -InitBackend    # solo la primera vez: bucket R2, base D1, schema.sql y secretos
.\tools\deploy.ps1 -Worker         # check + tests + wrangler deploy
.\tools\deploy.ps1 -App -Notes "Novedades"   # APK firmado + subida a R2 con su latest.json
.\tools\deploy.ps1 -All            # todo seguido
.\tools\deploy.ps1 -PublishCatalog # indexa en la base las partituras que ya estaban en R2
.\tools\deploy.ps1 -Categorize     # propone una categoria para cada partitura y la aplica
```

Detalles que conviene saber:

- **Pregunta antes de cada publicación**; `-Force` se las salta (y `--yes` en el de bash).
- **`-Worker` aplica el esquema él mismo** antes de publicar, y aborta si se cancela: así ninguna combinación de parámetros puede dejar el código nuevo contra una base vieja.
- **Las migraciones se aplican una a una y se comprueban después.** `migrations.sql` son `ALTER TABLE`, y D1 aborta el fichero entero al primer error: como el primer `ALTER` ya está aplicado desde la tanda anterior, pasarle el fichero completo hacía que los de abajo no llegaran a ejecutarse. Después de migrar, los guiones preguntan a la base si las columnas están de verdad y se niegan a publicar si falta alguna.
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

### Diccionario de acordes

El Worker trae **8.669 acordes** (~14.400 nombres contando los alias en
bemoles, y hasta 4 digitaciones cada uno) importados de
[guitar-chords-db-json](https://github.com/szaza/guitar-chords-db-json) (MIT).
El administrador los vuelca al diccionario global con **Importar diccionario
base**; el volcado solo AÑADE lo que falta, así que nunca pisa un acorde
editado a mano ni la semilla curada de 348 que ya venía.

El repositorio de origen ocupa 36 MB en 9.072 ficheros: eso ni cabe en el bundle
del Worker ni tiene sentido mandarlo al navegador. `worker/tools/generar-chords-db.mjs`
lo reduce a una cadena compacta (`src/chords-db.js`, ~570 KB, 92 KB del bundle
comprimido) y solo se expande cuando el administrador siembra.

También convierte el formato: el origen da los trastes **absolutos** en base 36
(`"8aa988"` = 8,10,10,9,8,8) y el diccionario de Accordio los quiere
**relativos** al traste base, que es la convención de chords-db y la que ya usa
la app. Ahí salió un fallo de fondo: el dibujante de la web trataba esos números
como absolutos, así que **todo acorde con traste base mayor que 1** —cejillas y
posiciones altas, media biblioteca— salía con los puntos corridos o sin ellos.
Ya está arreglado y cubierto por tests.

### La app Android viste lo mismo

La app usa el **mismo paquete de marca** que la web, no una interpretación:

- **Paleta**, en `ui/theme/Theme.kt`: los valores salen de `tokens.css` y
  `tokens.dark.css`, y los roles de Material 3 cuelgan de los roles del kit
  (`action`, `highlight`, `active`, `pending`). Lo que Material pide y el kit no
  define —contenedores, escalones de superficie— se deriva y va marcado como
  derivado. Los roles del kit que Material no tiene (`pending`, `chord`, `nav`)
  entran por `ExtendedColors`.
- **Color dinámico apagado.** El de Android 12+ pinta la app con el fondo de
  pantalla del móvil, y entonces deja de ser Accordio: la marca es justo lo que
  se quiere reconocer entre la web y el móvil.
- **Tipografías**: Montserrat para titulares y Poppins para el texto,
  empaquetadas (OFL, ver `app/licenses/`). JetBrains Mono se queda para la hoja
  de partitura y las cifras, que es la misma concesión que en la web: sin ancho
  fijo los acordes dejan de caer sobre su sílaba.
- **Formas**: los radios del kit (8/12/20/28) en `ui/theme/Shape.kt`.
- **Barra superior** teal maciza con texto crema, como la web, vía
  `accordioTopBarColors()` — un sitio y no las dos docenas de pantallas que la
  usan. Los iconos de la barra de estado van siempre claros: encima está el teal
  en los dos temas.
- **Iconos**: los 23 del kit, en `ui/icons/AccordioIcons.kt`. No son ficheros
  sino trazos que se montan con los colores del tema, porque los iconos del kit
  tienen **dos tonos** —trazo y detalle coral— y ese coral cambia entre claro y
  oscuro: un vector drawable necesitaría dos copias de cada uno, y el `tint` de
  Compose aplana el icono entero a un color. Donde el kit no tiene equivalente
  (atrás, borrar, cerrar, nube, reloj…) se conserva el icono de Material: son
  símbolos de sistema, y fabricarlos a mano sería inventar marca.
- **Diagramas de acordes** trazados en el color de marca (regla 7), y el número
  del dedo en el color de la tarjeta: en oscuro el punto es turquesa claro y el
  blanco fijo de antes casi no se distinguía.
- **Icono y splash** con la marca del mástil del kit sobre el teal.

Dos contrastes se subieron respecto al kit, y en los dos casos sube el **rol**,
no el token de marca: el texto secundario sobre tarjeta clara (`#7B8E92` da
3,2:1) y sobre tarjeta oscura (`#8CA6AA`, 3,6:1). Es la misma corrección que
lleva la web.

La app **pasa a llamarse Accordio** en lo visible (nombre del lanzador,
cabecera). El `applicationId` sigue siendo `com.guitarchords.app`: cambiarlo
desinstalaría la app de todo el mundo y rompería las actualizaciones. Por dentro
quedan nombres antiguos (`VivaceClient`, `GuitarChordsTheme`) que no ve nadie.

### Estilo: paquete de marca Accordio

La web viste el kit `accordio-brand-kit.zip`: teal `#1A535C` de marca, coral
para destacar, turquesa para lo seleccionado y amarillo **solo** como estado
(capo, estrellas, propuestas pendientes), sobre crema `#F7EFE3` con el mosaico
de notas de fondo. Titulares en Montserrat, texto en Poppins, controles en
píldora y tarjetas de 20 px.

Cómo está montado: los tokens `--ac-*` del kit son la única fuente de color y
tamaño, y encima va una capa semántica `--vv-*` (`--vv-accent` = acción,
`--vv-active` = seleccionado, `--vv-state` = pendiente/valoración,
`--vv-chord` = acordes) que es la que piden los componentes. Así se cambia el
papel de un color en un sitio y no en cuarenta reglas sueltas.

**Modo claro y modo oscuro son los del kit**, no una interpretación: fondo
`#F7EFE3` / `#0F2429`, tarjetas `#F2FAF6` / `#163A41`, barra `#1A535C` /
`#0B1D21`. La marca no se invierte; lo que cambia es el papel de cada color: en
oscuro el teal no contrasta contra el fondo y pasa a titular, la acción la toma
el turquesa (`#4ECDC4` sobre `#08262B`), y coral y amarillo suben un paso de
rampa. Cada tema trae además su mosaico de notas y su favicon; los recursos con
el color metido dentro del SVG los cambia `recursosDeTema()` al conmutar, junto
con el `theme-color` de la barra del navegador.

Del kit no se coge una cosa, a propósito: **JetBrains Mono** para la hoja, las
cifras y los diagramas. No trae monoespaciada, y sin ella los acordes dejan de
caer sobre su sílaba.

Los botones secundarios tienen **superficie**, no solo contorno: sobre el crema
con mosaico, un borde pálido y letra fina no se leían como algo pulsable
(«Cargar más» se perdía en el fondo). «Cargar más» va relleno con el color de
acción y centrado; las pestañas, con superficie y letra de marca, y la activa en
turquesa. En la barra teal los botones siguen siendo de contorno: ahí el fondo ya
es macizo.

**El color reparte papeles, no decora.** Teal es la acción, turquesa lo activo o
publicado, coral lo que llama (crear, favoritas, compartir, los acordes de la
hoja) y amarillo solo estado: capo, pulso del metrónomo, estrellas y propuestas
pendientes. Cada herramienta del visor lleva el color de su papel —turquesa lo
que corre, amarillo el pulso, coral los acordes, teal el tono— con los iconos
del kit (`icons/sprite.svg`, embebidos como `<symbol>`). Dos variables lo
gobiernan: `--tono` es el relleno cuando la herramienta está activa y
`--tono-glifo` el color del icono sobre la superficie clara; hacen falta las dos
porque el turquesa, el amarillo y el coral de marca son rellenos y como trazo
sobre crema se quedan en 2:1.

En oscuro, el gris secundario del kit (`#8CA6AA`) se queda en 3,6:1 sobre la
tarjeta, así que el **rol** de texto secundario sube a `#A9C3C6`; los tokens de
marca no se tocan.

En `worker/brand/` está la copia de lo que se ha tomado del kit, con el detalle
de qué se ha usado y qué no.


La misma URL del Worker sirve **Accordio** en `/`: catálogo publicado (visible sin cuenta), registro e inicio de sesión, tus partituras, y un **visor** con desplazamiento automático a velocidad regulable, metrónomo, transposición ±11 con bemoles, tamaño de letra, aviso de capo e **impresión/PDF** como documento: A4 con la marca, el título y
el artista en la cabecera, el número de hoja en el pie, y el capo y el tono
transpuesto en la píldora amarilla de estado — las mismas herramientas que en el móvil.

En **Mis partituras** hay dos filtros rápidos además de los desplegables:
**Favoritas** y **Solo privadas**. Este último **viene puesto**: ahí lo que se
va a hacer es rematar lo que falta, y lo ya publicado es justo lo que no necesita
atención. Al apagarlo aparece el repertorio entero.

Compartir abre una **ventana con el enlace** de la partitura, seleccionado y con
un botón que lo copia. Antes se llamaba a `navigator.share` o se copiaba en
silencio: en el escritorio no hay menú del sistema, y una copia sin ventana no
se distingue de un botón que no hace nada.

En **móvil** la cabecera del visor cabe en **una línea de 49 px** —atrás, título
y un botón de acciones— frente a los ~175 px que ocupaba apilada en tres filas;
compartir, PDF, editar y el enlace al original viven ahora en ese menú. Y la
partitura **entra de ancho al abrirla**: se mide la línea más larga de la canción
—y el ancho real de un carácter de la monoespaciada, no uno estimado— y se elige
el mayor tamaño de letra que quepa, hasta 18 px. El suelo son **13 px, y es de
legibilidad, no de encaje**: por debajo la letra no se lee de un vistazo con la
guitarra en las manos, así que una canción de columnas exageradas se queda a 13 y
se arrastra de lado. Es blando por un punto: si entra entera a 12, se le concede
—salirse por cinco píxeles obliga a arrastrar la línea completa para leer una
sílaba—. Partir la línea no es opción: descolocaría los acordes de sus sílabas.

**A-/A+ mandan después**, de píxel en píxel y hasta 8 px. Antes el paso era de 2
y el mínimo 11, que chocaba con el suelo del ajuste: la letra ya salía por debajo
del mínimo y el botón de reducir no hacía nada.

El **scroll automático** va de 2 a 100 px/s y arranca en 20. Llegaba a 300, que
no lo usa ninguna partitura y dejaba el rango útil —los primeros 40— apelotonado
en un centímetro de barra.

Los mandos —scroll, tono,
letra, acordes, metrónomo— viven en un **panel que sube desde abajo**, con velo,
asa y botón de cerrar; lo abre un botón anclado abajo a la derecha, donde llega
el pulgar. Ocupaban media pantalla y dejaban la partitura bajo el pliegue, que
es lo que se ha venido a leer. Las **versiones** salen del panel y se leen **al
final de la partitura**, que es donde uno decide «pruebo esta otra». Es el mismo
nodo movido según el ancho, no una copia: con dos listas habría dos cosas que
mantener. En pantalla ancha todo sigue en su columna, sin botón ni panel.

Al pie de cada partitura hay **recomendadas**: si el mismo artista tiene más en
el catálogo, salen esas («Más de David Bisbal»); si no, otras del mismo estilo
(«Más flamenco»). Lo decide el servidor y por prioridad, no mezclando: son dos
consultas, y la del estilo solo entra cuando la del artista no da nada. Solo se
recomienda lo publicado —recomendar algo que quien lo lee no puede abrir es
enseñar un 404— y nunca la partitura que se está viendo. Se abren en el mismo
visor, sin pasar por el catálogo.

Al entrar, la portada es siempre el **catálogo**, se tenga sesión o no: lo
primero es ver lo publicado, y el repertorio propio está a un clic en su
pestaña.

**Las páginas las reparte la web, no el navegador.** Se probaron antes las dos
vías que suelen recomendarse y ninguna sirve en Chrome: un elemento
`position:fixed` no se repite en cada hoja —se pinta una vez y donde caiga— y
`counter(page)` fuera de un margen con nombre devuelve `0`; los márgenes con
nombre de `@page` (`@bottom-center` y compañía), que resolverían esto en CSS, no
están implementados. Así que el documento se corta midiendo: se van metiendo
líneas en la hoja mientras quepan y, cuando una se sale, empieza otra. Una línea
de acordes y su letra viajan juntas: separarlas deja los acordes sin canción y
la canción sin acordes.

El **papel** tiene tres reglas propias, y las tres son a conciencia: sale
**siempre en claro** aunque se esté leyendo en modo oscuro —un PDF con fondo
teal es un cartucho de tinta y una fotocopia ilegible—, **sin el mosaico** de
notas, que en pantalla es identidad pero debajo de una letra que se lee tocando
es ruido, y con `print-color-adjust: exact` en todo lo que lleva color, porque
al imprimir el navegador descarta fondos y bordes salvo que se le diga. Antes de
paginar se espera a que carguen las tipografías: si se midiera con la de
reserva, el corte caería donde no toca.

**Buscar es cosa del servidor.** Con 3 letras o cifras el buscador consulta el
catálogo entero por título y artista, sin distinguir mayúsculas ni tildes
(«buleria» encuentra «Bulería»), y devuelve todos los resultados de una vez.
Antes solo filtraba lo que ya estaba descargado —la primera página de 60—, así
que una partitura que cayera más atrás no aparecía hasta pulsar «Cargar más» las
veces que hiciera falta. Con menos de 3 caracteres se recorta lo ya cargado y se
avisa de que hay que escribir un poco más.

Desde la web se gestiona el repertorio propio igual que desde el móvil: **listas (carpetas)** con su filtro, **favoritas**, **papelera** con restaurar y borrar definitivamente, y el editor con visibilidad, candado, categoría, capo, vídeo y lista.

Hay **conmutador de tema** claro/oscuro en la cabecera (se recuerda en el navegador; sin elección manda el del sistema).

Los **avisos y las confirmaciones son de la propia web**, no del navegador: al compartir una partitura, al borrar o al pedir un texto sale un diálogo con la tipografía y los colores de Vivace (y respeta el tema claro/oscuro), más un aviso flotante que se va solo. `alert`, `confirm` y `prompt` desentonaban y, encima, congelaban la página mientras estaban abiertos: con el metrónomo sonando o una restauración en marcha se notaba.

El **editor** son tres columnas: escribir, ver cómo queda y la ficha. Los campos
—título, artista, categoría, lista, visibilidad, capo, enlaces— iban antes
apilados arriba y empujaban el editor y la vista previa fuera de la pantalla,
cuando escribir la partitura es el grueso del trabajo y el título se toca una
vez. Ahora la hoja se lleva el alto de la ventana y la ficha va al lado, en su
columna, con los botones al pie. Las tres se alinean porque comparten
estructura —rótulo de alto fijo y debajo una caja del mismo alto, gobernada por
`--alto-editor`— y no por coincidencia de medidas: la ficha llevaba el fondo en
el propio panel, rótulo incluido, así que su caja arrancaba donde las otras dos
tenían el suyo, 38 px más arriba.

La ficha **hace su propio scroll**: sigue siendo columna mientras quepa (hasta
1000 px) y solo por debajo baja a lo ancho. Antes se apilaba ya en 1200, y
entonces para tocar la categoría o el capo había que recorrer la página entera
—en un portátil de 1280, o con la ventana sin maximizar, ese era el caso
normal—. Con la ventana baja los botones se ponen en fila: en columna se comían
156 px de los 390 disponibles y la ficha se veía por una rendija.

El editor trae **detección automática de acordes** (marca las líneas que solo llevan acordes envolviéndolos en `{X}`), **capo por botones** de 0 a 12 y un campo para la **URL de la partitura original**, que aparece como enlace en el visor.

Los editores y administradores tienen además una pestaña **Administración**: copia de seguridad y restauración en ZIP (se hace entera en el navegador), repaso de partituras sin vídeo y categorías automáticas.

Solo el **administrador** ve ahí el interruptor de **altas de cuenta**. Con las
altas cerradas nadie puede registrarse: la web deja de ofrecer «crear una
cuenta» y, sobre todo, `POST /auth/register` responde `403` — esconder un botón
no impide un `curl`. Quien ya tiene cuenta sigue entrando. La única excepción es
la instalación vacía: el primer usuario puede darse de alta aunque estén
cerradas, porque si no, cerrarlas antes de que exista administrador dejaría la
instalación sin dueño y sin forma de volver a abrirlas. El estado vive en la
tabla `settings` de D1.

> El antiguo panel `/admin` con token compartido **se ha retirado**, junto con las rutas `/list`, `/object`, `/bodies` y `/delete`. Se saltaban el modelo de permisos: con un único token se leía, sobrescribía y borraba el texto de cualquier partitura, incluidas las privadas de otras cuentas. Lo que valía la pena de aquel panel vive ahora en la pestaña Administración, bajo la sesión y los roles.

El CSS y el JavaScript se sirven aparte (`/static/vivace.css` y `/static/vivace-app.js`) con `ETag` y caché: antes iban dentro del HTML y se volvían a descargar ~90 KB en cada visita.

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
npx wrangler d1 execute vivace --remote --file=migrations.sql
npx wrangler secret put AUTH_SECRET                 # clave para firmar sesiones
```

**`AUTH_SECRET` es obligatorio.** Si falta, la API responde `503` y no emite ni
acepta ninguna sesión. Antes se conformaba con `SYNC_TOKEN` como respaldo, y ese
token iba en todas las instalaciones antiguas de la app: quien lo tuviera podía
firmarse una sesión con la identidad de cualquiera, administrador incluido.

Todo esto lo hace de una vez `./tools/deploy.sh backend` (ver **Despliegue**).

**El primer usuario que se registra queda como administrador.** Sus partituras públicas son el catálogo que ve cualquiera al entrar en la web.

Para indexar en la base de datos las partituras que ya estaban en R2 (quedan a tu nombre, sin mover ficheros):

```bash
curl -X POST "$URL/admin/migrate?visibility=public" -H "Authorization: Bearer $JWT"
```

### Dominio propio (`accordio.site`)

La web y la API son el mismo Worker, así que basta con apuntarle el dominio. En
`worker/wrangler.toml` están declarados como **custom domains**:

```toml
[[routes]]
pattern = "accordio.site"
custom_domain = true

[[routes]]
pattern = "www.accordio.site"
custom_domain = true
```

Requisito previo (una vez, en el panel de Cloudflare): tener la zona
`accordio.site` dada de alta en la **misma cuenta** que el Worker, con los
*nameservers* del registrador apuntando a Cloudflare y la zona en estado
**Active**. Con eso hecho, `npx wrangler deploy` crea solo el registro DNS y el
certificado TLS; no hay que añadir ningún CNAME a mano.

`www` **redirige al apex con un 301** (conservando ruta y query). Un solo host
canónico no es un capricho: la sesión de la web vive en el almacenamiento local
del navegador, y con dos orígenes entrar por uno u otro daría dos sesiones
distintas.

**HTTP también redirige a HTTPS**, y en el mismo salto (301 en las lecturas, 308
en lo demás: un 301 sobre un POST se reenvía como GET y perdería el cuerpo). Un dominio propio de
Cloudflare atiende igualmente el puerto 80, y allí la petición viaja en claro:
quien comparta red ve el token de sesión de la cabecera `Authorization`. Además,
toda respuesta por HTTPS lleva `Strict-Transport-Security: max-age=31536000;
includeSubDomains`, así que tras la primera visita el navegador ya no vuelve a
pedir el sitio en claro (ese primer salto sí viaja sin cifrar; por eso conviene
lo de abajo). Va sin `preload`: obliga a que todo subdominio futuro hable HTTPS y
salirse de la lista lleva meses.

En `localhost` no se hace ninguna de las dos cosas: `wrangler dev` sirve por HTTP
y un HSTS ahí dejaría el navegador convencido de que `localhost` es siempre
HTTPS, rompiendo cualquier otro proyecto en el mismo puerto.

Conviene además activarlo **en la zona**, que corta en el borde sin llegar a
ejecutar el Worker: *SSL/TLS → Edge Certificates → **Always Use HTTPS*** (y
**Minimum TLS Version** 1.2). Con un token de API que tenga `Zone Settings:Edit`:

```bash
curl -X PATCH "https://api.cloudflare.com/client/v4/zones/$ZONE_ID/settings/always_use_https" \
  -H "Authorization: Bearer $CF_API_TOKEN" -H "Content-Type: application/json" \
  --data '{"value":"on"}'
```

La dirección `*.workers.dev` sigue funcionando, así que las instalaciones
antiguas de la app no se quedan colgadas. Las nuevas ya vienen apuntando a
`https://accordio.site` de fábrica (`SyncPrefs` y `UPDATE_BASE_URL`).

### API

| Ruta | Acceso |
|---|---|
| `POST /auth/register`, `POST /auth/login` | pública |
| `GET /auth/me` | sesión |
| `GET /api/songs/public[?owner=<id>\|all]` | pública (catálogo del admin por defecto) |
| `GET /api/songs/:id/related` | como la partitura de origen (devuelve solo catálogo público) |
| `GET /api/settings` | pública (hoy solo `registrationOpen`) |
| `PUT /api/settings` | **solo administrador** |
| `GET /api/songs`, `POST /api/songs` | sesión |
| `GET /api/songs/:id` | pública si la partitura es pública; si no, dueño o admin |
| `PUT`/`DELETE /api/songs/:id` | dueño, editor si es pública, o admin |
| `GET /api/chords/global` | pública (los diagramas se ven sin cuenta) |
| `PUT /api/chords/global`, `POST /api/chords/global/seed` | editor o admin |
| `GET /api/genres` | pública (categorías con partituras publicadas) |
| `POST /api/genres/auto` | editor o admin |
| `GET /api/songs/without-video` | editor o admin |
| `GET`/`POST /api/songs/:id/versions` | ver: como la partitura · crear: dueño o editor |
| `GET`/`PUT`/`DELETE /api/versions/:id` | ver: como la partitura · tocar: dueño o editor |
| `POST /api/songs/:id/proposals` | sesión (publicar lo propio, versionar lo público) |
| `GET /api/proposals` | sesión (las tuyas; los editores ven la cola) |
| `POST /api/proposals/:id/approve`, `/reject` | editor o admin |
| `DELETE /api/proposals/:id` | su autor mientras siga pendiente, o un editor |
| `GET /api/users`, `PUT /api/users/:id/role` | solo admin |
| `GET`/`POST /api/songs/:id/comments` | leer: como la partitura · escribir: sesión |
| `DELETE /api/comments/:id` | su autor o un editor |
| `GET`/`PUT /api/songs/:id/ratings` | leer: como la partitura · votar: sesión |
| `GET /api/sync/changes` | sesión (lo que cambió desde tu cursor, texto incluido) |
| `POST /api/sync/push` | sesión (sube un lote; devuelve conflicto por elemento) |
| `GET`/`POST /api/playlists`, `PUT`/`DELETE /api/playlists/:id` | sesión (listas propias) |
| `PUT /api/songs/:id/favorite` | dueño |
| `GET /api/songs?trash=1`, `POST /api/songs/:id/restore` | dueño (papelera) |
| `DELETE /api/songs/:id?hard=1` | dueño, y solo desde la papelera |

Los tres listados de partituras (`/api/songs/public`, `/api/songs` y `/api/songs?trash=1`) aceptan `?limit=` (tope 500), `?offset=` y `?q=`. `q` busca en título y artista sin distinguir mayúsculas ni tildes, y escapa los comodines de `LIKE`: buscar `%` no devuelve el catálogo entero.

Cada partitura es `private` o `public`. **Publicar es un acto editorial**: quien la sube no marca la casilla, la propone.

Los listados vienen **por páginas** (`?limit=`, `?offset=`; 100 por defecto, 500
como máximo) y responden con `hasMore`. Antes no tenían tope y una cuenta grande
se traía la colección entera en cada llamada.

### Sincronización por lotes

`GET /api/sync/changes` acepta un cursor por flujo (`playlists`, `songs`,
`versions`), con el formato opaco `<updated_at>:<id>`, y devuelve lo que haya
cambiado después —**lápidas incluidas**, para que un borrado llegue a los demás
dispositivos— con el texto de cada partitura ya incrustado. Antes hacía falta
listar todo y pedir cada canción por separado, y de los borrados no se enteraba
nadie.

`POST /api/sync/push` sube listas, partituras y versiones en una sola llamada.
Cada partitura viaja con `baseRev`, la revisión que el cliente creía tener: si no
coincide con la del servidor, ese elemento vuelve marcado como `conflict` con la
copia del servidor adjunta, en vez de pisar el trabajo ajeno. `songs.rev` lo
incrementa el servidor en cada escritura; `updated_at` no servía para esto,
porque lo pisa cualquiera y dos relojes distintos no se pueden comparar.

### Roles

| Rol | Qué puede hacer |
|---|---|
| `user` | Crear partituras suyas (nacen privadas), proponer su publicación y proponer versiones de lo ya publicado |
| `editor` | Todo lo anterior, más editar y despublicar partituras **públicas**, resolver propuestas y mantener el diccionario de acordes |
| `admin` | Todo, incluidas las partituras privadas ajenas y el reparto de roles |

Un editor **no** toca las partituras privadas de nadie: manda sobre el catálogo, no sobre el cajón de cada uno. Los roles se reparten desde la pestaña «Usuarios», o sin entrar en la web:

```powershell
.\tools\deploy.ps1 -PromoteAdmin -AdminEmail alguien@correo.com -Role editor
```

### Versiones y propuestas

Una **versión** es un arreglo alternativo de una partitura —otro tono, otra cejilla, tablatura—; el contenido propio de la partitura es el «Original» y las versiones cuelgan de él, igual que en la app. Salen listadas en el visor y se cambia de una a otra sin perder tono ni tamaño de letra.

Quien puede editar la partitura añade versiones directamente. El resto **propone**, y la propuesta espera en la cola de revisión sin tocar nada. Al aprobarla, la versión se crea **a nombre de quien la propuso**, no del revisor.

Las propuestas viven en la pestaña «Propuestas»: cola de pendientes para editores, historial propio para todos los demás. Rechazar pide un motivo, que ve el autor.
### Categorías

Cada partitura tiene una categoría (`songs.genre`), y el catálogo filtra por ella. `-Categorize` propone una para las que no la tienen, mirando primero el **artista** y luego palabras del **título**; lo que no encaja va a «Varios». Las reglas están en [`worker/src/genres.js`](worker/src/genres.js), a la vista para corregirlas, y cualquier categoría se cambia luego a mano desde el editor.

Adivinar el estilo por el título y el artista es aproximado por definición: el comando enseña primero el recuento de lo que haría y pregunta antes de escribir. Con `-Overwrite` repasa también las que ya tienen categoría.
### Vídeo de la canción

Cada partitura puede llevar un enlace de YouTube (`songs.youtube_url`), que se pega en el editor y sale como reproductor **a la derecha de la partitura**. Se aceptan las formas habituales (`watch?v=`, `youtu.be/`, `shorts/`, `embed/`, o el id suelto) y el editor avisa al momento si el enlace no se reconoce; el botón «Buscar en YouTube» abre la búsqueda con el artista y el título ya puestos.

Se incrusta desde `youtube-nocookie.com` y el `iframe` **solo se crea si hay vídeo**, así que una partitura sin él no carga nada de YouTube. Al cerrar el visor el marco se vacía para que la música pare.

`GET /api/songs/without-video` (editor) lista las que aún no tienen ninguno, con su enlace de búsqueda ya montado, para ir completándolas.
### Comentarios, estrellas y candado

Al final de cada partitura hay un **hilo de comentarios**: escribe quien tenga sesión y pueda verla, y borra el autor o un editor. Cada **versión se valora por separado** con estrellas de 0 a 5; volver a pulsar la estrella que ya tenías marcada retira el voto. Las medias las ve cualquiera, votar pide cuenta.

El visor tiene una **columna a la izquierda** con dos recuadros: arriba los mandos —scroll, tono, tamaño, metrónomo, acordes— y debajo el listado de versiones. Por debajo de 900 px se apilan encima de la partitura. El recuadro de versiones sale siempre, aunque solo esté el Original, con su número y su puntuación.

El **candado** (`locked`) es un seguro contra el despiste, no un permiso: se pone desde el editor y hace que editar pida confirmación. No se enseña en el catálogo —a quien lee no le dice nada—, solo en «Mis partituras» y en el editor.


Después de desplegar esto por primera vez hay que crear las tablas nuevas (todo el esquema es `CREATE ... IF NOT EXISTS`, repetirlo no rompe nada):

```powershell
.\tools\deploy.ps1 -ApplySchema
```

### Acordes

Los diagramas salen de un **diccionario global** guardado en R2 (`chords/global-chords.json`): uno solo para toda la instalación, que lee cualquiera —también sin cuenta, porque ver la digitación es parte de leer la partitura— y que **solo edita el administrador**, desde la pestaña «Acordes» de la web.

En el visor, el botón **♦ Acordes** despliega los diagramas de los acordes de esa partitura. Además, cada acorde de la letra es interactivo: al **pasar el ratón** sale un globo con sus digitaciones y al **pulsarlo** se abren todas en grande. Si transpones, los diagramas cambian con la canción.

«Importar diccionario base» carga los 348 acordes de `src/chords-seed.js` (generado desde [chords-db](https://github.com/tombatossals/chords-db), MIT, el mismo origen que usa la app) **sin pisar** los que ya hayas definido a mano.

No confundir con `/api/chords`, que es el blob de acordes **personales** de cada usuario y sigue funcionando aparte.

### La app móvil usa la misma cuenta

En **Ajustes → Sincronización** se entra con email y contraseña (o se crea la cuenta desde ahí). La dirección del servidor viene puesta de fábrica (`https://accordio.site`) y solo hay que tocarla si el Worker está desplegado en otra cuenta. A partir de ahí no hay que volver a esa pantalla: la sincronización es **automática**.

Cómo funciona:

- Todo cambio se guarda **primero en el móvil** y queda marcado como pendiente. Eso incluye lo que antes no se subía nunca: mover una partitura de carpeta, marcarla favorita, reordenarla o mandarla a la papelera.
- Una tarea de **WorkManager** sube lo pendiente en cuanto hay red. Como la petición queda guardada en disco, sobrevive a cerrar la app y a reiniciar el móvil; sin cobertura, simplemente espera.
- Los disparadores son: cada cambio local (agrupado unos segundos), la reconexión a la red, el arranque de la app y un repaso cada seis horas.
- **Los borrados se propagan.** Antes, borrar definitivamente una partitura ya sincronizada la dejaba viva en el servidor y volvía a bajarse como nueva en la siguiente sincronización; ahora queda una lápida en cola hasta que el servidor la confirma.
- **Los conflictos no preguntan.** Si la misma partitura cambió aquí y en el servidor, se queda la del servidor como Original y la copia local se guarda como una versión más, llamada «Conflicto · fecha». La pantalla de sincronización lo avisa después.

Esa pantalla pasa a ser informativa: última pasada, cuántos cambios quedan por subir, errores y un «sincronizar ahora» como atajo.

La primera sincronización tras iniciar sesión **reconoce las partituras que ya estaban subidas** con el token compartido (las casa por su clave en R2), así que no se duplica nada.

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
