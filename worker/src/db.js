/*
 * Vivace · acceso a D1 (usuarios y metadatos de partituras).
 * El texto de cada partitura vive en R2; aquí solo su ficha y permisos.
 */

export const SONG_PREFIX = "songs/";

export function uuid() {
  return crypto.randomUUID();
}

// ---- usuarios ----
export async function findUserByEmail(db, emailLower) {
  return db.prepare("SELECT * FROM users WHERE email_lower = ?").bind(emailLower).first();
}

export async function findUserById(db, id) {
  return db.prepare("SELECT * FROM users WHERE id = ?").bind(id).first();
}

export async function countUsers(db) {
  const row = await db.prepare("SELECT COUNT(*) AS n FROM users").first();
  return Number(row?.n || 0);
}

export async function createUser(db, { email, name, passwordHash, role }) {
  const id = uuid();
  await db.prepare(
    `INSERT INTO users (id, email, email_lower, name, password_hash, role, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  ).bind(id, email, email.toLowerCase(), name || "", passwordHash, role || "user", Date.now()).run();
  return findUserById(db, id);
}

/** Datos del usuario que se pueden devolver al cliente (nunca el hash). */
export function publicUser(user) {
  if (!user) return null;
  return { id: user.id, email: user.email, name: user.name, role: user.role };
}

// ---- ajustes de la instalación ----

/*
 * Tabla clave -> valor. Lo que no está guardado vale su valor por defecto, así
 * que una instalación nueva (o una que aún no ha pasado la migración) se
 * comporta como siempre: con las altas abiertas, que es como entra el primer
 * administrador.
 *
 * Se lee en cada alta, no se cachea: son cuatro bytes y un interruptor que se
 * apaga precisamente cuando hay prisa por cerrar el grifo.
 */
export async function readSetting(db, key, porDefecto = null) {
  try {
    const fila = await db.prepare("SELECT value FROM settings WHERE key = ?").bind(key).first();
    return fila ? fila.value : porDefecto;
  } catch (e) {
    // La tabla puede no existir todavía (despliegue nuevo, migración sin pasar).
    // Quedarse sin ajustes no puede tumbar el registro ni la web.
    return porDefecto;
  }
}

export async function writeSetting(db, key, value) {
  await db.prepare(
    `INSERT INTO settings (key, value, updated_at) VALUES (?, ?, ?)
     ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at`
  ).bind(key, String(value), Date.now()).run();
}

/** Clave del interruptor de altas y su lectura como booleano. */
export const SETTING_REGISTRATION = "registration_open";

export async function registrationOpen(db) {
  return (await readSetting(db, SETTING_REGISTRATION, "1")) !== "0";
}

// ---- partituras ----
export async function findSongById(db, id) {
  return db.prepare("SELECT * FROM songs WHERE id = ?").bind(id).first();
}

export async function findSongByKey(db, r2Key) {
  return db.prepare("SELECT * FROM songs WHERE r2_key = ?").bind(r2Key).first();
}

/**
 * Tamaño de página de los listados. Ninguno estaba acotado: una cuenta con
 * miles de partituras devolvía la colección entera en cada llamada.
 * Se pide una fila de más para saber si queda algo detrás sin contar el total.
 */
export const PAGE_SIZE = 100;
export const PAGE_MAX = 500;

export function clampPage({ limit, offset } = {}) {
  const n = Number(limit);
  const o = Number(offset);
  return {
    limit: Number.isFinite(n) && n > 0 ? Math.min(Math.floor(n), PAGE_MAX) : PAGE_SIZE,
    offset: Number.isFinite(o) && o > 0 ? Math.floor(o) : 0
  };
}

/*
 * Búsqueda por texto. Se compara en minúsculas y SIN TILDES por los dos lados,
 * porque nadie teclea «Bulería» con el acento puesto en el buscador. LOWER() de
 * SQLite solo baja ASCII, así que las vocales acentuadas se quitan a mano; la
 * eñe se deja, que sí se escribe.
 *
 * Se busca en título y artista, que es lo que el usuario ve en la tarjeta.
 */
const VOCALES = [["á","a"],["à","a"],["é","e"],["è","e"],["í","i"],["ì","i"],
                 ["ó","o"],["ò","o"],["ú","u"],["ù","u"],["ü","u"]];

function sinTildesSql(columna) {
  return VOCALES.reduce(
    (sql, [con, sin]) => `REPLACE(${sql},'${con}','${sin}')`,
    `LOWER(${columna})`
  );
}

/** La misma normalización, para el texto que llega del buscador. */
export function normalizarBusqueda(q) {
  let t = String(q == null ? "" : q).trim().toLowerCase();
  for (const [con, sin] of VOCALES) t = t.split(con).join(sin);
  return t;
}

/**
 * Añade la condición LIKE del buscador a la consulta, si hay texto que buscar.
 * Los comodines del propio LIKE se escapan: sin esto, buscar "%" lo devolvería
 * todo y "_" haría de comodín de un carácter.
 */
function filtroBusqueda(q, condiciones, valores, prefijo = "") {
  const texto = normalizarBusqueda(q);
  if (!texto) return;
  const patron = "%" + texto.replace(/[\\%_]/g, "\\$&") + "%";
  condiciones.push(
    `(${sinTildesSql(prefijo + "title")} LIKE ? ESCAPE '\\' OR ` +
    `${sinTildesSql(prefijo + "artist")} LIKE ? ESCAPE '\\')`
  );
  valores.push(patron, patron);
}

/** Corta la fila sobrante y dice si había más. */
function paginar(filas, limit) {
  const hasMore = filas.length > limit;
  return { items: hasMore ? filas.slice(0, limit) : filas, hasMore };
}

/** Partituras del usuario (activas), por título. */
export async function listOwnSongs(db, ownerId, pagina = {}) {
  const { limit, offset } = clampPage(pagina);
  const condiciones = ["owner_id = ?", "deleted_at = 0"];
  const valores = [ownerId];
  filtroBusqueda(pagina.q, condiciones, valores);
  const { results } = await db.prepare(
    `SELECT * FROM songs WHERE ${condiciones.join(" AND ")}
     ORDER BY title COLLATE NOCASE ASC LIMIT ? OFFSET ?`
  ).bind(...valores, limit + 1, offset).all();
  return paginar(results || [], limit);
}

/**
 * Catálogo público. Sin [ownerId] devuelve todas las publicadas; el cliente web
 * lo usa por defecto con el id del admin para mostrar su selección.
 */
/**
 * Orden del catálogo. Se resuelve aquí y no en el cliente porque ordenar por
 * fecha con el listado ya recortado daría un resultado engañoso.
 */
const ORDENES = {
  title: "s.title COLLATE NOCASE ASC",
  recent: "s.created_at DESC",
  old: "s.created_at ASC"
};

export function isValidSort(sort) {
  return Object.prototype.hasOwnProperty.call(ORDENES, sort);
}

/**
 * Catálogo público. [ownerId] lo acota a un dueño; [genre] a una categoría
 * (comparada sin distinguir mayúsculas) y [sort] elige el orden.
 */
export async function listPublicSongs(db, ownerId = null, opciones = {}) {
  const { genre = "", sort = "title" } = opciones;
  const orden = ORDENES[sort] || ORDENES.title;
  const condiciones = ["s.visibility = 'public'", "s.deleted_at = 0"];
  const valores = [];
  if (ownerId) { condiciones.push("s.owner_id = ?"); valores.push(ownerId); }
  if (genre) { condiciones.push("LOWER(s.genre) = ?"); valores.push(String(genre).toLowerCase()); }
  filtroBusqueda(opciones.q, condiciones, valores, "s.");
  const { limit, offset } = clampPage(opciones);
  const sql = `SELECT s.*, u.name AS owner_name FROM songs s JOIN users u ON u.id = s.owner_id
     WHERE ${condiciones.join(" AND ")} ORDER BY ${orden} LIMIT ? OFFSET ?`;
  valores.push(limit + 1, offset);
  const { results } = await db.prepare(sql).bind(...valores).all();
  return paginar(results || [], limit);
}

/**
 * Recomendaciones para quien está leyendo una partitura: primero **del mismo
 * artista**, y si no hay, **del mismo estilo**. En dos consultas y no en una con
 * OR porque el criterio no es un filtro, es una prioridad: mezclarlos daría
 * media lista de cada cosa, y lo que se quiere es «más de este artista» y, solo
 * cuando no existe, «más de este palo».
 *
 * Solo entran partituras publicadas y nunca la que se está viendo. El artista se
 * compara sin tildes ni mayúsculas, igual que el buscador: «Jarabe de palo» y
 * «Jarabe de Palo» son el mismo grupo.
 *
 * Devuelve además [motivo] para que la web pueda titular la sección con la
 * verdad («Más de David Bisbal» / «Más flamenco»), en vez de un genérico.
 */
export async function listRelatedSongs(db, cancion, opciones = {}) {
  if (!cancion) return { items: [], motivo: "" };
  const limite = Math.min(Math.max(Number(opciones.limit) || 6, 1), 12);
  const ownerId = opciones.ownerId || null;

  const comunes = ["s.visibility = 'public'", "s.deleted_at = 0", "s.id <> ?"];
  const base = [cancion.id];
  if (ownerId) { comunes.push("s.owner_id = ?"); base.push(ownerId); }

  const consulta = async (condicion, valores) => {
    const sql = `SELECT s.*, u.name AS owner_name FROM songs s JOIN users u ON u.id = s.owner_id
       WHERE ${comunes.concat(condicion).join(" AND ")}
       ORDER BY s.title COLLATE NOCASE ASC LIMIT ?`;
    const { results } = await db.prepare(sql).bind(...base, ...valores, limite).all();
    return results || [];
  };

  const artista = normalizarBusqueda(cancion.artist);
  if (artista) {
    const mismos = await consulta([`${sinTildesSql("s.artist")} = ?`], [artista]);
    if (mismos.length) return { items: mismos, motivo: "artist" };
  }

  const genero = String(cancion.genre || "").trim().toLowerCase();
  if (genero) {
    // Se descarta el mismo artista: si estuviera, ya habría salido por arriba.
    const delPalo = await consulta(
      ["LOWER(s.genre) = ?", `${sinTildesSql("s.artist")} <> ?`],
      [genero, artista]
    );
    if (delPalo.length) return { items: delPalo, motivo: "genre" };
  }

  return { items: [], motivo: "" };
}

/** Categorías con al menos una partitura publicada, para poblar el filtro. */
export async function listPublicGenres(db, ownerId = null) {
  // Mismo alcance que el catálogo: si la portada solo enseña lo del admin, el
  // filtro no puede ofrecer categorías que allí no devuelven nada.
  const sql = ownerId
    ? `SELECT genre, COUNT(*) AS total FROM songs
       WHERE visibility = 'public' AND deleted_at = 0 AND genre <> '' AND owner_id = ?
       GROUP BY LOWER(genre) ORDER BY genre COLLATE NOCASE ASC`
    : `SELECT genre, COUNT(*) AS total FROM songs
       WHERE visibility = 'public' AND deleted_at = 0 AND genre <> ''
       GROUP BY LOWER(genre) ORDER BY genre COLLATE NOCASE ASC`;
  const stmt = ownerId ? db.prepare(sql).bind(ownerId) : db.prepare(sql);
  const { results } = await stmt.all();
  return results || [];
}

/**
 * Todas las claves de R2 ya indexadas, en una sola consulta. La migracion la
 * usa para saltarse lo conocido sin gastar una consulta por cancion: cada
 * llamada a un binding cuenta como subpeticion y el Worker tiene un tope.
 */
export async function listSongKeys(db) {
  const { results } = await db.prepare("SELECT r2_key FROM songs").all();
  return new Set((results || []).map((r) => r.r2_key));
}

/**
 * Inserta varias canciones en un solo lote. `db.batch` viaja como una unica
 * subpeticion y ademas es atomico: o entran todas o no entra ninguna.
 * No devuelve las filas: quien migra no las necesita.
 */
export async function insertSongs(db, songs) {
  if (!songs.length) return 0;
  const now = Date.now();
  const stmt = db.prepare(
    `INSERT INTO songs (id, owner_id, r2_key, title, artist, genre, capo, source_url,
                        locked, visibility, created_at, updated_at, deleted_at,
                        favorite, playlist_id, rev)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, 1)`
  );
  await db.batch(songs.map((song) => stmt.bind(
    song.id || uuid(), song.owner_id, song.r2_key, song.title || "", song.artist || "",
    song.genre || "", song.capo || 0, song.source_url || "", song.locked ? 1 : 0,
    song.visibility || "private", song.created_at || now, now,
    song.favorite ? 1 : 0, song.playlist_id || null
  )));
  return songs.length;
}

export async function insertSong(db, song) {
  const now = Date.now();
  const id = song.id || uuid();
  await db.prepare(
    `INSERT INTO songs (id, owner_id, r2_key, title, artist, genre, capo, source_url,
                        locked, visibility, created_at, updated_at, deleted_at, youtube_url,
                        favorite, position, playlist_id, rev)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, 1)`
  ).bind(
    id, song.owner_id, song.r2_key, song.title || "", song.artist || "", song.genre || "",
    song.capo || 0, song.source_url || "", song.locked ? 1 : 0,
    song.visibility || "private", song.created_at || now, now, song.youtube_url || "",
    song.favorite ? 1 : 0, song.position || 0, song.playlist_id || null
  ).run();
  return findSongById(db, id);
}

/**
 * Sentencia de actualización de metadatos, sin ejecutar. Se expone aparte para
 * poder meterla en un `db.batch()` junto con otra escritura y que las dos vayan
 * o no vayan (aprobar una propuesta son dos cambios que no pueden quedar a
 * medias).
 */
export function stmtUpdateSongMeta(db, id, meta) {
  return db.prepare(
    `UPDATE songs SET title = ?, artist = ?, genre = ?, capo = ?, source_url = ?,
                      locked = ?, visibility = ?, youtube_url = ?,
                      favorite = ?, position = ?, playlist_id = ?,
                      rev = rev + 1, updated_at = ?
     WHERE id = ?`
  ).bind(
    meta.title || "", meta.artist || "", meta.genre || "", meta.capo || 0,
    meta.source_url || "", meta.locked ? 1 : 0, meta.visibility || "private",
    meta.youtube_url || "", meta.favorite ? 1 : 0, meta.position || 0,
    meta.playlist_id || null, Date.now(), id
  );
}

export async function updateSongMeta(db, id, meta) {
  await stmtUpdateSongMeta(db, id, meta).run();
  return findSongById(db, id);
}

/**
 * Borrado lógico (papelera); el objeto de R2 se conserva.
 * Sube `rev` y `updated_at` a propósito: así la lápida sale en el feed de
 * cambios como una modificación más y los otros dispositivos se enteran.
 */
export async function softDeleteSong(db, id) {
  const now = Date.now();
  await db.prepare("UPDATE songs SET deleted_at = ?, updated_at = ?, rev = rev + 1 WHERE id = ?")
    .bind(now, now, id).run();
}

/** Saca una partitura de la papelera. */
export async function restoreSong(db, id) {
  const now = Date.now();
  await db.prepare("UPDATE songs SET deleted_at = 0, updated_at = ?, rev = rev + 1 WHERE id = ?")
    .bind(now, id).run();
  return findSongById(db, id);
}

/** Papelera del usuario, lo borrado primero. */
export async function listTrashedSongs(db, ownerId, pagina = {}) {
  const { limit, offset } = clampPage(pagina);
  const condiciones = ["owner_id = ?", "deleted_at > 0"];
  const valores = [ownerId];
  filtroBusqueda(pagina.q, condiciones, valores);
  const { results } = await db.prepare(
    `SELECT * FROM songs WHERE ${condiciones.join(" AND ")}
     ORDER BY deleted_at DESC LIMIT ? OFFSET ?`
  ).bind(...valores, limit + 1, offset).all();
  return paginar(results || [], limit);
}

/** Borrado definitivo: la fila desaparece. El objeto de R2 lo borra quien llama. */
export async function hardDeleteSong(db, id) {
  await db.prepare("DELETE FROM songs WHERE id = ?").bind(id).run();
}

/** Marca/desmarca favorito sin tocar el resto de la ficha. */
export async function setSongFavorite(db, id, favorite) {
  const now = Date.now();
  await db.prepare(
    "UPDATE songs SET favorite = ?, updated_at = ?, rev = rev + 1 WHERE id = ?"
  ).bind(favorite ? 1 : 0, now, id).run();
  return findSongById(db, id);
}

/**
 * Ficha que se envía al cliente. La clave interna de R2 solo se incluye para
 * quien puede editar la partitura ([includeKey]): la app Android la usa para
 * reconocer, al migrar del token compartido al login, las partituras que ya
 * tenía sincronizadas.
 */
export function publicSong(song, includeKey = false) {
  if (!song) return null;
  return {
    id: song.id,
    r2Key: includeKey ? song.r2_key : undefined,
    ownerId: song.owner_id,
    ownerName: song.owner_name || undefined,
    title: song.title,
    artist: song.artist,
    genre: song.genre,
    capo: song.capo,
    sourceUrl: song.source_url,
    youtubeUrl: song.youtube_url || "",
    locked: !!song.locked,
    visibility: song.visibility,
    favorite: !!song.favorite,
    position: song.position || 0,
    playlistId: song.playlist_id || null,
    // `rev` lo lleva el servidor: el cliente lo devuelve como `baseRev` al
    // subir y así se detecta que alguien más tocó la partitura mientras tanto.
    rev: song.rev || 1,
    // Se expone `deletedAt` para que el feed de cambios pueda mandar lápidas:
    // sin esto, un borrado en el servidor era invisible para la app.
    deletedAt: song.deleted_at || 0,
    createdAt: song.created_at,
    updatedAt: song.updated_at
  };
}

/* ---------------------------- versiones ---------------------------- */

export function publicVersion(version) {
  if (!version) return null;
  return {
    id: version.id,
    songId: version.song_id,
    name: version.name,
    capo: version.capo,
    sourceUrl: version.source_url,
    position: version.position,
    authorId: version.author_id,
    authorName: version.author_name || undefined,
    rev: version.rev || 1,
    deletedAt: version.deleted_at || 0,
    createdAt: version.created_at,
    updatedAt: version.updated_at
  };
}

export async function listVersions(db, songId) {
  const { results } = await db.prepare(
    `SELECT v.*, u.name AS author_name FROM song_versions v
     LEFT JOIN users u ON u.id = v.author_id
     WHERE v.song_id = ? AND v.deleted_at = 0
     ORDER BY v.position ASC, v.created_at ASC`
  ).bind(songId).all();
  return results || [];
}

export async function findVersionById(db, id) {
  return db.prepare("SELECT * FROM song_versions WHERE id = ? AND deleted_at = 0").bind(id).first();
}

/** Sentencia de alta de versión con la posición ya resuelta (ver stmtUpdateSongMeta). */
export function stmtInsertVersion(db, version, id, position) {
  const now = Date.now();
  return db.prepare(
    `INSERT INTO song_versions (id, song_id, name, r2_key, capo, source_url, position,
                                author_id, created_at, updated_at, deleted_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)`
  ).bind(
    id, version.song_id, version.name || "", version.r2_key, version.capo || 0,
    version.source_url || "", position, version.author_id, now, now
  );
}

export async function insertVersion(db, version) {
  const id = version.id || uuid();
  // La posición por defecto manda la nueva al final, que es donde se espera.
  const siguiente = version.position != null ? version.position : await nextVersionPosition(db, version.song_id);
  await stmtInsertVersion(db, version, id, siguiente).run();
  return findVersionById(db, id);
}

async function nextVersionPosition(db, songId) {
  const fila = await db.prepare(
    "SELECT MAX(position) AS maxima FROM song_versions WHERE song_id = ? AND deleted_at = 0"
  ).bind(songId).first();
  return ((fila && fila.maxima) || 0) + 1;
}

export async function updateVersionMeta(db, id, meta) {
  await db.prepare(
    `UPDATE song_versions SET name = ?, capo = ?, source_url = ?, position = ?,
                              rev = rev + 1, updated_at = ?
     WHERE id = ?`
  ).bind(
    meta.name || "", meta.capo || 0, meta.source_url || "", meta.position || 0, Date.now(), id
  ).run();
  return findVersionById(db, id);
}

/**
 * Borrado lógico, igual que las partituras: nada desaparece de golpe.
 * Sube `rev` para que la lápida salga en el feed de cambios.
 */
export async function softDeleteVersion(db, id) {
  const now = Date.now();
  await db.prepare(
    "UPDATE song_versions SET deleted_at = ?, updated_at = ?, rev = rev + 1 WHERE id = ?"
  ).bind(now, now, id).run();
}

/** Versión por id sin filtrar la papelera (el sync necesita ver las lápidas). */
export async function findVersionAnyState(db, id) {
  return db.prepare("SELECT * FROM song_versions WHERE id = ?").bind(id).first();
}

/* ------------------------------ listas ------------------------------ */

export async function findPlaylistById(db, id) {
  return db.prepare("SELECT * FROM playlists WHERE id = ?").bind(id).first();
}

export async function listPlaylists(db, ownerId) {
  const { results } = await db.prepare(
    `SELECT * FROM playlists WHERE owner_id = ? AND deleted_at = 0
     ORDER BY position ASC, name COLLATE NOCASE ASC`
  ).bind(ownerId).all();
  return results || [];
}

/** Busca por nombre entre las vivas: el backfill casa `#playlist:` con esto. */
export async function findPlaylistByName(db, ownerId, name) {
  return db.prepare(
    "SELECT * FROM playlists WHERE owner_id = ? AND name = ? AND deleted_at = 0"
  ).bind(ownerId, name).first();
}

