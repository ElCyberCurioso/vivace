-- Vivace · cambios sobre tablas que ya existen.
--
-- schema.sql solo CREA lo que falta, y SQLite no tiene "ADD COLUMN IF NOT
-- EXISTS": por eso los ALTER viven aquí. Al repetirlos fallan con "duplicate
-- column name", que es inofensivo — el despliegue los aplica tolerando ese
-- error concreto.

-- Vídeo de YouTube de la canción, para el reproductor del visor.
ALTER TABLE songs ADD COLUMN youtube_url TEXT NOT NULL DEFAULT '';

-- Cuarta tanda · sincronización automática y paridad con la app.
--
-- `rev` es un contador que incrementa el SERVIDOR en cada escritura. Es lo que
-- permite detectar un conflicto: `updated_at` no sirve porque lo pisa cualquiera
-- y dos relojes distintos no se pueden comparar.
ALTER TABLE songs ADD COLUMN rev INTEGER NOT NULL DEFAULT 1;
-- Carpeta, favorito y orden manual: antes vivían dentro del texto (ver schema.sql).
ALTER TABLE songs ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0;
ALTER TABLE songs ADD COLUMN position INTEGER NOT NULL DEFAULT 0;
ALTER TABLE songs ADD COLUMN playlist_id TEXT;
ALTER TABLE song_versions ADD COLUMN rev INTEGER NOT NULL DEFAULT 1;

-- Quinta tanda · interruptor de altas de cuenta.
-- La tabla se crea aquí además de en schema.sql para las instalaciones que ya
-- estaban en marcha; CREATE ... IF NOT EXISTS hace que repetirlo no rompa nada.
CREATE TABLE IF NOT EXISTS settings (
  key        TEXT PRIMARY KEY,
  value      TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
