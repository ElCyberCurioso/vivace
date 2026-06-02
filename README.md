# Vivace

Aplicación Android para gestionar listas de canciones con acordes, pensada para guitarristas que quieren tocar con letra y cifrado en pantalla. Incluye diccionario de acordes, buscador inverso sobre un mástil interactivo y afinador por micrófono.

Interfaz en español. UI 100% Jetpack Compose + Material 3, con soporte de color dinámico (Android 12+) y tema claro/oscuro automático según el sistema.

## Características

### Canciones y listas
- Listas de canciones (playlists) como contenedores.
- Vista de canción con letra y acordes alineados (fuente monoespaciada), tamaño de fuente ajustable y auto-scroll vertical a velocidad variable.
- Editor con inserción fácil de marcadores `[Acorde]` inline.
- Favoritos por canción (estrella).
- Imprimir/exportar a PDF: diagramas de acordes arriba + letra debajo, vía `PrintManager`.
- Compartir/importar playlists como archivo ZIP con extensión `.gtrlist`.

### Acordes
- Diccionario completo: 12 raíces × 15 calidades, con hasta 3 variaciones por acorde (abierto + barré shape E en 6ª cuerda + barré shape A en 5ª).
- Tap sobre cualquier acorde en la letra → modal con diagrama y navegación entre variaciones.
- **Buscador de acordes**: mástil interactivo donde el usuario marca trastes, cuerdas al aire (O) o silenciadas (X). La app identifica automáticamente el acorde (incluyendo slash chords cuando el bajo no coincide con la raíz). Cada punto muestra la nota que representa en notación inglesa.
- **Afinador**: detección de frecuencia en tiempo real por micrófono (algoritmo YIN), con aguja de ±50 cents, color según precisión (verde <5¢, ámbar <15¢, rojo el resto) y resalte de la cuerda más cercana en afinación estándar (E2 A2 D3 G3 B3 E4).

## Stack

- Kotlin 1.9.22, JVM 17
- Jetpack Compose (BOM 2024.02.00) + Material 3
- Navigation-Compose 2.7.7
- Room 2.6.1 con KSP 1.9.22-1.0.17
- kotlinx.serialization 1.6.3 (JSON para export)
- Coroutines 1.7.3
- Lifecycle 2.7.0 (incluye `lifecycle-runtime-compose`)
- `material-icons-extended`
- Gradle 8.7, Android Gradle Plugin 8.5.2

## Requisitos

- **minSdk**: 26 (Android 8.0)
- **targetSdk / compileSdk**: 34
- JDK 17
- Permiso `RECORD_AUDIO` (solo se solicita al entrar en el afinador)

## Estructura del proyecto

```
app/src/main/
├── AndroidManifest.xml
├── res/
│   ├── values/        (strings, themes claros)
│   ├── values-night/  (themes oscuros)
│   └── xml/           (file_paths para FileProvider)
└── kotlin/com/guitarchords/app/
    ├── GuitarChordsApp.kt          Application (repo lazy)
    ├── MainActivity.kt             Intent handling + import
    ├── data/
    │   ├── Entities.kt             Playlist, Song, *Export
    │   ├── Dao.kt                  PlaylistDao, SongDao
    │   ├── AppDatabase.kt          Room singleton
    │   ├── Repository.kt           CRUD + import/export
    │   └── ZipManager.kt           Build/parse .gtrlist
    ├── chords/
    │   ├── Chord.kt                ChordShape, Chord, Barre
    │   ├── ChordLibrary.kt         12 × 15 generador + diccionario abierto
    │   ├── ChordParser.kt          Parser de marcadores [Chord]
    │   ├── ChordDiagram.kt         Canvas del diagrama
    │   └── ChordRecognizer.kt      Inversa: frets[6] → nombre de acorde
    ├── tuner/
    │   ├── PitchDetector.kt        YIN (autocorrelación + CMND)
    │   └── TunerEngine.kt          AudioRecord + StateFlow
    ├── print/
    │   └── PrintAdapter.kt         PrintedPdfDocument
    └── ui/
        ├── theme/                  Material3, dynamic color
        ├── nav/                    NavGraph + rutas
        ├── components/             ChordModal
        ├── playlists/              Pantalla de listas
        ├── playlist/               Detalle playlist
        ├── song/                   Vista + editor
        ├── dictionary/             Diccionario filtrable
        ├── finder/                 Buscador inverso
        └── tuner/                  Afinador
```

## Modelo de datos

```
Playlist(id, name, createdAt)
Song(id, playlistId [FK CASCADE], title, artist, content, favorite, position)
```

Export JSON (`@Serializable`):

```
PlaylistExport { name, songs: [SongExport] }
SongExport     { title, artist, content, favorite, position }
```

### Formato `.gtrlist`

Archivo ZIP con:
- `playlist.json` — PlaylistExport serializado.
- `META-INF/format.txt` — versión del formato.

### Formato de letra

Marcadores inline `[Nombre]` dentro del texto:

```
[Am] Casa   [C] árbol
```

`ChordParser` extrae la posición de cada marcador para renderizar el diagrama alineado sobre la letra.

## Compilación

Antes de la primera compilación hay que generar el wrapper de Gradle (el binario `gradle-wrapper.jar` no se versiona en este snapshot):

**Opción A — Android Studio:** abrir la carpeta; el sync regenera el wrapper automáticamente.

**Opción B — línea de comandos** (con Gradle 8.7 instalado en el sistema):

```
gradle wrapper --gradle-version 8.7
```

Después:

```
./gradlew :app:assembleDebug
```

El APK resultante queda en `app/build/outputs/apk/debug/`.

## Intents soportados

`MainActivity` enlaza con el sistema para importar playlists desde otras apps:

- `ACTION_VIEW` sobre `content://` o `file://` con mimeType `application/zip` o ruta `*.gtrlist`.
- `ACTION_SEND` con `EXTRA_STREAM`.

El import corre en `Dispatchers.IO` y navega al detalle de la playlist importada.

## Licencia

Pendiente de definir.