export async function insertPlaylist(db, playlist) {
  const now = Date.now();
  const id = playlist.id || uuid();
  await db.prepare(
    `INSERT INTO playlists (id, owner_id, name, position, created_at, updated_at, deleted_at)
     VALUES (?, ?, ?, ?, ?, ?, 0)`
  ).bind(
    id, playlist.owner_id, playlist.name || "", playlist.position || 0,
    playlist.created_at || now, now
  ).run();
  return findPlaylistById(db, id);
}

export async function updatePlaylist(db, id, { name, position }) {
  await db.prepare(
    "UPDATE playlists SET name = ?, position = ?, updated_at = ? WHERE id = ?"
  ).bind(name || "", position || 0, Date.now(), id).run();
  return findPlaylistById(db, id);
}

/**
 * Borra la lista, no sus partituras: pasan a "Sin lista".
 * Es la misma regla que en la app (borrar una carpeta nunca borra canciones), y
 * va en un solo batch para que no quede a medias.
 */
export async function softDeletePlaylist(db, id) {
  const now = Date.now();
  await db.batch([
    db.prepare("UPDATE playlists SET deleted_at = ?, updated_at = ? WHERE id = ?")
      .bind(now, now, id),
    db.prepare(
      "UPDATE songs SET playlist_id = NULL, updated_at = ?, rev = rev + 1 WHERE playlist_id = ?"
    ).bind(now, id)
  ]);
}

