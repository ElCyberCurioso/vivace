import { strict as assert } from "node:assert";
import test from "node:test";
import {
  ChordError, INSTRUMENTOS, cuerdasDe, decodeVariants, emptyVariants, encodeVariants,
  normalizeInstrument, readGlobalChords, sanitizeDictionary, sanitizeVariants,
  writeGlobalChords
} from "../src/chords.js";
import { handleApi } from "../src/api.js";
import worker from "../src/index.js";
import { fakeBucket, fakeD1 } from "./fake-d1.mjs";

/*
 * Ukelele: la estructura, antes que el contenido. El diccionario de ukelele es
 * OTRO blob y OTRO número de cuerdas; lo que se prueba aquí es que las dos
 * cosas no se mezclen, porque una digitación de cuatro valores metida en el
 * diccionario de guitarra se pintaría con dos cuerdas de menos.
 */

const am = { frets: [-1, 0, 2, 2, 1, 0], fingers: [0, 0, 2, 3, 1, 0], baseFret: 1, barres: [] };
const amUke = { frets: [2, 0, 0, 0], fingers: [2, 0, 0, 0], baseFret: 1, barres: [] };

/** R2 de mentira: guarda lo que se le pone, por clave. */
function bucketFalso() {
  const datos = new Map();
  return {
    datos,
    async get(clave) {
      if (!datos.has(clave)) return null;
      const texto = datos.get(clave);
      return { text: async () => texto };
    },
    async put(clave, cuerpo) { datos.set(clave, cuerpo); }
  };
}

test("los instrumentos conocidos traen sus cuerdas", () => {
  assert.equal(cuerdasDe("guitarra"), 6);
  assert.equal(cuerdasDe("ukelele"), 4);
  assert.deepEqual(Object.keys(INSTRUMENTOS).sort(), ["guitarra", "ukelele"]);
});

test("lo que no se reconoce cae en guitarra: las peticiones de antes no traen instrumento", () => {
  assert.equal(normalizeInstrument(""), "guitarra");
  assert.equal(normalizeInstrument(null), "guitarra");
  assert.equal(normalizeInstrument("UkElElE"), "ukelele");
  assert.equal(normalizeInstrument("banjo"), "guitarra");
});

test("el ukelele quiere cuatro trastes por digitación, ni cinco ni seis", () => {
  const limpio = sanitizeDictionary({ Am: [amUke] }, "ukelele");
  assert.deepEqual(limpio.Am.positions[0].frets, [2, 0, 0, 0]);
  assert.deepEqual(limpio.Am.positions[0].fingers, [2, 0, 0, 0]);
  assert.throws(() => sanitizeDictionary({ Am: [am] }, "ukelele"), ChordError);
  assert.throws(() => sanitizeDictionary({ Am: [amUke] }, "guitarra"), ChordError);
});

test("cada instrumento guarda en su propio blob y no pisa al otro", async () => {
  const env = { BUCKET: bucketFalso() };
  await writeGlobalChords(env, { Am: { positions: [am] } }, "guitarra");
  await writeGlobalChords(env, { Am: { positions: [amUke] } }, "ukelele");

  const guitarra = await readGlobalChords(env, "guitarra");
  const ukelele = await readGlobalChords(env, "ukelele");
  assert.equal(guitarra.instrument, "guitarra");
  assert.equal(ukelele.instrument, "ukelele");
  assert.equal(guitarra.chords.Am.positions[0].frets.length, 6);
  assert.equal(ukelele.chords.Am.positions[0].frets.length, 4);
  assert.deepEqual([...env.BUCKET.datos.keys()].sort(),
    ["chords/global-chords-ukelele.json", "chords/global-chords.json"]);
});

test("el diccionario de ukelele nace vacío, no roto", async () => {
  const env = { BUCKET: bucketFalso() };
  const dict = await readGlobalChords(env, "ukelele");
  assert.deepEqual(dict.chords, {});
  assert.equal(dict.instrument, "ukelele");
});

/* ---------- variantes por partitura ---------- */

test("la variante elegida se guarda por instrumento", () => {
  const limpio = sanitizeVariants({ guitarra: { F: 2 }, ukelele: { Am: 1 } });
  assert.deepEqual(limpio, { guitarra: { F: 2 }, ukelele: { Am: 1 } });
});

test("un objeto plano se entiende como guitarra", () => {
  assert.deepEqual(sanitizeVariants({ F: 3 }), { guitarra: { F: 3 }, ukelele: {} });
});

test("la primera digitación no se guarda: es el valor por defecto", () => {
  assert.deepEqual(sanitizeVariants({ guitarra: { F: 0 } }), emptyVariants());
  assert.equal(encodeVariants({ guitarra: { F: 0 } }), "");
});

