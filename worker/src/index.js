/*
 * GuitarChords R2 sync Worker
 * ===========================
 * Fronts a Cloudflare R2 bucket so the Android app never holds S3 credentials.
 *
 * API endpoints (all require  Authorization: Bearer <SYNC_TOKEN>):
 *   GET    /list                -> JSON array: [{ key, etag, size, uploaded, title }]
 *   GET    /object?key=<key>    -> raw text body; headers ETag + X-Uploaded
 *   PUT    /object?key=<key>    -> stores body; returns { key, etag, size, uploaded }
 *   DELETE /object?key=<key>    -> removes object; returns { key, deleted: true }
 *   POST   /delete              -> body { keys: [...] }; bulk delete; returns
 *                                  { deleted: <n>, keys: [...] }
 *
 * The PUT handler parses the song's `#title:` header out of the body and stores
 * it in R2 customMetadata, so /list can show titles without reading every body.
 *
 * Admin UI:
 *   GET  /                      -> HTML page to manage files (asks for the token).
 *                                  The page is public; every API call it makes
 *                                  still carries the Bearer token.
 *
 * Deploy:
 *   1. npx wrangler r2 bucket create <bucket_name>   (if it does not exist)
 *   2. npx wrangler secret put SYNC_TOKEN            (choose a long random token)
 *   3. npx wrangler deploy
 *   The printed *.workers.dev URL + the token go into the app's sync screen,
 *   and the same URL opened in a browser shows the admin UI.
 */

import { ADMIN_HTML } from "./admin-html.js";
import { WEB_HTML, FAVICON_SVG } from "./web-html.js";
import { CLIENT_JS } from "./client-lib.js";
import { handleApi } from "./api.js";
import { migrateExistingSongs } from "./migrate.js";

const SONG_PREFIX = "songs/";
// Los acordes personalizados se guardan en un blob bajo este prefijo, fuera del
// flujo de partituras (no aparecen en /list, que filtra por songs/).
const CHORDS_PREFIX = "chords/";
// Binarios de la app (APK + metadatos de versión) para la auto-actualización.
const APP_PREFIX = "app/";
const TITLE_RE = /^#([A-Za-z]+):[ \t]?(.*)$/;

/** Claves que el Worker acepta escribir/borrar: partituras, acordes o app. */
function isAllowedKey(key) {
  return typeof key === "string" &&
    (key.startsWith(SONG_PREFIX) || key.startsWith(CHORDS_PREFIX) ||
     key.startsWith(APP_PREFIX));
}

