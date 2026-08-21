// Dev check: prueba la detección de acordes con texto real. La función vive en
// client-lib.js y la comparten la web y el panel /admin, así que probarla aquí
// cubre las dos páginas.
import { CLIENT_JS } from "./src/client-lib.js";

// La librería es JavaScript de navegador que viaja como texto: se compila y se
// pide la función, sin ejecutar nada más.
const detect = new Function(CLIENT_JS + "\nreturn vDetectChords;")();

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
console.log("vDetectChords: " + (cases.length + 2) + " casos OK");
