import { strict as assert } from "node:assert";
import test from "node:test";
import {
  hashPassword, normalizeEmail, signToken, validateCredentials, verifyPassword, verifyToken
} from "../src/auth.js";

// Menos iteraciones en los tests: la seguridad la da el algoritmo, no la espera.
const FAST = 1000;

test("una contraseña se verifica contra su propio hash", async () => {
  const stored = await hashPassword("secreto-largo", FAST);
  assert.equal(await verifyPassword("secreto-largo", stored), true);
});

test("una contraseña incorrecta no pasa", async () => {
  const stored = await hashPassword("secreto-largo", FAST);
  assert.equal(await verifyPassword("otra-cosa", stored), false);
});

test("cada hash usa una sal distinta", async () => {
  const a = await hashPassword("misma", FAST);
  const b = await hashPassword("misma", FAST);
  assert.notEqual(a, b);
  assert.equal(await verifyPassword("misma", a), true);
  assert.equal(await verifyPassword("misma", b), true);
});

test("un hash con formato inválido se rechaza sin romper", async () => {
  assert.equal(await verifyPassword("x", ""), false);
  assert.equal(await verifyPassword("x", "loquesea"), false);
  assert.equal(await verifyPassword("x", "pbkdf2$abc$s$h"), false);
});

test("el token se verifica con su secreto", async () => {
  const token = await signToken({ sub: "u1", role: "admin" }, "clave");
  const payload = await verifyToken(token, "clave");
  assert.equal(payload.sub, "u1");
  assert.equal(payload.role, "admin");
});

test("un token firmado con otro secreto no vale", async () => {
  const token = await signToken({ sub: "u1" }, "clave");
  assert.equal(await verifyToken(token, "otra-clave"), null);
});

test("un token manipulado no vale", async () => {
  const token = await signToken({ sub: "u1", role: "user" }, "clave");
  const [h, , s] = token.split(".");
  const forged = Buffer.from(JSON.stringify({ sub: "u1", role: "admin", exp: 9e9 }))
    .toString("base64url");
  assert.equal(await verifyToken(`${h}.${forged}.${s}`, "clave"), null);
});

test("un token caducado no vale", async () => {
  const token = await signToken({ sub: "u1" }, "clave", -10);
  assert.equal(await verifyToken(token, "clave"), null);
});

test("basura como token no rompe", async () => {
  assert.equal(await verifyToken("", "clave"), null);
  assert.equal(await verifyToken("a.b", "clave"), null);
  assert.equal(await verifyToken("a.b.c", "clave"), null);
});

test("el email se normaliza a minúsculas y sin espacios", () => {
  assert.equal(normalizeEmail("  Hola@Ejemplo.COM "), "hola@ejemplo.com");
});

test("se validan email y longitud de contraseña", () => {
  assert.equal(validateCredentials("a@b.com", "12345678"), null);
  assert.match(validateCredentials("sin-arroba", "12345678"), /email/);
  assert.match(validateCredentials("a@b.com", "corta"), /contraseña/);
});