export default {
  async fetch(request, env) {
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, PUT, POST, DELETE, OPTIONS",
      "Access-Control-Allow-Headers": "Authorization, Content-Type",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: cors });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    // --- Vivace web (pública): catálogo, visor y edición con sesión propia ---
    if (path === "/" && request.method === "GET") {
      return new Response(WEB_HTML, {
        headers: { "Content-Type": "text/html; charset=utf-8" },
      });
    }
    if (path === "/static/favicon.svg" && request.method === "GET") {
      return new Response(FAVICON_SVG, {
        headers: {
          "Content-Type": "image/svg+xml; charset=utf-8",
          "Cache-Control": "public, max-age=86400",
        },
      });
    }
    if (path === "/static/vivace.js" && request.method === "GET") {
      return new Response(CLIENT_JS, {
        headers: {
          "Content-Type": "application/javascript; charset=utf-8",
          "Cache-Control": "public, max-age=300",
        },
      });
    }

    // --- Panel de administración (token compartido; ahora en /admin) ---
    if ((path === "/admin" || path === "/admin/") && request.method === "GET") {
      // STORAGE_URL ([vars] en wrangler.toml) apunta al bucket en el dashboard
      // de Cloudflare; si no está definida, el botón Storage no se muestra.
      const html = ADMIN_HTML.replace("__STORAGE_URL__", env.STORAGE_URL || "");
      return new Response(html, {
        headers: { "Content-Type": "text/html; charset=utf-8" },
      });
    }

    // --- Auto-actualización (público, sin token): la app consulta la versión
    //     disponible y descarga el APK. Se sube a R2 en app/latest.json y
    //     app/app-release.apk (p. ej. con `wrangler r2 object put`). ---
    if (path === "/update" && request.method === "GET") {
      const obj = await env.BUCKET.get(APP_PREFIX + "latest.json");
      if (!obj) return new Response("{}", { status: 404, headers: { ...cors, "Content-Type": "application/json" } });
      return new Response(obj.body, { headers: { ...cors, "Content-Type": "application/json" } });
    }
    if (path === "/update/apk" && request.method === "GET") {
      const obj = await env.BUCKET.get(APP_PREFIX + "app-release.apk");
      if (!obj) return new Response("Not found", { status: 404, headers: cors });
      return new Response(obj.body, {
        headers: {
          ...cors,
          "Content-Type": "application/vnd.android.package-archive",
          "Content-Disposition": "attachment; filename=app-release.apk",
        },
      });
    }

    // --- API multiusuario (Vivace): /auth/* y /api/* con sesión propia ---
    try {
      const apiResponse = await handleApi(request, env, url, cors);
      if (apiResponse) return apiResponse;
    } catch (err) {
      return new Response(JSON.stringify({ error: String(err && err.message) }), {
        status: 500, headers: { ...cors, "Content-Type": "application/json" }
      });
    }

    // Indexa en D1 lo que ya había en R2, como partituras del admin que llama.
    if (path === "/admin/migrate" && request.method === "POST") {
      if (!env.DB) return json({ error: "sin base de datos" }, cors, 503);
      const { currentUser } = await import("./api.js");
      const user = await currentUser(request, env);
      if (!user || user.role !== "admin") {
        return json({ error: "solo el administrador" }, cors, 403);
      }
      const visibility = url.searchParams.get("visibility") === "public" ? "public" : "private";
      // Por tandas: el tope de subpeticiones del Worker no da para un catalogo
      // entero de una vez. Se repite mientras la respuesta traiga done:false.
      const limit = Number(url.searchParams.get("limit")) || undefined;
      const cursor = url.searchParams.get("cursor") || undefined;
      try {
        const result = await migrateExistingSongs(env, user.id, visibility, { limit, cursor });
        return json(result, cors);
      } catch (err) {
        // Sin esto la excepcion sale como un 500 sin cuerpo y no hay forma de
        // saber si fue el tope de subpeticiones, R2 o la base.
        return json({ error: String(err && err.message), imported: 0 }, cors, 500);
      }
    }

    // --- Endpoints heredados (token compartido) ---
    // Siguen vivos para que la app Android actual no deje de sincronizar
    // mientras se migra al login; se retirarán al completar esa migración.
    const expected = `Bearer ${env.SYNC_TOKEN}`;
    if (!env.SYNC_TOKEN || request.headers.get("Authorization") !== expected) {
      return new Response("Unauthorized", { status: 401, headers: cors });
    }

    try {
      if (path === "/list" && request.method === "GET") {
        return await listObjects(env, cors);
      }
      if (path === "/bodies" && request.method === "GET") {
        return await listBodies(env, cors);
      }
      if (path === "/object" && request.method === "GET") {
        return await getObject(env, url, cors);
      }
      if (path === "/object" && request.method === "PUT") {
        return await putObject(env, url, request, cors);
      }
      if (path === "/object" && request.method === "DELETE") {
        return await deleteObject(env, url, cors);
      }
      if (path === "/delete" && request.method === "POST") {
        return await deleteObjects(env, request, cors);
      }
      return new Response("Not found", { status: 404, headers: cors });
    } catch (err) {
      return new Response("Error: " + (err && err.message), {
        status: 500,
        headers: cors,
      });
    }
  },
};

/** Pull the indexed header values (title/artist/capo/url/locked) out of a song body. */
function parseHeaders(text) {
  const lines = text.replace(/\r\n/g, "\n").split("\n");
  const h = { title: "", artist: "", capo: "", url: "", locked: "" };
  for (const line of lines) {
    if (line.trim() === "---") break;
    const m = TITLE_RE.exec(line);
    if (!m) break;
    const k = m[1].toLowerCase();
    if (k === "title") h.title = m[2].trim();
    else if (k === "artist") h.artist = m[2].trim();
    else if (k === "capo") h.capo = m[2].trim();
    else if (k === "url") h.url = m[2].trim();
    else if (k === "locked") h.locked = m[2].trim().toLowerCase() === "true" ? "true" : "";
  }
  return h;
}

