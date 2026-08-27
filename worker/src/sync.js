/*
 * Vivace · sincronización por lotes (la usa la app Android en segundo plano).
 *
 *   GET  /api/sync/changes?songs=<cur>&playlists=<cur>&versions=<cur>&limit=
 *   POST /api/sync/push    { playlists: [...], songs: [...], versions: [...] }
 *
 * Por qué existe este módulo
 * --------------------------
 * La sincronización anterior pedía la lista entera de partituras y luego una
 * petición POR CANCIÓN para bajar el texto: N+1 llamadas cada vez, sin forma de
 * preguntar "¿qué ha cambiado desde la última vez?" y sin enterarse jamás de un
 * borrado. Así no se puede sincronizar solo en segundo plano.
 *
 * Aquí el cliente manda un cursor por flujo y recibe únicamente lo que cambió,
 * lápidas incluidas y con el texto ya incrustado. Al subir, cada partitura viaja
 * con el `rev` que el cliente creía tener: si no coincide con el del servidor,
 * ese elemento vuelve marcado como conflicto en vez de pisar el trabajo ajeno.
 *
 * Cursor: cadena opaca "<updated_at>:<id>". Vacía = desde el principio.
 */

import {
  SONG_PREFIX, changedPlaylists, changedSongs, changedVersions, findPlaylistById,
  findSongById, findVersionAnyState, hardDeleteSong, insertPlaylist, insertSong,
  insertVersion, listPlaylists, publicPlaylist, publicSong, publicVersion,
  restoreSong, softDeletePlaylist, softDeleteSong, softDeleteVersion, updatePlaylist,
  updateSongMeta, updateVersionMeta, uuid
} from "./db.js";
import {
  canEdit, canManageTrashed, canSetVisibility, editDenialReason, isValidVisibility
} from "./permissions.js";
import { checkSongFields } from "./limits.js";
import { isValidYoutube } from "./youtube.js";

/**
 * Cuántos elementos por tanda. Cada partitura o versión con texto es UNA
 * subpetición a R2, y un Worker tiene un tope por invocación: con 20 + 20 el
 * peor caso son ~40 lecturas más las consultas a D1, que entra de sobra. Es el
 * mismo criterio que ya usa MIGRATE_BATCH en migrate.js.
 */
export const SYNC_LIMIT_DEFAULT = 20;
export const SYNC_LIMIT_MAX = 25;
/** Tope de elementos por llamada a /push, por el mismo motivo. */
export const PUSH_MAX_SONGS = 20;
export const PUSH_MAX_VERSIONS = 20;
export const PUSH_MAX_PLAYLISTS = 50;

const json = (data, cors, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { ...cors, "Content-Type": "application/json; charset=utf-8" }
  });

const fail = (message, cors, status) => json({ error: message }, cors, status);

/* ------------------------------- cursores ------------------------------- */

/** "<updated_at>:<id>" → { since, id }. Cualquier basura equivale a "desde 0". */
export function parseCursor(raw) {
  const texto = String(raw || "");
  const corte = texto.indexOf(":");
  if (corte < 0) return { since: 0, id: "" };
  const since = Number(texto.slice(0, corte));
  return { since: Number.isFinite(since) && since > 0 ? since : 0, id: texto.slice(corte + 1) };
}

/** Cursor de la última fila de una tanda; si viene vacía, se mantiene el previo. */
export function buildCursor(filas, previo) {
  if (!filas.length) return previo;
  const ultima = filas[filas.length - 1];
  return `${ultima.updated_at}:${ultima.id}`;
}

function clampLimit(raw) {
  const n = Number(raw);
  if (!Number.isFinite(n) || n <= 0) return SYNC_LIMIT_DEFAULT;
  return Math.min(Math.floor(n), SYNC_LIMIT_MAX);
}

/* ------------------------------- cambios ------------------------------- */

async function readBody(env, row) {
  if (!row.r2_key) return "";
  const obj = await env.BUCKET.get(row.r2_key);
  return obj ? await obj.text() : "";
}

