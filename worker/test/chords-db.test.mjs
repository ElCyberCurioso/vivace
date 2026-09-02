import { strict as assert } from "node:assert";
import test from "node:test";
import { expandirChordDb } from "../src/chords-db.js";
import { sanitizeDictionary } from "../src/chords.js";
import { CLIENT_JS } from "../src/client-lib.js";

/*
 * Biblioteca de acordes importada de guitar-chords-db-json. Lo que se fija aquí
 * es la CONVERSIÓN: el origen da trastes absolutos en base 36 y el diccionario
 * de Accordio los quiere relativos al traste base, como chords-db y como la app.
 * Una conversión mal hecha no se nota en un test de tamaño: se nota en que el
 * diagrama sale con los dedos en otro sitio.
 */

const dict = expandirChordDb();

test("trae la biblioteca entera, con alias en bemoles", () => {
  assert.ok(Object.keys(dict).length > 8000, "acordes: " + Object.keys(dict).length);
  assert.ok(dict["C"], "falta C");
  assert.ok(dict["Bb"], "falta el alias Bb de A#");
  assert.ok(dict["Ebm7"], "falta el alias Ebm7 de D#m7");
  assert.ok(dict["D/F#"], "faltan los acordes con bajo");
});

test("las digitaciones conocidas salen bien", () => {
  // C mayor: x32010. En relativo con traste base 1 es lo mismo.
  assert.deepEqual(dict["C"].positions[0].frets, [-1, 3, 2, 0, 1, 0]);
  assert.equal(dict["C"].positions[0].baseFret, 1);
  // Cmaj7: x32000.
  assert.deepEqual(dict["Cmaj7"].positions[0].frets, [-1, 3, 2, 0, 0, 0]);
  // D/F#: 200232.
  assert.deepEqual(dict["D/F#"].positions[0].frets, [2, 0, 0, 2, 3, 2]);
});

test("todo cabe en el diagrama: 5 trastes, base entre 1 y 24", () => {
  for (const [nombre, entrada] of Object.entries(dict)) {
    for (const p of entrada.positions) {
      assert.equal(p.frets.length, 6, nombre);
      assert.equal(p.fingers.length, 6, nombre);
      assert.ok(p.baseFret >= 1 && p.baseFret <= 24, nombre + " base " + p.baseFret);
      for (const f of p.frets) assert.ok(f >= -1 && f <= 5, nombre + " traste " + f);
      for (const b of p.barres) assert.ok(b >= 1 && b <= 5, nombre + " cejilla " + b);
    }
  }
});

test("una posición alta se guarda desplazada, no recortada", () => {
  // Si algo quedara en absoluto, aparecerían trastes > 5 y el test de arriba
  // ya habría fallado; esto comprueba lo contrario: que las hay con base alta.
  const conBaseAlta = Object.values(dict).filter((e) => e.positions.some((p) => p.baseFret > 1));
  assert.ok(conBaseAlta.length > 500, "posiciones altas: " + conBaseAlta.length);
});

test("el diccionario pasa la validación del Worker", () => {
  const limpio = sanitizeDictionary(dict);
  assert.ok(Object.keys(limpio).length > 8000);
});

test("el dibujante coloca los puntos con la misma convención", () => {
  const vChordSvg = new Function(CLIENT_JS + "\nreturn vChordSvg;")();
  // Bb con cejilla: x13331 sobre el traste base 1 → cinco puntos y una cejilla.
  const svg = vChordSvg(dict["Bb"].positions[0], 110);
  assert.equal((svg.match(/<circle/g) || []).length, 5);
  assert.match(svg, /rx="/);           // la barra de la cejilla
});
