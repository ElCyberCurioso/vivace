// Dev check: extrae el núcleo de detección de acordes del ADMIN_HTML y lo
// prueba con texto real. Se prueba `detectChordsInText`, que es la función pura
// que comparten el editor y la detección en lote.
import { readFileSync } from "fs";

const src = readFileSync(new URL("./src/admin-html.js", import.meta.url), "utf8");
const tpl = src.match(/const ADMIN_HTML = `([\s\S]*)`;/)[1];
const html = tpl.replace(/\\`/g, "`").replace(/\\\\/g, "\\");
const script = html.match(/<script>([\s\S]*)<\/script>/)[1];

// Aísla las constantes + la función pura (desde CHORD_RE hasta el cierre de
// detectChordsInText, marcado por su `return { text: ..., marked };`).
const part = script.match(
  /const CHORD_RE[\s\S]*?return \{ text: out\.join\("\\n"\), marked \};\n\}/
)[0];
const detect = new Function(part + "\nreturn detectChordsInText;")();

const cases = [
  // [entrada, salida esperada]
  ["C  G  Am  F\nLetra de la canción", "{C}  {G}  {Am}  {F}\nLetra de la canción"],
  ["F#m7b5 D/F# Cmaj7\nhola", "{F#m7b5} {D/F#} {Cmaj7}\nhola"],
  ["Em | Am | B7  x2\ntexto", "{Em} | {Am} | {B7}  x2\ntexto"],
  ["A mi manera no le toques nada", "A mi manera no le toques nada"],
  ["{C} ya marcada\nD G", "{C} ya marcada\n{D} {G}"],
  ["{tab}\nE|--0--3--\n{/tab}\nC G", "{tab}\nE|--0--3--\n{/tab}\n{C} {G}"],
  ["  C   G\nletra", "  {C}   {G}\nletra"],
];

let fail = 0;
for (const [input, expected] of cases) {
  const got = detect(input).text;
  if (got !== expected) {
    fail++;
    console.error("FAIL\n  in:  " + JSON.stringify(input) +
      "\n  exp: " + JSON.stringify(expected) + "\n  got: " + JSON.stringify(got));
  }
}

// Sin acordes: el texto no se toca y no se marca nada.
const before = "solo letra\nsin acordes";
const none = detect(before);
if (none.text !== before || none.marked !== 0) {
  fail++;
  console.error("FAIL caso sin acordes: " + JSON.stringify(none));
}

// El contador de marcas cuenta cada acorde envuelto.
if (detect("C G Am\nletra").marked !== 3) {
  fail++;
  console.error("FAIL contador de marcas");
}

if (fail) { console.error(fail + " fallos"); process.exit(1); }
console.log("detectChordsInText: " + (cases.length + 2) + " casos OK");