/**
 * Todo lo que cambió desde los cursores del cliente. El texto viaja incrustado
 * salvo en las lápidas: bajar el cuerpo de algo que se acaba de borrar sería
 * gastar una subpetición para nada.
 */
async function getChanges(env, url, cors, user) {
  const limit = clampLimit(url.searchParams.get("limit"));
  const curP = parseCursor(url.searchParams.get("playlists"));
  const curS = parseCursor(url.searchParams.get("songs"));
  const curV = parseCursor(url.searchParams.get("versions"));

  const listas = await changedPlaylists(env.DB, user.id, curP.since, curP.id, limit);
  const canciones = await changedSongs(env.DB, user.id, curS.since, curS.id, limit);
  const versiones = await changedVersions(env.DB, user.id, curV.since, curV.id, limit);

  const songItems = [];
  for (const fila of canciones) {
    const ficha = publicSong(fila, true);
    ficha.content = fila.deleted_at ? "" : await readBody(env, fila);
    songItems.push(ficha);
  }
  const versionItems = [];
  for (const fila of versiones) {
    const ficha = publicVersion(fila);
    ficha.content = fila.deleted_at ? "" : await readBody(env, fila);
    versionItems.push(ficha);
  }

  const more = listas.length === limit || canciones.length === limit || versiones.length === limit;
  return json({
    serverTime: Date.now(),
    playlists: {
      items: listas.map(publicPlaylist),
      cursor: buildCursor(listas, url.searchParams.get("playlists") || ""),
      more: listas.length === limit
    },
    songs: {
      items: songItems,
      cursor: buildCursor(canciones, url.searchParams.get("songs") || ""),
      more: canciones.length === limit
    },
    versions: {
      items: versionItems,
      cursor: buildCursor(versiones, url.searchParams.get("versions") || ""),
      more: versiones.length === limit
    },
    more
  }, cors);
}

/* --------------------------------- push --------------------------------- */

/**
 * Resuelve a qué id apunta una referencia que puede venir de este mismo lote:
 * el cliente manda `playlistId` si ya existía en el servidor, o
 * `playlistClientId` si la lista se acaba de crear aquí mismo.
 */
function resolveRef(item, mapa, campoId, campoClient) {
  if (item[campoId]) return item[campoId];
  const cliente = item[campoClient];
  return cliente && mapa[cliente] ? mapa[cliente] : null;
}

async function pushPlaylists(env, user, entradas, mapa) {
  const salida = [];
  for (const item of entradas) {
    const resultado = { clientId: item.clientId || null };
    try {
      const existente = item.id ? await findPlaylistById(env.DB, item.id) : null;
      if (item.id && (!existente || existente.owner_id !== user.id)) {
        salida.push({ ...resultado, ok: false, error: "lista no encontrada" });
        continue;
      }
      if (item.deleted) {
        if (existente) await softDeletePlaylist(env.DB, existente.id);
        salida.push({ ...resultado, ok: true, id: item.id, deleted: true });
        continue;
      }
      const nombre = String(item.name || "").trim();
      if (!nombre) {
        salida.push({ ...resultado, ok: false, error: "la lista necesita un nombre" });
        continue;
      }
      const guardada = existente
        ? await updatePlaylist(env.DB, existente.id, { name: nombre, position: item.position })
        : await insertPlaylist(env.DB, {
            owner_id: user.id, name: nombre, position: item.position || 0
          });
      if (item.clientId) mapa[item.clientId] = guardada.id;
      salida.push({ ...resultado, ok: true, id: guardada.id, updatedAt: guardada.updated_at });
    } catch (err) {
      salida.push({ ...resultado, ok: false, error: String(err && err.message) });
    }
  }
  return salida;
}

/** Ficha del servidor + su texto, para que el cliente resuelva el conflicto. */
async function conflictPayload(env, song) {
  const ficha = publicSong(song, true);
  ficha.content = song.deleted_at ? "" : await readBody(env, song);
  return ficha;
}

