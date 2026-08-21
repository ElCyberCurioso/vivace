import { strict as assert } from "node:assert";
import test from "node:test";
import { ARTIST_RULES, FALLBACK_GENRE, TITLE_RULES, guessGenre, normalize } from "../src/genres.js";
import { handleApi } from "../src/api.js";
import { signToken } from "../src/auth.js";

const cors = {};
const SECRETO = "clave-de-prueba";

test("comparar sin tildes ni mayúsculas, que el catálogo está escrito a mano", () => {
  assert.equal(normalize("  JOSÉ Ramón  "), "jose ramon");
  assert.equal(guessGenre({ artist: "PACO DE LUCÍA", title: "Entre dos aguas" }), "Flamenco");
  assert.equal(guessGenre({ artist: "paco de lucia", title: "x" }), "Flamenco");
});

test("el artista manda sobre el título", () => {
  // "Volver" es tango por título, pero si lo firma Estopa es rumba.
  assert.equal(guessGenre({ artist: "Carlos Gardel", title: "Volver" }), "Tango");
  assert.equal(guessGenre({ artist: "Estopa", title: "Volver" }), "Rumba");
});

test("sin artista tira del título", () => {
  assert.equal(guessGenre({ title: "Bésame mucho" }), "Bolero");
  assert.equal(guessGenre({ title: "Noche de paz" }), "Villancico");
  assert.equal(guessGenre({ title: "Ave María" }), "Religioso");
});

test("el artista pegado al título también cuenta", () => {
  assert.equal(guessGenre({ title: "Serrat - Mediterráneo", artist: "" }), "Cantautor");
});

test("lo que no encaja cae en la categoría de reserva, nunca en vacío", () => {
  assert.equal(guessGenre({ title: "Canción rara", artist: "Nadie" }), FALLBACK_GENRE);
  assert.equal(guessGenre({}), FALLBACK_GENRE);
  assert.equal(guessGenre({ title: "x", artist: "y" }, "Sin clasificar"), "Sin clasificar");
});

test("ninguna regla está vacía ni repite categoría dentro de su tabla", () => {
  for (const [genero, fragmentos] of ARTIST_RULES.concat(TITLE_RULES)) {
    assert.ok(genero && genero.length > 1, "categoría sin nombre");
    assert.ok(fragmentos.length, "categoría " + genero + " sin fragmentos");
    for (const f of fragmentos) {
      assert.equal(f, normalize(f), "el fragmento «" + f + "» debe ir normalizado o no casará nunca");
    }
  }
  const artistas = ARTIST_RULES.map((r) => r[0]);
  assert.equal(artistas.length, new Set(artistas).size, "categoría repetida en ARTIST_RULES");
});

/* ---------------------------- la ruta ---------------------------- */

function fakeDb(canciones) {
  const escrituras = [];
  return {
    escrituras,
    prepare(sql) {
      return {
        bind(...valores) {
          return {
            first: async () => (sql.includes("FROM users WHERE id") ? valores[0] === "e1"
              ? { id: "e1", role: "editor", email: "e@x", name: "E" } : null : null),
            all: async () => {
              if (sql.includes("FROM songs") && sql.includes("id > ?")) {
                const desde = valores[0], limite = valores[1];
                return { results: canciones.filter((c) => c.id > desde).slice(0, limite) };
              }
              return { results: [] };
            },
            run: async () => ({})
          };
        },
        first: async () => null,
        all: async () => ({ results: [] })
      };
    },
    async batch(sentencias) {
      escrituras.push(...sentencias.map((s) => s._v));
      return [];
    }
  };
}

// db.batch recibe sentencias ya enlazadas; el fake las marca al enlazar.
function fakeDbConBatch(canciones) {
  const db = fakeDb(canciones);
  const prepareOriginal = db.prepare.bind(db);
  db.prepare = (sql) => {
    const stmt = prepareOriginal(sql);
    const bindOriginal = stmt.bind.bind(stmt);
    stmt.bind = (...v) => {
      const hijo = bindOriginal(...v);
      hijo._v = { sql, valores: v };
      return hijo;
    };
    return stmt;
  };
  return db;
}

async function llamar(env, body, user) {
  const headers = { "Content-Type": "application/json" };
  if (user) headers.Authorization = "Bearer " + await signToken({ sub: user, role: "editor" }, SECRETO);
  return handleApi(new Request("https://v.test/api/genres/auto", {
    method: "POST", headers, body: JSON.stringify(body)
  }), env, new URL("https://v.test/api/genres/auto"), cors);
}

const catalogo = [
  { id: "a", title: "Mediterráneo", artist: "Joan Manuel Serrat", genre: "" },
  { id: "b", title: "Entre dos aguas", artist: "Paco de Lucía", genre: "" },
  { id: "c", title: "Ya clasificada", artist: "Quien sea", genre: "Rock" },
  { id: "d", title: "Canción rara", artist: "Nadie", genre: "" }
];

test("en seco no escribe nada, pero dice lo que haría", async () => {
  const db = fakeDbConBatch(catalogo);
  const res = await llamar({ DB: db, AUTH_SECRET: SECRETO }, { dryRun: true }, "e1");
  assert.equal(res.status, 200);
  const d = await res.json();
  assert.equal(d.updated, 0);
  assert.equal(d.wouldUpdate, 3, "las tres sin categoría");
  assert.deepEqual(d.tally, { Cantautor: 1, Flamenco: 1, Varios: 1 });
  assert.equal(db.escrituras.length, 0);
  assert.equal(d.done, true);
});

test("aplicando, escribe solo las que no tenían categoría", async () => {
  const db = fakeDbConBatch(catalogo);
  const res = await llamar({ DB: db, AUTH_SECRET: SECRETO }, { dryRun: false }, "e1");
  const d = await res.json();
  assert.equal(d.updated, 3);
  assert.equal(db.escrituras.length, 3);
  const ids = db.escrituras.map((e) => e.valores[2]).sort();
  assert.deepEqual(ids, ["a", "b", "d"], "la que ya tenía categoría no se toca");
});

test("con overwrite sí repasa las que ya tenían", async () => {
  const db = fakeDbConBatch(catalogo);
  const res = await llamar({ DB: db, AUTH_SECRET: SECRETO }, { dryRun: false, overwrite: true }, "e1");
  const d = await res.json();
  assert.equal(d.wouldUpdate, 4);
  assert.equal(db.escrituras.length, 4);
});

test("solo el equipo editorial clasifica el catálogo", async () => {
  const db = fakeDbConBatch(catalogo);
  const res = await llamar({ DB: db, AUTH_SECRET: SECRETO }, { dryRun: true });
  assert.equal(res.status, 403);
});