export function publicPlaylist(playlist) {
  if (!playlist) return null;
  return {
    id: playlist.id,
    name: playlist.name,
    position: playlist.position || 0,
    deletedAt: playlist.deleted_at || 0,
    createdAt: playlist.created_at,
    updatedAt: playlist.updated_at
  };
}

/* --------------------------- feed de cambios --------------------------- */
//
// Las tres consultas siguen el mismo patrón: todo lo que cambió DESPUÉS de
// (updated_at, id), lápidas incluidas, ordenado por esa misma pareja para que
// el cursor sea estable aunque varias filas compartan milisegundo.

export async function changedPlaylists(db, ownerId, since, cursorId, limit) {
  const { results } = await db.prepare(
    `SELECT * FROM playlists
     WHERE owner_id = ? AND (updated_at > ? OR (updated_at = ? AND id > ?))
     ORDER BY updated_at ASC, id ASC LIMIT ?`
  ).bind(ownerId, since, since, cursorId || "", limit).all();
  return results || [];
}

export async function changedSongs(db, ownerId, since, cursorId, limit) {
  const { results } = await db.prepare(
    `SELECT * FROM songs
     WHERE owner_id = ? AND (updated_at > ? OR (updated_at = ? AND id > ?))
     ORDER BY updated_at ASC, id ASC LIMIT ?`
  ).bind(ownerId, since, since, cursorId || "", limit).all();
  return results || [];
}

