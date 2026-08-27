import { strict as assert } from "node:assert";
import test from "node:test";
import worker from "../src/index.js";
import { fakeBucket, fakeD1 } from "./fake-d1.mjs";

/*
 * Recorrido completo por la puerta de entrada real del Worker (src/index.js):
 * registro, edición desde la web, sincronización desde el móvil, conflicto,
 * borrado y lápida. Es la prueba de que las dos plataformas trabajan sobre lo
 * mismo, que era justo lo que no estaba cubierto.
 */

function entorno() {
  return {
    DB: fakeD1(),
    BUCKET: fakeBucket(),
    AUTH_SECRET: "secreto-de-prueba"
  };
}

async function pedir(env, method, path, { body, token } = {}) {
  const req = new Request("https://v.test" + path, {
    method,
    headers: {
      ...(token ? { Authorization: "Bearer " + token } : {}),
      ...(body !== undefined ? { "Content-Type": "application/json" } : {})
    },
    body: body !== undefined ? JSON.stringify(body) : undefined
  });
  const res = await worker.fetch(req, env);
  const texto = await res.text();
  let datos = {};
  try { datos = texto ? JSON.parse(texto) : {}; } catch (e) { datos = { raw: texto }; }
  return { status: res.status, datos, headers: res.headers };
}

async function registrar(env, email) {
  const r = await pedir(env, "POST", "/auth/register", {
    body: { email, password: "contrasena-larga", name: email.split("@")[0] }
  });
  assert.equal(r.status, 201, JSON.stringify(r.datos));
  return r.datos.token;
}

test("el primer registrado es admin y el segundo no", async () => {
  const env = entorno();
  await registrar(env, "ana@v.test");
  await registrar(env, "bea@v.test");
  assert.equal(env.DB.tablas.users[0].role, "admin");
  assert.equal(env.DB.tablas.users[1].role, "user");
});

test("crear en la web y bajarlo en el móvil, con carpeta incluida", async () => {
  const env = entorno();
  const token = await registrar(env, "ana@v.test");

  const lista = await pedir(env, "POST", "/api/playlists", { token, body: { name: "Conciertos" } });
  assert.equal(lista.status, 201);

  const alta = await pedir(env, "POST", "/api/songs", {
    token,
    body: { title: "Wonderwall", artist: "Oasis", content: "{Em7}Today", playlistId: lista.datos.playlist.id }
  });
  assert.equal(alta.status, 201, JSON.stringify(alta.datos));

  // Lo que vería la app en su primera sincronización.
  const cambios = await pedir(env, "GET", "/api/sync/changes", { token });
  assert.equal(cambios.status, 200);
  assert.equal(cambios.datos.playlists.items.length, 1);
  assert.equal(cambios.datos.songs.items.length, 1);
  const bajada = cambios.datos.songs.items[0];
  assert.equal(bajada.title, "Wonderwall");
  assert.equal(bajada.content, "{Em7}Today", "el texto viaja en el mismo viaje");
  assert.equal(bajada.playlistId, lista.datos.playlist.id, "la carpeta ya es un dato, no una cabecera");
});

test("una partitura de otra persona no aparece ni se puede tocar", async () => {
  const env = entorno();
  const ana = await registrar(env, "ana@v.test");
  const bea = await registrar(env, "bea@v.test");
  const alta = await pedir(env, "POST", "/api/songs", {
    token: ana, body: { title: "Privada de Ana", content: "secreto" }
  });
  const id = alta.datos.song.id;

  const deBea = await pedir(env, "GET", "/api/sync/changes", { token: bea });
  assert.equal(deBea.datos.songs.items.length, 0);
  // 404 y no 403: no se filtra ni que exista.
  assert.equal((await pedir(env, "GET", "/api/songs/" + id, { token: bea })).status, 404);
  const intento = await pedir(env, "POST", "/api/sync/push", {
    token: bea, body: { songs: [{ clientId: "c1", id, baseRev: 1, title: "Robada", content: "mío" }] }
  });
  assert.equal(intento.datos.songs[0].ok, false);
  assert.equal(env.DB.tablas.songs[0].title, "Privada de Ana");
});

