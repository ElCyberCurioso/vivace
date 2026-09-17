/*
 * Accordio · diccionario global de acordes.
 *
 * Un único blob en R2 (`chords/global-chords.json`) con las digitaciones que
 * ve TODO el mundo, incluido quien entra sin cuenta: los diagramas son parte
 * de leer una partitura, no una preferencia personal.
 *
 * Quien lo edita es solo el administrador. Los acordes personales de cada
 * usuario siguen por su lado (`users/<id>/chords.json`, ruta `/api/chords`):
 * son cosas distintas y no se mezclan.
 *
 * Formato:
 *   { "version": 1, "updatedAt": 1690000000000,
 *     "chords": { "Am": { "positions": [ { frets, fingers, baseFret, barres } ] } } }
 *
 * `frets` son seis números, de la sexta cuerda (Mi grave) a la primera:
 * -1 = cuerda al aire tachada (no suena), 0 = al aire, N = traste N contando
 * desde `baseFret`.
 */

export const GLOBAL_CHORDS_KEY = "chords/global-chords.json";

/*
 * Instrumentos. Cada uno tiene su propio diccionario, con su propio blob y su
 * número de cuerdas: un acorde de ukelele son CUATRO valores y no seis, así que
 * mezclarlos en el mismo fichero obligaría a adivinar de qué instrumento es
 * cada digitación. La guitarra conserva la clave de siempre para no mover lo
 * que ya está guardado en R2.
 *
 * El diccionario de ukelele nace vacío a propósito: la estructura está, y lo
 * que falta es el contenido, que se siembra con PUT /api/chords/global
 * (?instrument=ukelele) igual que se sembró el de guitarra.
 */
export const INSTRUMENTOS = {
  guitarra: { nombre: "Guitarra", cuerdas: 6, clave: GLOBAL_CHORDS_KEY },
  ukelele: { nombre: "Ukelele", cuerdas: 4, clave: "chords/global-chords-ukelele.json" }
};

export const INSTRUMENTO_POR_DEFECTO = "guitarra";

/**
 * Nombre de instrumento que llega de fuera (?instrument=, un campo del JSON).
 * Lo desconocido cae en la guitarra en vez de dar error: las peticiones que ya
 * existían no traen instrumento y tienen que seguir funcionando.
 */
export function normalizeInstrument(valor) {
  const limpio = String(valor == null ? "" : valor).trim().toLowerCase();
  return Object.prototype.hasOwnProperty.call(INSTRUMENTOS, limpio)
    ? limpio
    : INSTRUMENTO_POR_DEFECTO;
}

/** Cuántas cuerdas tiene el instrumento; es lo que valida cada digitación. */
export function cuerdasDe(instrumento) {
  return INSTRUMENTOS[normalizeInstrument(instrumento)].cuerdas;
}

/** Tantos acordes como para cubrir cualquier repertorio sin que el blob se desmande. */
/*
 * Tope de acordes del diccionario global. Eran 3.000, que se quedaba corto en
 * cuanto se siembra la biblioteca completa (guitar-chords-db-json: ~8.700
 * acordes, y el doble contando los alias en bemoles). El tope sigue estando
 * para que un PUT no pueda dejar cualquier cosa ahí dentro, solo que ahora a la
 * altura de la biblioteca que el propio Worker sirve.
 */
const MAX_CHORDS = 20000;
const MAX_POSITIONS = 12;
const MAX_FRET = 24;

/** Error de validación: la API lo traduce a un 400 con el motivo. */
export class ChordError extends Error {}

function entero(valor, min, max, campo) {
  const n = Number(valor);
  if (!Number.isInteger(n) || n < min || n > max) {
    throw new ChordError(`${campo} debe ser un entero entre ${min} y ${max}`);
  }
  return n;
}

/**
 * Nombre de acorde tal y como se escribe entre llaves en la partitura.
 * No se normaliza el enarmónico a propósito: si alguien define `Bb` y `A#`
 * por separado, es su decisión.
 */