async function pushSongs(env, user, entradas, mapaListas, mapaCanciones) {
  const salida = [];
  for (const item of entradas) {
    const resultado = { clientId: item.clientId || null };
    try {
      const problema = checkSongFields(item);
      if (problema) { salida.push({ ...resultado, ok: false, error: problema }); continue; }
      if (item.youtubeUrl !== undefined && !isValidYoutube(item.youtubeUrl)) {
        salida.push({ ...resultado, ok: false, error: "el enlace de YouTube no es válido" });
        continue;
      }

      const existente = item.id ? await findSongById(env.DB, item.id) : null;

      // ---- alta ----
      if (!existente) {
        if (item.id) {
          // El cliente creía tener una partitura que ya no está: se lo decimos
          // en vez de crear un duplicado con otro id a su espalda.
          salida.push({ ...resultado, ok: false, gone: true, error: "no encontrada" });
          continue;
        }
        if (item.deleted) { salida.push({ ...resultado, ok: true, skipped: true }); continue; }
        const r2Key = `${SONG_PREFIX}${uuid()}.txt`;
        await env.BUCKET.put(r2Key, String(item.content || ""), {
          httpMetadata: { contentType: "text/plain; charset=utf-8" }
        });
        const creada = await insertSong(env.DB, {
          owner_id: user.id, r2_key: r2Key,
          title: item.title, artist: item.artist, genre: item.genre,
          capo: Number(item.capo) || 0, source_url: item.sourceUrl,
          youtube_url: String(item.youtubeUrl || "").trim(),
          locked: !!item.locked,
          // Publicar sigue siendo un acto editorial: nace privada.
          visibility: "private",
          favorite: !!item.favorite, position: item.position || 0,
          playlist_id: resolveRef(item, mapaListas, "playlistId", "playlistClientId")
        });
        if (item.clientId) mapaCanciones[item.clientId] = creada.id;
        salida.push({ ...resultado, ok: true, id: creada.id, rev: creada.rev, updatedAt: creada.updated_at });
        continue;
      }

      // ---- papelera ----
      // Va ANTES del permiso de edición: `canEdit` da por inexistente lo que ya
      // está borrado, así que restaurar y purgar —las dos únicas operaciones
      // que solo tienen sentido sobre una fila borrada— nunca pasarían.
      if (item.purge) {
        if (!canManageTrashed(user, existente)) {
          salida.push({ ...resultado, ok: false, error: "no puedes borrar esta partitura" });
          continue;
        }
        if (existente.r2_key) await env.BUCKET.delete(existente.r2_key);
        await hardDeleteSong(env.DB, existente.id);
        salida.push({ ...resultado, ok: true, id: existente.id, purged: true });
        continue;
      }
      if (existente.deleted_at && !item.deleted) {
        // El servidor ya la tenía en la papelera y el móvil la trae viva:
        // restaurarla es del dueño, no de un editor.
        if (!canManageTrashed(user, existente)) {
          salida.push({ ...resultado, ok: false, error: "no puedes restaurar esta partitura" });
          continue;
        }
        const viva = await restoreSong(env.DB, existente.id);
        salida.push({ ...resultado, ok: true, id: viva.id, rev: viva.rev, updatedAt: viva.updated_at });
        continue;
      }

      // ---- edición o borrado de algo que ya existe ----
      const denial = editDenialReason(user, existente);
      if (denial) {
        // Borrar dos veces no es un error: si ya estaba en la papelera, el
        // móvil y el servidor dicen lo mismo y no hay nada que hacer.
        if (item.deleted && existente.deleted_at && canManageTrashed(user, existente)) {
          salida.push({ ...resultado, ok: true, id: existente.id, deleted: true });
          continue;
        }
        salida.push({ ...resultado, ok: false, error: "no puedes modificar esta partitura" });
        continue;
      }

      // Conflicto: alguien más la tocó desde que el cliente la bajó. No se
      // sobrescribe; se le devuelve la del servidor para que decida.
      const baseRev = Number(item.baseRev);
      if (Number.isFinite(baseRev) && baseRev > 0 && baseRev !== (existente.rev || 1)) {
        salida.push({
          ...resultado, ok: false, conflict: true, id: existente.id,
          server: await conflictPayload(env, existente)
        });
        continue;
      }

      if (item.deleted) {
        await softDeleteSong(env.DB, existente.id);
        const tras = await findSongById(env.DB, existente.id);
        salida.push({ ...resultado, ok: true, id: existente.id, rev: tras.rev, updatedAt: tras.updated_at, deleted: true });
        continue;
      }

      if (typeof item.content === "string") {
        await env.BUCKET.put(existente.r2_key, item.content, {
          httpMetadata: { contentType: "text/plain; charset=utf-8" }
        });
      }
      // La visibilidad solo la mueve quien puede: si la app manda "public" sin
      // ser editora, se ignora en silencio en vez de rechazar la edición entera.
      const pedida = isValidVisibility(item.visibility) ? item.visibility : existente.visibility;
      const actualizada = await updateSongMeta(env.DB, existente.id, {
        title: item.title ?? existente.title,
        artist: item.artist ?? existente.artist,
        genre: item.genre ?? existente.genre,
        capo: item.capo ?? existente.capo,
        source_url: item.sourceUrl ?? existente.source_url,
        youtube_url: item.youtubeUrl !== undefined
          ? String(item.youtubeUrl || "").trim() : (existente.youtube_url || ""),
        locked: item.locked ?? !!existente.locked,
        visibility: canSetVisibility(user) ? pedida : existente.visibility,
        favorite: item.favorite ?? !!existente.favorite,
        position: item.position ?? existente.position,
        playlist_id: item.playlistId !== undefined || item.playlistClientId
          ? resolveRef(item, mapaListas, "playlistId", "playlistClientId")
          : existente.playlist_id
      });
      if (item.clientId) mapaCanciones[item.clientId] = actualizada.id;
      salida.push({ ...resultado, ok: true, id: actualizada.id, rev: actualizada.rev, updatedAt: actualizada.updated_at });
    } catch (err) {
      salida.push({ ...resultado, ok: false, error: String(err && err.message) });
    }
  }
  return salida;
}