async function listObjects(env, cors) {
  const out = [];
  let cursor = undefined;
  do {
    const page = await env.BUCKET.list({
      prefix: SONG_PREFIX,
      cursor,
      include: ["customMetadata"],
    });
    for (const o of page.objects) {
      const cm = o.customMetadata || {};
      const uploadedMs = o.uploaded ? new Date(o.uploaded).getTime() : 0;
      out.push({
        key: o.key,
        etag: o.httpEtag || o.etag || "",
        size: o.size || 0,
        uploaded: uploadedMs,
        // Ficheros antiguos sin metadata "created": se aproxima con uploaded.
        created: Number(cm.created) || uploadedMs,
        title: cm.title || "",
        artist: cm.artist || "",
        capo: cm.capo || "",
        url: cm.url || "",
        locked: cm.locked || "",
      });
    }
    cursor = page.truncated ? page.cursor : undefined;
  } while (cursor);

  return json(out, cors);
}

// Índice de búsqueda por contenido en UNA sola respuesta: { key: cuerpo, ... }.
// Lo usa el modal "Buscar" para no tener que pedir cada partitura por separado.
async function listBodies(env, cors) {
  const out = {};
  let cursor = undefined;
  do {
    const page = await env.BUCKET.list({ prefix: SONG_PREFIX, cursor });
    for (const o of page.objects) {
      const obj = await env.BUCKET.get(o.key);
      if (obj) out[o.key] = await obj.text();
    }
    cursor = page.truncated ? page.cursor : undefined;
  } while (cursor);
  return json(out, cors);
}

async function getObject(env, url, cors) {
  const key = url.searchParams.get("key");
  if (!key) return new Response("key required", { status: 400, headers: cors });

  const obj = await env.BUCKET.get(key);
  if (!obj) return new Response("Not found", { status: 404, headers: cors });

  const uploaded = obj.uploaded ? new Date(obj.uploaded).getTime() : Date.now();
  return new Response(obj.body, {
    headers: {
      ...cors,
      "Content-Type": "text/plain; charset=utf-8",
      "ETag": obj.httpEtag || "",
      "X-Uploaded": String(uploaded),
    },
  });
}

async function putObject(env, url, request, cors) {
  const key = url.searchParams.get("key");
  if (!isAllowedKey(key)) {
    return new Response("invalid key", { status: 400, headers: cors });
  }
  const body = await request.text();
  const h = parseHeaders(body);
  // La fecha de creación se conserva entre ediciones: se hereda de la metadata
  // existente y solo se fija en el primer PUT del objeto.
  const existing = await env.BUCKET.head(key);
  const created =
    (existing && existing.customMetadata && existing.customMetadata.created) ||
    String(Date.now());
  const obj = await env.BUCKET.put(key, body, {
    httpMetadata: { contentType: "text/plain; charset=utf-8" },
    customMetadata: {
      title: h.title, artist: h.artist, capo: h.capo, url: h.url,
      locked: h.locked, created,
    },
  });
  return json(
    {
      key,
      etag: obj.httpEtag || "",
      size: obj.size || body.length,
      uploaded: obj.uploaded ? new Date(obj.uploaded).getTime() : Date.now(),
      created: Number(created) || 0,
      title: h.title,
      artist: h.artist,
      capo: h.capo,
      url: h.url,
      locked: h.locked,
    },
    cors
  );
}

async function deleteObject(env, url, cors) {
  const key = url.searchParams.get("key");
  if (!isAllowedKey(key)) {
    return new Response("invalid key", { status: 400, headers: cors });
  }
  await env.BUCKET.delete(key);
  return json({ key, deleted: true }, cors);
}

async function deleteObjects(env, request, cors) {
  let keys;
  try {
    keys = (await request.json()).keys;
  } catch (e) {
    return new Response("invalid JSON", { status: 400, headers: cors });
  }
  if (!Array.isArray(keys) || keys.length === 0 ||
      !keys.every(isAllowedKey)) {
    return new Response("invalid keys", { status: 400, headers: cors });
  }
  // R2 admits hasta 1000 claves por llamada; se trocea por si acaso.
  for (let i = 0; i < keys.length; i += 1000) {
    await env.BUCKET.delete(keys.slice(i, i + 1000));
  }
  return json({ deleted: keys.length, keys }, cors);
}

function json(data, cors) {
  return new Response(JSON.stringify(data), {
    headers: { ...cors, "Content-Type": "application/json" },
  });
}

