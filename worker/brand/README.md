# Accordio · fuentes de marca

Copia de lo que se usa del paquete de estilo (`accordio_claro_oscuro.zip`,
carpeta `accordio-web-kit/`), que trae la paleta de **modo claro y modo oscuro**.
No se copia el kit entero: las capturas y las imágenes de referencia pesan
2,3 MB y no entran en el Worker, que sirve todo desde el bundle.

| Fichero | De dónde sale | Dónde acaba |
| --- | --- | --- |
| `tokens.css` | `tokens/tokens.css` | valores `--ac-*` del `:root` de `src/web-html.js` |
| `tokens.dark.css` | `tokens/tokens.dark.css` | bloques `[data-theme=dark]` y `prefers-color-scheme: dark` |
| `favicon.svg` / `favicon-dark.svg` | `logo/` | `FAVICON_SVG` / `FAVICON_DARK_SVG`, en `/static/favicon*.svg` |
| `music-tile.svg` / `music-tile-dark.svg` | `patterns/` | `PATTERN_SVG` / `PATTERN_DARK_SVG`, en `/static/pattern*.svg` |
| `mark-fretboard.svg` | `logo/mark-fretboard.svg` | marca de la cabecera, en línea en el HTML |

Están aquí como referencia: **el Worker no los lee**. Si el kit cambia, se
actualiza esta carpeta y, a mano, la copia embebida en `src/web-html.js`.

## Cómo se aplica el tema

Los `--ac-*` de rol (`--ac-action`, `--ac-on-action`, `--ac-highlight`,
`--ac-active`, `--ac-pending`, `--ac-nav-bg`, `--ac-nav-fg`, `--ac-pattern`) son
los que cambian entre claro y oscuro; las rampas de marca no se invierten. La
capa `--vv-*` de la web cuelga de esos roles (regla 9 del kit), así que el modo
oscuro entra casi solo: solo hay que rectificar los rellenos tintados, el borde
fuerte y el coral de texto, que en claro necesita la rampa 700 para poder
leerse.

Lo que NO se ha cogido del kit y por qué:

- `css/accordio.css`: es una capa de componentes `.ac-*` para montar una web
  desde cero. Aquí ya había componentes con su propio marcado, así que se han
  reasignado sus colores y formas a los tokens del kit en vez de reescribir el
  HTML entero.
- `css/fonts.css`: usa `@import`, que encadena dos descargas. Las fuentes se
  piden con un `<link>` en el `<head>`, junto a JetBrains Mono.
- `js/theme.js`: la web ya traía su conmutador (mismo mecanismo: `data-theme`
  en `<html>` y memoria en `localStorage`). Meter el del kit sería un segundo
  conmutador con otra clave de almacenamiento. Sí se ha copiado su idea de
  cambiar los recursos con color fijo: el favicon tiene par claro/oscuro y lo
  cambia `recursosDeTema()`.
- `icons/`, `icons-dark/`, `chords/`, `images/`: los iconos de la web son texto
  y símbolos, y los diagramas de acordes se dibujan a partir de las digitaciones
  reales (`vChordSvg`), no como SVG fijos. Sí se han adoptado sus medidas
  (trazo 2,6, cejuela 6 y número de dedo dentro del punto) y su color, que sale
  de `--ac-primary` y por tanto cambia con el tema.
