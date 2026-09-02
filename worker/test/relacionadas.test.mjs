import { strict as assert } from "node:assert";
import test from "node:test";
import { listRelatedSongs } from "../src/db.js";

/*
 * Recomendaciones al pie de la partitura: primero del mismo artista y, si no
 * hay, del mismo estilo. Se prueba contra un D1 de mentira que apunta cada
 * consulta: lo que importa aquí es el CRITERIO (a quién se pregunta y en qué
 * orden), no el motor de SQL.
 */
function dbFalso(respuestas) {
  const consultas = [];
  let i = 0;
  return {
    consultas,
    prepare(sql) {
      return {
        bind(...v) {
          consultas.push({ sql: sql.replace(/\s+/g, " ").trim(), valores: v });
          const salida = respuestas[i++] || [];
          return { all: async () => ({ results: salida }) };
        }
      };
    }
  };
}

const cancion = { id: "s1", artist: "David Bisbal", genre: "Flamenco" };

test("si hay más del mismo artista, se recomiendan esas", async () => {
  const db = dbFalso([[{ id: "s2", title: "Ave María" }]]);
  const r = await listRelatedSongs(db, cancion);
  assert.equal(r.motivo, "artist");
  assert.deepEqual(r.items.map((s) => s.id), ["s2"]);
  // Una sola consulta: no se pregunta por el estilo si el artista ya dio fruto.
  assert.equal(db.consultas.length, 1);
  // Comparado sin tildes ni mayúsculas, igual que el buscador.
  assert.ok(db.consultas[0].valores.includes("david bisbal"));
});

test("sin más del artista, se cae al mismo estilo", async () => {
  const db = dbFalso([[], [{ id: "s3", title: "Entre dos aguas" }]]);
  const r = await listRelatedSongs(db, cancion);
  assert.equal(r.motivo, "genre");
  assert.deepEqual(r.items.map((s) => s.id), ["s3"]);
  assert.equal(db.consultas.length, 2);
  assert.ok(db.consultas[1].valores.includes("flamenco"));
  // Y sin repetir al artista, que ya se ha descartado arriba.
  assert.ok(db.consultas[1].sql.includes("<> ?"));
});

test("nunca se recomienda la partitura que se está leyendo", async () => {
  const db = dbFalso([[{ id: "s2" }]]);
  await listRelatedSongs(db, cancion);
  assert.ok(db.consultas[0].sql.includes("s.id <> ?"));
  assert.equal(db.consultas[0].valores[0], "s1");
});

test("solo se recomienda lo publicado", async () => {
  const db = dbFalso([[{ id: "s2" }]]);
  await listRelatedSongs(db, cancion);
  assert.ok(db.consultas[0].sql.includes("s.visibility = 'public'"));
  assert.ok(db.consultas[0].sql.includes("s.deleted_at = 0"));
});

test("sin artista ni estilo no se recomienda nada, y no se consulta", async () => {
  const db = dbFalso([]);
  const r = await listRelatedSongs(db, { id: "s1", artist: "", genre: "" });
  assert.deepEqual(r, { items: [], motivo: "" });
  assert.equal(db.consultas.length, 0);
});

test("si no hay nada de nada, se devuelve vacío sin motivo", async () => {
  const db = dbFalso([[], []]);
  const r = await listRelatedSongs(db, cancion);
  assert.deepEqual(r.items, []);
  assert.equal(r.motivo, "");
});

test("el tope se acota: ni 0 ni una lista infinita", async () => {
  const db = dbFalso([[{ id: "s2" }], [{ id: "s3" }], [{ id: "s4" }]]);
  await listRelatedSongs(db, cancion, { limit: 999 });
  assert.equal(db.consultas[0].valores.at(-1), 12);
  await listRelatedSongs(db, cancion, { limit: 0 });
  assert.equal(db.consultas[1].valores.at(-1), 6);
  await listRelatedSongs(db, cancion, { limit: 3 });
  assert.equal(db.consultas[2].valores.at(-1), 3);
});

test("con dueño de catálogo, no se sale de él", async () => {
  const db = dbFalso([[{ id: "s2" }]]);
  await listRelatedSongs(db, cancion, { ownerId: "u-admin" });
  assert.ok(db.consultas[0].sql.includes("s.owner_id = ?"));
  assert.ok(db.consultas[0].valores.includes("u-admin"));
});
