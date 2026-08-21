/*
 * Vivace · API multiusuario (la usan la web y, desde la fase 3, la app Android).
 *
 *   POST /auth/register   { email, password, name }  -> { token, user }
 *   POST /auth/login      { email, password }        -> { token, user }
 *   GET  /auth/me                                    -> { user }
 *
 *   GET    /api/songs                 -> partituras propias
 *   GET    /api/songs/public[?owner=] -> catálogo publicado (por defecto, admin)
 *   POST   /api/songs                 { title, artist, …, content }
 *   GET    /api/songs/:id             -> ficha + contenido (según permisos)
 *   PUT    /api/songs/:id             -> actualiza metadatos y/o contenido
 *   DELETE /api/songs/:id             -> a la papelera
 *
 * Todo lo que no sea público exige `Authorization: Bearer <jwt>`.
 */

import {
  bearerToken, hashPassword, normalizeEmail, signToken, validateCredentials,
  verifyPassword, verifyToken
} from "./auth.js";
import {
  SONG_PREFIX, countPendingProposals, createUser, countUsers, deleteRating,
  findCommentById, insertComment, listComments, listRatings, listUserRatings,
  publicComment, setRating, softDeleteComment, findProposalById, listSongsAfter, setGenres,
  listSongsWithoutVideo,
  findSongById, findUserByEmail, findUserById, findVersionById, insertProposal,
  insertSong, insertVersion, isValidSort, listOwnSongs, listProposals,
  listPublicGenres, listPublicSongs, listUsers, listVersions, publicProposal,
  publicSong, publicUser, publicVersion, resolveProposal, softDeleteSong,
  softDeleteVersion, updateSongMeta, updateUserRole, updateVersionMeta, uuid
} from "./db.js";
import {
  canAddVersion, canComment, canDeleteComment, canRate,
  canEdit, canEditChords, canManageRoles, canPropose, canReview,
  canSetVisibility, canView, canWithdrawProposal, editDenialReason, isEditor,
  isValidRole, isValidVisibility
} from "./permissions.js";
import {
  ChordError, mergeSeed, readGlobalChords, sanitizeDictionary, writeGlobalChords
} from "./chords.js";
import { CHORD_SEED } from "./chords-seed.js";
import { FALLBACK_GENRE, guessGenre } from "./genres.js";
import { isValidYoutube, youtubeSearch } from "./youtube.js";

const json = (data, cors, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { ...cors, "Content-Type": "application/json; charset=utf-8" }
  });

const fail = (message, cors, status) => json({ error: message }, cors, status);

/** Secreto de firma: AUTH_SECRET, o el token de sync como respaldo. */
const authSecret = (env) => env.AUTH_SECRET || env.SYNC_TOKEN || "";

/** Usuario autenticado a partir del JWT, o null si no hay sesión válida. */
export async function currentUser(request, env) {
  const secret = authSecret(env);
  if (!secret || !env.DB) return null;
  const payload = await verifyToken(bearerToken(request), secret);
  if (!payload?.sub) return null;
  return findUserById(env.DB, payload.sub);
}

async function readJson(request) {
  try {
    return await request.json();
  } catch (e) {
    return null;
  }
}

// ---------------------------------------------------------------- auth ----
async function register(request, env, cors) {
  const body = await readJson(request);
  if (!body) return fail("cuerpo JSON no válido", cors, 400);
  const problem = validateCredentials(body.email, body.password);
  if (problem) return fail(problem, cors, 400);

  const email = normalizeEmail(body.email);
  if (await findUserByEmail(env.DB, email)) {
    return fail("ese email ya está registrado", cors, 409);
  }
  // El primer usuario que se da de alta administra la instalación.
  const role = (await countUsers(env.DB)) === 0 ? "admin" : "user";
  const user = await createUser(env.DB, {
    email: body.email.trim(),
    name: body.name || "",
    passwordHash: await hashPassword(body.password),
    role
  });
  const token = await signToken({ sub: user.id, role: user.role }, authSecret(env));
  return json({ token, user: publicUser(user) }, cors, 201);
}

