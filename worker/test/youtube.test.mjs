import { strict as assert } from "node:assert";
import test from "node:test";
import { isValidYoutube, youtubeEmbed, youtubeId, youtubeSearch } from "../src/youtube.js";
import { CLIENT_JS } from "../src/client-lib.js";

const ID = "dQw4w9WgXcQ";

const FORMAS = [
  ["https://www.youtube.com/watch?v=" + ID, ID],
  ["https://www.youtube.com/watch?v=" + ID + "&t=42s", ID],
  ["https://youtu.be/" + ID, ID],
  ["https://youtu.be/" + ID + "?t=42", ID],
  ["https://www.youtube.com/shorts/" + ID, ID],
  ["https://www.youtube.com/embed/" + ID, ID],
  ["https://www.youtube-nocookie.com/embed/" + ID, ID],
  ["https://m.youtube.com/watch?v=" + ID, ID],
  ["youtube.com/watch?v=" + ID, ID],
  [ID, ID],
  ["  " + ID + "  ", ID],
  ["https://vimeo.com/12345", ""],
  ["https://ejemplo.com/watch?v=" + ID, ""],
  ["https://www.youtube.com/watch?v=corto", ""],
  ["no es una url", ""],
  ["", ""],
  [null, ""],
  ["javascript:alert(1)", ""]
];

test("se reconocen las formas habituales de enlace y se rechaza el resto", () => {
  for (const [entrada, esperado] of FORMAS) {
    assert.equal(youtubeId(entrada), esperado, "fallo con " + JSON.stringify(entrada));
  }
});

test("se incrusta en el dominio sin cookies", () => {
  assert.equal(youtubeEmbed("https://youtu.be/" + ID),
               "https://www.youtube-nocookie.com/embed/" + ID);
  assert.equal(youtubeEmbed("cualquier cosa"), "", "sin id no hay iframe que montar");
});

test("vacío vale (quitar el vídeo), basura no", () => {
  assert.equal(isValidYoutube(""), true);
  assert.equal(isValidYoutube("   "), true);
  assert.equal(isValidYoutube("https://youtu.be/" + ID), true);
  assert.equal(isValidYoutube("https://ejemplo.com/vete"), false);
});

test("la búsqueda junta artista y título", () => {
  const u = youtubeSearch({ artist: "Joan Manuel Serrat", title: "Mediterráneo" });
  assert.ok(u.startsWith("https://www.youtube.com/results?search_query="));
  assert.ok(u.indexOf("Serrat") > 0);
  assert.equal(youtubeSearch({}), "", "sin datos no hay búsqueda");
});

test("la copia del navegador dice lo mismo que la del servidor", () => {
  // Hay dos implementaciones a la fuerza: el Worker no puede ejecutar el texto
  // de client-lib. Esto es lo que evita que se separen sin que nadie se entere.
  const vYoutubeId = new Function(CLIENT_JS + "\nreturn vYoutubeId;")();
  const vEmbedUrl = new Function(CLIENT_JS + "\nreturn vEmbedUrl;")();
  for (const [entrada, esperado] of FORMAS) {
    assert.equal(vYoutubeId(entrada), esperado,
      "el navegador discrepa con " + JSON.stringify(entrada));
    assert.equal(vEmbedUrl(entrada), youtubeEmbed(entrada),
      "embed distinto para " + JSON.stringify(entrada));
  }
});
