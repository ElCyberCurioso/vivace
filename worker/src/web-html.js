/*
 * Vivace · aplicación web (servida en /).
 *
 * La página no lleva build: es HTML servido tal cual, con el CSS y el JS en
 * dos ficheros aparte (/static/vivace.css y /static/vivace-app.js).
 *
 * Están fuera del HTML a propósito. Antes iba todo en línea, así que cada
 * visita se descargaba ~90 KB otra vez —el HTML no se puede cachear, porque
 * cambia con el contenido— y cualquier CSP que prohibiera 'unsafe-inline'
 * habría roto la página entera. Separados, el navegador los guarda y solo
 * revalida su ETag.
 */

/** Hoja de estilos, servida en /static/vivace.css. */
export const WEB_CSS = `  /* Vivace · estilo Nocturno. Oscuro por defecto; claro con [data-theme=light]
     o si el sistema lo pide. Los nombres --vv-* son los del paquete de marca. */
  :root {
    color-scheme: dark light;
    --vv-bg:#0F1113; --vv-surface:#17191C; --vv-surface-alt:#1E2124;
    --vv-border:#2B2D31; --vv-border-strong:#3A3D42;
    --vv-text:#F2EFE9; --vv-text-muted:#A7ABB2; --vv-text-subtle:#8B8F96;
    --vv-accent:#E8B04B; --vv-accent-strong:#C2762B; --vv-on-accent:#0F1113;
    --vv-beat:#7FB2A0; --vv-danger:#E0654B;
    --vv-accent-soft:rgba(232,176,75,.16);
    --vv-glow:0 0 14px rgba(232,176,75,.85);
    --vv-shadow-card:0 1px 2px rgba(0,0,0,.4);
    --vv-font-ui:'Space Grotesk',system-ui,-apple-system,Segoe UI,Roboto,sans-serif;
    --vv-font-mono:'JetBrains Mono',ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;
    --vv-radius-sm:8px; --vv-radius-md:10px; --vv-radius-lg:16px;
    /* Ancho de la hoja de partitura, en caracteres del tipo monoespaciado.
       Va en ch y no en px a propósito: al cambiar el tamaño de letra, la hoja
       sigue teniendo el mismo número de columnas y la lectura no se descoloca. */
    --vv-sheet:68ch;
  }
  [data-theme=light] {
    color-scheme: light;
    --vv-bg:#F6F4EF; --vv-surface:#FFFFFF; --vv-surface-alt:#EFEDE7;
    --vv-border:#E2DED5; --vv-border-strong:#D5D0C5;
    --vv-text:#16181A; --vv-text-muted:#4A4D52; --vv-text-subtle:#6B7076;
    --vv-accent:#B8791F; --vv-accent-strong:#8F5A12; --vv-on-accent:#FFFFFF;
    --vv-beat:#2F6B5B; --vv-danger:#B33F26;
    --vv-accent-soft:rgba(184,121,31,.14); --vv-glow:none;
    --vv-shadow-card:0 1px 2px rgba(22,24,26,.06);
  }
  @media (prefers-color-scheme: light) {
    :root:not([data-theme=dark]):not([data-theme=light]) {
      color-scheme: light;
      --vv-bg:#F6F4EF; --vv-surface:#FFFFFF; --vv-surface-alt:#EFEDE7;
      --vv-border:#E2DED5; --vv-border-strong:#D5D0C5;
      --vv-text:#16181A; --vv-text-muted:#4A4D52; --vv-text-subtle:#6B7076;
      --vv-accent:#B8791F; --vv-accent-strong:#8F5A12; --vv-on-accent:#FFFFFF;
      --vv-beat:#2F6B5B; --vv-danger:#B33F26;
      --vv-accent-soft:rgba(184,121,31,.14); --vv-glow:none;
      --vv-shadow-card:0 1px 2px rgba(22,24,26,.06);
    }
  }
  * { box-sizing:border-box; }
  a { color:var(--vv-accent); text-decoration:none; }
  a:hover { color:var(--vv-accent-strong); }
  a.brand { color:var(--vv-text); }
  a.brand:hover { color:var(--vv-text); }
  body { margin:0; background:var(--vv-bg); color:var(--vv-text);
         font:16px/1.5 var(--vv-font-ui); -webkit-font-smoothing:antialiased; }
  button, input, select, textarea { font:inherit; color:inherit; }
  :focus-visible { outline:2px solid var(--vv-accent); outline-offset:2px; }
  ::selection { background:var(--vv-accent-soft); }
  button { cursor:pointer; border:1px solid var(--vv-border-strong); background:transparent;
           color:var(--vv-text); border-radius:var(--vv-radius-md); padding:9px 15px;
           font-weight:500; white-space:nowrap; }
  button:hover { background:var(--vv-accent-soft); }
  button.primary { background:var(--vv-accent); color:var(--vv-on-accent);
                   border-color:transparent; font-weight:600; }
  button.primary:hover { background:var(--vv-accent-strong); }
  button.ghost { border-color:transparent; background:transparent; }
  button:disabled { opacity:.45; cursor:default; background:transparent; }
  input[type=text], input[type=email], input[type=password], input[type=url], textarea, select {
    background:var(--vv-surface-alt); border:1px solid var(--vv-border);
    border-radius:var(--vv-radius-md); padding:10px 12px; width:100%; }
  input::placeholder, textarea::placeholder { color:var(--vv-text-subtle); }
  header { position:sticky; top:0; z-index:10; display:flex; gap:12px; align-items:center;
           padding:10px 16px; background:var(--vv-surface); border-bottom:1px solid var(--vv-border); }
  header .brand { display:flex; align-items:center; gap:10px; min-width:0; }
  header .brand svg { flex:0 0 auto; }
  header h1 { margin:0; font-size:22px; font-weight:500; letter-spacing:.01em; line-height:1; }
  header .kicker { display:block; font-size:9px; letter-spacing:.22em; font-weight:500;
                   text-transform:uppercase; color:var(--vv-text-subtle); margin-top:3px; }
  header .grow { flex:1; }
  main { padding:16px; max-width:1520px; margin:0 auto; }
  #listView, #authView { max-width:960px; margin:0 auto; }
  .tabs { display:flex; gap:8px; margin-bottom:14px; flex-wrap:wrap; }
  .tabs button[aria-selected=true] { background:var(--vv-accent); color:var(--vv-on-accent);
                                     border-color:transparent; font-weight:600; }
  .grid { display:grid; gap:10px; grid-template-columns:repeat(auto-fill,minmax(230px,1fr)); align-items:start; }
  /* Las tarjetas son <button>, y la regla de arriba les pone nowrap: sin esto
     un titulo largo se sale de la tarjeta por la derecha. */
  .card, .chordCard { white-space:normal; }
  .card { display:block; text-align:left; width:100%; border:1px solid var(--vv-border);
          border-radius:var(--vv-radius-lg); padding:14px 16px; background:var(--vv-surface);
          box-shadow:var(--vv-shadow-card); }
  .card:hover { border-color:var(--vv-accent); background:var(--vv-surface); }
  .card .a { font-size:13px; color:var(--vv-text-muted); overflow-wrap:anywhere; }
  .card .t { font-weight:600; overflow-wrap:anywhere; }
  #vSource { border:1px solid var(--vv-border-strong); border-radius:var(--vv-radius-md);
             padding:9px 15px; font-size:14px; font-weight:500; white-space:nowrap;
             color:var(--vv-text); }
  #vSource:hover { background:var(--vv-accent-soft); color:var(--vv-text); }
  /* Tira de versiones bajo la cabecera del visor. */
  .versions { display:flex; gap:6px; overflow-x:auto; padding:8px 14px;
              background:var(--vv-surface); border-bottom:1px solid var(--vv-border); }
  .versions button { flex:0 0 auto; padding:6px 12px; font-size:13px; }
  .versions button[aria-pressed=true] { background:var(--vv-accent); color:var(--vv-on-accent);
                                        border-color:transparent; font-weight:600; }
  .versions .sep { flex:1 1 auto; min-width:8px; }
  /* Fichas de la cola de revisión y del listado de usuarios. */
  .fila { display:flex; gap:12px; align-items:flex-start; flex-wrap:wrap;
          border:1px solid var(--vv-border); border-radius:var(--vv-radius-lg);
          background:var(--vv-surface); padding:14px 16px; margin-bottom:10px; }
  .fila .cuerpo { flex:1 1 320px; min-width:0; }
  .fila .t { font-weight:600; overflow-wrap:anywhere; }
  .fila .meta { font-size:13px; color:var(--vv-text-muted); overflow-wrap:anywhere; }
  .fila .nota { margin-top:6px; font-size:14px; overflow-wrap:anywhere; }
  .estado { font-size:11px; border-radius:999px; padding:2px 9px; border:1px solid var(--vv-border-strong);
            color:var(--vv-text-muted); font-family:var(--vv-font-mono); }
  .estado[data-s=pending] { color:var(--vv-accent); border-color:var(--vv-accent); }
  .estado[data-s=approved] { color:var(--vv-beat); border-color:var(--vv-beat); }
  .estado[data-s=rejected], .estado[data-s=withdrawn] { color:var(--vv-danger); border-color:var(--vv-danger); }
  .aviso { background:var(--vv-accent-soft); border:1px solid var(--vv-accent);
           border-radius:var(--vv-radius-md); padding:10px 12px; margin-bottom:12px; font-size:14px; }
  .filtros { display:flex; gap:8px; margin-bottom:12px; flex-wrap:wrap; align-items:flex-end; }
  /* La etiqueta va encima del control: antes el nombre del filtro solo existía
     como title=, que ni se lee con lector de pantalla ni se ve al tocar. */
  .filtro { display:flex; flex-direction:column; gap:4px; flex:0 1 190px; font-size:11px;
            text-transform:uppercase; letter-spacing:.08em; color:var(--vv-text-muted); }
  .filtro:first-child { flex:1 1 220px; }
  /* El control ocupa el ancho de su etiqueta y NADA de flex propio: dentro de
     una columna, un flex-basis se convertiría en altura y salen cajas gigantes. */
  .filtro input, .filtro select { width:100%; flex:0 0 auto; }
  #favFilter[aria-pressed=true] { background:var(--vv-accent); color:var(--vv-on-accent);
                                  border-color:var(--vv-accent); }
  .listas { display:flex; gap:8px; align-items:center; flex-wrap:wrap; margin-bottom:12px; }
  .listas .etiqueta { font-size:11px; text-transform:uppercase; letter-spacing:.08em;
                      color:var(--vv-text-muted); }
  .chips { display:flex; gap:6px; flex-wrap:wrap; }
  .chip { border:1px solid var(--vv-border); border-radius:999px; padding:4px 12px;
          font-size:13px; background:transparent; color:inherit; cursor:pointer; }
  .chip[aria-pressed=true] { background:var(--vv-accent); color:var(--vv-on-accent);
                             border-color:var(--vv-accent); }
  .card .fav { float:right; border:0; background:transparent; font-size:16px; line-height:1;
               padding:0 0 0 8px; cursor:pointer; color:var(--vv-text-muted); }
  .card .fav[aria-pressed=true] { color:var(--vv-accent); }
  .acciones { display:flex; gap:6px; margin-top:8px; }
  .adminTool { border:1px solid var(--vv-border); border-radius:var(--vv-radius);
               padding:14px; margin-bottom:12px; }
  .adminTool h3 { margin:0 0 6px; font-size:15px; }
  .adminTool p { margin:0 0 10px; font-size:13px; color:var(--vv-text-muted); }
  .badge { display:inline-block; font-size:11px; border-radius:999px; padding:2px 9px;
           border:1px solid var(--vv-border-strong); color:var(--vv-text-muted); margin-top:6px;
           font-family:var(--vv-font-mono); }
  .msg { color:var(--vv-danger); min-height:1.2em; }
  .empty { color:var(--vv-text-muted); text-align:center; padding:40px 12px; }
  .row { display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
  .stack { display:flex; flex-direction:column; gap:12px; max-width:420px; margin:40px auto; }
  /* La partitura se centra como bloque, pero el texto sigue alineado a la
     izquierda: las columnas son las del fichero y mover el texto descolocaria
     los acordes de sus silabas. max-content = ancho de la linea mas larga. */
  /* Ancho fijo para todas. Antes era max-content y se centraba con margin
     auto: cada partitura salía con un ancho distinto según su línea más larga,
     y las cortas aparecían centradas mientras las largas iban a la izquierda. */
  .sheet { width:var(--vv-sheet); max-width:100%; margin:0; text-align:left;
           /* Las líneas más anchas que la hoja se desplazan DENTRO de ella:
              los acordes van sobre la sílaba exacta, así que partir la línea
              no es una opción. */
           overflow-x:auto; }
  /* ---- editor a dos paneles ---- */
  .editor { max-width:1520px; margin:0 auto; display:flex; flex-direction:column; gap:12px; }
  .editHead { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
  #editSplit { display:grid; grid-template-columns:1fr 1fr; gap:12px; align-items:start; }
  .pane { display:flex; flex-direction:column; gap:6px; min-width:0; }
  .pane .hd { display:flex; align-items:center; gap:8px; font-size:11px; letter-spacing:.18em;
              text-transform:uppercase; color:var(--vv-text-subtle); min-height:38px; }
  .pane .hd small { letter-spacing:0; text-transform:none; font-size:12px; }
  #eContent { height:58vh; min-height:280px; resize:vertical;
              font-family:var(--vv-font-mono); font-size:14px; line-height:1.45; white-space:pre; }
  #ePreview { height:58vh; min-height:280px; overflow:auto;
              background:var(--vv-surface); border:1px solid var(--vv-border);
              border-radius:var(--vv-radius-md); padding:10px 12px;
              font-family:var(--vv-font-mono); font-size:14px; line-height:1.45; }
  /* Misma altura de linea que el textarea: asi la linea N de la izquierda cae
     a la altura de la linea N de la derecha y el scroll atado cuadra. En el
     visor se mantiene 1.35, que ahi se lee mas apretado y no hay con que
     comparar. */
  #ePreview .ln, #ePreview .tab { line-height:1.45; }
  @media (max-width:900px) {
    #editSplit, .editHead { grid-template-columns:1fr; }
    #eContent, #ePreview { height:40vh; }
  }
  label small { color:var(--vv-text-subtle); }
  /* ---- visor ---- */
  #viewer { position:fixed; inset:0; background:var(--vv-bg); display:none; flex-direction:column; z-index:20; }
  #viewer.on { display:flex; }
  /* Con el visor abierto la pagina de detras no debe poder desplazarse: si no,
     salen dos barras verticales y una de ellas no hace nada. Va en los dos
     elementos porque el desplazamiento del body se propaga a la raiz. */
  html.conVisor, body.conVisor { overflow:hidden; }
  /* Tres zonas: acciones, centro, acciones. Con 1fr a los lados el bloque del
     medio queda centrado en la barra aunque los botones de cada lado no midan
     lo mismo — que es lo que pasaría con un simple flex. */
  #vHead { display:grid; grid-template-columns:1fr auto 1fr; gap:10px;
           align-items:center; padding:10px 14px;
           border-bottom:1px solid var(--vv-border); background:var(--vv-surface); }
  #vHead .lado { display:flex; align-items:center; gap:8px; min-width:0; }
  #vHead .lado.der { justify-content:flex-end; }
  /* El bloque de título tiene que poder encoger: sin min-width:0 no baja de su
     contenido y el título largo se sale de la cabecera. */
  /* Una sola línea: autor – título, y el capo a su derecha. */
  #vHead .vMeta { min-width:0; display:flex; align-items:center; gap:12px; }
  #vHead .titulo { min-width:0; display:flex; align-items:baseline; gap:6px; }
  #vHead .titulo > * { white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
  #vSep { color:var(--vv-text-subtle); flex:0 0 auto; }
  /* El capo cambia cómo se toca la canción entera: va en ámbar macizo, que no
     se pueda pasar por alto como pasaba con la píldora gris de antes. */
  #vCapo { display:inline-flex; align-items:baseline; gap:6px; flex:0 0 auto;
           background:var(--vv-accent); color:var(--vv-on-accent); border:0;
           border-radius:999px; padding:3px 12px 4px; font-size:11px;
           font-weight:600; letter-spacing:.16em; text-transform:uppercase; }
  #vCapo .n { font-family:var(--vv-font-mono); font-size:15px; letter-spacing:0; }
  #vTitle { font-weight:600; }
  #vArtist { color:var(--vv-text-muted); }
  /* El vídeo, a la derecha de la partitura. Se queda fijo mientras la hoja se
     desplaza: es para acompañarse, no para leerlo. */
  #vTube { flex:0 0 340px; display:none; flex-direction:column; gap:8px; min-height:0; }
  #vTube.on { display:flex; }
  #vTube .marco { position:relative; width:100%; padding-top:56.25%;
                  border:1px solid var(--vv-border); border-radius:var(--vv-radius-lg);
                  overflow:hidden; background:#000; }
  #vTube iframe { position:absolute; inset:0; width:100%; height:100%; border:0; }
  @media (max-width:1200px) { #vTube { flex-basis:280px; } }
  @media (max-width:900px) { #vTube { flex:0 0 auto; } }
  /* En vertical se mueve toda la columna; en horizontal, solo la hoja (ver
     .sheet). Si el desplazamiento lateral fuese de aquí, al leer una tablatura
     ancha se irían de lado también los comentarios y el pie. */
  #vBody { flex:0 1 auto; min-width:0; overflow-y:auto; overflow-x:hidden;
           padding:16px 18px 60px;
           font-family:var(--vv-font-mono); font-size:var(--fs,18px);
           /* Hueco de la barra reservado siempre. Sin esto, una canción larga
              tiene barra y una corta no, y la hoja se desplaza unos píxeles al
              cambiar de una a otra: el mismo baile que se quería quitar. */
           scrollbar-gutter:stable; }
  /* La columna de lectura mide siempre lo mismo, la cante quien la cante.
     Lo que se centra en la ventana es el CONJUNTO (mandos + hoja + vídeo),
     que es cosa de #vMain; dentro de la columna, el texto empieza a la
     izquierda y se queda ahí. */
  #vSheet { width:var(--vv-sheet); max-width:100%; flex:0 0 auto; }
  .ln { white-space:pre; line-height:1.35; margin:0; }
  .tab { white-space:pre; line-height:1.35; color:var(--vv-text-muted); }
  .chord { color:var(--vv-accent); font-weight:600; }
  #vBody .chord { cursor:pointer; border-radius:3px; }
  /* Globo de digitaciones al pasar por encima. Fuera del flujo y sin capturar
     el ratón: si lo capturase, entrar en él contaría como salir del acorde. */
  #chordHover { position:fixed; z-index:45; display:none; gap:8px; padding:10px;
                pointer-events:none; background:var(--vv-surface);
                border:1px solid var(--vv-border-strong); border-radius:var(--vv-radius-md);
                box-shadow:0 6px 24px rgba(0,0,0,.35); }
  #chordHover.on { display:flex; }
  #chordHover .nm { font-family:var(--vv-font-mono); font-weight:600; color:var(--vv-accent);
                    align-self:center; padding-right:2px; }
  /* Fila de botones para capo y traste base: un toque en vez de teclear. */
  .pills { display:flex; flex-wrap:wrap; gap:4px; }
  .pills button { padding:6px 10px; font-family:var(--vv-font-mono); font-size:13px;
                  min-width:38px; }
  .pills button[aria-pressed=true] { background:var(--vv-accent); color:var(--vv-on-accent);
                                     border-color:transparent; font-weight:600; }
  #vBody .chord:hover, #vBody .chord:focus-visible { background:var(--vv-accent-soft); }
  /* Los mandos y las versiones son dos recuadros en una columna propia a la
     izquierda de la partitura. Columna real, no algo flotando por encima: la
     hoja ocupa lo que queda y no hay que reservarle hueco a mano. */
  #vMain { flex:1; display:flex; min-height:0; gap:14px; padding:14px; justify-content:center; }
  #vSide { flex:0 0 260px; display:flex; flex-direction:column; gap:14px;
           min-height:0; overflow:auto; }
  #vCtrl { background:var(--vv-surface); border:1px solid var(--vv-border);
           border-radius:var(--vv-radius-lg);
           padding:12px; display:flex; flex-direction:column; gap:10px; flex:0 0 auto; }
  #vCtrl .row { gap:6px; }
  #vCtrl button { padding:7px 10px; font-size:13px; }
  #vVersionPanel { background:var(--vv-surface); border:1px solid var(--vv-border);
                   border-radius:var(--vv-radius-lg);
                   padding:12px; display:flex; flex-direction:column; gap:8px;
                   flex:0 1 auto; min-height:0; overflow:auto; }
  #vVersionPanel .hd { font-size:11px; letter-spacing:.18em; text-transform:uppercase;
                       color:var(--vv-text-subtle); }
  .vRow { display:flex; flex-direction:column; gap:4px; padding:8px; border-radius:var(--vv-radius-md);
          border:1px solid transparent; cursor:pointer; background:transparent; text-align:left;
          white-space:normal; }
  .vRow:hover { background:var(--vv-accent-soft); }
  .vRow[aria-pressed=true] { border-color:var(--vv-accent); background:var(--vv-accent-soft); }
  .vRow .num { font-family:var(--vv-font-mono); font-size:12px; color:var(--vv-text-subtle); }
  .vRow .nm { font-weight:600; overflow-wrap:anywhere; }
  .vRow .val { display:flex; align-items:center; gap:6px; }
  .stars { display:inline-flex; gap:1px; }
  .stars button, .stars span { border:0; background:none; padding:0 1px; font-size:15px;
                               line-height:1; color:var(--vv-border-strong); cursor:default; }
  .stars button { cursor:pointer; }
  .stars .on { color:var(--vv-accent); }
  .nota { font-family:var(--vv-font-mono); font-size:12px; color:var(--vv-text-muted); }
  /* Comentarios, al final de la partitura. */
  #vComments { max-width:100%; margin:32px 0 0; border-top:1px solid var(--vv-border);
               padding-top:16px; font-family:var(--vv-font-ui); font-size:15px; }
  #vComments h4 { margin:0 0 12px; font-size:11px; letter-spacing:.18em; text-transform:uppercase;
                  color:var(--vv-text-subtle); font-weight:600; }
  .comentario { padding:10px 0; border-bottom:1px solid var(--vv-border); }
  .comentario .quien { font-size:13px; color:var(--vv-text-muted); display:flex; gap:8px; align-items:baseline; }
  .comentario .texto { white-space:pre-wrap; overflow-wrap:anywhere; margin-top:4px; }
  .comentario .quitar { border:0; background:none; color:var(--vv-danger); font-size:12px;
                        padding:0; cursor:pointer; }
  #vCommentForm { display:flex; flex-direction:column; gap:8px; margin-top:12px; }
  #vCommentForm textarea { min-height:80px; font-family:var(--vv-font-ui); }
  /* En estrecho no caben dos columnas: los recuadros pasan encima de la hoja,
     apilados, y se les limita la altura para que quede partitura a la vista. */
  @media (max-width:900px) {
    #vMain { flex-direction:column; gap:10px; padding:10px; }
    #vSide { flex:0 0 auto; max-height:46vh; }
  }
  #vCtrl .row label { font-size:12px; color:var(--vv-text-muted); white-space:nowrap; }
  #vCtrl input[type=range] { flex:1; min-width:80px; width:auto; padding:0; border:0;
                             background:transparent; accent-color:var(--vv-accent); }
  /* Cifras (BPM, tono, velocidad, capo) siempre en la mono de la marca. */
  #vSpeedVal, #vBpmVal, #vTone {
    font-family:var(--vv-font-mono); font-variant-numeric:tabular-nums; }
  .beat { width:12px; height:12px; border-radius:50%; background:var(--vv-border-strong); display:inline-block; }
  .beat.on { background:var(--vv-accent); box-shadow:var(--vv-glow); }
  /* ---- acordes ---- */
  .chordSvg { display:block; }
  .chordBar { display:flex; gap:8px; overflow-x:auto; padding:8px 14px; background:var(--vv-surface);
              border-bottom:1px solid var(--vv-border); }
  .chordBar button { flex:0 0 auto; display:flex; flex-direction:column; align-items:center; gap:2px;
                     padding:6px 8px; border-color:transparent; }
  .chordBar .nm { font-family:var(--vv-font-mono); font-size:12px; font-weight:600; color:var(--vv-accent); }
  .chordBar .none { color:var(--vv-text-subtle); font-size:13px; padding:8px 4px; white-space:nowrap; }
  .modal { position:fixed; inset:0; z-index:40; background:rgba(0,0,0,.55);
           display:flex; align-items:center; justify-content:center; padding:16px; }
  .modalBox { background:var(--vv-surface); border:1px solid var(--vv-border); width:100%;
              border-radius:var(--vv-radius-lg); padding:16px; max-width:780px; max-height:86vh;
              overflow:auto; display:flex; flex-direction:column; gap:12px; }
  .modalBox h3 { margin:0; font-size:20px; font-family:var(--vv-font-mono);
                 min-width:0; overflow-wrap:anywhere; }
  .chordCard .nm { overflow-wrap:anywhere; text-align:center; }
  .chordGrid { display:grid; gap:10px; grid-template-columns:repeat(auto-fill,minmax(118px,1fr)); }
  .chordCard { display:flex; flex-direction:column; align-items:center; gap:4px; padding:10px;
               border:1px solid var(--vv-border); border-radius:var(--vv-radius-md);
               background:var(--vv-surface); }
  .chordCard .nm { font-family:var(--vv-font-mono); font-weight:600; }
  .chordCard .va { font-size:11px; color:var(--vv-text-subtle); }
  .posRow { display:flex; gap:12px; align-items:center; flex-wrap:wrap; padding:10px;
            border:1px solid var(--vv-border); border-radius:var(--vv-radius-md); }
  .posRow input { width:54px; text-align:center; font-family:var(--vv-font-mono); padding:6px 4px; }
  .posRow .lbl { font-size:11px; letter-spacing:.14em; text-transform:uppercase; color:var(--vv-text-subtle); }
  .posRow .grp { display:flex; gap:4px; align-items:center; }
  .posRow .pills button { padding:4px 8px; min-width:30px; font-size:12px; }
  .hidden { display:none !important; }
`;

