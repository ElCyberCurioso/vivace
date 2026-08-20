/*
 * Vivace · autenticación
 * ----------------------
 * Contraseñas: PBKDF2-SHA256 con sal aleatoria por usuario (WebCrypto, sin
 * dependencias). Formato almacenado: `pbkdf2$<iteraciones>$<salt>$<hash>`.
 *
 * Sesión: JWT HS256 firmado con AUTH_SECRET. Es autocontenido (sin tabla de
 * sesiones); para invalidar todo basta con rotar el secreto.
 */

const ITERATIONS = 100_000;
const enc = new TextEncoder();

// ---- base64url ----
function b64urlEncode(bytes) {
  let bin = "";
  for (const b of new Uint8Array(bytes)) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
function b64urlDecode(str) {
  const pad = str.length % 4 ? "=".repeat(4 - (str.length % 4)) : "";
  const bin = atob(str.replace(/-/g, "+").replace(/_/g, "/") + pad);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

// ---- contraseñas ----
async function pbkdf2(password, salt, iterations) {
  const key = await crypto.subtle.importKey(
    "raw", enc.encode(password), "PBKDF2", false, ["deriveBits"]
  );
  const bits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", hash: "SHA-256", salt, iterations }, key, 256
  );
  return new Uint8Array(bits);
}

export async function hashPassword(password, iterations = ITERATIONS) {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const hash = await pbkdf2(password, salt, iterations);
  return `pbkdf2$${iterations}$${b64urlEncode(salt)}$${b64urlEncode(hash)}`;
}

/** Comprueba la contraseña en tiempo constante respecto al hash guardado. */
export async function verifyPassword(password, stored) {
  const parts = String(stored || "").split("$");
  if (parts.length !== 4 || parts[0] !== "pbkdf2") return false;
  const iterations = Number(parts[1]);
  if (!Number.isFinite(iterations) || iterations <= 0) return false;
  const salt = b64urlDecode(parts[2]);
  const expected = b64urlDecode(parts[3]);
  const actual = await pbkdf2(password, salt, iterations);
  if (actual.length !== expected.length) return false;
  let diff = 0;
  for (let i = 0; i < actual.length; i++) diff |= actual[i] ^ expected[i];
  return diff === 0;
}

// ---- JWT (HS256) ----
async function hmacKey(secret) {
  return crypto.subtle.importKey(
    "raw", enc.encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign", "verify"]
  );
}

/** Firma un token con caducidad en [expiresInSec] (7 días por defecto). */
export async function signToken(payload, secret, expiresInSec = 7 * 24 * 3600) {
  const header = { alg: "HS256", typ: "JWT" };
  const now = Math.floor(Date.now() / 1000);
  const body = { ...payload, iat: now, exp: now + expiresInSec };
  const data = `${b64urlEncode(enc.encode(JSON.stringify(header)))}.${b64urlEncode(enc.encode(JSON.stringify(body)))}`;
  const sig = await crypto.subtle.sign("HMAC", await hmacKey(secret), enc.encode(data));
  return `${data}.${b64urlEncode(sig)}`;
}

/**
 * Devuelve el payload si la firma y la caducidad son válidas; si no, null.
 * Nunca lanza: un encabezado con basura debe traducirse en "sin sesión"
 * (401), no en un error del servidor.
 */
export async function verifyToken(token, secret) {
  try {
    const parts = String(token || "").split(".");
    if (parts.length !== 3) return null;
    const data = `${parts[0]}.${parts[1]}`;
    const ok = await crypto.subtle.verify(
      "HMAC", await hmacKey(secret), b64urlDecode(parts[2]), enc.encode(data)
    );
    if (!ok) return null;
    const payload = JSON.parse(new TextDecoder().decode(b64urlDecode(parts[1])));
    const now = Math.floor(Date.now() / 1000);
    if (typeof payload.exp !== "number" || payload.exp < now) return null;
    return payload;
  } catch (e) {
    return null;
  }
}

/** Extrae el token del encabezado `Authorization: Bearer <token>`. */
export function bearerToken(request) {
  const h = request.headers.get("Authorization") || "";
  return h.startsWith("Bearer ") ? h.slice(7).trim() : "";
}

/** Normaliza el email para comparar/guardar sin distinguir mayúsculas. */
export function normalizeEmail(email) {
  return String(email || "").trim().toLowerCase();
}

/** Validaciones mínimas de alta de usuario. */
export function validateCredentials(email, password) {
  const e = normalizeEmail(email);
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(e)) return "email no válido";
  if (String(password || "").length < 8) return "la contraseña debe tener al menos 8 caracteres";
  return null;
}