export function normalizeChordName(name) {
  const limpio = String(name == null ? "" : name).trim();
  if (!limpio) throw new ChordError("el acorde necesita un nombre");
  if (limpio.length > 24) throw new ChordError(`nombre de acorde demasiado largo: ${limpio}`);
  if (/[{}\s]/.test(limpio)) throw new ChordError(`nombre de acorde no válido: ${limpio}`);
  return limpio;
}

function sanitizePosition(pos, nombre, cuerdas) {
  if (!pos || typeof pos !== "object") {
    throw new ChordError(`posición no válida en ${nombre}`);
  }
  if (!Array.isArray(pos.frets) || pos.frets.length !== cuerdas) {
    throw new ChordError(`${nombre}: frets debe traer ${cuerdas} valores, uno por cuerda`);
  }
  const frets = pos.frets.map((f) => entero(f, -1, MAX_FRET, `${nombre}: traste`));
  if (frets.every((f) => f === -1)) {
    throw new ChordError(`${nombre}: no puede tener todas las cuerdas apagadas`);
  }

  // chords-db marca con -1 el dedo de una cuerda apagada; aquí "sin dedo" es
  // 0 y punto, así que se normaliza en vez de rechazar el diccionario base.
  let fingers = new Array(cuerdas).fill(0);
  if (Array.isArray(pos.fingers) && pos.fingers.length === cuerdas) {
    fingers = pos.fingers.map((f) => Math.max(0, entero(f, -1, 5, `${nombre}: dedo`)));
  }

  const baseFret = pos.baseFret == null ? 1 : entero(pos.baseFret, 1, MAX_FRET, `${nombre}: baseFret`);

  let barres = [];
  if (Array.isArray(pos.barres)) {
    barres = pos.barres.slice(0, 4).map((b) => entero(b, 1, MAX_FRET, `${nombre}: cejilla`));
  }

  return { frets, fingers, baseFret, barres };
}

/**
 * Valida y limpia un diccionario entero. Devuelve solo lo que se guarda: nada
 * de campos sueltos que vengan de más, para que el blob no se llene de basura.
 */
export function sanitizeDictionary(input, instrumento = INSTRUMENTO_POR_DEFECTO) {
  const cuerdas = cuerdasDe(instrumento);
  const fuente = input && typeof input === "object" ? (input.chords || input) : null;
  if (!fuente || typeof fuente !== "object" || Array.isArray(fuente)) {
    throw new ChordError("se esperaba un objeto de acordes");
  }
  const nombres = Object.keys(fuente);
  if (nombres.length > MAX_CHORDS) {
    throw new ChordError(`demasiados acordes (máximo ${MAX_CHORDS})`);
  }

  const chords = {};
  for (const bruto of nombres) {
    const nombre = normalizeChordName(bruto);
    const entrada = fuente[bruto];
    const lista = Array.isArray(entrada) ? entrada
      : (entrada && Array.isArray(entrada.positions) ? entrada.positions : null);
    if (!lista) throw new ChordError(`${nombre}: falta la lista de posiciones`);
    if (!lista.length) continue;                       // acorde sin digitaciones: se descarta
    if (lista.length > MAX_POSITIONS) {
      throw new ChordError(`${nombre}: máximo ${MAX_POSITIONS} posiciones`);
    }
    chords[nombre] = { positions: lista.map((p) => sanitizePosition(p, nombre, cuerdas)) };
  }
  return chords;
}

/** Diccionario vacío, que es lo que se sirve mientras nadie haya guardado nada. */
export function emptyDictionary(instrumento = INSTRUMENTO_POR_DEFECTO) {
  return { version: 1, updatedAt: 0, instrument: normalizeInstrument(instrumento), chords: {} };
}

/**
 * Añade del diccionario base lo que falte, sin pisar nada de lo que ya hay:
 * si el administrador cambió una digitación, la suya manda.
 */
export function mergeSeed(actuales, semilla) {
  const chords = { ...actuales };
  let added = 0;
  for (const nombre of Object.keys(semilla)) {
    if (chords[nombre]) continue;
    chords[nombre] = semilla[nombre];
    added++;
  }
  return { chords, added, kept: Object.keys(actuales).length };
}

