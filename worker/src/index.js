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

import {
  WEB_APP_JS, WEB_CSS, WEB_HTML,
  FAVICON_SVG, FAVICON_DARK_SVG, PATTERN_SVG, PATTERN_DARK_SVG,
} from "./web-html.js";
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
const ETAG_ICON_DARK = weakEtag(FAVICON_DARK_SVG);
const ETAG_PATTERN = weakEtag(PATTERN_SVG);
const ETAG_PATTERN_DARK = weakEtag(PATTERN_DARK_SVG);

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

/**
 * Desarrollo local: `wrangler dev` sirve en http://localhost, así que ahí no se
 * puede forzar HTTPS ni mandar HSTS (dejaría el navegador convencido de que
 * localhost es siempre https y rompería el siguiente proyecto que use el mismo
 * puerto).
 */
function esLocal(hostname) {
  return hostname === "localhost" || hostname === "127.0.0.1" || hostname === "[::1]" ||
         hostname.endsWith(".localhost");
}

/**
 * Una sola dirección buena: HTTPS y sin `www.`.
 *
 * - Un dominio propio de Cloudflare atiende también el puerto 80, y ahí la
 *   petición viaja en claro: cualquiera en la misma red ve el token de sesión
 *   de la cabecera Authorization. Se corta con un 301 antes de mirar la ruta.
 * - `www.` va al apex por lo mismo de siempre: la sesión de la web vive en el
 *   almacenamiento local del navegador, y con dos orígenes entrar por uno o por
 *   otro daría dos sesiones distintas.
 *
 * Se conserva ruta y query. El fragmento (`#/cancion/ID`) no llega al servidor,
 * pero el navegador lo mantiene al seguir el redirect, así que los enlaces
 * compartidos siguen valiendo.
 */
function canonicalRedirect(request, url) {
  const enClaro = url.protocol === "http:" && !esLocal(url.hostname);
  const conWww = url.hostname.startsWith("www.");
  if (!enClaro && !conWww) return null;
  const destino = new URL(url);
  if (enClaro) destino.protocol = "https:";
  if (conWww) destino.hostname = url.hostname.slice(4);
  // 301 solo para lecturas: un 301 sobre un POST deja que el navegador lo
  // reenvíe como GET y se pierda el cuerpo. El 308 obliga a repetir el mismo
  // método, así que una subida de la app por http acaba subiendo, no fallando.
  const soloLee = request.method === "GET" || request.method === "HEAD";
  return new Response(null, {
    status: soloLee ? 301 : 308,
    headers: { Location: destino.toString(), "Cache-Control": "public, max-age=3600" },
  });
}

/**
 * HSTS: tras la primera visita por HTTPS el navegador ya no vuelve a pedir el
 * sitio en claro ni aunque se teclee http:// o se siga un enlace viejo, así que
 * el 301 de arriba deja de ser la única defensa (ese primer salto sí viaja en
 * claro). Un año, subdominios incluidos; sin `preload`, que es irreversible a
 * corto plazo y obliga a que TODO subdominio futuro hable HTTPS.
 */
function conHsts(url, res) {
  if (url.protocol !== "https:" || esLocal(url.hostname)) return res;
  const salida = new Response(res.body, res);
  salida.headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
  return salida;
}

/*
 * La entrada solo se ocupa del transporte (HTTPS y host canónico) y le pone
 * HSTS a lo que salga; el enrutado de verdad está en `ruta`. Así la cabecera se
 * pone en UN sitio y no en los treinta y pico `new Response` repartidos por el
 * Worker. Se llama `app.ruta` y no `this.ruta` a propósito: según cómo invoque
 * el runtime al manejador, `this` puede no ser este objeto.
 */
const app = {
  async fetch(request, env) {
    const url = new URL(request.url);
    const canonico = canonicalRedirect(request, url);
    if (canonico) return canonico;
    return conHsts(url, await app.ruta(request, env));
  },

  async ruta(request, env) {
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
    // Recursos con dos versiones (el color va dentro del SVG, así que no hay
    // currentColor que valga): el mosaico del fondo y el favicon. No cambian
    // entre despliegues, así que se cachean un día.
    if (path === "/static/pattern.svg" && request.method === "GET") {
      return staticResponse(request, PATTERN_SVG, "image/svg+xml; charset=utf-8", ETAG_PATTERN, 86400);
    }
    if (path === "/static/pattern-dark.svg" && request.method === "GET") {
      return staticResponse(request, PATTERN_DARK_SVG, "image/svg+xml; charset=utf-8", ETAG_PATTERN_DARK, 86400);
    }
    if (path === "/static/favicon-dark.svg" && request.method === "GET") {
      return staticResponse(request, FAVICON_DARK_SVG, "image/svg+xml; charset=utf-8", ETAG_ICON_DARK, 86400);
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

export default app;

function json(data, cors, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}