async function login(request, env, cors) {
  const body = await readJson(request);
  if (!body) return fail("cuerpo JSON no válido", cors, 400);
  const user = await findUserByEmail(env.DB, normalizeEmail(body.email));
  // Mismo mensaje para email inexistente y contraseña mala: no se filtra
  // qué correos están registrados.
  const ok = user && await verifyPassword(String(body.password || ""), user.password_hash);
  if (!ok) return fail("credenciales incorrectas", cors, 401);
  const token = await signToken({ sub: user.id, role: user.role }, authSecret(env));
  return json({ token, user: publicUser(user) }, cors);
}

// --------------------------------------------------------------- songs ----
async function readBody(env, song) {
  const obj = await env.BUCKET.get(song.r2_key);
  return obj ? await obj.text() : "";
}

async function createSong(request, env, cors, user) {
  const body = await readJson(request);
  if (!body) return fail("cuerpo JSON no válido", cors, 400);
  // Publicar es un acto editorial: quien sube una partitura la crea privada y,
  // si quiere que salga en el catálogo, lo propone (POST .../proposals).
  const pedida = isValidVisibility(body.visibility) ? body.visibility : "private";
  const visibility = canSetVisibility(user) ? pedida : "private";
  const r2Key = `${SONG_PREFIX}${uuid()}.txt`;
  await env.BUCKET.put(r2Key, String(body.content || ""), {
    httpMetadata: { contentType: "text/plain; charset=utf-8" }
  });
  if (!isValidYoutube(body.youtubeUrl)) return fail("el enlace de YouTube no es válido", cors, 400);
  const song = await insertSong(env.DB, {
    owner_id: user.id, r2_key: r2Key,
    title: body.title, artist: body.artist, genre: body.genre,
    capo: Number(body.capo) || 0, source_url: body.sourceUrl,
    youtube_url: String(body.youtubeUrl || "").trim(),
    locked: !!body.locked, visibility
  });
  return json({ song: publicSong(song, true) }, cors, 201);
}

async function getSong(env, cors, user, id) {
  const song = await findSongById(env.DB, id);
  if (!canView(user, song)) return fail("no encontrada", cors, 404);
  return json({ song: publicSong(song, canEdit(user, song)), content: await readBody(env, song) }, cors);
}

async function updateSong(request, env, cors, user, id) {
  const song = await findSongById(env.DB, id);
  const denial = editDenialReason(user, song);
  if (denial === "not_found") return fail("no encontrada", cors, 404);
  if (denial === "unauthorized") return fail("inicia sesión", cors, 401);
  if (denial) return fail("no puedes modificar esta partitura", cors, 403);

  const body = await readJson(request);
  if (!body) return fail("cuerpo JSON no válido", cors, 400);
  if (typeof body.content === "string") {
    await env.BUCKET.put(song.r2_key, body.content, {
      httpMetadata: { contentType: "text/plain; charset=utf-8" }
    });
  }
  // Igual que al crear: un cambio de visibilidad de quien no es editor se
  // ignora en silencio, no se rechaza la edición entera.
  const pedida = isValidVisibility(body.visibility) ? body.visibility : song.visibility;
  const visibility = canSetVisibility(user) ? pedida : song.visibility;
  if (body.youtubeUrl !== undefined && !isValidYoutube(body.youtubeUrl)) {
    return fail("el enlace de YouTube no es válido", cors, 400);
  }
  const updated = await updateSongMeta(env.DB, id, {
    youtube_url: body.youtubeUrl !== undefined
      ? String(body.youtubeUrl || "").trim() : (song.youtube_url || ""),
    title: body.title ?? song.title,
    artist: body.artist ?? song.artist,
    genre: body.genre ?? song.genre,
    capo: body.capo ?? song.capo,
    source_url: body.sourceUrl ?? song.source_url,
    locked: body.locked ?? !!song.locked,
    visibility
  });
  return json({ song: publicSong(updated, true) }, cors);
}

async function deleteSong(env, cors, user, id) {
  const song = await findSongById(env.DB, id);
  const denial = editDenialReason(user, song);
  if (denial === "not_found") return fail("no encontrada", cors, 404);
  if (denial === "unauthorized") return fail("inicia sesión", cors, 401);
  if (denial) return fail("no puedes borrar esta partitura", cors, 403);
  await softDeleteSong(env.DB, id);
  return json({ id, deleted: true }, cors);
}