test("editar desde el móvil sin conexión y volcarlo después", async () => {
  const env = entorno();
  const token = await registrar(env, "ana@v.test");

  // El móvil crea la partitura estando fuera de línea; al recuperar la red,
  // sube el lote entero de una vez.
  const subida = await pedir(env, "POST", "/api/sync/push", {
    token,
    body: {
      playlists: [{ clientId: "pl-1", name: "Ensayo" }],
      songs: [{ clientId: "song-1", title: "Offline", content: "{C}algo", playlistClientId: "pl-1" }]
    }
  });
  assert.equal(subida.status, 200);
  assert.equal(subida.datos.songs[0].ok, true);
  const idServidor = subida.datos.songs[0].id;

  // Y ahora la web lo ve, con su carpeta.
  const mias = await pedir(env, "GET", "/api/songs", { token });
  assert.equal(mias.datos.songs.length, 1);
  assert.equal(mias.datos.songs[0].id, idServidor);
  assert.ok(mias.datos.songs[0].playlistId, "la carpeta creada en el móvil existe en la web");
});

test("dos ediciones a la vez: el servidor avisa en vez de tragarse una", async () => {
  const env = entorno();
  const token = await registrar(env, "ana@v.test");
  const alta = await pedir(env, "POST", "/api/songs", {
    token, body: { title: "Compartida", content: "versión 1" }
  });
  const id = alta.datos.song.id;
  const rev1 = alta.datos.song.rev;

  // Alguien la edita desde la web…
  await pedir(env, "PUT", "/api/songs/" + id, {
    token, body: { title: "Compartida", content: "versión de la web" }
  });

  // …y el móvil sube la suya con el rev viejo.
  const choque = await pedir(env, "POST", "/api/sync/push", {
    token, body: { songs: [{ clientId: "c1", id, baseRev: rev1, title: "Compartida", content: "versión del móvil" }] }
  });
  const fila = choque.datos.songs[0];
  assert.equal(fila.conflict, true);
  assert.equal(fila.server.content, "versión de la web");
  // Nada se ha perdido en el servidor; el móvil guardará la suya como versión.
  const actual = await pedir(env, "GET", "/api/songs/" + id, { token });
  assert.equal(actual.datos.content, "versión de la web");
});

test("borrar en el móvil llega a la web, y la lápida al resto de aparatos", async () => {
  const env = entorno();
  const token = await registrar(env, "ana@v.test");
  const alta = await pedir(env, "POST", "/api/songs", {
    token, body: { title: "Efímera", content: "x" }
  });
  const id = alta.datos.song.id;

  // Otro dispositivo ya la tenía: se queda con este cursor.
  const primera = await pedir(env, "GET", "/api/sync/changes", { token });
  const cursor = primera.datos.songs.cursor;

  await pedir(env, "POST", "/api/sync/push", {
    token, body: { songs: [{ clientId: "c1", id, baseRev: alta.datos.song.rev, deleted: true }] }
  });

  // La web ya no la lista, pero está en la papelera.
  assert.equal((await pedir(env, "GET", "/api/songs", { token })).datos.songs.length, 0);
  assert.equal((await pedir(env, "GET", "/api/songs?trash=1", { token })).datos.songs.length, 1);

  // Y el otro dispositivo se entera, que es lo que antes no pasaba.
  const despues = await pedir(env, "GET",
    "/api/sync/changes?songs=" + encodeURIComponent(cursor), { token });
  assert.equal(despues.datos.songs.items.length, 1);
  assert.ok(despues.datos.songs.items[0].deletedAt > 0);
});

