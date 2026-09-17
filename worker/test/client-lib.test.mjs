import { strict as assert } from "node:assert";
import test from "node:test";
import { CLIENT_JS } from "../src/client-lib.js";

// La librería se sirve como texto al navegador: se evalúa aquí para poder
// probar de verdad el render y la transposición que verá el usuario.
const lib = new Function(
  CLIENT_JS + "\nreturn { acRenderSong, acTransposeBody, acTransposeChord, acParseSong," +
              " acStripChords };"
)();

test("los acordes se resaltan y cada línea se mantiene", () => {
  const html = lib.acRenderSong("{Am}Hola\nsin acordes");
  assert.match(html, /<span class="chord">Am<\/span>Hola/);
  assert.equal((html.match(/class="ln"/g) || []).length, 2);
});

test("el HTML del contenido se escapa", () => {
  const html = lib.acRenderSong("<script>alerta</script>");
  assert.ok(!html.includes("<script>"));
  assert.match(html, /&lt;script&gt;/);
});

test("los bloques de tablatura se marcan aparte", () => {
  const html = lib.acRenderSong("{tab}\nе|--0--\n{/tab}");
  assert.match(html, /class="tab"/);
  assert.ok(!html.includes("{tab}"));
});

test("transponer respeta el sufijo y el bajo", () => {
  assert.equal(lib.acTransposeChord("Am7", 2, false), "Bm7");
  assert.equal(lib.acTransposeChord("D/F#", 2, false), "E/G#");
});

test("con bemoles se escribe la escala bemol", () => {
  assert.equal(lib.acTransposeChord("C", 1, true), "Db");
  assert.equal(lib.acTransposeChord("C", 1, false), "C#");
});

test("la tablatura no se transpone", () => {
  const src = "{C}letra\n{tab}\ne|--0--\n{/tab}\n{G}fin";
  assert.equal(lib.acTransposeBody(src, 2, false), "{D}letra\n{tab}\ne|--0--\n{/tab}\n{A}fin");
});

test("sin cambios devuelve el texto tal cual", () => {
  const src = "{C}igual";
  assert.equal(lib.acTransposeBody(src, 0, false), src);
});

test("las cabeceras se separan del cuerpo", () => {
  const parsed = lib.acParseSong("#title: Prueba\n#artist: Yo\n---\n{C}letra");
  assert.equal(parsed.head.title, "Prueba");
  assert.equal(parsed.head.artist, "Yo");
  assert.equal(parsed.body, "{C}letra");
});

test("una partitura sin cabeceras se queda entera como cuerpo", () => {
  const parsed = lib.acParseSong("{C}solo letra");
  assert.equal(parsed.body, "{C}solo letra");
});

test("solo letra: las líneas de acordes se van enteras", () => {
  const src = "{C}      {G}\nCasa árbol\n\nD  A\nsegunda línea";
  assert.equal(lib.acStripChords(src), "Casa árbol\n\nsegunda línea");
});

test("solo letra: el acorde intercalado no deja un agujero en la frase", () => {
  assert.equal(lib.acStripChords("{Am} Casa   {C} árbol"), "Casa árbol");
});

test("solo letra: la tablatura es cifra, no letra", () => {
  const src = "letra\n{tab}\ne|--0--3--\n{/tab}\nmás letra";
  assert.equal(lib.acStripChords(src), "letra\nmás letra");
});

test("solo letra: los blancos no se amontonan ni sobran al final", () => {
  assert.equal(lib.acStripChords("uno\n\n\n{C}\n\ndos\n{G}\n\n"), "uno\n\ndos");
});

test("solo letra: la sangría de la letra y los rótulos se respetan", () => {
  assert.equal(lib.acStripChords("[Estribillo]\n   sangrada\n{C}canta"),
               "[Estribillo]\n   sangrada\ncanta");
});

test("solo letra: una letra sin cifrar se queda igual", () => {
  const src = "A mi manera no le toques nada\nsegunda";
  assert.equal(lib.acStripChords(src), src);
});

test("acUrlSegura solo acepta http y https", () => {
  const acUrlSegura = new Function(CLIENT_JS + "; return acUrlSegura;")();
  for (const buena of ["http://a.test", "https://a.test/x?y=1", "HTTPS://A.TEST",
                       // Pegar una dirección suele traerse espacios de sobra.
                       "  https://a.test/x  "]) {
    assert.equal(acUrlSegura(buena), true, buena);
  }
  // El caso que importa: esto acababa en el href del enlace "Original ↗", que
  // ve cualquiera que abra la partitura.
  for (const mala of ["javascript:alert(1)", "JavaScript:alert(1)", "data:text/html,x",
                      "//evil.test", "ftp://a.test", "", null, undefined, "   ",
                      // Los otros esquemas que ejecutan o incrustan contenido.
                      "vbscript:msgbox(1)", "file:///etc/passwd", "blob:https://a.test/x",
                      // Y lo que ni siquiera es texto: así llega desde la API.
                      42]) {
    assert.equal(acUrlSegura(mala), false, String(mala));
  }
});

test("ningún regex servido al navegador se ha comido su barra invertida", async () => {
  // Toda la web va dentro de template literals, donde "\/" se colapsa a "/".
  // Un regex escrito con una sola barra invertida se convierte en otra cosa —
  // normalmente en un regex corto seguido de un comentario de línea— y deja de
  // filtrar sin que nada falle ni avise. Así se rompió la comprobación de
  // protocolo del enlace "Original", que pasó a aceptar javascript:.
  const { WEB_APP_JS, WEB_CSS } = await import("../src/web-html.js");
  const sospechoso = /\/[^/\n]*:\/\/\//;      // p. ej.  /^https?:///i
  for (const [nombre, texto] of [["CLIENT_JS", CLIENT_JS], ["WEB_APP_JS", WEB_APP_JS], ["WEB_CSS", WEB_CSS]]) {
    const m = sospechoso.exec(texto);
    assert.equal(m, null, `${nombre} trae un regex con la barra colapsada: ${m && m[0]}`);
  }
});
