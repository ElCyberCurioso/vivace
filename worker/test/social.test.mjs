import { strict as assert } from "node:assert";
import test from "node:test";
import { handleApi } from "../src/api.js";
import { signToken } from "../src/auth.js";
import { canComment, canDeleteComment, canRate, canSetLocked } from "../src/permissions.js";

const cors = {};
const SECRETO = "clave-de-prueba";

const usuario = { id: "u1", role: "user" };
const otro = { id: "u2", role: "user" };
const editor = { id: "e1", role: "editor" };
const publica = { id: "s2", owner_id: "u9", visibility: "public", deleted_at: 0 };
const privadaAjena = { id: "s3", owner_id: "u9", visibility: "private", deleted_at: 0 };

test("comentar y valorar: hace falta sesión y poder ver la partitura", () => {
  assert.equal(canComment(usuario, publica), true);
  assert.equal(canRate(usuario, publica), true);
  assert.equal(canComment(null, publica), false, "sin sesión no se comenta");
  assert.equal(canRate(null, publica), false);
  assert.equal(canComment(usuario, privadaAjena), false, "si no la ves, no opinas");
});

test("un comentario lo borra su autor o el equipo editorial", () => {
  const mio = { id: "c1", author_id: "u1", deleted_at: 0 };
  assert.equal(canDeleteComment(usuario, mio), true);
  assert.equal(canDeleteComment(otro, mio), false);
  assert.equal(canDeleteComment(editor, mio), true);
  assert.equal(canDeleteComment(usuario, { ...mio, deleted_at: 5 }), false, "ya estaba borrado");
});

test("el candado lo pone quien puede editar", () => {
  assert.equal(canSetLocked(usuario, { owner_id: "u1", visibility: "private", deleted_at: 0 }), true);
  assert.equal(canSetLocked(otro, publica), false);
  assert.equal(canSetLocked(editor, publica), true);
});

/* ---------------------------- rutas ---------------------------- */

function fakeDb(estado) {
  const escrituras = [];
  return {
    escrituras,
    prepare(sql) {
      const ejecutar = (valores) => {
        if (sql.includes("FROM users WHERE id")) return estado.users[valores[0]] || null;
        if (sql.includes("FROM songs WHERE id")) return estado.songs[valores[0]] || null;
        if (sql.includes("FROM comments c")) return estado.comments[valores[0]] || null;
        if (sql.includes("FROM song_versions WHERE id")) return estado.versions[valores[0]] || null;
        return null;
      };
      return {
        bind(...valores) {
          return {
            first: async () => ejecutar(valores),
            all: async () => ({ results: [] }),
            run: async () => {
              if (sql.includes("INSERT INTO comments")) {
                escrituras.push({ tipo: "comentario", body: valores[3], autor: valores[2] });
              } else if (sql.includes("INSERT INTO ratings")) {
                escrituras.push({ tipo: "voto", songId: valores[0], versionId: valores[1],
                                  userId: valores[2], stars: valores[3] });
              } else if (sql.startsWith("DELETE FROM ratings")) {
                escrituras.push({ tipo: "voto-retirado", versionId: valores[1] });
              } else if (sql.includes("UPDATE comments SET deleted_at")) {
                escrituras.push({ tipo: "comentario-borrado", id: valores[1] });
              }
              return {};
            }
          };
        },
        first: async () => ejecutar([]),
        all: async () => ({ results: [] })
      };
    }
  };
}

function escenario() {
  const estado = {
    users: { u1: { ...usuario, email: "u@x", name: "Usuaria" }, e1: { ...editor, email: "e@x", name: "Editor" } },
    songs: { s2: { ...publica, title: "Publicada", r2_key: "songs/s2.txt" },
             s3: { ...privadaAjena, title: "Ajena", r2_key: "songs/s3.txt" } },
    comments: {}, versions: {}
  };
  const db = fakeDb(estado);
  return { estado, db, env: { DB: db, BUCKET: { async get() { return null; }, async put() {} }, AUTH_SECRET: SECRETO } };
}

async function llamar(env, method, path, body, user) {
  const headers = { "Content-Type": "application/json" };
  if (user) headers.Authorization = "Bearer " + await signToken({ sub: user.id, role: user.role }, SECRETO);
  return handleApi(new Request("https://v.test" + path, {
    method, headers, body: body ? JSON.stringify(body) : undefined
  }), env, new URL("https://v.test" + path), cors);
}

