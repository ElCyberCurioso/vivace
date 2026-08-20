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
  SONG_PREFIX, createUser, countUsers, findSongById, findUserByEmail, findUserById,
  insertSong, listOwnSongs, listPublicSongs, publicSong, publicUser, softDeleteSong,
  updateSongMeta, uuid
} from "./db.js";
import { canEdit, canView, editDenialReason, isValidVisibility } from "./permissions.js";
import {
  ChordError, mergeSeed, readGlobalChords, sanitizeDictionary, writeGlobalChords
} from "./chords.js";
import { CHORD_SEED } from "./chords-seed.js";

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
  const visibility = isValidVisibility(body.visibility) ? body.visibility : "private";
  const r2Key = `${SONG_PREFIX}${uuid()}.txt`;
  await env.BUCKET.put(r2Key, String(body.content || ""), {
    httpMetadata: { contentType: "text/plain; charset=utf-8" }
  });
  const song = await insertSong(env.DB, {
    owner_id: user.id, r2_key: r2Key,
    title: body.title, artist: body.artist, genre: body.genre,
    capo: Number(body.capo) || 0, source_url: body.sourceUrl,
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
  const visibility = isValidVisibility(body.visibility) ? body.visibility : song.visibility;
  const updated = await updateSongMeta(env.DB, id, {
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
async function publicCatalog(env, cors, url) {
  const owner = url.searchParams.get("owner");
  let ownerId = owner || null;
  if (!ownerId) {
    const admin = await env.DB.prepare(
      "SELECT id FROM users WHERE role = 'admin' ORDER BY created_at ASC LIMIT 1"
    ).first();
    ownerId = admin?.id || null;
  }
  if (owner === "all") ownerId = null;
  const songs = await listPublicSongs(env.DB, ownerId);
  return json({ songs: songs.map(publicSong) }, cors);
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
    if (user.role !== "admin") return fail("solo el administrador", cors, 403);
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
    if (user.role !== "admin") return fail("solo el administrador", cors, 403);
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

  const match = path.match(/^\/api\/songs\/([A-Za-z0-9-]+)$/);
  if (match) {
    const id = match[1];
    if (method === "GET") return getSong(env, cors, user, id);      // público si lo es
    if (method === "PUT") return updateSong(request, env, cors, user, id);
    if (method === "DELETE") return deleteSong(env, cors, user, id);
  }

  return fail("ruta no encontrada", cors, 404);
}
