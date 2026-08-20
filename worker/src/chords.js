/*
 * Vivace · diccionario global de acordes.
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

/** Tantos acordes como para cubrir cualquier repertorio sin que el blob se desmande. */
const MAX_CHORDS = 3000;
const MAX_POSITIONS = 12;
const MAX_FRET = 24;
const CUERDAS = 6;

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

function sanitizePosition(pos, nombre) {
  if (!pos || typeof pos !== "object") {
    throw new ChordError(`posición no válida en ${nombre}`);
  }
  if (!Array.isArray(pos.frets) || pos.frets.length !== CUERDAS) {
    throw new ChordError(`${nombre}: frets debe traer ${CUERDAS} valores, uno por cuerda`);
  }
  const frets = pos.frets.map((f) => entero(f, -1, MAX_FRET, `${nombre}: traste`));
  if (frets.every((f) => f === -1)) {
    throw new ChordError(`${nombre}: no puede tener las seis cuerdas apagadas`);
  }

  // chords-db marca con -1 el dedo de una cuerda apagada; aquí "sin dedo" es
  // 0 y punto, así que se normaliza en vez de rechazar el diccionario base.
  let fingers = [0, 0, 0, 0, 0, 0];
  if (Array.isArray(pos.fingers) && pos.fingers.length === CUERDAS) {
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
export function sanitizeDictionary(input) {
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
    chords[nombre] = { positions: lista.map((p) => sanitizePosition(p, nombre)) };
  }
  return chords;
}

/** Diccionario vacío, que es lo que se sirve mientras nadie haya guardado nada. */
export function emptyDictionary() {
  return { version: 1, updatedAt: 0, chords: {} };
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

export async function readGlobalChords(env) {
  const obj = await env.BUCKET.get(GLOBAL_CHORDS_KEY);
  if (!obj) return emptyDictionary();
  try {
    const datos = JSON.parse(await obj.text());
    return {
      version: datos.version || 1,
      updatedAt: datos.updatedAt || 0,
      chords: datos.chords || {}
    };
  } catch (e) {
    // Un blob corrupto no puede tumbar la lectura de una partitura.
    return emptyDictionary();
  }
}

export async function writeGlobalChords(env, chords) {
  const cuerpo = { version: 1, updatedAt: Date.now(), chords };
  await env.BUCKET.put(GLOBAL_CHORDS_KEY, JSON.stringify(cuerpo), {
    httpMetadata: { contentType: "application/json; charset=utf-8" }
  });
  return cuerpo;
}
