/*
 * Vivace · aplicación web (servida en /).
 *
 * Página autocontenida (sin build) que habla con la API multiusuario:
 * catálogo publicado, partituras propias, visor con auto-scroll, metrónomo y
 * transposición, y editor con publicación.
 *
 * Los estilos son deliberadamente sobrios: el diseño definitivo llegará
 * después y solo habrá que tocar el bloque <style> y las variables de color.
 */

export const WEB_HTML = `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>Vivace</title>
<link rel="icon" href="/static/favicon.svg" type="image/svg+xml">
<meta name="theme-color" content="#0F1113" media="(prefers-color-scheme: dark)">
<meta name="theme-color" content="#F6F4EF" media="(prefers-color-scheme: light)">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
<style>
  /* Vivace · estilo Nocturno. Oscuro por defecto; claro con [data-theme=light]
     o si el sistema lo pide. Los nombres --vv-* son los del paquete de marca. */
  :root {
    color-scheme: dark light;
    --vv-bg:#0F1113; --vv-surface:#17191C; --vv-surface-alt:#1E2124;
    --vv-border:#2B2D31; --vv-border-strong:#3A3D42;
    --vv-text:#F2EFE9; --vv-text-muted:#A7ABB2; --vv-text-subtle:#8B8F96;
    --vv-accent:#E8B04B; --vv-accent-strong:#C2762B; --vv-on-accent:#0F1113;
    --vv-beat:#7FB2A0; --vv-danger:#E0654B;
    --vv-accent-soft:rgba(232,176,75,.16);
    --vv-glow:0 0 14px rgba(232,176,75,.85);
    --vv-shadow-card:0 1px 2px rgba(0,0,0,.4);
    --vv-font-ui:'Space Grotesk',system-ui,-apple-system,Segoe UI,Roboto,sans-serif;
    --vv-font-mono:'JetBrains Mono',ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;
    --vv-radius-sm:8px; --vv-radius-md:10px; --vv-radius-lg:16px;
  }
  [data-theme=light] {
    color-scheme: light;
    --vv-bg:#F6F4EF; --vv-surface:#FFFFFF; --vv-surface-alt:#EFEDE7;
    --vv-border:#E2DED5; --vv-border-strong:#D5D0C5;
    --vv-text:#16181A; --vv-text-muted:#4A4D52; --vv-text-subtle:#6B7076;
    --vv-accent:#B8791F; --vv-accent-strong:#8F5A12; --vv-on-accent:#FFFFFF;
    --vv-beat:#2F6B5B; --vv-danger:#B33F26;
    --vv-accent-soft:rgba(184,121,31,.14); --vv-glow:none;
    --vv-shadow-card:0 1px 2px rgba(22,24,26,.06);
  }
  @media (prefers-color-scheme: light) {
    :root:not([data-theme=dark]):not([data-theme=light]) {
      color-scheme: light;
      --vv-bg:#F6F4EF; --vv-surface:#FFFFFF; --vv-surface-alt:#EFEDE7;
      --vv-border:#E2DED5; --vv-border-strong:#D5D0C5;
      --vv-text:#16181A; --vv-text-muted:#4A4D52; --vv-text-subtle:#6B7076;
      --vv-accent:#B8791F; --vv-accent-strong:#8F5A12; --vv-on-accent:#FFFFFF;
      --vv-beat:#2F6B5B; --vv-danger:#B33F26;
      --vv-accent-soft:rgba(184,121,31,.14); --vv-glow:none;
      --vv-shadow-card:0 1px 2px rgba(22,24,26,.06);
    }
  }
  * { box-sizing:border-box; }
  a { color:var(--vv-accent); text-decoration:none; }
  a:hover { color:var(--vv-accent-strong); }
  a.brand { color:var(--vv-text); }
  a.brand:hover { color:var(--vv-text); }
  body { margin:0; background:var(--vv-bg); color:var(--vv-text);
         font:16px/1.5 var(--vv-font-ui); -webkit-font-smoothing:antialiased; }
  button, input, select, textarea { font:inherit; color:inherit; }
  :focus-visible { outline:2px solid var(--vv-accent); outline-offset:2px; }
  ::selection { background:var(--vv-accent-soft); }
  button { cursor:pointer; border:1px solid var(--vv-border-strong); background:transparent;
           color:var(--vv-text); border-radius:var(--vv-radius-md); padding:9px 15px;
           font-weight:500; white-space:nowrap; }
  button:hover { background:var(--vv-accent-soft); }
  button.primary { background:var(--vv-accent); color:var(--vv-on-accent);
                   border-color:transparent; font-weight:600; }
  button.primary:hover { background:var(--vv-accent-strong); }
  button.ghost { border-color:transparent; background:transparent; }
  button:disabled { opacity:.45; cursor:default; background:transparent; }
  input[type=text], input[type=email], input[type=password], textarea, select {
    background:var(--vv-surface-alt); border:1px solid var(--vv-border);
    border-radius:var(--vv-radius-md); padding:10px 12px; width:100%; }
  input::placeholder, textarea::placeholder { color:var(--vv-text-subtle); }
  header { position:sticky; top:0; z-index:10; display:flex; gap:12px; align-items:center;
           padding:10px 16px; background:var(--vv-surface); border-bottom:1px solid var(--vv-border); }
  header .brand { display:flex; align-items:center; gap:10px; min-width:0; }
  header .brand svg { flex:0 0 auto; }
  header h1 { margin:0; font-size:22px; font-weight:500; letter-spacing:.01em; line-height:1; }
  header .kicker { display:block; font-size:9px; letter-spacing:.22em; font-weight:500;
                   text-transform:uppercase; color:var(--vv-text-subtle); margin-top:3px; }
  header .grow { flex:1; }
  main { padding:16px; max-width:1520px; margin:0 auto; }
  #listView, #authView { max-width:960px; margin:0 auto; }
  .tabs { display:flex; gap:8px; margin-bottom:14px; flex-wrap:wrap; }
  .tabs button[aria-selected=true] { background:var(--vv-accent); color:var(--vv-on-accent);
                                     border-color:transparent; font-weight:600; }
  .grid { display:grid; gap:10px; grid-template-columns:repeat(auto-fill,minmax(230px,1fr)); align-items:start; }
  .card { display:block; text-align:left; width:100%; border:1px solid var(--vv-border);
          border-radius:var(--vv-radius-lg); padding:14px 16px; background:var(--vv-surface);
          box-shadow:var(--vv-shadow-card); }
  .card:hover { border-color:var(--vv-accent); background:var(--vv-surface); }
  .card .a { font-size:13px; color:var(--vv-text-muted); overflow-wrap:anywhere; }
  .card .t { font-weight:600; overflow-wrap:anywhere; }
  .badge { display:inline-block; font-size:11px; border-radius:999px; padding:2px 9px;
           border:1px solid var(--vv-border-strong); color:var(--vv-text-muted); margin-top:6px;
           font-family:var(--vv-font-mono); }
  .msg { color:var(--vv-danger); min-height:1.2em; }
  .empty { color:var(--vv-text-muted); text-align:center; padding:40px 12px; }
  .row { display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
  .stack { display:flex; flex-direction:column; gap:12px; max-width:420px; margin:40px auto; }
  /* La partitura se centra como bloque, pero el texto sigue alineado a la
     izquierda: las columnas son las del fichero y mover el texto descolocaria
     los acordes de sus silabas. max-content = ancho de la linea mas larga. */
  .sheet { width:max-content; max-width:100%; margin:0 auto; }
  /* ---- editor a dos paneles ---- */
  .editor { max-width:1520px; margin:0 auto; display:flex; flex-direction:column; gap:12px; }
  .editHead { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
  #editSplit { display:grid; grid-template-columns:1fr 1fr; gap:12px; align-items:start; }
  .pane { display:flex; flex-direction:column; gap:6px; min-width:0; }
  .pane .hd { display:flex; align-items:baseline; gap:8px; font-size:11px; letter-spacing:.18em;
              text-transform:uppercase; color:var(--vv-text-subtle); }
  .pane .hd small { letter-spacing:0; text-transform:none; font-size:12px; }
  #eContent { height:58vh; min-height:280px; resize:vertical;
              font-family:var(--vv-font-mono); font-size:14px; line-height:1.45; white-space:pre; }
  #ePreview { height:58vh; min-height:280px; overflow:auto;
              background:var(--vv-surface); border:1px solid var(--vv-border);
              border-radius:var(--vv-radius-md); padding:12px 14px;
              font-family:var(--vv-font-mono); font-size:14px; }
  @media (max-width:900px) {
    #editSplit, .editHead { grid-template-columns:1fr; }
    #eContent, #ePreview { height:40vh; }
  }
  label small { color:var(--vv-text-subtle); }
  /* ---- visor ---- */
  #viewer { position:fixed; inset:0; background:var(--vv-bg); display:none; flex-direction:column; z-index:20; }
  #viewer.on { display:flex; }
  #vHead { display:flex; gap:10px; align-items:center; padding:10px 14px;
           border-bottom:1px solid var(--vv-border); background:var(--vv-surface); }
  #vTitle { font-weight:600; }
  #vArtist { font-size:13px; color:var(--vv-text-muted); }
  #vBody { flex:1; overflow:auto; padding:16px 18px 120px;
           font-family:var(--vv-font-mono); font-size:var(--fs,18px); }
  .ln { white-space:pre; line-height:1.35; margin:0; }
  .tab { white-space:pre; line-height:1.35; color:var(--vv-text-muted); }
  .chord { color:var(--vv-accent); font-weight:600; }
  #vCtrl { border-top:1px solid var(--vv-border); background:var(--vv-surface); padding:8px 14px;
           display:flex; flex-direction:column; gap:8px; padding-bottom:max(8px,env(safe-area-inset-bottom)); }
  #vCtrl .row label { font-size:12px; color:var(--vv-text-muted); white-space:nowrap; }
  #vCtrl input[type=range] { flex:1; min-width:80px; width:auto; padding:0; border:0;
                             background:transparent; accent-color:var(--vv-accent); }
  /* Cifras (BPM, tono, velocidad, capo) siempre en la mono de la marca. */
  #vSpeedVal, #vBpmVal, #vTone, #vCapo {
    font-family:var(--vv-font-mono); font-variant-numeric:tabular-nums; }
  .beat { width:12px; height:12px; border-radius:50%; background:var(--vv-border-strong); display:inline-block; }
  .beat.on { background:var(--vv-accent); box-shadow:var(--vv-glow); }
  /* ---- acordes ---- */
  .chordSvg { display:block; }
  .chordBar { display:flex; gap:8px; overflow-x:auto; padding:8px 14px; background:var(--vv-surface);
              border-bottom:1px solid var(--vv-border); }
  .chordBar button { flex:0 0 auto; display:flex; flex-direction:column; align-items:center; gap:2px;
                     padding:6px 8px; border-color:transparent; }
  .chordBar .nm { font-family:var(--vv-font-mono); font-size:12px; font-weight:600; color:var(--vv-accent); }
  .chordBar .none { color:var(--vv-text-subtle); font-size:13px; padding:8px 4px; white-space:nowrap; }
  .modal { position:fixed; inset:0; z-index:40; background:rgba(0,0,0,.55);
           display:flex; align-items:center; justify-content:center; padding:16px; }
  .modalBox { background:var(--vv-surface); border:1px solid var(--vv-border); width:100%;
              border-radius:var(--vv-radius-lg); padding:16px; max-width:780px; max-height:86vh;
              overflow:auto; display:flex; flex-direction:column; gap:12px; }
  .modalBox h3 { margin:0; font-size:20px; font-family:var(--vv-font-mono); }
  .chordGrid { display:grid; gap:10px; grid-template-columns:repeat(auto-fill,minmax(118px,1fr)); }
  .chordCard { display:flex; flex-direction:column; align-items:center; gap:4px; padding:10px;
               border:1px solid var(--vv-border); border-radius:var(--vv-radius-md);
               background:var(--vv-surface); }
  .chordCard .nm { font-family:var(--vv-font-mono); font-weight:600; }
  .chordCard .va { font-size:11px; color:var(--vv-text-subtle); }
  .posRow { display:flex; gap:12px; align-items:center; flex-wrap:wrap; padding:10px;
            border:1px solid var(--vv-border); border-radius:var(--vv-radius-md); }
  .posRow input { width:54px; text-align:center; font-family:var(--vv-font-mono); padding:6px 4px; }
  .posRow .lbl { font-size:11px; letter-spacing:.14em; text-transform:uppercase; color:var(--vv-text-subtle); }
  .posRow .grp { display:flex; gap:4px; align-items:center; }
  .hidden { display:none !important; }
</style>
</head>
<body>

<header>
  <a class="brand" href="/" aria-label="Vivace">
    <svg width="30" height="30" viewBox="0 0 48 48" aria-hidden="true">
      <rect x="18" y="6" width="12" height="30" fill="none" stroke="var(--vv-accent)" stroke-width="2.4"></rect>
      <g fill="currentColor">
        <circle cx="12" cy="14" r="2.4"></circle><circle cx="12" cy="22" r="2.4"></circle><circle cx="12" cy="30" r="2.4"></circle>
        <circle cx="36" cy="14" r="2.4"></circle><circle cx="36" cy="22" r="2.4"></circle><circle cx="36" cy="30" r="2.4"></circle>
      </g>
      <rect x="18" y="38" width="12" height="3.4" fill="var(--vv-accent)"></rect>
    </svg>
    <span>
      <h1>vivace</h1>
      <span class="kicker">Practice room</span>
    </span>
  </a>
  <span class="grow"></span>
  <span id="who" class="hidden" style="font-size:13px;color:var(--vv-text-muted)"></span>
  <button id="loginBtn">Entrar</button>
  <button id="logoutBtn" class="hidden">Salir</button>
</header>

<main>
  <!-- acceso -->
  <section id="authView" class="hidden">
    <div class="stack">
      <h2 id="authTitle" style="margin:0">Entrar en Vivace</h2>
      <label>Email<input type="email" id="email" autocomplete="email"></label>
      <label id="nameWrap" class="hidden">Nombre<input type="text" id="name" autocomplete="name"></label>
      <label>Contraseña <small>(mínimo 8 caracteres)</small>
        <input type="password" id="password" autocomplete="current-password"></label>
      <div class="msg" id="authMsg"></div>
      <button class="primary" id="authSubmit">Entrar</button>
      <button class="ghost" id="authSwitch">Crear una cuenta</button>
    </div>
  </section>

  <!-- listados -->
  <section id="listView">
    <div class="tabs">
      <button id="tabPublic" aria-selected="true">Catálogo</button>
      <button id="tabMine" aria-selected="false" class="hidden">Mis partituras</button>
      <button id="tabChords" aria-selected="false" class="hidden" title="Diccionario global, visible para todo el mundo">Acordes</button>
      <span class="grow"></span>
      <button id="newBtn" class="hidden">+ Nueva</button>
    </div>
    <input type="text" id="search" placeholder="Buscar por título o artista…" style="margin-bottom:12px">
    <div id="list" class="grid"></div>
    <div id="listEmpty" class="empty hidden"></div>
  </section>

  <!-- editor -->
  <section id="editView" class="hidden">
    <div class="editor">
      <div class="editHead">
        <label>Título<input type="text" id="eTitle"></label>
        <label>Artista<input type="text" id="eArtist"></label>
        <label>Capo<input type="text" id="eCapo" inputmode="numeric" placeholder="0"></label>
        <label>Visibilidad
          <select id="eVisibility">
            <option value="private">Privada (solo yo)</option>
            <option value="public">Pública (cualquiera puede verla)</option>
          </select>
        </label>
      </div>
      <div id="editSplit">
        <div class="pane">
          <div class="hd">Partitura <small>acordes entre llaves: {Am}</small></div>
          <textarea id="eContent" spellcheck="false"
                    placeholder="#title: Título&#10;#artist: Autor&#10;---&#10;{Am} Primera línea"></textarea>
        </div>
        <div class="pane">
          <div class="hd">Vista previa <small>tal cual se verá</small></div>
          <div id="ePreview"></div>
        </div>
      </div>
      <div class="msg" id="editMsg"></div>
      <div class="row">
        <button class="primary" id="saveBtn">Guardar</button>
        <button id="cancelEdit">Cancelar</button>
        <span class="grow"></span>
        <button id="deleteBtn" class="hidden" style="color:var(--vv-danger)">Eliminar</button>
      </div>
    </div>
  </section>
  <!-- diccionario global de acordes (solo administración) -->
  <section id="chordsView" class="hidden">
    <div class="row" style="margin-bottom:12px">
      <input type="text" id="chordSearch" placeholder="Buscar acorde…" style="flex:1;min-width:160px">
      <button id="chordNew">+ Nuevo acorde</button>
      <button id="chordSeed" title="Añade los acordes que falten sin tocar los que ya has definido">Importar diccionario base</button>
    </div>
    <div class="row" style="margin-bottom:10px">
      <span id="chordCount" class="vv-kicker"></span>
      <span class="grow"></span>
      <span id="chordSaved" class="vv-kicker"></span>
    </div>
    <div class="msg" id="chordMsg"></div>
    <div id="chordList" class="chordGrid"></div>
    <div id="chordEmpty" class="empty hidden"></div>
  </section>
</main>

<!-- diagramas del acorde que se está mirando (cualquiera) -->
<div id="chordModal" class="modal hidden">
  <div class="modalBox" style="max-width:620px">
    <div class="row">
      <h3 id="cmName"></h3>
      <span class="grow"></span>
      <button id="cmClose">Cerrar</button>
    </div>
    <div id="cmBody" class="chordGrid"></div>
    <div id="cmEmpty" class="empty hidden"></div>
  </div>
</div>

<!-- edición de un acorde (solo administración) -->
<div id="chordEditor" class="modal hidden">
  <div class="modalBox">
    <div class="row">
      <label style="flex:1">Nombre <small>tal cual se escribe entre llaves</small>
        <input type="text" id="chName" placeholder="Am7"></label>
      <button id="chClose">Cerrar</button>
    </div>
    <div class="vv-kicker">Posiciones — trastes de la 6ª cuerda (Mi grave) a la 1ª; -1 no suena, 0 al aire</div>
    <div id="chPositions"></div>
    <div class="msg" id="chMsg"></div>
    <div class="row">
      <button id="chAddPos">+ Posición</button>
      <span class="grow"></span>
      <button id="chDelete" style="color:var(--vv-danger)">Eliminar acorde</button>
      <button class="primary" id="chSave">Guardar</button>
    </div>
  </div>
</div>

<!-- visor a pantalla completa -->
<div id="viewer">
  <div id="vHead">
    <button id="vClose" class="ghost">‹ Volver</button>
    <div style="min-width:0">
      <div id="vTitle"></div>
      <div id="vArtist"></div>
    </div>
    <span class="grow"></span>
    <span id="vCapo" class="badge hidden"></span>
    <button id="vEdit" class="hidden">Editar</button>
  </div>
  <div id="vChordBar" class="chordBar hidden"></div>
  <div id="vBody"></div>
  <div id="vCtrl">
    <div class="row">
      <button id="vPlay" title="Desplazamiento automático">▶ Scroll</button>
      <input type="range" id="vSpeed" min="5" max="300" value="40">
      <label id="vSpeedVal">40 px/s</label>
      <span class="grow"></span>
      <button id="vChords" title="Diagramas de los acordes de esta partitura">♦ Acordes</button>
      <button id="vMetro" title="Metrónomo">♩ Metrónomo</button>
      <span id="vBeats" class="row" style="gap:4px"></span>
    </div>
    <div class="row">
      <label>Tono</label>
      <button id="vDown">–</button>
      <label id="vTone" style="min-width:2.5em;text-align:center">±0</label>
      <button id="vUp">+</button>
      <button id="vFlat">♭</button>
      <span class="grow"></span>
      <label>Letra</label>
      <button id="vFontDown">A-</button>
      <button id="vFontUp">A+</button>
      <span class="grow"></span>
      <label>BPM</label>
      <input type="range" id="vBpm" min="40" max="200" value="100" style="max-width:120px">
      <label id="vBpmVal">100</label>
    </div>
  </div>
</div>

<script src="/static/vivace.js"></script>
<script>
var token = localStorage.getItem("vivace_token") || "";
var user = null;
var tab = "public";
var songs = [];
var current = null;          // { song, content }
var editingId = null;        // null = alta nueva
var semis = 0, flats = false, fontSize = 18;
var scrolling = false, lastTs = 0, remainder = 0, raf = 0, wakeLock = null;
var metro = new VMetronome();
var chordDict = null;          // diccionario global, cacheado tras la primera carga
var chordBarOn = false;
var editingChord = null;       // nombre que se está editando, "" si es nuevo
var chordPositions = [];       // posiciones del acorde en edición

/* ---------- API ---------- */
function api(method, path, body) {
  var opt = { method: method, headers: {} };
  if (token) opt.headers["Authorization"] = "Bearer " + token;
  if (body !== undefined) {
    opt.headers["Content-Type"] = "application/json";
    opt.body = JSON.stringify(body);
  }
  return fetch(path, opt).then(function (r) {
    return r.json().catch(function () { return {}; }).then(function (data) {
      if (!r.ok) throw new Error(data.error || ("Error " + r.status));
      return data;
    });
  });
}

/* ---------- sesión ---------- */
function setSession(tok, u) {
  token = tok || "";
  user = u || null;
  if (token) localStorage.setItem("vivace_token", token);
  else localStorage.removeItem("vivace_token");
  who.textContent = user ? (user.name || user.email) : "";
  who.classList.toggle("hidden", !user);
  loginBtn.classList.toggle("hidden", !!user);
  logoutBtn.classList.toggle("hidden", !user);
  tabMine.classList.toggle("hidden", !user);
  tabChords.classList.toggle("hidden", !(user && user.role === "admin"));
  newBtn.classList.toggle("hidden", !user);
  if (!user && tab === "mine") tab = "public";
  if (!(user && user.role === "admin") && tab === "chords") tab = "public";
}

function restoreSession() {
  if (!token) return Promise.resolve();
  return api("GET", "/auth/me")
    .then(function (d) { setSession(token, d.user); })
    .catch(function () { setSession("", null); });   // token caducado
}

var registering = false;
function showAuth(on) {
  authView.classList.toggle("hidden", !on);
  listView.classList.toggle("hidden", on);
  authMsg.textContent = "";
}
function submitAuth() {
  var path = registering ? "/auth/register" : "/auth/login";
  var body = { email: email.value.trim(), password: password.value };
  // getElementById y no el global implicito: window.name ya existe como
  // cadena nativa y gana al elemento, asi que name.value seria undefined.
  if (registering) body.name = document.getElementById("name").value.trim();
  authMsg.textContent = "";
  api("POST", path, body).then(function (d) {
    setSession(d.token, d.user);
    password.value = "";
    showAuth(false);
    tab = "mine";
    refresh();
  }).catch(function (e) { authMsg.textContent = e.message; });
}

/* ---------- listados ---------- */
function refresh() {
  tabPublic.setAttribute("aria-selected", tab === "public");
  tabMine.setAttribute("aria-selected", tab === "mine");
  tabChords.setAttribute("aria-selected", tab === "chords");
  showChords(tab === "chords");
  if (tab === "chords") return;
  var path = tab === "mine" ? "/api/songs" : "/api/songs/public";
  api("GET", path).then(function (d) {
    songs = d.songs || [];
    renderList();
  }).catch(function (e) {
    songs = [];
    renderList(e.message);
  });
}

function renderList(error) {
  var q = search.value.trim().toLowerCase();
  var shown = songs.filter(function (s) {
    if (!q) return true;
    return ((s.title || "") + " " + (s.artist || "")).toLowerCase().indexOf(q) >= 0;
  });
  list.innerHTML = "";
  shown.forEach(function (s) {
    var card = document.createElement("button");
    card.className = "card";
    var a = document.createElement("div");
    a.className = "a";
    a.textContent = s.artist || (s.ownerName ? "de " + s.ownerName : "");
    var t = document.createElement("div");
    t.className = "t";
    t.textContent = (s.locked ? "🔒 " : "") + (s.title || "(sin título)");
    card.appendChild(a); card.appendChild(t);
    if (tab === "mine") {
      var b = document.createElement("span");
      b.className = "badge";
      b.textContent = s.visibility === "public" ? "Pública" : "Privada";
      card.appendChild(b);
    }
    card.onclick = function () { openSong(s.id); };
    list.appendChild(card);
  });
  var msg = error ? error
    : shown.length ? ""
    : (tab === "mine" ? "Todavía no tienes partituras. Crea la primera con «+ Nueva»."
                      : "Aún no hay partituras publicadas.");
  listEmpty.textContent = msg;
  listEmpty.classList.toggle("hidden", !msg);
}

/* ---------- visor ---------- */
function openSong(id) {
  api("GET", "/api/songs/" + id).then(function (d) {
    current = d;
    semis = 0; flats = false;
    var parsed = vParseSong(d.content);
    current.body = parsed.body || d.content;
    vTitle.textContent = d.song.title || "(sin título)";
    vArtist.textContent = d.song.artist || "";
    var capo = Number(d.song.capo) || 0;
    vCapo.textContent = "Capo " + capo;
    vCapo.classList.toggle("hidden", capo <= 0);
    vEdit.classList.toggle("hidden", !(user && (user.id === d.song.ownerId || user.role === "admin")));
    renderViewer();
    vBody.scrollTop = 0;
    viewer.classList.add("on");
  }).catch(function (e) { alert(e.message); });
}

function renderViewer() {
  vTone.textContent = (semis > 0 ? "+" : semis < 0 ? "" : "±") + semis;
  vFlat.style.fontWeight = flats ? "700" : "400";
  var keep = vBody.scrollTop;
  vBody.innerHTML = '<div class="sheet">' +
                    vRenderSong(vTransposeBody(current.body, semis, flats)) + "</div>";
  vBody.style.setProperty("--fs", fontSize + "px");
  vBody.scrollTop = keep;
  renderChordBar();
}

function closeViewer() {
  vChordBar.classList.add("hidden");
  stopScroll();
  if (metro.isRunning()) toggleMetro();
  viewer.classList.remove("on");
}

function step(ts) {
  if (!scrolling) return;
  var dt = lastTs ? ts - lastTs : 0;
  lastTs = ts;
  remainder += (+vSpeed.value) * dt / 1000;
  var px = Math.floor(remainder);
  if (px > 0) {
    remainder -= px;
    var max = vBody.scrollHeight - vBody.clientHeight;
    vBody.scrollTop = Math.min(vBody.scrollTop + px, max);
    if (vBody.scrollTop >= max) { stopScroll(); return; }
  }
  raf = requestAnimationFrame(step);
}
function startScroll() {
  if (scrolling) return;
  scrolling = true; lastTs = 0; remainder = 0;
  vPlay.textContent = "⏸ Scroll";
  if (navigator.wakeLock) {
    navigator.wakeLock.request("screen").then(function (w) { wakeLock = w; }).catch(function () {});
  }
  raf = requestAnimationFrame(step);
}
function stopScroll() {
  scrolling = false;
  vPlay.textContent = "▶ Scroll";
  if (raf) { cancelAnimationFrame(raf); raf = 0; }
  if (wakeLock) { try { wakeLock.release(); } catch (e) {} wakeLock = null; }
}

function renderBeats(beat) {
  vBeats.innerHTML = "";
  for (var i = 1; i <= metro.beatsPerBar; i++) {
    var d = document.createElement("span");
    d.className = "beat" + (beat === i ? " on" : "");
    vBeats.appendChild(d);
  }
}
function toggleMetro() {
  if (metro.isRunning()) {
    metro.stop();
    vMetro.textContent = "♩ Metrónomo";
  } else {
    metro.bpm = +vBpm.value;
    metro.start();
    vMetro.textContent = "■ Metrónomo";
  }
}
metro.onBeat = renderBeats;

/* ---------- acordes ---------- */

/**
 * El diccionario global es el mismo para todo el mundo y cambia poco, así que
 * se pide una vez por carga de página y se guarda en memoria.
 */
function loadChords() {
  if (chordDict) return Promise.resolve(chordDict);
  return api("GET", "/api/chords/global").then(function (d) {
    chordDict = d.chords || {};
    return chordDict;
  }).catch(function () {
    chordDict = {};
    return chordDict;
  });
}

/** Posiciones de un acorde; [] si no está en el diccionario. */
function chordPositionsOf(name) {
  var entrada = chordDict && chordDict[name];
  if (!entrada) return [];
  return entrada.positions || [];
}

/**
 * Barra de diagramas del visor. Los nombres se transponen con la partitura:
 * si estás tocando dos tonos arriba, los diagramas son los de ahí.
 */
function renderChordBar() {
  if (!chordBarOn || !current) { vChordBar.classList.add("hidden"); return; }
  vChordBar.classList.remove("hidden");
  var nombres = vSongChords(current.body).map(function (n) {
    return vTransposeChord(n, semis, flats);
  });
  vChordBar.innerHTML = "";
  var pintados = 0;
  nombres.forEach(function (nombre) {
    var posiciones = chordPositionsOf(nombre);
    if (!posiciones.length) return;
    pintados++;
    var b = document.createElement("button");
    b.innerHTML = vChordSvg(posiciones[0], 78) + '<span class="nm">' + vEsc(nombre) + '</span>';
    b.title = posiciones.length > 1 ? posiciones.length + " digitaciones" : "";
    b.onclick = function () { openChordModal(nombre); };
    vChordBar.appendChild(b);
  });
  if (!pintados) {
    var aviso = document.createElement("span");
    aviso.className = "none";
    aviso.textContent = nombres.length
      ? "Ningún acorde de esta partitura está en el diccionario todavía."
      : "Esta partitura no lleva acordes marcados.";
    vChordBar.appendChild(aviso);
  }
}

function toggleChordBar() {
  chordBarOn = !chordBarOn;
  vChords.textContent = chordBarOn ? "♦ Ocultar acordes" : "♦ Acordes";
  if (chordBarOn) loadChords().then(renderChordBar);
  else renderChordBar();
}

/** Todas las digitaciones de un acorde, para mirarlas de cerca. */
function openChordModal(nombre) {
  loadChords().then(function () {
    cmName.textContent = nombre;
    cmBody.innerHTML = "";
    var posiciones = chordPositionsOf(nombre);
    posiciones.forEach(function (pos, i) {
      var caja = document.createElement("div");
      caja.className = "chordCard";
      caja.innerHTML = vChordSvg(pos, 116) +
                       '<span class="va">' + (i + 1) + " de " + posiciones.length + "</span>";
      cmBody.appendChild(caja);
    });
    cmEmpty.textContent = posiciones.length ? "" : "Este acorde no está en el diccionario.";
    cmEmpty.classList.toggle("hidden", !!posiciones.length);
    chordModal.classList.remove("hidden");
  });
}

/* ---------- diccionario: administración ---------- */

function showChords(on) {
  chordsView.classList.toggle("hidden", !on);
  listView.classList.toggle("hidden", on);
  if (on) loadChords().then(renderChordList);
}

function renderChordList() {
  var q = chordSearch.value.trim().toLowerCase();
  var nombres = Object.keys(chordDict || {}).sort();
  var vistos = nombres.filter(function (n) { return !q || n.toLowerCase().indexOf(q) >= 0; });
  chordCount.textContent = nombres.length + " acordes en el diccionario";
  chordList.innerHTML = "";
  // Con el diccionario base entero son cientos de tarjetas: sin buscar se
  // enseña un aperitivo y se pide filtrar, que si no el navegador sufre.
  var tope = q ? 400 : 60;
  vistos.slice(0, tope).forEach(function (nombre) {
    var posiciones = chordPositionsOf(nombre);
    var card = document.createElement("button");
    card.className = "chordCard";
    card.innerHTML = (posiciones[0] ? vChordSvg(posiciones[0], 92) : "") +
                     '<span class="nm">' + vEsc(nombre) + '</span>' +
                     '<span class="va">' + posiciones.length +
                     (posiciones.length === 1 ? " posición" : " posiciones") + '</span>';
    card.onclick = function () { openChordEditor(nombre); };
    chordList.appendChild(card);
  });
  var resto = vistos.length - Math.min(vistos.length, tope);
  var msg = !nombres.length
    ? "El diccionario está vacío. Empieza con «Importar diccionario base»."
    : !vistos.length ? "Ningún acorde coincide con la búsqueda."
    : resto > 0 ? "Y " + resto + " más. Busca por nombre para acotar."
    : "";
  chordEmpty.textContent = msg;
  chordEmpty.classList.toggle("hidden", !msg);
}

function openChordEditor(nombre) {
  editingChord = nombre || "";
  chName.value = editingChord;
  chordPositions = JSON.parse(JSON.stringify(chordPositionsOf(editingChord)));
  if (!chordPositions.length) {
    chordPositions = [{ frets: [-1, -1, -1, -1, -1, -1], fingers: [0, 0, 0, 0, 0, 0], baseFret: 1, barres: [] }];
  }
  chMsg.textContent = "";
  chDelete.classList.toggle("hidden", !editingChord);
  renderChordPositions();
  chordEditor.classList.remove("hidden");
}

/** Una fila por posición: seis trastes, traste base, cejillas y el dibujo al lado. */
function renderChordPositions() {
  chPositions.innerHTML = "";
  chordPositions.forEach(function (pos, idx) {
    var fila = document.createElement("div");
    fila.className = "posRow";

    var vista = document.createElement("div");
    vista.innerHTML = vChordSvg(pos, 92);
    fila.appendChild(vista);

    var trastes = document.createElement("div");
    trastes.className = "grp";
    for (var c = 0; c < 6; c++) {
      (function (cuerda) {
        var inp = document.createElement("input");
        inp.type = "number"; inp.min = "-1"; inp.max = "24";
        inp.value = pos.frets[cuerda];
        inp.oninput = function () {
          var v = parseInt(inp.value, 10);
          pos.frets[cuerda] = isNaN(v) ? -1 : v;
          vista.innerHTML = vChordSvg(pos, 92);
        };
        trastes.appendChild(inp);
      })(c);
    }
    var etiqueta = document.createElement("span");
    etiqueta.className = "lbl";
    etiqueta.textContent = "Mi  La  Re  Sol  Si  Mi";
    var envoltorio = document.createElement("div");
    envoltorio.appendChild(etiqueta);
    envoltorio.appendChild(trastes);
    fila.appendChild(envoltorio);

    var base = document.createElement("div");
    base.className = "grp";
    var lb = document.createElement("span");
    lb.className = "lbl"; lb.textContent = "Traste base";
    var ib = document.createElement("input");
    ib.type = "number"; ib.min = "1"; ib.max = "24"; ib.value = pos.baseFret || 1;
    ib.oninput = function () {
      pos.baseFret = parseInt(ib.value, 10) || 1;
      vista.innerHTML = vChordSvg(pos, 92);
    };
    base.appendChild(lb); base.appendChild(ib);
    fila.appendChild(base);

    var cej = document.createElement("div");
    cej.className = "grp";
    var lc = document.createElement("span");
    lc.className = "lbl"; lc.textContent = "Cejilla";
    var ic = document.createElement("input");
    ic.type = "text"; ic.style.width = "70px";
    ic.value = (pos.barres || []).join(",");
    ic.oninput = function () {
      pos.barres = ic.value.split(",").map(function (v) { return parseInt(v, 10); })
                     .filter(function (v) { return !isNaN(v); });
      vista.innerHTML = vChordSvg(pos, 92);
    };
    cej.appendChild(lc); cej.appendChild(ic);
    fila.appendChild(cej);

    var quitar = document.createElement("button");
    quitar.textContent = "Quitar";
    quitar.onclick = function () {
      chordPositions.splice(idx, 1);
      renderChordPositions();
    };
    fila.appendChild(quitar);

    chPositions.appendChild(fila);
  });
}

/** Guarda el diccionario entero: es un blob único y el que manda para todos. */
function saveChordDict(siguiente, mensaje) {
  return api("PUT", "/api/chords/global", { chords: siguiente }).then(function (d) {
    chordDict = siguiente;
    chordSaved.textContent = mensaje || ("Guardado · " + d.count + " acordes");
    chordMsg.textContent = "";
    renderChordList();
    return d;
  });
}

function saveChord() {
  var nombre = chName.value.trim();
  if (!nombre) { chMsg.textContent = "El acorde necesita un nombre."; return; }
  if (!chordPositions.length) { chMsg.textContent = "Añade al menos una posición."; return; }
  var siguiente = {};
  Object.keys(chordDict).forEach(function (k) { siguiente[k] = chordDict[k]; });
  // Renombrar es mover: se borra el nombre viejo para no dejar un duplicado.
  if (editingChord && editingChord !== nombre) delete siguiente[editingChord];
  siguiente[nombre] = { positions: chordPositions };
  saveChordDict(siguiente, "Guardado " + nombre).then(function () {
    chordEditor.classList.add("hidden");
  }).catch(function (e) { chMsg.textContent = e.message; });
}

function deleteChord() {
  if (!editingChord) return;
  if (!confirm("¿Quitar " + editingChord + " del diccionario global? Deja de verse para todo el mundo.")) return;
  var siguiente = {};
  Object.keys(chordDict).forEach(function (k) { if (k !== editingChord) siguiente[k] = chordDict[k]; });
  saveChordDict(siguiente, "Eliminado " + editingChord).then(function () {
    chordEditor.classList.add("hidden");
  }).catch(function (e) { chMsg.textContent = e.message; });
}

function seedChords() {
  chordMsg.textContent = "";
  chordSaved.textContent = "Importando…";
  api("POST", "/api/chords/global/seed").then(function (d) {
    chordDict = null;
    return loadChords().then(function () {
      chordSaved.textContent = "Añadidos " + d.added + " acordes (ya tenías " + d.kept + ")";
      renderChordList();
    });
  }).catch(function (e) {
    chordSaved.textContent = "";
    chordMsg.textContent = e.message;
  });
}

/* ---------- editor ---------- */

/**
 * Pinta el panel derecho con el mismo render que el visor. Las cabeceras
 * #clave: valor no se muestran (son metadatos, no letra), igual que al ver la
 * partitura: lo de la derecha es exactamente lo que vera quien la toque.
 */
function renderEditorPreview() {
  var parsed = vParseSong(eContent.value);
  ePreview.innerHTML = '<div class="sheet">' +
                       vRenderSong(parsed.body || eContent.value) + "</div>";
}

/**
 * Ata el scroll de los dos paneles en proporcion a su recorrido: tienen
 * alturas distintas (la previa envuelve menos que el texto crudo), asi que
 * copiar scrollTop tal cual los desincroniza.
 */
var syncingPanes = false;
function linkPaneScroll(origen, destino) {
  origen.addEventListener("scroll", function () {
    if (syncingPanes) return;
    syncingPanes = true;
    var maxOrigen = origen.scrollHeight - origen.clientHeight;
    var maxDestino = destino.scrollHeight - destino.clientHeight;
    destino.scrollTop = maxOrigen > 0 ? (origen.scrollTop / maxOrigen) * maxDestino : 0;
    requestAnimationFrame(function () { syncingPanes = false; });
  });
}

function showEdit(on) {
  editView.classList.toggle("hidden", !on);
  listView.classList.toggle("hidden", on);
  editMsg.textContent = "";
}
function newSong() {
  editingId = null;
  eTitle.value = ""; eArtist.value = ""; eCapo.value = ""; eContent.value = "";
  eVisibility.value = "private";
  deleteBtn.classList.add("hidden");
  renderEditorPreview();
  showEdit(true);
}
function editCurrent() {
  if (!current) return;
  editingId = current.song.id;
  if (current.song.locked &&
      !confirm("Esta partitura está bloqueada para evitar cambios accidentales. ¿Editarla igualmente?")) return;
  eTitle.value = current.song.title || "";
  eArtist.value = current.song.artist || "";
  eCapo.value = current.song.capo || "";
  eVisibility.value = current.song.visibility || "private";
  eContent.value = current.content || "";
  deleteBtn.classList.remove("hidden");
  renderEditorPreview();
  closeViewer();
  showEdit(true);
}
function saveSong() {
  var payload = {
    title: eTitle.value.trim(),
    artist: eArtist.value.trim(),
    capo: parseInt(eCapo.value, 10) || 0,
    visibility: eVisibility.value,
    content: eContent.value
  };
  var req = editingId ? api("PUT", "/api/songs/" + editingId, payload)
                      : api("POST", "/api/songs", payload);
  req.then(function () {
    showEdit(false);
    tab = "mine";
    refresh();
  }).catch(function (e) { editMsg.textContent = e.message; });
}
function deleteSong() {
  if (!editingId) return;
  if (!confirm("¿Mover esta partitura a la papelera?")) return;
  api("DELETE", "/api/songs/" + editingId).then(function () {
    showEdit(false);
    refresh();
  }).catch(function (e) { editMsg.textContent = e.message; });
}

/* ---------- eventos ---------- */
loginBtn.onclick = function () { registering = false; authTitle.textContent = "Entrar en Vivace";
  authSubmit.textContent = "Entrar"; authSwitch.textContent = "Crear una cuenta";
  nameWrap.classList.add("hidden"); showAuth(true); };
logoutBtn.onclick = function () { setSession("", null); tab = "public"; refresh(); };
authSwitch.onclick = function () {
  registering = !registering;
  authTitle.textContent = registering ? "Crear cuenta en Vivace" : "Entrar en Vivace";
  authSubmit.textContent = registering ? "Crear cuenta" : "Entrar";
  authSwitch.textContent = registering ? "Ya tengo cuenta" : "Crear una cuenta";
  nameWrap.classList.toggle("hidden", !registering);
  authMsg.textContent = "";
};
authSubmit.onclick = submitAuth;
password.addEventListener("keydown", function (e) { if (e.key === "Enter") submitAuth(); });

tabPublic.onclick = function () { tab = "public"; refresh(); };
tabMine.onclick = function () { tab = "mine"; refresh(); };
search.oninput = function () { renderList(); };
tabChords.onclick = function () { tab = "chords"; refresh(); };
vChords.onclick = toggleChordBar;
cmClose.onclick = function () { chordModal.classList.add("hidden"); };
chordModal.onclick = function (e) { if (e.target === chordModal) chordModal.classList.add("hidden"); };
chordSearch.oninput = renderChordList;
chordNew.onclick = function () { openChordEditor(""); };
chordSeed.onclick = seedChords;
chClose.onclick = function () { chordEditor.classList.add("hidden"); };
chAddPos.onclick = function () {
  chordPositions.push({ frets: [-1, -1, -1, -1, -1, -1], fingers: [0, 0, 0, 0, 0, 0], baseFret: 1, barres: [] });
  renderChordPositions();
};
chSave.onclick = saveChord;
chDelete.onclick = deleteChord;
newBtn.onclick = newSong;
eContent.oninput = renderEditorPreview;
linkPaneScroll(eContent, ePreview);
linkPaneScroll(ePreview, eContent);
saveBtn.onclick = saveSong;
cancelEdit.onclick = function () { showEdit(false); };
deleteBtn.onclick = deleteSong;

vClose.onclick = closeViewer;
vEdit.onclick = editCurrent;
vPlay.onclick = function () { scrolling ? stopScroll() : startScroll(); };
vSpeed.oninput = function () { vSpeedVal.textContent = vSpeed.value + " px/s"; };
vUp.onclick = function () { semis = Math.min(11, semis + 1); renderViewer(); };
vDown.onclick = function () { semis = Math.max(-11, semis - 1); renderViewer(); };
vFlat.onclick = function () { flats = !flats; renderViewer(); };
vFontUp.onclick = function () { fontSize = Math.min(40, fontSize + 2); renderViewer(); };
vFontDown.onclick = function () { fontSize = Math.max(11, fontSize - 2); renderViewer(); };
vMetro.onclick = toggleMetro;
vBpm.oninput = function () { vBpmVal.textContent = vBpm.value; metro.bpm = +vBpm.value; };
document.addEventListener("keydown", function (e) {
  if (e.key === "Escape" && viewer.classList.contains("on")) closeViewer();
});

renderBeats(0);
restoreSession().then(function () {
  if (user) tab = "mine";
  refresh();
});
</script>
</body>
</html>`;

/** Marca reducida (4 clavijas) sobre el fondo Nocturno; se sirve en /static/favicon.svg. */
export const FAVICON_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48">
  <rect width="48" height="48" rx="10" fill="#0F1113"></rect>
  <rect x="17.5" y="8" width="13" height="26" fill="none" stroke="#E8B04B" stroke-width="2.6"></rect>
  <g fill="#F2EFE9"><circle cx="11" cy="16" r="2.6"></circle><circle cx="11" cy="26" r="2.6"></circle><circle cx="37" cy="16" r="2.6"></circle><circle cx="37" cy="26" r="2.6"></circle></g>
  <rect x="17.5" y="36" width="13" height="3.5" fill="#E8B04B"></rect>
</svg>`;
