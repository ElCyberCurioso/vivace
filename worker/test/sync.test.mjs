import { strict as assert } from "node:assert";
import test from "node:test";
import { handleApi } from "../src/api.js";
import { signToken } from "../src/auth.js";
import { buildCursor, parseCursor } from "../src/sync.js";
import { fakeBucket, fakeD1 } from "./fake-d1.mjs";

const cors = {};
const SECRETO = "clave-de-prueba";
const ANA = { id: "u-ana", email: "ana@v.test", email_lower: "ana@v.test", name: "Ana", role: "user", password_hash: "x", created_at: 1 };
const BEA = { id: "u-bea", email: "bea@v.test", email_lower: "bea@v.test", name: "Bea", role: "user", password_hash: "x", created_at: 2 };

async function tokenDe(user) {
  return signToken({ sub: user.id, role: user.role }, SECRETO);
}

function entorno(estado = {}, objetos = {}) {
  return {
    DB: fakeD1({ users: [{ ...ANA }, { ...BEA }], ...estado }),
    BUCKET: fakeBucket(objetos),
    AUTH_SECRET: SECRETO
  };
}

async function llamar(env, method, path, { body, token } = {}) {
  const url = new URL("https://v.test" + path);
  const req = new Request(url, {
    method,
    headers: {
      ...(token ? { Authorization: "Bearer " + token } : {}),
      ...(body ? { "Content-Type": "application/json" } : {})
    },
    body: body ? JSON.stringify(body) : undefined
  });
  return handleApi(req, env, url, cors);
}

/* ------------------------------ cursores ------------------------------ */

test("el cursor se parte en marca de tiempo e id", () => {
  assert.deepEqual(parseCursor("1700:abc"), { since: 1700, id: "abc" });
  assert.deepEqual(parseCursor(""), { since: 0, id: "" }, "vacío = desde el principio");
  assert.deepEqual(parseCursor("basura"), { since: 0, id: "" });
  // Un id con dos puntos dentro no debe romper el corte.
  assert.deepEqual(parseCursor("5:a:b"), { since: 5, id: "a:b" });
});

test("una tanda vacía conserva el cursor anterior", () => {
  assert.equal(buildCursor([], "9:z"), "9:z");
  assert.equal(buildCursor([{ updated_at: 12, id: "q" }], "9:z"), "12:q");
});

/* --------------------------- feed de cambios --------------------------- */

test("el feed baja el texto incrustado y solo lo del dueño", async () => {
  const env = entorno({
    songs: [
      { id: "s1", owner_id: ANA.id, r2_key: "songs/s1.txt", title: "Mía", artist: "", genre: "",
        capo: 0, source_url: "", locked: 0, visibility: "private", favorite: 0, position: 0,
        playlist_id: null, rev: 1, created_at: 10, updated_at: 10, deleted_at: 0, youtube_url: "" },
      { id: "s2", owner_id: BEA.id, r2_key: "songs/s2.txt", title: "Ajena", artist: "", genre: "",
        capo: 0, source_url: "", locked: 0, visibility: "private", favorite: 0, position: 0,
        playlist_id: null, rev: 1, created_at: 10, updated_at: 10, deleted_at: 0, youtube_url: "" }
    ]
  }, { "songs/s1.txt": "#title: Mía\n---\n{Am}letra", "songs/s2.txt": "de Bea" });

  const res = await llamar(env, "GET", "/api/sync/changes", { token: await tokenDe(ANA) });
  assert.equal(res.status, 200);
  const datos = await res.json();
  assert.equal(datos.songs.items.length, 1, "solo las suyas");
  assert.equal(datos.songs.items[0].content, "#title: Mía\n---\n{Am}letra");
  assert.equal(datos.songs.cursor, "10:s1");
  assert.equal(datos.more, false);
});

test("el feed manda lápidas, y sin gastar una lectura en su cuerpo", async () => {
  const env = entorno({
    songs: [
      { id: "s1", owner_id: ANA.id, r2_key: "songs/s1.txt", title: "Borrada", artist: "", genre: "",
        capo: 0, source_url: "", locked: 0, visibility: "private", favorite: 0, position: 0,
        playlist_id: null, rev: 3, created_at: 10, updated_at: 50, deleted_at: 40, youtube_url: "" }
    ]
  }, { "songs/s1.txt": "texto que ya no importa" });

  const datos = await (await llamar(env, "GET", "/api/sync/changes", { token: await tokenDe(ANA) })).json();
  assert.equal(datos.songs.items.length, 1);
  assert.equal(datos.songs.items[0].deletedAt, 40, "la lápida viaja");
  assert.equal(datos.songs.items[0].content, "", "no se baja el cuerpo de lo borrado");
});