/** Versiones de las partituras del usuario (el JOIN limita a lo suyo). */
export async function changedVersions(db, ownerId, since, cursorId, limit) {
  const { results } = await db.prepare(
    `SELECT v.* FROM song_versions v
     JOIN songs s ON s.id = v.song_id
     WHERE s.owner_id = ? AND (v.updated_at > ? OR (v.updated_at = ? AND v.id > ?))
     ORDER BY v.updated_at ASC, v.id ASC LIMIT ?`
  ).bind(ownerId, since, since, cursorId || "", limit).all();
  return results || [];
}

/* ---------------------------- propuestas ---------------------------- */

export function publicProposal(proposal) {
  if (!proposal) return null;
  return {
    id: proposal.id,
    kind: proposal.kind,
    status: proposal.status,
    songId: proposal.song_id,
    songTitle: proposal.song_title || undefined,
    songArtist: proposal.song_artist || undefined,
    authorId: proposal.author_id,
    authorName: proposal.author_name || undefined,
    name: proposal.name,
    capo: proposal.capo,
    sourceUrl: proposal.source_url,
    note: proposal.note,
    reviewerId: proposal.reviewer_id || undefined,
    reviewNote: proposal.review_note,
    createdAt: proposal.created_at,
    resolvedAt: proposal.resolved_at
  };
}

