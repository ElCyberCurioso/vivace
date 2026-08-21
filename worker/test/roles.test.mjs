import { strict as assert } from "node:assert";
import test from "node:test";
import {
  canAddVersion, canEdit, canEditChords, canManageRoles, canPropose, canReview,
  canSetVisibility, canView, canWithdrawProposal, editDenialReason, isEditor,
  isValidRole
} from "../src/permissions.js";
import { isValidSort as sortDeDb, publicProposal, publicVersion } from "../src/db.js";

const usuario = { id: "u1", role: "user" };
const otro = { id: "u2", role: "user" };
const editor = { id: "e1", role: "editor" };
const admin = { id: "a1", role: "admin" };

const suya = { id: "s1", owner_id: "u1", visibility: "private", deleted_at: 0 };
const suyaPublica = { id: "s2", owner_id: "u1", visibility: "public", deleted_at: 0 };
const ajenaPrivada = { id: "s3", owner_id: "u9", visibility: "private", deleted_at: 0 };
const ajenaPublica = { id: "s4", owner_id: "u9", visibility: "public", deleted_at: 0 };

test("el editor manda sobre lo publicado, no sobre el cajón de nadie", () => {
  assert.equal(canEdit(editor, ajenaPublica), true);
  assert.equal(canEdit(editor, ajenaPrivada), false, "una privada ajena no es suya");
  assert.equal(canView(editor, ajenaPrivada), false);
});

test("el admin llega a todo", () => {
  assert.equal(canEdit(admin, ajenaPrivada), true);
  assert.equal(canView(admin, ajenaPrivada), true);
});

test("cada uno con lo suyo", () => {
  assert.equal(canEdit(usuario, suya), true);
  assert.equal(canEdit(usuario, ajenaPublica), false);
  assert.equal(canEdit(otro, suya), false);
});

test("publicar no lo decide quien sube la partitura", () => {
  assert.equal(canSetVisibility(usuario), false);
  assert.equal(canSetVisibility(editor), true);
  assert.equal(canSetVisibility(admin), true);
  assert.equal(canSetVisibility(null), false);
});

test("proponer publicación: solo lo tuyo y solo si no está publicado", () => {
  assert.equal(canPropose(usuario, suya, "publish"), true);
  assert.equal(canPropose(usuario, suyaPublica, "publish"), false, "ya está publicada");
  assert.equal(canPropose(usuario, ajenaPrivada, "publish"), false, "no es suya");
  assert.equal(canPropose(null, suya, "publish"), false);
});

test("proponer versión: de lo publicado, y cualquiera con sesión", () => {
  assert.equal(canPropose(usuario, ajenaPublica, "version"), true);
  assert.equal(canPropose(otro, ajenaPublica, "version"), true);
  assert.equal(canPropose(usuario, ajenaPrivada, "version"), false, "no está publicada");
  assert.equal(canPropose(null, ajenaPublica, "version"), false);
});

test("añadir versión directamente es de quien puede editar; el resto propone", () => {
  assert.equal(canAddVersion(usuario, suya), true);
  assert.equal(canAddVersion(otro, ajenaPublica), false);
  assert.equal(canAddVersion(editor, ajenaPublica), true);
});

test("revisar y tocar el diccionario es del equipo editorial; los roles, del admin", () => {
  assert.equal(canReview(editor), true);
  assert.equal(canEditChords(editor), true);
  assert.equal(canManageRoles(editor), false, "un editor no reparte roles");
  assert.equal(canManageRoles(admin), true);
  assert.equal(canReview(usuario), false);
  assert.equal(canEditChords(usuario), false);
});

test("isEditor incluye al admin: manda quien más manda", () => {
  assert.equal(isEditor(editor), true);
  assert.equal(isEditor(admin), true);
  assert.equal(isEditor(usuario), false);
  assert.equal(isEditor(null), false);
});

test("una propuesta la retira su autor mientras siga pendiente", () => {
  const pendiente = { author_id: "u1", status: "pending" };
  const resuelta = { author_id: "u1", status: "approved" };
  assert.equal(canWithdrawProposal(usuario, pendiente), true);
  assert.equal(canWithdrawProposal(otro, pendiente), false);
  assert.equal(canWithdrawProposal(editor, pendiente), true);
  assert.equal(canWithdrawProposal(usuario, resuelta), false, "ya está resuelta");
});

test("la papelera gana a cualquier permiso", () => {
  const enPapelera = { ...suya, deleted_at: 123 };
  assert.equal(canView(admin, enPapelera), false);
  assert.equal(canEdit(admin, enPapelera), false);
  assert.equal(canPropose(usuario, enPapelera, "publish"), false);
  assert.equal(editDenialReason(admin, enPapelera), "not_found");
});

test("solo se aceptan roles y órdenes conocidos", () => {
  assert.equal(isValidRole("user"), true);
  assert.equal(isValidRole("editor"), true);
  assert.equal(isValidRole("admin"), true);
  assert.equal(isValidRole("jefe"), false);
  assert.equal(isValidRole(""), false);
  // El orden se interpola en el SQL: si algo cuela aquí, entra en la consulta.
  assert.equal(sortDeDb("recent"), true);
  assert.equal(sortDeDb("old"), true);
  assert.equal(sortDeDb("title"), true);
  assert.equal(sortDeDb("s.title; DROP TABLE songs"), false);
  assert.equal(sortDeDb("__proto__"), false, "no vale una propiedad heredada");
});

test("los mapeadores no filtran claves internas de R2", () => {
  const v = publicVersion({
    id: "v1", song_id: "s1", name: "Acústica", r2_key: "songs/secreta.txt",
    capo: 2, source_url: "", position: 1, author_id: "u1",
    created_at: 1, updated_at: 2
  });
  assert.equal(v.name, "Acústica");
  assert.equal(v.capo, 2);
  assert.equal("r2Key" in v, false, "la clave de R2 no sale al cliente");

  const p = publicProposal({
    id: "p1", kind: "version", status: "pending", song_id: "s1", author_id: "u1",
    name: "En Do", capo: 0, source_url: "", r2_key: "songs/propuesta.txt",
    note: "más fácil", review_note: "", created_at: 1, resolved_at: 0
  });
  assert.equal(p.kind, "version");
  assert.equal(p.note, "más fácil");
  assert.equal("r2Key" in p, false);
});
