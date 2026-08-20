import { strict as assert } from "node:assert";
import test from "node:test";
import { handleApi } from "../src/api.js";

const cors = {};

/** BD que estalla si alguien la consulta: prueba que no se llega a los datos. */
const poisonDb = {
  prepare() { throw new Error("no debería consultarse sin sesión"); }
};

/** BD mínima que solo sabe devolver una partitura por id. */
function dbWithSong(song) {
  return {
    prepare(sql) {
      return {
        bind() {
          return {
            first: async () => (sql.includes("FROM songs") ? song : null),
            all: async () => ({ results: [] }),
            run: async () => ({})
          };
        },
        first: async () => null,
        all: async () => ({ results: [] })
      };
    }
  };
}

const req = (method, path, body) => new Request("https://v.test" + path, {
  method,
  headers: body ? { "Content-Type": "application/json" } : {},
  body: body ? JSON.stringify(body) : undefined
});

const url = (path) => new URL("https://v.test" + path);

test("las rutas ajenas a la API se dejan pasar", async () => {
  const res = await handleApi(req("GET", "/list"), { DB: poisonDb }, url("/list"), cors);
  assert.equal(res, null);
});

test("sin base de datos configurada responde 503", async () => {
  const res = await handleApi(req("GET", "/api/songs"), {}, url("/api/songs"), cors);
  assert.equal(res.status, 503);
});

test("las rutas privadas exigen sesión", async () => {
  const env = { DB: poisonDb, AUTH_SECRET: "s" };
  for (const [method, path] of [
    ["GET", "/api/songs"],
    ["POST", "/api/songs"],
    ["GET", "/auth/me"]
  ]) {
    const res = await handleApi(req(method, path), env, url(path), cors);
    assert.equal(res.status, 401, `${method} ${path} debería pedir sesión`);
  }
});

test("un token con basura no autentica ni rompe", async () => {
  const request = new Request("https://v.test/api/songs", {
    headers: { Authorization: "Bearer no-es-un-token" }
  });
  const res = await handleApi(request, { DB: poisonDb, AUTH_SECRET: "s" }, url("/api/songs"), cors);
  assert.equal(res.status, 401);
});

test("una partitura privada no se sirve a un visitante", async () => {
  const song = { id: "s1", owner_id: "u1", visibility: "private", deleted_at: 0, r2_key: "songs/a.txt" };
  const env = { DB: dbWithSong(song), AUTH_SECRET: "s", BUCKET: { get: async () => null } };
  const res = await handleApi(req("GET", "/api/songs/s1"), env, url("/api/songs/s1"), cors);
  assert.equal(res.status, 404);
});

test("una partitura pública sí se sirve sin sesión", async () => {
  const song = { id: "s1", owner_id: "u1", visibility: "public", deleted_at: 0, r2_key: "songs/a.txt" };
  const env = {
    DB: dbWithSong(song), AUTH_SECRET: "s",
    BUCKET: { get: async () => ({ text: async () => "{C}hola" }) }
  };
  const res = await handleApi(req("GET", "/api/songs/s1"), env, url("/api/songs/s1"), cors);
  assert.equal(res.status, 200);
  const data = await res.json();
  assert.equal(data.content, "{C}hola");
  assert.equal(data.song.visibility, "public");
});

test("modificar sin sesión da 401, no 403", async () => {
  const song = { id: "s1", owner_id: "u1", visibility: "public", deleted_at: 0, r2_key: "songs/a.txt" };
  const env = { DB: dbWithSong(song), AUTH_SECRET: "s", BUCKET: {} };
  const res = await handleApi(
    req("PUT", "/api/songs/s1", { title: "x" }), env, url("/api/songs/s1"), cors
  );
  assert.equal(res.status, 401);
});

test("una ruta de API desconocida da 404", async () => {
  const res = await handleApi(
    req("GET", "/api/loquesea"), { DB: poisonDb, AUTH_SECRET: "s" }, url("/api/loquesea"), cors
  );
  assert.equal(res.status, 404);
});