export async function insertProposal(db, proposal) {
  const now = Date.now();
  const id = proposal.id || uuid();
  await db.prepare(
    `INSERT INTO proposals (id, kind, status, song_id, author_id, name, capo, source_url,
                            r2_key, note, review_note, created_at, resolved_at)
     VALUES (?, ?, 'pending', ?, ?, ?, ?, ?, ?, ?, '', ?, 0)`
  ).bind(
    id, proposal.kind, proposal.song_id, proposal.author_id, proposal.name || "",
    proposal.capo || 0, proposal.source_url || "", proposal.r2_key || "",
    proposal.note || "", now
  ).run();
  return findProposalById(db, id);
}

export async function findProposalById(db, id) {
  return db.prepare(
    `SELECT p.*, u.name AS author_name, s.title AS song_title, s.artist AS song_artist
     FROM proposals p
     LEFT JOIN users u ON u.id = p.author_id
     LEFT JOIN songs s ON s.id = p.song_id
     WHERE p.id = ?`
  ).bind(id).first();
}

/** Cola de revisión, o el historial de quien la envió. */
export async function listProposals(db, { status = "pending", authorId = null } = {}) {
  const condiciones = [];
  const valores = [];
  if (status && status !== "all") { condiciones.push("p.status = ?"); valores.push(status); }
  if (authorId) { condiciones.push("p.author_id = ?"); valores.push(authorId); }
  const where = condiciones.length ? "WHERE " + condiciones.join(" AND ") : "";
  const sql = `SELECT p.*, u.name AS author_name, s.title AS song_title, s.artist AS song_artist
     FROM proposals p
     LEFT JOIN users u ON u.id = p.author_id
     LEFT JOIN songs s ON s.id = p.song_id
     ${where} ORDER BY p.created_at DESC`;
  const stmt = valores.length ? db.prepare(sql).bind(...valores) : db.prepare(sql);
  const { results } = await stmt.all();
  return results || [];
}

