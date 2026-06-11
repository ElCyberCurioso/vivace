// Dev check: compila el <script> embebido en ADMIN_HTML sin ejecutarlo.
import { readFileSync } from "fs";

const src = readFileSync(new URL("./src/index.js", import.meta.url), "utf8");
const tpl = src.match(/const ADMIN_HTML = `([\s\S]*)`;/)[1];
// des-escapa el template literal: \\ -> \  y \` -> `
const html = tpl.replace(/\\`/g, "`").replace(/\\\\/g, "\\");
const script = html.match(/<script>([\s\S]*)<\/script>/)[1];
try {
  new Function(script);
  console.log("admin script syntax OK");
} catch (e) {
  console.error("ADMIN SCRIPT ERROR: " + e.message);
  process.exit(1);
}
