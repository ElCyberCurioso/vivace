/*
 * D1 de mentira con estado, para probar la sincronización de punta a punta.
 *
 * Los otros tests usan fakes que solo reconocen la consulta y devuelven algo
 * fijo; eso vale para comprobar permisos, pero no para el sync, donde lo que
 * importa es que `rev` suba, que la lápida quede escrita y que el cursor avance.
 * Reconoce por un trozo del SQL y aplica el efecto sobre arrays en memoria.
 */

export function fakeD1(inicial = {}) {
  const t = {
    users: inicial.users || [],
    songs: inicial.songs || [],
    playlists: inicial.playlists || [],
    song_versions: inicial.song_versions || [],
    proposals: inicial.proposals || [],
    auth_attempts: inicial.auth_attempts || []
  };

  const norm = (sql) => sql.replace(/\s+/g, " ").trim();

  function ejecutar(sql, v) {
    const q = norm(sql);

    // ---- lecturas de una fila ----
    if (q.startsWith("SELECT * FROM users WHERE id")) return t.users.find((u) => u.id === v[0]) || null;
    if (q.startsWith("SELECT * FROM users WHERE email_lower")) return t.users.find((u) => u.email_lower === v[0]) || null;
    if (q.startsWith("SELECT * FROM songs WHERE id")) return t.songs.find((s) => s.id === v[0]) || null;
    if (q.startsWith("SELECT * FROM playlists WHERE id")) return t.playlists.find((p) => p.id === v[0]) || null;
    if (q.startsWith("SELECT * FROM playlists WHERE owner_id = ? AND name")) {
      return t.playlists.find((p) => p.owner_id === v[0] && p.name === v[1] && !p.deleted_at) || null;
    }
    if (q.startsWith("SELECT * FROM song_versions WHERE id = ? AND deleted_at = 0")) {
      return t.song_versions.find((x) => x.id === v[0] && !x.deleted_at) || null;
    }
    if (q.startsWith("SELECT * FROM song_versions WHERE id")) {
      return t.song_versions.find((x) => x.id === v[0]) || null;
    }
    if (q.includes("FROM proposals p")) {
      const p = t.proposals.find((x) => x.id === v[0]);
      if (!p) return null;
      const song = t.songs.find((x) => x.id === p.song_id) || {};
      const autor = t.users.find((x) => x.id === p.author_id) || {};
      return { ...p, author_name: autor.name || "", song_title: song.title || "", song_artist: song.artist || "" };
    }
    if (q.includes("MAX(position) AS maxima")) {
      const suyas = t.song_versions.filter((x) => x.song_id === v[0] && !x.deleted_at);
      return { maxima: suyas.reduce((m, x) => Math.max(m, x.position || 0), 0) };
    }
    if (q.includes("COUNT(*) AS n FROM users")) return { n: t.users.length };
    return null;
  }

  function ejecutarTodos(sql, v) {
    const q = norm(sql);
    const despues = (filas, since, id) =>
      filas.filter((f) => f.updated_at > since || (f.updated_at === since && String(f.id) > String(id)))
        .sort((a, b) => (a.updated_at - b.updated_at) || String(a.id).localeCompare(String(b.id)));

    if (q.startsWith("SELECT * FROM playlists WHERE owner_id = ? AND (updated_at")) {
      return despues(t.playlists.filter((p) => p.owner_id === v[0]), v[1], v[3]).slice(0, v[4]);
    }
    if (q.startsWith("SELECT * FROM songs WHERE owner_id = ? AND (updated_at")) {
      return despues(t.songs.filter((s) => s.owner_id === v[0]), v[1], v[3]).slice(0, v[4]);
    }
    if (q.includes("FROM song_versions v JOIN songs s")) {
      const mias = new Set(t.songs.filter((s) => s.owner_id === v[0]).map((s) => s.id));
      return despues(t.song_versions.filter((x) => mias.has(x.song_id)), v[1], v[3]).slice(0, v[4]);
    }
    // LIMIT/OFFSET se respetan de verdad: si no, un fake "generoso" dejaría
    // pasar un fallo de paginación sin que ningún test se enterase.
    const pagina = (filas) => {
      const limit = Number(v[v.length - 2]);
      const offset = Number(v[v.length - 1]);
      if (!Number.isFinite(limit)) return filas;
      return filas.slice(offset || 0, (offset || 0) + limit);
    };
    // La migración pregunta por las claves ya indexadas para saltárselas: sin
    // esto el fake devolvía vacío y hacía creer que reimportaba cada vez.
    if (q.startsWith("SELECT r2_key FROM songs")) {
      return t.songs.map((s) => ({ r2_key: s.r2_key }));
    }
    if (q.startsWith("SELECT * FROM playlists WHERE owner_id = ? AND deleted_at = 0")) {
      return t.playlists.filter((p) => p.owner_id === v[0] && !p.deleted_at);
    }
    if (q.startsWith("SELECT * FROM songs WHERE owner_id = ? AND deleted_at = 0")) {
      return pagina(t.songs.filter((s) => s.owner_id === v[0] && !s.deleted_at)
        .sort((a, b) => String(a.title).localeCompare(String(b.title))));
    }
    if (q.startsWith("SELECT * FROM songs WHERE owner_id = ? AND deleted_at > 0")) {
      return pagina(t.songs.filter((s) => s.owner_id === v[0] && s.deleted_at > 0));
    }
    if (q.includes("FROM song_versions v LEFT JOIN users")) {
      return t.song_versions.filter((x) => x.song_id === v[0] && !x.deleted_at);
    }
    return [];
  }

  function escribir(sql, v) {
    const q = norm(sql);

    if (q.startsWith("INSERT INTO playlists")) {
      t.playlists.push({
        id: v[0], owner_id: v[1], name: v[2], position: v[3],
        created_at: v[4], updated_at: v[5], deleted_at: 0
      });
      return;
    }
    if (q.startsWith("UPDATE playlists SET name")) {
      const p = t.playlists.find((x) => x.id === v[3]);
      if (p) Object.assign(p, { name: v[0], position: v[1], updated_at: v[2] });
      return;
    }
    if (q.startsWith("UPDATE playlists SET deleted_at")) {
      const p = t.playlists.find((x) => x.id === v[2]);
      if (p) Object.assign(p, { deleted_at: v[0], updated_at: v[1] });
      return;
    }
    if (q.startsWith("UPDATE songs SET playlist_id = NULL")) {
      t.songs.filter((s) => s.playlist_id === v[1])
        .forEach((s) => Object.assign(s, { playlist_id: null, updated_at: v[0], rev: (s.rev || 1) + 1 }));
      return;
    }
    if (q.startsWith("INSERT INTO songs")) {
      // Hay DOS altas de partitura con columnas distintas: la de una en una
      // (insertSong, con youtube_url y position) y la masiva de la migración
      // (insertSongs, sin ellas). Mapear las dos con el mismo orden posicional
      // metía el playlist_id en la columna del favorito.
      const masiva = !q.includes("youtube_url");
      t.songs.push(masiva ? {
        id: v[0], owner_id: v[1], r2_key: v[2], title: v[3], artist: v[4], genre: v[5],
        capo: v[6], source_url: v[7], locked: v[8], visibility: v[9],
        created_at: v[10], updated_at: v[11], deleted_at: 0, youtube_url: "",
        favorite: v[12], position: 0, playlist_id: v[13], rev: 1
      } : {
        id: v[0], owner_id: v[1], r2_key: v[2], title: v[3], artist: v[4], genre: v[5],
        capo: v[6], source_url: v[7], locked: v[8], visibility: v[9],
        created_at: v[10], updated_at: v[11], deleted_at: 0, youtube_url: v[12],
        favorite: v[13], position: v[14], playlist_id: v[15], rev: 1
      });
      return;
    }
    if (q.startsWith("UPDATE songs SET title")) {
      const s = t.songs.find((x) => x.id === v[12]);
      if (s) Object.assign(s, {
        title: v[0], artist: v[1], genre: v[2], capo: v[3], source_url: v[4],
        locked: v[5], visibility: v[6], youtube_url: v[7], favorite: v[8],
        position: v[9], playlist_id: v[10], updated_at: v[11], rev: (s.rev || 1) + 1
      });
      return;
    }
    if (q.startsWith("UPDATE songs SET deleted_at = ?, updated_at = ?, rev")) {
      const s = t.songs.find((x) => x.id === v[2]);
      if (s) Object.assign(s, { deleted_at: v[0], updated_at: v[1], rev: (s.rev || 1) + 1 });
      return;
    }
    if (q.startsWith("UPDATE songs SET deleted_at = 0")) {
      const s = t.songs.find((x) => x.id === v[1]);
      if (s) Object.assign(s, { deleted_at: 0, updated_at: v[0], rev: (s.rev || 1) + 1 });
      return;
    }
    if (q.startsWith("UPDATE songs SET favorite")) {
      const s = t.songs.find((x) => x.id === v[2]);
      if (s) Object.assign(s, { favorite: v[0], updated_at: v[1], rev: (s.rev || 1) + 1 });
      return;
    }
    if (q.startsWith("DELETE FROM songs WHERE id")) {
      t.songs = t.songs.filter((s) => s.id !== v[0]);
      return;
    }
    if (q.startsWith("INSERT INTO song_versions")) {
      t.song_versions.push({
        id: v[0], song_id: v[1], name: v[2], r2_key: v[3], capo: v[4],
        source_url: v[5], position: v[6], author_id: v[7],
        created_at: v[8], updated_at: v[9], deleted_at: 0, rev: 1
      });
      return;
    }
    if (q.startsWith("UPDATE song_versions SET name")) {
      const x = t.song_versions.find((y) => y.id === v[5]);
      if (x) Object.assign(x, {
        name: v[0], capo: v[1], source_url: v[2], position: v[3],
        updated_at: v[4], rev: (x.rev || 1) + 1
      });
      return;
    }
    if (q.startsWith("UPDATE song_versions SET deleted_at")) {
      const x = t.song_versions.find((y) => y.id === v[2]);
      if (x) Object.assign(x, { deleted_at: v[0], updated_at: v[1], rev: (x.rev || 1) + 1 });
      return;
    }
    if (q.startsWith("INSERT INTO proposals")) {
      t.proposals.push({
        id: v[0], kind: v[1], status: "pending", song_id: v[2], author_id: v[3],
        name: v[4], capo: v[5], source_url: v[6], r2_key: v[7], note: v[8],
        review_note: "", reviewer_id: null, created_at: v[9], resolved_at: 0
      });
      return;
    }
    if (q.startsWith("UPDATE proposals SET status")) {
      const p = t.proposals.find((x) => x.id === v[4]);
      if (p) Object.assign(p, { status: v[0], reviewer_id: v[1], review_note: v[2], resolved_at: v[3] });
      return;
    }
    if (q.startsWith("INSERT INTO users")) {
      t.users.push({
        id: v[0], email: v[1], email_lower: v[2], name: v[3],
        password_hash: v[4], role: v[5], created_at: v[6]
      });
      return;
    }
    if (q.startsWith("INSERT INTO auth_attempts")) {
      const y = t.auth_attempts.find((a) => a.key === v[0]);
      if (y) Object.assign(y, { count: 1, window_start: v[1] });
      else t.auth_attempts.push({ key: v[0], count: 1, window_start: v[1] });
      return;
    }
    if (q.startsWith("UPDATE auth_attempts SET count")) {
      const y = t.auth_attempts.find((a) => a.key === v[0]);
      if (y) y.count++;
      return;
    }
    if (q.startsWith("DELETE FROM auth_attempts")) {
      t.auth_attempts = t.auth_attempts.filter((a) => a.key !== v[0]);
      return;
    }
    if (q.startsWith("SELECT count, window_start FROM auth_attempts")) return;
    throw new Error("SQL no contemplado por el fake: " + q.slice(0, 70));
  }

  const db = {
    tablas: t,
    prepare(sql) {
      const hacer = (v) => ({
        first: async () => {
          if (norm(sql).startsWith("SELECT count, window_start FROM auth_attempts")) {
            return t.auth_attempts.find((a) => a.key === v[0]) || null;
          }
          return ejecutar(sql, v);
        },
        all: async () => ({ results: ejecutarTodos(sql, v) }),
        run: async () => { escribir(sql, v); return {}; }
      });
      return {
        bind: (...v) => hacer(v),
        first: async () => ejecutar(sql, []),
        all: async () => ({ results: ejecutarTodos(sql, []) }),
        run: async () => { escribir(sql, []); return {}; }
      };
    },
    async batch(sentencias) {
      const salida = [];
      for (const s of sentencias) salida.push(await s.run());
      return salida;
    }
  };
  return db;
}