test("restaurar y borrar del todo desde la web", async () => {
  const env = entorno();
  const token = await registrar(env, "ana@v.test");
  const alta = await pedir(env, "POST", "/api/songs", { token, body: { title: "Ida y vuelta", content: "x" } });
  const id = alta.datos.song.id;
  const clave = env.DB.tablas.songs[0].r2_key;

  await pedir(env, "DELETE", "/api/songs/" + id, { token });
  // No se puede borrar del todo algo que no está en la papelera.
  await pedir(env, "POST", "/api/songs/" + id + "/restore", { token });
  assert.equal((await pedir(env, "DELETE", "/api/songs/" + id + "?hard=1", { token })).status, 409);

  await pedir(env, "DELETE", "/api/songs/" + id, { token });
  assert.equal((await pedir(env, "DELETE", "/api/songs/" + id + "?hard=1", { token })).status, 200);
  assert.equal(env.DB.tablas.songs.length, 0);
  assert.equal(env.BUCKET.objetos.has(clave), false, "el objeto de R2 también se va");
});

test("marcar favorita no toca el resto de la ficha", async () => {
  const env = entorno();
  const token = await registrar(env, "ana@v.test");
  const alta = await pedir(env, "POST", "/api/songs", {
    token, body: { title: "Con estrella", artist: "Alguien", content: "cuerpo" }
  });
  const id = alta.datos.song.id;
  const r = await pedir(env, "PUT", "/api/songs/" + id + "/favorite", { token, body: { favorite: true } });
  assert.equal(r.datos.song.favorite, true);
  assert.equal(r.datos.song.title, "Con estrella");
  const det = await pedir(env, "GET", "/api/songs/" + id, { token });
  assert.equal(det.datos.content, "cuerpo", "el texto sigue intacto");
});

test("editar el texto desde la web no borra el favorito", async () => {
  const env = entorno();
  const token = await registrar(env, "ana@v.test");
  const alta = await pedir(env, "POST", "/api/songs", { token, body: { title: "T", content: "a" } });
  const id = alta.datos.song.id;
  await pedir(env, "PUT", "/api/songs/" + id + "/favorite", { token, body: { favorite: true } });
  await pedir(env, "PUT", "/api/songs/" + id, { token, body: { title: "T", content: "b" } });
  assert.equal(env.DB.tablas.songs[0].favorite, 1, "el favorito no viaja en esta ruta y no debe perderse");
});

test("las rutas del token compartido ya no existen", async () => {
  const env = entorno();
  env.SYNC_TOKEN = "token-heredado";
  for (const [method, path] of [["GET", "/list"], ["GET", "/bodies"],
                                ["GET", "/object?key=songs/x.txt"], ["POST", "/delete"]]) {
    const r = await pedir(env, method, path, { token: "token-heredado" });
    assert.equal(r.status, 404, path + " debería haber desaparecido");
  }
});

test("sin AUTH_SECRET no se emite ni se acepta ninguna sesión", async () => {
  const env = entorno();
  delete env.AUTH_SECRET;
  // Antes caía a SYNC_TOKEN, que iba en todas las apps antiguas.
  env.SYNC_TOKEN = "token-heredado";
  const r = await pedir(env, "POST", "/auth/login", {
    body: { email: "ana@v.test", password: "contrasena-larga" }
  });
  assert.equal(r.status, 503);
  assert.ok(String(r.datos.error).includes("AUTH_SECRET"));
});

test("el catálogo público se puede incrustar; lo privado no abre CORS", async () => {
  const env = entorno();
  const publico = await pedir(env, "GET", "/api/songs/public");
  assert.equal(publico.headers.get("Access-Control-Allow-Origin"), "*");
  const privado = await pedir(env, "GET", "/api/songs");
  assert.equal(privado.headers.get("Access-Control-Allow-Origin"), null);
});

