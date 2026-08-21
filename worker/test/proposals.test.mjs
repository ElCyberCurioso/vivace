import { strict as assert } from "node:assert";
import test from "node:test";
import { handleApi } from "../src/api.js";
import { signToken } from "../src/auth.js";

const cors = {};
const SECRETO = "clave-de-prueba";

/**
 * D1 de mentira: reconoce las consultas por un trozo de su SQL y apunta lo que
 * se escribe, que es justo lo que hay que comprobar al aprobar una propuesta.
 */
function fakeDb(estado) {
  const escrituras = [];
  const db = {
    escrituras,
    prepare(sql) {
      const ejecutar = (valores) => {
        if (sql.includes("FROM users WHERE id")) return estado.users[valores[0]] || null;
        if (sql.includes("FROM songs s JOIN users")) return null;
        if (sql.includes("FROM songs WHERE id")) return estado.songs[valores[0]] || null;
        if (sql.includes("FROM proposals p")) return estado.proposals[valores[0]] || null;
        if (sql.includes("MAX(position)")) return { maxima: 0 };
        return null;
      };
      return {
        bind(...valores) {
          return {
            first: async () => ejecutar(valores),
            all: async () => ({ results: [] }),
            run: async () => {
              if (sql.startsWith("UPDATE songs SET")) {
                // Orden del UPDATE: title, artist, genre, capo, source_url, locked,
                // visibility, youtube_url, updated_at, id.
                escrituras.push({ tipo: "song", visibility: valores[6], youtube: valores[7],
                                  id: valores[valores.length - 1] });
              } else if (sql.includes("INSERT INTO songs")) {
                escrituras.push({ tipo: "alta", title: valores[3], visibility: valores[9] });
              } else if (sql.includes("INSERT INTO song_versions")) {
                escrituras.push({ tipo: "version", songId: valores[1], name: valores[2],
                                  r2Key: valores[3], capo: valores[4], authorId: valores[7] });
              } else if (sql.startsWith("UPDATE proposals SET status")) {
                escrituras.push({ tipo: "proposal", status: valores[0], reviewerId: valores[1] });
                const p = estado.proposals[valores[4]];
                if (p) p.status = valores[0];
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
  return db;
}

const bucket = { async get() { return null; }, async put() {} };

async function llamar(env, method, path, body, user) {
  const headers = { "Content-Type": "application/json" };
  if (user) headers.Authorization = "Bearer " + await signToken({ sub: user.id, role: user.role }, SECRETO);
  const request = new Request("https://v.test" + path, {
    method, headers, body: body ? JSON.stringify(body) : undefined
  });
  return handleApi(request, env, new URL("https://v.test" + path), cors);
}

function escenario(overrides = {}) {
  const estado = {
    users: {
      u1: { id: "u1", role: "user", email: "u@x", name: "Usuaria" },
      e1: { id: "e1", role: "editor", email: "e@x", name: "Editor" }
    },
    songs: {
      s1: { id: "s1", owner_id: "u1", visibility: "private", deleted_at: 0, title: "Mi tema",
            artist: "Yo", genre: "folk", capo: 0, source_url: "", locked: 0, r2_key: "songs/s1.txt" },
      s2: { id: "s2", owner_id: "u9", visibility: "public", deleted_at: 0, title: "Publicada",
            artist: "Otra", genre: "", capo: 0, source_url: "", locked: 0, r2_key: "songs/s2.txt" }
    },
    proposals: {},
    ...overrides
  };
  const db = fakeDb(estado);
  return { estado, db, env: { DB: db, BUCKET: bucket, AUTH_SECRET: SECRETO } };
}

const propuestaPublicar = {
  id: "p1", kind: "publish", status: "pending", song_id: "s1", author_id: "u1",
  name: "", capo: 0, source_url: "", r2_key: "", note: "va que arde",
  review_note: "", created_at: 1, resolved_at: 0
};
const propuestaVersion = {
  id: "p2", kind: "version", status: "pending", song_id: "s2", author_id: "u1",
  name: "Acústica", capo: 2, source_url: "", r2_key: "songs/prop.txt", note: "",
  review_note: "", created_at: 1, resolved_at: 0
};

test("aprobar una propuesta de publicación pone la partitura en el catálogo", async () => {
  const { estado, db, env } = escenario();
  estado.proposals.p1 = { ...propuestaPublicar };
  const res = await llamar(env, "POST", "/api/proposals/p1/approve", {}, estado.users.e1);
  assert.equal(res.status, 200);
  const song = db.escrituras.find((e) => e.tipo === "song");
  assert.equal(song.visibility, "public");
  assert.equal(song.id, "s1");
  const prop = db.escrituras.find((e) => e.tipo === "proposal");
  assert.equal(prop.status, "approved");
  assert.equal(prop.reviewerId, "e1", "queda constancia de quién la aprobó");
});

test("aprobar una versión la crea a nombre de quien la propuso, no del revisor", async () => {
  const { estado, db, env } = escenario();
  estado.proposals.p2 = { ...propuestaVersion };
  const res = await llamar(env, "POST", "/api/proposals/p2/approve", {}, estado.users.e1);
  assert.equal(res.status, 200);
  const version = db.escrituras.find((e) => e.tipo === "version");
  assert.equal(version.songId, "s2");
  assert.equal(version.name, "Acústica");
  assert.equal(version.capo, 2);
  assert.equal(version.authorId, "u1", "el crédito es de quien la propuso");
  assert.equal(version.r2Key, "songs/prop.txt", "se reutiliza el texto ya subido");
  assert.equal(db.escrituras.some((e) => e.tipo === "song"), false, "la partitura no se toca");
});

test("un usuario normal no puede aprobar ni rechazar", async () => {
  const { estado, db, env } = escenario();
  estado.proposals.p1 = { ...propuestaPublicar };
  const aprobar = await llamar(env, "POST", "/api/proposals/p1/approve", {}, estado.users.u1);
  assert.equal(aprobar.status, 403);
  const rechazar = await llamar(env, "POST", "/api/proposals/p1/reject", { note: "no" }, estado.users.u1);
  assert.equal(rechazar.status, 403);
  assert.equal(db.escrituras.length, 0, "no se ha escrito nada");
});

test("una propuesta ya resuelta no se aprueba dos veces", async () => {
  const { estado, env } = escenario();
  estado.proposals.p1 = { ...propuestaPublicar, status: "approved" };
  const res = await llamar(env, "POST", "/api/proposals/p1/approve", {}, estado.users.e1);
  assert.equal(res.status, 409);
});

test("rechazar guarda el motivo y no cambia la partitura", async () => {
  const { estado, db, env } = escenario();
  estado.proposals.p1 = { ...propuestaPublicar };
  const res = await llamar(env, "POST", "/api/proposals/p1/reject", { note: "faltan acordes" }, estado.users.e1);
  assert.equal(res.status, 200);
  assert.equal(db.escrituras.some((e) => e.tipo === "song"), false);
  const prop = db.escrituras.find((e) => e.tipo === "proposal");
  assert.equal(prop.status, "rejected");
});

test("crear una partitura pidiendo que sea pública no la publica", async () => {
  const { estado, db, env } = escenario();
  const res = await llamar(env, "POST", "/api/songs",
    { title: "Nueva", content: "{C} hola", visibility: "public" }, estado.users.u1);
  assert.equal(res.status, 201);
  const alta = db.escrituras.find((e) => e.tipo === "alta");
  assert.equal(alta.title, "Nueva");
  assert.equal(alta.visibility, "private", "pedir public al crear no publica nada");
});

test("un editor sí puede publicar directamente al editar", async () => {
  const { estado, db, env } = escenario();
  const res = await llamar(env, "PUT", "/api/songs/s2", { visibility: "private" }, estado.users.e1);
  assert.equal(res.status, 200);
  const song = db.escrituras.find((e) => e.tipo === "song");
  assert.equal(song.visibility, "private", "el editor sí cambia la visibilidad");
});

test("el dueño no puede cambiar la visibilidad de su partitura", async () => {
  const { estado, db, env } = escenario();
  estado.songs.s3 = { ...estado.songs.s1, id: "s3", visibility: "private" };
  const res = await llamar(env, "PUT", "/api/songs/s3", { visibility: "public" }, estado.users.u1);
  assert.equal(res.status, 200, "la edición se acepta");
  const song = db.escrituras.find((e) => e.tipo === "song");
  assert.equal(song.visibility, "private", "pero la visibilidad no se mueve");
});

test("sin sesión no se proponen cambios", async () => {
  const { env } = escenario();
  const res = await llamar(env, "POST", "/api/songs/s2/proposals", { kind: "version", content: "{C}" });
  assert.equal(res.status, 401);
});

test("no se propone una versión de algo que no está publicado", async () => {
  const { estado, env } = escenario();
  const res = await llamar(env, "POST", "/api/songs/s1/proposals",
    { kind: "version", content: "{C} algo" }, estado.users.u1);
  assert.equal(res.status, 403);
});

test("una versión propuesta sin contenido se rechaza", async () => {
  const { estado, env } = escenario();
  const res = await llamar(env, "POST", "/api/songs/s2/proposals",
    { kind: "version", content: "   " }, estado.users.u1);
  assert.equal(res.status, 400);
});

test("nadie se cambia el rol a sí mismo", async () => {
  const { estado, env } = escenario();
  estado.users.a1 = { id: "a1", role: "admin", email: "a@x", name: "Admin" };
  const res = await llamar(env, "PUT", "/api/users/a1/role", { role: "user" }, estado.users.a1);
  assert.equal(res.status, 400);
});

test("un editor no reparte roles", async () => {
  const { estado, env } = escenario();
  const res = await llamar(env, "PUT", "/api/users/u1/role", { role: "admin" }, estado.users.e1);
  assert.equal(res.status, 403);
});

test("un rol inventado no cuela", async () => {
  const { estado, env } = escenario();
  estado.users.a1 = { id: "a1", role: "admin", email: "a@x", name: "Admin" };
  const res = await llamar(env, "PUT", "/api/users/u1/role", { role: "jefazo" }, estado.users.a1);
  assert.equal(res.status, 400);
});

test("las versiones de una partitura privada ajena no se listan", async () => {
  const { estado, env } = escenario();
  const res = await llamar(env, "GET", "/api/songs/s1/versions", null, estado.users.e1);
  assert.equal(res.status, 404, "para un editor, una privada ajena no existe");
});

test("quien no puede editar no añade versiones: propone", async () => {
  const { estado, env } = escenario();
  const res = await llamar(env, "POST", "/api/songs/s2/versions",
    { name: "Mía", content: "{C}" }, estado.users.u1);
  assert.equal(res.status, 403);
});
