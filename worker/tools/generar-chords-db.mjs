/*
 * Genera src/chords-db.js a partir de guitar-chords-db-json (szaza, MIT):
 *
 *   node tools/generar-chords-db.mjs <carpeta-del-repo> [posiciones-por-acorde]
 *
 * El repo trae 9.072 acordes con ~99.000 digitaciones y ocupa 36 MB: no cabe en
 * el bundle del Worker ni tiene sentido mandárselo entero al navegador. Aquí se
 * queda con las primeras N posiciones de cada acorde (las de traste más bajo,
 * que es el orden del origen) y se escribe en una cadena compacta que pesa una
 * fracción del JSON equivalente.
 *
 * Conversión de formato. El origen da trastes ABSOLUTOS en base 36 por carácter
 * ("8aa988" = 8,10,10,9,8,8) y la cejilla también absoluta. El diccionario de
 * Accordio —y la app— usan la convención de chords-db: trastes RELATIVOS al
 * traste base, donde 1 es el propio baseFret, 0 al aire y -1 muda.
 */
import { readdirSync, readFileSync, writeFileSync, statSync } from "node:fs";
import { join } from "node:path";

const [, , carpeta, topeArg] = process.argv;
if (!carpeta) {
  console.error("uso: node tools/generar-chords-db.mjs <carpeta-del-repo> [posiciones]");
  process.exit(1);
}
const TOPE = Number(topeArg) || 5;

/** Un carácter del origen -> número de traste; "x" es cuerda muda. */
function traste(c) {
  if (c === "x" || c === "X") return -1;
  const n = parseInt(c, 36);
  return Number.isFinite(n) ? n : -1;
}

/**
 * Traste base de una digitación: el más bajo que se pisa. Hasta el 5 se dibuja
 * con cejuela (base 1) porque así se lee el acorde con las cuerdas al aire; a
 * partir de ahí el diagrama se desplaza y lo dice con su número.
 */
function baseDe(absolutos) {
  const pisados = absolutos.filter((f) => f > 0);
  if (!pisados.length) return 1;
  const alto = Math.max(...pisados);
  return alto <= 5 ? 1 : Math.min(...pisados);
}

const SOSTENIDO_A_BEMOL = { "A#": "Bb", "C#": "Db", "D#": "Eb", "F#": "Gb", "G#": "Ab" };

/** Nombre de acorde a partir de la clave y el sufijo del origen. */
function nombreDe(key, suffix) {
  const [base, bajo] = suffix.split("/");
  const cuerpo = base === "major" ? "" : base === "minor" ? "m" : base;
  return key + cuerpo + (bajo ? "/" + bajo : "");
}

const entradas = [];
let leidos = 0, saltados = 0;

for (const clave of readdirSync(carpeta).sort()) {
  const dir = join(carpeta, clave);
  if (!statSync(dir).isDirectory()) continue;
  for (const fichero of readdirSync(dir).sort()) {
    if (!fichero.endsWith(".json")) continue;
    const datos = JSON.parse(readFileSync(join(dir, fichero), "utf8"));
    const nombre = nombreDe(datos.key, datos.suffix);
    const trozos = [];
    for (const pos of (datos.positions || [])) {
      if (trozos.length >= TOPE) break;
      const absolutos = String(pos.frets || "").split("").map(traste);
      if (absolutos.length !== 6) { saltados++; continue; }
      const base = baseDe(absolutos);
      const rel = absolutos.map((f) => (f <= 0 ? f : f - base + 1));
      // Una digitación que no cabe en las cinco casillas del diagrama no se
      // puede dibujar; mejor no guardarla que guardarla mal.
      if (rel.some((f) => f > 5)) { saltados++; continue; }
      const dedos = String(pos.fingers || "000000").padEnd(6, "0").slice(0, 6);
      const cejillas = String(pos.barres || "").split(",").map((b) => Number(b) - base + 1)
        .filter((b) => b >= 1 && b <= 5);
      trozos.push([
        rel.map((f) => (f < 0 ? "x" : String(f))).join(""),
        dedos,
        base.toString(36),
        cejillas.join("")
      ].join(","));
      leidos++;
    }
    if (trozos.length) entradas.push(nombre + "=" + trozos.join(";"));
  }
}

const compacto = entradas.join("|");
const salida = `/*
 * Vivace · biblioteca de acordes (generada, no editar a mano).
 *
 * Origen: github.com/szaza/guitar-chords-db-json (MIT). Se regenera con
 *   node tools/generar-chords-db.mjs <carpeta-del-repo> [posiciones]
 *
 * ${entradas.length} acordes · ${leidos} digitaciones · hasta ${TOPE} por acorde.
 *
 * Va en una cadena y no en un objeto JSON a propósito: el mismo contenido como
 * literal de objeto multiplica por tres lo que ocupa el bundle, y esto solo se
 * expande cuando el administrador siembra el diccionario.
 *
 * Formato: nombre=posición;posición|nombre=… y cada posición es
 *   trastes,dedos,trasteBase,cejillas   (trastes relativos al base; x = muda)
 */

const COMPACTO = "${compacto}";

/** Sostenidos del origen y su nombre en bemoles, para registrar los dos. */
const BEMOL = { "A#": "Bb", "C#": "Db", "D#": "Eb", "F#": "Gb", "G#": "Ab" };

/** Cambia sostenidos por bemoles en la fundamental y en el bajo del nombre. */
function conBemoles(nombre) {
  const barra = nombre.indexOf("/");
  const cuerpo = barra < 0 ? nombre : nombre.slice(0, barra);
  const bajo = barra < 0 ? "" : nombre.slice(barra + 1);
  const raiz = cuerpo.slice(0, 2) in BEMOL ? BEMOL[cuerpo.slice(0, 2)] + cuerpo.slice(2) : cuerpo;
  const bajo2 = bajo in BEMOL ? BEMOL[bajo] : bajo;
  const salida = raiz + (bajo2 ? "/" + bajo2 : "");
  return salida === nombre ? null : salida;
}

function posicionDe(texto) {
  const [trastes, dedos, base, cejillas] = texto.split(",");
  return {
    frets: trastes.split("").map((c) => (c === "x" ? -1 : Number(c))),
    fingers: dedos.split("").map((c) => Number(c) || 0),
    baseFret: parseInt(base, 36) || 1,
    barres: cejillas ? cejillas.split("").map(Number) : []
  };
}

/**
 * Expande la biblioteca al formato del diccionario. Se hace bajo demanda (solo
 * la usa el sembrado del administrador), no al cargar el Worker.
 */
export function expandirChordDb() {
  const dict = {};
  for (const entrada of COMPACTO.split("|")) {
    const igual = entrada.indexOf("=");
    const nombre = entrada.slice(0, igual);
    const positions = entrada.slice(igual + 1).split(";").map(posicionDe);
    dict[nombre] = { positions };
    // El origen solo escribe sostenidos; media España escribe Bb y Eb.
    const alias = conBemoles(nombre);
    if (alias && !dict[alias]) dict[alias] = { positions };
  }
  return dict;
}
`;
writeFileSync(new URL("../src/chords-db.js", import.meta.url), salida);
console.log(`escrito src/chords-db.js · ${entradas.length} acordes · ${leidos} digitaciones · ${saltados} descartadas`);
