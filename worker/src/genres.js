/*
 * Vivace · categorías automáticas.
 *
 * Adivinar el estilo de una canción por su título y su artista es, por
 * definición, aproximado: esto es un punto de partida para no tener el catálogo
 * entero sin clasificar, no una verdad. Las reglas están a la vista para poder
 * corregirlas, y cualquier categoría se cambia luego a mano desde el editor.
 *
 * Orden de decisión:
 *   1. El artista, que es la señal más fiable.
 *   2. Palabras del título, para lo que el artista no resuelve.
 *   3. La categoría de reserva, para que no quede nada vacío.
 *
 * Las comparaciones van sin tildes y en minúsculas: en un catálogo escrito a
 * mano conviven "Mecano", "MECANO" y "Mecano ".
 */

/** Quita tildes y pasa a minúsculas, para comparar sin sorpresas. */
export function normalize(texto) {
  return String(texto || "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .trim();
}

/**
 * Reglas por artista. La clave es la categoría y el valor, fragmentos de
 * nombre: basta con que el nombre del artista contenga uno.
 */
export const ARTIST_RULES = [
  ["Flamenco", ["camaron", "paco de lucia", "tomatito", "vicente amigo", "nina pastori",
                "estrella morente", "enrique morente", "diego el cigala", "ketama",
                "pata negra", "raimundo amador", "farruquito"]],
  ["Rumba", ["gipsy kings", "peret", "los chichos", "los chunguitos", "las grecas",
             "estopa", "chambao", "manu chao"]],
  ["Cantautor", ["serrat", "sabina", "aute", "silvio rodriguez", "pablo milanes",
                 "victor jara", "violeta parra", "atahualpa", "ismael serrano",
                 "luis eduardo aute", "paco ibanez", "labordeta", "quilapayun",
                 "inti-illimani", "mercedes sosa", "alberto cortez", "facundo cabral",
                 "jorge drexler", "fito paez", "silvio"]],
  ["Rock", ["heroes del silencio", "extremoduro", "marea", "platero", "barricada",
            "leno", "burning", "loquillo", "rosendo", "ilegales", "los suaves",
            "fito", "mclan", "m clan", "los rodriguez", "soda stereo", "charly garcia",
            "sumo", "divididos", "beatles", "rolling stones", "led zeppelin",
            "queen", "nirvana", "u2", "bon jovi", "guns n", "pink floyd",
            "creedence", "dire straits", "eric clapton", "jimi hendrix", "the who",
            "deep purple", "ac/dc", "acdc", "the doors", "bruce springsteen"]],
  ["Punk", ["ramones", "sex pistols", "la polla", "eskorbuto", "kortatu", "ska-p",
            "boikot", "the clash", "green day", "the offspring", "nofx"]],
  ["Metal", ["metallica", "iron maiden", "black sabbath", "megadeth", "slayer",
             "judas priest", "mago de oz", "avalanch", "saratoga", "baron rojo",
             "obus", "angeles del infierno"]],
  ["Pop", ["mecano", "hombres g", "duncan dhu", "la union", "alaska", "nacha pop",
           "radio futura", "el ultimo de la fila", "presuntos implicados",
           "amaral", "la oreja de van gogh", "el canto del loco", "pereza",
           "vetusta morla", "love of lesbian", "izal", "supersubmarina",
           "coldplay", "oasis", "abba", "michael jackson", "madonna",
           "ed sheeran", "adele", "maroon"]],
  ["Bolero", ["los panchos", "armando manzanero", "luis miguel", "antonio machin",
              "olga guillot", "lucho gatica", "bebo", "chavela vargas",
              "consuelo velazquez", "agustin lara"]],
  ["Ranchera", ["vicente fernandez", "jose alfredo jimenez", "pedro infante",
                "jorge negrete", "antonio aguilar", "juan gabriel", "mariachi"]],
  ["Salsa", ["hector lavoe", "willie colon", "celia cruz", "ruben blades",
             "marc anthony", "el gran combo", "fania", "oscar d", "juan luis guerra",
             "grupo niche"]],
  ["Cumbia", ["los angeles azules", "carlos vives", "los palmeras", "aniceto molina",
              "la sonora dinamita"]],
  ["Tango", ["gardel", "piazzolla", "goyeneche", "anibal troilo"]],
  ["Reggae", ["bob marley", "peter tosh", "jimmy cliff", "toots", "steel pulse"]],
  ["Blues", ["b.b. king", "bb king", "muddy waters", "john lee hooker",
             "stevie ray", "howlin"]],
  ["Jazz", ["ella fitzgerald", "louis armstrong", "frank sinatra", "nat king cole",
            "chet baker", "miles davis", "john coltrane", "billie holiday"]],
  ["Country", ["johnny cash", "willie nelson", "dolly parton", "hank williams",
               "kenny rogers", "john denver"]],
  ["Folk", ["bob dylan", "simon and garfunkel", "simon & garfunkel", "joan baez",
            "neil young", "cat stevens", "leonard cohen", "james taylor",
            "the mamas", "peter paul and mary"]],
  ["Flamenco-pop", ["rosalia", "india martinez", "pastora soler", "melendi",
                    "antonio orozco", "alejandro sanz", "david bisbal", "malu"]],
  ["Latino", ["shakira", "juanes", "mana", "cafe tacvba", "los fabulosos cadillacs",
              "jarabe de palo", "diego torres", "ricardo arjona", "camilo sesto",
              "jose luis perales", "julio iglesias", "raphael", "nino bravo",
              "rocio durcal", "roberto carlos"]],
  ["Infantil", ["cantajuegos", "el reino infantil", "pica-pica", "parchis"]]
];

/**
 * Reglas por palabras del título, para cuando el artista no dice nada. Se
 * miran después: un villancico de Mecano es un villancico.
 */
export const TITLE_RULES = [
  ["Villancico", ["navidad", "noche de paz", "belen", "villancico", "campana sobre campana",
                  "los peces en el rio", "arre borriquito", "blanca navidad", "jingle bells",
                  "adeste fideles", "el tamborilero"]],
  ["Religioso", ["aleluya", "senor", "padre nuestro", "ave maria", "misa", "gloria a dios",
                 "resucito", "amazing grace", "salmo"]],
  ["Infantil", ["cumpleanos feliz", "cancion infantil", "debajo un boton", "el patio de mi casa",
                "cinco lobitos", "la vaca lola"]],
  ["Himno", ["himno"]],
  ["Copla", ["copla", "pasodoble", "espana cani", "suspiros de espana"]],
  ["Banda sonora", ["banda sonora", "soundtrack", "tema principal", "star wars",
                    "el padrino", "titanic", "james bond"]],
  ["Bolero", ["bolero", "besame mucho", "solamente una vez", "sabor a mi",
              "contigo en la distancia", "historia de un amor", "quizas"]],
  ["Tango", ["tango", "por una cabeza", "el dia que me quieras", "volver"]],
  ["Ranchera", ["ranchera", "cielito lindo", "el rey", "guadalajara"]],
  ["Blues", ["blues"]],
  ["Rock", ["rock and roll", "rock & roll"]]
];

/** Categoría de reserva: mejor "sin clasificar" que un hueco vacío. */
export const FALLBACK_GENRE = "Varios";

function coincide(texto, fragmentos) {
  for (const f of fragmentos) if (texto.indexOf(f) >= 0) return true;
  return false;
}

/**
 * Estilo propuesto para una canción. Devuelve la categoría de reserva si no
 * encaja en ninguna regla, y nunca cadena vacía: quien llama decide si la
 * aplica o no.
 */
export function guessGenre(song, fallback = FALLBACK_GENRE) {
  const artista = normalize(song && song.artist);
  const titulo = normalize(song && song.title);

  if (artista) {
    for (const [genero, fragmentos] of ARTIST_RULES) {
      if (coincide(artista, fragmentos)) return genero;
    }
  }
  // El título también puede llevar el artista delante ("Serrat - Mediterráneo"),
  // así que se busca en el conjunto.
  const todo = titulo + " " + artista;
  for (const [genero, fragmentos] of TITLE_RULES) {
    if (coincide(todo, fragmentos)) return genero;
  }
  for (const [genero, fragmentos] of ARTIST_RULES) {
    if (coincide(titulo, fragmentos)) return genero;
  }
  return fallback;
}