/** Catálogo público: por defecto, lo publicado por el administrador. */
/**
 * De quién es el catálogo que se enseña: por defecto, del administrador más
 * antiguo; `?owner=all` enseña lo publicado por todo el mundo.
 */
async function catalogOwnerId(env, url) {
  const owner = url.searchParams.get("owner");
  if (owner === "all") return null;
  if (owner) return owner;
  const admin = await env.DB.prepare(
    "SELECT id FROM users WHERE role = 'admin' ORDER BY created_at ASC LIMIT 1"
  ).first();
  return admin?.id || null;
}

async function publicCatalog(env, cors, url) {
  const genre = url.searchParams.get("genre") || "";
  const sortPedido = url.searchParams.get("sort") || "title";
  const sort = isValidSort(sortPedido) ? sortPedido : "title";
  const ownerId = await catalogOwnerId(env, url);
  const songs = await listPublicSongs(env.DB, ownerId, { genre, sort });
  return json({ songs: songs.map(publicSong), sort, genre }, cors);
}

/* ---------------------------- versiones ---------------------------- */

async function createVersion(request, env, cors, user, song) {
  const body = await readJson(request);
  if (!body) return fail("cuerpo JSON no válido", cors, 400);
  const r2Key = `${SONG_PREFIX}${uuid()}.txt`;
  await env.BUCKET.put(r2Key, String(body.content || ""), {
    httpMetadata: { contentType: "text/plain; charset=utf-8" }
  });
  const version = await insertVersion(env.DB, {
    song_id: song.id, name: body.name, r2_key: r2Key,
    capo: Number(body.capo) || 0, source_url: body.sourceUrl,
    author_id: user.id
  });
  return json({ version: publicVersion(version) }, cors, 201);
}

async function updateVersion(request, env, cors, version) {
  const body = await readJson(request);
  if (!body) return fail("cuerpo JSON no válido", cors, 400);
  if (typeof body.content === "string") {
    await env.BUCKET.put(version.r2_key, body.content, {
      httpMetadata: { contentType: "text/plain; charset=utf-8" }
    });
  }
  const actualizada = await updateVersionMeta(env.DB, version.id, {
    name: body.name ?? version.name,
    capo: body.capo ?? version.capo,
    source_url: body.sourceUrl ?? version.source_url,
    position: body.position ?? version.position
  });
  return json({ version: publicVersion(actualizada) }, cors);
}

/* --------------------------- comentarios --------------------------- */

const MAX_COMENTARIO = 2000;

async function createComment(request, env, cors, user, song) {
  const body = await readJson(request);
  if (!body) return fail("cuerpo JSON no válido", cors, 400);
  const texto = String(body.body || "").trim();
  if (!texto) return fail("el comentario está vacío", cors, 400);
  if (texto.length > MAX_COMENTARIO) {
    return fail("el comentario no puede pasar de " + MAX_COMENTARIO + " caracteres", cors, 400);
  }
  const comentario = await insertComment(env.DB, {
    song_id: song.id, author_id: user.id, body: texto
  });
  return json({ comment: publicComment(comentario) }, cors, 201);
}

/* -------------------------- valoraciones -------------------------- */

/**
 * Guarda o cambia el voto de esta persona. Un cero retira el voto, que es
 * lo que espera quien vuelve a pulsar la estrella que ya tenía marcada.
 */
async function rateVersion(request, env, cors, user, song) {
  const body = await readJson(request);
  if (!body) return fail("cuerpo JSON no válido", cors, 400);
  const stars = Number(body.stars);
  if (!Number.isInteger(stars) || stars < 0 || stars > 5) {
    return fail("la puntuación va de 0 a 5", cors, 400);
  }
  // El "Original" es la cadena vacía; cualquier otra cosa tiene que ser una
  // versión viva de ESTA partitura, no de otra.
  const versionId = String(body.versionId || "");
  if (versionId) {
    const version = await findVersionById(env.DB, versionId);
    if (!version || version.song_id !== song.id) return fail("esa versión no existe", cors, 404);
  }
  const clave = { songId: song.id, versionId, userId: user.id };
  if (stars === 0) await deleteRating(env.DB, clave);
  else await setRating(env.DB, { ...clave, stars });

  return json({
    ratings: await listRatings(env.DB, song.id),
    mine: await listUserRatings(env.DB, song.id, user.id)
  }, cors);
}

