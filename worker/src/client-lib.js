/*
 * Vivace · librería de cliente (se sirve tal cual en /static/vivace.js)
 *
 * Lógica compartida por la web: pintar acordes sobre la letra, transponer y
 * el metrónomo. Es JavaScript de navegador, no del Worker: aquí solo viaja
 * como texto.
 */

export const CLIENT_JS = `
"use strict";

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

  partes.push('<svg viewBox="0 0 ' + W + ' ' + alto + '" width="' + W + '" height="' + alto +
              '" class="chordSvg" aria-hidden="true">');
  // Cejuela gruesa solo en primera posición: si empieza más arriba, no hay cejuela.
  if (base === 1) {
    partes.push('<rect x="' + mx + '" y="' + (my - 3) + '" width="' + gw + '" height="3.4" fill="currentColor"/>');
  } else {
    partes.push('<text x="' + (mx - 6) + '" y="' + (my + dy * 0.75) + '" text-anchor="end" ' +
                'font-size="' + (W * 0.13) + '" fill="currentColor" opacity=".7">' + base + '</text>');
  }
  for (var c = 0; c < cuerdas; c++) {
    var x = mx + dx * c;
    partes.push('<line x1="' + x + '" y1="' + my + '" x2="' + x + '" y2="' + (my + dy * trastes) +
                '" stroke="currentColor" stroke-width="1" opacity=".55"/>');
  }
  for (var f = 0; f <= trastes; f++) {
    var y = my + dy * f;
    partes.push('<line x1="' + mx + '" y1="' + y + '" x2="' + (mx + gw) + '" y2="' + y +
                '" stroke="currentColor" stroke-width="1" opacity=".55"/>');
  }

  var frets = pos.frets || [];
  // Cejilla: se pinta como barra entre la primera y la última cuerda que la usan.
  var barres = pos.barres || [];
  for (var b = 0; b < barres.length; b++) {
    var traste = barres[b] - base + 1;
    if (traste < 1 || traste > trastes) continue;
    var desde = -1, hasta = -1;
    for (var k = 0; k < cuerdas; k++) {
      if (frets[k] - base + 1 === traste) { if (desde < 0) desde = k; hasta = k; }
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
      var rel = valor - base + 1;
      if (rel < 1 || rel > trastes) continue;
      partes.push('<circle cx="' + xs + '" cy="' + (my + dy * (rel - 0.5)) + '" r="' + (dx * 0.28) +
                  '" fill="currentColor"/>');
    }
  }
  partes.push("</svg>");
  return partes.join("");
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
`;
