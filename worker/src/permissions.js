/*
 * Vivace · reglas de acceso a partituras (lógica pura, testeable)
 *
 * Modelo:
 *  - `private`: solo la ve y edita su dueño.
 *  - `public`:  la ve cualquiera (incluso sin sesión); editarla sigue siendo
 *               cosa del dueño. Es lo que permite publicar: el catálogo por
 *               defecto de la web son las partituras públicas del admin.
 *  - `admin`:   puede ver y editar cualquier partitura (es quien administra).
 */

export const VISIBILITY = Object.freeze({ PRIVATE: "private", PUBLIC: "public" });

export function isValidVisibility(v) {
  return v === VISIBILITY.PRIVATE || v === VISIBILITY.PUBLIC;
}

const isAdmin = (user) => !!user && user.role === "admin";
const isOwner = (user, song) => !!user && !!song && song.owner_id === user.id;

/** ¿Puede [user] (o un visitante, si es null) ver esta partitura? */
export function canView(user, song) {
  if (!song || song.deleted_at) return false;
  if (song.visibility === VISIBILITY.PUBLIC) return true;
  return isOwner(user, song) || isAdmin(user);
}

/** ¿Puede modificarla o borrarla? Publicarla es solo del dueño (o admin). */
export function canEdit(user, song) {
  if (!song || song.deleted_at) return false;
  return isOwner(user, song) || isAdmin(user);
}

/**
 * Motivo por el que se rechaza una edición, o null si se permite.
 * El candado (`locked`) no bloquea en el servidor: la app y la web piden
 * confirmación al usuario. Aquí solo se comprueba la propiedad.
 */
export function editDenialReason(user, song) {
  if (!song || song.deleted_at) return "not_found";
  if (!user) return "unauthorized";
  if (!canEdit(user, song)) return "forbidden";
  return null;
}