/* ---------------------------- propuestas ---------------------------- */

async function createProposal(request, env, cors, user, song) {
  const body = await readJson(request);
  if (!body) return fail("cuerpo JSON no válido", cors, 400);
  const kind = body.kind === "version" ? "version" : "publish";
  if (!canPropose(user, song, kind)) {
    return fail(kind === "publish"
      ? "solo puedes proponer la publicación de una partitura tuya que no esté publicada"
      : "solo se pueden proponer versiones de partituras publicadas", cors, 403);
  }
  // El texto propuesto no toca la partitura original: vive en su propio objeto
  // y solo se convierte en versión si alguien la aprueba.
  let r2Key = "";
  if (kind === "version") {
    if (!String(body.content || "").trim()) return fail("la versión necesita contenido", cors, 400);
    r2Key = `${SONG_PREFIX}${uuid()}.txt`;
    await env.BUCKET.put(r2Key, String(body.content), {
      httpMetadata: { contentType: "text/plain; charset=utf-8" }
    });
  }
  const propuesta = await insertProposal(env.DB, {
    kind, song_id: song.id, author_id: user.id,
    name: body.name, capo: Number(body.capo) || 0, source_url: body.sourceUrl,
    r2_key: r2Key, note: body.note
  });
  return json({ proposal: publicProposal(propuesta) }, cors, 201);
}

/**
 * Aprobar aplica el cambio y deja constancia de quién lo hizo: publicar la
 * partitura, o convertir el texto propuesto en una versión más.
 */
async function approveProposal(request, env, cors, user, proposal) {
  if (proposal.status !== "pending") return fail("esa propuesta ya está resuelta", cors, 409);
  const body = await readJson(request) || {};
  const song = await findSongById(env.DB, proposal.song_id);
  if (!song || song.deleted_at) return fail("la partitura ya no existe", cors, 404);

  if (proposal.kind === "publish") {
    await updateSongMeta(env.DB, song.id, {
      title: song.title, artist: song.artist, genre: song.genre, capo: song.capo,
      source_url: song.source_url, youtube_url: song.youtube_url,
      locked: !!song.locked, visibility: "public"
    });
  } else {
    await insertVersion(env.DB, {
      song_id: song.id,
      name: proposal.name || "Versión propuesta",
      r2_key: proposal.r2_key,
      capo: proposal.capo,
      source_url: proposal.source_url,
      author_id: proposal.author_id      // el crédito es de quien la propuso
    });
  }
  const resuelta = await resolveProposal(env.DB, proposal.id, {
    status: "approved", reviewerId: user.id, reviewNote: String(body.note || "")
  });
  return json({ proposal: publicProposal(resuelta) }, cors);
}

/**
 * Enruta /auth/* y /api/*. Devuelve null si la ruta no es de esta API, para
 * que el Worker siga con los endpoints heredados (token compartido).
 */