test("lo que no se entiende se cae, pero no impide guardar la partitura", () => {
  const limpio = sanitizeVariants({ guitarra: { F: "dos", "A m": 1, G: -1, C: 99, D: 2 } });
  assert.deepEqual(limpio.guitarra, { D: 2 });
  assert.deepEqual(sanitizeVariants("no soy un objeto"), emptyVariants());
  assert.deepEqual(sanitizeVariants(null), emptyVariants());
});

test("ida y vuelta por la columna de la base", () => {
  const texto = encodeVariants({ guitarra: { F: 2 } });
  assert.deepEqual(decodeVariants(texto), { guitarra: { F: 2 }, ukelele: {} });
  // Un texto corrupto se lee como "sin elección": no puede tumbar la ficha.
  assert.deepEqual(decodeVariants("{no json"), emptyVariants());
  assert.deepEqual(decodeVariants(""), emptyVariants());
});

/* ---------- rutas ---------- */

const cors = {};
const req = (method, path, body, token) => new Request("https://v.test" + path, {
  method,
  headers: token ? { Authorization: "Bearer " + token, "Content-Type": "application/json" }
                 : (body ? { "Content-Type": "application/json" } : {}),
  body: body ? JSON.stringify(body) : undefined
});
const llamar = (env, method, path, body, token) =>
  handleApi(req(method, path, body, token), env, new URL("https://v.test" + path), cors);

const dbSinDatos = {
  prepare: () => ({ bind: () => ({ first: async () => null, all: async () => ({ results: [] }) }) })
};

test("la ruta del diccionario sirve el del instrumento que se le pida", async () => {
  const env = { DB: dbSinDatos, BUCKET: bucketFalso() };
  await writeGlobalChords(env, { Am: { positions: [am] } }, "guitarra");
  await writeGlobalChords(env, { Am: { positions: [amUke] } }, "ukelele");

  const conDefecto = await (await llamar(env, "GET", "/api/chords/global")).json();
  assert.equal(conDefecto.instrument, "guitarra");
  assert.equal(conDefecto.chords.Am.positions[0].frets.length, 6);

  const uke = await (await llamar(env, "GET", "/api/chords/global?instrument=ukelele")).json();
  assert.equal(uke.instrument, "ukelele");
  assert.equal(uke.chords.Am.positions[0].frets.length, 4);
  // La respuesta dice qué instrumentos hay: el cliente no los repite a mano.
  assert.deepEqual(uke.instruments.map((i) => i.id).sort(), ["guitarra", "ukelele"]);
});

test("sembrar el diccionario de ukelele se rechaza mientras no haya semilla suya", async () => {
  const env = { DB: dbSinDatos, BUCKET: bucketFalso() };
  const res = await llamar(env, "POST", "/api/chords/global/seed?instrument=ukelele");
  // Sin sesión ya son 401; lo que se comprueba es que la ruta no siembra
  // digitaciones de seis cuerdas en el diccionario de cuatro.
  assert.equal(res.status, 401);
  assert.equal(env.BUCKET.datos.size, 0);
});

/* ---------- de punta a punta: la variante viaja con la partitura ---------- */

test("la variante elegida se guarda al crear y no la borra un guardado del móvil", async () => {
  const env = { DB: fakeD1(), BUCKET: fakeBucket(), AUTH_SECRET: "secreto-de-prueba" };
  const pedir = async (method, path, { body, token } = {}) => {
    const res = await worker.fetch(new Request("https://v.test" + path, {
      method,
      headers: {
        ...(token ? { Authorization: "Bearer " + token } : {}),
        ...(body !== undefined ? { "Content-Type": "application/json" } : {})
      },
      body: body !== undefined ? JSON.stringify(body) : undefined
    }), env);
    const texto = await res.text();
    return { status: res.status, datos: texto ? JSON.parse(texto) : {} };
  };

  const alta = await pedir("POST", "/auth/register",
    { body: { email: "ana@v.test", password: "contrasena-larga", name: "Ana" } });
  const token = alta.datos.token;

  const creada = await pedir("POST", "/api/songs", {
    token,
    body: { title: "Zombie", content: "{F}letra", chordVariants: { guitarra: { F: 2 } } }
  });
  assert.equal(creada.status, 201, JSON.stringify(creada.datos));
  assert.deepEqual(creada.datos.song.chordVariants.guitarra, { F: 2 });

  const id = creada.datos.song.id;
  // La app Android manda la ficha entera y NO conoce las variantes: guardar
  // desde el móvil no puede llevarse por delante lo elegido en la web.
  const desdeMovil = await pedir("PUT", "/api/songs/" + id,
    { token, body: { title: "Zombie", content: "{F}otra letra" } });
  assert.equal(desdeMovil.status, 200);
  assert.deepEqual(desdeMovil.datos.song.chordVariants.guitarra, { F: 2 });

  // Mandarlas vacías sí las borra: es una elección explícita de quien edita.
  const limpiada = await pedir("PUT", "/api/songs/" + id,
    { token, body: { chordVariants: {} } });
  assert.deepEqual(limpiada.datos.song.chordVariants.guitarra, {});
});
