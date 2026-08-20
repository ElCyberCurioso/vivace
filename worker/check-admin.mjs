// Dev check: compila (sin ejecutar) el JavaScript embebido en las páginas que
// sirve el Worker, para que un error de sintaxis no llegue a producción.
//
// Se importan los módulos y se mira la cadena EXACTA que se sirve, en vez de
// recortar el template literal con una expresión regular: escapes como \" o \n
// valen una cosa en el fichero y otra en la página, y esa diferencia ya dejó
// pasar un error a producción.
import { ADMIN_HTML } from "./src/admin-html.js";
import { WEB_HTML } from "./src/web-html.js";
import { CLIENT_JS } from "./src/client-lib.js";

/** Los <script> con cuerpo propio (los que tienen src= no traen código aquí). */
function scriptsOf(html) {
  return [...html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g)]
    .map((m) => m[1]);
}

let failed = 0;

for (const [nombre, html] of [
  ["admin-html.js", ADMIN_HTML],
  ["web-html.js", WEB_HTML],
]) {
  const scripts = scriptsOf(html);
  if (!scripts.length) {
    console.error(`${nombre}: no se encontró ningún <script> embebido`);
    failed++;
    continue;
  }
  scripts.forEach((code, i) => {
    try {
      new Function(code);
    } catch (e) {
      console.error(`${nombre} (script ${i + 1}): ${e.message}`);
      failed++;
    }
  });
}

// La librería de cliente se sirve tal cual en /static/vivace.js: mismo riesgo.
try {
  new Function(CLIENT_JS);
} catch (e) {
  console.error(`client-lib.js: ${e.message}`);
  failed++;
}

if (failed) process.exit(1);
console.log("scripts de las páginas: sintaxis OK");
