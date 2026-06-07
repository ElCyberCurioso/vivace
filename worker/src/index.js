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
const TITLE_RE = /^#([A-Za-z]+):[ \t]?(.*)$/;

export default {
  async fetch(request, env) {
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers": "Authorization, Content-Type",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: cors });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    // --- Admin UI (public HTML; API calls below still require the token) ---
    if (path === "/" && request.method === "GET") {
      return new Response(ADMIN_HTML, {
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
      return new Response("Not found", { status: 404, headers: cors });
    } catch (err) {
      return new Response("Error: " + (err && err.message), {
        status: 500,
        headers: cors,
      });
    }
  },
};

/** Pull the indexed header values (title/artist/capo) out of a song body. */
function parseHeaders(text) {
  const lines = text.replace(/\r\n/g, "\n").split("\n");
  const h = { title: "", artist: "", capo: "" };
  for (const line of lines) {
    if (line.trim() === "---") break;
    const m = TITLE_RE.exec(line);
    if (!m) break;
    const k = m[1].toLowerCase();
    if (k === "title") h.title = m[2].trim();
    else if (k === "artist") h.artist = m[2].trim();
    else if (k === "capo") h.capo = m[2].trim();
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
      out.push({
        key: o.key,
        etag: o.httpEtag || o.etag || "",
        size: o.size || 0,
        uploaded: o.uploaded ? new Date(o.uploaded).getTime() : 0,
        title: cm.title || "",
        artist: cm.artist || "",
        capo: cm.capo || "",
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
  if (!key || !key.startsWith(SONG_PREFIX)) {
    return new Response("invalid key", { status: 400, headers: cors });
  }
  const body = await request.text();
  const h = parseHeaders(body);
  const obj = await env.BUCKET.put(key, body, {
    httpMetadata: { contentType: "text/plain; charset=utf-8" },
    customMetadata: { title: h.title, artist: h.artist, capo: h.capo },
  });
  return json(
    {
      key,
      etag: obj.httpEtag || "",
      size: obj.size || body.length,
      uploaded: obj.uploaded ? new Date(obj.uploaded).getTime() : Date.now(),
      title: h.title,
      artist: h.artist,
      capo: h.capo,
    },
    cors
  );
}

async function deleteObject(env, url, cors) {
  const key = url.searchParams.get("key");
  if (!key || !key.startsWith(SONG_PREFIX)) {
    return new Response("invalid key", { status: 400, headers: cors });
  }
  await env.BUCKET.delete(key);
  return json({ key, deleted: true }, cors);
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
  #side .bar { padding:8px; border-bottom:1px solid var(--line); display:flex; gap:6px; }
  #search { flex:1; }
  #list { flex:1; overflow:auto; }
  .item { padding:9px 12px; border-bottom:1px solid var(--line); cursor:pointer; }
  .item:hover { background:var(--bg); }
  .item.active { background:var(--bg); border-left:3px solid var(--accent);
                 padding-left:9px; }
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
  .grow { flex:1; }
  #count { color:var(--muted); font-size:12px; padding:6px 12px;
           border-bottom:1px solid var(--line); }
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
  <button id="logoutBtn">Salir</button>
</header>

<div id="main" class="hidden">
  <aside id="side">
    <div class="bar">
      <input type="text" id="search" placeholder="Buscar…">
      <button class="primary" id="newBtn" title="Nuevo fichero">+ Nuevo</button>
    </div>
    <div id="count"></div>
    <div id="list"></div>
  </aside>

  <section id="empty">Selecciona un fichero o crea uno nuevo.</section>

  <section id="editor" class="hidden">
    <div class="row">
      <label for="title">Título</label>
      <input type="text" id="title" class="grow" placeholder="Título de la canción">
      <label for="artist">Autor</label>
      <input type="text" id="artist" class="grow" placeholder="Autor original">
      <label for="capo">Capo</label>
      <input type="number" id="capo" min="0" max="12" step="1" placeholder="0" style="width:70px">
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

// ---- parsing: mirrors Android SongTextFormat ----
function parse(text) {
  const lines = text.replace(/\\r\\n/g, "\\n").split("\\n");
  let title = "", artist = "", capo = "", i = 0;
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
    else otherHeaders.push(line);
    i++;
  }
  const body = lines.slice(i).join("\\n");
  return { title, artist, capo, otherHeaders, body };
}

function serialize(c) {
  let out = "#title: " + c.title + "\\n";
  if (c.artist && c.artist.trim()) out += "#artist: " + c.artist.trim() + "\\n";
  if (c.capo && String(c.capo).trim() && String(c.capo).trim() !== "0")
    out += "#capo: " + String(c.capo).trim() + "\\n";
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
  items = raw.map(o => ({ key: o.key, title: o.title || "" }));
  items.sort((a, b) => (a.title || "~").localeCompare(b.title || "~", "es"));
  renderList();
  backfillTitles();                        // legacy files lacking metadata
}

function renderList() {
  const q = search.value.trim().toLowerCase();
  list.innerHTML = "";
  let shown = 0;
  for (const it of items) {
    const hay = (it.title + " " + it.key).toLowerCase();
    if (q && !hay.includes(q)) continue;
    shown++;
    const div = document.createElement("div");
    div.className = "item" + (current && current.key === it.key ? " active" : "");
    const t = document.createElement("div");
    if (it.title) { t.className = "t"; t.textContent = it.title; }
    else { t.className = "t untitled"; t.textContent = "(sin título)"; }
    const k = document.createElement("div"); k.className = "k"; k.textContent = it.key;
    div.appendChild(t); div.appendChild(k);
    div.onclick = () => open(it.key);
    list.appendChild(div);
  }
  count.textContent = shown + " fichero" + (shown === 1 ? "" : "s");
}

// Fetch + parse titles for files that have no title yet (uploaded before the
// worker stored title metadata). One re-PUT indexes them for next time.
let backfilling = false;
async function backfillTitles() {
  if (backfilling) return;
  backfilling = true;
  try {
    for (const it of items) {
      if (it.title) continue;
      try {
        const r = await api("GET", "/object?key=" + encodeURIComponent(it.key));
        const p = parse(await r.text());
        if (p.title) { it.title = p.title; renderList(); }
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
  current = { key, title: p.title, artist: p.artist, capo: p.capo,
              otherHeaders: p.otherHeaders, body: p.body, isNew: false };
  title.value = p.title;
  artist.value = p.artist;
  capo.value = p.capo;
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
  current = { key, title: "", artist: "", capo: "", otherHeaders: [], body: "", isNew: true };
  title.value = ""; artist.value = ""; capo.value = "";
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
  current.capo = capo.value;
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
    const existing = items.find(it => it.key === c.key);
    if (existing) { existing.title = c.title; }
    else { items.push({ key: c.key, title: c.title }); }
    if (c.isNew) { c.isNew = false; keyline.textContent = c.key; }
    items.sort((a, b) => (a.title || "~").localeCompare(b.title || "~", "es"));
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
    current = null;
    editor.classList.add("hidden");
    empty.classList.remove("hidden");
    renderList();
  } catch (e) { setStatus("Error al eliminar", "error"); }
}

function setStatus(text, cls) {
  const el = document.getElementById("saveStatus");
  el.textContent = text;
  el.className = cls;
}

// ---- wiring ----
connectBtn.onclick = connect;
tokenInput.onkeydown = e => { if (e.key === "Enter") connect(); };
logoutBtn.onclick = () => { flushSave().finally(() => logout()); };
newBtn.onclick = newFile;
deleteBtn.onclick = del;
title.oninput = onEdit;
artist.oninput = onEdit;
capo.oninput = onEdit;
body.oninput = onEdit;
search.oninput = renderList;
window.addEventListener("beforeunload", e => {
  if (saveTimer) { e.preventDefault(); e.returnValue = ""; }
});

if (token) showApp(); else tokenInput.focus();
</script>
</body>
</html>`;