/** JavaScript de la aplicación, servido en /static/vivace-app.js. */
export const WEB_APP_JS = `var token = localStorage.getItem("vivace_token") || "";
var user = null;
var tab = "public";
var songs = [];
var current = null;          // { song, content }
var editingId = null;        // null = alta nueva
var semis = 0, flats = false, fontSize = 18;
var scrolling = false, lastTs = 0, remainder = 0, raf = 0, wakeLock = null;
var metro = new VMetronome();
var ratings = {};              // medias por versión ("" = el Original)
var myRatings = {};            // lo que ha votado quien está mirando
var comments = [];
var versions = [];             // versiones de la partitura abierta
var currentVersion = null;     // null = el "Original"
var editorMode = "song";       // song | version | proposal
var editingVersionId = null;
var proposalTarget = null;     // partitura sobre la que se propone
var proposals = [];
var genres = [];               // categorías con partituras publicadas
var sortBy = "title";
var genreBy = "";
var listOffset = 0;            // desplazamiento del listado que se está viendo
var listHasMore = false;       // queda más detrás en el servidor
var playlists = [];            // listas (carpetas) del usuario
var playlistBy = "";           // filtro de lista activo en "Mis partituras"
var favOnly = false;           // ver solo las favoritas
var chordDict = null;          // diccionario global, cacheado tras la primera carga
var chordBarOn = false;
var editingChord = null;       // nombre que se está editando, "" si es nuevo
var chordPositions = [];       // posiciones del acorde en edición

/** Cuántas partituras se piden de golpe. */
var PAGINA = 60;

/* ---------- diálogos accesibles ---------- */
/*
 * Los modales no eran modales para nadie que no viera la pantalla: sin
 * role/aria-modal no se anunciaban, el foco se quedaba detrás y el tabulador
 * paseaba por la página de debajo. Esto lo arregla sin cambiar el aspecto.
 */
var focoPrevio = null;

function trapFoco(caja, e) {
  if (e.key !== "Tab") return;
  var focos = caja.querySelectorAll(
    'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
  );
  if (!focos.length) return;
  var primero = focos[0], ultimo = focos[focos.length - 1];
  if (e.shiftKey && document.activeElement === primero) { e.preventDefault(); ultimo.focus(); }
  else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primero.focus(); }
}

function abrirDialogo(caja) {
  focoPrevio = document.activeElement;
  caja.classList.remove("hidden");
  var primero = caja.querySelector("input, button, select, textarea, a[href]");
  if (primero) primero.focus();
  caja.__trap = function (e) { trapFoco(caja, e); };
  caja.addEventListener("keydown", caja.__trap);
}

function cerrarDialogo(caja) {
  caja.classList.add("hidden");
  if (caja.__trap) { caja.removeEventListener("keydown", caja.__trap); caja.__trap = null; }
  // Devolver el foco a donde estaba: si no, se cae al principio de la página.
  if (focoPrevio && focoPrevio.focus) focoPrevio.focus();
  focoPrevio = null;
}

/* ---------- tema ---------- */
/*
 * Los tokens de color ya contemplaban [data-theme=light], pero nadie ponía
 * nunca ese atributo: el modo claro existía en el CSS y era inalcanzable.
 * Aquí se fija y se recuerda; sin elección guardada manda el sistema.
 */
function applyTheme(modo) {
  if (modo === "light" || modo === "dark") document.documentElement.setAttribute("data-theme", modo);
  else document.documentElement.removeAttribute("data-theme");
}
function currentTheme() {
  try { return localStorage.getItem("vivace_theme") || ""; } catch (e) { return ""; }
}
function toggleTheme() {
  var actual = currentTheme();
  var oscuroAhora = actual
    ? actual === "dark"
    : window.matchMedia("(prefers-color-scheme: dark)").matches;
  var siguiente = oscuroAhora ? "light" : "dark";
  try { localStorage.setItem("vivace_theme", siguiente); } catch (e) {}
  applyTheme(siguiente);
}

/* ---------- API ---------- */
function api(method, path, body) {
  var opt = { method: method, headers: {} };
  if (token) opt.headers["Authorization"] = "Bearer " + token;
  if (body !== undefined) {
    opt.headers["Content-Type"] = "application/json";
    opt.body = JSON.stringify(body);
  }
  return fetch(path, opt).then(function (r) {
    return r.json().catch(function () { return {}; }).then(function (data) {
      if (!r.ok) throw new Error(data.error || ("Error " + r.status));
      return data;
    });
  });
}

/* ---------- sesión ---------- */
function setSession(tok, u) {
  token = tok || "";
  user = u || null;
  if (token) localStorage.setItem("vivace_token", token);
  else localStorage.removeItem("vivace_token");
  who.textContent = user ? (user.name || user.email) : "";
  who.classList.toggle("hidden", !user);
  loginBtn.classList.toggle("hidden", !!user);
  logoutBtn.classList.toggle("hidden", !user);
  tabMine.classList.toggle("hidden", !user);
  tabChords.classList.toggle("hidden", !esEditor());
  tabProposals.classList.toggle("hidden", !user);
  tabUsers.classList.toggle("hidden", !esAdmin());
  tabTrash.classList.toggle("hidden", !user);
  tabAdmin.classList.toggle("hidden", !esEditor());
  newBtn.classList.toggle("hidden", !user);
  if (!user && tab === "mine") tab = "public";
  if (!esEditor() && tab === "chords") tab = "public";
  if (!user && tab === "proposals") tab = "public";
  if (!esAdmin() && tab === "users") tab = "public";
  if (!user && tab === "trash") tab = "public";
  if (!esEditor() && tab === "admin") tab = "public";
  if (user) {
    loadPlaylists();
  } else {
    // Al salir no puede quedar rastro de la cuenta anterior en los filtros.
    playlists = [];
    playlistBy = "";
    favOnly = false;
    favFilter.setAttribute("aria-pressed", "false");
    renderPlaylistControls();
  }
}

/* ---------- listas (carpetas) ---------- */
/*
 * Las carpetas existían solo en el móvil, escondidas dentro del texto de la
 * partitura como una cabecera "#playlist:". Ahora son datos de la API y la web
 * puede enseñarlas, crearlas y mover partituras entre ellas.
 */
function loadPlaylists() {
  return api("GET", "/api/playlists").then(function (d) {
    playlists = d.playlists || [];
    renderPlaylistControls();
  }).catch(function (e) {
    playlists = [];
    renderPlaylistControls();
    aviso(listEmpty, "No se han podido cargar las listas: " + e.message);
  });
}

function renderPlaylistControls() {
  // Filtro del listado.
  playlistFilter.innerHTML = "";
  [{ id: "", name: "Todas" }, { id: "none", name: "Sin lista" }]
    .concat(playlists)
    .forEach(function (p) {
      var o = document.createElement("option");
      o.value = p.id; o.textContent = p.name;
      playlistFilter.appendChild(o);
    });
  playlistFilter.value = playlistBy;

  // Selector del editor.
  ePlaylist.innerHTML = "";
  [{ id: "", name: "Sin lista" }].concat(playlists).forEach(function (p) {
    var o = document.createElement("option");
    o.value = p.id; o.textContent = p.name;
    ePlaylist.appendChild(o);
  });

  // Chips de acceso rápido.
  listasChips.innerHTML = "";
  playlists.forEach(function (p) {
    var b = document.createElement("button");
    b.className = "chip";
    b.textContent = p.name;
    b.setAttribute("aria-pressed", playlistBy === p.id);
    b.onclick = function () {
      playlistBy = playlistBy === p.id ? "" : p.id;
      playlistFilter.value = playlistBy;
      renderPlaylistControls();
      renderList();
    };
    listasChips.appendChild(b);
  });
  var haySeleccion = !!playlistBy && playlistBy !== "none";
  renameListBtn.classList.toggle("hidden", !haySeleccion);
  deleteListBtn.classList.toggle("hidden", !haySeleccion);
}

function crearLista() {
  var nombre = prompt("Nombre de la lista");
  if (!nombre || !nombre.trim()) return;
  api("POST", "/api/playlists", { name: nombre.trim() })
    .then(loadPlaylists)
    .catch(function (e) { aviso(listEmpty, e.message); });
}

function renombrarLista() {
  var lista = playlists.filter(function (p) { return p.id === playlistBy; })[0];
  if (!lista) return;
  var nombre = prompt("Nuevo nombre", lista.name);
  if (!nombre || !nombre.trim()) return;
  api("PUT", "/api/playlists/" + lista.id, { name: nombre.trim() })
    .then(loadPlaylists)
    .catch(function (e) { aviso(listEmpty, e.message); });
}

function borrarLista() {
  var lista = playlists.filter(function (p) { return p.id === playlistBy; })[0];
  if (!lista) return;
  // Igual que en la app: borrar la carpeta NO borra las partituras.
  if (!confirm("Se borrará la lista «" + lista.name + "». Sus partituras no se borran: pasan a «Sin lista». ¿Continuar?")) return;
  api("DELETE", "/api/playlists/" + lista.id).then(function () {
    playlistBy = "";
    return loadPlaylists();
  }).then(refresh).catch(function (e) { aviso(listEmpty, e.message); });
}

/** Aviso corto en un hueco de la página (sin alert(), que bloquea). */
function aviso(nodo, texto) {
  nodo.textContent = texto;
  nodo.classList.remove("hidden");
}

function restoreSession() {
  if (!token) return Promise.resolve();
  return api("GET", "/auth/me")
    .then(function (d) { setSession(token, d.user); })
    .catch(function () { setSession("", null); });   // token caducado
}

var registering = false;
function showAuth(on) {
  showView(on ? "auth" : "list");
  authMsg.textContent = "";
}
function submitAuth() {
  var path = registering ? "/auth/register" : "/auth/login";
  var body = { email: email.value.trim(), password: password.value };
  // getElementById y no el global implicito: window.name ya existe como
  // cadena nativa y gana al elemento, asi que name.value seria undefined.
  if (registering) body.name = document.getElementById("name").value.trim();
  authMsg.textContent = "";
  api("POST", path, body).then(function (d) {
    setSession(d.token, d.user);
    password.value = "";
    showAuth(false);
    tab = "mine";
    listOffset = 0;
    refresh();
  }).catch(function (e) { authMsg.textContent = e.message; });
}

/**
 * Las secciones de <main> son excluyentes: se ve una y solo una. Tenerlo en un
 * único sitio evita el fallo de que cada pantalla se escondiera por su cuenta
 * y dejara otra abierta debajo.
 */
function showView(vista) {
  var vistas = {
    list: listView, auth: authView, edit: editView,
    chords: chordsView, proposals: proposalsView, users: usersView,
    admin: adminView
  };
  Object.keys(vistas).forEach(function (nombre) {
    vistas[nombre].classList.toggle("hidden", nombre !== vista);
  });
}

/** El equipo editorial: editor y admin. Misma regla que en el servidor. */
function esEditor() { return !!user && (user.role === "editor" || user.role === "admin"); }
function esAdmin() { return !!user && user.role === "admin"; }

/* ---------- listados ---------- */

/** Categorías que de verdad tienen partituras publicadas. */
/**
 * Pinta el desplegable del filtro. [lista] son pares {genre, total}. Si la
 * categoría elegida ya no está entre ellas se añade igualmente: si no, al
 * repintar se perdería el filtro sin avisar.
 */
function renderGenreOptions(lista) {
  var opciones = '<option value="">Todas las categorías</option>';
  var hayElegida = !genreBy;
  lista.forEach(function (g) {
    if (g.genre === genreBy) hayElegida = true;
    opciones += '<option value="' + vEsc(g.genre) + '">' + vEsc(g.genre) +
                (g.total ? " (" + g.total + ")" : "") + "</option>";
  });
  if (!hayElegida) {
    opciones += '<option value="' + vEsc(genreBy) + '">' + vEsc(genreBy) + "</option>";
  }
  genreFilter.innerHTML = opciones;
  genreFilter.value = genreBy;
}

/** Sugerencias del editor: las públicas y las propias, sin repetir. */
function renderGenreDatalist(extra) {
  var vistas = {};
  var todas = [];
  genres.concat(extra || []).forEach(function (g) {
    var clave = (g.genre || "").toLowerCase();
    if (!clave || vistas[clave]) return;
    vistas[clave] = 1;
    todas.push(g.genre);
  });
  todas.sort(function (a, b) { return a.localeCompare(b, "es"); });
  genreList.innerHTML = todas.map(function (g) {
    return '<option value="' + vEsc(g) + '"></option>';
  }).join("");
}

/** Categorías presentes en una lista de partituras ya cargada. */
function genresOf(lista) {
  var cuenta = {};
  lista.forEach(function (s) {
    var g = (s.genre || "").trim();
    if (g) cuenta[g] = (cuenta[g] || 0) + 1;
  });
  return Object.keys(cuenta).sort(function (a, b) { return a.localeCompare(b, "es"); })
    .map(function (g) { return { genre: g, total: cuenta[g] }; });
}

/**
 * Categorías del catálogo público. Se piden al arrancar y cada vez que algo
 * puede haberlas cambiado (guardar, borrar, aprobar una publicación): antes se
 * pedían una sola vez y una categoría nueva no aparecía hasta recargar.
 */
function loadGenres() {
  return api("GET", "/api/genres").then(function (d) {
    genres = d.genres || [];
    if (tab !== "mine") renderGenreOptions(genres);
    renderGenreDatalist(genresOf(songs));
  }).catch(function (e) {
    // El filtro se queda vacío, que es tolerable; pero que se sepa por qué.
    renderGenreOptions([]);
    console.warn("No se han podido cargar las categorías:", e.message);
  });
}

function refresh() {
  tabPublic.setAttribute("aria-selected", tab === "public");
  tabMine.setAttribute("aria-selected", tab === "mine");
  tabChords.setAttribute("aria-selected", tab === "chords");
  tabProposals.setAttribute("aria-selected", tab === "proposals");
  tabUsers.setAttribute("aria-selected", tab === "users");
  tabTrash.setAttribute("aria-selected", tab === "trash");
  tabAdmin.setAttribute("aria-selected", tab === "admin");
  if (tab === "chords") { showChords(); return; }
  if (tab === "proposals") { showProposals(); return; }
  if (tab === "users") { showUsers(); return; }
  if (tab === "admin") { showView("admin"); return; }
  showView("list");
  // Listas y favoritos solo tienen sentido sobre lo propio.
  var propio = tab === "mine";
  listasBar.classList.toggle("hidden", !propio);
  playlistFilterWrap.classList.toggle("hidden", !propio);
  favFilter.classList.toggle("hidden", !propio);
  // El catálogo puede ser enorme: género y orden los resuelve SQL. "Mis
  // partituras" son pocas y se ordenan aquí mismo, sin ida y vuelta.
  // Los listados vienen por páginas: sin tope, una cuenta grande se traía
  // el catálogo entero en cada visita.
  var pag = "limit=" + PAGINA + "&offset=" + listOffset;
  var path = tab === "trash"
    ? "/api/songs?trash=1&" + pag
    : tab === "mine"
    ? "/api/songs?" + pag
    : "/api/songs/public?sort=" + encodeURIComponent(sortBy) +
      (genreBy ? "&genre=" + encodeURIComponent(genreBy) : "") + "&" + pag;
  api("GET", path).then(function (d) {
    // Al pedir "más" se añade; al cambiar de pestaña o filtro se empieza de cero.
    songs = listOffset ? songs.concat(d.songs || []) : (d.songs || []);
    listHasMore = !!d.hasMore;
    moreBtn.classList.toggle("hidden", !listHasMore);
    if (tab === "mine" || tab === "trash") songs = sortMine(songs);
    // El filtro ofrece lo que hay delante: en "Mis partituras", tus categorías
    // (también las de las privadas, que el catálogo público no conoce).
    var propias = genresOf(songs);
    if (tab === "mine") renderGenreOptions(propias);
    else renderGenreOptions(genres);
    renderGenreDatalist(propias);
    renderList();
  }).catch(function (e) {
    songs = [];
    renderList(e.message);
  });
}

/** Mismo criterio que el catálogo, aplicado en el navegador. */
function sortMine(lista) {
  var copia = lista.slice();
  if (sortBy === "recent") copia.sort(function (a, b) { return (b.createdAt || 0) - (a.createdAt || 0); });
  else if (sortBy === "old") copia.sort(function (a, b) { return (a.createdAt || 0) - (b.createdAt || 0); });
  else copia.sort(function (a, b) { return (a.title || "").localeCompare(b.title || "", "es"); });
  return copia;
}

function renderList(error) {
  var q = search.value.trim().toLowerCase();
  var propio = tab === "mine";
  var shown = songs.filter(function (s) {
    if (propio && genreBy && (s.genre || "").toLowerCase() !== genreBy.toLowerCase()) return false;
    if (propio && favOnly && !s.favorite) return false;
    if (propio && playlistBy === "none" && s.playlistId) return false;
    if (propio && playlistBy && playlistBy !== "none" && s.playlistId !== playlistBy) return false;
    if (!q) return true;
    return ((s.title || "") + " " + (s.artist || "")).toLowerCase().indexOf(q) >= 0;
  });
  list.innerHTML = "";
  shown.forEach(function (s) {
    var card = document.createElement("button");
    card.className = "card";
    var a = document.createElement("div");
    a.className = "a";
    a.textContent = s.artist || (s.ownerName ? "de " + s.ownerName : "");
    var t = document.createElement("div");
    t.className = "t";
    // El candado es un seguro para quien edita, no información para quien lee:
    // solo aparece en "Mis partituras" y en el editor.
    t.textContent = (tab === "mine" && s.locked ? "🔒 " : "") + (s.title || "(sin título)");
    card.appendChild(a); card.appendChild(t);
    if (s.genre) {
      var g = document.createElement("span");
      g.className = "badge";
      g.textContent = s.genre;
      card.appendChild(g);
    }
    if (propio) {
      var b = document.createElement("span");
      b.className = "badge";
      b.textContent = s.visibility === "public" ? "Pública" : "Privada";
      card.appendChild(b);
      var lista = playlists.filter(function (p) { return p.id === s.playlistId; })[0];
      if (lista) {
        var lb = document.createElement("span");
        lb.className = "badge";
        lb.textContent = lista.name;
        card.appendChild(lb);
      }
      // La estrella va dentro de la tarjeta, que es un <button>: se marca como
      // control aparte y se corta la propagación para no abrir el visor.
      var star = document.createElement("span");
      star.className = "fav";
      star.setAttribute("role", "button");
      star.setAttribute("tabindex", "0");
      star.setAttribute("aria-pressed", !!s.favorite);
      star.setAttribute("aria-label", s.favorite ? "Quitar de favoritas" : "Marcar como favorita");
      star.textContent = s.favorite ? "★" : "☆";
      var alternar = function (ev) {
        ev.stopPropagation();
        ev.preventDefault();
        toggleFavorite(s);
      };
      star.onclick = alternar;
      star.onkeydown = function (ev) {
        if (ev.key === "Enter" || ev.key === " ") alternar(ev);
      };
      card.insertBefore(star, card.firstChild);
    }
    if (tab === "trash") {
      var acc = document.createElement("span");
      acc.className = "acciones";
      var rest = document.createElement("span");
      rest.className = "chip";
      rest.setAttribute("role", "button");
      rest.setAttribute("tabindex", "0");
      rest.textContent = "Restaurar";
      rest.onclick = function (ev) { ev.stopPropagation(); restaurar(s); };
      var borrar = document.createElement("span");
      borrar.className = "chip";
      borrar.setAttribute("role", "button");
      borrar.setAttribute("tabindex", "0");
      borrar.textContent = "Borrar del todo";
      borrar.onclick = function (ev) { ev.stopPropagation(); borrarDelTodo(s); };
      acc.appendChild(rest); acc.appendChild(borrar);
      card.appendChild(acc);
    }
    card.onclick = function () { if (tab !== "trash") openSong(s.id); };
    list.appendChild(card);
  });
  var msg = error ? error
    : shown.length ? ""
    : tab === "trash" ? "La papelera está vacía."
    : (propio ? "Todavía no tienes partituras. Crea la primera con «+ Nueva»."
              : "Aún no hay partituras publicadas.");
  listEmpty.textContent = msg;
  listEmpty.classList.toggle("hidden", !msg);
}

/* ---------- enlaces a una partitura ---------- */
/*
 * La web no cambiaba nunca de URL: no se podía enlazar a una partitura, ni usar
 * el botón «atrás», ni recargar sin volver al principio. Se resuelve con el
 * fragmento (#/cancion/<id>) y no con una ruta de verdad porque el Worker solo
 * sirve la página en «/»: una ruta como /cancion/x daría 404 antes de que el
 * navegador llegara a ejecutar nada.
 */
var RUTA_CANCION = /^#[/]cancion[/]([A-Za-z0-9-]+)$/;

/** Id de partitura que pide la URL actual, o "" si no pide ninguna. */
function cancionDeLaUrl() {
  var m = RUTA_CANCION.exec(location.hash || "");
  return m ? m[1] : "";
}

function enlaceDeCancion(id) {
  return location.origin + location.pathname + location.search + "#/cancion/" + id;
}

/*
 * Al navegar por la interfaz se escribe la URL, y al navegar por la URL (atrás,
 * adelante, pegar un enlace) se mueve la interfaz. La bandera aplicandoUrl corta el
 * bucle: sin él, abrir una partitura desde popstate volvería a empujar estado.
 */
var aplicandoUrl = false;

function ponerUrlDeCancion(id, reemplazar) {
  if (aplicandoUrl) return;
  var destino = id ? enlaceDeCancion(id) : location.origin + location.pathname + location.search;
  if (destino === location.href) return;
  if (reemplazar) history.replaceState({ cancion: id || null }, "", destino);
  else history.pushState({ cancion: id || null }, "", destino);
}

/** Lleva la interfaz a donde diga la URL. Se usa al arrancar y en cada popstate. */
function aplicarUrl() {
  var id = cancionDeLaUrl();
  aplicandoUrl = true;
  try {
    if (id) {
      // Ya abierta la que toca: no hay nada que hacer. Hay que mirar TAMBIÉN si
      // el visor está abierto: current sigue apuntando a la última partitura
      // después de cerrarlo, así que sin esto «adelante» no la reabría.
      var yaEstá = viewer.classList.contains("on") &&
                   current && current.song && current.song.id === id;
      if (!yaEstá) openSong(id, { desdeUrl: true });
    } else if (viewer.classList.contains("on")) {
      closeViewer();
    }
  } finally {
    aplicandoUrl = false;
  }
}

window.addEventListener("popstate", aplicarUrl);

/** Comparte el enlace: menú del sistema si lo hay, y si no, al portapapeles. */
function compartirCancion() {
  if (!current || !current.song) return;
  var enlace = enlaceDeCancion(current.song.id);
  var privada = current.song.visibility !== "public";

  function avisar(texto) {
    var antes = vShare.textContent;
    vShare.textContent = texto;
    setTimeout(function () { vShare.textContent = antes; }, 2200);
  }

  // Quien reciba el enlace de una privada se encontrará un 404: la API no
  // enseña lo privado a nadie más. Mejor decirlo al compartir que después.
  if (privada && !confirm(
        "Esta partitura es privada: solo tú puedes abrirla.\\n\\n" +
        "Para que el enlace le sirva a otra persona hay que publicarla " +
        "(desde el editor, «Proponer publicación»).\\n\\n¿Copiar el enlace igualmente?")) {
    return;
  }

  if (navigator.share) {
    navigator.share({ title: current.song.title || "Partitura", url: enlace })
      .catch(function () { /* cancelar el menú del sistema no es un error */ });
    return;
  }
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(enlace)
      .then(function () { avisar("✓ Enlace copiado"); })
      .catch(function () { prompt("Copia el enlace:", enlace); });
    return;
  }
  prompt("Copia el enlace:", enlace);
}

/* ---------- favoritos y papelera ---------- */

function toggleFavorite(s) {
  api("PUT", "/api/songs/" + s.id + "/favorite", { favorite: !s.favorite })
    .then(function (d) {
      s.favorite = !!(d.song && d.song.favorite);
      renderList();
    })
    .catch(function (e) { aviso(listEmpty, e.message); });
}

function restaurar(s) {
  api("POST", "/api/songs/" + s.id + "/restore")
    .then(refresh)
    .catch(function (e) { aviso(listEmpty, e.message); });
}

function borrarDelTodo(s) {
  // Este sí es irreversible, y es el único sitio donde puede ocurrir.
  if (!confirm("Se borrará «" + (s.title || "sin título") + "» para siempre. Esto no se puede deshacer. ¿Continuar?")) return;
  api("DELETE", "/api/songs/" + s.id + "?hard=1")
    .then(refresh)
    .catch(function (e) { aviso(listEmpty, e.message); });
}

/* ---------- impresión ---------- */
/*
 * Imprime lo que se está viendo: con el tono transpuesto y la cejilla puestos,
 * no el texto original. Se abre una ventana con estilos propios de papel
 * (tinta oscura sobre blanco) en vez de imprimir la interfaz oscura.
 * Portado del panel /admin, que era el único sitio que sabía imprimir.
 */
function printViewer() {
  if (!current) return;
  var w = window.open("", "_blank");
  if (!w) { alert("El navegador ha bloqueado la ventana de impresión."); return; }
  // current.body ya es lo que se está leyendo: el Original o la versión
  // elegida. La cejilla se saca del propio rótulo, que es quien la sabe.
  var lineas = vRenderSong(vTransposeBody(current.body || "", semis, flats));
  var capo = Number((vCapo.textContent || "").replace(/\D+/g, "")) || 0;
  var doc = '<!doctype html><meta charset="utf-8"><title>' +
    vEsc(current.song.title || "Partitura") + '</title>' +
    '<style>body{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:13px;' +
    'padding:20px;color:#111;background:#fff;}' +
    'h1{font-size:18px;margin:0;}h2{font-size:13px;color:#555;margin:2px 0 10px;font-weight:normal;}' +
    '.chord{color:#1558d6;font-weight:bold;}.ln{white-space:pre;margin:0;line-height:1.35;}' +
    '.tab{white-space:pre;color:#666;line-height:1.35;}' +
    '.capo{display:inline-block;border:1px solid #888;border-radius:4px;padding:1px 6px;' +
    'margin-bottom:10px;font-size:12px;}</style>' +
    '<h1>' + vEsc(current.song.title || "") + '</h1>' +
    (current.song.artist ? '<h2>' + vEsc(current.song.artist) + '</h2>' : '') +
    (capo > 0 ? '<div class="capo">Capo ' + capo + '</div>' : '') +
    '<div>' + lineas + '</div>';
  w.document.write(doc);
  w.document.close();
  w.focus();
  setTimeout(function () { w.print(); }, 250);
}

/* ---------- administración del catálogo ---------- */
/*
 * Lo que sobrevive del panel /admin, ya retirado: copia de seguridad en ZIP,
 * repaso de vídeos y categorías automáticas. Ahora todo pasa por la sesión y
 * por el modelo de permisos, en vez de por un token compartido que se saltaba
 * las dos cosas.
 */

function nombreFichero(s) {
  var base = (s.title || "sin-titulo").replace(/[\\/:*?"<>|]+/g, "-").slice(0, 80);
  return base + "-" + s.id.slice(0, 8) + ".txt";
}

function backupZip() {
  aviso(adminMsg, "Preparando copia…");
  api("GET", "/api/songs").then(function (d) {
    var lista = d.songs || [];
    if (!lista.length) { aviso(adminMsg, "No hay partituras que guardar."); return; }
    var enc = new TextEncoder();
    var entradas = [];
    // Una petición por partitura: es una operación manual y puntual, y así no
    // hace falta un endpoint nuevo que vuelque todo el catálogo de golpe.
    var cadena = lista.reduce(function (previa, s) {
      return previa.then(function () {
        return api("GET", "/api/songs/" + s.id).then(function (det) {
          entradas.push({ name: nombreFichero(s), data: enc.encode(det.content || "") });
          aviso(adminMsg, "Preparando copia… " + entradas.length + "/" + lista.length);
        });
      });
    }, Promise.resolve());
    return cadena.then(function () {
      var d2 = new Date();
      var p2 = function (n) { return String(n).padStart(2, "0"); };
      var a = document.createElement("a");
      a.href = URL.createObjectURL(vBuildZip(entradas));
      a.download = "vivace-" + d2.getFullYear() + p2(d2.getMonth() + 1) + p2(d2.getDate()) +
                   "-" + p2(d2.getHours()) + p2(d2.getMinutes()) + ".zip";
      a.click();
      URL.revokeObjectURL(a.href);
      aviso(adminMsg, "Copia de " + entradas.length + " partituras ✓");
    });
  }).catch(function (e) { aviso(adminMsg, "Error en la copia: " + e.message); });
}

function restoreZip(file) {
  aviso(adminMsg, "Leyendo ZIP…");
  file.arrayBuffer()
    .then(vReadZip)
    .then(function (entradas) {
      var textos = entradas.filter(function (e) { return /\.txt$/i.test(e.name); });
      if (!textos.length) { aviso(adminMsg, "El ZIP no trae partituras."); return; }
      if (!confirm("Se crearán " + textos.length + " partituras a partir del ZIP. ¿Continuar?")) {
        aviso(adminMsg, "");
        return;
      }
      var ok = 0, fallos = 0;
      return textos.reduce(function (previa, e) {
        return previa.then(function () {
          var cab = vParseSong(e.text).head || {};
          return api("POST", "/api/songs", {
            title: cab.title || e.name.replace(/\.txt$/i, ""),
            artist: cab.artist || "",
            genre: cab.genre || "",
            capo: Number(cab.capo) || 0,
            sourceUrl: cab.url || "",
            content: e.text
          }).then(function () { ok++; }, function () { fallos++; })
            .then(function () {
              aviso(adminMsg, "Restaurando… " + (ok + fallos) + "/" + textos.length);
            });
        });
      }, Promise.resolve()).then(function () {
        aviso(adminMsg, "Restauradas " + ok + (fallos ? ", fallidas " + fallos : "") + " ✓");
        refresh();
      });
    })
    .catch(function (e) { aviso(adminMsg, "No se ha podido leer el ZIP: " + e.message); });
}

function listarSinVideo() {
  noVideoList.textContent = "Buscando…";
  api("GET", "/api/songs/without-video").then(function (d) {
    var lista = d.songs || [];
    noVideoList.innerHTML = "";
    if (!lista.length) { noVideoList.textContent = "Todas tienen vídeo."; return; }
    lista.forEach(function (s) {
      var fila = document.createElement("div");
      fila.className = "row";
      var t = document.createElement("span");
      t.style.flex = "1";
      t.textContent = (s.artist ? s.artist + " – " : "") + (s.title || "(sin título)");
      var buscar = document.createElement("a");
      buscar.href = s.search || "#";
      buscar.target = "_blank";
      buscar.rel = "noopener noreferrer";
      buscar.textContent = "Buscar en YouTube ↗";
      fila.appendChild(t); fila.appendChild(buscar);
      noVideoList.appendChild(fila);
    });
  }).catch(function (e) { noVideoList.textContent = e.message; });
}

function categoriasAuto(aplicar) {
  aviso(genresMsg, aplicar ? "Aplicando…" : "Calculando…");
  api("POST", "/api/genres/auto", { dryRun: !aplicar }).then(function (d) {
    var cuenta = d.tally || {};
    var resumen = Object.keys(cuenta).map(function (g) { return g + ": " + cuenta[g]; }).join(" · ");
    var cuantas = aplicar ? d.updated : d.wouldUpdate;
    var cola = d.done ? "" : " (quedan más; repite para seguir)";
    aviso(
      genresMsg,
      (aplicar ? "Aplicadas " : "Se cambiarían ") + cuantas + " de " + d.scanned +
      (resumen ? " · " + resumen : "") + cola
    );
    genresApplyBtn.classList.toggle("hidden", aplicar || !cuantas);
    if (aplicar) { loadGenres(); refresh(); }
  }).catch(function (e) { aviso(genresMsg, e.message); });
}

/* ---------- visor ---------- */
function openSong(id, opciones) {
  var desdeUrl = !!(opciones && opciones.desdeUrl);
  api("GET", "/api/songs/" + id).then(function (d) {
    current = d;
    semis = 0; flats = false;
    var parsed = vParseSong(d.content);
    current.body = parsed.body || d.content;
    // Autor – título en una línea; sin autor, el guion sobra.
    vArtist.textContent = d.song.artist || "";
    vSep.classList.toggle("hidden", !d.song.artist);
    vTitle.textContent = d.song.title || "(sin título)";
    setCapo(d.song.capo);
    // Solo http(s): un href con javascript: en la cabecera sería un agujero.
    var origen = String(d.song.sourceUrl || "");
    var valida = vUrlSegura(origen);
    vSource.href = valida ? origen : "#";
    vSource.classList.toggle("hidden", !valida);
    vEdit.classList.toggle("hidden", !(user && (user.id === d.song.ownerId || user.role === "admin")));
    renderViewer();
    viewer.classList.add("on");
    // El scroll se reinicia DESPUÉS de mostrar el visor. Mientras está en
    // display:none no tiene caja, así que asignarle scrollTop no hace nada y el
    // navegador restaura la posición anterior al mostrarlo: al abrir la segunda
    // partitura se entraba por el final de la primera.
    vBody.scrollTop = 0;
    document.documentElement.classList.add("conVisor");
    document.body.classList.add("conVisor");
    renderTube();
    loadVersions(d.song.id);
    loadRatings(d.song.id);
    loadComments(d.song.id);
    // Al abrirla desde un enlace se REEMPLAZA el estado: si se empujara, el
    // botón «atrás» devolvería a la misma página en vez de salir de ella.
    ponerUrlDeCancion(d.song.id, desdeUrl);
  }).catch(function (e) {
    if (desdeUrl) {
      // Llegó por un enlace: el mensaje crudo de la API no dice qué hacer.
      aviso(listEmpty, "No se ha podido abrir esa partitura: " + e.message +
        ". Puede ser privada, o haber sido borrada.");
      ponerUrlDeCancion("", true);
    } else {
      alert(e.message);
    }
  });
}

/** Capo de lo que se está mirando; con 0 no se enseña nada. */
function setCapo(capo) {
  var n = Number(capo) || 0;
  vCapo.innerHTML = n > 0 ? 'Capo <span class="n">' + n + '</span>' : "";
  vCapo.classList.toggle("hidden", n <= 0);
}

function renderViewer() {
  vTone.textContent = (semis > 0 ? "+" : semis < 0 ? "" : "±") + semis;
  vFlat.style.fontWeight = flats ? "700" : "400";
  var keep = vBody.scrollTop;
  vSheet.innerHTML = '<div class="sheet">' +
                     vRenderSong(vTransposeBody(current.body, semis, flats)) + "</div>";
  vBody.style.setProperty("--fs", fontSize + "px");
  vBody.scrollTop = keep;
  renderChordBar();
}

function closeViewer() {
  // Vaciar el marco para que el vídeo deje de sonar al salir.
  vTube.classList.remove("on");
  vTube.querySelector(".marco").innerHTML = "";
  document.documentElement.classList.remove("conVisor");
  document.body.classList.remove("conVisor");
  vChordBar.classList.add("hidden");
  stopScroll();
  if (metro.isRunning()) toggleMetro();
  viewer.classList.remove("on");
  ponerUrlDeCancion("");
}

function step(ts) {
  if (!scrolling) return;
  var dt = lastTs ? ts - lastTs : 0;
  lastTs = ts;
  remainder += (+vSpeed.value) * dt / 1000;
  var px = Math.floor(remainder);
  if (px > 0) {
    remainder -= px;
    var max = vBody.scrollHeight - vBody.clientHeight;
    vBody.scrollTop = Math.min(vBody.scrollTop + px, max);
    if (vBody.scrollTop >= max) { stopScroll(); return; }
  }
  raf = requestAnimationFrame(step);
}
function startScroll() {
  if (scrolling) return;
  scrolling = true; lastTs = 0; remainder = 0;
  vPlay.textContent = "⏸ Scroll";
  if (navigator.wakeLock) {
    navigator.wakeLock.request("screen").then(function (w) { wakeLock = w; }).catch(function () {});
  }
  raf = requestAnimationFrame(step);
}
function stopScroll() {
  scrolling = false;
  vPlay.textContent = "▶ Scroll";
  if (raf) { cancelAnimationFrame(raf); raf = 0; }
  if (wakeLock) { try { wakeLock.release(); } catch (e) {} wakeLock = null; }
}

function renderBeats(beat) {
  vBeats.innerHTML = "";
  for (var i = 1; i <= metro.beatsPerBar; i++) {
    var d = document.createElement("span");
    d.className = "beat" + (beat === i ? " on" : "");
    vBeats.appendChild(d);
  }
}
function toggleMetro() {
  if (metro.isRunning()) {
    metro.stop();
    vMetro.textContent = "♩ Metrónomo";
  } else {
    metro.bpm = +vBpm.value;
    metro.start();
    vMetro.textContent = "■ Metrónomo";
  }
}
metro.onBeat = renderBeats;

/**
 * Reproductor de la canción. El iframe solo se crea si hay vídeo: así no se
 * carga nada de YouTube en las partituras que no lo tienen.
 */
function renderTube() {
  var marco = vTube.querySelector(".marco");
  var enlace = current && current.song ? current.song.youtubeUrl : "";
  var incrustado = vEmbedUrl(enlace);
  if (!incrustado) {
    vTube.classList.remove("on");
    marco.innerHTML = "";
    return;
  }
  // Se compara antes de reescribir: reasignar el src reinicia el vídeo, y
  // transponer o cambiar de versión no debería cortar la música.
  var actual = marco.querySelector("iframe");
  if (actual && actual.getAttribute("src") === incrustado) {
    vTube.classList.add("on");
    return;
  }
  marco.innerHTML = '<iframe src="' + vEsc(incrustado) + '" title="Vídeo de la canción" ' +
    'allow="accelerometer; clipboard-write; encrypted-media; gyroscope; picture-in-picture" ' +
    'referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>';
  vTube.classList.add("on");
}

/* ---------- versiones ---------- */

/**
 * Una versión es un arreglo alternativo de la misma partitura: otro tono, otra
 * cejilla, tablatura… El contenido propio de la partitura es el "Original" y
 * las demás cuelgan de él, igual que en la app.
 */
/*
 * Las piezas secundarias del visor se cargaban tragándose el error: si el
 * servidor fallaba, la sección se quedaba vacía y era indistinguible de "no hay
 * nada". Ahora se pinta lo que haya Y se dice que falló, sin bloquear la
 * lectura de la partitura, que es lo que de verdad importa.
 */
function loadVersions(songId) {
  versions = [];
  currentVersion = null;
  api("GET", "/api/songs/" + songId + "/versions").then(function (d) {
    versions = d.versions || [];
    renderVersionBar();
  }).catch(function (e) {
    renderVersionBar();
    falloSecundario(vVersions, "No se han podido cargar las versiones: " + e.message);
  });
}

/** Nota de error al final de una sección, sin robar el sitio a lo que sí cargó. */
function falloSecundario(caja, texto) {
  var n = document.createElement("div");
  n.className = "msg";
  n.setAttribute("role", "status");
  n.textContent = texto;
  caja.appendChild(n);
}

/**
 * Estrellas de una versión. Con sesión son pulsables (y volver a pulsar la que
 * ya tenías marcada retira el voto); sin sesión son solo el dibujo de la media.
 */
function starsFor(versionId) {
  var caja = document.createElement("span");
  caja.className = "stars";
  var mio = myRatings[versionId] || 0;
  var media = (ratings[versionId] && ratings[versionId].average) || 0;
  // Marcadas: las tuyas si has votado; si no, la media redondeada.
  var marcadas = mio || Math.round(media);
  for (var i = 1; i <= 5; i++) {
    (function (valor) {
      var e = document.createElement(user ? "button" : "span");
      e.textContent = valor <= marcadas ? "★" : "☆";
      e.className = valor <= marcadas ? "on" : "";
      if (user) {
        e.title = mio === valor ? "Quitar mi voto" : "Valorar con " + valor;
        e.onclick = function (ev) {
          ev.stopPropagation();          // no cambiar de versión al votar
          rateVersion(versionId, mio === valor ? 0 : valor);
        };
      }
      caja.appendChild(e);
    })(i);
  }
  return caja;
}

/**
 * Panel de versiones. Sale siempre, aunque solo esté el Original: es donde se
 * ve la valoración de lo que estás tocando.
 */
function renderVersionBar() {
  if (!current) return;
  var puedeEditar = !!user && (user.id === current.song.ownerId || esEditor());
  var puedeProponer = !!user && !puedeEditar && current.song.visibility === "public";

  var filas = [{ id: "", nombre: "Original", capo: current.song.capo, autor: "" }].concat(
    versions.map(function (v) {
      return { id: v.id, nombre: v.name || "Sin nombre", capo: v.capo, autor: v.authorName || "" };
    })
  );

  vVersions.classList.remove("hidden");
  vVersions.innerHTML = "";
  filas.forEach(function (f, indice) {
    var fila = document.createElement("button");
    fila.className = "vRow";
    fila.setAttribute("aria-pressed", (currentVersion || "") === f.id);
    fila.onclick = function () { showVersion(f.id || null); };

    var cab = document.createElement("div");
    cab.className = "num";
    cab.textContent = "Versión " + (indice + 1) +
                      (f.capo ? " · capo " + f.capo : "") +
                      (f.autor ? " · " + f.autor : "");
    var nombre = document.createElement("div");
    nombre.className = "nm";
    nombre.textContent = f.nombre;

    var val = document.createElement("div");
    val.className = "val";
    val.appendChild(starsFor(f.id));
    var nota = document.createElement("span");
    nota.className = "nota";
    var datos = ratings[f.id];
    // Sin votos se enseña 0.0 igualmente: el hueco vacío confunde más.
    nota.textContent = (datos ? datos.average.toFixed(1) : "0.0") + " / 5" +
                       (datos && datos.count ? " (" + datos.count + ")" : "");
    val.appendChild(nota);

    fila.appendChild(cab);
    fila.appendChild(nombre);
    fila.appendChild(val);
    vVersions.appendChild(fila);
  });

  vVersionActions.innerHTML = "";
  if (puedeEditar) {
    var nueva = document.createElement("button");
    nueva.textContent = "+ Versión";
    nueva.onclick = function () { newVersion(); };
    vVersionActions.appendChild(nueva);
    if (currentVersion) {
      var editar = document.createElement("button");
      editar.textContent = "Editar";
      editar.onclick = function () { editVersion(currentVersion); };
      vVersionActions.appendChild(editar);
      var borrar = document.createElement("button");
      borrar.textContent = "Eliminar";
      borrar.style.color = "var(--vv-danger)";
      borrar.onclick = function () { deleteVersion(currentVersion); };
      vVersionActions.appendChild(borrar);
    }
  }
  if (puedeProponer) {
    var propon = document.createElement("button");
    propon.textContent = "Proponer versión";
    propon.title = "Un editor la revisará antes de que aparezca";
    propon.onclick = function () { proposeVersion(); };
    vVersionActions.appendChild(propon);
  }
}

/* ---------- valoraciones ---------- */

function loadRatings(songId) {
  ratings = {}; myRatings = {};
  api("GET", "/api/songs/" + songId + "/ratings").then(function (d) {
    ratings = d.ratings || {};
    myRatings = d.mine || {};
    renderVersionBar();
  }).catch(function () { renderVersionBar(); });
}

function rateVersion(versionId, stars) {
  api("PUT", "/api/songs/" + current.song.id + "/ratings", {
    versionId: versionId, stars: stars
  }).then(function (d) {
    ratings = d.ratings || {};
    myRatings = d.mine || {};
    renderVersionBar();
  }).catch(function (e) { alert(e.message); });
}

/* ---------- comentarios ---------- */

function loadComments(songId) {
  comments = [];
  api("GET", "/api/songs/" + songId + "/comments").then(function (d) {
    comments = d.comments || [];
    renderComments();
  }).catch(function (e) {
    renderComments();
    falloSecundario(vComments, "No se han podido cargar los comentarios: " + e.message);
  });
}

function renderComments() {
  vComments.innerHTML = "";
  if (!current) return;
  var titulo = document.createElement("h4");
  titulo.textContent = "Comentarios (" + comments.length + ")";
  vComments.appendChild(titulo);

  comments.forEach(function (c) {
    var caja = document.createElement("div");
    caja.className = "comentario";
    var quien = document.createElement("div");
    quien.className = "quien";
    var nombre = document.createElement("span");
    nombre.textContent = c.authorName || "Alguien";
    var cuando = document.createElement("span");
    cuando.textContent = new Date(c.createdAt).toLocaleDateString("es");
    quien.appendChild(nombre);
    quien.appendChild(cuando);
    // Borrar: quien lo escribió y el equipo editorial. El servidor lo repite.
    if (user && (c.authorId === user.id || esEditor())) {
      var quitar = document.createElement("button");
      quitar.className = "quitar";
      quitar.textContent = "Borrar";
      quitar.onclick = function () { deleteComment(c.id); };
      quien.appendChild(quitar);
    }
    var texto = document.createElement("div");
    texto.className = "texto";
    texto.textContent = c.body;
    caja.appendChild(quien);
    caja.appendChild(texto);
    vComments.appendChild(caja);
  });

  if (!comments.length) {
    var vacio = document.createElement("div");
    vacio.className = "nota";
    vacio.textContent = "Todavía no hay comentarios.";
    vComments.appendChild(vacio);
  }

  if (!user) {
    var aviso = document.createElement("div");
    aviso.className = "nota";
    aviso.style.marginTop = "12px";
    aviso.textContent = "Entra en tu cuenta para comentar.";
    vComments.appendChild(aviso);
    return;
  }
  var form = document.createElement("div");
  form.id = "vCommentForm";
  var area = document.createElement("textarea");
  area.placeholder = "Escribe un comentario…";
  area.maxLength = 2000;
  var fila = document.createElement("div");
  fila.className = "row";
  var enviar = document.createElement("button");
  enviar.className = "primary";
  enviar.textContent = "Publicar";
  enviar.onclick = function () {
    var texto = area.value.trim();
    if (!texto) return;
    enviar.disabled = true;
    api("POST", "/api/songs/" + current.song.id + "/comments", { body: texto })
      .then(function () { area.value = ""; loadComments(current.song.id); })
      .catch(function (e) { alert(e.message); })
      .then(function () { enviar.disabled = false; });
  };
  fila.appendChild(enviar);
  form.appendChild(area);
  form.appendChild(fila);
  vComments.appendChild(form);
}

function deleteComment(id) {
  if (!confirm("¿Borrar este comentario?")) return;
  api("DELETE", "/api/comments/" + id)
    .then(function () { loadComments(current.song.id); })
    .catch(function (e) { alert(e.message); });
}

/** Cambia lo que se lee sin salir del visor: se conservan tono y tamaño. */
function showVersion(id) {
  if (!id) {
    currentVersion = null;
    setCapo(current.song.capo);
    var parsed = vParseSong(current.content);
    current.body = parsed.body || current.content;
    renderViewer();
    renderVersionBar();
    return;
  }
  api("GET", "/api/versions/" + id).then(function (d) {
    currentVersion = id;
    var p = vParseSong(d.content);
    current.body = p.body || d.content;
    setCapo(d.version.capo);
    renderViewer();
    renderVersionBar();
    vBody.scrollTop = 0;
  }).catch(function (e) { alert(e.message); });
}

function newVersion() {
  editorMode = "version";
  editingVersionId = null;
  editingId = current.song.id;
  eVersionName.value = "";
  eSource.value = "";
  editCapo = Number(current.song.capo) || 0;
  renderCapoPills();
  eContent.value = current.body || "";
  renderEditorPreview();
  closeViewer();
  applyEditorMode();
  showEdit(true);
}

function editVersion(id) {
  var v = versions.filter(function (x) { return x.id === id; })[0];
  if (!v) return;
  api("GET", "/api/versions/" + id).then(function (d) {
    editorMode = "version";
    editingVersionId = id;
    editingId = current.song.id;
    eVersionName.value = v.name || "";
    eSource.value = v.sourceUrl || "";
    editCapo = Number(v.capo) || 0;
    renderCapoPills();
    eContent.value = d.content || "";
    renderEditorPreview();
    closeViewer();
    applyEditorMode();
    showEdit(true);
  }).catch(function (e) { alert(e.message); });
}

function deleteVersion(id) {
  if (!confirm("¿Eliminar esta versión? La partitura original no se toca.")) return;
  api("DELETE", "/api/versions/" + id).then(function () {
    currentVersion = null;
    showVersion(null);
    loadVersions(current.song.id);
  }).catch(function (e) { alert(e.message); });
}

/** Proponer una versión: no toca nada hasta que alguien la apruebe. */
function proposeVersion() {
  editorMode = "proposal";
  proposalTarget = current.song.id;
  editingVersionId = null;
  eVersionName.value = "";
  eSource.value = "";
  editCapo = Number(current.song.capo) || 0;
  renderCapoPills();
  eContent.value = current.body || "";
  eNote.value = "";
  renderEditorPreview();
  closeViewer();
  applyEditorMode();
  showEdit(true);
}

/* ---------- propuestas ---------- */

var ETIQUETA_ESTADO = {
  pending: "pendiente", approved: "aprobada", rejected: "rechazada", withdrawn: "retirada"
};

function showProposals() {
  showView("proposals");
  propMineWrap.classList.toggle("hidden", !esEditor());   // quien no revisa solo ve las suyas
  loadProposals();
}

function loadProposals() {
  propMsg.textContent = "";
  var q = "?status=" + encodeURIComponent(propStatus.value);
  if (esEditor() && propMine.checked) q += "&mine=1";
  api("GET", "/api/proposals" + q).then(function (d) {
    proposals = d.proposals || [];
    renderProposals();
  }).catch(function (e) {
    proposals = [];
    renderProposals(e.message);
  });
}

function renderProposals(error) {
  propList.innerHTML = "";
  propCount.textContent = proposals.length
    ? proposals.length + (proposals.length === 1 ? " propuesta" : " propuestas")
    : "";
  proposals.forEach(function (p) {
    var fila = document.createElement("div");
    fila.className = "fila";

    var cuerpo = document.createElement("div");
    cuerpo.className = "cuerpo";
    var titulo = document.createElement("div");
    titulo.className = "t";
    titulo.textContent = (p.kind === "publish" ? "Publicar: " : "Versión: ") +
                         (p.kind === "publish" ? (p.songTitle || "(sin título)")
                                               : (p.name || "sin nombre"));
    var meta = document.createElement("div");
    meta.className = "meta";
    var partes = [];
    if (p.kind === "version") partes.push("sobre «" + (p.songTitle || "?") + "»");
    if (p.authorName) partes.push("de " + p.authorName);
    if (p.capo) partes.push("capo " + p.capo);
    partes.push(new Date(p.createdAt).toLocaleDateString("es"));
    meta.textContent = partes.join(" · ");
    cuerpo.appendChild(titulo);
    cuerpo.appendChild(meta);
    if (p.note) {
      var nota = document.createElement("div");
      nota.className = "nota";
      nota.textContent = "«" + p.note + "»";
      cuerpo.appendChild(nota);
    }
    if (p.reviewNote) {
      var revision = document.createElement("div");
      revision.className = "meta";
      revision.textContent = "Revisión: " + p.reviewNote;
      cuerpo.appendChild(revision);
    }
    fila.appendChild(cuerpo);

    var estado = document.createElement("span");
    estado.className = "estado";
    estado.setAttribute("data-s", p.status);
    estado.textContent = ETIQUETA_ESTADO[p.status] || p.status;
    fila.appendChild(estado);

    var acciones = document.createElement("div");
    acciones.className = "row";
    if (p.kind === "version") {
      var ver = document.createElement("button");
      ver.textContent = "Ver";
      ver.onclick = function () { openProposal(p); };
      acciones.appendChild(ver);
    }
    var abrir = document.createElement("button");
    abrir.textContent = "Partitura";
    abrir.onclick = function () { openSong(p.songId); };
    acciones.appendChild(abrir);

    if (p.status === "pending" && esEditor()) {
      var aprobar = document.createElement("button");
      aprobar.className = "primary";
      aprobar.textContent = "Aprobar";
      aprobar.onclick = function () { resolveProposal(p, "approve"); };
      acciones.appendChild(aprobar);
      var rechazar = document.createElement("button");
      rechazar.textContent = "Rechazar";
      rechazar.style.color = "var(--vv-danger)";
      rechazar.onclick = function () { resolveProposal(p, "reject"); };
      acciones.appendChild(rechazar);
    }
    if (p.status === "pending" && user && p.authorId === user.id) {
      var retirar = document.createElement("button");
      retirar.textContent = "Retirar";
      retirar.onclick = function () {
        if (!confirm("¿Retirar esta propuesta?")) return;
        api("DELETE", "/api/proposals/" + p.id)
          .then(loadProposals)
          .catch(function (e) { propMsg.textContent = e.message; });
      };
      acciones.appendChild(retirar);
    }
    fila.appendChild(acciones);
    propList.appendChild(fila);
  });

  var msg = error ? error
    : proposals.length ? ""
    : esEditor() ? "No hay propuestas con ese estado."
                 : "Todavía no has enviado ninguna propuesta.";
  propEmpty.textContent = msg;
  propEmpty.classList.toggle("hidden", !msg);
}

/** Aprobar aplica el cambio; rechazar pide el motivo, que le llega al autor. */
function resolveProposal(p, accion) {
  var nota = "";
  if (accion === "reject") {
    nota = prompt("Motivo del rechazo (lo verá quien la propuso):");
    if (nota === null) return;
  } else if (!confirm(p.kind === "publish"
      ? "¿Publicar «" + (p.songTitle || "") + "» en el catálogo? La verá cualquiera."
      : "¿Añadir esta versión a la partitura?")) {
    return;
  }
  api("POST", "/api/proposals/" + p.id + "/" + accion, { note: nota }).then(function () {
    loadProposals();
    loadGenres();
  }).catch(function (e) { propMsg.textContent = e.message; });
}

/** Enseña el texto propuesto tal y como quedaría, sin aplicarlo. */
function openProposal(p) {
  api("GET", "/api/proposals/" + p.id).then(function (d) {
    pmTitle.textContent = p.name || "Versión propuesta";
    pmMeta.textContent = (p.authorName ? "de " + p.authorName : "") +
                         (p.capo ? " · capo " + p.capo : "");
    var parsed = vParseSong(d.content || "");
    pmBody.innerHTML = '<div class="sheet">' + vRenderSong(parsed.body || d.content || "") + '</div>';
    abrirDialogo(propModal);
  }).catch(function (e) { propMsg.textContent = e.message; });
}

/* ---------- usuarios y roles ---------- */

function showUsers() {
  showView("users");
  loadUsers();
}

function loadUsers() {
  usersMsg.textContent = "";
  api("GET", "/api/users").then(function (d) {
    renderUsers(d.users || []);
  }).catch(function (e) { usersMsg.textContent = e.message; });
}

function renderUsers(lista) {
  usersList.innerHTML = "";
  lista.forEach(function (u) {
    var fila = document.createElement("div");
    fila.className = "fila";
    var cuerpo = document.createElement("div");
    cuerpo.className = "cuerpo";
    var nombre = document.createElement("div");
    nombre.className = "t";
    nombre.textContent = u.name || u.email;
    var correo = document.createElement("div");
    correo.className = "meta";
    correo.textContent = u.email;
    cuerpo.appendChild(nombre);
    cuerpo.appendChild(correo);
    fila.appendChild(cuerpo);

    var select = document.createElement("select");
    select.style.width = "auto";
    [["user", "Usuario"], ["editor", "Editor"], ["admin", "Administrador"]].forEach(function (par) {
      var o = document.createElement("option");
      o.value = par[0];
      o.textContent = par[1];
      if (u.role === par[0]) o.selected = true;
      select.appendChild(o);
    });
    // El servidor también lo rechaza; aquí se evita el viaje y la confusión.
    var esYo = user && u.id === user.id;
    select.disabled = esYo;
    select.title = esYo ? "No puedes cambiar tu propio rol" : "";
    select.onchange = function () {
      api("PUT", "/api/users/" + u.id + "/role", { role: select.value })
        .then(loadUsers)
        .catch(function (e) { usersMsg.textContent = e.message; loadUsers(); });
    };
    fila.appendChild(select);
    usersList.appendChild(fila);
  });
}

/* ---------- acordes ---------- */

/**
 * El diccionario global es el mismo para todo el mundo y cambia poco, así que
 * se pide una vez por carga de página y se guarda en memoria.
 */
function loadChords() {
  if (chordDict) return Promise.resolve(chordDict);
  return api("GET", "/api/chords/global").then(function (d) {
    chordDict = d.chords || {};
    return chordDict;
  }).catch(function () {
    chordDict = {};
    return chordDict;
  });
}

/** Posiciones de un acorde; [] si no está en el diccionario. */
function chordPositionsOf(name) {
  var entrada = chordDict && chordDict[name];
  if (!entrada) return [];
  return entrada.positions || [];
}

/**
 * Barra de diagramas del visor. Los nombres se transponen con la partitura:
 * si estás tocando dos tonos arriba, los diagramas son los de ahí.
 */
function renderChordBar() {
  if (!chordBarOn || !current) { vChordBar.classList.add("hidden"); return; }
  vChordBar.classList.remove("hidden");
  var nombres = vSongChords(current.body).map(function (n) {
    return vTransposeChord(n, semis, flats);
  });
  vChordBar.innerHTML = "";
  var pintados = 0;
  nombres.forEach(function (nombre) {
    var posiciones = chordPositionsOf(nombre);
    if (!posiciones.length) return;
    pintados++;
    var b = document.createElement("button");
    b.innerHTML = vChordSvg(posiciones[0], 78) + '<span class="nm">' + vEsc(nombre) + '</span>';
    b.title = posiciones.length > 1 ? posiciones.length + " digitaciones" : "";
    b.onclick = function () { openChordModal(nombre); };
    vChordBar.appendChild(b);
  });
  if (!pintados) {
    var aviso = document.createElement("span");
    aviso.className = "none";
    aviso.textContent = nombres.length
      ? "Ningún acorde de esta partitura está en el diccionario todavía."
      : "Esta partitura no lleva acordes marcados.";
    vChordBar.appendChild(aviso);
  }
}

function toggleChordBar() {
  chordBarOn = !chordBarOn;
  vChords.textContent = chordBarOn ? "♦ Ocultar acordes" : "♦ Acordes";
  if (chordBarOn) loadChords().then(renderChordBar);
  else renderChordBar();
}

/** Todas las digitaciones de un acorde, para mirarlas de cerca. */
function openChordModal(nombre) {
  loadChords().then(function () {
    cmName.textContent = nombre;
    cmBody.innerHTML = "";
    var posiciones = chordPositionsOf(nombre);
    posiciones.forEach(function (pos, i) {
      var caja = document.createElement("div");
      caja.className = "chordCard";
      caja.innerHTML = vChordSvg(pos, 116) +
                       '<span class="va">' + (i + 1) + " de " + posiciones.length + "</span>";
      cmBody.appendChild(caja);
    });
    cmEmpty.textContent = posiciones.length
      ? ""
      : "Este acorde no está en el diccionario global todavía.";
    cmEmpty.classList.toggle("hidden", !!posiciones.length);
    abrirDialogo(chordModal);
  });
}

/* ---------- diccionario: administración ---------- */

function showChords() {
  showView("chords");
  loadChords().then(renderChordList);
}

function renderChordList() {
  var q = chordSearch.value.trim().toLowerCase();
  var nombres = Object.keys(chordDict || {}).sort();
  var vistos = nombres.filter(function (n) { return !q || n.toLowerCase().indexOf(q) >= 0; });
  chordCount.textContent = nombres.length + " acordes en el diccionario";
  chordList.innerHTML = "";
  // Con el diccionario base entero son cientos de tarjetas: sin buscar se
  // enseña un aperitivo y se pide filtrar, que si no el navegador sufre.
  var tope = q ? 400 : 60;
  vistos.slice(0, tope).forEach(function (nombre) {
    var posiciones = chordPositionsOf(nombre);
    var card = document.createElement("button");
    card.className = "chordCard";
    card.innerHTML = (posiciones[0] ? vChordSvg(posiciones[0], 92) : "") +
                     '<span class="nm">' + vEsc(nombre) + '</span>' +
                     '<span class="va">' + posiciones.length +
                     (posiciones.length === 1 ? " posición" : " posiciones") + '</span>';
    card.onclick = function () { openChordEditor(nombre); };
    chordList.appendChild(card);
  });
  var resto = vistos.length - Math.min(vistos.length, tope);
  var msg = !nombres.length
    ? "El diccionario está vacío. Empieza con «Importar diccionario base»."
    : !vistos.length ? "Ningún acorde coincide con la búsqueda."
    : resto > 0 ? "Y " + resto + " más. Busca por nombre para acotar."
    : "";
  chordEmpty.textContent = msg;
  chordEmpty.classList.toggle("hidden", !msg);
}

function openChordEditor(nombre) {
  editingChord = nombre || "";
  chName.value = editingChord;
  chordPositions = JSON.parse(JSON.stringify(chordPositionsOf(editingChord)));
  if (!chordPositions.length) {
    chordPositions = [{ frets: [-1, -1, -1, -1, -1, -1], fingers: [0, 0, 0, 0, 0, 0], baseFret: 1, barres: [] }];
  }
  chMsg.textContent = "";
  chDelete.classList.toggle("hidden", !editingChord);
  renderChordPositions();
  abrirDialogo(chordEditor);
}

/** Una fila por posición: seis trastes, traste base, cejillas y el dibujo al lado. */
function renderChordPositions() {
  chPositions.innerHTML = "";
  chordPositions.forEach(function (pos, idx) {
    var fila = document.createElement("div");
    fila.className = "posRow";

    var vista = document.createElement("div");
    vista.innerHTML = vChordSvg(pos, 92);
    fila.appendChild(vista);

    var trastes = document.createElement("div");
    trastes.className = "grp";
    for (var c = 0; c < 6; c++) {
      (function (cuerda) {
        var inp = document.createElement("input");
        inp.type = "number"; inp.min = "-1"; inp.max = "24";
        inp.value = pos.frets[cuerda];
        inp.oninput = function () {
          var v = parseInt(inp.value, 10);
          pos.frets[cuerda] = isNaN(v) ? -1 : v;
          vista.innerHTML = vChordSvg(pos, 92);
        };
        trastes.appendChild(inp);
      })(c);
    }
    var etiqueta = document.createElement("span");
    etiqueta.className = "lbl";
    etiqueta.textContent = "Mi  La  Re  Sol  Si  Mi";
    var envoltorio = document.createElement("div");
    envoltorio.appendChild(etiqueta);
    envoltorio.appendChild(trastes);
    fila.appendChild(envoltorio);

    var base = document.createElement("div");
    var lb = document.createElement("span");
    lb.className = "lbl"; lb.textContent = "Traste base";
    var pastillas = document.createElement("div");
    pastillas.className = "pills";
    function pintaBase() {
      pastillas.innerHTML = "";
      for (var t = 1; t <= 12; t++) {
        (function (traste) {
          var b = document.createElement("button");
          b.textContent = traste;
          b.setAttribute("aria-pressed", (pos.baseFret || 1) === traste);
          b.onclick = function () {
            pos.baseFret = traste;
            pintaBase();
            vista.innerHTML = vChordSvg(pos, 92);
          };
          pastillas.appendChild(b);
        })(t);
      }
    }
    pintaBase();
    base.appendChild(lb); base.appendChild(pastillas);
    fila.appendChild(base);

    var cej = document.createElement("div");
    cej.className = "grp";
    var lc = document.createElement("span");
    lc.className = "lbl"; lc.textContent = "Cejilla";
    var ic = document.createElement("input");
    ic.type = "text"; ic.style.width = "70px";
    ic.value = (pos.barres || []).join(",");
    ic.oninput = function () {
      pos.barres = ic.value.split(",").map(function (v) { return parseInt(v, 10); })
                     .filter(function (v) { return !isNaN(v); });
      vista.innerHTML = vChordSvg(pos, 92);
    };
    cej.appendChild(lc); cej.appendChild(ic);
    fila.appendChild(cej);

    var quitar = document.createElement("button");
    quitar.textContent = "Quitar";
    quitar.onclick = function () {
      chordPositions.splice(idx, 1);
      renderChordPositions();
    };
    fila.appendChild(quitar);

    chPositions.appendChild(fila);
  });
}

/** Guarda el diccionario entero: es un blob único y el que manda para todos. */
function saveChordDict(siguiente, mensaje) {
  return api("PUT", "/api/chords/global", { chords: siguiente }).then(function (d) {
    chordDict = siguiente;
    chordSaved.textContent = mensaje || ("Guardado · " + d.count + " acordes");
    chordMsg.textContent = "";
    renderChordList();
    return d;
  });
}

function saveChord() {
  var nombre = chName.value.trim();
  if (!nombre) { chMsg.textContent = "El acorde necesita un nombre."; return; }
  if (!chordPositions.length) { chMsg.textContent = "Añade al menos una posición."; return; }
  var siguiente = {};
  Object.keys(chordDict).forEach(function (k) { siguiente[k] = chordDict[k]; });
  // Renombrar es mover: se borra el nombre viejo para no dejar un duplicado.
  if (editingChord && editingChord !== nombre) delete siguiente[editingChord];
  siguiente[nombre] = { positions: chordPositions };
  saveChordDict(siguiente, "Guardado " + nombre).then(function () {
    cerrarDialogo(chordEditor);
  }).catch(function (e) { chMsg.textContent = e.message; });
}

function deleteChord() {
  if (!editingChord) return;
  if (!confirm("¿Quitar " + editingChord + " del diccionario global? Deja de verse para todo el mundo.")) return;
  var siguiente = {};
  Object.keys(chordDict).forEach(function (k) { if (k !== editingChord) siguiente[k] = chordDict[k]; });
  saveChordDict(siguiente, "Eliminado " + editingChord).then(function () {
    cerrarDialogo(chordEditor);
  }).catch(function (e) { chMsg.textContent = e.message; });
}

function seedChords() {
  chordMsg.textContent = "";
  chordSaved.textContent = "Importando…";
  api("POST", "/api/chords/global/seed").then(function (d) {
    chordDict = null;
    return loadChords().then(function () {
      chordSaved.textContent = "Añadidos " + d.added + " acordes (ya tenías " + d.kept + ")";
      renderChordList();
    });
  }).catch(function (e) {
    chordSaved.textContent = "";
    chordMsg.textContent = e.message;
  });
}

/* ---------- editor ---------- */

var editCapo = 0;

/**
 * Capo de 0 a 12 en botones. Se elige de un toque y no hay forma de escribir
 * un valor imposible; 0 es "sin capo" y por eso va con su propia etiqueta.
 */
function renderCapoPills() {
  eCapoPills.innerHTML = "";
  for (var i = 0; i <= 12; i++) {
    (function (valor) {
      var b = document.createElement("button");
      b.textContent = valor === 0 ? "Sin capo" : String(valor);
      b.setAttribute("aria-pressed", valor === editCapo);
      b.onclick = function () { editCapo = valor; renderCapoPills(); };
      eCapoPills.appendChild(b);
    })(i);
  }
}

/** Marca los acordes del texto con la misma lógica que el panel /admin. */
function detectChords() {
  var r = vDetectChords(eContent.value);
  if (!r.marked) { editMsg.textContent = "No se ha detectado ninguna línea de acordes."; return; }
  eContent.value = r.text;
  renderEditorPreview();
  editMsg.textContent = "Marcados " + r.marked + " acordes.";
}

/**
 * Pinta el panel derecho con el mismo render que el visor. Las cabeceras
 * #clave: valor no se muestran (son metadatos, no letra), igual que al ver la
 * partitura: lo de la derecha es exactamente lo que vera quien la toque.
 */
function renderEditorPreview() {
  var parsed = vParseSong(eContent.value);
  ePreview.innerHTML = '<div class="sheet">' +
                       vRenderSong(parsed.body || eContent.value) + "</div>";
}

/**
 * Ata el scroll de los dos paneles en proporcion a su recorrido: tienen
 * alturas distintas (la previa envuelve menos que el texto crudo), asi que
 * copiar scrollTop tal cual los desincroniza.
 */
var syncingPanes = false;
function linkPaneScroll(origen, destino) {
  origen.addEventListener("scroll", function () {
    if (syncingPanes) return;
    syncingPanes = true;
    var maxOrigen = origen.scrollHeight - origen.clientHeight;
    var maxDestino = destino.scrollHeight - destino.clientHeight;
    destino.scrollTop = maxOrigen > 0 ? (origen.scrollTop / maxOrigen) * maxDestino : 0;
    requestAnimationFrame(function () { syncingPanes = false; });
  });
}

/**
 * El editor sirve para tres cosas y enseña solo lo que toca en cada una:
 * la partitura, una versión suya, o una versión que se propone a revisión.
 */
function applyEditorMode() {
  var esVersion = editorMode === "version" || editorMode === "proposal";
  eVersionHead.classList.toggle("hidden", !esVersion);
  eNoteWrap.classList.toggle("hidden", editorMode !== "proposal");
  eTitle.parentNode.classList.toggle("hidden", esVersion);
  eArtist.parentNode.classList.toggle("hidden", esVersion);
  eGenre.parentNode.classList.toggle("hidden", esVersion);
  ePlaylistWrap.classList.toggle("hidden", esVersion);
  eVisibilityWrap.classList.toggle("hidden", esVersion || !esEditor());
  eLockedWrap.classList.toggle("hidden", esVersion);
  deleteBtn.classList.toggle("hidden", esVersion || !editingId);
  saveBtn.textContent = editorMode === "proposal" ? "Enviar a revisión" : "Guardar";

  var puedeProponerPublicar = editorMode === "song" && !!editingId && !esEditor() &&
                              current && current.song && current.song.visibility !== "public";
  proposeBtn.classList.toggle("hidden", !puedeProponerPublicar);

  editAviso.classList.add("hidden");
  if (editorMode === "proposal") {
    editAviso.textContent = "Esto no cambia la partitura: se envía a revisión y un editor decide.";
    editAviso.classList.remove("hidden");
  } else if (editorMode === "song" && !esEditor()) {
    editAviso.textContent = "Tus partituras nacen privadas. Para que salgan en el catálogo, propón su publicación.";
    editAviso.classList.remove("hidden");
  }
}

function showEdit(on) {
  showView(on ? "edit" : "list");
  editMsg.textContent = "";
}
function newSong() {
  editingId = null;
  eTitle.value = ""; eArtist.value = ""; eSource.value = ""; eContent.value = ""; eGenre.value = "";
  eTube.value = ""; eTubeMsg.textContent = "";
  editCapo = 0; renderCapoPills();
  eLocked.checked = false;
  editorMode = "song"; editingVersionId = null;
  eVisibility.value = "private";
  // Si se está mirando una lista concreta, la nueva nace ahí: es lo que espera
  // quien acaba de pulsar "+ Nueva" dentro de esa carpeta.
  ePlaylist.value = (playlistBy && playlistBy !== "none") ? playlistBy : "";
  renderEditorPreview();
  applyEditorMode();
  showEdit(true);
}
function editCurrent() {
  if (!current) return;
  editorMode = "song";
  editingVersionId = null;
  editingId = current.song.id;
  if (current.song.locked &&
      !confirm("Esta partitura está bloqueada para evitar cambios accidentales. ¿Editarla igualmente?")) return;
  eTitle.value = current.song.title || "";
  eArtist.value = current.song.artist || "";
  editCapo = Number(current.song.capo) || 0;
  renderCapoPills();
  eSource.value = current.song.sourceUrl || "";
  eGenre.value = current.song.genre || "";
  eTube.value = current.song.youtubeUrl || "";
  eTubeMsg.textContent = "";
  eLocked.checked = !!current.song.locked;
  eVisibility.value = current.song.visibility || "private";
  ePlaylist.value = current.song.playlistId || "";
  eContent.value = current.content || "";
  renderEditorPreview();
  closeViewer();
  applyEditorMode();
  showEdit(true);
}
function saveSong() {
  if (editorMode === "version") return saveVersion();
  if (editorMode === "proposal") return sendVersionProposal();
  var payload = {
    title: eTitle.value.trim(),
    artist: eArtist.value.trim(),
    capo: editCapo,
    genre: eGenre.value.trim(),
    youtubeUrl: eTube.value.trim(),
    locked: eLocked.checked,
    sourceUrl: eSource.value.trim(),
    visibility: eVisibility.value,
    playlistId: ePlaylist.value || null,
    content: eContent.value
  };
  var req = editingId ? api("PUT", "/api/songs/" + editingId, payload)
                      : api("POST", "/api/songs", payload);
  req.then(function () {
    showEdit(false);
    tab = "mine";
    listOffset = 0;
    loadGenres();          // la categoría puede ser nueva
    refresh();
  }).catch(function (e) { editMsg.textContent = e.message; });
}
/** Guarda una versión de la partitura abierta (alta o edición). */
function saveVersion() {
  var payload = {
    name: eVersionName.value.trim() || "Sin nombre",
    capo: editCapo,
    sourceUrl: eSource.value.trim(),
    content: eContent.value
  };
  var req = editingVersionId
    ? api("PUT", "/api/versions/" + editingVersionId, payload)
    : api("POST", "/api/songs/" + editingId + "/versions", payload);
  req.then(function () {
    editorMode = "song";
    showEdit(false);
    openSong(editingId);
  }).catch(function (e) { editMsg.textContent = e.message; });
}

/** Envía la versión a revisión en vez de aplicarla. */
function sendVersionProposal() {
  api("POST", "/api/songs/" + proposalTarget + "/proposals", {
    kind: "version",
    name: eVersionName.value.trim() || "Versión propuesta",
    capo: editCapo,
    sourceUrl: eSource.value.trim(),
    note: eNote.value.trim(),
    content: eContent.value
  }).then(function () {
    editorMode = "song";
    showEdit(false);
    tab = "proposals";
    listOffset = 0;
    refresh();
  }).catch(function (e) { editMsg.textContent = e.message; });
}

/** Pide que la partitura entre en el catálogo. No la publica: la propone. */
function proposePublish() {
  if (!editingId) return;
  var nota = prompt("¿Algo que quieras contarle a quien la revise? (opcional)");
  if (nota === null) return;
  api("POST", "/api/songs/" + editingId + "/proposals", { kind: "publish", note: nota })
    .then(function () {
      showEdit(false);
      tab = "proposals";
      listOffset = 0;
      refresh();
    }).catch(function (e) { editMsg.textContent = e.message; });
}

function deleteSong() {
  if (!editingId) return;
  if (!confirm("¿Mover esta partitura a la papelera?")) return;
  api("DELETE", "/api/songs/" + editingId).then(function () {
    showEdit(false);
    loadGenres();
    refresh();
  }).catch(function (e) { editMsg.textContent = e.message; });
}

/* ---------- eventos ---------- */
/**
 * El campo de contraseña cambia de papel según el modo. Con
 * "current-password" fijo, al registrarse el gestor intentaba rellenar una
 * contraseña que no existe en vez de ofrecer generar una nueva.
 */
function ajustarAutocomplete() {
  password.setAttribute("autocomplete", registering ? "new-password" : "current-password");
}

loginBtn.onclick = function () { registering = false; authTitle.textContent = "Entrar en Vivace";
  authSubmit.textContent = "Entrar"; authSwitch.textContent = "Crear una cuenta";
  nameWrap.classList.add("hidden"); ajustarAutocomplete(); showAuth(true); };
logoutBtn.onclick = function () {
  closeViewer();
  current = null;
  editingId = null;
  editingVersionId = null;
  editorMode = "song";
  chordDict = null;              // el diccionario se recarga con la sesión nueva
  setSession("", null);
  tab = "public";
  listOffset = 0;
  refresh();
};
authSwitch.onclick = function () {
  registering = !registering;
  authTitle.textContent = registering ? "Crear cuenta en Vivace" : "Entrar en Vivace";
  authSubmit.textContent = registering ? "Crear cuenta" : "Entrar";
  authSwitch.textContent = registering ? "Ya tengo cuenta" : "Crear una cuenta";
  nameWrap.classList.toggle("hidden", !registering);
  ajustarAutocomplete();
  authMsg.textContent = "";
};
authSubmit.onclick = submitAuth;
password.addEventListener("keydown", function (e) { if (e.key === "Enter") submitAuth(); });

/** Cambiar de pestaña o de filtro empieza el listado desde el principio. */
function irA(pestana) {
  tab = pestana;
  listOffset = 0;
  refresh();
}

tabPublic.onclick = function () { irA("public"); };
tabMine.onclick = function () { irA("mine"); };
tabTrash.onclick = function () { irA("trash"); };
tabAdmin.onclick = function () { irA("admin"); };
moreBtn.onclick = function () { listOffset += PAGINA; refresh(); };
search.oninput = function () { renderList(); };
tabChords.onclick = function () { irA("chords"); };

themeBtn.onclick = toggleTheme;

playlistFilter.onchange = function () {
  playlistBy = playlistFilter.value;
  renderPlaylistControls();
  renderList();
};
favFilter.onclick = function () {
  favOnly = !favOnly;
  favFilter.setAttribute("aria-pressed", favOnly);
  renderList();
};
vShare.onclick = compartirCancion;
vPrint.onclick = printViewer;

backupBtn.onclick = backupZip;
restoreBtn.onclick = function () { restoreFile.click(); };
restoreFile.onchange = function () {
  if (restoreFile.files && restoreFile.files[0]) restoreZip(restoreFile.files[0]);
  restoreFile.value = "";
};
noVideoBtn.onclick = listarSinVideo;
genresDryBtn.onclick = function () { categoriasAuto(false); };
genresApplyBtn.onclick = function () { categoriasAuto(true); };

newListBtn.onclick = crearLista;
renameListBtn.onclick = renombrarLista;
deleteListBtn.onclick = borrarLista;
vChords.onclick = toggleChordBar;
/**
 * Globo con las digitaciones al pasar por encima de un acorde. El clic sigue
 * abriendo el modal completo; esto es para consultar de pasada sin perder el
 * sitio en la letra.
 */
var hoverTimer = 0;
function hideChordHover() {
  if (hoverTimer) { clearTimeout(hoverTimer); hoverTimer = 0; }
  chordHover.classList.remove("on");
}
function showChordHover(el, nombre) {
  var posiciones = chordPositionsOf(nombre);
  if (!posiciones.length) return;
  chordHover.innerHTML = '<span class="nm">' + vEsc(nombre) + '</span>' +
    posiciones.slice(0, 4).map(function (p) { return vChordSvg(p, 84); }).join("");
  chordHover.classList.add("on");
  // Se coloca debajo del acorde y se mete hacia dentro si no cabe a la derecha.
  var r = el.getBoundingClientRect();
  var ancho = chordHover.offsetWidth;
  var alto = chordHover.offsetHeight;
  var x = Math.min(Math.max(8, r.left), window.innerWidth - ancho - 8);
  var y = r.bottom + 8;
  if (y + alto > window.innerHeight - 8) y = Math.max(8, r.top - alto - 8);
  chordHover.style.left = x + "px";
  chordHover.style.top = y + "px";
}
vBody.onmouseover = function (e) {
  var destino = e.target;
  if (!destino || !destino.classList || !destino.classList.contains("chord")) return;
  var nombre = destino.textContent.trim();
  if (hoverTimer) clearTimeout(hoverTimer);
  // Un respiro antes de aparecer: si no, pasar el ratón por encima de la letra
  // dispara globos sin parar.
  hoverTimer = setTimeout(function () {
    loadChords().then(function () { showChordHover(destino, nombre); });
  }, 220);
};
vBody.onmouseout = function (e) {
  var destino = e.target;
  if (destino && destino.classList && destino.classList.contains("chord")) hideChordHover();
};
vBody.onscroll = hideChordHover;

vBody.onclick = function (e) {
  var destino = e.target;
  if (!destino || !destino.classList || !destino.classList.contains("chord")) return;
  // El texto del acorde ya está transpuesto: es lo que se está tocando.
  hideChordHover();
  openChordModal(destino.textContent.trim());
};
cmClose.onclick = function () { cerrarDialogo(chordModal); };
document.addEventListener("keydown", function (e) {
  if (e.key === "Escape" && !chordModal.classList.contains("hidden")) {
    cerrarDialogo(chordModal);
  }
});
chordModal.onclick = function (e) { if (e.target === chordModal) cerrarDialogo(chordModal); };
tabProposals.onclick = function () { irA("proposals"); };
tabUsers.onclick = function () { irA("users"); };
propStatus.onchange = loadProposals;
propMine.onchange = loadProposals;
pmClose.onclick = function () { cerrarDialogo(propModal); };
propModal.onclick = function (e) { if (e.target === propModal) cerrarDialogo(propModal); };
proposeBtn.onclick = proposePublish;
genreFilter.onchange = function () { genreBy = genreFilter.value; listOffset = 0; refresh(); };
sortSel.onchange = function () { sortBy = sortSel.value; listOffset = 0; refresh(); };
chordSearch.oninput = renderChordList;
chordNew.onclick = function () { openChordEditor(""); };
chordSeed.onclick = seedChords;
chClose.onclick = function () { cerrarDialogo(chordEditor); };
chAddPos.onclick = function () {
  chordPositions.push({ frets: [-1, -1, -1, -1, -1, -1], fingers: [0, 0, 0, 0, 0, 0], baseFret: 1, barres: [] });
  renderChordPositions();
};
eDetect.onclick = detectChords;
// Comprobación en el momento: pegar algo que no es de YouTube se ve al instante.
eTube.oninput = function () {
  var v = eTube.value.trim();
  eTubeMsg.textContent = !v ? "" : (vEmbedUrl(v) ? "Vídeo reconocido" : "No parece un enlace de YouTube");
};
eTubeSearch.onclick = function () {
  var consulta = (eArtist.value.trim() + " " + eTitle.value.trim()).trim();
  if (!consulta) { eTubeMsg.textContent = "Pon antes el título o el artista."; return; }
  window.open("https://www.youtube.com/results?search_query=" + encodeURIComponent(consulta),
              "_blank", "noopener");
};
chSave.onclick = saveChord;
chDelete.onclick = deleteChord;
newBtn.onclick = newSong;
eContent.oninput = renderEditorPreview;
linkPaneScroll(eContent, ePreview);
linkPaneScroll(ePreview, eContent);
saveBtn.onclick = saveSong;
cancelEdit.onclick = function () { showEdit(false); };
deleteBtn.onclick = deleteSong;

vClose.onclick = closeViewer;
vEdit.onclick = editCurrent;
vPlay.onclick = function () { scrolling ? stopScroll() : startScroll(); };
vSpeed.oninput = function () { vSpeedVal.textContent = vSpeed.value + " px/s"; };
vUp.onclick = function () { semis = Math.min(11, semis + 1); renderViewer(); };
vDown.onclick = function () { semis = Math.max(-11, semis - 1); renderViewer(); };
vFlat.onclick = function () { flats = !flats; renderViewer(); };
vFontUp.onclick = function () { fontSize = Math.min(40, fontSize + 2); renderViewer(); };
vFontDown.onclick = function () { fontSize = Math.max(11, fontSize - 2); renderViewer(); };
vMetro.onclick = toggleMetro;
vBpm.oninput = function () { vBpmVal.textContent = vBpm.value; metro.bpm = +vBpm.value; };
document.addEventListener("keydown", function (e) {
  if (e.key === "Escape" && viewer.classList.contains("on")) closeViewer();
});

applyTheme(currentTheme());
renderBeats(0);
loadGenres();
restoreSession().then(function () {
  if (user) tab = "mine";
  refresh();
  // Lo último: así una partitura privada propia se abre con la sesión ya puesta.
  aplicarUrl();
});
`;