test("sin sesión no se comenta, pero los comentarios se leen", async () => {
  const { env } = escenario();
  assert.equal((await llamar(env, "GET", "/api/songs/s2/comments")).status, 200);
  assert.equal((await llamar(env, "POST", "/api/songs/s2/comments", { body: "hola" })).status, 401);
});

test("un comentario vacío o kilométrico se rechaza", async () => {
  const { estado, env, db } = escenario();
  assert.equal((await llamar(env, "POST", "/api/songs/s2/comments", { body: "   " }, estado.users.u1)).status, 400);
  const largo = "x".repeat(2001);
  assert.equal((await llamar(env, "POST", "/api/songs/s2/comments", { body: largo }, estado.users.u1)).status, 400);
  assert.equal(db.escrituras.length, 0);
});

test("un comentario normal se guarda recortado", async () => {
  const { estado, env, db } = escenario();
  const res = await llamar(env, "POST", "/api/songs/s2/comments", { body: "  muy buena  " }, estado.users.u1);
  assert.equal(res.status, 201);
  const guardado = db.escrituras.find((e) => e.tipo === "comentario");
  assert.equal(guardado.body, "muy buena");
  assert.equal(guardado.autor, "u1");
});

test("no se comenta en una partitura que no puedes ver", async () => {
  const { estado, env } = escenario();
  const res = await llamar(env, "POST", "/api/songs/s3/comments", { body: "hola" }, estado.users.u1);
  assert.equal(res.status, 404, "para quien no la ve, no existe");
});

test("valorar exige sesión y una puntuación de 0 a 5", async () => {
  const { estado, env } = escenario();
  assert.equal((await llamar(env, "PUT", "/api/songs/s2/ratings", { stars: 4 })).status, 401);
  assert.equal((await llamar(env, "PUT", "/api/songs/s2/ratings", { stars: 6 }, estado.users.u1)).status, 400);
  assert.equal((await llamar(env, "PUT", "/api/songs/s2/ratings", { stars: -1 }, estado.users.u1)).status, 400);
  assert.equal((await llamar(env, "PUT", "/api/songs/s2/ratings", { stars: 2.5 }, estado.users.u1)).status, 400);
});

test("votar el Original guarda el voto con versión vacía", async () => {
  const { estado, env, db } = escenario();
  const res = await llamar(env, "PUT", "/api/songs/s2/ratings", { stars: 5 }, estado.users.u1);
  assert.equal(res.status, 200);
  const voto = db.escrituras.find((e) => e.tipo === "voto");
  assert.equal(voto.versionId, "", "el Original es la cadena vacía");
  assert.equal(voto.stars, 5);
  assert.equal(voto.userId, "u1");
});

test("un cero retira el voto en vez de guardar un cero", async () => {
  const { estado, env, db } = escenario();
  const res = await llamar(env, "PUT", "/api/songs/s2/ratings", { stars: 0 }, estado.users.u1);
  assert.equal(res.status, 200);
  assert.equal(db.escrituras.some((e) => e.tipo === "voto"), false);
  assert.equal(db.escrituras.some((e) => e.tipo === "voto-retirado"), true);
});

test("no se puede colar el voto de una versión de otra partitura", async () => {
  const { estado, env } = escenario();
  estado.versions.v9 = { id: "v9", song_id: "otra", deleted_at: 0 };
  const res = await llamar(env, "PUT", "/api/songs/s2/ratings",
    { versionId: "v9", stars: 5 }, estado.users.u1);
  assert.equal(res.status, 404);
});

test("borrar el comentario de otro no cuela; el editor sí puede", async () => {
  const { estado, env, db } = escenario();
  estado.comments.c1 = { id: "c1", song_id: "s2", author_id: "u9", body: "hola", deleted_at: 0 };
  assert.equal((await llamar(env, "DELETE", "/api/comments/c1", null, estado.users.u1)).status, 403);
  assert.equal(db.escrituras.length, 0);
  assert.equal((await llamar(env, "DELETE", "/api/comments/c1", null, estado.users.e1)).status, 200);
  assert.equal(db.escrituras.some((e) => e.tipo === "comentario-borrado"), true);
});
