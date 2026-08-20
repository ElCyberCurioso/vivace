import { strict as assert } from "node:assert";
import test from "node:test";
import {
  ChordError, emptyDictionary, mergeSeed, normalizeChordName,
  readGlobalChords, sanitizeDictionary, writeGlobalChords
} from "../src/chords.js";
import { CHORD_SEED } from "../src/chords-seed.js";
import { handleApi } from "../src/api.js";

const cors = {};
const am = { frets: [-1, 0, 2, 2, 1, 0], fingers: [0, 0, 2, 3, 1, 0], baseFret: 1, barres: [] };

test("un acorde válido sobrevive entero", () => {
  const limpio = sanitizeDictionary({ chords: { Am: { positions: [am] } } });
  assert.deepEqual(limpio.Am.positions[0], am);
});

test("acepta tanto {positions:[...]} como la lista pelada", () => {
  const conObjeto = sanitizeDictionary({ Am: { positions: [am] } });
  const conLista = sanitizeDictionary({ Am: [am] });
  assert.deepEqual(conObjeto, conLista);
});

test("los campos que sobran no se guardan", () => {
  const limpio = sanitizeDictionary({ Am: [{ ...am, midi: [1, 2, 3], jaja: "x" }] });
  assert.deepEqual(Object.keys(limpio.Am.positions[0]).sort(), ["barres", "baseFret", "fingers", "frets"]);
});

test("los trastes se validan: seis, y dentro de rango", () => {
  assert.throws(() => sanitizeDictionary({ Am: [{ frets: [0, 0, 0] }] }), ChordError);
  assert.throws(() => sanitizeDictionary({ Am: [{ frets: [0, 0, 0, 0, 0, 99] }] }), ChordError);
  assert.throws(() => sanitizeDictionary({ Am: [{ frets: [0, 0, 0, 0, 0, -2] }] }), ChordError);
  assert.throws(() => sanitizeDictionary({ Am: [{ frets: [-1, -1, -1, -1, -1, -1] }] }), ChordError);
});

test("el nombre no puede llevar llaves ni espacios: rompe el parser de partituras", () => {
  assert.equal(normalizeChordName("  Am7  "), "Am7");
  assert.throws(() => normalizeChordName("A m"), ChordError);
  assert.throws(() => normalizeChordName("{Am}"), ChordError);
  assert.throws(() => normalizeChordName(""), ChordError);
});

test("sin dedos indicados se rellenan a cero, no se inventa nada", () => {
  const limpio = sanitizeDictionary({ C: [{ frets: [-1, 3, 2, 0, 1, 0] }] });
  assert.deepEqual(limpio.C.positions[0].fingers, [0, 0, 0, 0, 0, 0]);
  assert.equal(limpio.C.positions[0].baseFret, 1);
});

test("el -1 de dedo de chords-db se normaliza a 'sin dedo'", () => {
  // El F del diccionario base viene con fingers [-1,-1,3,2,1,1].
  const limpio = sanitizeDictionary({ F: [{ frets: [1, 3, 3, 2, 1, 1], fingers: [-1, -1, 3, 2, 1, 1], baseFret: 1, barres: [1] }] });
  assert.deepEqual(limpio.F.positions[0].fingers, [0, 0, 3, 2, 1, 1]);
  assert.throws(() => sanitizeDictionary({ F: [{ frets: [1, 1, 1, 1, 1, 1], fingers: [0, 0, 0, 0, 0, 9] }] }), ChordError);
});

test("la semilla no pisa lo que el administrador ya definió", () => {
  const mio = { Am: { positions: [{ frets: [5, 7, 7, 5, 5, 5], fingers: [1, 3, 4, 1, 1, 1], baseFret: 5, barres: [5] }] } };
  const { chords, added, kept } = mergeSeed(mio, { Am: { positions: [am] }, C: { positions: [am] } });
  assert.equal(kept, 1);
  assert.equal(added, 1);
  assert.equal(chords.Am.positions[0].baseFret, 5, "la versión del admin manda");
  assert.ok(chords.C);
});

test("el diccionario base pasa su propia validación", () => {
  const limpio = sanitizeDictionary(CHORD_SEED);
  const nombres = Object.keys(limpio);
  assert.ok(nombres.length > 300, "se esperaban cientos de acordes, hay " + nombres.length);
  for (const n of ["C", "Am", "G", "D7", "Fmaj7", "Bm"]) {
    assert.ok(limpio[n], "falta el acorde " + n);
  }
});

/* ---------- almacenamiento ---------- */

function fakeBucket(inicial) {
  return {
    guardado: inicial,
    async get() {
      if (this.guardado == null) return null;
      const texto = this.guardado;
      return { text: async () => texto };
    },
    async put(key, body) { this.guardado = body; }
  };
}

test("un blob corrupto no tumba la lectura, devuelve vacío", async () => {
  const env = { BUCKET: fakeBucket("{esto no es json") };
  assert.deepEqual(await readGlobalChords(env), emptyDictionary());
});

test("lo guardado se vuelve a leer igual", async () => {
  const env = { BUCKET: fakeBucket(null) };
  await writeGlobalChords(env, { Am: { positions: [am] } });
  const leido = await readGlobalChords(env);
  assert.deepEqual(leido.chords.Am.positions[0], am);
  assert.ok(leido.updatedAt > 0);
});

/* ---------- rutas ---------- */

const req = (method, path, body, token) => new Request("https://v.test" + path, {
  method,
  headers: token ? { Authorization: "Bearer " + token, "Content-Type": "application/json" }
                 : (body ? { "Content-Type": "application/json" } : {}),
  body: body ? JSON.stringify(body) : undefined
});
const llamar = (env, method, path, body, token) =>
  handleApi(req(method, path, body, token), env, new URL("https://v.test" + path), cors);

/** BD que solo sabe de usuarios: basta para probar los permisos de la ruta. */
const dbSinDatos = { prepare: () => ({ bind: () => ({ first: async () => null, all: async () => ({ results: [] }) }) }) };

test("el diccionario global lo lee cualquiera, también sin sesión", async () => {
  const env = { DB: dbSinDatos, BUCKET: fakeBucket(JSON.stringify({ version: 1, updatedAt: 5, chords: { Am: { positions: [am] } } })) };
  const res = await llamar(env, "GET", "/api/chords/global");
  assert.equal(res.status, 200);
  const datos = await res.json();
  assert.ok(datos.chords.Am);
});

test("sin sesión no se puede escribir el diccionario", async () => {
  const env = { DB: dbSinDatos, BUCKET: fakeBucket(null) };
  const res = await llamar(env, "PUT", "/api/chords/global", { chords: { Am: [am] } });
  assert.equal(res.status, 401);
  assert.equal(env.BUCKET.guardado, null, "no se ha escrito nada");
});
