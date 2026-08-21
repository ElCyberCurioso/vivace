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
export async function listPublicSongs(db, ownerId = null, { genre = "", sort = "title" } = {}) {
  const orden = ORDENES[sort] || ORDENES.title;
  const condiciones = ["s.visibility = 'public'", "s.deleted_at = 0"];
  const valores = [];
  if (ownerId) { condiciones.push("s.owner_id = ?"); valores.push(ownerId); }
  if (genre) { condiciones.push("LOWER(s.genre) = ?"); valores.push(String(genre).toLowerCase()); }
  const sql = `SELECT s.*, u.name AS owner_name FROM songs s JOIN users u ON u.id = s.owner_id
     WHERE ${condiciones.join(" AND ")} ORDER BY ${orden}`;
  const stmt = valores.length ? db.prepare(sql).bind(...valores) : db.prepare(sql);
  const { results } = await stmt.all();
  return results || [];
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
                        locked, visibility, created_at, updated_at, deleted_at, youtube_url)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)`
  ).bind(
    id, song.owner_id, song.r2_key, song.title || "", song.artist || "", song.genre || "",
    song.capo || 0, song.source_url || "", song.locked ? 1 : 0,
    song.visibility || "private", song.created_at || now, now, song.youtube_url || ""
  ).run();
  return findSongById(db, id);
}

export async function updateSongMeta(db, id, meta) {
  await db.prepare(
    `UPDATE songs SET title = ?, artist = ?, genre = ?, capo = ?, source_url = ?,
                      locked = ?, visibility = ?, youtube_url = ?, updated_at = ?
     WHERE id = ?`
  ).bind(
    meta.title || "", meta.artist || "", meta.genre || "", meta.capo || 0,
    meta.source_url || "", meta.locked ? 1 : 0, meta.visibility || "private",
    meta.youtube_url || "", Date.now(), id
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
    youtubeUrl: song.youtube_url || "",
    locked: !!song.locked,
    visibility: song.visibility,
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

export async function insertVersion(db, version) {
  const now = Date.now();
  const id = version.id || uuid();
  // La posición por defecto manda la nueva al final, que es donde se espera.
  const siguiente = version.position != null ? version.position : await nextVersionPosition(db, version.song_id);
  await db.prepare(
    `INSERT INTO song_versions (id, song_id, name, r2_key, capo, source_url, position,
                                author_id, created_at, updated_at, deleted_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)`
  ).bind(
    id, version.song_id, version.name || "", version.r2_key, version.capo || 0,
    version.source_url || "", siguiente, version.author_id, now, now
  ).run();
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
    `UPDATE song_versions SET name = ?, capo = ?, source_url = ?, position = ?, updated_at = ?
     WHERE id = ?`
  ).bind(
    meta.name || "", meta.capo || 0, meta.source_url || "", meta.position || 0, Date.now(), id
  ).run();
  return findVersionById(db, id);
}

/** Borrado lógico, igual que las partituras: nada desaparece de golpe. */
export async function softDeleteVersion(db, id) {
  await db.prepare("UPDATE song_versions SET deleted_at = ?, updated_at = ? WHERE id = ?")
    .bind(Date.now(), Date.now(), id).run();
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

export async function resolveProposal(db, id, { status, reviewerId, reviewNote }) {
  await db.prepare(
    "UPDATE proposals SET status = ?, reviewer_id = ?, review_note = ?, resolved_at = ? WHERE id = ?"
  ).bind(status, reviewerId || null, reviewNote || "", Date.now(), id).run();
  return findProposalById(db, id);
}

/** Cuántas propuestas esperan revisión: el aviso del equipo editorial. */
export async function countPendingProposals(db) {
  const fila = await db.prepare("SELECT COUNT(*) AS total FROM proposals WHERE status = 'pending'").first();
  return (fila && fila.total) || 0;
}

/* ------------------------------ usuarios ------------------------------ */

export async function listUsers(db) {
  const { results } = await db.prepare(
    "SELECT id, email, name, role, created_at FROM users ORDER BY created_at ASC"
  ).all();
  return results || [];
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

export async function listComments(db, songId) {
  const { results } = await db.prepare(
    `SELECT c.*, u.name AS author_name FROM comments c
     LEFT JOIN users u ON u.id = c.author_id
     WHERE c.song_id = ? AND c.deleted_at = 0
     ORDER BY c.created_at ASC`
  ).bind(songId).all();
  return results || [];
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
  const stmt = db.prepare("UPDATE songs SET genre = ?, updated_at = ? WHERE id = ?");
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
