import { strict as assert } from "node:assert";
import test from "node:test";
import { listPublicSongs, listOwnSongs, normalizarBusqueda } from "../src/db.js";
import { CLIENT_JS } from "../src/client-lib.js";

/*
 * El buscador de la web filtraba solo lo que ya estaba descargado (la primera
 * página de 60), así que una partitura que cayera más atrás en el catálogo no
 * salía hasta pulsar «Cargar más». Ahora la búsqueda va al Worker; esto fija
 * las dos mitades: el SQL que sale de aquí y que el navegador normalice igual.
 */

/** D1 de mentira que solo apunta la consulta y los valores. */
function dbEspia() {
  const visto = { sql: "", valores: [] };
  return {
    visto,
    prepare(sql) {
      visto.sql = sql.replace(/\s+/g, " ").trim();
      return {
        bind(...v) { visto.valores = v; return this; },
        all: async () => ({ results: [] })
      };
    }
  };
}

test("el catálogo busca en título y artista, sin tildes", async () => {
  const db = dbEspia();
  await listPublicSongs(db, null, { q: "BuLERÍA" });
  assert.match(db.visto.sql, /LOWER\(s\.title\)/);
  assert.match(db.visto.sql, /LOWER\(s\.artist\)/);
  assert.match(db.visto.sql, /LIKE \? ESCAPE/);
  // El patrón viaja ya normalizado: así casa «Bulería» aunque se teclee sin acento.
  assert.deepEqual(db.visto.valores.slice(0, 2), ["%buleria%", "%buleria%"]);
});

test("las partituras propias también se buscan", async () => {
  const db = dbEspia();
  await listOwnSongs(db, "u1", { q: "zombie" });
  assert.match(db.visto.sql, /LIKE \? ESCAPE/);
  assert.deepEqual(db.visto.valores.slice(0, 3), ["u1", "%zombie%", "%zombie%"]);
});

test("sin texto no se añade ninguna condición de búsqueda", async () => {
  const db = dbEspia();
  await listPublicSongs(db, null, {});
  assert.doesNotMatch(db.visto.sql, /LIKE/);
});

test("los comodines de LIKE se escapan: % no lo devuelve todo", async () => {
  const db = dbEspia();
  await listPublicSongs(db, null, { q: "100%_ok" });
  assert.equal(db.visto.valores[0], "%100\\%\\_ok%");
});

test("la copia del navegador normaliza igual que el servidor", () => {
  // Si una mitad quitara tildes y la otra no, el navegador escondería al pintar
  // justo los resultados que el servidor acaba de encontrar.
  const vNormalizarBusqueda = new Function(CLIENT_JS + "\nreturn vNormalizarBusqueda;")();
  for (const t of ["Bulería", "  ZOMBIE ", "Mañana", "Où", "Über", "canción 3", ""]) {
    assert.equal(vNormalizarBusqueda(t), normalizarBusqueda(t), "discrepan con " + JSON.stringify(t));
  }
});

test("el umbral cuenta letras y cifras, no espacios ni signos", () => {
  const vLetrasYCifras = new Function(CLIENT_JS + "\nreturn vLetrasYCifras;")();
  assert.equal(vLetrasYCifras("ab"), 2);
  assert.equal(vLetrasYCifras("a b"), 2);
  assert.equal(vLetrasYCifras("a-b."), 2);
  assert.equal(vLetrasYCifras("ab1"), 3);
  assert.equal(vLetrasYCifras("añ2"), 3);
  assert.equal(vLetrasYCifras("   "), 0);
});
