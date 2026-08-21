/*
 * Vivace · vídeo de YouTube asociado a una partitura.
 *
 * Se guarda la URL tal cual la pega quien edita (que es lo que reconocerá si
 * vuelve a mirarla) y el identificador se extrae al pintar. Se aceptan las
 * formas habituales porque nadie copia siempre la misma:
 *
 *   https://www.youtube.com/watch?v=ID&t=30
 *   https://youtu.be/ID
 *   https://www.youtube.com/shorts/ID
 *   https://www.youtube.com/embed/ID
 *   ID  (pegado a pelo)
 */

/** Un id de YouTube son 11 caracteres de un alfabeto conocido. */
const ID_RE = /^[A-Za-z0-9_-]{11}$/;

/**
 * Identificador del vídeo, o "" si no se reconoce. No lanza: una URL mal
 * pegada deja la partitura sin vídeo, no rompe la página.
 */
export function youtubeId(url) {
  const texto = String(url == null ? "" : url).trim();
  if (!texto) return "";
  if (ID_RE.test(texto)) return texto;

  let u;
  try {
    u = new URL(texto.indexOf("//") < 0 ? "https://" + texto : texto);
  } catch (e) {
    return "";
  }
  const host = u.hostname.replace(/^www\./, "").toLowerCase();
  if (host === "youtu.be") {
    const id = u.pathname.slice(1).split("/")[0];
    return ID_RE.test(id) ? id : "";
  }
  if (host !== "youtube.com" && host !== "m.youtube.com" && host !== "youtube-nocookie.com") {
    return "";
  }
  const v = u.searchParams.get("v");
  if (v && ID_RE.test(v)) return v;
  // /embed/ID, /shorts/ID, /live/ID, /v/ID
  const partes = u.pathname.split("/").filter(Boolean);
  if (partes.length >= 2 && ["embed", "shorts", "live", "v"].indexOf(partes[0]) >= 0) {
    return ID_RE.test(partes[1]) ? partes[1] : "";
  }
  return "";
}

/**
 * URL para incrustar. Se usa el dominio sin cookies: la partitura no necesita
 * que YouTube siga a quien la lee.
 */
export function youtubeEmbed(url) {
  const id = youtubeId(url);
  return id ? "https://www.youtube-nocookie.com/embed/" + id : "";
}

/** Búsqueda en YouTube para una canción, para no teclearla a mano. */
export function youtubeSearch(song) {
  const consulta = [(song && song.artist) || "", (song && song.title) || ""]
    .filter(Boolean).join(" ").trim();
  if (!consulta) return "";
  return "https://www.youtube.com/results?search_query=" + encodeURIComponent(consulta);
}

/** ¿Vale como valor guardable? Vacío sí (quitar el vídeo); basura no. */
export function isValidYoutube(url) {
  const texto = String(url == null ? "" : url).trim();
  return texto === "" || youtubeId(texto) !== "";
}
