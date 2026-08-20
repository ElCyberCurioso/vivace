import { strict as assert } from "node:assert";
import test from "node:test";
import { migrateExistingSongs, MIGRATE_BATCH } from "../src/migrate.js";

/**
 * Bucket de mentira: guarda objetos en un Map y pagina como R2, respetando
 * `limit` y devolviendo cursor cuando queda cola.
 */
function fakeBucket(objetos) {
  const claves = Object.keys(objetos).sort();
  return {
    lecturas: 0,
    async list({ cursor, limit }) {
      const desde = cursor ? claves.indexOf(cursor) + 1 : 0;
      const trozo = claves.slice(desde, desde + limit);
      const ultimo = trozo[trozo.length - 1];
      return {
        objects: trozo.map((key) => ({ key, uploaded: new Date(1000), customMetadata: {} })),
        truncated: desde + trozo.length < claves.length,
        cursor: ultimo
      };
    },
    async get(key) {
      this.lecturas++;
      return { text: async () => objetos[key] };
    }
  };
}

/** BD de mentira: recuerda lo insertado y cuenta los lotes. */
function fakeDb(clavesConocidas = []) {
  return {
    insertadas: [],
    lotes: 0,
    consultas: 0,
    prepare(sql) {
      const db = this;
      return {
        _sql: sql,
        bind(...valores) { return { _sql: sql, _valores: valores }; },
        async all() {
          db.consultas++;
          return { results: clavesConocidas.map((r2_key) => ({ r2_key })) };
        }
      };
    },
    async batch(sentencias) {
      this.lotes++;
      for (const s of sentencias) this.insertadas.push(s._valores);
      return [];
    }
  };
}

const cancion = (titulo) => `#title: ${titulo}\n#artist: Autor\n---\n{Am} letra\n`;

test("indexa una tanda y devuelve cursor mientras quede cola", async () => {
  const objetos = {};
  for (let i = 0; i < 5; i++) objetos[`songs/${i}.txt`] = cancion("Cancion " + i);
  const env = { BUCKET: fakeBucket(objetos), DB: fakeDb() };

  const primera = await migrateExistingSongs(env, "admin-1", "public", { limit: 2 });
  assert.equal(primera.imported, 2);
  assert.equal(primera.done, false);
  assert.ok(primera.cursor);

  const segunda = await migrateExistingSongs(env, "admin-1", "public", { limit: 2, cursor: primera.cursor });
  assert.equal(segunda.imported, 2);
  assert.equal(segunda.done, false);

  const tercera = await migrateExistingSongs(env, "admin-1", "public", { limit: 2, cursor: segunda.cursor });
  assert.equal(tercera.imported, 1);
  assert.equal(tercera.done, true);
  assert.equal(tercera.cursor, null);
});

test("lo ya indexado se salta sin leerlo de R2", async () => {
  const objetos = { "songs/a.txt": cancion("A"), "songs/b.txt": cancion("B") };
  const env = { BUCKET: fakeBucket(objetos), DB: fakeDb(["songs/a.txt"]) };

  const resultado = await migrateExistingSongs(env, "admin-1", "private", { limit: 10 });
  assert.equal(resultado.imported, 1);
  assert.equal(resultado.skipped, 1);
  assert.equal(resultado.done, true);
  // La clave conocida no se descarga: es lo que abarata repetir la migracion.
  assert.equal(env.BUCKET.lecturas, 1);
  // Y la lista de claves se pide una sola vez, no una por cancion.
  assert.equal(env.DB.consultas, 1);
});

test("todas las altas de una tanda viajan en un solo lote", async () => {
  const objetos = {};
  for (let i = 0; i < 8; i++) objetos[`songs/${i}.txt`] = cancion("C" + i);
  const env = { BUCKET: fakeBucket(objetos), DB: fakeDb() };

  await migrateExistingSongs(env, "admin-1", "public", { limit: 8 });
  assert.equal(env.DB.lotes, 1);
  assert.equal(env.DB.insertadas.length, 8);
});

test("una tanda vacia no abre lote ni rompe", async () => {
  const env = { BUCKET: fakeBucket({}), DB: fakeDb() };
  const resultado = await migrateExistingSongs(env, "admin-1", "public", {});
  assert.deepEqual(
    { imported: resultado.imported, skipped: resultado.skipped, done: resultado.done },
    { imported: 0, skipped: 0, done: true }
  );
  assert.equal(env.DB.lotes, 0);
});

test("el tamano de tanda se acota: ni cero ni desmedido", async () => {
  const objetos = {};
  for (let i = 0; i < 300; i++) objetos[`songs/${String(i).padStart(3, "0")}.txt`] = cancion("C" + i);

  const env = { BUCKET: fakeBucket(objetos), DB: fakeDb() };
  const porDefecto = await migrateExistingSongs(env, "admin-1", "public", { limit: 0 });
  assert.equal(porDefecto.imported, MIGRATE_BATCH);

  const otro = { BUCKET: fakeBucket(objetos), DB: fakeDb() };
  const tope = await migrateExistingSongs(otro, "admin-1", "public", { limit: 9999 });
  assert.equal(tope.imported, 200);
});

test("la cabecera del fichero manda sobre los metadatos de R2", async () => {
  const env = {
    BUCKET: fakeBucket({ "songs/x.txt": "#title: Real\n#artist: Autora\n#capo: 3\n---\n{C} ahi\n" }),
    DB: fakeDb()
  };
  await migrateExistingSongs(env, "admin-9", "public", {});
  const [fila] = env.DB.insertadas;
  // id, owner_id, r2_key, title, artist, genre, capo, ...
  assert.equal(fila[1], "admin-9");
  assert.equal(fila[2], "songs/x.txt");
  assert.equal(fila[3], "Real");
  assert.equal(fila[4], "Autora");
  assert.equal(fila[6], 3);
});
