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

const SONG_PREFIX = "songs/";
// Los acordes personalizados se guardan en un blob bajo este prefijo, fuera del
// flujo de partituras (no aparecen en /list, que filtra por songs/).
const CHORDS_PREFIX = "chords/";
const TITLE_RE = /^#([A-Za-z]+):[ \t]?(.*)$/;

/** Claves que el Worker acepta escribir/borrar: partituras o acordes. */
function isAllowedKey(key) {
  return typeof key === "string" &&
    (key.startsWith(SONG_PREFIX) || key.startsWith(CHORDS_PREFIX));
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

    // --- Admin UI (public HTML; API calls below still require the token) ---
    if (path === "/" && request.method === "GET") {
      // STORAGE_URL ([vars] en wrangler.toml) apunta al bucket en el dashboard
      // de Cloudflare; si no está definida, el botón Storage no se muestra.
      const html = ADMIN_HTML.replace("__STORAGE_URL__", env.STORAGE_URL || "");
      return new Response(html, {
        headers: { "Content-Type": "text/html; charset=utf-8" },
      });
    }

    // --- Auth (everything below) ---
    const expected = `Bearer ${env.SYNC_TOKEN}`;
    if (!env.SYNC_TOKEN || request.headers.get("Authorization") !== expected) {
      return new Response("Unauthorized", { status: 401, headers: cors });
    }

    try {
      if (path === "/list" && request.method === "GET") {
        return await listObjects(env, cors);
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

/** Pull the indexed header values (title/artist/capo/url) out of a song body. */
function parseHeaders(text) {
  const lines = text.replace(/\r\n/g, "\n").split("\n");
  const h = { title: "", artist: "", capo: "", url: "" };
  for (const line of lines) {
    if (line.trim() === "---") break;
    const m = TITLE_RE.exec(line);
    if (!m) break;
    const k = m[1].toLowerCase();
    if (k === "title") h.title = m[2].trim();
    else if (k === "artist") h.artist = m[2].trim();
    else if (k === "capo") h.capo = m[2].trim();
    else if (k === "url") h.url = m[2].trim();
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
      });
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
    customMetadata: { title: h.title, artist: h.artist, capo: h.capo, url: h.url, created },
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

// ===========================================================================
// Admin UI — single self-contained HTML page, served at GET /
// ===========================================================================
const ADMIN_HTML = `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>GuitarChords · R2</title>
<style>
  :root { --bg:#1e1e24; --panel:#27272f; --line:#3a3a44; --fg:#e8e8ea;
          --muted:#9a9aa6; --accent:#6ea8fe; --danger:#e06c75; --ok:#5fd07a; }
  * { box-sizing:border-box; }
  body { margin:0; font:14px/1.45 system-ui,sans-serif; background:var(--bg);
         color:var(--fg); height:100vh; display:flex; flex-direction:column; }
  header { display:flex; gap:8px; align-items:center; padding:10px 14px;
           border-bottom:1px solid var(--line); background:var(--panel); }
  header h1 { font-size:15px; margin:0 auto 0 0; font-weight:600; }
  input, textarea, button { font:inherit; color:var(--fg); }
  input[type=text], input[type=password], textarea {
    background:var(--bg); border:1px solid var(--line); border-radius:6px;
    padding:7px 9px; }
  input:focus, textarea:focus { outline:none; border-color:var(--accent); }
  button { background:var(--line); border:none; border-radius:6px; padding:7px 12px;
           cursor:pointer; color:var(--fg); }
  button:hover { filter:brightness(1.2); }
  button.primary { background:var(--accent); color:#10243f; font-weight:600; }
  button.danger { background:transparent; color:var(--danger);
                  border:1px solid var(--danger); }
  #main { flex:1; display:flex; min-height:0; }
  #side { width:300px; border-right:1px solid var(--line); display:flex;
          flex-direction:column; background:var(--panel); }
  #side .bar { padding:8px; border-bottom:1px solid var(--line); display:flex;
               gap:6px; flex-wrap:wrap; }
  #side .bar button { white-space:nowrap; flex:0 0 auto; }
  #search { flex:1 1 120px; min-width:0; }
  #sortSel { flex:1 1 auto; min-width:0; max-width:100%; }
  #list { flex:1; overflow:auto; }
  .item { padding:9px 12px; border-bottom:1px solid var(--line); cursor:pointer;
          display:flex; gap:8px; align-items:flex-start; }
  .item:hover { background:var(--bg); }
  .item.active { background:var(--bg); border-left:3px solid var(--accent);
                 padding-left:9px; }
  .item input[type=checkbox] { margin-top:3px; flex:0 0 auto; accent-color:var(--accent); }
  .item .num { flex:0 0 auto; min-width:26px; text-align:right; color:var(--muted);
               font-size:12px; margin-top:2px; font-variant-numeric:tabular-nums; }
  .item .txt { flex:1; min-width:0; }
  .item .a { font-size:12px; color:var(--muted); }
  .item .t { font-weight:600; }
  .item .k { color:var(--muted); font-size:12px; word-break:break-all; }
  .item .untitled { color:var(--danger); font-style:italic; }
  #editor { flex:1; display:flex; flex-direction:column; padding:14px; gap:10px;
            min-width:0; }
  #empty { flex:1; display:flex; align-items:center; justify-content:center;
           color:var(--muted); padding:20px; text-align:center; }
  #title { font-size:16px; font-weight:600; }
  #editSplit { flex:1; display:flex; gap:10px; min-height:0; }
  #body { flex:1; resize:none; font-family:ui-monospace,Menlo,Consolas,monospace;
          font-size:13px; line-height:1.5; white-space:pre; }
  #preview { flex:1; overflow:auto; background:var(--bg); border:1px solid var(--line);
             border-radius:6px; padding:8px 10px;
             font-family:ui-monospace,Menlo,Consolas,monospace; font-size:13px; }
  #previewWrap { flex:1; display:flex; flex-direction:column; min-width:0; }
  #previewWrap .hd { color:var(--muted); font-size:12px; margin-bottom:4px; }
  .pc { color:var(--accent); font-weight:600; }
  .pl { margin:0; white-space:pre; line-height:1.3; }
  .ptab { color:var(--muted); white-space:pre; line-height:1.3; }
  @media (max-width:760px){ #editSplit{flex-direction:column;} }
  .row { display:flex; gap:8px; align-items:center; }
  .row label { color:var(--muted); width:60px; }
  #capoBtns { display:flex; gap:4px; flex-wrap:wrap; }
  #capoBtns button { width:36px; padding:5px 0; text-align:center; }
  #capoBtns button.sel { background:var(--accent); color:#10243f; font-weight:600; }
  .grow { flex:1; }
  #count { color:var(--muted); font-size:12px; padding:6px 12px;
           border-bottom:1px solid var(--line); display:flex; gap:8px;
           align-items:center; }
  #count input[type=checkbox] { accent-color:var(--accent); margin:0; }
  #countTxt { flex:1; }
  #delSelBtn { padding:3px 8px; font-size:12px; }
  #saveStatus { font-size:12px; color:var(--muted); min-width:120px; text-align:right; }
  #saveStatus.saving { color:var(--accent); }
  #saveStatus.saved { color:var(--ok); }
  #saveStatus.error { color:var(--danger); }
  #keyline { color:var(--muted); font-size:12px; word-break:break-all; }
  .hidden { display:none !important; }
  #gate { position:fixed; inset:0; background:var(--bg); display:flex;
          align-items:center; justify-content:center; }
  #gate .box { background:var(--panel); border:1px solid var(--line);
               border-radius:10px; padding:24px; width:360px; max-width:90vw;
               display:flex; flex-direction:column; gap:12px; }
  #gate h2 { margin:0; font-size:16px; }
  #gate p { margin:0; color:var(--muted); font-size:13px; }
  #gateErr { color:var(--danger); font-size:13px; min-height:16px; }
</style>
</head>
<body>

<div id="gate">
  <div class="box">
    <h2>GuitarChords · R2</h2>
    <p>Introduce el token de sincronización (SYNC_TOKEN).</p>
    <input type="password" id="tokenInput" placeholder="Token" autocomplete="off">
    <button class="primary" id="connectBtn">Conectar</button>
    <div id="gateErr"></div>
  </div>
</div>

<header class="hidden" id="app">
  <h1>GuitarChords · R2</h1>
  <span id="saveStatus"></span>
  <button id="storageBtn" class="hidden" title="Abrir el bucket R2 en el dashboard de Cloudflare">Storage ↗</button>
  <button id="backupBtn" title="Descargar todas las partituras en un ZIP">⬇ Backup</button>
  <button id="restoreBtn" title="Restaurar partituras desde un ZIP de backup">⬆ Restaurar</button>
  <input type="file" id="restoreInput" accept=".zip,application/zip" class="hidden">
  <button id="logoutBtn">Salir</button>
</header>

<div id="main" class="hidden">
  <aside id="side">
    <div class="bar">
      <input type="text" id="search" placeholder="Buscar…">
      <button id="importBtn" title="Subir varios ficheros de partituras">⇪ Importar</button>
      <button class="primary" id="newBtn" title="Nuevo fichero">+ Nuevo</button>
      <input type="file" id="importInput" multiple accept=".txt,.text,text/plain" class="hidden">
    </div>
    <div class="bar">
      <label for="sortSel" style="color:var(--muted);align-self:center;flex:0 0 auto">Orden</label>
      <select id="sortSel" style="background:var(--bg);border:1px solid var(--line);border-radius:6px;padding:6px 8px;color:var(--fg)">
        <option value="name">Nombre</option>
        <option value="artist">Artista</option>
        <option value="created">Creación ↓</option>
        <option value="edited">Última edición ↓</option>
      </select>
    </div>
    <div id="count">
      <input type="checkbox" id="selAll" title="Seleccionar todo lo visible">
      <span id="countTxt"></span>
      <button class="danger hidden" id="delSelBtn">Eliminar</button>
    </div>
    <div id="list"></div>
  </aside>

  <section id="empty">Selecciona un fichero o crea uno nuevo.</section>

  <section id="editor" class="hidden">
    <div class="row">
      <label for="title">Título</label>
      <input type="text" id="title" class="grow" placeholder="Título de la canción">
      <label for="artist">Autor</label>
      <input type="text" id="artist" class="grow" placeholder="Autor original">
    </div>
    <div class="row">
      <label>Capo</label>
      <div id="capoBtns"></div>
    </div>
    <div class="row">
      <label for="srcurl">URL</label>
      <input type="url" id="srcurl" class="grow" placeholder="https://… (web de origen de los acordes)">
      <button id="openUrlBtn" title="Abrir en una pestaña nueva">Link</button>
    </div>
    <div id="keyline"></div>
    <div id="editSplit">
      <textarea id="body" spellcheck="false" placeholder="Texto de la partitura…"></textarea>
      <div id="previewWrap">
        <div class="hd">Vista previa — los acordes {X} se muestran colocados sobre la letra</div>
        <div id="preview"></div>
      </div>
    </div>
    <div class="row">
      <button class="danger" id="deleteBtn">Eliminar</button>
      <span class="grow"></span>
      <button id="detectBtn" title="Detecta las líneas que solo contienen acordes y los envuelve en {X}">♪ Detectar acordes</button>
    </div>
  </section>
</div>

<script>
const SONG_PREFIX = "songs/";
const TITLE_RE = /^#([A-Za-z]+):[ \\t]?(.*)$/;
let token = localStorage.getItem("gc_token") || "";
let items = [];          // [{ key, title }]
let current = null;      // { key, title, otherHeaders[], body, isNew }
let saveTimer = null;
let selected = new Set();   // keys marcadas para borrado masivo
let visibleKeys = [];       // keys que pasan el filtro de búsqueda actual
let lastClickIdx = null;    // ancla (índice visible) para selección de rango con Mayús
let capoVal = "";           // capo del fichero abierto ("" = sin capo)

// Fila de botones 0–12 que sustituye al input numérico de capo.
function buildCapoBtns() {
  for (let i = 0; i <= 12; i++) {
    const b = document.createElement("button");
    b.type = "button";
    b.textContent = String(i);
    b.title = i === 0 ? "Sin capo" : "Capo en traste " + i;
    b.onclick = () => { setCapo(i); onEdit(); };
    capoBtns.appendChild(b);
  }
}
function setCapo(v) {
  capoVal = v > 0 ? String(v) : "";
  Array.from(capoBtns.children).forEach((b, i) =>
    b.classList.toggle("sel", i === (v || 0)));
}

// ---- parsing: mirrors Android SongTextFormat ----
function parse(text) {
  const lines = text.replace(/\\r\\n/g, "\\n").split("\\n");
  let title = "", artist = "", capo = "", url = "", i = 0;
  const otherHeaders = [];               // genre/favorite/playlist/… preserved verbatim
  while (i < lines.length) {
    const line = lines[i];
    if (line.trim() === "---") { i++; break; }
    const m = TITLE_RE.exec(line);
    if (!m) break;                       // body starts at first non-header line
    const k = m[1].toLowerCase();
    if (k === "title") title = m[2].trim();
    else if (k === "artist") artist = m[2].trim();
    else if (k === "capo") capo = m[2].trim();
    else if (k === "url") url = m[2].trim();
    else otherHeaders.push(line);
    i++;
  }
  const body = lines.slice(i).join("\\n");
  return { title, artist, capo, url, otherHeaders, body };
}

function serialize(c) {
  let out = "#title: " + c.title + "\\n";
  if (c.artist && c.artist.trim()) out += "#artist: " + c.artist.trim() + "\\n";
  if (c.capo && String(c.capo).trim() && String(c.capo).trim() !== "0")
    out += "#capo: " + String(c.capo).trim() + "\\n";
  if (c.url && c.url.trim()) out += "#url: " + c.url.trim() + "\\n";
  for (const h of c.otherHeaders) out += h + "\\n";
  out += "---\\n" + c.body;
  return out;
}

// ---- chord preview: render {X} placed above the lyric column ----
function esc(s) {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
// Render one source line verbatim: keep exact text/columns, only swap the
// {chord} braces for a coloured span. One source line stays one preview line.
function renderLine(line) {
  let html = "", i = 0;
  while (i < line.length) {
    if (line[i] === "{") {
      const end = line.indexOf("}", i);
      if (end !== -1) {
        html += '<span class="pc">' + esc(line.slice(i + 1, end)) + "</span>";
        i = end + 1;
        continue;
      }
    }
    html += esc(line[i]);
    i++;
  }
  return html;
}
function renderPreview(text) {
  const lines = (text || "").replace(/\\r\\n/g, "\\n").split("\\n");
  let html = "", tab = false;
  for (const line of lines) {
    const t = line.trim();
    if (t === "{tab}" || t === "{/tab}") { tab = (t === "{tab}"); continue; }
    if (tab) { html += '<div class="ptab">' + (esc(line) || "&nbsp;") + "</div>"; continue; }
    html += '<div class="pl">' + (renderLine(line) || "&nbsp;") + "</div>";
  }
  return html;
}
function updatePreview() {
  preview.innerHTML = renderPreview(body.value);
}
// Keep textarea and preview scrolled together (proportional to scroll range).
let syncing = false;
function linkScroll(src, dst) {
  src.addEventListener("scroll", function () {
    if (syncing) return;
    syncing = true;
    const sMax = src.scrollHeight - src.clientHeight;
    const dMax = dst.scrollHeight - dst.clientHeight;
    dst.scrollTop = sMax > 0 ? (src.scrollTop / sMax) * dMax : 0;
    requestAnimationFrame(function () { syncing = false; });
  });
}
linkScroll(body, preview);
linkScroll(preview, body);

// ---- api ----
async function api(method, path, body) {
  const opt = { method, headers: { "Authorization": "Bearer " + token } };
  if (body != null) { opt.body = body; opt.headers["Content-Type"] = "text/plain; charset=utf-8"; }
  const r = await fetch(path, opt);
  if (r.status === 401) { logout("Token inválido."); throw new Error("401"); }
  if (!r.ok) throw new Error("HTTP " + r.status + " " + (await r.text()));
  return r;
}

// ---- gate / auth ----
function showApp() {
  gate.classList.add("hidden");
  app.classList.remove("hidden");
  main.classList.remove("hidden");
  refresh();
}
function logout(msg) {
  localStorage.removeItem("gc_token"); token = "";
  app.classList.add("hidden"); main.classList.add("hidden");
  gate.classList.remove("hidden");
  gateErr.textContent = msg || "";
  tokenInput.value = "";
}
async function connect() {
  const t = tokenInput.value.trim();
  if (!t) return;
  token = t; gateErr.textContent = "Comprobando…";
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), 15000);
  try {
    const r = await fetch("/list", {
      headers: { "Authorization": "Bearer " + t },
      signal: ctrl.signal,
      cache: "no-store",
    });
    if (r.status === 401) { gateErr.textContent = "Token inválido."; return; }
    if (!r.ok) { gateErr.textContent = "Error " + r.status + ": " + (await r.text()).slice(0, 120); return; }
    localStorage.setItem("gc_token", t);
    showApp();
  } catch (e) {
    gateErr.textContent = e && e.name === "AbortError"
      ? "Tiempo de espera agotado (15s). ¿URL correcta?"
      : "Error de red: " + (e && e.message ? e.message : e);
  } finally {
    clearTimeout(timer);
  }
}

// ---- list ----
async function refresh() {
  const r = await api("GET", "/list");
  const raw = await r.json();
  items = raw.map(o => ({
    key: o.key,
    title: o.title || "",
    artist: o.artist || "",
    uploaded: o.uploaded || 0,
    created: o.created || o.uploaded || 0,
  }));
  selected = new Set([...selected].filter(k => items.some(it => it.key === k)));
  lastClickIdx = null;
  sortItems();
  renderList();
  backfillTitles();                        // legacy files lacking metadata
}

function sortItems() {
  const mode = sortSel.value;
  if (mode === "created") {
    items.sort((a, b) => (b.created || 0) - (a.created || 0));
  } else if (mode === "edited") {
    items.sort((a, b) => (b.uploaded || 0) - (a.uploaded || 0));
  } else if (mode === "artist") {
    items.sort((a, b) =>
      (a.artist || "~").localeCompare(b.artist || "~", "es") ||
      (a.title || "~").localeCompare(b.title || "~", "es"));
  } else {
    items.sort((a, b) => (a.title || "~").localeCompare(b.title || "~", "es"));
  }
}

function fmtDate(ms) {
  if (!ms) return "";
  const d = new Date(ms);
  const p = n => String(n).padStart(2, "0");
  return p(d.getDate()) + "/" + p(d.getMonth() + 1) + "/" + d.getFullYear() +
    " " + p(d.getHours()) + ":" + p(d.getMinutes());
}

// Número fijo de cada partitura: su posición ordenando todas por título.
// No depende del orden ni del filtro activos, así sirve como referencia estable.
function assignNumbers() {
  const byTitle = [...items].sort((a, b) =>
    (a.title || "~").localeCompare(b.title || "~", "es"));
  byTitle.forEach((it, i) => { it.num = i + 1; });
}

function renderList() {
  assignNumbers();
  const q = search.value.trim().toLowerCase();
  list.innerHTML = "";
  visibleKeys = [];
  for (const it of items) {
    const hay = (it.title + " " + it.artist + " " + it.key).toLowerCase();
    if (q && !hay.includes(q)) continue;
    const idx = visibleKeys.length;
    visibleKeys.push(it.key);
    const div = document.createElement("div");
    div.className = "item" + (current && current.key === it.key ? " active" : "");
    const cb = document.createElement("input");
    cb.type = "checkbox";
    cb.checked = selected.has(it.key);
    cb.title = "Mayús+clic selecciona un rango";
    cb.onclick = e => {
      e.stopPropagation();             // marcar no abre el fichero
      const on = cb.checked;
      if (e.shiftKey && lastClickIdx !== null && lastClickIdx !== idx) {
        // Selección de rango estilo Gmail: aplica el mismo estado a todo el tramo.
        const lo = Math.min(lastClickIdx, idx), hi = Math.max(lastClickIdx, idx);
        for (let j = lo; j <= hi; j++) {
          if (on) selected.add(visibleKeys[j]); else selected.delete(visibleKeys[j]);
        }
        renderList();
      } else {
        if (on) selected.add(it.key); else selected.delete(it.key);
        updateSelUI();
      }
      lastClickIdx = idx;
    };
    const num = document.createElement("div");
    num.className = "num";
    num.textContent = it.num;
    const txt = document.createElement("div"); txt.className = "txt";
    const a = document.createElement("div");
    if (it.artist) { a.className = "a"; a.textContent = it.artist; }
    else { a.className = "a untitled"; a.textContent = "(sin artista)"; }
    const t = document.createElement("div");
    if (it.title) { t.className = "t"; t.textContent = it.title; }
    else { t.className = "t untitled"; t.textContent = "(sin título)"; }
    txt.appendChild(a); txt.appendChild(t);
    // La ruta del fichero no se enseña; el bucket se consulta con el botón
    // Storage. Solo las ordenaciones por fecha añaden su línea informativa.
    const mode = sortSel.value;
    if (mode === "created" || mode === "edited") {
      const k = document.createElement("div"); k.className = "k";
      k.textContent = mode === "created" ? "creado " + fmtDate(it.created)
        : "editado " + fmtDate(it.uploaded);
      txt.appendChild(k);
    }
    div.appendChild(cb); div.appendChild(num); div.appendChild(txt);
    div.onclick = () => open(it.key);
    list.appendChild(div);
  }
  updateSelUI();
}

function updateSelUI() {
  const shown = visibleKeys.length;
  let txt = shown + " fichero" + (shown === 1 ? "" : "s");
  if (selected.size) txt += " · " + selected.size + " sel.";
  countTxt.textContent = txt;
  delSelBtn.textContent = "Eliminar (" + selected.size + ")";
  delSelBtn.classList.toggle("hidden", selected.size === 0);
  const allVisible = shown > 0 && visibleKeys.every(k => selected.has(k));
  selAll.checked = allVisible;
  selAll.indeterminate = !allVisible && visibleKeys.some(k => selected.has(k));
}

// Fetch + parse titles for files that have no title yet (uploaded before the
// worker stored title metadata). One re-PUT indexes them for next time.
let backfilling = false;
async function backfillTitles() {
  if (backfilling) return;
  backfilling = true;
  try {
    for (const it of items) {
      if (it.title && it.artist) continue;
      try {
        const r = await api("GET", "/object?key=" + encodeURIComponent(it.key));
        const p = parse(await r.text());
        let changed = false;
        if (!it.title && p.title) { it.title = p.title; changed = true; }
        if (!it.artist && p.artist) { it.artist = p.artist; changed = true; }
        if (changed) renderList();
      } catch (e) { /* skip */ }
    }
  } finally { backfilling = false; }
}

// ---- open / edit / save ----
async function open(key) {
  await flushSave();                       // don't lose pending edits
  const r = await api("GET", "/object?key=" + encodeURIComponent(key));
  const text = await r.text();
  const p = parse(text);
  current = { key, title: p.title, artist: p.artist, capo: p.capo, url: p.url,
              otherHeaders: p.otherHeaders, body: p.body, isNew: false };
  title.value = p.title;
  artist.value = p.artist;
  setCapo(parseInt(p.capo, 10) || 0);
  srcurl.value = p.url;
  body.value = p.body;
  keyline.textContent = key;
  empty.classList.add("hidden");
  editor.classList.remove("hidden");
  updatePreview();
  setStatus("", "");
  renderList();
}

function newFile() {
  flushSave();
  const key = SONG_PREFIX + crypto.randomUUID() + ".txt";
  current = { key, title: "", artist: "", capo: "", url: "", otherHeaders: [], body: "", isNew: true };
  title.value = ""; artist.value = ""; setCapo(0); srcurl.value = "";
  body.value = ""; keyline.textContent = key + "  (sin guardar)";
  empty.classList.add("hidden");
  editor.classList.remove("hidden");
  updatePreview();
  setStatus("", "");
  title.focus();
}

function onEdit() {
  if (!current) return;
  current.title = title.value;
  current.artist = artist.value;
  current.capo = capoVal;
  current.url = srcurl.value;
  current.body = body.value;
  updatePreview();
  setStatus("Editando…", "");
  if (saveTimer) clearTimeout(saveTimer);
  saveTimer = setTimeout(save, 1500);      // debounced autosave
}

async function save() {
  if (saveTimer) { clearTimeout(saveTimer); saveTimer = null; }
  if (!current) return;
  const c = current;
  setStatus("Guardando…", "saving");
  try {
    await api("PUT", "/object?key=" + encodeURIComponent(c.key), serialize(c));
    const now = Date.now();
    const existing = items.find(it => it.key === c.key);
    if (existing) { existing.title = c.title; existing.uploaded = now; }
    else { items.push({ key: c.key, title: c.title, uploaded: now, created: now }); }
    if (c.isNew) { c.isNew = false; keyline.textContent = c.key; }
    sortItems();
    renderList();
    setStatus("Guardado ✓", "saved");
  } catch (e) {
    setStatus("Error al guardar", "error");
  }
}
function flushSave() {
  if (saveTimer && current) return save();
  return Promise.resolve();
}

async function del() {
  if (!current) return;
  if (!confirm("¿Eliminar " + current.key + " de R2? Acción irreversible.")) return;
  if (saveTimer) { clearTimeout(saveTimer); saveTimer = null; }
  const key = current.key;
  try {
    if (!current.isNew) await api("DELETE", "/object?key=" + encodeURIComponent(key));
    items = items.filter(it => it.key !== key);
    selected.delete(key);
    current = null;
    editor.classList.add("hidden");
    empty.classList.remove("hidden");
    renderList();
  } catch (e) { setStatus("Error al eliminar", "error"); }
}

async function delSelected() {
  const keys = [...selected];
  if (!keys.length) return;
  if (!confirm("¿Eliminar " + keys.length + " fichero" + (keys.length === 1 ? "" : "s") +
               " de R2? Acción irreversible.")) return;
  setStatus("Eliminando…", "saving");
  try {
    await api("POST", "/delete", JSON.stringify({ keys }));
    items = items.filter(it => !selected.has(it.key));
    if (current && selected.has(current.key)) {
      if (saveTimer) { clearTimeout(saveTimer); saveTimer = null; }
      current = null;
      editor.classList.add("hidden");
      empty.classList.remove("hidden");
    }
    selected.clear();
    renderList();
    setStatus("Eliminados " + keys.length + " ✓", "saved");
  } catch (e) { setStatus("Error al eliminar", "error"); }
}

function setStatus(text, cls) {
  const el = document.getElementById("saveStatus");
  el.textContent = text;
  el.className = cls;
}

// ---- importación masiva ----
// Sube cada fichero como una canción nueva. El título sale de la PRIMERA línea
// no vacía del fichero (que se retira del cuerpo). Si el fichero ya trae
// cabecera #title se respeta; si está vacío, se usa el nombre del fichero.
// Artista/capo/URL se rellenan después a mano, aunque si vienen como
// cabeceras #artist/#capo/#url se conservan.
async function importFiles(files) {
  let ok = 0, fail = 0;
  for (const f of files) {
    try {
      const text = await f.text();
      const p = parse(text);
      let title = p.title;
      let bodyText = p.body;
      if (!title) {
        const lines = bodyText.replace(/\\r\\n/g, "\\n").split("\\n");
        const idx = lines.findIndex(l => l.trim() !== "");
        if (idx >= 0) {
          title = lines[idx].trim();
          bodyText = lines.slice(idx + 1).join("\\n").replace(/^\\n+/, "");
        } else {
          title = f.name.replace(/\\.[^.]+$/, "").trim() || f.name;
        }
      }
      const c = { title, artist: p.artist, capo: p.capo, url: p.url,
                  otherHeaders: p.otherHeaders, body: bodyText };
      const key = SONG_PREFIX + crypto.randomUUID() + ".txt";
      await api("PUT", "/object?key=" + encodeURIComponent(key), serialize(c));
      ok++;
      setStatus("Importando… " + (ok + fail) + "/" + files.length, "saving");
    } catch (e) { fail++; }
  }
  setStatus("Importados " + ok + (fail ? ", fallidos " + fail : "") + " ✓",
            fail ? "error" : "saved");
  refresh();
}

// ---- detección automática de acordes ----
// Una línea cuenta como "línea de acordes" si TODOS sus tokens son acordes en
// notación anglosajona (C, Am7, F#m7b5, D/F#…) o separadores (|, x2, N.C.…).
// Sus acordes se envuelven en {X}; como la vista quita las llaves al pintar,
// las columnas sobre la letra no se desplazan. No toca bloques {tab} ni
// líneas que ya tengan alguna llave.
const CHORD_RE = /^\\(?[A-G][#b]?(?:maj|min|sus|add|aug|dim|m|M|º|°|\\+|-|b|#|\\d)*(?:\\/[A-G][#b]?)?\\)?$/;
const SEP_RE = /^(?:\\||\\/|-+|–|%|x\\d+|\\(x\\d+\\)|N\\.?C\\.?)$/i;

function detectChords() {
  if (!current) return;
  const lines = body.value.replace(/\\r\\n/g, "\\n").split("\\n");
  let inTab = false, marked = 0;
  const out = lines.map(line => {
    const t = line.trim();
    if (t === "{tab}") { inTab = true; return line; }
    if (t === "{/tab}") { inTab = false; return line; }
    if (inTab || !t || line.includes("{")) return line;
    let chords = 0;
    for (const tok of t.split(/\\s+/)) {
      if (CHORD_RE.test(tok)) chords++;
      else if (!SEP_RE.test(tok)) return line;   // token de letra: no es línea de acordes
    }
    if (!chords) return line;
    marked += chords;
    return line.split(/(\\s+)/).map(part =>
      part && !/^\\s/.test(part) && CHORD_RE.test(part) ? "{" + part + "}" : part
    ).join("");
  });
  if (!marked) { setStatus("No se detectaron acordes", "error"); return; }
  body.value = out.join("\\n");
  onEdit();                                      // refresca preview y lanza el autosave
  setStatus("Marcados " + marked + " acordes", "saved");
}

// ---- backup / restore ----
// El ZIP se construye y se lee íntegramente en el navegador: el Worker no
// necesita endpoints nuevos. Al crear se usa STORE (texto pequeño); al leer
// se admiten STORE y DEFLATE (DecompressionStream).
const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
    t[n] = c >>> 0;
  }
  return t;
})();
function crc32(bytes) {
  let c = 0xFFFFFFFF;
  for (let i = 0; i < bytes.length; i++) c = CRC_TABLE[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8);
  return (c ^ 0xFFFFFFFF) >>> 0;
}
function buildZip(entries) {            // entries: [{ name, data: Uint8Array }]
  const enc = new TextEncoder();
  const parts = [], central = [];
  let offset = 0;
  const now = new Date();
  const dosTime = ((now.getHours() << 11) | (now.getMinutes() << 5) | (now.getSeconds() >> 1)) & 0xFFFF;
  const dosDate = (((now.getFullYear() - 1980) << 9) | ((now.getMonth() + 1) << 5) | now.getDate()) & 0xFFFF;
  for (const e of entries) {
    const name = enc.encode(e.name);
    const crc = crc32(e.data);
    const lh = new DataView(new ArrayBuffer(30));
    lh.setUint32(0, 0x04034b50, true);  // local file header
    lh.setUint16(4, 20, true);          // versión mínima
    lh.setUint16(6, 0x0800, true);      // nombres en UTF-8
    lh.setUint16(8, 0, true);           // método: store
    lh.setUint16(10, dosTime, true);
    lh.setUint16(12, dosDate, true);
    lh.setUint32(14, crc, true);
    lh.setUint32(18, e.data.length, true);
    lh.setUint32(22, e.data.length, true);
    lh.setUint16(26, name.length, true);
    lh.setUint16(28, 0, true);
    parts.push(new Uint8Array(lh.buffer), name, e.data);
    const ch = new DataView(new ArrayBuffer(46));
    ch.setUint32(0, 0x02014b50, true);  // central directory header
    ch.setUint16(4, 20, true); ch.setUint16(6, 20, true);
    ch.setUint16(8, 0x0800, true); ch.setUint16(10, 0, true);
    ch.setUint16(12, dosTime, true); ch.setUint16(14, dosDate, true);
    ch.setUint32(16, crc, true);
    ch.setUint32(20, e.data.length, true); ch.setUint32(24, e.data.length, true);
    ch.setUint16(28, name.length, true);
    ch.setUint32(42, offset, true);
    central.push(new Uint8Array(ch.buffer), name);
    offset += 30 + name.length + e.data.length;
  }
  const cdSize = central.reduce((s, p) => s + p.length, 0);
  const eocd = new DataView(new ArrayBuffer(22));
  eocd.setUint32(0, 0x06054b50, true);  // end of central directory
  eocd.setUint16(8, entries.length, true);
  eocd.setUint16(10, entries.length, true);
  eocd.setUint32(12, cdSize, true);
  eocd.setUint32(16, offset, true);
  return new Blob([...parts, ...central, new Uint8Array(eocd.buffer)],
                  { type: "application/zip" });
}

async function backupAll() {
  try {
    setStatus("Preparando backup…", "saving");
    const r = await api("GET", "/list");
    const objs = await r.json();
    if (!objs.length) { setStatus("No hay partituras", "error"); return; }
    const enc = new TextEncoder();
    const entries = [];
    for (const o of objs) {
      const rr = await api("GET", "/object?key=" + encodeURIComponent(o.key));
      entries.push({ name: o.key, data: enc.encode(await rr.text()) });
      setStatus("Backup… " + entries.length + "/" + objs.length, "saving");
    }
    const d = new Date();
    const p = n => String(n).padStart(2, "0");
    const a = document.createElement("a");
    a.href = URL.createObjectURL(buildZip(entries));
    a.download = "guitarchords-backup-" + d.getFullYear() + p(d.getMonth() + 1) +
                 p(d.getDate()) + "-" + p(d.getHours()) + p(d.getMinutes()) + ".zip";
    a.click();
    URL.revokeObjectURL(a.href);
    setStatus("Backup de " + entries.length + " ficheros ✓", "saved");
  } catch (e) { setStatus("Error en backup", "error"); }
}

async function readZip(buf) {
  const dv = new DataView(buf);
  const u8 = new Uint8Array(buf);
  let eocd = -1;
  for (let i = buf.byteLength - 22; i >= Math.max(0, buf.byteLength - 22 - 65535); i--) {
    if (dv.getUint32(i, true) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error("ZIP inválido");
  const count = dv.getUint16(eocd + 10, true);
  let p = dv.getUint32(eocd + 16, true);
  const dec = new TextDecoder();
  const out = [];
  for (let n = 0; n < count; n++) {
    if (dv.getUint32(p, true) !== 0x02014b50) throw new Error("ZIP inválido");
    const method = dv.getUint16(p + 10, true);
    const csize = dv.getUint32(p + 20, true);
    const nameLen = dv.getUint16(p + 28, true);
    const extraLen = dv.getUint16(p + 30, true);
    const commentLen = dv.getUint16(p + 32, true);
    const lho = dv.getUint32(p + 42, true);
    const name = dec.decode(u8.subarray(p + 46, p + 46 + nameLen));
    // El name/extra de la cabecera local puede diferir del directorio central.
    const dataStart = lho + 30 + dv.getUint16(lho + 26, true) + dv.getUint16(lho + 28, true);
    const raw = u8.slice(dataStart, dataStart + csize);
    if (!name.endsWith("/")) {
      if (method === 0) {
        out.push({ name, text: dec.decode(raw) });
      } else if (method === 8) {
        const ds = new Blob([raw]).stream().pipeThrough(new DecompressionStream("deflate-raw"));
        out.push({ name, text: await new Response(ds).text() });
      }
      // otros métodos: se omiten
    }
    p += 46 + nameLen + extraLen + commentLen;
  }
  return out;
}

async function restoreBackup(file) {
  try {
    setStatus("Leyendo ZIP…", "saving");
    const entries = (await readZip(await file.arrayBuffer()))
      .filter(e => e.name.toLowerCase().endsWith(".txt"));
    if (!entries.length) { setStatus("ZIP sin partituras", "error"); return; }
    if (!confirm("Se restaurarán " + entries.length + " partituras desde el ZIP. " +
                 "Las claves que ya existan se sobrescribirán. ¿Continuar?")) {
      setStatus("", "");
      return;
    }
    let ok = 0, fail = 0;
    for (const e of entries) {
      // Conserva la clave original; lo que venga fuera de songs/ entra con clave nueva.
      const key = e.name.startsWith(SONG_PREFIX)
        ? e.name
        : SONG_PREFIX + crypto.randomUUID() + ".txt";
      try {
        await api("PUT", "/object?key=" + encodeURIComponent(key), e.text);
        ok++;
      } catch (err) { fail++; }
      setStatus("Restaurando… " + (ok + fail) + "/" + entries.length, "saving");
    }
    setStatus("Restaurados " + ok + (fail ? ", fallidos " + fail : "") + " ✓",
              fail ? "error" : "saved");
    refresh();
  } catch (e) { setStatus("Error al restaurar", "error"); }
}

// ---- wiring ----
connectBtn.onclick = connect;
tokenInput.onkeydown = e => { if (e.key === "Enter") connect(); };
logoutBtn.onclick = () => { flushSave().finally(() => logout()); };
const STORAGE_URL = "__STORAGE_URL__";
if (STORAGE_URL && !STORAGE_URL.startsWith("__")) {
  storageBtn.classList.remove("hidden");
  storageBtn.onclick = () => window.open(STORAGE_URL, "_blank", "noopener");
}
backupBtn.onclick = backupAll;
restoreBtn.onclick = () => restoreInput.click();
restoreInput.onchange = () => {
  const f = restoreInput.files && restoreInput.files[0];
  restoreInput.value = "";
  if (f) restoreBackup(f);
};
newBtn.onclick = newFile;
importBtn.onclick = () => importInput.click();
importInput.onchange = () => {
  const files = Array.from(importInput.files || []);
  importInput.value = "";
  if (files.length) importFiles(files);
};
deleteBtn.onclick = del;
detectBtn.onclick = detectChords;
delSelBtn.onclick = delSelected;
selAll.onchange = () => {
  if (selAll.checked) visibleKeys.forEach(k => selected.add(k));
  else visibleKeys.forEach(k => selected.delete(k));
  renderList();
};
title.oninput = onEdit;
artist.oninput = onEdit;
srcurl.oninput = onEdit;
buildCapoBtns();
body.oninput = onEdit;
openUrlBtn.onclick = () => {
  const u = srcurl.value.trim();
  if (!u) return;
  window.open(/^https?:\\/\\//i.test(u) ? u : "https://" + u, "_blank", "noopener");
};
search.oninput = () => { lastClickIdx = null; renderList(); };
sortSel.onchange = () => { lastClickIdx = null; sortItems(); renderList(); };
window.addEventListener("beforeunload", e => {
  if (saveTimer) { e.preventDefault(); e.returnValue = ""; }
});

if (token) showApp(); else tokenInput.focus();
</script>
</body>
</html>`;
