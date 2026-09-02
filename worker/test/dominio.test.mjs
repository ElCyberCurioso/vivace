import { strict as assert } from "node:assert";
import test from "node:test";
import worker from "../src/index.js";
import { fakeBucket, fakeD1 } from "./fake-d1.mjs";

/*
 * El dominio propio (accordio.site) entra por el mismo Worker que la web y la
 * API. Lo único que hace falta comprobar aquí es que hay UN host canónico: con
 * dos (apex y www) la sesión del navegador, que vive en el almacenamiento
 * local, se partiría en dos según por dónde se entrase.
 */

const env = () => ({ DB: fakeD1(), BUCKET: fakeBucket(), AUTH_SECRET: "secreto-de-prueba" });

const pedir = (url, method = "GET") => worker.fetch(new Request(url, { method }), env());

test("www redirige al apex conservando ruta y query", async () => {
  const res = await pedir("https://www.accordio.site/api/songs/public?limit=5");
  assert.equal(res.status, 301);
  assert.equal(res.headers.get("Location"), "https://accordio.site/api/songs/public?limit=5");
});

test("el apex no redirige: sirve la web", async () => {
  const res = await pedir("https://accordio.site/");
  assert.equal(res.status, 200);
  assert.match(res.headers.get("Content-Type") || "", /text\/html/);
});

test("el subdominio de workers.dev sigue funcionando", async () => {
  const res = await pedir("https://guitarchords-sync.elcybercurioso.workers.dev/");
  assert.equal(res.status, 200);
});

test("HTTP en claro redirige a HTTPS", async () => {
  const res = await pedir("http://accordio.site/api/songs/public?limit=5");
  assert.equal(res.status, 301);
  assert.equal(res.headers.get("Location"), "https://accordio.site/api/songs/public?limit=5");
});

test("HTTP y www se arreglan de una vez, no en dos saltos", async () => {
  const res = await pedir("http://www.accordio.site/algo");
  assert.equal(res.status, 301);
  assert.equal(res.headers.get("Location"), "https://accordio.site/algo");
});

test("las respuestas por HTTPS llevan HSTS", async () => {
  const res = await pedir("https://accordio.site/");
  assert.match(res.headers.get("Strict-Transport-Security") || "", /max-age=31536000/);
});

test("en local (wrangler dev) ni se fuerza HTTPS ni se manda HSTS", async () => {
  // Con esto puesto, el navegador dejaría de poder abrir http://localhost.
  const res = await pedir("http://localhost:8788/");
  assert.equal(res.status, 200);
  assert.equal(res.headers.get("Strict-Transport-Security"), null);
});

test("un POST en claro se redirige con 308: no puede perder el cuerpo", async () => {
  const res = await pedir("http://accordio.site/auth/login", "POST");
  assert.equal(res.status, 308);
  assert.equal(res.headers.get("Location"), "https://accordio.site/auth/login");
});
