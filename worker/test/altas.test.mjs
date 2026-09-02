import { strict as assert } from "node:assert";
import test from "node:test";
import worker from "../src/index.js";
import { fakeBucket, fakeD1 } from "./fake-d1.mjs";

/*
 * Interruptor de altas de cuenta. Lo importante que fija esto: el corte está en
 * la API, no en la web —esconder el botón no impide un POST con curl—, y solo lo
 * mueve un administrador. Y la excepción del primer usuario, sin la cual cerrar
 * las altas en una instalación vacía la dejaría sin dueño y sin arreglo.
 */

const entorno = () => ({ DB: fakeD1(), BUCKET: fakeBucket(), AUTH_SECRET: "secreto-de-prueba" });

async function pedir(env, method, path, { body, token } = {}) {
  const res = await worker.fetch(new Request("https://accordio.site" + path, {
    method,
    headers: {
      ...(token ? { Authorization: "Bearer " + token } : {}),
      ...(body !== undefined ? { "Content-Type": "application/json" } : {})
    },
    body: body !== undefined ? JSON.stringify(body) : undefined
  }), env);
  const texto = await res.text();
  let datos = {};
  try { datos = texto ? JSON.parse(texto) : {}; } catch (e) { datos = { raw: texto }; }
  return { status: res.status, datos };
}

const alta = (env, email) => pedir(env, "POST", "/auth/register", {
  body: { email, password: "contrasena-larga", name: email.split("@")[0] }
});

test("por defecto las altas están abiertas", async () => {
  const env = entorno();
  const r = await pedir(env, "GET", "/api/settings");
  assert.equal(r.status, 200);
  assert.equal(r.datos.registrationOpen, true);
});

test("el administrador las cierra y entonces no se puede crear cuenta", async () => {
  const env = entorno();
  const admin = await alta(env, "admin@accordio.test");     // el primero, admin
  assert.equal(admin.status, 201);

  const cerrar = await pedir(env, "PUT", "/api/settings",
    { token: admin.datos.token, body: { registrationOpen: false } });
  assert.equal(cerrar.status, 200);
  assert.equal(cerrar.datos.registrationOpen, false);

  const intento = await alta(env, "otro@accordio.test");
  assert.equal(intento.status, 403);
  assert.match(intento.datos.error, /cerrad/i);

  // Y se refleja en la lectura pública, que es lo que mira la pantalla de entrada.
  const ajustes = await pedir(env, "GET", "/api/settings");
  assert.equal(ajustes.datos.registrationOpen, false);
});

test("volver a abrirlas devuelve el alta", async () => {
  const env = entorno();
  const admin = await alta(env, "admin@accordio.test");
  await pedir(env, "PUT", "/api/settings", { token: admin.datos.token, body: { registrationOpen: false } });
  await pedir(env, "PUT", "/api/settings", { token: admin.datos.token, body: { registrationOpen: true } });
  const r = await alta(env, "otro@accordio.test");
  assert.equal(r.status, 201);
});

test("cerrar las altas no impide entrar a quien ya tiene cuenta", async () => {
  const env = entorno();
  const admin = await alta(env, "admin@accordio.test");
  const usuaria = await alta(env, "marta@accordio.test");
  assert.equal(usuaria.status, 201);
  await pedir(env, "PUT", "/api/settings", { token: admin.datos.token, body: { registrationOpen: false } });

  const login = await pedir(env, "POST", "/auth/login",
    { body: { email: "marta@accordio.test", password: "contrasena-larga" } });
  assert.equal(login.status, 200);
});

test("un usuario normal no puede tocar el interruptor", async () => {
  const env = entorno();
  await alta(env, "admin@accordio.test");
  const usuaria = await alta(env, "marta@accordio.test");
  const r = await pedir(env, "PUT", "/api/settings",
    { token: usuaria.datos.token, body: { registrationOpen: false } });
  assert.equal(r.status, 403);
  // Y no ha cambiado nada.
  const ajustes = await pedir(env, "GET", "/api/settings");
  assert.equal(ajustes.datos.registrationOpen, true);
});

test("sin sesión tampoco", async () => {
  const env = entorno();
  await alta(env, "admin@accordio.test");
  const r = await pedir(env, "PUT", "/api/settings", { body: { registrationOpen: false } });
  assert.equal(r.status, 401);
});

test("el valor tiene que ser booleano", async () => {
  const env = entorno();
  const admin = await alta(env, "admin@accordio.test");
  const r = await pedir(env, "PUT", "/api/settings",
    { token: admin.datos.token, body: { registrationOpen: "no" } });
  assert.equal(r.status, 400);
});

test("con las altas cerradas, el PRIMER usuario aún puede darse de alta", async () => {
  // Si no, cerrar el grifo antes de que exista administrador dejaría la
  // instalación sin dueño y sin manera de volver a abrirlo.
  const env = { DB: fakeD1({ settings: [{ key: "registration_open", value: "0", updated_at: 1 }] }),
                BUCKET: fakeBucket(), AUTH_SECRET: "secreto-de-prueba" };
  const r = await alta(env, "admin@accordio.test");
  assert.equal(r.status, 201);
  assert.equal(r.datos.user.role, "admin");
  // Pero el segundo ya no.
  assert.equal((await alta(env, "otro@accordio.test")).status, 403);
});
