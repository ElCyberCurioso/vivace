/*
 * El esquema, contra SQLite de verdad.
 *
 * Todo lo demás se prueba contra `fake-d1.mjs`, que reconoce las consultas por
 * trozos de texto y nunca ejecuta una sentencia: sirve para probar reglas de
 * negocio, pero da por bueno cualquier SQL, incluido el que no compila.
 * Resultado: `schema.sql` y `migrations.sql` eran los dos únicos ficheros del
 * repositorio que solo se probaban al aplicarlos en PRODUCCIÓN.
 *
 * El reparto entre los dos ficheros es a propósito y NO es que uno duplique al
 * otro: `schema.sql` crea las tablas como eran, y `migrations.sql` acumula las
 * tandas posteriores. Una instalación nueva recibe los dos, en ese orden, que
 * es justo lo que hace `tools/deploy.sh`.
 *
 * Lo que vigila esto, por orden de importancia:
 *
 *  1. Que el SQL existe y compila. Una coma de más se veía al desplegar.
 *  2. Que aplicarlo DOS VECES no cambia nada. El despliegue lo repite cada vez,
 *     y se apoya en que el único error posible sea «duplicate column name»; si
 *     algún día aparece otro, el guion lo trata como fallo real y se planta.
 *  3. Que cada ALTER apunta a una tabla que existe.
 *  4. Que están las columnas y tablas que el código nombra. Publicar sin la
 *     columna `chord_variants` dejó la web sin poder guardar partituras.
 *
 * Usa `node:sqlite`, que viene con Node: cero dependencias nuevas. No es D1,
 * pero D1 ES SQLite, y para DDL se comportan igual.
 */
import { strict as assert } from "node:assert";
import test from "node:test";
import { DatabaseSync } from "node:sqlite";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const aqui = dirname(fileURLToPath(import.meta.url));
const lee = (nombre) => readFileSync(join(aqui, "..", nombre), "utf8");

/*
 * Partir el fichero en sentencias, igual que hace `tools/deploy.sh`: fuera los
 * comentarios y a trocear por el punto y coma. Vale porque los dos ficheros son
 * DDL sin cadenas de texto; si algún día llevan datos, esto habrá que cambiarlo
 * a la vez que el guion de despliegue.
 */
function sentencias(sql) {
  return sql.replace(/--[^\n]*/g, "").split(";").map((s) => s.trim()).filter(Boolean);
}

/** Radiografía de la base: tablas con sus columnas, e índices por nombre. */
function radiografia(db) {
  const tablas = db.prepare(
    "SELECT name FROM sqlite_master WHERE type = 'table' " +
    "AND name NOT LIKE 'sqlite_%' ORDER BY name"
  ).all().map((f) => f.name);

  const columnas = {};
  for (const tabla of tablas) {
    columnas[tabla] = db.prepare(
      `SELECT name FROM pragma_table_info('${tabla}') ORDER BY name`
    ).all().map((f) => f.name);
  }

  const indices = db.prepare(
    "SELECT name FROM sqlite_master WHERE type = 'index' " +
    "AND name NOT LIKE 'sqlite_%' ORDER BY name"
  ).all().map((f) => f.name);

  return { columnas, indices };
}

/**
 * Aplica `migrations.sql` como lo hace el despliegue: sentencia a sentencia,
 * tragándose «duplicate column name» y solo eso. Devuelve cuántas entraron de
 * verdad, que es lo que distingue el primer pase del segundo.
 */
function aplicarMigraciones(db) {
  let aplicadas = 0;
  for (const sentencia of sentencias(lee("migrations.sql"))) {
    try {
      db.exec(sentencia);
      if (/^ALTER\s+TABLE/i.test(sentencia)) aplicadas++;
    } catch (err) {
      assert.match(
        String(err.message), /duplicate column name/i,
        `migrations.sql falla por algo que no es «duplicate column name»: ${sentencia}`
      );
    }
  }
  return aplicadas;
}

/** La base tal como queda tras un despliegue: schema.sql y luego migrations.sql. */
function baseDesplegada() {
  const db = new DatabaseSync(":memory:");
  for (const sentencia of sentencias(lee("schema.sql"))) db.exec(sentencia);
  aplicarMigraciones(db);
  return db;
}

test("schema.sql se aplica entero sobre una base vacía", () => {
  const db = new DatabaseSync(":memory:");
  for (const sentencia of sentencias(lee("schema.sql"))) db.exec(sentencia);
  const { columnas } = radiografia(db);
  for (const tabla of ["users", "songs", "song_versions", "proposals", "comments",
                       "ratings", "playlists", "auth_attempts", "settings"]) {
    assert.ok(columnas[tabla], `falta la tabla ${tabla}`);
  }
  db.close();
});

test("repetir el despliegue no cambia el esquema", () => {
  /*
   * Esta es la invariante de la que vive `tools/deploy.sh`: puede aplicar
   * migrations.sql una y otra vez porque, pasado el primer turno, todo choca
   * con «duplicate column name» y ahí no pasa nada. Si alguna tanda futura deja
   * de ser repetible, el despliegue se plantará en producción; mejor aquí.
   */
  const db = baseDesplegada();
  const despuesDelPrimero = radiografia(db);

  const enElSegundoPase = aplicarMigraciones(db);
  assert.equal(enElSegundoPase, 0, "un ALTER ha vuelto a entrar en el segundo pase");
  assert.deepEqual(radiografia(db), despuesDelPrimero, "el segundo pase cambió el esquema");
  db.close();
});

test("cada ALTER de migrations.sql apunta a una tabla que existe", () => {
  const db = baseDesplegada();
  const { columnas } = radiografia(db);
  for (const sentencia of sentencias(lee("migrations.sql"))) {
    const alter = sentencia.match(/^ALTER\s+TABLE\s+([A-Za-z_][A-Za-z0-9_]*)/i);
    if (!alter) continue;
    assert.ok(columnas[alter[1]], `migrations.sql toca la tabla ${alter[1]}, que no existe`);
  }
  db.close();
});

test("están las columnas y tablas que el código nombra", () => {
  const db = baseDesplegada();
  const { columnas } = radiografia(db);

  // songs: lo que leen db.js y sync.js al guardar una partitura.
  for (const columna of ["chord_variants", "youtube_url", "rev", "favorite",
                         "position", "playlist_id", "deleted_at", "visibility"]) {
    assert.ok(columnas.songs.includes(columna), `songs no tiene ${columna}`);
  }
  // El contador de escrituras del que depende la detección de conflictos.
  assert.ok(columnas.song_versions.includes("rev"), "song_versions no tiene rev");
  // El freno de fuerza bruta y el interruptor de altas.
  assert.deepEqual(columnas.auth_attempts, ["count", "key", "window_start"]);
  assert.deepEqual(columnas.settings, ["key", "updated_at", "value"]);
  db.close();
});

test("los índices que sostienen el feed de sincronización existen", () => {
  const db = baseDesplegada();
  const { indices } = radiografia(db);
  // El feed recorre (updated_at, id); sin estos índices, cada sincronización de
  // la app se lleva por delante la tabla entera.
  for (const indice of ["idx_songs_owner_updated", "idx_versions_song_updated",
                        "idx_playlists_updated"]) {
    assert.ok(indices.includes(indice), `falta el índice ${indice}`);
  }
  db.close();
});