test("el listado propio viene por páginas y avisa de que hay más", async () => {
  const env = entorno();
  const token = await registrar(env, "ana@v.test");
  for (let i = 0; i < 5; i++) {
    await pedir(env, "POST", "/api/songs", {
      token, body: { title: "Canción " + i, content: "x" }
    });
  }
  const p1 = await pedir(env, "GET", "/api/songs?limit=2&offset=0", { token });
  assert.equal(p1.datos.songs.length, 2);
  assert.equal(p1.datos.hasMore, true);

  const p3 = await pedir(env, "GET", "/api/songs?limit=2&offset=4", { token });
  assert.equal(p3.datos.songs.length, 1);
  assert.equal(p3.datos.hasMore, false, "la última página no promete más");

  // Sin límite se aplica el de por defecto, no "todo".
  const todo = await pedir(env, "GET", "/api/songs", { token });
  assert.equal(todo.datos.songs.length, 5);
  assert.equal(todo.datos.hasMore, false);
});

test("publicar una propuesta no se lleva por delante el favorito ni la carpeta", async () => {
  const env = entorno();
  const token = await registrar(env, "ana@v.test");   // el primero es admin
  const lista = await pedir(env, "POST", "/api/playlists", { token, body: { name: "Ensayo" } });
  const alta = await pedir(env, "POST", "/api/songs", {
    token, body: { title: "Para publicar", content: "x", playlistId: lista.datos.playlist.id }
  });
  const id = alta.datos.song.id;
  await pedir(env, "PUT", "/api/songs/" + id + "/favorite", { token, body: { favorite: true } });

  const prop = await pedir(env, "POST", "/api/songs/" + id + "/proposals", {
    token, body: { kind: "publish", note: "va" }
  });
  assert.equal(prop.status, 201, JSON.stringify(prop.datos));
  const ok = await pedir(env, "POST", "/api/proposals/" + prop.datos.proposal.id + "/approve", {
    token, body: {}
  });
  assert.equal(ok.status, 200, JSON.stringify(ok.datos));

  const fila = env.DB.tablas.songs[0];
  assert.equal(fila.visibility, "public");
  assert.equal(fila.favorite, 1, "el favorito sobrevive a la publicación");
  assert.equal(fila.playlist_id, lista.datos.playlist.id, "y la carpeta también");
});

test("indexar el catálogo dos veces no duplica nada, y rescata carpeta y favorito", async () => {
  const env = entorno();
  // Partituras de antes del multiusuario: texto suelto en R2 con la carpeta y
  // el favorito escondidos en las cabeceras.
  env.BUCKET.objetos.set("songs/a.txt", "#title: Asturias\n#playlist: Clásico\n#favorite: true\n---\n{Am}x");
  env.BUCKET.objetos.set("songs/b.txt", "#title: Suelta\n---\n{C}y");
  const token = await registrar(env, "admin@v.test");   // el primero es admin

  const uno = await pedir(env, "POST", "/admin/migrate?visibility=public&backfill=1", { token });
  assert.equal(uno.status, 200, JSON.stringify(uno.datos));
  assert.equal(uno.datos.imported, 2);
  assert.equal(uno.datos.done, true);

  const dos = await pedir(env, "POST", "/admin/migrate?visibility=public&backfill=1", { token });
  assert.equal(dos.datos.imported, 0, "la segunda pasada no importa nada");
  assert.equal(dos.datos.skipped, 2);
  assert.equal(env.DB.tablas.songs.length, 2, "y no ha duplicado filas");

  const asturias = env.DB.tablas.songs.find((s) => s.title === "Asturias");
  assert.equal(asturias.favorite, 1, "el favorito se rescata de la cabecera");
  const lista = env.DB.tablas.playlists.find((p) => p.id === asturias.playlist_id);
  assert.equal(lista.name, "Clásico", "y la carpeta se crea y se enlaza");
  const suelta = env.DB.tablas.songs.find((s) => s.title === "Suelta");
  assert.equal(suelta.playlist_id, null, "lo que no tenía carpeta se queda sin ella");
});
