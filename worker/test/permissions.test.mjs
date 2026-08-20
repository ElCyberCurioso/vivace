import { strict as assert } from "node:assert";
import test from "node:test";
import { canEdit, canView, editDenialReason, isValidVisibility } from "../src/permissions.js";

const ana = { id: "u-ana", role: "user" };
const luis = { id: "u-luis", role: "user" };
const admin = { id: "u-admin", role: "admin" };

const song = (over = {}) => ({
  id: "s1", owner_id: "u-ana", visibility: "private", deleted_at: 0, ...over
});

test("el dueño ve y edita lo suyo", () => {
  assert.equal(canView(ana, song()), true);
  assert.equal(canEdit(ana, song()), true);
});

test("una partitura privada no la ve otro usuario ni un visitante", () => {
  assert.equal(canView(luis, song()), false);
  assert.equal(canView(null, song()), false);
});

test("una partitura pública la ve cualquiera, incluso sin sesión", () => {
  const s = song({ visibility: "public" });
  assert.equal(canView(luis, s), true);
  assert.equal(canView(null, s), true);
});

test("publicar no da permiso para editar", () => {
  const s = song({ visibility: "public" });
  assert.equal(canEdit(luis, s), false);
  assert.equal(canEdit(null, s), false);
});

test("el administrador ve y edita cualquier partitura", () => {
  assert.equal(canView(admin, song()), true);
  assert.equal(canEdit(admin, song()), true);
});

test("lo que está en la papelera no se ve ni se edita", () => {
  const s = song({ deleted_at: 123, visibility: "public" });
  assert.equal(canView(ana, s), false);
  assert.equal(canEdit(ana, s), false);
  assert.equal(canView(null, s), false);
});

test("el motivo del rechazo distingue no encontrada, sin sesión y prohibido", () => {
  assert.equal(editDenialReason(ana, null), "not_found");
  assert.equal(editDenialReason(null, song()), "unauthorized");
  assert.equal(editDenialReason(luis, song()), "forbidden");
  assert.equal(editDenialReason(ana, song()), null);
});

test("solo se aceptan visibilidades conocidas", () => {
  assert.equal(isValidVisibility("private"), true);
  assert.equal(isValidVisibility("public"), true);
  assert.equal(isValidVisibility("secreta"), false);
  assert.equal(isValidVisibility(undefined), false);
});
