// ===========================================================================
// Admin UI — single self-contained HTML page, served at GET /
// ===========================================================================
export const ADMIN_HTML = `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>GuitarChords · R2</title>
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
<link rel="icon" href="/static/favicon.svg" type="image/svg+xml">
<style>
  :root { --bg:#0F1113; --panel:#17191C; --line:#2B2D31; --fg:#F2EFE9;
          --muted:#A7ABB2; --accent:#E8B04B; --on-accent:#0F1113;
          --danger:#E0654B; --ok:#7FB2A0;
          --font-ui:'Space Grotesk',system-ui,sans-serif;
          --font-mono:'JetBrains Mono',ui-monospace,Menlo,Consolas,monospace; }
  * { box-sizing:border-box; }
  body { margin:0; font:14px/1.45 var(--font-ui); background:var(--bg);
         color:var(--fg); height:100vh; display:flex; flex-direction:column; }
  header { display:flex; gap:8px; align-items:center; padding:10px 14px;
           border-bottom:1px solid var(--line); background:var(--panel); }
  header h1 { font-size:15px; margin:0 auto 0 0; font-weight:600; }
  input, textarea, button { font:inherit; color:var(--fg); }
  input[type=text], input[type=password], textarea {
    background:var(--bg); border:1px solid var(--line); border-radius:10px;
    padding:7px 9px; }
  input:focus, textarea:focus { outline:none; border-color:var(--accent); }
  button { background:var(--line); border:none; border-radius:10px; padding:7px 12px;
           cursor:pointer; color:var(--fg); }
  button:hover { filter:brightness(1.2); }
  button.primary { background:var(--accent); color:var(--on-accent); font-weight:600; }
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
  #body { flex:1; resize:none; font-family:var(--font-mono);
          font-size:13px; line-height:1.5; white-space:pre; }
  #preview { flex:1; overflow:auto; background:var(--bg); border:1px solid var(--line);
             border-radius:10px; padding:8px 10px;
             font-family:var(--font-mono); font-size:13px; }
  #previewWrap { flex:1; display:flex; flex-direction:column; min-width:0; }
  #previewWrap .hd { color:var(--muted); font-size:12px; margin-bottom:4px; }
  .pc { color:var(--accent); font-weight:600; }
  .pl { margin:0; white-space:pre; line-height:1.3; }
  .ptab { color:var(--muted); white-space:pre; line-height:1.3; }
  /* Modo visualización a pantalla completa */
  #viewer { position:fixed; inset:0; z-index:50; background:var(--bg);
            display:none; flex-direction:column; }
  #viewer.show { display:flex; }
  #vHead { display:flex; align-items:center; gap:10px; padding:10px 14px;
           border-bottom:1px solid var(--line); background:var(--panel); }
  #vHead .vt { font-weight:600; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
  #vHead .va { color:var(--muted); font-size:13px; white-space:nowrap;
               overflow:hidden; text-overflow:ellipsis; }
  #vCapo { margin-left:auto; background:var(--accent); color:var(--on-accent); font-weight:600;
           font-size:12px; border-radius:10px; padding:2px 8px; }
  #vBody { flex:1; overflow:auto; padding:16px 18px 140px;
           font-family:var(--font-mono);
           font-size:var(--vfs,18px); }
  #vCtrl { border-top:1px solid var(--line); background:var(--panel);
           padding:8px 14px; display:flex; flex-direction:column; gap:8px; }
  #vCtrl .vrow { display:flex; align-items:center; gap:8px; }
  #vCtrl .vrow label, #vCtrl .vsep { color:var(--muted); font-size:12px; white-space:nowrap; }
  #vCtrl input[type=range] { flex:1; min-width:0; accent-color:var(--accent); }
  #vSpeedVal { color:var(--muted); font-size:12px; min-width:56px; text-align:right; }
  #vTone { font-weight:600; min-width:34px; text-align:center; }
  #vFlat.sel { background:var(--accent); color:var(--on-accent); font-weight:600; }
  /* Modal "explorar todas las partituras" */
  #browseModal { position:fixed; inset:0; z-index:60; background:rgba(0,0,0,.5);
                 display:none; align-items:center; justify-content:center; padding:20px; }
  #browseModal.show { display:flex; }
  #bmBox { background:var(--panel); border:1px solid var(--line); border-radius:12px;
           width:920px; max-width:100%; height:80vh; display:flex; flex-direction:column; }
  #bmHead { display:flex; gap:8px; align-items:center; padding:12px 14px;
            border-bottom:1px solid var(--line); flex-wrap:wrap; }
  #bmHead h3 { margin:0; font-size:15px; flex:0 0 auto; }
  #bmSearch { flex:1 1 180px; min-width:0; }
  #bmSort { flex:0 0 auto; background:var(--bg); border:1px solid var(--line);
            border-radius:10px; padding:6px 8px; color:var(--fg); }
  #bmGrid { flex:1; overflow:auto; padding:14px; display:grid;
            grid-template-columns:repeat(auto-fill,minmax(200px,1fr));
            gap:10px; align-content:start; align-items:start; }
  /* Tarjeta en bloque (no flex): la altura crece con el contenido sin recortes. */
  .bmCard { display:block; border:1px solid var(--line); border-radius:10px;
            padding:12px; cursor:pointer; background:var(--bg); min-width:0; }
  .bmCard:hover { border-color:var(--accent); }
  .bmCard .ca, .bmCard .ct { overflow-wrap:anywhere; word-break:break-word; }
  .bmCard .ca { font-size:12px; color:var(--muted); margin-bottom:3px; }
  .bmCard .ct { font-weight:600; }
  #bmInfo { padding:6px 14px; font-size:12px; color:var(--muted);
            border-top:1px solid var(--line); }
  @media (max-width:760px){ #editSplit{flex-direction:column;} }
  .row { display:flex; gap:8px; align-items:center; }
  .row label { color:var(--muted); width:60px; }
  #capoBtns { display:flex; gap:4px; flex-wrap:wrap; }
  #capoBtns button { width:36px; padding:5px 0; text-align:center; }
  #capoBtns button.sel { background:var(--accent); color:var(--on-accent); font-weight:600; }
  #lockBtn.on { background:var(--danger); color:#fff; }
  .item .lk { flex:0 0 auto; margin-top:2px; }
  .grow { flex:1; }
  #count { color:var(--muted); font-size:12px; padding:6px 12px;
           border-bottom:1px solid var(--line); display:flex; gap:8px;
           align-items:center; }
  #count input[type=checkbox] { accent-color:var(--accent); margin:0; }
  #countTxt { flex:1; }
  #delSelBtn, #detectSelBtn { padding:3px 8px; font-size:12px; }
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
      <button id="browseBtn" title="Explorar todas las partituras (nombre, artista o contenido)">🔍 Buscar</button>
      <button id="randomBtn" title="Abrir una partitura al azar">🎲 Aleatoria</button>
      <input type="file" id="importInput" multiple accept=".txt,.text,text/plain" class="hidden">
    </div>
    <div class="bar">
      <label for="sortSel" style="color:var(--muted);align-self:center;flex:0 0 auto">Orden</label>
      <select id="sortSel" style="background:var(--bg);border:1px solid var(--line);border-radius:10px;padding:6px 8px;color:var(--fg)">
        <option value="name">Nombre</option>
        <option value="artist">Artista</option>
        <option value="created">Creación ↓</option>
        <option value="edited">Última edición ↓</option>
      </select>
    </div>
    <div id="count">
      <input type="checkbox" id="selAll" title="Seleccionar todo lo visible">
      <span id="countTxt"></span>
      <button class="hidden" id="detectSelBtn" title="Detectar acordes en las partituras seleccionadas">♪ Detectar</button>
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
      <span class="grow"></span>
      <button id="lockBtn" title="Bloquear evita ediciones accidentales en los dispositivos">🔓 Desbloqueada</button>
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
      <button id="viewBtn" title="Modo visualización: auto-scroll, tono y tamaño">▶ Ver</button>
      <button id="detectBtn" title="Detecta las líneas que solo contienen acordes y los envuelve en {X}">♪ Detectar acordes</button>
    </div>
  </section>
</div>

<!-- Modo visualización (auto-scroll, transposición, tamaño), tipo visor de la app -->
<div id="viewer">
  <div id="vHead">
    <button id="vClose" title="Cerrar (Esc)">✕</button>
    <div style="min-width:0">
      <div class="vt" id="vTitle"></div>
      <div class="va" id="vArtist"></div>
    </div>
    <span id="vCapo" class="hidden"></span>
    <button id="vPrint" title="Imprimir">🖨</button>
  </div>
  <div id="vBody"></div>
  <div id="vCtrl">
    <div class="vrow">
      <button id="vPlay" title="Reproducir/Pausar">▶</button>
      <label for="vSpeed">Velocidad</label>
      <input type="range" id="vSpeed" min="5" max="800" value="40">
      <span id="vSpeedVal">40 px/s</span>
    </div>
    <div class="vrow">
      <label for="vFont">Tamaño</label>
      <input type="range" id="vFont" min="10" max="48" value="18">
      <span class="vsep">Tono</span>
      <button id="vToneDown" title="Bajar medio tono">–</button>
      <span id="vTone">±0</span>
      <button id="vToneUp" title="Subir medio tono">+</button>
      <button id="vFlat" title="Usar bemoles">♭</button>
      <button id="vReset" title="Sin transposición">Reset</button>
    </div>
  </div>
</div>

<!-- Modal: explorar todas las partituras en recuadros -->
<div id="browseModal">
  <div id="bmBox">
    <div id="bmHead">
      <h3>Todas las partituras</h3>
      <input type="text" id="bmSearch" placeholder="Nombre, artista o contenido…">
      <select id="bmSort">
        <option value="name">Nombre</option>
        <option value="artist">Artista</option>
        <option value="created">Creación ↓</option>
        <option value="edited">Última edición ↓</option>
      </select>
      <button id="bmClose" title="Cerrar (Esc)">✕</button>
    </div>
    <div id="bmGrid"></div>
    <div id="bmInfo"></div>
  </div>
</div>

<script src="/static/vivace.js"></script>
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
let lockedVal = false;      // bloqueo del fichero abierto (cabecera #locked)
let songBodies = {};        // caché key→cuerpo en minúsculas (búsqueda por contenido del modal)
let indexFp = "";           // huella (key:etag) a la que corresponde songBodies
let indexing = false;       // hay una indexación de cuerpos en curso

// Refleja el estado de bloqueo en el botón (el candado solo se fija aquí).
function setLock(on) {
  lockedVal = !!on;
  lockBtn.textContent = lockedVal ? "🔒 Bloqueada" : "🔓 Desbloqueada";
  lockBtn.classList.toggle("on", lockedVal);
}

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
  let title = "", artist = "", capo = "", url = "", locked = false, i = 0;
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
    else if (k === "locked") locked = m[2].trim().toLowerCase() === "true";
    else otherHeaders.push(line);
    i++;
  }
  const body = lines.slice(i).join("\\n");
  return { title, artist, capo, url, locked, otherHeaders, body };
}

function serialize(c) {
  let out = "#title: " + c.title + "\\n";
  if (c.artist && c.artist.trim()) out += "#artist: " + c.artist.trim() + "\\n";
  if (c.capo && String(c.capo).trim() && String(c.capo).trim() !== "0")
    out += "#capo: " + String(c.capo).trim() + "\\n";
  if (c.url && c.url.trim()) out += "#url: " + c.url.trim() + "\\n";
  if (c.locked) out += "#locked: true\\n";
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
  invalidateIndex();                    // el índice cacheado es de esta cuenta
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
    locked: o.locked === "true",
    etag: o.etag || "",
    uploaded: o.uploaded || 0,
    created: o.created || o.uploaded || 0,
  }));
  selected = new Set([...selected].filter(k => items.some(it => it.key === k)));
  lastClickIdx = null;
  sortItems();
  renderList();
  backfillTitles();                        // legacy files lacking metadata
}

// Comparador según el modo de orden; lo comparten el listado y el modal.
function sortComparator(mode) {
  if (mode === "created") return (a, b) => (b.created || 0) - (a.created || 0);
  if (mode === "edited") return (a, b) => (b.uploaded || 0) - (a.uploaded || 0);
  if (mode === "artist") return (a, b) =>
    (a.artist || "~").localeCompare(b.artist || "~", "es") ||
    (a.title || "~").localeCompare(b.title || "~", "es");
  return (a, b) => (a.title || "~").localeCompare(b.title || "~", "es");
}

function sortItems() {
  items.sort(sortComparator(sortSel.value));
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
    div.appendChild(cb); div.appendChild(num);
    if (it.locked) {
      const lk = document.createElement("div");
      lk.className = "lk"; lk.textContent = "🔒"; lk.title = "Bloqueada";
      div.appendChild(lk);
    }
    div.appendChild(txt);
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
  detectSelBtn.textContent = "♪ Detectar (" + selected.size + ")";
  detectSelBtn.classList.toggle("hidden", selected.size === 0);
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
        if (it.locked !== p.locked) { it.locked = p.locked; changed = true; }
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
              locked: p.locked, otherHeaders: p.otherHeaders, body: p.body, isNew: false };
  title.value = p.title;
  artist.value = p.artist;
  setCapo(parseInt(p.capo, 10) || 0);
  setLock(p.locked);
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
  current = { key, title: "", artist: "", capo: "", url: "", locked: false, otherHeaders: [], body: "", isNew: true };
  title.value = ""; artist.value = ""; setCapo(0); setLock(false); srcurl.value = "";
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
  current.locked = lockedVal;
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
    const body = serialize(c);
    const res = await api("PUT", "/object?key=" + encodeURIComponent(c.key), body);
    const data = await res.json().catch(() => ({}));
    const now = Date.now();
    const existing = items.find(it => it.key === c.key);
    if (existing) {
      existing.title = c.title; existing.artist = c.artist; existing.locked = c.locked;
      existing.uploaded = now; existing.etag = data.etag || "";
    } else {
      items.push({ key: c.key, title: c.title, artist: c.artist, locked: c.locked,
                   etag: data.etag || "", uploaded: now, created: now });
    }
    // Mantén el índice del modal al día sin re-indexar: actualiza solo esta clave.
    if (indexFp) { songBodies[c.key] = body.toLowerCase(); indexFp = indexFingerprint(); saveIndexCache(); }
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
    delete songBodies[key];
    if (indexFp) { indexFp = indexFingerprint(); saveIndexCache(); }
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
    keys.forEach(k => delete songBodies[k]);
    if (indexFp) { indexFp = indexFingerprint(); saveIndexCache(); }
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
  invalidateIndex();
  refresh();
}

// ---- detección automática de acordes ----
// La lógica vive en client-lib.js (/static/vivace.js) y la comparten el panel
// y la web: una sola implementación, un solo sitio donde arreglar los fallos.
// Envuelta y no asignada directamente: si /static/vivace.js no cargara, una
// const que evalua vDetectChords al arrancar tumbaria el panel entero.
function detectChordsInText(text) { return vDetectChords(text); }

function detectChords() {
  if (!current) return;
  const r = detectChordsInText(body.value);
  if (!r.marked) { setStatus("No se detectaron acordes", "error"); return; }
  body.value = r.text;
  onEdit();                                      // refresca preview y lanza el autosave
  setStatus("Marcados " + r.marked + " acordes", "saved");
}

// Detección en lote sobre las partituras seleccionadas: descarga cada una,
// marca los acordes y reescribe solo las que cambian. No toca cabeceras.
async function detectSelected() {
  const keys = [...selected];
  if (!keys.length) return;
  if (!confirm("¿Detectar acordes en " + keys.length + " partitura" +
               (keys.length === 1 ? "" : "s") + "? Se guardarán los cambios.")) return;
  await flushSave();                             // no pisar una edición en curso
  let changed = 0, totalMarks = 0, fail = 0, done = 0;
  for (const key of keys) {
    try {
      const r = await api("GET", "/object?key=" + encodeURIComponent(key));
      const p = parse(await r.text());
      const det = detectChordsInText(p.body);
      if (det.marked > 0) {
        p.body = det.text;
        await api("PUT", "/object?key=" + encodeURIComponent(key), serialize(p));
        changed++; totalMarks += det.marked;
      }
    } catch (e) { fail++; }
    setStatus("Detectando… " + (++done) + "/" + keys.length, "saving");
  }
  if (changed) invalidateIndex();              // el contenido cambió en varias claves
  // Si el fichero abierto estaba en la selección, recargarlo para ver los {X}.
  if (current && selected.has(current.key)) {
    const ck = current.key;
    current = null;
    await open(ck);
  }
  setStatus("Acordes detectados en " + changed + " de " + keys.length +
            " (" + totalMarks + " marcas)" + (fail ? ", fallidos " + fail : ""),
            fail ? "error" : "saved");
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
    invalidateIndex();
    refresh();
  } catch (e) { setStatus("Error al restaurar", "error"); }
}

// ---- modo visualización (auto-scroll, transposición, tamaño) ----
// Transposición portada de chords/ChordTransposer.kt (mismas tablas y reglas).
const VCHROMA = ["C","C#","D","D#","E","F","F#","G","G#","A","A#","B"];
const VCHROMA_FLAT = ["C","Db","D","Eb","E","F","Gb","G","Ab","A","Bb","B"];
const VFLAT = { Db:"C#", Eb:"D#", Gb:"F#", Ab:"G#", Bb:"A#", Cb:"B", Fb:"E" };
function transposeChordToken(name, semis, flats) {
  if ((semis === 0 && !flats) || !name) return name;
  const slash = name.indexOf("/");
  if (slash >= 0)
    return transposeChordToken(name.slice(0, slash), semis, flats) + "/" +
           transposeChordToken(name.slice(slash + 1), semis, flats);
  const root = (name.length >= 2 && (name[1] === "#" || name[1] === "b"))
    ? name.slice(0, 2) : name.slice(0, 1);
  const suffix = name.slice(root.length);
  const idx = VCHROMA.indexOf(VFLAT[root] || root);
  if (idx < 0) return name;
  const shifted = ((idx + semis) % 12 + 12) % 12;
  return (flats ? VCHROMA_FLAT : VCHROMA)[shifted] + suffix;
}
// Transpone solo los {acordes}, respetando bloques {tab}. Espeja transposeContent.
function transposeBody(text, semis, flats) {
  if (semis === 0 && !flats) return text;
  let out = "", i = 0, inTab = false;
  while (i < text.length) {
    if (text[i] === "{") {
      const end = text.indexOf("}", i + 1);
      if (end > i) {
        const low = text.slice(i + 1, end).trim().toLowerCase();
        if (low === "tab") { inTab = true; out += text.slice(i, end + 1); }
        else if (low === "/tab") { inTab = false; out += text.slice(i, end + 1); }
        else if (inTab) out += text.slice(i, end + 1);
        else out += "{" + transposeChordToken(text.slice(i + 1, end).trim(), semis, flats) + "}";
        i = end + 1;
        continue;
      }
    }
    out += text[i];
    i++;
  }
  return out;
}

let vSemis = 0, vFlats = false;
let vSrcTitle = "", vSrcArtist = "", vSrcCapo = "", vSrcBody = "";
let vScrolling = false, vLast = 0, vRem = 0, vRaf = 0, vWake = null;

function openViewer() {
  if (!current) return;
  vSemis = 0; vFlats = false;
  vSrcTitle = title.value; vSrcArtist = artist.value;
  vSrcCapo = capoVal; vSrcBody = body.value;
  vTitle.textContent = vSrcTitle || "(sin título)";
  vArtist.textContent = vSrcArtist || "";
  vArtist.classList.toggle("hidden", !vSrcArtist);
  vFont.value = 18; vBody.style.setProperty("--vfs", "18px");
  vSpeed.value = 40; vSpeedVal.textContent = "40 px/s";
  renderViewer();
  vBody.scrollTop = 0;
  viewer.classList.add("show");
}
function renderViewer() {
  const cap = parseInt(vSrcCapo, 10) || 0;
  vCapo.textContent = cap > 0 ? ("Capo " + cap) : "";
  vCapo.classList.toggle("hidden", cap <= 0);
  vTone.textContent = (vSemis > 0 ? "+" : vSemis < 0 ? "" : "±") + vSemis;
  vFlat.classList.toggle("sel", vFlats);
  // El nº de líneas no cambia al transponer: preservamos el scroll.
  const st = vBody.scrollTop;
  vBody.innerHTML = renderPreview(transposeBody(vSrcBody, vSemis, vFlats));
  vBody.scrollTop = st;
}
function closeViewer() { stopScroll(); viewer.classList.remove("show"); }

function vStep(ts) {
  if (!vScrolling) return;
  const dt = vLast ? (ts - vLast) : 0;
  vLast = ts;
  vRem += (+vSpeed.value) * dt / 1000;     // acumula sub-píxel para velocidades lentas
  const px = Math.floor(vRem);
  if (px > 0) {
    vRem -= px;
    const maxv = vBody.scrollHeight - vBody.clientHeight;
    vBody.scrollTop = Math.min(vBody.scrollTop + px, maxv);
    if (vBody.scrollTop >= maxv) { stopScroll(); return; }
  }
  vRaf = requestAnimationFrame(vStep);
}
async function startScroll() {
  if (vScrolling) return;
  vScrolling = true; vLast = 0; vRem = 0;
  vPlay.textContent = "⏸";
  try { if (navigator.wakeLock) vWake = await navigator.wakeLock.request("screen"); } catch (e) {}
  vRaf = requestAnimationFrame(vStep);
}
function stopScroll() {
  vScrolling = false;
  vPlay.textContent = "▶";
  if (vRaf) { cancelAnimationFrame(vRaf); vRaf = 0; }
  if (vWake) { try { vWake.release(); } catch (e) {} vWake = null; }
}

function printViewer() {
  const w = window.open("", "_blank");
  if (!w) return;
  const cap = parseInt(vSrcCapo, 10) || 0;
  const lines = renderPreview(transposeBody(vSrcBody, vSemis, vFlats));
  w.document.write(
    '<!doctype html><meta charset="utf-8"><title>' + esc(vSrcTitle || "Partitura") + '</title>' +
    '<style>body{font-family:var(--font-mono);font-size:13px;padding:20px;color:#111;}' +
    'h1{font-size:18px;margin:0;}h2{font-size:13px;color:#555;margin:2px 0 10px;font-weight:normal;}' +
    '.pc{color:#1558d6;font-weight:bold;}.pl{white-space:pre;margin:0;line-height:1.3;}' +
    '.ptab{white-space:pre;color:#666;line-height:1.3;}' +
    '.capo{display:inline-block;border:1px solid #888;border-radius:4px;padding:1px 6px;' +
    'margin-bottom:10px;font-size:12px;}</style>' +
    '<h1>' + esc(vSrcTitle) + '</h1>' +
    (vSrcArtist ? '<h2>' + esc(vSrcArtist) + '</h2>' : '') +
    (cap > 0 ? '<div class="capo">Capo ' + cap + '</div>' : '') +
    '<div>' + lines + '</div>'
  );
  w.document.close();
  w.focus();
  setTimeout(() => w.print(), 250);
}

// ---- modal "todas las partituras" + aleatoria ----
function openBrowse() {
  bmSort.value = sortSel.value;            // arranca con el orden del listado
  browseModal.classList.add("show");
  renderBrowse();
  bmSearch.focus();
  ensureBodiesLoaded();                    // indexa cuerpos para buscar por contenido
}
function closeBrowse() { browseModal.classList.remove("show"); }

function renderBrowse() {
  const q = bmSearch.value.trim().toLowerCase();
  const list = items.filter(it => {
    if (!q) return true;
    if ((it.title + " " + it.artist + " " + it.key).toLowerCase().includes(q)) return true;
    const body = songBodies[it.key];
    return body != null && body.includes(q);
  }).sort(sortComparator(bmSort.value));
  bmGrid.innerHTML = "";
  for (const it of list) {
    const c = document.createElement("div");
    c.className = "bmCard";
    const a = document.createElement("div");
    a.className = "ca";
    a.textContent = it.artist || "(sin artista)";
    const t = document.createElement("div");
    t.className = "ct";
    t.textContent = (it.locked ? "🔒 " : "") + (it.title || "(sin título)");
    c.appendChild(a); c.appendChild(t);
    c.onclick = () => { closeBrowse(); open(it.key); };  // carga en el editor principal
    bmGrid.appendChild(c);
  }
  const ready = Object.keys(songBodies).length > 0;
  bmInfo.textContent = list.length + " de " + items.length +
    (indexing ? " · indexando contenido…" : ready ? " · búsqueda por contenido lista" : "");
}

// Huella del conjunto de partituras (clave+etag). Cambia solo si el contenido
// cambia, así sabemos si el índice cacheado sigue siendo válido.
function indexFingerprint() {
  return items.map(it => it.key + ":" + (it.etag || "")).sort().join("|");
}
function loadIndexCache() {
  try { return JSON.parse(localStorage.getItem("gc_index") || "null"); } catch (e) { return null; }
}
function saveIndexCache() {
  try { localStorage.setItem("gc_index", JSON.stringify({ fp: indexFp, bodies: songBodies })); }
  catch (e) { /* sin espacio: se queda solo en memoria */ }
}
// Invalida el índice tras cambios masivos (import/borrado/detección/restore).
function invalidateIndex() {
  songBodies = {}; indexFp = "";
  try { localStorage.removeItem("gc_index"); } catch (e) {}
}

// Garantiza el índice de contenido. Tras la primera vez no re-indexa al recargar
// (usa localStorage) y solo se rehace —en UNA petición— si cambia el contenido.
async function ensureBodiesLoaded() {
  if (indexing) return;
  const fp = indexFingerprint();
  if (indexFp === fp && Object.keys(songBodies).length) return;   // ya válido en memoria
  indexing = true;
  try {
    const cached = loadIndexCache();
    if (cached && cached.fp === fp && cached.bodies) {             // válido en localStorage
      songBodies = cached.bodies; indexFp = fp;
      if (browseModal.classList.contains("show")) renderBrowse();
      return;
    }
    const r = await api("GET", "/bodies");                        // un único request
    const map = await r.json();
    const bodies = {};
    for (const k in map) bodies[k] = (map[k] || "").toLowerCase();
    songBodies = bodies; indexFp = fp;
    saveIndexCache();
    if (browseModal.classList.contains("show")) renderBrowse();
  } catch (e) {
    // si falla, la búsqueda por contenido queda limitada hasta el próximo intento
  } finally { indexing = false; }
}

function openRandom() {
  if (!items.length) { setStatus("No hay partituras", "error"); return; }
  const it = items[Math.floor(Math.random() * items.length)];
  open(it.key);                            // carga en el editor principal
}

// ---- wiring ----
connectBtn.onclick = connect;
browseBtn.onclick = openBrowse;
randomBtn.onclick = openRandom;
bmClose.onclick = closeBrowse;
bmSearch.oninput = renderBrowse;
bmSort.onchange = renderBrowse;
browseModal.onclick = e => { if (e.target === browseModal) closeBrowse(); };
viewBtn.onclick = openViewer;
vClose.onclick = closeViewer;
vPrint.onclick = printViewer;
vPlay.onclick = () => { vScrolling ? stopScroll() : startScroll(); };
vSpeed.oninput = () => { vSpeedVal.textContent = vSpeed.value + " px/s"; };
vFont.oninput = () => { vBody.style.setProperty("--vfs", vFont.value + "px"); };
vToneDown.onclick = () => { vSemis = Math.max(-11, vSemis - 1); renderViewer(); };
vToneUp.onclick = () => { vSemis = Math.min(11, vSemis + 1); renderViewer(); };
vFlat.onclick = () => { vFlats = !vFlats; renderViewer(); };
vReset.onclick = () => { vSemis = 0; vFlats = false; renderViewer(); };
document.addEventListener("keydown", e => {
  if (e.key !== "Escape") return;
  if (browseModal.classList.contains("show")) closeBrowse();
  else if (viewer.classList.contains("show")) closeViewer();
});
// Reclama el wake lock al volver a la pestaña si seguía el auto-scroll.
document.addEventListener("visibilitychange", () => {
  if (!document.hidden && vScrolling && navigator.wakeLock) {
    navigator.wakeLock.request("screen").then(w => { vWake = w; }).catch(() => {});
  }
});
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
detectSelBtn.onclick = detectSelected;
selAll.onchange = () => {
  if (selAll.checked) visibleKeys.forEach(k => selected.add(k));
  else visibleKeys.forEach(k => selected.delete(k));
  renderList();
};
title.oninput = onEdit;
artist.oninput = onEdit;
srcurl.oninput = onEdit;
buildCapoBtns();
lockBtn.onclick = () => { setLock(!lockedVal); onEdit(); };
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
