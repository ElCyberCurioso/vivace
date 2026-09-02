-- Vivace · esquema de la base de datos (Cloudflare D1)
--
-- Crear/actualizar con:
--   npx wrangler d1 execute vivace --file=schema.sql            (local)
--   npx wrangler d1 execute vivace --remote --file=schema.sql   (producción)
--
-- El TEXTO de cada partitura sigue viviendo en R2 (columna r2_key); aquí solo
-- están los usuarios, los metadatos y quién puede ver qué.

CREATE TABLE IF NOT EXISTS users (
  id            TEXT PRIMARY KEY,           -- uuid
  email         TEXT NOT NULL,
  email_lower   TEXT NOT NULL UNIQUE,       -- para buscar sin distinguir mayúsculas
  name          TEXT NOT NULL DEFAULT '',
  password_hash TEXT NOT NULL,              -- pbkdf2$<iteraciones>$<salt>$<hash>
  role          TEXT NOT NULL DEFAULT 'user', -- 'user' | 'editor' | 'admin'
  created_at    INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS songs (
  id          TEXT PRIMARY KEY,             -- uuid
  owner_id    TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  r2_key      TEXT NOT NULL UNIQUE,         -- objeto en R2 con el texto
  title       TEXT NOT NULL DEFAULT '',
  artist      TEXT NOT NULL DEFAULT '',
  genre       TEXT NOT NULL DEFAULT '',
  capo        INTEGER NOT NULL DEFAULT 0,
  source_url  TEXT NOT NULL DEFAULT '',
  locked      INTEGER NOT NULL DEFAULT 0,   -- protege de ediciones accidentales
  visibility  TEXT NOT NULL DEFAULT 'private', -- 'private' | 'public'
  created_at  INTEGER NOT NULL,
  updated_at  INTEGER NOT NULL,
  deleted_at  INTEGER NOT NULL DEFAULT 0    -- papelera (0 = activa)
);

CREATE INDEX IF NOT EXISTS idx_songs_owner      ON songs(owner_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_songs_visibility ON songs(visibility, deleted_at);
CREATE INDEX IF NOT EXISTS idx_songs_title      ON songs(title);

-- ---------------------------------------------------------------------------
-- Versiones y propuestas (segunda tanda)
--
-- Una "versión" es un arreglo alternativo de una partitura: otro tono, otra
-- cejilla, tablatura… El contenido propio de la partitura es el "Original" y
-- las demás cuelgan de aquí, igual que en la app Android (song_versions).
--
-- El texto vive en R2, como el de las partituras: aquí solo la clave.
CREATE TABLE IF NOT EXISTS song_versions (
  id          TEXT PRIMARY KEY,
  song_id     TEXT NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
  name        TEXT NOT NULL DEFAULT '',
  r2_key      TEXT NOT NULL UNIQUE,
  capo        INTEGER NOT NULL DEFAULT 0,
  source_url  TEXT NOT NULL DEFAULT '',
  position    INTEGER NOT NULL DEFAULT 0,
  author_id   TEXT NOT NULL REFERENCES users(id),
  created_at  INTEGER NOT NULL,
  updated_at  INTEGER NOT NULL,
  deleted_at  INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_versions_song ON song_versions(song_id, deleted_at, position);

-- Propuestas pendientes de revisión: publicar una partitura propia en el
-- catálogo, o aportar una versión a algo ya publicado.
CREATE TABLE IF NOT EXISTS proposals (
  id           TEXT PRIMARY KEY,
  kind         TEXT NOT NULL,                     -- 'publish' | 'version'
  status       TEXT NOT NULL DEFAULT 'pending',   -- pending|approved|rejected|withdrawn
  song_id      TEXT NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
  author_id    TEXT NOT NULL REFERENCES users(id),
  name         TEXT NOT NULL DEFAULT '',          -- nombre de la versión propuesta
  capo         INTEGER NOT NULL DEFAULT 0,
  source_url   TEXT NOT NULL DEFAULT '',
  r2_key       TEXT NOT NULL DEFAULT '',          -- texto propuesto (solo 'version')
  note         TEXT NOT NULL DEFAULT '',          -- por qué lo propone
  reviewer_id  TEXT REFERENCES users(id),
  review_note  TEXT NOT NULL DEFAULT '',
  created_at   INTEGER NOT NULL,
  resolved_at  INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_proposals_status ON proposals(status, created_at);
CREATE INDEX IF NOT EXISTS idx_proposals_author ON proposals(author_id, status);
CREATE INDEX IF NOT EXISTS idx_proposals_song ON proposals(song_id, status);

-- Género/categoría: la columna ya existe en songs desde el principio, pero sin
-- índice. El catálogo filtra por ella.
CREATE INDEX IF NOT EXISTS idx_songs_genre ON songs(genre, visibility, deleted_at);

-- ---------------------------------------------------------------------------
-- Comentarios y valoraciones (tercera tanda)

-- Un hilo plano por partitura: quien tiene sesión opina, y el autor o un
-- editor pueden retirar un comentario. Borrado lógico, como todo lo demás.
CREATE TABLE IF NOT EXISTS comments (
  id          TEXT PRIMARY KEY,
  song_id     TEXT NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
  author_id   TEXT NOT NULL REFERENCES users(id),
  body        TEXT NOT NULL,
  created_at  INTEGER NOT NULL,
  deleted_at  INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_comments_song ON comments(song_id, deleted_at, created_at);

-- Estrellas por VERSIÓN, no por partitura: cada arreglo se valora aparte.
-- `version_id` vacío es el "Original". La clave primaria compuesta garantiza
-- un voto por persona y versión, y permite el upsert sin buscar antes.
CREATE TABLE IF NOT EXISTS ratings (
  song_id     TEXT NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
  version_id  TEXT NOT NULL DEFAULT '',
  user_id     TEXT NOT NULL REFERENCES users(id),
  stars       INTEGER NOT NULL,
  updated_at  INTEGER NOT NULL,
  PRIMARY KEY (song_id, version_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_ratings_song ON ratings(song_id, version_id);

-- ---------------------------------------------------------------------------
-- Listas (carpetas) del usuario · cuarta tanda
--
-- Hasta ahora la carpeta y el favorito viajaban ESCONDIDOS dentro del texto de
-- la partitura, como cabeceras `#playlist:` y `#favorite:`. La base no los
-- conocía, así que la web no podía tener carpetas ni favoritos por mucho que
-- la app sí. Pasan a ser datos de verdad.
CREATE TABLE IF NOT EXISTS playlists (
  id         TEXT PRIMARY KEY,
  owner_id   TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name       TEXT NOT NULL,
  position   INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  deleted_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_playlists_owner   ON playlists(owner_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_playlists_updated ON playlists(owner_id, updated_at, id);

-- Freno a la fuerza bruta en /auth/login y /auth/register. Una fila por
-- (email + IP) con su ventana; ver src/limits.js.
CREATE TABLE IF NOT EXISTS auth_attempts (
  key          TEXT PRIMARY KEY,
  count        INTEGER NOT NULL DEFAULT 0,
  window_start INTEGER NOT NULL
);

-- Índices del feed de cambios (GET /api/sync/changes): se recorre por
-- (updated_at, id), que es también el cursor.
CREATE INDEX IF NOT EXISTS idx_songs_owner_updated   ON songs(owner_id, updated_at, id);
CREATE INDEX IF NOT EXISTS idx_versions_song_updated ON song_versions(song_id, updated_at, id);

-- Ajustes de la instalación (clave -> valor). De momento solo `registration_open`,
-- el interruptor de altas de cuenta, que maneja el administrador desde la web.
-- Lo que no está en la tabla vale su valor por defecto: una instalación recién
-- creada admite registros, que es como se da de alta el primer administrador.
CREATE TABLE IF NOT EXISTS settings (
  key        TEXT PRIMARY KEY,
  value      TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
