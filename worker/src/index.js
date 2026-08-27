/*
 * Vivace · Worker de Cloudflare
 * =============================
 * Una sola pieza hace de web, de API multiusuario y de almacén (D1 + R2).
 *
 *   GET  /                      Web de Vivace (pública)
 *   GET  /static/*              CSS, JS y favicon (cacheados, con ETag)
 *   POST /auth/*                Registro y sesión
 *   *    /api/*                 API multiusuario (ver src/api.js y src/sync.js)
 *   POST /admin/migrate         Indexa en D1 lo que ya había en R2 (solo admin)
 *   GET  /update, /update/apk   Auto-actualización de la app (público)
 *
 * Las rutas heredadas con token compartido (/list, /object, /bodies, /delete) y
 * el panel /admin se retiraron: se saltaban el modelo de permisos por completo
 * —con un único token se leía, sobrescribía y borraba el texto de CUALQUIER
 * partitura, incluidas las privadas de otras cuentas—. Todo pasa ahora por la
 * sesión del usuario y por `permissions.js`.
 *
 * Despliegue:
 *   npx wrangler d1 execute vivace --remote --file=schema.sql
 *   npx wrangler secret put AUTH_SECRET      (obligatorio; sin él no arranca)
 *   npx wrangler deploy
 */

import { WEB_APP_JS, WEB_CSS, WEB_HTML, FAVICON_SVG } from "./web-html.js";
import { CLIENT_JS } from "./client-lib.js";
import { handleApi } from "./api.js";
import { migrateExistingSongs } from "./migrate.js";

/** Binarios de la app (APK + metadatos de versión) para la auto-actualización. */
const APP_PREFIX = "app/";

/**
 * ETag débil calculada una vez por despliegue (FNV-1a + longitud). Los recursos
 * estáticos van embebidos en el bundle, así que su contenido solo cambia al
 * desplegar: basta con revalidar contra esto para ahorrar el cuerpo entero.
 */
function weakEtag(text) {
  let h = 2166136261;
  for (let i = 0; i < text.length; i++) {
    h ^= text.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return `W/"${(h >>> 0).toString(36)}-${text.length.toString(36)}"`;
}

const ETAG_WEB = weakEtag(WEB_HTML);
const ETAG_JS = weakEtag(CLIENT_JS);
const ETAG_APP = weakEtag(WEB_APP_JS);
const ETAG_CSS = weakEtag(WEB_CSS);
const ETAG_ICON = weakEtag(FAVICON_SVG);

/**
 * Recursos que puede incrustar cualquiera: la portada, la auto-actualización y
 * las lecturas públicas del catálogo. Solo estos llevan `Origin: *`.
 */
function isPublicRead(method, path) {
  if (method !== "GET") return false;
  return path === "/update" || path === "/update/apk" ||
    path === "/api/songs/public" || path === "/api/genres" ||
    path === "/api/chords/global";
}

/**
 * CORS al mínimo necesario. Antes iba `*` en todas las rutas, autenticadas
 * incluidas; ahora solo las lecturas públicas son abiertas y el resto acepta
 * únicamente el propio origen del Worker (que es quien sirve la web). La app
 * Android no manda `Origin`, así que esto no la afecta: CORS es cosa del
 * navegador.
 */
function corsHeaders(request, url, path) {
  const base = {
    "Access-Control-Allow-Methods": "GET, PUT, POST, DELETE, OPTIONS",
    "Access-Control-Allow-Headers": "Authorization, Content-Type, If-None-Match",
  };
  if (isPublicRead(request.method, path)) {
    return { ...base, "Access-Control-Allow-Origin": "*" };
  }
  const origin = request.headers.get("Origin");
  if (origin && origin === url.origin) {
    return { ...base, "Access-Control-Allow-Origin": origin, Vary: "Origin" };
  }
  return base;
}

/** Respuesta estática con revalidación: si la ETag coincide, 304 sin cuerpo. */
function staticResponse(request, body, contentType, etag, maxAge) {
  const headers = {
    "Content-Type": contentType,
    "Cache-Control": `public, max-age=${maxAge}, must-revalidate`,
    ETag: etag,
  };
  if (request.headers.get("If-None-Match") === etag) {
    return new Response(null, { status: 304, headers });
  }
  return new Response(body, { headers });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;
    const cors = corsHeaders(request, url, path);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: cors });
    }

    // --- Vivace web (pública): catálogo, visor y edición con sesión propia ---
    if (path === "/" && request.method === "GET") {
      return staticResponse(request, WEB_HTML, "text/html; charset=utf-8", ETAG_WEB, 0);
    }
    if (path === "/static/favicon.svg" && request.method === "GET") {
      return staticResponse(request, FAVICON_SVG, "image/svg+xml; charset=utf-8", ETAG_ICON, 86400);
    }
    if (path === "/static/vivace.js" && request.method === "GET") {
      return staticResponse(request, CLIENT_JS, "application/javascript; charset=utf-8", ETAG_JS, 3600);
    }
    if (path === "/static/vivace-app.js" && request.method === "GET") {
      return staticResponse(request, WEB_APP_JS, "application/javascript; charset=utf-8", ETAG_APP, 3600);
    }
    if (path === "/static/vivace.css" && request.method === "GET") {
      return staticResponse(request, WEB_CSS, "text/css; charset=utf-8", ETAG_CSS, 3600);
    }

    // --- Auto-actualización (público, sin sesión): la app consulta la versión
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
      // Con backfill=1 además rellena carpetas/favoritos leyendo las cabeceras
      // #playlist:/#favorite: que la app antigua escondía dentro del texto.
      const backfill = url.searchParams.get("backfill") === "1";
      try {
        const result = await migrateExistingSongs(env, user.id, visibility, { limit, cursor, backfill });
        return json(result, cors);
      } catch (err) {
        // Sin esto la excepcion sale como un 500 sin cuerpo y no hay forma de
        // saber si fue el tope de subpeticiones, R2 o la base.
        return json({ error: String(err && err.message), imported: 0 }, cors, 500);
      }
    }

    return new Response("Not found", { status: 404, headers: cors });
  },
};

function json(data, cors, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}