export const WEB_HTML = `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>Vivace</title>
<link rel="icon" href="/static/favicon.svg" type="image/svg+xml">
<meta name="theme-color" content="#0F1113" media="(prefers-color-scheme: dark)">
<meta name="theme-color" content="#F6F4EF" media="(prefers-color-scheme: light)">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
<script>
  /* El tema elegido se aplica ANTES de pintar: si se hiciera al final del
     <body>, la página aparecería un instante con el tema contrario. */
  try {
    var vvTema = localStorage.getItem("vivace_theme");
    if (vvTema === "light" || vvTema === "dark") document.documentElement.setAttribute("data-theme", vvTema);
  } catch (e) {}
</script>
<link rel="stylesheet" href="/static/vivace.css">
</head>
<body>

<header>
  <a class="brand" href="/" aria-label="Vivace">
    <svg width="30" height="30" viewBox="0 0 48 48" aria-hidden="true">
      <rect x="18" y="6" width="12" height="30" fill="none" stroke="var(--vv-accent)" stroke-width="2.4"></rect>
      <g fill="currentColor">
        <circle cx="12" cy="14" r="2.4"></circle><circle cx="12" cy="22" r="2.4"></circle><circle cx="12" cy="30" r="2.4"></circle>
        <circle cx="36" cy="14" r="2.4"></circle><circle cx="36" cy="22" r="2.4"></circle><circle cx="36" cy="30" r="2.4"></circle>
      </g>
      <rect x="18" y="38" width="12" height="3.4" fill="var(--vv-accent)"></rect>
    </svg>
    <span>
      <h1>vivace</h1>
      <span class="kicker">Practice room</span>
    </span>
  </a>
  <span class="grow"></span>
  <span id="who" class="hidden" style="font-size:13px;color:var(--vv-text-muted)"></span>
  <button id="themeBtn" title="Cambiar entre claro y oscuro" aria-label="Cambiar tema">🌓</button>
  <button id="loginBtn">Entrar</button>
  <button id="logoutBtn" class="hidden">Salir</button>
</header>

<main>
  <!-- acceso -->
  <section id="authView" class="hidden">
    <div class="stack">
      <h2 id="authTitle" style="margin:0">Entrar en Vivace</h2>
      <label>Email<input type="email" id="email" autocomplete="email"></label>
      <label id="nameWrap" class="hidden">Nombre<input type="text" id="name" autocomplete="name"></label>
      <label>Contraseña <small>(mínimo 8 caracteres)</small>
        <input type="password" id="password" minlength="8" autocomplete="current-password"></label>
      <div class="msg" id="authMsg"></div>
      <button class="primary" id="authSubmit">Entrar</button>
      <button class="ghost" id="authSwitch">Crear una cuenta</button>
    </div>
  </section>

  <!-- listados -->
  <section id="listView">
    <div class="tabs">
      <button id="tabPublic" aria-selected="true">Catálogo</button>
      <button id="tabMine" aria-selected="false" class="hidden">Mis partituras</button>
      <button id="tabProposals" aria-selected="false" class="hidden">Propuestas</button>
      <button id="tabChords" aria-selected="false" class="hidden" title="Diccionario global, visible para todo el mundo">Acordes</button>
      <button id="tabUsers" aria-selected="false" class="hidden">Usuarios</button>
      <button id="tabTrash" aria-selected="false" class="hidden" title="Partituras borradas">Papelera</button>
      <button id="tabAdmin" aria-selected="false" class="hidden" title="Herramientas del catálogo">Administración</button>
      <span class="grow"></span>
      <button id="newBtn" class="hidden">+ Nueva</button>
    </div>
    <div class="filtros">
      <label class="filtro" for="search">Buscar
        <input type="text" id="search" placeholder="Título o artista…"></label>
      <label class="filtro" for="genreFilter">Categoría
        <select id="genreFilter">
          <option value="">Todas</option>
        </select></label>
      <label class="filtro" for="sortSel">Orden
        <select id="sortSel">
          <option value="title">Título (A–Z)</option>
          <option value="recent">Más recientes primero</option>
          <option value="old">Más antiguas primero</option>
        </select></label>
      <label class="filtro hidden" id="playlistFilterWrap" for="playlistFilter">Lista
        <select id="playlistFilter">
          <option value="">Todas</option>
        </select></label>
      <button id="favFilter" class="hidden" aria-pressed="false" title="Ver solo las favoritas">★ Favoritas</button>
    </div>
    <div id="listasBar" class="listas hidden">
      <span class="etiqueta">Listas</span>
      <span id="listasChips" class="chips"></span>
      <button id="newListBtn" class="ghost">+ Nueva lista</button>
      <button id="renameListBtn" class="ghost hidden">Renombrar</button>
      <button id="deleteListBtn" class="ghost hidden">Borrar lista</button>
    </div>
    <div id="list" class="grid"></div>
    <div id="listEmpty" class="empty hidden"></div>
    <div class="row" style="margin-top:14px">
      <button id="moreBtn" class="hidden">Cargar más</button>
    </div>
  </section>

  <!-- administración del catálogo (editor/admin) -->
  <section id="adminView" class="hidden">
    <h2 style="margin:0 0 14px">Administración</h2>
    <div class="adminTool">
      <h3>Copia de seguridad</h3>
      <p>Descarga un ZIP con el texto de todas tus partituras, una por fichero.
         Restaurar vuelve a subirlas: las que ya existan con el mismo título se
         sobrescriben.</p>
      <div class="acciones">
        <button id="backupBtn">Descargar ZIP</button>
        <button id="restoreBtn">Restaurar desde ZIP</button>
        <input type="file" id="restoreFile" accept=".zip" class="hidden">
      </div>
      <div class="msg" id="adminMsg"></div>
    </div>
    <div class="adminTool">
      <h3>Sin vídeo</h3>
      <p>Partituras publicadas a las que aún no se les ha puesto un enlace de YouTube.</p>
      <button id="noVideoBtn">Listar</button>
      <div id="noVideoList" class="stack" style="margin-top:10px"></div>
    </div>
    <div class="adminTool">
      <h3>Categorías automáticas</h3>
      <p>Propone una categoría para las partituras que no tienen ninguna.
         Primero enseña el recuento; no escribe nada hasta confirmar.</p>
      <div class="acciones">
        <button id="genresDryBtn">Ver propuesta</button>
        <button id="genresApplyBtn" class="hidden">Aplicar</button>
      </div>
      <div class="msg" id="genresMsg"></div>
    </div>
  </section>

  <!-- editor -->
  <section id="editView" class="hidden">
    <div class="editor">
      <div class="editHead">
        <label>Título<input type="text" id="eTitle"></label>
        <label>Artista<input type="text" id="eArtist"></label>
        <label>Categoría <small>estilo musical</small>
          <input type="text" id="eGenre" list="genreList" placeholder="Rock, bolero, folk…">
          <datalist id="genreList"></datalist></label>
        <label id="ePlaylistWrap">Lista
          <select id="ePlaylist">
            <option value="">Sin lista</option>
          </select></label>
        <label id="eLockedWrap" class="row" style="gap:8px;align-items:center">
          <input type="checkbox" id="eLocked" style="width:auto">
          <span>Bloqueada <small>pide confirmación antes de editarla</small></span>
        </label>
        <label id="eVisibilityWrap">Visibilidad
          <select id="eVisibility">
            <option value="private">Privada (solo yo)</option>
            <option value="public">Pública (cualquiera puede verla)</option>
          </select>
        </label>
      </div>
      <div class="editHead" style="grid-template-columns:1fr" id="eVersionHead">
        <label>Nombre de la versión <small>«Acústica», «En Do», «Tablatura»…</small>
          <input type="text" id="eVersionName" placeholder="Acústica"></label>
        <label id="eNoteWrap">Mensaje para quien la revise <small>opcional</small>
          <input type="text" id="eNote" placeholder="Qué cambia y por qué"></label>
      </div>
      <div class="editHead" style="grid-template-columns:1fr">
        <label>URL de la partitura original <small>opcional</small>
          <input type="url" id="eSource" placeholder="https://…"></label>
        <label>Vídeo de YouTube <small>opcional; se ve junto a la partitura</small>
          <input type="text" id="eTube" placeholder="https://youtu.be/…"></label>
        <div class="row">
          <button id="eTubeSearch" title="Abre la búsqueda en otra pestaña">Buscar en YouTube</button>
          <span id="eTubeMsg" class="nota"></span>
        </div>
        <div>
          <div class="vv-kicker" style="margin-bottom:6px">Capo</div>
          <div class="pills" id="eCapoPills"></div>
        </div>
      </div>
      <div id="editSplit">
        <div class="pane">
          <div class="hd">Partitura <small>acordes entre llaves: {Am}</small>
            <span class="grow"></span>
            <button id="eDetect" title="Marca las líneas que solo llevan acordes">♪ Detectar acordes</button>
          </div>
          <textarea id="eContent" spellcheck="false"
                    placeholder="#title: Título&#10;#artist: Autor&#10;---&#10;{Am} Primera línea"></textarea>
        </div>
        <div class="pane">
          <div class="hd">Vista previa <small>tal cual se verá</small></div>
          <div id="ePreview"></div>
        </div>
      </div>
      <div id="editAviso" class="aviso hidden"></div>
      <div class="msg" id="editMsg"></div>
      <div class="row">
        <button class="primary" id="saveBtn">Guardar</button>
        <button id="proposeBtn" class="hidden" title="Un editor la revisará antes de publicarla">Proponer publicación</button>
        <button id="cancelEdit">Cancelar</button>
        <span class="grow"></span>
        <button id="deleteBtn" class="hidden" style="color:var(--vv-danger)">Eliminar</button>
      </div>
    </div>
  </section>
  <!-- propuestas: cola de revisión para editores, historial para el resto -->
  <section id="proposalsView" class="hidden">
    <div class="row" style="margin-bottom:12px">
      <select id="propStatus" style="flex:0 1 220px;width:auto">
        <option value="pending">Pendientes</option>
        <option value="approved">Aprobadas</option>
        <option value="rejected">Rechazadas</option>
        <option value="all">Todas</option>
      </select>
      <label id="propMineWrap" class="row hidden" style="gap:6px">
        <input type="checkbox" id="propMine" style="width:auto"> Solo las mías</label>
      <span class="grow"></span>
      <span id="propCount" class="vv-kicker"></span>
    </div>
    <div class="msg" id="propMsg"></div>
    <div id="propList"></div>
    <div id="propEmpty" class="empty hidden"></div>
  </section>

  <!-- usuarios y roles (solo administración) -->
  <section id="usersView" class="hidden">
    <div class="aviso">
      El <b>editor</b> gestiona el catálogo: edita y despublica partituras públicas,
      resuelve propuestas y mantiene el diccionario de acordes. El <b>usuario</b> crea
      partituras suyas y propone publicarlas o aportar versiones.
    </div>
    <div class="msg" id="usersMsg"></div>
    <div id="usersList"></div>
  </section>

  <!-- diccionario global de acordes (solo administración) -->
  <section id="chordsView" class="hidden">
    <div class="row" style="margin-bottom:12px">
      <input type="text" id="chordSearch" placeholder="Buscar acorde…" style="flex:1;min-width:160px">
      <button id="chordNew">+ Nuevo acorde</button>
      <button id="chordSeed" title="Añade los acordes que falten sin tocar los que ya has definido">Importar diccionario base</button>
    </div>
    <div class="row" style="margin-bottom:10px">
      <span id="chordCount" class="vv-kicker"></span>
      <span class="grow"></span>
      <span id="chordSaved" class="vv-kicker"></span>
    </div>
    <div class="msg" id="chordMsg"></div>
    <div id="chordList" class="chordGrid"></div>
    <div id="chordEmpty" class="empty hidden"></div>
  </section>
</main>

<div id="chordHover"></div>

<!-- contenido de una propuesta, tal y como quedaría -->
<div id="propModal" class="modal hidden" role="dialog" aria-modal="true" aria-labelledby="pmTitle">
  <div class="modalBox">
    <div class="row">
      <h3 id="pmTitle"></h3>
      <span class="grow"></span>
      <button id="pmClose">Cerrar</button>
    </div>
    <div id="pmMeta" class="meta"></div>
    <div id="pmBody" style="font-family:var(--vv-font-mono);font-size:15px;overflow:auto"></div>
  </div>
</div>

<!-- diagramas del acorde que se está mirando (cualquiera) -->
<div id="chordModal" class="modal hidden" role="dialog" aria-modal="true" aria-labelledby="cmName">
  <div class="modalBox" style="max-width:620px">
    <div class="row">
      <h3 id="cmName"></h3>
      <span class="grow"></span>
      <button id="cmClose">Cerrar</button>
    </div>
    <div id="cmBody" class="chordGrid"></div>
    <div id="cmEmpty" class="empty hidden"></div>
  </div>
</div>

<!-- edición de un acorde (solo administración) -->
<div id="chordEditor" class="modal hidden" role="dialog" aria-modal="true" aria-label="Editar acorde">
  <div class="modalBox">
    <div class="row">
      <label style="flex:1">Nombre <small>tal cual se escribe entre llaves</small>
        <input type="text" id="chName" placeholder="Am7"></label>
      <button id="chClose">Cerrar</button>
    </div>
    <div class="vv-kicker">Posiciones — trastes de la 6ª cuerda (Mi grave) a la 1ª; -1 no suena, 0 al aire</div>
    <div id="chPositions"></div>
    <div class="msg" id="chMsg"></div>
    <div class="row">
      <button id="chAddPos">+ Posición</button>
      <span class="grow"></span>
      <button id="chDelete" style="color:var(--vv-danger)">Eliminar acorde</button>
      <button class="primary" id="chSave">Guardar</button>
    </div>
  </div>
</div>

<!-- visor a pantalla completa -->
<div id="viewer" role="dialog" aria-modal="true" aria-labelledby="vTitle">
  <div id="vHead">
    <div class="lado">
      <button id="vClose" class="ghost">‹ Volver</button>
    </div>
    <div class="vMeta">
      <div class="titulo">
        <span id="vArtist"></span>
        <span id="vSep" class="hidden">–</span>
        <span id="vTitle"></span>
      </div>
      <span id="vCapo" class="hidden"></span>
    </div>
    <div class="lado der">
      <a id="vSource" class="hidden" href="#" target="_blank" rel="noopener noreferrer"
         title="Abrir la partitura original en otra pestaña">Original ↗</a>
      <button id="vShare" title="Copiar el enlace de esta partitura">🔗 Compartir</button>
      <button id="vPrint" title="Abre el diálogo de impresión; ahí puedes elegir «Guardar como PDF»">🖨 PDF</button>
      <button id="vEdit" class="hidden">Editar</button>
    </div>
  </div>
  <div id="vChordBar" class="chordBar hidden"></div>
  <div id="vMain">
    <div id="vSide">
      <div id="vCtrl">
      <div class="row">
        <button id="vPlay" title="Desplazamiento automático">▶ Scroll</button>
        <input type="range" id="vSpeed" min="5" max="300" value="40" style="min-width:60px">
        <label id="vSpeedVal">40 px/s</label>
      </div>
      <div class="row">
        <label>Tono</label>
        <button id="vDown">–</button>
        <label id="vTone" style="min-width:2.5em;text-align:center">±0</label>
        <button id="vUp">+</button>
        <button id="vFlat">♭</button>
      </div>
      <div class="row">
        <label>Letra</label>
        <button id="vFontDown">A-</button>
        <button id="vFontUp">A+</button>
        <span class="grow"></span>
        <button id="vChords" title="Diagramas de los acordes de esta partitura">♦ Acordes</button>
      </div>
      <div class="row">
        <button id="vMetro" title="Metrónomo">♩ Metrónomo</button>
        <span id="vBeats" class="row" style="gap:4px"></span>
      </div>
      <div class="row">
        <label>BPM</label>
        <input type="range" id="vBpm" min="40" max="200" value="100" style="min-width:70px">
        <label id="vBpmVal">100</label>
        </div>
      </div>
      <div id="vVersionPanel">
        <div class="hd">Versiones</div>
        <div id="vVersions"></div>
        <div id="vVersionActions" class="row" style="gap:6px"></div>
      </div>
    </div>
    <div id="vBody"><div id="vSheet"></div><div id="vComments"></div></div>
    <div id="vTube"><div class="marco"></div></div>
  </div>
</div>

<script src="/static/vivace.js" defer></script>
<script src="/static/vivace-app.js" defer></script>

</body>
</html>`;

/** Marca reducida (4 clavijas) sobre el fondo Nocturno; se sirve en /static/favicon.svg. */
export const FAVICON_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48">
  <rect width="48" height="48" rx="10" fill="#0F1113"></rect>
  <rect x="17.5" y="8" width="13" height="26" fill="none" stroke="#E8B04B" stroke-width="2.6"></rect>
  <g fill="#F2EFE9"><circle cx="11" cy="16" r="2.6"></circle><circle cx="11" cy="26" r="2.6"></circle><circle cx="37" cy="16" r="2.6"></circle><circle cx="37" cy="26" r="2.6"></circle></g>
  <rect x="17.5" y="36" width="13" height="3.5" fill="#E8B04B"></rect>
</svg>`;