export async function readGlobalChords(env, instrumento = INSTRUMENTO_POR_DEFECTO) {
  const nombre = normalizeInstrument(instrumento);
  const obj = await env.BUCKET.get(INSTRUMENTOS[nombre].clave);
  if (!obj) return emptyDictionary(nombre);
  try {
    const datos = JSON.parse(await obj.text());
    return {
      version: datos.version || 1,
      updatedAt: datos.updatedAt || 0,
      instrument: nombre,
      chords: datos.chords || {}
    };
  } catch {
    // Un blob corrupto no puede tumbar la lectura de una partitura.
    return emptyDictionary(nombre);
  }
}

export async function writeGlobalChords(env, chords, instrumento = INSTRUMENTO_POR_DEFECTO) {
  const nombre = normalizeInstrument(instrumento);
  const cuerpo = { version: 1, updatedAt: Date.now(), instrument: nombre, chords };
  await env.BUCKET.put(INSTRUMENTOS[nombre].clave, JSON.stringify(cuerpo), {
    httpMetadata: { contentType: "application/json; charset=utf-8" }
  });
  return cuerpo;
}

/* ---------------------------- variantes por canción ---------------------------- */
/*
 * Qué digitación usa CADA acorde en ESTA partitura. Antes se pintaba siempre la
 * primera del diccionario, que para media biblioteca es una postura alta que no
 * es la que toca quien escribió la partitura.
 *
 * Formato: { guitarra: { "Am": 0, "F": 2 }, ukelele: { … } } — el número es el
 * índice dentro de `positions`. Se admite también el objeto plano (sin
 * instrumento), que se entiende como guitarra: es lo que mandaría un cliente
 * anterior a los instrumentos.
 */
const MAX_VARIANTES = 400;

export function emptyVariants() {
  const out = {};
  for (const nombre of Object.keys(INSTRUMENTOS)) out[nombre] = {};
  return out;
}

function sanitizeVariantMap(fuente) {
  const out = {};
  if (!fuente || typeof fuente !== "object" || Array.isArray(fuente)) return out;
  for (const bruto of Object.keys(fuente).slice(0, MAX_VARIANTES)) {
    const indice = Number(fuente[bruto]);
    if (!Number.isInteger(indice) || indice < 0 || indice >= MAX_POSITIONS) continue;
    let nombre;
    try {
      nombre = normalizeChordName(bruto);
    } catch {
      continue;                      // un nombre imposible se ignora, no rompe el guardado
    }
    if (indice === 0) continue;      // la primera es el valor por defecto: no se guarda
    out[nombre] = indice;
  }
  return out;
}

/**
 * Limpia lo que llega del cliente. Nunca lanza: una elección de variante rara
 * no puede impedir guardar la partitura, así que lo que no se entiende se cae.
 */
export function sanitizeVariants(input) {
  const out = emptyVariants();
  if (!input || typeof input !== "object" || Array.isArray(input)) return out;
  const porInstrumento = Object.keys(INSTRUMENTOS).some((n) =>
    input[n] && typeof input[n] === "object");
  if (!porInstrumento) {
    out[INSTRUMENTO_POR_DEFECTO] = sanitizeVariantMap(input);
    return out;
  }
  for (const nombre of Object.keys(INSTRUMENTOS)) out[nombre] = sanitizeVariantMap(input[nombre]);
  return out;
}

/** Lo que se guarda en la columna: "" cuando no hay ninguna elección. */
export function encodeVariants(variantes) {
  const limpio = sanitizeVariants(variantes);
  const vacio = Object.keys(limpio).every((n) => !Object.keys(limpio[n]).length);
  return vacio ? "" : JSON.stringify(limpio);
}

/** Lo que se lee de la columna. Un texto corrupto se trata como "sin elección". */
export function decodeVariants(texto) {
  if (!texto) return emptyVariants();
  try {
    return sanitizeVariants(JSON.parse(texto));
  } catch {
    return emptyVariants();
  }
}
