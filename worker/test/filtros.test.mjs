import { strict as assert } from "node:assert";
import test from "node:test";
import { listOwnSongs, listTrashedSongs } from "../src/db.js";

/*
 * Los filtros de "Mis partituras" se resolvían en el navegador, sobre la página
 * que ya estaba descargada. Con las primeras treinta partituras publicadas,
 * "Solo privadas" enseñaba la lista vacía y había que pulsar "Cargar más" una
 * vez por página hasta que apareciera algo. Esto fija que el filtro viaje a SQL.
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

test("«Solo privadas» filtra en SQL, no en el navegador", async () => {
  const db = dbEspia();
  await listOwnSongs(db, "u1", { visibility: "private" });
  assert.match(db.visto.sql, /visibility = \?/);
  assert.deepEqual(db.visto.valores.slice(0, 2), ["u1", "private"]);
});

test("una visibilidad que no existe se ignora", async () => {
  const db = dbEspia();
  await listOwnSongs(db, "u1", { visibility: "loquesea" });
  assert.doesNotMatch(db.visto.sql, /visibility = \?/);
});

test("favoritas, categoría y carpeta también van en la consulta", async () => {
  const db = dbEspia();
  await listOwnSongs(db, "u1", { favorite: true, genre: "Rock", playlist: "p7" });
  assert.match(db.visto.sql, /favorite = 1/);
  assert.match(db.visto.sql, /LOWER\(genre\) = \?/);
  assert.match(db.visto.sql, /playlist_id = \?/);
  assert.deepEqual(db.visto.valores.slice(0, 3), ["u1", "rock", "p7"]);
});

test("«sin carpeta» es playlist_id IS NULL, que no es lo mismo que «todas»", async () => {
  const db = dbEspia();
  await listOwnSongs(db, "u1", { playlist: "none" });
  assert.match(db.visto.sql, /playlist_id IS NULL/);
  assert.doesNotMatch(db.visto.sql, /playlist_id = \?/);
});

test("el orden lo elige quien pregunta, y lo desconocido cae en título", async () => {
  const porFecha = dbEspia();
  await listOwnSongs(porFecha, "u1", { sort: "recent" });
  assert.match(porFecha.visto.sql, /ORDER BY created_at DESC/);

  const raro = dbEspia();
  await listOwnSongs(raro, "u1", { sort: "; DROP TABLE songs" });
  assert.match(raro.visto.sql, /ORDER BY title COLLATE NOCASE ASC/);
});

test("la papelera admite los mismos filtros, con su propio orden", async () => {
  const db = dbEspia();
  await listTrashedSongs(db, "u1", { visibility: "private" });
  assert.match(db.visto.sql, /deleted_at > 0/);
  assert.match(db.visto.sql, /visibility = \?/);
  assert.match(db.visto.sql, /ORDER BY deleted_at DESC/);
});
