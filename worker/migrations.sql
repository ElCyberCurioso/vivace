-- Vivace · cambios sobre tablas que ya existen.
--
-- schema.sql solo CREA lo que falta, y SQLite no tiene "ADD COLUMN IF NOT
-- EXISTS": por eso los ALTER viven aquí. Al repetirlos fallan con "duplicate
-- column name", que es inofensivo — el despliegue los aplica tolerando ese
-- error concreto.

-- Vídeo de YouTube de la canción, para el reproductor del visor.
ALTER TABLE songs ADD COLUMN youtube_url TEXT NOT NULL DEFAULT '';
