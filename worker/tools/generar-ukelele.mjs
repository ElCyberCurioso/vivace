/*
 * Genera el diccionario de ukelele del servidor a partir del que ya viene
 * empaquetado con la app:
 *
 *   node tools/generar-ukelele.mjs [fichero-de-salida]
 *
 * La fuente es `app/src/main/assets/chords/ukulele.json` (tombatossals/chords-db,
 * MIT) — el MISMO fichero que lee la app. Una sola copia en el repositorio y dos
 * consumidores: si algún día se actualiza, las dos piezas se mueven a la vez y
 * no hay manera de que la web y el móvil dibujen posturas distintas.
 *
 * No hace falta convertir trastes: chords-db ya los da RELATIVOS al traste base
 * (1 = el propio baseFret, 0 al aire, -1 muda), que es la convención de Accordio.
 * El trabajo de aquí es otro:
 *
 *  - Componer el NOMBRE del acorde tal como se escribe entre llaves en una
 *    partitura: clave + sufijo, con `major` -> "" y `minor` -> "m".
 *  - Registrar el alias enarmónico. chords-db escribe el ukelele en BEMOLES
 *    (Db, Eb, Gb, Ab, Bb); quien teclee {C#m} en una partitura no encontraría
 *    nada. Se apunta con los dos nombres, señalando a las mismas digitaciones.
 *  - Tirar lo que el servidor no guarda (`midi`), para que el blob no viaje con
 *    campos que `sanitizeDictionary` va a descartar de todas formas.
 *
 * La salida se sube con el PUT del diccionario global:
 *   PUT /api/chords/global?instrument=ukelele   (hace falta ser editor)
 * o desde la web: Acordes -> Instrumento: Ukelele -> Importar JSON…
 */
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join, resolve } from "node:path";

const aqui = dirname(fileURLToPath(import.meta.url));
const FUENTE = resolve(aqui, "../../app/src/main/assets/chords/ukulele.json");
const salidaPath = process.argv[2] || join(aqui, "ukelele-diccionario.json");

const db = JSON.parse(readFileSync(FUENTE, "utf8"));

// Cuatro cuerdas, o esto no es un ukelele: el servidor rechaza cualquier
// digitación que no traiga exactamente una por cuerda (src/chords.js).
if (db?.main?.strings !== 4) {
  console.error(`la fuente dice ${db?.main?.strings} cuerdas, se esperaban 4`);
  process.exit(1);
}

/** Bemol de chords-db -> sostenido, que es como lo escribe media España. */
const SOSTENIDO = { Db: "C#", Eb: "D#", Gb: "F#", Ab: "G#", Bb: "A#" };

/** Nombre del acorde: clave + sufijo, con los dos casos que no se escriben. */
function nombreDe(clave, sufijo) {
  const cuerpo = sufijo === "major" ? "" : sufijo === "minor" ? "m" : sufijo;
  return clave + cuerpo;
}

const chords = {};
let acordes = 0, digitaciones = 0, alias = 0;

for (const clave of Object.keys(db.chords)) {
  for (const entrada of db.chords[clave]) {
    // La clave del objeto lleva la sostenida escrita a la manera de chords-db
    // ("Csharp"); la buena es la que trae dentro cada acorde.
    const raiz = entrada.key || clave;
    const positions = (entrada.positions || []).map((p) => ({
      frets: p.frets,
      fingers: p.fingers && p.fingers.length === 4 ? p.fingers : [0, 0, 0, 0],
      baseFret: p.baseFret || 1,
      barres: p.barres || []
    }));
    if (!positions.length) continue;

    const nombre = nombreDe(raiz, entrada.suffix);
    chords[nombre] = { positions };
    acordes++;
    digitaciones += positions.length;

    const otro = SOSTENIDO[raiz];
    if (otro) {
      const enarmonico = nombreDe(otro, entrada.suffix);
      if (!chords[enarmonico]) { chords[enarmonico] = { positions }; alias++; }
    }
  }
}

writeFileSync(salidaPath, JSON.stringify({ chords }) + "\n");
console.log(
  `escrito ${salidaPath}\n` +
  `${acordes} acordes (+${alias} alias en sostenidos) · ${digitaciones} digitaciones`
);