export function stmtResolveProposal(db, id, { status, reviewerId, reviewNote }) {
  return db.prepare(
    "UPDATE proposals SET status = ?, reviewer_id = ?, review_note = ?, resolved_at = ? WHERE id = ?"
  ).bind(status, reviewerId || null, reviewNote || "", Date.now(), id);
}

export async function resolveProposal(db, id, { status, reviewerId, reviewNote }) {
  await stmtResolveProposal(db, id, { status, reviewerId, reviewNote }).run();
  return findProposalById(db, id);
}

/** La siguiente posición libre de una versión (pública para el batch de aprobar). */
export async function nextVersionPositionFor(db, songId) {
  return nextVersionPosition(db, songId);
}

/** Cuántas propuestas esperan revisión: el aviso del equipo editorial. */
export async function countPendingProposals(db) {
  const fila = await db.prepare("SELECT COUNT(*) AS total FROM proposals WHERE status = 'pending'").first();
  return (fila && fila.total) || 0;
}

/* ------------------------------ usuarios ------------------------------ */

export async function listUsers(db, pagina = {}) {
  const { limit, offset } = clampPage(pagina);
  const { results } = await db.prepare(
    "SELECT id, email, name, role, created_at FROM users ORDER BY created_at ASC LIMIT ? OFFSET ?"
  ).bind(limit + 1, offset).all();
  return paginar(results || [], limit);
}