async function pushVersions(env, user, entradas, mapaCanciones) {
  const salida = [];
  for (const item of entradas) {
    const resultado = { clientId: item.clientId || null };
    try {
      const problema = checkSongFields(item);
      if (problema) { salida.push({ ...resultado, ok: false, error: problema }); continue; }

      const existente = item.id ? await findVersionAnyState(env.DB, item.id) : null;
      const songId = existente
        ? existente.song_id
        : resolveRef(item, mapaCanciones, "songId", "songClientId");
      if (!songId) {
        salida.push({ ...resultado, ok: false, error: "versión sin partitura" });
        continue;
      }
      const song = await findSongById(env.DB, songId);
      if (!song || !canEdit(user, song)) {
        salida.push({ ...resultado, ok: false, error: "no puedes modificar esta partitura" });
        continue;
      }

      if (item.deleted) {
        if (existente) await softDeleteVersion(env.DB, existente.id);
        salida.push({ ...resultado, ok: true, id: item.id || null, deleted: true });
        continue;
      }

      if (existente) {
        if (typeof item.content === "string") {
          await env.BUCKET.put(existente.r2_key, item.content, {
            httpMetadata: { contentType: "text/plain; charset=utf-8" }
          });
        }
        const actualizada = await updateVersionMeta(env.DB, existente.id, {
          name: item.name ?? existente.name,
          capo: item.capo ?? existente.capo,
          source_url: item.sourceUrl ?? existente.source_url,
          position: item.position ?? existente.position
        });
        salida.push({ ...resultado, ok: true, id: actualizada.id, rev: actualizada.rev, updatedAt: actualizada.updated_at });
        continue;
      }

      const r2Key = `${SONG_PREFIX}${uuid()}.txt`;
      await env.BUCKET.put(r2Key, String(item.content || ""), {
        httpMetadata: { contentType: "text/plain; charset=utf-8" }
      });
      const creada = await insertVersion(env.DB, {
        song_id: songId, name: item.name, r2_key: r2Key,
        capo: Number(item.capo) || 0, source_url: item.sourceUrl,
        position: item.position, author_id: user.id
      });
      salida.push({ ...resultado, ok: true, id: creada.id, rev: creada.rev, updatedAt: creada.updated_at });
    } catch (err) {
      salida.push({ ...resultado, ok: false, error: String(err && err.message) });
    }
  }
  return salida;
}