test("el cursor avanza y no repite lo ya visto", async () => {
  const songs = [];
  for (let i = 1; i <= 3; i++) {
    songs.push({ id: "s" + i, owner_id: ANA.id, r2_key: "songs/s" + i + ".txt", title: "T" + i,
      artist: "", genre: "", capo: 0, source_url: "", locked: 0, visibility: "private",
      favorite: 0, position: 0, playlist_id: null, rev: 1,
      created_at: 10 * i, updated_at: 10 * i, deleted_at: 0, youtube_url: "" });
  }
  const env = entorno({ songs }, {});
  const token = await tokenDe(ANA);

  const p1 = await (await llamar(env, "GET", "/api/sync/changes?limit=2", { token })).json();
  assert.deepEqual(p1.songs.items.map((s) => s.id), ["s1", "s2"]);
  assert.equal(p1.songs.more, true);

  const p2 = await (await llamar(env, "GET",
    "/api/sync/changes?limit=2&songs=" + encodeURIComponent(p1.songs.cursor), { token })).json();
  assert.deepEqual(p2.songs.items.map((s) => s.id), ["s3"]);
  assert.equal(p2.songs.more, false);
});

/* ---------------------------------- push ---------------------------------- */

test("el push crea la partitura, guarda el texto y devuelve el id nuevo", async () => {
  const env = entorno();
  const res = await llamar(env, "POST", "/api/sync/push", {
    token: await tokenDe(ANA),
    body: { songs: [{ clientId: "local-7", title: "Nueva", content: "{C}hola" }] }
  });
  assert.equal(res.status, 200);
  const datos = await res.json();
  const fila = datos.songs[0];
  assert.equal(fila.ok, true);
  assert.equal(fila.clientId, "local-7");
  assert.equal(fila.rev, 1);
  const guardada = env.DB.tablas.songs[0];
  assert.equal(guardada.title, "Nueva");
  assert.equal(env.BUCKET.objetos.get(guardada.r2_key), "{C}hola");
});

test("una partitura nueva no se publica sola por mucho que la app lo pida", async () => {
  const env = entorno();
  await llamar(env, "POST", "/api/sync/push", {
    token: await tokenDe(ANA),
    body: { songs: [{ clientId: "c1", title: "T", content: "x", visibility: "public" }] }
  });
  assert.equal(env.DB.tablas.songs[0].visibility, "private",
    "publicar es un acto editorial, no una casilla del móvil");
});

test("editar con un rev viejo devuelve conflicto y NO pisa el servidor", async () => {
  const env = entorno({
    songs: [{ id: "s1", owner_id: ANA.id, r2_key: "songs/s1.txt", title: "Servidor", artist: "",
      genre: "", capo: 0, source_url: "", locked: 0, visibility: "private", favorite: 0,
      position: 0, playlist_id: null, rev: 5, created_at: 1, updated_at: 100, deleted_at: 0,
      youtube_url: "" }]
  }, { "songs/s1.txt": "texto del servidor" });

  const datos = await (await llamar(env, "POST", "/api/sync/push", {
    token: await tokenDe(ANA),
    body: { songs: [{ clientId: "c1", id: "s1", baseRev: 3, title: "Móvil", content: "texto del móvil" }] }
  })).json();

  const fila = datos.songs[0];
  assert.equal(fila.ok, false);
  assert.equal(fila.conflict, true);
  assert.equal(fila.server.title, "Servidor");
  assert.equal(fila.server.content, "texto del servidor", "el cliente recibe con qué comparar");
  assert.equal(env.DB.tablas.songs[0].title, "Servidor", "no se ha sobrescrito");
  assert.equal(env.BUCKET.objetos.get("songs/s1.txt"), "texto del servidor");
});

test("con el rev correcto la edición entra y el rev sube", async () => {
  const env = entorno({
    songs: [{ id: "s1", owner_id: ANA.id, r2_key: "songs/s1.txt", title: "Antes", artist: "",
      genre: "", capo: 0, source_url: "", locked: 0, visibility: "private", favorite: 0,
      position: 0, playlist_id: null, rev: 5, created_at: 1, updated_at: 100, deleted_at: 0,
      youtube_url: "" }]
  }, { "songs/s1.txt": "antes" });

  const datos = await (await llamar(env, "POST", "/api/sync/push", {
    token: await tokenDe(ANA),
    body: { songs: [{ id: "s1", baseRev: 5, title: "Después", content: "después" }] }
  })).json();
  assert.equal(datos.songs[0].ok, true);
  assert.equal(datos.songs[0].rev, 6);
  assert.equal(env.DB.tablas.songs[0].title, "Después");
});

