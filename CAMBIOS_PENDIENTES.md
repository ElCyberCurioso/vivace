# Cambios pendientes de subir a GitHub

Rama: `master` · Generado: 2026-06-12

Resumen de todo lo no commiteado: **48 ficheros modificados** + **nuevos**
(~2.000 inserciones). Agrupado por funcionalidad.

---

## 1. Modernización del stack Android
- `build.gradle.kts`, `app/build.gradle.kts` *(mod)* — Kotlin 2.0.21
  (+plugin.compose), compile/targetSdk 35, AGP 8.6.1, KSP, Compose BOM
  2024.12.01, dependencias actualizadas.
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` *(nuevos)* —
  wrapper de Gradle 8.7 incluido en el repo.
- `app/src/test/` *(nuevo)* — tests unitarios (ChordTransposer, SongTextFormat).
- `AndroidManifest.xml` *(mod)* + `res/xml/backup_rules.xml`,
  `res/xml/data_extraction_rules.xml` *(nuevos)* — reglas de backup.

## 2. UI / i18n
- `res/values-en/` *(nuevo)* — traducción completa al inglés (es por defecto).
- `ui/theme/Type.kt` *(nuevo)*, `Theme.kt` *(mod)* — tipografía propia,
  ExtendedColors, edge-to-edge + splash API.
- `ui/components/EmptyState.kt` *(nuevo)* — estado vacío reutilizable.
- Iconos AutoMirrored, icono monocromo (`mipmap-anydpi-v26`), predictive
  back, transiciones de navegación (`MainActivity.kt`, `NavGraph.kt`,
  `themes.xml`).

## 3. Metrónomo
- `metronome/` y `ui/metronome/` *(nuevos)* — MetronomeEngine con
  AudioTrack estático, acento de compás, pantalla propia.

## 4. Acordes personalizados
- `chords/CustomChords.kt` *(nuevo)* — caché de digitaciones de usuario
  con prioridad sobre chords-db.
- `ui/components/FretboardInput.kt` *(nuevo)* — diagrama editable
  compartido (finder + editor de acordes).
- `ui/components/ChordShapeEditorDialog.kt` *(nuevo)* — editor de
  digitaciones desde el modal de acorde.
- `data/` *(mod)* — entidad CustomChord, DB v9.

## 5. Sincronización R2 estilo git (app)
- `sync/SyncManager.kt` *(mod)* — `sync()` solo descarga y planifica;
  la subida pasa a `push()` explícito. Conflictos excluidos del push.
- `sync/SyncModels.kt` *(mod)* — `PendingUpload`; `SyncResult` con
  `pendingUploads`.
- `ui/sync/SyncViewModel.kt`, `SyncScreen.kt` *(mod)* — diálogo
  "Confirmar subida" con lista de cambios locales (marca "nueva");
  conflictos resueltos canción a canción (Local/Servidor); los no
  resueltos quedan en espera hasta el siguiente sync ("Más tarde").
- `res/values*/strings.xml` *(mod)* — strings nuevas es/en.

## 6. Buscador de acordes por diagrama (tablet)
- `ui/finder/ChordFinderScreen.kt` *(mod)* — ancho máximo 380dp: en
  tablets el 90 % del ancho (ratio 6:7) desbordaba la pantalla.

## 7. Worker R2 — panel de administración (`worker/src/index.js`)
- **Backup**: botón "⬇ Backup" descarga un ZIP con todas las partituras;
  **Restaurar**: sube un ZIP (admite STORE y DEFLATE), confirma antes y
  conserva las claves originales. ZIP construido/parseado 100 % en el
  navegador — sin endpoints nuevos.
- **Borrado masivo**: endpoint `POST /delete` (body `{ keys: [...] }`,
  troceado a 1000 claves por llamada R2) + checkboxes por fichero,
  seleccionar todo (visible), selección de rango con Mayús+clic y botón
  "Eliminar (N)".
- **Listado**: artista (línea 1) sobre título (línea 2); numeración
  estable por orden alfabético de título; orden por artista; búsqueda
  también por artista; sin rutas de fichero — botón "Storage ↗" abre el
  bucket en el dashboard de Cloudflare (`[vars] STORAGE_URL` en
  `wrangler.toml`).
- **Capo**: botonera 0–12 en lugar del campo numérico.
- **Detección de acordes**: botón "♪ Detectar acordes" envuelve en `{X}`
  las líneas compuestas solo por acordes/separadores; respeta `{tab}` y
  líneas ya marcadas.
- Checks de desarrollo *(nuevos)*: `worker/check-admin.mjs` (sintaxis del
  script embebido) y `worker/test-detect.mjs` (8 casos de detección).

---

## NO se sube (gitignored)
- `worker/credentials.txt`, `worker/node_modules/`, `worker/.wrangler/`,
  `worker/.dev.vars`.

---

## Pendiente de verificar antes/después de subir
- App compilada ✅ (`compileDebugKotlin` OK, solo warnings preexistentes).
- Worker verificado ✅ (sintaxis módulo + script admin, tests de detección).
- **Desplegar el worker**: `cd worker && npx wrangler deploy`.
- Probar el buscador por diagrama en la tablet.