/**
 * Sube un lote. El orden importa: primero las listas (para que una partitura
 * pueda apuntar a una carpeta recién creada) y al final las versiones (que
 * necesitan el id de su partitura).
 */
async function push(request, env, cors, user) {
  let body;
  try {
    body = await request.json();
  } catch (e) {
    return fail("cuerpo JSON no válido", cors, 400);
  }
  const listas = Array.isArray(body.playlists) ? body.playlists : [];
  const canciones = Array.isArray(body.songs) ? body.songs : [];
  const versiones = Array.isArray(body.versions) ? body.versions : [];
  if (listas.length > PUSH_MAX_PLAYLISTS || canciones.length > PUSH_MAX_SONGS ||
      versiones.length > PUSH_MAX_VERSIONS) {
    return fail(
      `lote demasiado grande (máximo ${PUSH_MAX_PLAYLISTS} listas, ` +
      `${PUSH_MAX_SONGS} partituras y ${PUSH_MAX_VERSIONS} versiones)`, cors, 413
    );
  }

  const mapaListas = {};
  const mapaCanciones = {};
  const resListas = await pushPlaylists(env, user, listas, mapaListas);
  const resCanciones = await pushSongs(env, user, canciones, mapaListas, mapaCanciones);
  const resVersiones = await pushVersions(env, user, versiones, mapaCanciones);

  return json({
    serverTime: Date.now(),
    playlists: resListas,
    songs: resCanciones,
    versions: resVersiones
  }, cors);
}

/* -------------------------------- listas -------------------------------- */
// CRUD suelto para la web, que edita de una en una y no por lotes.

async function playlistsRoute(request, env, url, cors, user, method, path) {
  if (path === "/api/playlists" && method === "GET") {
    const filas = await listPlaylists(env.DB, user.id);
    return json({ playlists: filas.map(publicPlaylist) }, cors);
  }
  if (path === "/api/playlists" && method === "POST") {
    const body = await request.json().catch(() => null);
    const nombre = String((body && body.name) || "").trim();
    if (!nombre) return fail("la lista necesita un nombre", cors, 400);
    const creada = await insertPlaylist(env.DB, {
      owner_id: user.id, name: nombre, position: (body && body.position) || 0
    });
    return json({ playlist: publicPlaylist(creada) }, cors, 201);
  }
  const m = path.match(/^\/api\/playlists\/([^/]+)$/);
  if (!m) return null;
  const lista = await findPlaylistById(env.DB, m[1]);
  if (!lista || lista.deleted_at || lista.owner_id !== user.id) {
    return fail("no encontrada", cors, 404);
  }
  if (method === "PUT") {
    const body = await request.json().catch(() => null);
    const nombre = String((body && body.name) || "").trim();
    if (!nombre) return fail("la lista necesita un nombre", cors, 400);
    const guardada = await updatePlaylist(env.DB, lista.id, {
      name: nombre, position: (body && body.position) ?? lista.position
    });
    return json({ playlist: publicPlaylist(guardada) }, cors);
  }
  if (method === "DELETE") {
    await softDeletePlaylist(env.DB, lista.id);
    return json({ id: lista.id, deleted: true }, cors);
  }
  return null;
}

/**
 * Enruta /api/sync/* y /api/playlists*. Devuelve null si la ruta no es de aquí.
 * `user` ya viene resuelto por api.js.
 */
export async function handleSync(request, env, url, cors, user) {
  const path = url.pathname;
  const method = request.method;
  if (!path.startsWith("/api/sync") && !path.startsWith("/api/playlists")) return null;
  if (!user) return fail("inicia sesión", cors, 401);

  if (path === "/api/sync/changes" && method === "GET") {
    return getChanges(env, url, cors, user);
  }
  if (path === "/api/sync/push" && method === "POST") {
    return push(request, env, cors, user);
  }
  if (path.startsWith("/api/playlists")) {
    return playlistsRoute(request, env, url, cors, user, method, path);
  }
  return null;
}