/** Bucket de mentira: un Map de clave -> texto. */
export function fakeBucket(inicial = {}) {
  const objetos = new Map(Object.entries(inicial));
  const meta = new Map();
  return {
    objetos,
    /**
     * Listado con prefijo y cursor, como el de R2. Lo usa la migracion, que va
     * por tandas: sin cursor no se podria probar que retoma donde lo dejo.
     */
    async list({ prefix = "", cursor, limit = 1000 } = {}) {
      const claves = [...objetos.keys()].filter((k) => k.startsWith(prefix)).sort();
      const desde = cursor ? claves.indexOf(cursor) + 1 : 0;
      const tanda = claves.slice(desde, desde + limit);
      const truncated = desde + tanda.length < claves.length;
      return {
        objects: tanda.map((key) => ({
          key,
          size: objetos.get(key).length,
          uploaded: new Date(0),
          customMetadata: meta.get(key) || {}
        })),
        truncated,
        cursor: truncated ? tanda[tanda.length - 1] : undefined
      };
    },
    async get(key) {
      if (!objetos.has(key)) return null;
      const texto = objetos.get(key);
      return { text: async () => texto };
    },
    async put(key, body, opciones) {
      objetos.set(key, String(body));
      if (opciones && opciones.customMetadata) meta.set(key, opciones.customMetadata);
    },
    async delete(key) { objetos.delete(key); },
    async head(key) { return objetos.has(key) ? { customMetadata: {} } : null; }
  };
}
