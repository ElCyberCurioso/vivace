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
  role          TEXT NOT NULL DEFAULT 'user', -- 'user' | 'admin'
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