export async function handleApi(request, env, url, cors) {
  const path = url.pathname;
  if (!path.startsWith("/auth/") && !path.startsWith("/api/")) return null;
  if (!env.DB) return fail("la base de datos no está configurada", cors, 503);

  const method = request.method;

  if (path === "/auth/register" && method === "POST") return register(request, env, cors);
  if (path === "/auth/login" && method === "POST") return login(request, env, cors);

  // Catálogo publicado: accesible sin sesión (es la portada de la web).
  if (path === "/api/songs/public" && method === "GET") return publicCatalog(env, cors, url);

  const user = await currentUser(request, env);

  if (path === "/auth/me" && method === "GET") {
    if (!user) return fail("sin sesión", cors, 401);
    return json({ user: publicUser(user) }, cors);
  }

  if (path === "/api/songs" && method === "GET") {
    if (!user) return fail("inicia sesión", cors, 401);
    const songs = await listOwnSongs(env.DB, user.id);
    return json({ songs: songs.map((s) => publicSong(s, true)) }, cors);
  }

  // Diccionario global de acordes: lo lee cualquiera (los diagramas son parte
  // de leer la partitura, también sin cuenta) y solo lo escribe el admin.
  // Va ANTES de /api/chords para que la ruta más específica gane.
  if (path === "/api/chords/global" && method === "GET") {
    const dict = await readGlobalChords(env);
    return json(dict, cors);
  }
  if (path === "/api/chords/global" && method === "PUT") {
    if (!user) return fail("inicia sesión", cors, 401);
    if (!canEditChords(user)) return fail("hace falta ser editor", cors, 403);
    let body;
    try {
      body = await request.json();
    } catch (e) {
      return fail("cuerpo JSON no válido", cors, 400);
    }
    try {
      const limpio = sanitizeDictionary(body);
      const guardado = await writeGlobalChords(env, limpio);
      return json({ ok: true, count: Object.keys(limpio).length, updatedAt: guardado.updatedAt }, cors);
    } catch (e) {
      if (e instanceof ChordError) return fail(e.message, cors, 400);
      throw e;
    }
  }
  // Carga el diccionario base sin pisar lo que el admin ya haya tocado.
  if (path === "/api/chords/global/seed" && method === "POST") {
    if (!user) return fail("inicia sesión", cors, 401);
    if (!canEditChords(user)) return fail("hace falta ser editor", cors, 403);
    const actual = await readGlobalChords(env);
    const { chords, added, kept } = mergeSeed(actual.chords, sanitizeDictionary(CHORD_SEED));
    const guardado = await writeGlobalChords(env, chords);
    return json({ ok: true, added, kept, count: Object.keys(chords).length, updatedAt: guardado.updatedAt }, cors);
  }

  // Acordes personalizados del usuario: un blob JSON por cuenta, igual que
  // hacía el flujo antiguo pero ya separado por usuario.
  if (path === "/api/chords") {
    if (!user) return fail("inicia sesión", cors, 401);
    const key = `users/${user.id}/chords.json`;
    if (method === "GET") {
      const obj = await env.BUCKET.get(key);
      if (!obj) return json({ version: 1, updatedAt: 0, chords: [] }, cors);
      return new Response(obj.body, {
        headers: { ...cors, "Content-Type": "application/json; charset=utf-8" }
      });
    }
    if (method === "PUT") {
      const raw = await request.text();
      await env.BUCKET.put(key, raw, {
        httpMetadata: { contentType: "application/json; charset=utf-8" }
      });
      return json({ ok: true, updatedAt: Date.now() }, cors);
    }
  }
  if (path === "/api/songs" && method === "POST") {
    if (!user) return fail("inicia sesión", cors, 401);
    return createSong(request, env, cors, user);
  }

  // Clasificación automática del catálogo. Va por tandas, como la migración:
  // el Worker tiene un tope de subpeticiones por invocación.
  if (path === "/api/genres/auto" && method === "POST") {
    if (!canReview(user)) return fail("hace falta ser editor", cors, 403);
    const body = await readJson(request) || {};
    const limit = Math.min(Math.max(Number(body.limit) || 200, 1), 500);
    const fallback = String(body.fallback || FALLBACK_GENRE);
    const overwrite = body.overwrite === true;
    const dryRun = body.dryRun !== false;            // por defecto no escribe

    const filas = await listSongsAfter(env.DB, String(body.cursor || ""), limit);
    const cambios = [];
    const tally = {};
    for (const fila of filas) {
      if (fila.genre && !overwrite) continue;
      const genre = guessGenre(fila, fallback);
      if (genre === fila.genre) continue;
      tally[genre] = (tally[genre] || 0) + 1;
      cambios.push({ id: fila.id, genre });
    }
    if (!dryRun) await setGenres(env.DB, cambios);

    const ultima = filas.length ? filas[filas.length - 1].id : "";
    return json({
      scanned: filas.length,
      updated: dryRun ? 0 : cambios.length,
      wouldUpdate: cambios.length,
      tally,
      dryRun,
      done: filas.length < limit,
      cursor: ultima
    }, cors);
  }

  // Las que aún no tienen vídeo, con su búsqueda de YouTube ya montada.
  if (path === "/api/songs/without-video" && method === "GET") {
    if (!canReview(user)) return fail("hace falta ser editor", cors, 403);
    const filas = await listSongsWithoutVideo(env.DB, Number(url.searchParams.get("limit")) || 500);
    return json({
      songs: filas.map((s) => ({
        id: s.id, title: s.title, artist: s.artist, genre: s.genre,
        visibility: s.visibility, search: youtubeSearch(s)
      }))
    }, cors);
  }

  // Categorías con partituras publicadas, para el filtro del catálogo.
  if (path === "/api/genres" && method === "GET") {
    return json({ genres: await listPublicGenres(env.DB, await catalogOwnerId(env, url)) }, cors);
  }

  // Versiones de una partitura: se ven con las mismas reglas que ella.
  const versionesDe = path.match(/^\/api\/songs\/([A-Za-z0-9-]+)\/versions$/);
  if (versionesDe) {
    const song = await findSongById(env.DB, versionesDe[1]);
    if (!canView(user, song)) return fail("no encontrada", cors, 404);
    if (method === "GET") {
      const versions = await listVersions(env.DB, song.id);
      return json({ versions: versions.map(publicVersion) }, cors);
    }
    if (method === "POST") {
      if (!user) return fail("inicia sesión", cors, 401);
      if (!canAddVersion(user, song)) return fail("propón la versión en vez de añadirla", cors, 403);
      return createVersion(request, env, cors, user, song);
    }
  }

  const versionSuelta = path.match(/^\/api\/versions\/([A-Za-z0-9-]+)$/);
  if (versionSuelta) {
    const version = await findVersionById(env.DB, versionSuelta[1]);
    if (!version) return fail("no encontrada", cors, 404);
    const song = await findSongById(env.DB, version.song_id);
    if (method === "GET") {
      if (!canView(user, song)) return fail("no encontrada", cors, 404);
      return json({ version: publicVersion(version), content: await readBody(env, version) }, cors);
    }
    if (!user) return fail("inicia sesión", cors, 401);
    if (!canEdit(user, song)) return fail("no puedes tocar esta versión", cors, 403);
    if (method === "PUT") return updateVersion(request, env, cors, version);
    if (method === "DELETE") {
      await softDeleteVersion(env.DB, version.id);
      return json({ id: version.id, deleted: true }, cors);
    }
  }

  // Comentarios de una partitura: se leen con las mismas reglas que ella.
  const comentariosDe = path.match(/^\/api\/songs\/([A-Za-z0-9-]+)\/comments$/);
  if (comentariosDe) {
    const song = await findSongById(env.DB, comentariosDe[1]);
    if (!canView(user, song)) return fail("no encontrada", cors, 404);
    if (method === "GET") {
      const comments = await listComments(env.DB, song.id);
      return json({ comments: comments.map(publicComment) }, cors);
    }
    if (method === "POST") {
      if (!canComment(user, song)) return fail("inicia sesión para comentar", cors, 401);
      return createComment(request, env, cors, user, song);
    }
  }

  const comentario = path.match(/^\/api\/comments\/([A-Za-z0-9-]+)$/);
  if (comentario && method === "DELETE") {
    if (!user) return fail("inicia sesión", cors, 401);
    const registro = await findCommentById(env.DB, comentario[1]);
    if (!registro) return fail("no encontrado", cors, 404);
    if (!canDeleteComment(user, registro)) return fail("no puedes borrar este comentario", cors, 403);
    await softDeleteComment(env.DB, registro.id);
    return json({ id: registro.id, deleted: true }, cors);
  }

  // Valoraciones por versión. Las medias las ve cualquiera; votar pide sesión.
  const valoracionesDe = path.match(/^\/api\/songs\/([A-Za-z0-9-]+)\/ratings$/);
  if (valoracionesDe) {
    const song = await findSongById(env.DB, valoracionesDe[1]);
    if (!canView(user, song)) return fail("no encontrada", cors, 404);
    if (method === "GET") {
      return json({
        ratings: await listRatings(env.DB, song.id),
        mine: user ? await listUserRatings(env.DB, song.id, user.id) : {}
      }, cors);
    }
    if (method === "PUT") {
      if (!canRate(user, song)) return fail("inicia sesión para valorar", cors, 401);
      return rateVersion(request, env, cors, user, song);
    }
  }

  // Propuestas sobre una partitura.
  const propuestasDe = path.match(/^\/api\/songs\/([A-Za-z0-9-]+)\/proposals$/);
  if (propuestasDe && method === "POST") {
    if (!user) return fail("inicia sesión", cors, 401);
    const song = await findSongById(env.DB, propuestasDe[1]);
    if (!canView(user, song)) return fail("no encontrada", cors, 404);
    return createProposal(request, env, cors, user, song);
  }

  // Cola de revisión (editores) o las propias (cualquiera con sesión).
  if (path === "/api/proposals" && method === "GET") {
    if (!user) return fail("inicia sesión", cors, 401);
    const status = url.searchParams.get("status") || "pending";
    const mias = url.searchParams.get("mine") === "1" || !canReview(user);
    const proposals = await listProposals(env.DB, {
      status,
      authorId: mias ? user.id : null
    });
    return json({
      proposals: proposals.map(publicProposal),
      pending: canReview(user) ? await countPendingProposals(env.DB) : undefined
    }, cors);
  }

  const propuesta = path.match(/^\/api\/proposals\/([A-Za-z0-9-]+)(\/approve|\/reject)?$/);
  if (propuesta) {
    if (!user) return fail("inicia sesión", cors, 401);
    const registro = await findProposalById(env.DB, propuesta[1]);
    if (!registro) return fail("no encontrada", cors, 404);
    const accion = propuesta[2];
    if (method === "POST" && accion === "/approve") {
      if (!canReview(user)) return fail("hace falta ser editor", cors, 403);
      return approveProposal(request, env, cors, user, registro);
    }
    if (method === "POST" && accion === "/reject") {
      if (!canReview(user)) return fail("hace falta ser editor", cors, 403);
      const body = await readJson(request) || {};
      const resuelta = await resolveProposal(env.DB, registro.id, {
        status: "rejected", reviewerId: user.id, reviewNote: String(body.note || "")
      });
      return json({ proposal: publicProposal(resuelta) }, cors);
    }
    if (method === "DELETE" && !accion) {
      if (!canWithdrawProposal(user, registro)) return fail("no puedes retirarla", cors, 403);
      const resuelta = await resolveProposal(env.DB, registro.id, {
        status: "withdrawn", reviewerId: user.id, reviewNote: ""
      });
      return json({ proposal: publicProposal(resuelta) }, cors);
    }
    if (method === "GET") {
      if (registro.author_id !== user.id && !canReview(user)) return fail("no encontrada", cors, 404);
      return json({
        proposal: publicProposal(registro),
        content: registro.r2_key ? await readBody(env, registro) : ""
      }, cors);
    }
  }

  // Usuarios y roles: solo administración.
  if (path === "/api/users" && method === "GET") {
    if (!canManageRoles(user)) return fail("solo el administrador", cors, 403);
    return json({ users: (await listUsers(env.DB)).map(publicUser) }, cors);
  }
  const rolDe = path.match(/^\/api\/users\/([A-Za-z0-9-]+)\/role$/);
  if (rolDe && method === "PUT") {
    if (!canManageRoles(user)) return fail("solo el administrador", cors, 403);
    const body = await readJson(request);
    if (!body || !isValidRole(body.role)) return fail("rol no válido", cors, 400);
    if (rolDe[1] === user.id) return fail("no puedes cambiar tu propio rol", cors, 400);
    const actualizado = await updateUserRole(env.DB, rolDe[1], body.role);
    if (!actualizado) return fail("no encontrado", cors, 404);
    return json({ user: publicUser(actualizado) }, cors);
  }

  const match = path.match(/^\/api\/songs\/([A-Za-z0-9-]+)$/);
  if (match) {
    const id = match[1];
    if (method === "GET") return getSong(env, cors, user, id);      // público si lo es
    if (method === "PUT") return updateSong(request, env, cors, user, id);
    if (method === "DELETE") return deleteSong(env, cors, user, id);
  }

  return fail("ruta no encontrada", cors, 404);
}