test("el borrado del móvil llega al servidor como papelera, no como olvido", async () => {
  const env = entorno({
    songs: [{ id: "s1", owner_id: ANA.id, r2_key: "songs/s1.txt", title: "Adiós", artist: "",
      genre: "", capo: 0, source_url: "", locked: 0, visibility: "private", favorite: 0,
      position: 0, playlist_id: null, rev: 1, created_at: 1, updated_at: 1, deleted_at: 0,
      youtube_url: "" }]
  }, { "songs/s1.txt": "adiós" });

  const datos = await (await llamar(env, "POST", "/api/sync/push", {
    token: await tokenDe(ANA),
    body: { songs: [{ id: "s1", baseRev: 1, deleted: true }] }
  })).json();
  assert.equal(datos.songs[0].ok, true);
  assert.ok(env.DB.tablas.songs[0].deleted_at > 0, "queda en la papelera");
  assert.ok(env.BUCKET.objetos.has("songs/s1.txt"), "el texto se conserva: es recuperable");
});

test("el borrado definitivo se lleva la fila y el objeto", async () => {
  const env = entorno({
    songs: [{ id: "s1", owner_id: ANA.id, r2_key: "songs/s1.txt", title: "Fin", artist: "",
      genre: "", capo: 0, source_url: "", locked: 0, visibility: "private", favorite: 0,
      position: 0, playlist_id: null, rev: 2, created_at: 1, updated_at: 9, deleted_at: 5,
      youtube_url: "" }]
  }, { "songs/s1.txt": "fin" });

  await llamar(env, "POST", "/api/sync/push", {
    token: await tokenDe(ANA),
    body: { songs: [{ id: "s1", purge: true }] }
  });
  assert.equal(env.DB.tablas.songs.length, 0);
  assert.equal(env.BUCKET.objetos.has("songs/s1.txt"), false);
});

test("no se puede tocar la partitura de otra persona", async () => {
  const env = entorno({
    songs: [{ id: "s1", owner_id: BEA.id, r2_key: "songs/s1.txt", title: "De Bea", artist: "",
      genre: "", capo: 0, source_url: "", locked: 0, visibility: "private", favorite: 0,
      position: 0, playlist_id: null, rev: 1, created_at: 1, updated_at: 1, deleted_at: 0,
      youtube_url: "" }]
  }, { "songs/s1.txt": "de Bea" });

  const datos = await (await llamar(env, "POST", "/api/sync/push", {
    token: await tokenDe(ANA),
    body: { songs: [{ id: "s1", baseRev: 1, title: "Robada", content: "mío ahora" }] }
  })).json();
  assert.equal(datos.songs[0].ok, false);
  assert.equal(env.DB.tablas.songs[0].title, "De Bea");
  assert.equal(env.BUCKET.objetos.get("songs/s1.txt"), "de Bea");
});

test("una carpeta nueva y su partitura suben en el mismo lote", async () => {
  const env = entorno();
  const datos = await (await llamar(env, "POST", "/api/sync/push", {
    token: await tokenDe(ANA),
    body: {
      playlists: [{ clientId: "pl-1", name: "Conciertos" }],
      songs: [{ clientId: "s-1", title: "Con carpeta", content: "x", playlistClientId: "pl-1" }]
    }
  })).json();
  const idLista = datos.playlists[0].id;
  assert.ok(idLista);
  assert.equal(env.DB.tablas.songs[0].playlist_id, idLista,
    "la partitura apunta a la carpeta recién creada en el mismo envío");
});

test("borrar una carpeta no borra sus partituras", async () => {
  const env = entorno({
    playlists: [{ id: "p1", owner_id: ANA.id, name: "Vieja", position: 0, created_at: 1, updated_at: 1, deleted_at: 0 }],
    songs: [{ id: "s1", owner_id: ANA.id, r2_key: "songs/s1.txt", title: "Dentro", artist: "",
      genre: "", capo: 0, source_url: "", locked: 0, visibility: "private", favorite: 0,
      position: 0, playlist_id: "p1", rev: 1, created_at: 1, updated_at: 1, deleted_at: 0,
      youtube_url: "" }]
  });
  await llamar(env, "DELETE", "/api/playlists/p1", { token: await tokenDe(ANA) });
  assert.ok(env.DB.tablas.playlists[0].deleted_at > 0);
  assert.equal(env.DB.tablas.songs[0].deleted_at, 0, "la partitura sigue viva");
  assert.equal(env.DB.tablas.songs[0].playlist_id, null, "y pasa a Sin lista");
});

test("un lote enorme se rechaza entero en vez de aceptarlo a medias", async () => {
  const env = entorno();
  const muchas = Array.from({ length: 40 }, (_, i) => ({ clientId: "c" + i, title: "T" + i, content: "x" }));
  const res = await llamar(env, "POST", "/api/sync/push", {
    token: await tokenDe(ANA), body: { songs: muchas }
  });
  assert.equal(res.status, 413);
  assert.equal(env.DB.tablas.songs.length, 0);
});

test("sin sesión no hay sincronización", async () => {
  const env = entorno();
  assert.equal((await llamar(env, "GET", "/api/sync/changes")).status, 401);
  assert.equal((await llamar(env, "POST", "/api/sync/push", { body: { songs: [] } })).status, 401);
});