export async function updateUserRole(db, id, role) {
  await db.prepare("UPDATE users SET role = ? WHERE id = ?").bind(role, id).run();
  return findUserById(db, id);
}

/* --------------------------- comentarios --------------------------- */

export function publicComment(comment) {
  if (!comment) return null;
  return {
    id: comment.id,
    songId: comment.song_id,
    authorId: comment.author_id,
    authorName: comment.author_name || undefined,
    body: comment.body,
    createdAt: comment.created_at
  };
}

export async function listComments(db, songId, pagina = {}) {
  const { limit, offset } = clampPage(pagina);
  const { results } = await db.prepare(
    `SELECT c.*, u.name AS author_name FROM comments c
     LEFT JOIN users u ON u.id = c.author_id
     WHERE c.song_id = ? AND c.deleted_at = 0
     ORDER BY c.created_at ASC LIMIT ? OFFSET ?`
  ).bind(songId, limit + 1, offset).all();
  return paginar(results || [], limit);
}

export async function findCommentById(db, id) {
  return db.prepare(
    `SELECT c.*, u.name AS author_name FROM comments c
     LEFT JOIN users u ON u.id = c.author_id
     WHERE c.id = ? AND c.deleted_at = 0`
  ).bind(id).first();
}

export async function insertComment(db, { song_id, author_id, body }) {
  const id = uuid();
  await db.prepare(
    "INSERT INTO comments (id, song_id, author_id, body, created_at, deleted_at) VALUES (?, ?, ?, ?, ?, 0)"
  ).bind(id, song_id, author_id, body, Date.now()).run();
  return findCommentById(db, id);
}

