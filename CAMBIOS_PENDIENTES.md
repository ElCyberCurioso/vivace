# Cambios pendientes de subir a GitHub

Rama: `master` · Generado: 2026-06-02

Resumen de todo lo no commiteado: **30 ficheros modificados** + **22 nuevos**.
Agrupado por funcionalidad.

---

## 1. Worker de Cloudflare (R2 sync + panel de administración)
Nuevo directorio `worker/` — Worker que hace de frente al bucket R2.

- `worker/src/index.js` *(nuevo)* — API (`/list`, `/object` GET/PUT/DELETE) + **interfaz web de administración** servida en `GET /`:
  - Login por token (Bearer, guardado en localStorage), con timeout y errores explícitos.
  - Listado con búsqueda, editor de ficheros con **autoguardado (debounce 1.5 s)**.
  - Campos **Título / Autor / Capo** en una misma fila; se guardan en cabeceras `#title/#artist/#capo` y en `customMetadata` de R2.
  - **Panel de vista previa** a la derecha: muestra los acordes `{X}` colocados sobre la letra.
  - DELETE de ficheros.
- `worker/wrangler.toml`, `worker/package.json` *(nuevos)* — config/deploy.

> Para desplegar: `cd worker && npx wrangler deploy`.

---

## 2. Sincronización de metadatos app ↔ worker (app side)
El título/autor/capo fijados en el worker fluyen a la app al sincronizar.

- `app/.../sync/` *(directorio nuevo)*: `R2Client.kt`, `SyncManager.kt`, `SyncModels.kt`, `SongTextFormat.kt`, `SyncPrefs.kt`.
  - `SyncModels.RemoteObject` lleva `title/artist/capo` (de `/list`).
  - `SyncManager.fillTitleFromRemote` rellena título/autor/capo en cada sync sin descargar el cuerpo y sin pisar valores ya puestos en el dispositivo.
- `app/.../data/Repository.kt` *(mod)* — `setRemoteTitle/Artist/Capo`, ops en lote `deleteSongs/moveSongs/setFavoriteFor`.
- `app/.../data/Dao.kt` *(mod)* — `setTitle/setArtist/setCapo`.
- `app/.../ui/sync/` *(nuevo)* — `SyncScreen.kt`, `SyncViewModel.kt`.

---

## 3. Selección múltiple de canciones (4 pantallas)
Mantener pulsado → checkbox; barra contextual con seleccionar todo / favorita / mover / borrar.

- `app/.../ui/components/SongSelection.kt` *(nuevo)* — estado + `SelectionTopBar`, `BulkMoveDialog`, `BulkDeleteDialog`.
- Pantallas *(mod)*: `ui/unassigned/UnassignedSongsScreen.kt`, `ui/playlist/PlaylistDetailScreen.kt`, `ui/search/SongSearchScreen.kt`, `ui/favorites/FavoritesScreen.kt`.
- ViewModels *(mod)*: los 4 correspondientes (`deleteSelected/moveSelected/favoriteSelected`, `playlists` expuesto donde faltaba).

---

## 4. Reproductor de canción
- `app/.../ui/song/SongViewScreen.kt` *(mod)*:
  - Auto-desplazamiento a velocidades lentas (<65 px/s) — acumulador sub-píxel.
  - Si interrumpes el scroll a mano, **sigue auto-desplazando desde donde lo dejaste** (captura de `CancellationException`).

---

## 5. Teclado tapaba el editor
- `app/src/main/AndroidManifest.xml` *(mod)* — `windowSoftInputMode="adjustResize"`.
- `app/.../ui/song/SongEditorScreen.kt` *(mod)* y `VersionEditorScreen.kt` *(nuevo)* — `.imePadding()`.

---

## 6. Diagramas de acordes
- `app/.../chords/ChordLibrary.kt` *(mod)*:
  - Fix cejilla fantasma (solo cejilla si ≥2 cuerdas en el traste de la raíz).
  - Consulta primero la base de datos de acordes; plantillas como respaldo.
- `app/.../chords/ChordDb.kt` *(nuevo)* — loader de `guitar.json` (chords-db, MIT): convierte frets/fingers/barres y mapea tónicas/sufijos.
- `app/src/main/assets/chords/guitar.json` *(nuevo, 378 KB)* — datos de acordes (chords-db).
- `app/src/main/assets/chords/LICENSE.txt` *(nuevo)* — atribución MIT (obligatoria).
- `app/.../GuitarChordsApp.kt` *(mod)* — `ChordDb.init(this)`.

---

## 7. Otros cambios sin commitear (de trabajo previo en el árbol)
Ficheros tocados/nuevos que ya estaban sin subir antes de esta tanda:

- *(mod)* `.gitignore`, `README.md`, `settings.gradle.kts`, `MainActivity.kt`,
  `chords/ChordRecognizer.kt`, `data/AppDatabase.kt`, `data/Entities.kt`,
  `ui/components/ChordModal.kt`, `ui/dictionary/ChordDictionaryScreen.kt`,
  `ui/nav/NavGraph.kt`, `ui/playlists/PlaylistsScreen.kt`,
  `ui/song/SongViewModel.kt`, `ui/song/SongEditorScreen.kt`,
  `res/drawable/ic_launcher_foreground.xml`, `res/values/colors.xml`, `res/values/strings.xml`.
- *(nuevos)* `chords/MusicTheory.kt`, `ui/components/ChordPickerDialog.kt`,
  `ui/dictionary/TheoryGuide.kt`, `ui/home/HomeScreen.kt`,
  `ui/responsive/WindowSize.kt`, `ui/settings/SettingsScreen.kt`,
  `ui/song/VersionEditorScreen.kt`, `ui/song/VersionEditorViewModel.kt`.

---

## NO se sube (gitignored)
- `worker/credentials.txt` — fichero local, ignorado por `.gitignore`.
- `worker/node_modules/`, `worker/.wrangler/`, `worker/.dev.vars` — ignorados.

---

## Pendiente de verificar antes/después de subir
- **Compilar la app** (no se ejecutó Gradle aquí por riesgo de cuelgue de la VM).
- **Desplegar el worker** (`npx wrangler deploy`).
