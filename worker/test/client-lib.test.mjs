import { strict as assert } from "node:assert";
import test from "node:test";
import { CLIENT_JS } from "../src/client-lib.js";

// La librería se sirve como texto al navegador: se evalúa aquí para poder
// probar de verdad el render y la transposición que verá el usuario.
const lib = new Function(
  CLIENT_JS + "\nreturn { vRenderSong, vTransposeBody, vTransposeChord, vParseSong };"
)();

test("los acordes se resaltan y cada línea se mantiene", () => {
  const html = lib.vRenderSong("{Am}Hola\nsin acordes");
  assert.match(html, /<span class="chord">Am<\/span>Hola/);
  assert.equal((html.match(/class="ln"/g) || []).length, 2);
});

test("el HTML del contenido se escapa", () => {
  const html = lib.vRenderSong("<script>alerta</script>");
  assert.ok(!html.includes("<script>"));
  assert.match(html, /&lt;script&gt;/);
});

test("los bloques de tablatura se marcan aparte", () => {
  const html = lib.vRenderSong("{tab}\nе|--0--\n{/tab}");
  assert.match(html, /class="tab"/);
  assert.ok(!html.includes("{tab}"));
});

test("transponer respeta el sufijo y el bajo", () => {
  assert.equal(lib.vTransposeChord("Am7", 2, false), "Bm7");
  assert.equal(lib.vTransposeChord("D/F#", 2, false), "E/G#");
});

test("con bemoles se escribe la escala bemol", () => {
  assert.equal(lib.vTransposeChord("C", 1, true), "Db");
  assert.equal(lib.vTransposeChord("C", 1, false), "C#");
});

test("la tablatura no se transpone", () => {
  const src = "{C}letra\n{tab}\ne|--0--\n{/tab}\n{G}fin";
  assert.equal(lib.vTransposeBody(src, 2, false), "{D}letra\n{tab}\ne|--0--\n{/tab}\n{A}fin");
});

test("sin cambios devuelve el texto tal cual", () => {
  const src = "{C}igual";
  assert.equal(lib.vTransposeBody(src, 0, false), src);
});

test("las cabeceras se separan del cuerpo", () => {
  const parsed = lib.vParseSong("#title: Prueba\n#artist: Yo\n---\n{C}letra");
  assert.equal(parsed.head.title, "Prueba");
  assert.equal(parsed.head.artist, "Yo");
  assert.equal(parsed.body, "{C}letra");
});

test("una partitura sin cabeceras se queda entera como cuerpo", () => {
  const parsed = lib.vParseSong("{C}solo letra");
  assert.equal(parsed.body, "{C}solo letra");
});
