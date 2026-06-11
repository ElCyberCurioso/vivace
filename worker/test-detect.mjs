// Dev check: extrae detectChords del ADMIN_HTML y lo prueba con texto real.
import { readFileSync } from "fs";

const src = readFileSync(new URL("./src/index.js", import.meta.url), "utf8");
const tpl = src.match(/const ADMIN_HTML = `([\s\S]*)`;/)[1];
const html = tpl.replace(/\\`/g, "`").replace(/\\\\/g, "\\");
const script = html.match(/<script>([\s\S]*)<\/script>/)[1];

// Aísla las constantes y la función bajo prueba con stubs mínimos.
const part = script.match(/const CHORD_RE[\s\S]*?\n}\n/)[0];
let status = "";
const ctx = {
  current: {},
  body: { value: "" },
  setStatus: (t, c) => { status = t + "|" + c; },
  onEdit: () => {},
};
const fn = new Function(
  "current", "body", "setStatus", "onEdit",
  part + "\nreturn detectChords;"
)(ctx.current, ctx.body, ctx.setStatus, ctx.onEdit);

function run(text) {
  ctx.body.value = text;
  status = "";
  fn();
  return ctx.body.value;
}

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
  const got = run(input);
  if (got !== expected) {
    fail++;
    console.error("FAIL\n  in:  " + JSON.stringify(input) +
      "\n  exp: " + JSON.stringify(expected) + "\n  got: " + JSON.stringify(got));
  }
}
// caso sin acordes: no debe tocar el texto y debe avisar
const before = "solo letra\nsin acordes";
if (run(before) !== before || !status.includes("error")) { fail++; console.error("FAIL caso sin acordes: " + status); }

if (fail) { console.error(fail + " fallos"); process.exit(1); }
console.log("detectChords: " + (cases.length + 1) + " casos OK");
