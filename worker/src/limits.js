/*
 * Vivace · topes de entrada y límite de intentos.
 *
 * Antes no había ninguno: `content` entraba en R2 sin medir, el blob de acordes
 * personales se guardaba con `request.text()` en crudo (sin comprobar siquiera
 * que fuese JSON) y `/auth/login` se podía probar a fuerza bruta sin freno.
 *
 * La decisión del límite de intentos vive en `rateDecision`, que es pura: no
 * toca la base y se puede probar sola.
 */

/** Texto de una partitura o de una versión. */
export const MAX_CONTENT = 512 * 1024;
/** Título, artista, género, nombre de versión o de lista. */
export const MAX_FIELD = 200;
/** URLs (origen, YouTube). */
export const MAX_URL = 2000;
/** Nota de una propuesta o motivo de un rechazo. */
export const MAX_NOTE = 2000;
/** Blob de acordes personales de un usuario. */
export const MAX_CHORDS_BLOB = 1024 * 1024;

/** Intentos de sesión permitidos por clave (email+IP) y ventana. */
export const AUTH_MAX_ATTEMPTS = 10;
export const AUTH_WINDOW_MS = 15 * 60 * 1000;

/**
 * Comprueba un campo de texto corto. Devuelve el mensaje de error o null.
 * Se aplica a lo que va a D1: sin esto, un solo campo podía llenar la fila.
 */
export function checkField(value, nombre, max = MAX_FIELD) {
  if (value === undefined || value === null) return null;
  if (typeof value !== "string") return `${nombre} debe ser texto`;
  if (value.length > max) return `${nombre} no puede pasar de ${max} caracteres`;
  return null;
}

/** Comprueba el cuerpo de una partitura. Devuelve el mensaje de error o null. */
export function checkContent(value, nombre = "el contenido") {
  if (value === undefined || value === null) return null;
  if (typeof value !== "string") return `${nombre} debe ser texto`;
  // Se mide en bytes UTF-8, que es lo que ocupa de verdad en R2.
  const bytes = new TextEncoder().encode(value).length;
  if (bytes > MAX_CONTENT) {
    return `${nombre} pasa del máximo (${Math.round(MAX_CONTENT / 1024)} KB)`;
  }
  return null;
}

/**
 * Valida de una vez los campos comunes de partitura/versión. Devuelve el primer
 * mensaje de error, o null si todo cabe.
 */
export function checkSongFields(body) {
  return checkField(body.title, "el título") ||
    checkField(body.artist, "el artista") ||
    checkField(body.genre, "la categoría") ||
    checkField(body.name, "el nombre") ||
    checkField(body.sourceUrl, "la URL de origen", MAX_URL) ||
    checkField(body.youtubeUrl, "el enlace de YouTube", MAX_URL) ||
    checkField(body.note, "la nota", MAX_NOTE) ||
    checkContent(body.content);
}

/**
 * Decide qué hacer con un intento de sesión, a partir de la fila guardada.
 * Pura a propósito: la parte que toca D1 es trivial, la que se equivoca es
 * esta.
 *
 *  - `start`  → no había ventana o ya venció: empieza una nueva con count = 1.
 *  - `allow`  → dentro de la ventana y por debajo del tope: suma uno.
 *  - `block`  → tope alcanzado; `retryAfter` son segundos hasta que expire.
 */
export function rateDecision(row, now, limit = AUTH_MAX_ATTEMPTS, windowMs = AUTH_WINDOW_MS) {
  if (!row || now - row.window_start >= windowMs) {
    return { action: "start", retryAfter: 0 };
  }
  if (row.count >= limit) {
    const restan = row.window_start + windowMs - now;
    return { action: "block", retryAfter: Math.max(1, Math.ceil(restan / 1000)) };
  }
  return { action: "allow", retryAfter: 0 };
}

/**
 * Aplica el límite sobre D1. Devuelve `{ ok, retryAfter }`.
 * Si no hay base de datos no bloquea a nadie: el objetivo es frenar la fuerza
 * bruta, no dejar la API inservible cuando falta el binding.
 */
export async function rateLimit(db, key, now = Date.now()) {
  if (!db) return { ok: true, retryAfter: 0 };
  const row = await db
    .prepare("SELECT count, window_start FROM auth_attempts WHERE key = ?")
    .bind(key).first();
  const { action, retryAfter } = rateDecision(row, now);
  if (action === "block") return { ok: false, retryAfter };
  if (action === "start") {
    await db.prepare(
      "INSERT INTO auth_attempts (key, count, window_start) VALUES (?, 1, ?) " +
      "ON CONFLICT(key) DO UPDATE SET count = 1, window_start = excluded.window_start"
    ).bind(key, now).run();
  } else {
    await db.prepare("UPDATE auth_attempts SET count = count + 1 WHERE key = ?")
      .bind(key).run();
  }
  return { ok: true, retryAfter: 0 };
}

/** Sesión correcta: se borra el contador para no penalizar al dueño legítimo. */
export async function clearRate(db, key) {
  if (!db) return;
  await db.prepare("DELETE FROM auth_attempts WHERE key = ?").bind(key).run();
}

/** Clave del contador: por email Y por IP, para no dejar puerta trasera. */
export function rateKey(prefix, email, request) {
  const ip = request.headers.get("CF-Connecting-IP") || "0.0.0.0";
  return `${prefix}:${String(email || "").toLowerCase().trim()}:${ip}`;
}
