import { strict as assert } from "node:assert";
import test from "node:test";
import {
  AUTH_MAX_ATTEMPTS, AUTH_WINDOW_MS, MAX_CONTENT, checkContent, checkField,
  checkSongFields, rateDecision
} from "../src/limits.js";

test("un campo corto pasa y uno largo no", () => {
  assert.equal(checkField("Wonderwall", "el título"), null);
  assert.equal(checkField(undefined, "el título"), null, "lo que no viene no se valida");
  assert.ok(checkField("x".repeat(201), "el título"));
  assert.ok(checkField(42, "el título"), "un número no es texto");
});

test("el contenido se mide en bytes UTF-8, no en caracteres", () => {
  // 'ñ' ocupa dos bytes: justo por eso medir .length engañaría.
  const casiLleno = "ñ".repeat(MAX_CONTENT / 2);
  assert.equal(checkContent(casiLleno), null);
  assert.ok(checkContent("ñ".repeat(MAX_CONTENT / 2 + 1)));
});

test("checkSongFields devuelve el primer problema que encuentra", () => {
  assert.equal(checkSongFields({ title: "A", artist: "B", content: "{Am}" }), null);
  assert.ok(checkSongFields({ title: "x".repeat(500) }));
  assert.ok(checkSongFields({ sourceUrl: "u".repeat(3000) }));
});

test("la primera ventana empieza, las siguientes suman", () => {
  assert.equal(rateDecision(null, 1000).action, "start");
  assert.equal(rateDecision({ count: 1, window_start: 1000 }, 1500).action, "allow");
  assert.equal(rateDecision({ count: AUTH_MAX_ATTEMPTS - 1, window_start: 1000 }, 1500).action, "allow");
});

test("al llegar al tope bloquea y dice cuánto falta", () => {
  const inicio = 1_000_000;
  const d = rateDecision({ count: AUTH_MAX_ATTEMPTS, window_start: inicio }, inicio + 60_000);
  assert.equal(d.action, "block");
  assert.equal(d.retryAfter, Math.ceil((AUTH_WINDOW_MS - 60_000) / 1000));
});

test("pasada la ventana se empieza de cero aunque se hubiera bloqueado", () => {
  const inicio = 1_000_000;
  const d = rateDecision({ count: 999, window_start: inicio }, inicio + AUTH_WINDOW_MS);
  assert.equal(d.action, "start", "el bloqueo caduca solo");
});