export async function softDeleteComment(db, id) {
  await db.prepare("UPDATE comments SET deleted_at = ? WHERE id = ?").bind(Date.now(), id).run();
}

/* -------------------------- valoraciones -------------------------- */

/**
 * Medias por versión. La media se redondea a un decimal aquí y no en el
 * cliente, para que todos los sitios que la enseñan digan lo mismo.
 */
export async function listRatings(db, songId) {
  const { results } = await db.prepare(
    `SELECT version_id, COUNT(*) AS total, AVG(stars) AS media
     FROM ratings WHERE song_id = ? GROUP BY version_id`
  ).bind(songId).all();
  const salida = {};
  for (const fila of results || []) {
    salida[fila.version_id] = {
      count: fila.total,
      average: Math.round((fila.media || 0) * 10) / 10
    };
  }
  return salida;
}

/** Lo que ha votado una persona, para poder pintar sus estrellas marcadas. */
export async function listUserRatings(db, songId, userId) {
  const { results } = await db.prepare(
    "SELECT version_id, stars FROM ratings WHERE song_id = ? AND user_id = ?"
  ).bind(songId, userId).all();
  const salida = {};
  for (const fila of results || []) salida[fila.version_id] = fila.stars;
  return salida;
}

/** Votar dos veces la misma versión cambia el voto, no lo duplica. */
export async function setRating(db, { songId, versionId, userId, stars }) {
  await db.prepare(
    `INSERT INTO ratings (song_id, version_id, user_id, stars, updated_at)
     VALUES (?, ?, ?, ?, ?)
     ON CONFLICT(song_id, version_id, user_id)
     DO UPDATE SET stars = excluded.stars, updated_at = excluded.updated_at`
  ).bind(songId, versionId, userId, stars, Date.now()).run();
}

export async function deleteRating(db, { songId, versionId, userId }) {
  await db.prepare(
    "DELETE FROM ratings WHERE song_id = ? AND version_id = ? AND user_id = ?"
  ).bind(songId, versionId, userId).run();
}

/**
 * Partituras vivas a partir de un id, en orden. Se pagina por id y no por
 * OFFSET porque al ir escribiendo el género las filas se moverían de sitio.
 */
export async function listSongsAfter(db, cursor = "", limit = 200) {
  const { results } = await db.prepare(
    `SELECT id, title, artist, genre FROM songs
     WHERE deleted_at = 0 AND id > ? ORDER BY id ASC LIMIT ?`
  ).bind(cursor || "", limit).all();
  return results || [];
}

/** Asigna categorías en un solo lote: una subpetición para todas. */
export async function setGenres(db, parejas) {
  if (!parejas.length) return 0;
  // `rev` también sube: si no, el cambio de categoría llegaría al feed pero el
  // cliente lo daría por "ya lo tengo" y no lo aplicaría nunca.
  const stmt = db.prepare("UPDATE songs SET genre = ?, updated_at = ?, rev = rev + 1 WHERE id = ?");
  const ahora = Date.now();
  await db.batch(parejas.map((p) => stmt.bind(p.genre, ahora, p.id)));
  return parejas.length;
}

/** Partituras sin vídeo asociado, para poder ir completándolas. */
export async function listSongsWithoutVideo(db, limit = 500) {
  const { results } = await db.prepare(
    `SELECT id, title, artist, genre, visibility FROM songs
     WHERE deleted_at = 0 AND (youtube_url IS NULL OR youtube_url = '')
     ORDER BY artist COLLATE NOCASE ASC, title COLLATE NOCASE ASC LIMIT ?`
  ).bind(limit).all();
  return results || [];
}

/**
 * Coloca una partitura en su carpeta y/o la marca favorita, sin tocar el resto.
 * Lo usa el backfill: las partituras indexadas antes de que existieran estas
 * columnas llevaban el dato escondido en el texto (`#playlist:`, `#favorite:`).
 */
export async function setSongPlacement(db, id, { favorite, playlist_id }) {
  const now = Date.now();
  await db.prepare(
    "UPDATE songs SET favorite = ?, playlist_id = ?, updated_at = ?, rev = rev + 1 WHERE id = ?"
  ).bind(favorite ? 1 : 0, playlist_id || null, now, id).run();
}
