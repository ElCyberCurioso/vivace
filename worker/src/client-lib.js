/*
 * Vivace · librería de cliente (se sirve tal cual en /static/vivace.js)
 *
 * Lógica compartida por la web: pintar acordes sobre la letra, transponer y
 * el metrónomo. Es JavaScript de navegador, no del Worker: aquí solo viaja
 * como texto.
 */

export const CLIENT_JS = `
"use strict";

/* ---------- búsqueda ---------- */

/*
 * La misma normalización que hace el Worker en SQL (normalizarBusqueda en
 * db.js): minúsculas y sin tildes. Tiene que coincidir carácter a carácter,
 * porque el servidor filtra y luego el navegador vuelve a filtrar lo que ya
 * tiene cargado: si uno quitara tildes y el otro no, los resultados del
 * servidor desaparecerían al pintarlos.
 */
var V_VOCALES = [["á","a"],["à","a"],["é","e"],["è","e"],["í","i"],["ì","i"],
                 ["ó","o"],["ò","o"],["ú","u"],["ù","u"],["ü","u"]];

function vNormalizarBusqueda(q) {
  var t = String(q == null ? "" : q).trim().toLowerCase();
  for (var i = 0; i < V_VOCALES.length; i++) {
    t = t.split(V_VOCALES[i][0]).join(V_VOCALES[i][1]);
  }
  return t;
}

/**
 * Cuántas letras o cifras tiene el texto. Es el umbral para ir al servidor: con
 * "a" o "la" se buscaría media base de datos, y los espacios y signos no
 * cuentan como escribir.
 */
function vLetrasYCifras(q) {
  var t = String(q == null ? "" : q);
  var n = 0;
  for (var i = 0; i < t.length; i++) {
    var c = t[i];
    if (c.toLowerCase() !== c.toUpperCase() || (c >= "0" && c <= "9")) n++;
  }
  return n;
}

/* ---------- render de partituras ---------- */

function vEsc(s) {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

/**
 * Una línea del fichero es una línea en pantalla: se conservan las columnas
 * exactas y solo se sustituyen las llaves {X} por el acorde resaltado, así el
 * acorde queda justo encima de su sílaba.
 */
function vRenderLine(line) {
  var html = "", i = 0;
  while (i < line.length) {
    if (line[i] === "{") {
      var end = line.indexOf("}", i);
      if (end !== -1) {
        html += '<span class="chord">' + vEsc(line.slice(i + 1, end)) + "</span>";
        i = end + 1;
        continue;
      }
    }
    html += vEsc(line[i]);
    i++;
  }
  return html;
}

function vRenderSong(text) {
  var lines = (text || "").replace(/\\r\\n/g, "\\n").split("\\n");
  var html = "", inTab = false;
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i];
    var t = line.trim();
    if (t === "{tab}" || t === "{/tab}") { inTab = (t === "{tab}"); continue; }
    if (inTab) { html += '<div class="tab">' + (vEsc(line) || "&nbsp;") + "</div>"; continue; }
    html += '<div class="ln">' + (vRenderLine(line) || "&nbsp;") + "</div>";
  }
  return html;
}

/** Separa las cabeceras #clave: valor del cuerpo de la partitura. */
function vParseSong(text) {
  var lines = (text || "").replace(/\\r\\n/g, "\\n").split("\\n");
  var head = {}, i = 0;
  var RE = /^#([A-Za-z]+):[ \\t]?(.*)$/;
  while (i < lines.length) {
    if (lines[i].trim() === "---") { i++; break; }
    var m = RE.exec(lines[i]);
    if (!m) break;
    head[m[1].toLowerCase()] = m[2].trim();
    i++;
  }
  return { head: head, body: lines.slice(i).join("\\n") };
}

/**
 * ¿Es seguro meter esta URL en un href? Solo http(s).
 *
 * Vive aquí, y no suelto en la página, porque este fichero se sirve tal cual y
 * los tests lo comprueban. La comprobación equivalente que había en el visor
 * estaba ROTA sin que se notara: iba escrita dentro de un template literal,
 * donde la barra invertida se colapsa, y el regex acababa siendo /^https?:/
 * seguido de un comentario. Aceptaba cualquier cosa, "javascript:" incluido.
 */
function vUrlSegura(url) {
  // Sin barras invertidas a propósito: dentro de un template literal se
  // colapsan y el regex queda roto (así se rompió la comprobación anterior).
  var u = String(url || "").trim().toLowerCase();
  return u.indexOf("http://") === 0 || u.indexOf("https://") === 0;
}

/* ---------- diagramas de acorde ---------- */

/**
 * Acordes que aparecen en una partitura, en orden de aparición y sin repetir.
 * Los bloques {tab} se saltan: ahí las llaves no son acordes.
 */
function vSongChords(text) {
  var fuera = [], vistos = {}, inTab = false, i = 0;
  var t = text || "";
  while (i < t.length) {
    if (t[i] === "{") {
      var end = t.indexOf("}", i + 1);
      if (end > i) {
        var dentro = t.slice(i + 1, end).trim();
        var low = dentro.toLowerCase();
        if (low === "tab") inTab = true;
        else if (low === "/tab") inTab = false;
        else if (!inTab && dentro && !vistos[dentro]) { vistos[dentro] = 1; fuera.push(dentro); }
        i = end + 1;
        continue;
      }
    }
    i++;
  }
  return fuera;
}

/**
 * Diagrama en SVG de una digitación.
 *
 * Los trastes van de la sexta cuerda (Mi grave) a la primera: -1 no suena, 0 al
 * aire, N traste N contando desde baseFret. Los colores salen de las
 * variables de la marca, así que el diagrama sigue al tema claro/oscuro.
 */
/*
 * Diagrama de un acorde.
 *
 * Convención de los datos (la de chords-db, que es la que traen el diccionario y
 * la app): "frets" son 6 valores de la 6ª a la 1ª cuerda, con -1 = muda, 0 = al
 * aire y 1..5 = traste RELATIVO a "baseFret" (1 es el propio baseFret). "barres"
 * también es relativo. Esto se dibujaba como si fuera absoluto, así que todo
 * acorde con baseFret > 1 —media biblioteca: cejillas, posiciones altas— salía
 * con los puntos corridos o directamente sin ellos.
 */
function vChordSvg(pos, ancho) {
  var W = ancho || 96;
  var cuerdas = 6, trastes = 5;
  var mx = W * 0.14, my = W * 0.20;
  var gw = W - mx * 2;
  var alto = my + gw * 1.15 + W * 0.10;
  var dx = gw / (cuerdas - 1);
  var dy = (gw * 1.15 - my * 0.2) / trastes;
  var base = pos.baseFret || 1;
  var partes = [];
  // Medidas del kit Accordio (trazo 2,6 px y cejuela 6 px sobre un diagrama de
  // 110 px), en proporción para que valgan igual en la tira pequeña del visor
  // que en la rejilla del diccionario.
  var trazo = W * 0.024;
  var dedos = pos.fingers || [];
  // El número del dedo solo cabe a partir de cierto tamaño; por debajo, punto liso.
  var conDedos = W >= 84;

  partes.push('<svg viewBox="0 0 ' + W + ' ' + alto + '" width="' + W + '" height="' + alto +
              '" class="chordSvg" aria-hidden="true">');
  // Cejuela gruesa solo en primera posición: si empieza más arriba, no hay cejuela.
  if (base === 1) {
    partes.push('<rect x="' + mx + '" y="' + (my - W * 0.055) + '" width="' + gw +
                '" height="' + (W * 0.055) + '" fill="currentColor"/>');
  } else {
    partes.push('<text x="' + (mx - 6) + '" y="' + (my + dy * 0.75) + '" text-anchor="end" ' +
                'font-size="' + (W * 0.13) + '" fill="currentColor" opacity=".7">' + base + '</text>');
  }
  for (var c = 0; c < cuerdas; c++) {
    var x = mx + dx * c;
    partes.push('<line x1="' + x + '" y1="' + my + '" x2="' + x + '" y2="' + (my + dy * trastes) +
                '" stroke="currentColor" stroke-width="' + trazo + '" opacity=".8"/>');
  }
  for (var f = 0; f <= trastes; f++) {
    var y = my + dy * f;
    partes.push('<line x1="' + mx + '" y1="' + y + '" x2="' + (mx + gw) + '" y2="' + y +
                '" stroke="currentColor" stroke-width="' + trazo + '" opacity=".8"/>');
  }

  var frets = pos.frets || [];
  // Cejilla: se pinta como barra entre la primera y la última cuerda que la usan.
  var barres = pos.barres || [];
  for (var b = 0; b < barres.length; b++) {
    var traste = barres[b];
    if (traste < 1 || traste > trastes) continue;
    var desde = -1, hasta = -1;
    for (var k = 0; k < cuerdas; k++) {
      if (frets[k] === traste) { if (desde < 0) desde = k; hasta = k; }
    }
    if (desde < 0 || hasta <= desde) continue;
    var yb = my + dy * (traste - 0.5);
    partes.push('<rect x="' + (mx + dx * desde - dx * 0.22) + '" y="' + (yb - dx * 0.22) +
                '" width="' + (dx * (hasta - desde) + dx * 0.44) + '" height="' + (dx * 0.44) +
                '" rx="' + (dx * 0.22) + '" fill="currentColor"/>');
  }

  for (var s2 = 0; s2 < cuerdas; s2++) {
    var valor = frets[s2];
    var xs = mx + dx * s2;
    if (valor === -1 || valor == null) {
      var r = W * 0.035;
      partes.push('<path d="M' + (xs - r) + ' ' + (my - W * 0.13 - r) + ' L' + (xs + r) + ' ' + (my - W * 0.13 + r) +
                  ' M' + (xs + r) + ' ' + (my - W * 0.13 - r) + ' L' + (xs - r) + ' ' + (my - W * 0.13 + r) +
                  '" stroke="currentColor" stroke-width="1.6" opacity=".65"/>');
    } else if (valor === 0) {
      partes.push('<circle cx="' + xs + '" cy="' + (my - W * 0.13) + '" r="' + (W * 0.038) +
                  '" fill="none" stroke="currentColor" stroke-width="1.6" opacity=".65"/>');
    } else {
      var rel = valor;
      if (rel < 1 || rel > trastes) continue;
      var cy = my + dy * (rel - 0.5);
      partes.push('<circle cx="' + xs + '" cy="' + cy + '" r="' + (dx * 0.3) + '" fill="currentColor"/>');
      // Dedo dentro del punto, en el color de la tarjeta: es lo que distingue
      // "pisa aquí" de "pisa aquí CON ESTE dedo", que es lo que hace falta al
      // aprender el acorde.
      if (conDedos && dedos[s2] > 0) {
        partes.push('<text x="' + xs + '" y="' + (cy + dx * 0.13) + '" text-anchor="middle" ' +
                    'font-size="' + (dx * 0.38) + '" font-weight="600" fill="var(--ac-surface, #F2FAF6)">' +
                    dedos[s2] + '</text>');
      }
    }
  }
  partes.push("</svg>");
  return partes.join("");
}

/* ---------- detección automática de acordes ---------- */

// Una línea cuenta como "línea de acordes" si TODOS sus tokens son acordes en
// notación anglosajona (C, Am7, F#m7b5, D/F#…) o separadores (|, x2, N.C.…).
// Sus acordes se envuelven en {X}; como al pintar se quitan las llaves, las
// columnas sobre la letra no se desplazan. No toca bloques {tab} ni líneas que
// ya tengan alguna llave.
var V_CHORD_RE = /^\\(?[A-G][#b]?(?:maj|min|sus|add|aug|dim|m|M|º|°|\\+|-|b|#|\\d)*(?:\\/[A-G][#b]?)?\\)?$/;
var V_SEP_RE = /^(?:\\||\\/|-+|–|%|x\\d+|\\(x\\d+\\)|N\\.?C\\.?)$/i;

/**
 * Marca los acordes de un texto. Devuelve { text, marked }: el texto con los
 * acordes entre llaves y cuántos ha marcado, para poder decírselo a quien
 * pulsa el botón.
 */
function vDetectChords(text) {
  var lines = (text || "").replace(/\\r\\n/g, "\\n").split("\\n");
  var inTab = false, marked = 0;
  var out = lines.map(function (line) {
    var t = line.trim();
    if (t === "{tab}") { inTab = true; return line; }
    if (t === "{/tab}") { inTab = false; return line; }
    if (inTab || !t || line.indexOf("{") >= 0) return line;
    var chords = 0, tokens = t.split(/\\s+/), corta = false;
    for (var i = 0; i < tokens.length; i++) {
      if (V_CHORD_RE.test(tokens[i])) chords++;
      else if (!V_SEP_RE.test(tokens[i])) { corta = true; break; }  // token de letra
    }
    if (corta || !chords) return line;
    marked += chords;
    return line.split(/(\\s+)/).map(function (part) {
      return part && !/^\\s/.test(part) && V_CHORD_RE.test(part) ? "{" + part + "}" : part;
    }).join("");
  });
  return { text: out.join("\\n"), marked: marked };
}

/* ---------- vídeo de YouTube ---------- */

// Mismo criterio que src/youtube.js, que es quien valida en el servidor. El
// Worker no puede ejecutar este texto (nada de eval), asi que hay dos copias:
// test/youtube.test.mjs las compara caso por caso para que no se separen.
var V_YT_ID = /^[A-Za-z0-9_-]{11}$/;

function vYoutubeId(url) {
  var texto = String(url == null ? "" : url).trim();
  if (!texto) return "";
  if (V_YT_ID.test(texto)) return texto;
  var u;
  try {
    u = new URL(texto.indexOf("//") < 0 ? "https://" + texto : texto);
  } catch (e) {
    return "";
  }
  var host = u.hostname.replace(/^www\./, "").toLowerCase();
  if (host === "youtu.be") {
    var corto = u.pathname.slice(1).split("/")[0];
    return V_YT_ID.test(corto) ? corto : "";
  }
  if (host !== "youtube.com" && host !== "m.youtube.com" && host !== "youtube-nocookie.com") {
    return "";
  }
  var v = u.searchParams.get("v");
  if (v && V_YT_ID.test(v)) return v;
  var partes = u.pathname.split("/").filter(Boolean);
  if (partes.length >= 2 && ["embed", "shorts", "live", "v"].indexOf(partes[0]) >= 0) {
    return V_YT_ID.test(partes[1]) ? partes[1] : "";
  }
  return "";
}

/** URL para incrustar, en el dominio sin cookies. Vacio si no se reconoce. */
function vEmbedUrl(url) {
  var id = vYoutubeId(url);
  return id ? "https://www.youtube-nocookie.com/embed/" + id : "";
}

/* ---------- transposición ---------- */

var V_SHARP = ["C","C#","D","D#","E","F","F#","G","G#","A","A#","B"];
var V_FLAT  = ["C","Db","D","Eb","E","F","Gb","G","Ab","A","Bb","B"];
var V_NORM  = { Db:"C#", Eb:"D#", Gb:"F#", Ab:"G#", Bb:"A#", Cb:"B", Fb:"E" };

function vTransposeChord(name, semis, flats) {
  if ((semis === 0 && !flats) || !name) return name;
  var slash = name.indexOf("/");
  if (slash >= 0) {
    return vTransposeChord(name.slice(0, slash), semis, flats) + "/" +
           vTransposeChord(name.slice(slash + 1), semis, flats);
  }
  var root = (name.length >= 2 && (name[1] === "#" || name[1] === "b"))
    ? name.slice(0, 2) : name.slice(0, 1);
  var suffix = name.slice(root.length);
  var idx = V_SHARP.indexOf(V_NORM[root] || root);
  if (idx < 0) return name;
  var shifted = ((idx + semis) % 12 + 12) % 12;
  return (flats ? V_FLAT : V_SHARP)[shifted] + suffix;
}

/** Transpone solo los {acordes}; el texto y los bloques {tab} no se tocan. */
function vTransposeBody(text, semis, flats) {
  if (semis === 0 && !flats) return text;
  var out = "", i = 0, inTab = false;
  while (i < text.length) {
    if (text[i] === "{") {
      var end = text.indexOf("}", i + 1);
      if (end > i) {
        var low = text.slice(i + 1, end).trim().toLowerCase();
        if (low === "tab") { inTab = true; out += text.slice(i, end + 1); }
        else if (low === "/tab") { inTab = false; out += text.slice(i, end + 1); }
        else if (inTab) out += text.slice(i, end + 1);
        else out += "{" + vTransposeChord(text.slice(i + 1, end).trim(), semis, flats) + "}";
        i = end + 1;
        continue;
      }
    }
    out += text[i];
    i++;
  }
  return out;
}

/* ---------- metrónomo (Web Audio) ---------- */

function VMetronome() {
  this.ctx = null;
  this.timer = null;
  this.bpm = 100;
  this.beatsPerBar = 4;
  this.beat = 0;
  this.onBeat = null;
}

VMetronome.prototype.click = function (accent) {
  var ctx = this.ctx;
  var osc = ctx.createOscillator();
  var gain = ctx.createGain();
  osc.frequency.value = accent ? 1760 : 1175;
  gain.gain.setValueAtTime(0.001, ctx.currentTime);
  gain.gain.exponentialRampToValueAtTime(0.6, ctx.currentTime + 0.002);
  gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.05);
  osc.connect(gain).connect(ctx.destination);
  osc.start();
  osc.stop(ctx.currentTime + 0.06);
};

VMetronome.prototype.start = function () {
  if (this.timer) return;
  if (!this.ctx) this.ctx = new (window.AudioContext || window.webkitAudioContext)();
  if (this.ctx.state === "suspended") this.ctx.resume();
  var self = this;
  this.beat = 0;
  var tick = function () {
    self.beat = self.beat % self.beatsPerBar + 1;
    self.click(self.beat === 1);
    if (self.onBeat) self.onBeat(self.beat);
    self.timer = setTimeout(tick, 60000 / self.bpm);
  };
  tick();
};

VMetronome.prototype.stop = function () {
  if (this.timer) clearTimeout(this.timer);
  this.timer = null;
  this.beat = 0;
  if (this.onBeat) this.onBeat(0);
};

VMetronome.prototype.isRunning = function () { return !!this.timer; };

/* ---------- ZIP (copia de seguridad) ---------- */
/*
 * Se construye y se lee entero en el navegador, así que el Worker no necesita
 * endpoints nuevos. Al crear se usa STORE (el texto es pequeño); al leer se
 * admiten STORE y DEFLATE. Portado tal cual del panel /admin, ya retirado.
 */
var V_CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
    t[n] = c >>> 0;
  }
  return t;
})();
function vCrc32(bytes) {
  let c = 0xFFFFFFFF;
  for (let i = 0; i < bytes.length; i++) c = V_CRC_TABLE[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8);
  return (c ^ 0xFFFFFFFF) >>> 0;
}
function vBuildZip(entries) {            // entries: [{ name, data: Uint8Array }]
  const enc = new TextEncoder();
  const parts = [], central = [];
  let offset = 0;
  const now = new Date();
  const dosTime = ((now.getHours() << 11) | (now.getMinutes() << 5) | (now.getSeconds() >> 1)) & 0xFFFF;
  const dosDate = (((now.getFullYear() - 1980) << 9) | ((now.getMonth() + 1) << 5) | now.getDate()) & 0xFFFF;
  for (const e of entries) {
    const name = enc.encode(e.name);
    const crc = vCrc32(e.data);
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

async function vReadZip(buf) {
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
`;
