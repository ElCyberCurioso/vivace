/*
 * Vivace · migración de las partituras que ya existen en R2.
 *
 * Antes del multiusuario todo el contenido vivía en R2 bajo `songs/` sin dueño.
 * Este proceso las indexa en D1 como partituras del administrador, SIN mover ni
 * reescribir los objetos: la columna `r2_key` apunta a la clave original.
 *
 * Se dispara con  POST /admin/migrate  (solo administradores) y es idempotente:
 * las claves ya indexadas se saltan, así que se puede repetir sin duplicar.
 *
 * Va POR TANDAS a proposito. Un Worker tiene un tope de subpeticiones por
 * invocacion (50 en el plan gratuito, 1000 en el de pago) y cada lectura de R2
 * cuenta una: hacerlo todo de golpe revienta con un 500 en cuanto el catalogo
 * crece. Cada llamada devuelve un cursor y quien llama repite hasta done.
 */

import { SONG_PREFIX, insertSongs, listSongKeys } from "./db.js";

/** Extrae los metadatos de la cabecera `#clave: valor` del propio fichero. */
function parseHeaders(text) {
  const HEADER = /^#([A-Za-z]+):[ \t]?(.*)$/;
  const out = { title: "", artist: "", genre: "", capo: 0, url: "", locked: false };
  for (const line of text.replace(/\r\n/g, "\n").split("\n")) {
    if (line.trim() === "---") break;
    const m = HEADER.exec(line);
    if (!m) break;
    const key = m[1].toLowerCase();
    const value = m[2].trim();
    if (key === "title") out.title = value;
    else if (key === "artist") out.artist = value;
    else if (key === "genre") out.genre = value;
    else if (key === "capo") out.capo = Number(value) || 0;
    else if (key === "url") out.url = value;
    else if (key === "locked") out.locked = value.toLowerCase() === "true";
  }
  return out;
}

/** Tamano de tanda por defecto: cabe de sobra en el plan gratuito. */
export const MIGRATE_BATCH = 40;

/**
 * Indexa una tanda de lo que haya en `songs/` y aún no esté registrado.
 *
 * @param visibility con la que se dan de alta las nuevas (`private` por defecto).
 * @param options.limit cuántos objetos mirar en esta llamada.
 * @param options.cursor por dónde seguir; lo devuelve la llamada anterior.
 * @returns { imported, skipped, done, cursor } — repetir mientras `done` sea falso.
 */
export async function migrateExistingSongs(env, ownerId, visibility = "private", options = {}) {
  const limit = Math.min(Math.max(Number(options.limit) || MIGRATE_BATCH, 1), 200);
  // Una sola consulta para todo lo ya indexado: repetir la migracion sale gratis.
  const known = await listSongKeys(env.DB);
  const page = await env.BUCKET.list({
    prefix: SONG_PREFIX,
    cursor: options.cursor || undefined,
    limit,
    include: ["customMetadata"]
  });

  const nuevas = [];
  let skipped = 0;
  for (const obj of page.objects) {
    if (known.has(obj.key)) { skipped++; continue; }
    const stored = await env.BUCKET.get(obj.key);
    const text = stored ? await stored.text() : "";
    const meta = parseHeaders(text);
    const cm = obj.customMetadata || {};
    const uploaded = obj.uploaded ? new Date(obj.uploaded).getTime() : Date.now();
    nuevas.push({
      owner_id: ownerId,
      r2_key: obj.key,
      title: meta.title || cm.title || "",
      artist: meta.artist || cm.artist || "",
      genre: meta.genre,
      capo: meta.capo,
      source_url: meta.url || cm.url || "",
      locked: meta.locked,
      visibility,
      created_at: Number(cm.created) || uploaded
    });
  }
  await insertSongs(env.DB, nuevas);

  const cursor = page.truncated ? page.cursor : null;
  return { imported: nuevas.length, skipped, done: !cursor, cursor };
}
