/*
 * Vivace · reglas de acceso (lógica pura, testeable)
 *
 * Visibilidad de una partitura:
 *  - `private`: solo la ve y edita su dueño.
 *  - `public`:  está en el catálogo y la ve cualquiera, incluso sin sesión.
 *
 * Roles:
 *  - `user`:    crea partituras para él. No publica por su cuenta: propone, y
 *               alguien con permiso lo aprueba. También puede proponer
 *               versiones de las partituras que ya están publicadas.
 *  - `editor`:  cuida el catálogo. Edita y despublica partituras públicas,
 *               resuelve propuestas y mantiene el diccionario de acordes. No
 *               toca las partituras privadas de nadie ni reparte roles.
 *  - `admin`:   todo lo anterior, más los roles de los demás.
 *
 * La idea de fondo: publicar es un acto editorial, no una casilla que marca
 * quien sube el fichero. Por eso `visibility` no la cambia el dueño.
 */

export const VISIBILITY = Object.freeze({ PRIVATE: "private", PUBLIC: "public" });
export const ROLES = Object.freeze({ USER: "user", EDITOR: "editor", ADMIN: "admin" });

export function isValidVisibility(v) {
  return v === VISIBILITY.PRIVATE || v === VISIBILITY.PUBLIC;
}

export function isValidRole(r) {
  return r === ROLES.USER || r === ROLES.EDITOR || r === ROLES.ADMIN;
}

export const isAdmin = (user) => !!user && user.role === ROLES.ADMIN;

/** Editor o admin: quien tiene responsabilidad sobre el catálogo. */
export const isEditor = (user) => !!user && (user.role === ROLES.EDITOR || isAdmin(user));

const isOwner = (user, song) => !!user && !!song && song.owner_id === user.id;

const isPublic = (song) => !!song && song.visibility === VISIBILITY.PUBLIC;

/** ¿Puede [user] (o un visitante, si es null) ver esta partitura? */
export function canView(user, song) {
  if (!song || song.deleted_at) return false;
  if (isPublic(song)) return true;
  return isOwner(user, song) || isAdmin(user);
}

/**
 * ¿Puede modificarla o borrarla?
 *
 * El editor manda sobre lo publicado, no sobre el cajón de cada uno: una
 * partitura privada ajena sigue siendo intocable para él. El admin sí llega a
 * todo, porque es quien tiene que poder arreglar desaguisados.
 */
export function canEdit(user, song) {
  if (!song || song.deleted_at) return false;
  if (isAdmin(user)) return true;
  if (isOwner(user, song)) return true;
  return isEditor(user) && isPublic(song);
}

/**
 * ¿Puede decidir si una partitura sale en el catálogo? Solo el equipo
 * editorial. El dueño propone (ver `canPropose`), no dispone.
 */
export function canSetVisibility(user) {
  return isEditor(user);
}

/** Resolver propuestas y mantener el diccionario global de acordes. */
export const canReview = (user) => isEditor(user);
export const canEditChords = (user) => isEditor(user);

/** Repartir roles es cosa del administrador. */
export const canManageRoles = (user) => isAdmin(user);

/**
 * Motivo por el que se rechaza una edición, o null si se permite.
 * El candado (`locked`) no bloquea en el servidor: la app y la web piden
 * confirmación al usuario. Aquí solo se comprueban propiedad y rol.
 */
export function editDenialReason(user, song) {
  if (!song || song.deleted_at) return "not_found";
  if (!user) return "unauthorized";
  if (!canEdit(user, song)) return "forbidden";
  return null;
}

/**
 * ¿Puede [user] proponer algo sobre esta partitura?
 *
 *  - `publish`: pedir que su propia partitura privada entre en el catálogo.
 *    Solo el dueño, y solo si aún no está publicada.
 *  - `version`: ofrecer un arreglo alternativo de algo ya publicado. Cualquiera
 *    con sesión, porque de eso va tener un catálogo común.
 */
export function canPropose(user, song, kind) {
  if (!user || !song || song.deleted_at) return false;
  if (kind === "publish") return isOwner(user, song) && !isPublic(song);
  if (kind === "version") return isPublic(song);
  return false;
}

/**
 * Quién puede añadir versiones directamente, sin pasar por revisión: el dueño
 * de la partitura y el equipo editorial sobre lo público. El resto propone.
 */
export function canAddVersion(user, song) {
  return canEdit(user, song);
}

/**
 * Comentar y valorar: cualquiera con sesión sobre lo que pueda ver. No se pide
 * más que eso a propósito — si alguien puede leer la partitura, puede opinar.
 */
export function canComment(user, song) {
  return !!user && canView(user, song);
}
export function canRate(user, song) {
  return !!user && canView(user, song);
}

/** Retirar un comentario: quien lo escribió, o el equipo editorial. */
export function canDeleteComment(user, comment) {
  if (!user || !comment || comment.deleted_at) return false;
  return comment.author_id === user.id || isEditor(user);
}

/**
 * El candado no bloquea en el servidor: es un seguro contra el despiste, no un
 * permiso. Quien puede editar puede ponerlo y quitarlo; la interfaz avisa antes
 * de dejar tocar una partitura bloqueada.
 */
export function canSetLocked(user, song) {
  return canEdit(user, song);
}

/** Retirar una propuesta: su autor mientras siga pendiente, o un editor. */
export function canWithdrawProposal(user, proposal) {
  if (!user || !proposal) return false;
  if (proposal.status !== "pending") return false;
  return proposal.author_id === user.id || isEditor(user);
}
