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

// ---- partituras ----
export async function findSongById(db, id) {
  return db.prepare("SELECT * FROM songs WHERE id = ?").bind(id).first();
}

export async function findSongByKey(db, r2Key) {
  return db.prepare("SELECT * FROM songs WHERE r2_key = ?").bind(r2Key).first();
}

/** Partituras del usuario (activas), más recientes primero. */
export async function listOwnSongs(db, ownerId) {
  const { results } = await db.prepare(
    `SELECT * FROM songs WHERE owner_id = ? AND deleted_at = 0
     ORDER BY title COLLATE NOCASE ASC`
  ).bind(ownerId).all();
  return results || [];
}

/**
 * Catálogo público. Sin [ownerId] devuelve todas las publicadas; el cliente web
 * lo usa por defecto con el id del admin para mostrar su selección.
 */
export async function listPublicSongs(db, ownerId = null) {
  const sql = ownerId
    ? `SELECT s.*, u.name AS owner_name FROM songs s JOIN users u ON u.id = s.owner_id
       WHERE s.visibility = 'public' AND s.deleted_at = 0 AND s.owner_id = ?
       ORDER BY s.title COLLATE NOCASE ASC`
    : `SELECT s.*, u.name AS owner_name FROM songs s JOIN users u ON u.id = s.owner_id
       WHERE s.visibility = 'public' AND s.deleted_at = 0
       ORDER BY s.title COLLATE NOCASE ASC`;
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
                        locked, visibility, created_at, updated_at, deleted_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)`
  );
  await db.batch(songs.map((song) => stmt.bind(
    song.id || uuid(), song.owner_id, song.r2_key, song.title || "", song.artist || "",
    song.genre || "", song.capo || 0, song.source_url || "", song.locked ? 1 : 0,
    song.visibility || "private", song.created_at || now, now
  )));
  return songs.length;
}

export async function insertSong(db, song) {
  const now = Date.now();
  const id = song.id || uuid();
  await db.prepare(
    `INSERT INTO songs (id, owner_id, r2_key, title, artist, genre, capo, source_url,
                        locked, visibility, created_at, updated_at, deleted_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)`
  ).bind(
    id, song.owner_id, song.r2_key, song.title || "", song.artist || "", song.genre || "",
    song.capo || 0, song.source_url || "", song.locked ? 1 : 0,
    song.visibility || "private", song.created_at || now, now
  ).run();
  return findSongById(db, id);
}

export async function updateSongMeta(db, id, meta) {
  await db.prepare(
    `UPDATE songs SET title = ?, artist = ?, genre = ?, capo = ?, source_url = ?,
                      locked = ?, visibility = ?, updated_at = ?
     WHERE id = ?`
  ).bind(
    meta.title || "", meta.artist || "", meta.genre || "", meta.capo || 0,
    meta.source_url || "", meta.locked ? 1 : 0, meta.visibility || "private",
    Date.now(), id
  ).run();
  return findSongById(db, id);
}

/** Borrado lógico (papelera); el objeto de R2 se conserva. */
export async function softDeleteSong(db, id) {
  await db.prepare("UPDATE songs SET deleted_at = ?, updated_at = ? WHERE id = ?")
    .bind(Date.now(), Date.now(), id).run();
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
    locked: !!song.locked,
    visibility: song.visibility,
    createdAt: song.created_at,
    updatedAt: song.updated_at
  };
}
