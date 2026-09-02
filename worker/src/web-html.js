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
export const WEB_CSS = `  /* Accordio · paquete de estilo de marca (accordio-web-kit, claro + oscuro).
     Los valores --ac-* salen tal cual de tokens.css y tokens.dark.css del kit;
     encima va la capa semántica --vv-* que es la que piden los componentes.

     Regla 9 del kit: los --vv-* cuelgan de los ROLES (--ac-action, --ac-active,
     --ac-highlight, --ac-pending, --ac-nav-*), no de las rampas de marca. Los
     roles ya cambian solos con el tema, así que casi nada hay que redefinir en
     oscuro: la marca no se invierte; lo que cambia es quién hace de acción.
     En oscuro el teal deja de contrastar y pasa a titular, y la acción la toma
     el turquesa. */
  :root {
    color-scheme: light dark;

    /* ---- kit: marca y rampas (iguales en los dos temas) ---- */
    --ac-primary:#1A535C;
    --ac-primary-100:#E3EEF0; --ac-primary-200:#BCD6DA; --ac-primary-300:#8DB6BD;
    --ac-primary-400:#4E8A94; --ac-primary-600:#154650; --ac-primary-700:#113941;
    --ac-primary-800:#0D2B31; --ac-primary-900:#081D21;
    --ac-coral:#FF6B6B; --ac-coral-200:#FFCCCC; --ac-coral-400:#FF8888;
    --ac-coral-600:#EE5253; --ac-coral-700:#C93B3C;
    --ac-turquoise:#4ECDC4; --ac-turquoise-200:#C2F0EB; --ac-turquoise-300:#94E4DD;
    --ac-turquoise-400:#6DD9D0; --ac-turquoise-600:#37AFA6; --ac-turquoise-700:#2A8A83;
    --ac-yellow:#FFE66D; --ac-yellow-600:#F2CE3D; --ac-yellow-900:#6B550A;

    /* ---- kit: superficies y texto (claro) ---- */
    --ac-bg:#F7EFE3; --ac-bg-alt:#F3E3CD;
    --ac-surface:#F2FAF6; --ac-surface-2:#FFFFFF;
    --ac-ink:#12363D; --ac-body:#3F5257; --ac-muted:#7B8E92;
    --ac-line:#DCE8E5; --ac-line-strong:#1A535C;

    /* ---- kit: roles (esto es lo que cambia con el tema) ---- */
    --ac-action:var(--ac-primary); --ac-action-hover:var(--ac-primary-600);
    --ac-on-action:#F2FAF6;
    --ac-highlight:var(--ac-coral); --ac-highlight-hover:var(--ac-coral-600);
    --ac-on-highlight:#FFFFFF;
    --ac-active:var(--ac-turquoise); --ac-pending:var(--ac-yellow);
    --ac-focus:var(--ac-turquoise-700);
    --ac-nav-bg:var(--ac-primary); --ac-nav-fg:#F2FAF6;
    --ac-icon-color:var(--ac-primary);
    --ac-pattern:url("/static/pattern.svg");

    /* ---- kit: tipografía, radios, sombras y movimiento ---- */
    --ac-font-heading:'Montserrat',system-ui,-apple-system,Segoe UI,Roboto,sans-serif;
    --ac-font-body:'Poppins',system-ui,-apple-system,Segoe UI,Roboto,sans-serif;
    --ac-radius-sm:8px; --ac-radius-md:12px; --ac-radius-lg:20px; --ac-radius-pill:999px;
    --ac-shadow-sm:0 1px 2px rgba(18,54,61,.06), 0 2px 6px rgba(18,54,61,.05);
    --ac-shadow-md:0 4px 10px rgba(18,54,61,.07), 0 12px 28px rgba(18,54,61,.07);
    --ac-shadow-lg:0 10px 24px rgba(18,54,61,.10), 0 28px 60px rgba(18,54,61,.10);
    --ac-ease:cubic-bezier(.2,.8,.2,1); --ac-dur:200ms;

    /* ---- capa semántica de la web ---- */
    --vv-bg:var(--ac-bg); --vv-surface:var(--ac-surface); --vv-surface-alt:var(--ac-surface-2);
    --vv-border:var(--ac-line); --vv-border-strong:var(--ac-primary-200);
    --vv-text:var(--ac-body); --vv-head:var(--ac-ink);
    --vv-text-muted:var(--ac-muted); --vv-text-subtle:var(--ac-muted);
    --vv-accent:var(--ac-action); --vv-accent-strong:var(--ac-action-hover);
    --vv-on-accent:var(--ac-on-action); --vv-accent-soft:var(--ac-primary-100);
    /* Estado seleccionado/activo: turquesa (regla 1, un acento por bloque). */
    --vv-active:var(--ac-active); --vv-on-active:var(--ac-primary-800);
    --vv-active-soft:var(--ac-turquoise-200);
    /* Amarillo SOLO como estado: capo, estrellas, propuestas pendientes. */
    --vv-state:var(--ac-pending); --vv-on-state:var(--ac-yellow-900);
    --vv-state-soft:#FFFBE6; --vv-state-text:var(--ac-yellow-900);
    --vv-danger-soft:#FFE9E9; --vv-danger-text:var(--ac-coral-700);
    /* Regla 5: coral vale de relleno, no de texto. Sobre crema, el coral de
       marca da 2,4:1; para LEER acordes hace falta la rampa 700. En oscuro sí
       vale el rol, que ya viene aclarado (#FF8A8A sobre #0F2429). */
    --vv-chord:var(--ac-coral-700); --vv-danger:var(--ac-coral-700);
    --vv-danger-solid:var(--ac-highlight); --vv-on-danger:var(--ac-on-highlight);
    --vv-beat:var(--ac-active);
    --vv-focus:var(--ac-focus);
    --vv-header-bg:var(--ac-nav-bg); --vv-header-text:var(--ac-nav-fg);
    --vv-glow:none;
    --vv-shadow-card:var(--ac-shadow-sm); --vv-shadow-pop:var(--ac-shadow-md);
    --vv-font-ui:var(--ac-font-body); --vv-font-head:var(--ac-font-heading);
    /* El kit no trae monoespaciada y la hoja no puede prescindir de ella: los
       acordes van sobre la sílaba exacta, columna a columna. Se queda JetBrains
       Mono para partitura, cifras y diagramas. */
    --vv-font-mono:'JetBrains Mono',ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;
    --vv-radius-sm:var(--ac-radius-sm); --vv-radius-md:var(--ac-radius-md);
    --vv-radius-lg:var(--ac-radius-lg); --vv-radius-pill:var(--ac-radius-pill);
    /* Ancho de la hoja de partitura, en caracteres del tipo monoespaciado.
       Va en ch y no en px a propósito: al cambiar el tamaño de letra, la hoja
       sigue teniendo el mismo número de columnas y la lectura no se descoloca. */
    --vv-sheet:68ch;
  }

  /* Modo oscuro del kit (tokens.dark.css), palabra por palabra. Se escribe dos
     veces —media query y atributo— igual que en el kit: el atributo tiene que
     poder ganarle al sistema en los dos sentidos. */
  @media (prefers-color-scheme: dark) {
    :root:not([data-theme=light]) {
      color-scheme: dark;
      --ac-bg:#0F2429; --ac-bg-alt:#122E34;
      --ac-surface:#163A41; --ac-surface-2:#1C464E;
      --ac-ink:#F2FAF6; --ac-body:#C6DCDE; --ac-muted:#8CA6AA;
      --ac-line:#26545C; --ac-line-strong:#94E4DD;
      --ac-primary:#94E4DD; --ac-coral:#FF8A8A; --ac-turquoise:#6DD9D0; --ac-yellow:#FFEA82;
      --ac-action:#4ECDC4; --ac-action-hover:#94E4DD;
      --ac-highlight:#FF8A8A; --ac-highlight-hover:#FFA8A8;
      --ac-active:#6DD9D0; --ac-pending:#FFE66D; --ac-focus:#94E4DD;
      --ac-on-action:#08262B; --ac-on-highlight:#3B0E0E;
      --ac-nav-bg:#0B1D21; --ac-nav-fg:#F2FAF6;
      --ac-icon-color:#DCECEE;
      --ac-pattern:url("/static/pattern-dark.svg");
      --ac-shadow-sm:0 1px 2px rgba(0,0,0,.30), 0 2px 6px rgba(0,0,0,.24);
      --ac-shadow-md:0 4px 10px rgba(0,0,0,.34), 0 12px 28px rgba(0,0,0,.30);
      --ac-shadow-lg:0 10px 24px rgba(0,0,0,.40), 0 28px 60px rgba(0,0,0,.36);
      /* Lo único que la capa semántica tiene que rectificar en oscuro: los
         rellenos tintados y el borde fuerte, que en claro salen de rampas
         claras, y el coral de texto, que aquí ya es legible como rol. */
      --vv-border-strong:#2F636B;
      --vv-accent-soft:rgba(78,205,196,.16);
      --vv-active-soft:rgba(109,217,208,.20);
      --vv-chord:var(--ac-highlight); --vv-danger:var(--ac-highlight);
      --vv-on-active:var(--ac-primary-800);
      --vv-state-soft:rgba(255,230,109,.16); --vv-state-text:var(--ac-yellow);
      --vv-danger-soft:rgba(255,138,138,.16); --vv-danger-text:var(--ac-highlight);
      /* El gris del kit (#8CA6AA) se queda en 3,6:1 sobre la tarjeta oscura: el
         artista y las notas al pie costaban de leer. Se sube el ROL de texto
         secundario; el token de marca --ac-muted no se toca. */
      --vv-text-muted:#A9C3C6; --vv-text-subtle:#93AFB2;
    }
  }
  [data-theme=dark] {
    color-scheme: dark;
    --ac-bg:#0F2429; --ac-bg-alt:#122E34;
    --ac-surface:#163A41; --ac-surface-2:#1C464E;
    --ac-ink:#F2FAF6; --ac-body:#C6DCDE; --ac-muted:#8CA6AA;
    --ac-line:#26545C; --ac-line-strong:#94E4DD;
    --ac-primary:#94E4DD; --ac-coral:#FF8A8A; --ac-turquoise:#6DD9D0; --ac-yellow:#FFEA82;
    --ac-action:#4ECDC4; --ac-action-hover:#94E4DD;
    --ac-highlight:#FF8A8A; --ac-highlight-hover:#FFA8A8;
    --ac-active:#6DD9D0; --ac-pending:#FFE66D; --ac-focus:#94E4DD;
    --ac-on-action:#08262B; --ac-on-highlight:#3B0E0E;
    --ac-nav-bg:#0B1D21; --ac-nav-fg:#F2FAF6;
    --ac-icon-color:#DCECEE;
    --ac-pattern:url("/static/pattern-dark.svg");
    --ac-shadow-sm:0 1px 2px rgba(0,0,0,.30), 0 2px 6px rgba(0,0,0,.24);
    --ac-shadow-md:0 4px 10px rgba(0,0,0,.34), 0 12px 28px rgba(0,0,0,.30);
    --ac-shadow-lg:0 10px 24px rgba(0,0,0,.40), 0 28px 60px rgba(0,0,0,.36);
    --vv-border-strong:#2F636B;
    --vv-accent-soft:rgba(78,205,196,.16);
    --vv-active-soft:rgba(109,217,208,.20);
    --vv-chord:var(--ac-highlight); --vv-danger:var(--ac-highlight);
    --vv-on-active:var(--ac-primary-800);
    --vv-state-soft:rgba(255,230,109,.16); --vv-state-text:var(--ac-yellow);
    --vv-danger-soft:rgba(255,138,138,.16); --vv-danger-text:var(--ac-highlight);
    --vv-text-muted:#A9C3C6; --vv-text-subtle:#93AFB2;
  }
  * { box-sizing:border-box; }
  a { color:var(--ac-turquoise-700); text-decoration:none; }
  a:hover { color:var(--vv-accent); }
  a.brand { color:var(--vv-header-text); }
  a.brand:hover { color:var(--vv-header-text); }
  /* Fondo crema con el mosaico de notas del kit (regla 6: solo sobre crema y
     sin tintar). En oscuro no se pone: el mosaico está dibujado en tonos crema
     y sobre el fondo oscuro sería ruido. */
  body { margin:0; background:var(--vv-bg); color:var(--vv-text);
         font:400 16px/1.6 var(--vv-font-ui); -webkit-font-smoothing:antialiased; }
  /* Mosaico de notas de fondo (regla 6): va en el fondo de PÁGINA y tal cual,
     sin tintar. El kit trae su versión night, así que ahora sale en los dos
     temas: la elige el token --ac-pattern. En una capa propia detrás de todo y
     con pointer-events en none, para no comerse ningún clic. */
  body::before { content:""; position:fixed; inset:0; z-index:-1; pointer-events:none;
                 background-image:var(--ac-pattern); background-repeat:repeat;
                 background-size:400px 400px; }
  h1, h2, h3, h4 { font-family:var(--vv-font-head); color:var(--vv-head);
                   font-weight:700; letter-spacing:-.02em; line-height:1.15; }
  button, input, select, textarea { font:inherit; color:inherit; }
  /* Regla 8 del kit: el foco se ve siempre, y en turquesa. */
  :focus-visible { outline:3px solid var(--vv-focus); outline-offset:2px;
                   border-radius:var(--vv-radius-sm); }
  ::selection { background:var(--ac-turquoise-200); color:var(--ac-ink); }
  /* Nada cuadrado (regla 3): los controles son píldoras. */
  /* Los botones secundarios tienen SUPERFICIE, no solo contorno: sobre el crema
     con mosaico, un borde pálido y letra fina no se leían como algo pulsable, y
     «Cargar más» directamente se perdía en el fondo. */
  button { cursor:pointer; border:1.5px solid var(--vv-border-strong);
           background:var(--vv-surface-alt); color:var(--vv-accent);
           border-radius:var(--vv-radius-pill); padding:9px 18px;
           font-weight:600; font-size:14px; white-space:nowrap;
           box-shadow:var(--vv-shadow-card);
           transition:background var(--ac-dur) var(--ac-ease), color var(--ac-dur) var(--ac-ease),
                      border-color var(--ac-dur) var(--ac-ease); }
  /* El tinte de hover es para los CONTROLES. Las tarjetas también son <button>
     (se pulsan enteras), pero tienen superficie propia, y --vv-accent-soft es
     semitransparente: al pasar por encima dejaban ver el mosaico del fondo a
     través de la tarjeta. Se excluyen aquí y se les da su propio hover. */
  button:hover:not(.card):not(.chordCard) { background:var(--vv-accent-soft);
                                            border-color:var(--vv-accent); }
  button.primary { background:var(--vv-accent); color:var(--vv-on-accent);
                   border-color:transparent; }
  button.primary:hover { background:var(--vv-accent-strong); }
  button.ghost { border-color:transparent; background:transparent; color:var(--vv-text);
                 box-shadow:none; }
  button:disabled { opacity:.45; cursor:default; background:transparent; }
  input[type=text], input[type=email], input[type=password], input[type=url], textarea, select {
    background:var(--vv-surface-alt); border:1.5px solid var(--vv-border);
    border-radius:var(--vv-radius-md); padding:10px 14px; width:100%; color:var(--vv-head); }
  input:hover, textarea:hover, select:hover { border-color:var(--ac-turquoise-400); }
  input:focus, textarea:focus, select:focus { border-color:var(--vv-focus); outline:none;
    box-shadow:0 0 0 3px var(--vv-active-soft); }
  input::placeholder, textarea::placeholder { color:var(--vv-text-subtle); }
  /* Barra superior teal maciza, como en la guía del kit. */
  header { position:sticky; top:0; z-index:10; display:flex; gap:12px; align-items:center;
           padding:12px 24px; background:var(--vv-header-bg); color:var(--vv-header-text);
           border-bottom:0; box-shadow:var(--ac-shadow-sm); }
  header .brand { display:flex; align-items:center; gap:12px; min-width:0; }
  header .brand svg { flex:0 0 auto; }
  header h1 { margin:0; font-size:22px; font-weight:700; letter-spacing:-.02em; line-height:1;
              color:var(--vv-header-text); }
  header .kicker { display:block; font-size:9px; letter-spacing:.18em; font-weight:600;
                   text-transform:uppercase; color:var(--ac-turquoise-200); margin-top:4px; }
  /* Sobre el teal, los botones de la cabecera van en claro. */
  /* En la barra teal los botones NO llevan la superficie clara de los demás:
     ahí el fondo ya es macizo y una píldora blanca con letra clara se quedaba
     ilegible. Contorno claro y letra crema. */
  header button { background:transparent; box-shadow:none;
                  border-color:rgba(242,250,246,.45); color:var(--vv-header-text); }
  /* El velo claro de antes quedaba casi del color del icono y se lo comía. Sobre
     la barra siempre hay teal, así que el hover OSCURECE en vez de aclarar, y el
     icono se pasa al amarillo de la marca (lo que el kit usa para los iconos de
     la barra): amarillo sobre teal oscuro se ve de lejos.
     Los :not() son para ganarle al hover genérico de los botones, que si no
     repintaría el fondo en menta clara y volvería a comerse el icono. */
  header button:hover:not(.card):not(.chordCard) {
    background:rgba(8,29,33,.42); border-color:var(--ac-yellow); color:var(--ac-yellow); }
  header button:hover .iconoTema { color:var(--ac-yellow); }
  header button.primary { background:var(--ac-turquoise); color:var(--ac-primary-800);
                          border-color:transparent; }
  header button.primary:hover { background:var(--ac-turquoise-600); }
  header #who { color:var(--ac-turquoise-200); }
  header .grow { flex:1; }
  main { padding:16px; max-width:1520px; margin:0 auto; }
  #listView, #authView { max-width:960px; margin:0 auto; }
  .tabs { display:flex; gap:8px; margin-bottom:14px; flex-wrap:wrap; }
  .tabs button { border-color:var(--vv-border-strong); color:var(--vv-accent); }
  .tabs button[aria-selected=true] { background:var(--vv-active); color:var(--vv-on-active);
                                     border-color:transparent; box-shadow:var(--vv-shadow-pop); }
  .tabs button[aria-selected=true]:hover { background:var(--ac-turquoise-600); }
  .grid { display:grid; gap:10px; grid-template-columns:repeat(auto-fill,minmax(230px,1fr)); align-items:start; }
  /* Las tarjetas son <button>, y la regla de arriba les pone nowrap: sin esto
     un titulo largo se sale de la tarjeta por la derecha. */
  .card, .chordCard { white-space:normal; }
  .card { display:block; text-align:left; width:100%; border:1.5px solid transparent;
          border-radius:var(--vv-radius-lg); padding:16px 18px; background:var(--vv-surface);
          box-shadow:var(--vv-shadow-card);
          transition:box-shadow var(--ac-dur) var(--ac-ease),
                     border-color var(--ac-dur) var(--ac-ease),
                     transform var(--ac-dur) var(--ac-ease); }
  /* La tarjeta no cambia de color al pasar por encima: se levanta. Mantiene su
     superficie opaca (si no, se transparenta sobre el mosaico) y solo se le
     suben la sombra y el borde. */
  .card:hover { background:var(--vv-surface); border-color:var(--vv-active);
                box-shadow:var(--vv-shadow-pop); transform:translateY(-2px); }
  .card:active { transform:translateY(0); box-shadow:var(--vv-shadow-card); }
  .card .t { font-family:var(--vv-font-head); font-weight:700; letter-spacing:-.01em;
             color:var(--vv-head); }
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
  .versions button[aria-pressed=true] { background:var(--vv-active); color:var(--vv-on-active);
                                        border-color:transparent; }
  .versions .sep { flex:1 1 auto; min-width:8px; }
  /* Fichas de la cola de revisión y del listado de usuarios. */
  .fila { display:flex; gap:12px; align-items:flex-start; flex-wrap:wrap;
          border:1px solid var(--vv-border); border-radius:var(--vv-radius-lg);
          background:var(--vv-surface); padding:14px 16px; margin-bottom:10px; }
  .fila .cuerpo { flex:1 1 320px; min-width:0; }
  .fila .t { font-weight:600; overflow-wrap:anywhere; }
  .fila .meta { font-size:13px; color:var(--vv-text-muted); overflow-wrap:anywhere; }
  .fila .nota { margin-top:6px; font-size:14px; overflow-wrap:anywhere; }
  .estado { font-size:12px; border-radius:var(--vv-radius-pill); padding:4px 12px;
            border:1.5px solid transparent; background:var(--vv-accent-soft);
            color:var(--vv-accent); font-weight:600; }
  .estado[data-s=pending] { background:var(--vv-state); color:var(--vv-on-state);
                            border-color:transparent; }
  .estado[data-s=approved] { color:var(--vv-beat); border-color:var(--vv-beat); }
  .estado[data-s=rejected], .estado[data-s=withdrawn] { color:var(--vv-danger); border-color:var(--vv-danger); }
  .aviso { background:var(--vv-active-soft); border:1.5px solid var(--ac-turquoise-400);
           color:var(--vv-head); border-radius:var(--vv-radius-md); padding:11px 14px;
           margin-bottom:12px; font-size:14px; }
  .filtros { display:flex; gap:8px; margin-bottom:12px; flex-wrap:wrap; align-items:flex-end; }
  /* La etiqueta va encima del control: antes el nombre del filtro solo existía
     como title=, que ni se lee con lector de pantalla ni se ve al tocar. */
  .filtro { display:flex; flex-direction:column; gap:4px; flex:0 1 190px; font-size:11px;
            text-transform:uppercase; letter-spacing:.08em; color:var(--vv-text-muted); }
  .filtro:first-child { flex:1 1 220px; }
  /* El control ocupa el ancho de su etiqueta y NADA de flex propio: dentro de
     una columna, un flex-basis se convertiría en altura y salen cajas gigantes. */
  .filtro input, .filtro select { width:100%; flex:0 0 auto; }
  /* El estado relleno lo pone .tool[aria-pressed=true] con --tono. */
  #favFilter[aria-pressed=true] .ic { color:var(--ac-on-highlight); }
  .listas { display:flex; gap:8px; align-items:center; flex-wrap:wrap; margin-bottom:12px; }
  .listas .etiqueta { font-size:11px; text-transform:uppercase; letter-spacing:.08em;
                      color:var(--vv-text-muted); }
  .chips { display:flex; gap:6px; flex-wrap:wrap; }
  .chip { border:1px solid var(--vv-border); border-radius:999px; padding:4px 12px;
          font-size:13px; background:transparent; color:inherit; cursor:pointer; }
  .chip[aria-pressed=true] { background:var(--vv-active); color:var(--vv-on-active);
                             border-color:var(--vv-active); font-weight:600; }
  .card .fav { float:right; border:0; background:transparent; font-size:16px; line-height:1;
               padding:0 0 0 8px; cursor:pointer; color:var(--vv-text-muted); }
  .card .fav[aria-pressed=true] { color:var(--ac-coral); }
  .acciones { display:flex; gap:6px; margin-top:8px; }
  /* Antes: fondo transparente y un radio inexistente (--vv-radius), así que la
     sección se leía sobre el mosaico del fondo y las esquinas salían rectas. */
  .adminTool { border:1.5px solid var(--vv-border); border-radius:var(--vv-radius-lg);
               background:var(--vv-surface); box-shadow:var(--vv-shadow-card);
               padding:16px 18px; margin-bottom:12px; }
  .adminTool h3 { margin:0 0 6px; font-size:16px; font-family:var(--vv-font-head);
                  font-weight:700; letter-spacing:-.01em; color:var(--vv-head); }
  .adminTool p { margin:0 0 10px; font-size:13px; color:var(--vv-text-muted); }
  /* Las etiquetas van en su propia fila con separación real: pegadas unas a
     otras parecían una sola píldora partida. */
  .etiquetas { display:flex; flex-wrap:wrap; gap:8px; margin-top:12px; }
  .badge { display:inline-block; font-size:12px; border-radius:var(--vv-radius-pill);
           padding:4px 12px; border:1.5px solid transparent; background:var(--vv-accent-soft);
           color:var(--vv-accent); font-weight:600; }
  /* Cada etiqueta dice una cosa distinta, así que no todas son del mismo color:
     categoría en el teal de marca, pública en turquesa (estado), privada sin
     relleno y lista en amarillo. */
  /* Además del color cambia la FORMA, que es lo que se distingue de un vistazo
     y también en oscuro, donde varios tintes turquesa se parecen entre sí:
     pública va rellena, privada solo de contorno y la lista en amarillo. */
  .badge.publica { background:var(--vv-active); color:var(--vv-on-active); }
  .badge.privada { background:transparent; border-color:var(--vv-border-strong);
                   color:var(--vv-text-muted); }
  .badge.lista { background:var(--vv-state-soft); color:var(--vv-state-text);
                 border-color:var(--vv-state); }
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
  /* ---- editor: escribir · ver · ajustar ----
     Las dos primeras columnas se reparten lo que sobra y la de ajustes tiene
     ancho fijo: es un formulario, y estirarlo no lo hace más útil, mientras que
     cada píxel de más en la hoja sí se nota al escribir. */
  .editor { max-width:1760px; margin:0 auto; display:flex; flex-direction:column; gap:12px; }
  /*
   * Las tres columnas se alinean porque comparten estructura —cabecera de alto
   * fijo y debajo una caja del mismo alto— y no por casualidad. La ficha llevaba
   * su fondo en el propio panel, cabecera incluida, así que su caja empezaba
   * donde las OTRAS tenían el rótulo: 38 px más arriba, y otros tantos de menos
   * por abajo.
   */
  #editSplit { display:grid; grid-template-columns:1fr 1fr clamp(280px, 22vw, 340px); gap:14px;
               align-items:stretch; --alto-editor:calc(100vh - 210px); }
  .pane { display:flex; flex-direction:column; gap:6px; min-width:0; }
  .pane .hd { display:flex; align-items:center; gap:8px; font-size:11px; letter-spacing:.18em;
              text-transform:uppercase; color:var(--vv-text-subtle);
              height:38px; flex:0 0 auto; }
  .pane .hd small { letter-spacing:0; text-transform:none; font-size:12px; }
  /* Un solo alto para las tres cajas: cambiarlo es tocar --alto-editor. */
  #eContent, #ePreview, #eSideBox { height:var(--alto-editor); }
  #eContent, #ePreview { min-height:320px; }
  /* La hoja ocupa lo que hay: es el trabajo de verdad. */
  #eContent { resize:vertical;
              font-family:var(--vv-font-mono); font-size:14px; line-height:1.45; white-space:pre; }
  #ePreview { overflow:auto;
              background:var(--vv-surface); border:1.5px solid var(--vv-border);
              border-radius:var(--vv-radius-md); padding:10px 12px;
              font-family:var(--vv-font-mono); font-size:14px; line-height:1.45; }
  /* Misma altura de linea que el textarea: asi la linea N de la izquierda cae
     a la altura de la linea N de la derecha y el scroll atado cuadra. En el
     visor se mantiene 1.35, que ahi se lee mas apretado y no hay con que
     comparar. */
  #ePreview .ln, #ePreview .tab { line-height:1.45; }

  /* Columna de ajustes: se desplaza sola si no cabe, y los botones quedan
     siempre abajo, pegados al borde de la tarjeta. */
  #eSideBox { display:flex; flex-direction:column;
              background:var(--vv-surface); border:1.5px solid var(--vv-border);
              border-radius:var(--vv-radius-lg); box-shadow:var(--vv-shadow-card);
              padding:14px; }
  /*
   * min-height:0 no es un adorno: sin él, un hijo flexible NO encoge por debajo
   * de su contenido, así que la lista de campos empujaba la caja hacia abajo, el
   * overflow no llegaba a activarse y había que recorrer la página entera para
   * llegar al último campo. Con esto el scroll se queda dentro del panel.
   */
  #eSideBody { display:flex; flex-direction:column; gap:12px;
               overflow-y:auto; flex:1 1 auto; min-height:0; padding-right:4px; }
  #eSide label { display:block; font-size:13px; }
  #eSide label input, #eSide label select { margin-top:4px; }
  #eVersionHead { display:flex; flex-direction:column; gap:12px; }
  #eAcciones { display:flex; flex-direction:column; gap:8px; flex:0 0 auto;
               margin-top:12px; padding-top:12px; border-top:1.5px solid var(--vv-border); }
  #eAcciones .msg:empty, #eAcciones .aviso.hidden { display:none; }

  /*
   * La ficha sigue siendo una COLUMNA mientras quepa, hasta 1000 px. Antes bajaba
   * a lo ancho ya en 1200, y entonces para tocar la categoría o el capo había que
   * recorrer toda la página: el panel dejaba de tener scroll propio. En un
   * portátil de 1280 —o con la ventana sin maximizar— eso era el caso normal.
   */
  @media (max-width:1280px) {
    /* La vista previa es lo primero que sobra: se escribe mirando el textarea. */
    #editSplit { grid-template-columns:1.2fr 1fr clamp(260px, 24vw, 320px); }
    /* Las aclaraciones del rótulo se parten en dos líneas y se salen de los
       38 px de la cabecera. Son ayuda, no información: se quitan antes que
       descuadrar las columnas. */
    #editSplit .hd small { display:none; }
  }
  /*
   * Pantalla baja (portátiles de 768, o con la ventana sin maximizar): los
   * botones en columna se comían 156 px de los 390 que quedaban, y la ficha se
   * veía por una rendija de dos campos. En fila ocupan la mitad.
   */
  @media (max-height:820px) and (min-width:1001px) {
    #eAcciones { flex-direction:row; flex-wrap:wrap; align-items:center; }
    #eAcciones button { flex:1 1 auto; }
    #eAcciones .msg, #eAcciones .aviso { flex:1 1 100%; }
  }
  @media (max-width:1000px) {
    /* Ya no caben tres: la ficha baja a lo ancho, en rejilla, y ahí sí fluye con
       la página porque no hay ninguna columna con la que alinearse. */
    #editSplit { grid-template-columns:1fr 1fr; }
    #eSide { grid-column:1 / -1; }
    #eSideBox { height:auto; }
    #eSideBody { display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr));
                 align-items:end; overflow:visible; }
    #eAcciones { flex-direction:row; flex-wrap:wrap; align-items:center; }
  }
  @media (max-width:900px) {
    #editSplit { grid-template-columns:1fr; --alto-editor:40vh; }
    #eContent, #ePreview { min-height:220px; }
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
           background:var(--vv-state); color:var(--vv-on-state); border:0;
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
  .chord { color:var(--vv-chord); font-weight:600; }
  #vBody .chord { cursor:pointer; border-radius:3px; }
  /* Globo de digitaciones al pasar por encima. Fuera del flujo y sin capturar
     el ratón: si lo capturase, entrar en él contaría como salir del acorde. */
  #chordHover { position:fixed; z-index:45; display:none; gap:8px; padding:10px;
                pointer-events:none; background:var(--vv-surface);
                border:1px solid var(--vv-border-strong); border-radius:var(--vv-radius-md);
                box-shadow:0 6px 24px rgba(0,0,0,.35); }
  #chordHover.on { display:flex; }
  #chordHover .nm { font-family:var(--vv-font-mono); font-weight:600; color:var(--vv-chord);
                    align-self:center; padding-right:2px; }
  /* Fila de botones para capo y traste base: un toque en vez de teclear. */
  .pills { display:flex; flex-wrap:wrap; gap:4px; }
  .pills button { padding:6px 10px; font-family:var(--vv-font-mono); font-size:13px;
                  min-width:38px; }
  .pills button[aria-pressed=true] { background:var(--vv-active); color:var(--vv-on-active);
                                     border-color:transparent; }
  #vBody .chord:hover, #vBody .chord:focus-visible { background:var(--ac-coral-200); color:var(--ac-coral-700); }
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
  .vRow:hover { background:var(--vv-active-soft); }
  .vRow[aria-pressed=true] { border-color:var(--vv-active); background:var(--vv-active-soft); }
  .vRow .num { font-family:var(--vv-font-mono); font-size:12px; color:var(--vv-text-subtle); }
  .vRow .nm { font-weight:600; overflow-wrap:anywhere; }
  .vRow .val { display:flex; align-items:center; gap:6px; }
  .stars { display:inline-flex; gap:1px; }
  .stars button, .stars span { border:0; background:none; padding:0 1px; font-size:15px;
                               line-height:1; color:var(--vv-border-strong); cursor:default; }
  .stars button { cursor:pointer; }
  .stars .on { color:var(--ac-yellow-600); }
  .nota { font-family:var(--vv-font-mono); font-size:12px; color:var(--vv-text-muted); }
  /* Comentarios, al final de la partitura. */
  #vComments { max-width:100%; margin:32px 0 0; border-top:1px solid var(--vv-border);
               padding-top:16px; font-family:var(--vv-font-ui); font-size:15px; }
  #vComments h4, #vRelated h4 { margin:0 0 12px; font-size:11px; letter-spacing:.18em;
                  text-transform:uppercase; color:var(--vv-text-subtle); font-weight:600; }
  /* Recomendadas al pie de la partitura: mismas tarjetas que el catálogo, en
     una rejilla más apretada porque aquí compiten con la hoja, no la sustituyen. */
  #vRelated { max-width:100%; margin:28px 0 0; border-top:1px solid var(--vv-border);
              padding-top:16px; font-family:var(--vv-font-ui); font-size:15px; }
  #vRelated:empty { display:none; }
  #vRelated h4 { color:var(--vv-chord); }
  .relGrid { display:grid; gap:10px; grid-template-columns:repeat(auto-fill,minmax(210px,1fr));
             align-items:start; }
  .comentario { padding:10px 0; border-bottom:1px solid var(--vv-border); }
  .comentario .quien { font-size:13px; color:var(--vv-text-muted); display:flex; gap:8px; align-items:baseline; }
  .comentario .texto { white-space:pre-wrap; overflow-wrap:anywhere; margin-top:4px; }
  .comentario .quitar { border:0; background:none; color:var(--vv-danger); font-size:12px;
                        padding:0; cursor:pointer; }
  #vCommentForm { display:flex; flex-direction:column; gap:8px; margin-top:12px; }
  #vCommentForm textarea { min-height:80px; font-family:var(--vv-font-ui); }
  /* ---- visor en pantalla estrecha ---- */
  /* Con selector de elemento a propósito: .tool también fija display y, a
     igualdad de peso, ganaba la que va después en la hoja. */
  button.soloEstrecho { display:none; }
  @media (max-width:900px) {
    /*
     * Barra de una sola línea: atrás, título y un botón de acciones. Antes se
     * apilaba en tres filas y se comía un tercio de la pantalla, que es hoja
     * que no se lee. Lo que había en la barra vive ahora en el menú.
     */
    #vHead { grid-template-columns:auto 1fr auto; gap:8px; padding:6px 10px;
             position:relative; }
    #vHead .vMeta { min-width:0; flex-wrap:nowrap; gap:8px; }
    #vHead .titulo { min-width:0; }
    #vHead .lado.der { justify-content:flex-end; }
    #vClose { padding:6px 10px; }
    /* El capo se queda —cambia cómo suena todo— pero en pequeño. */
    #vCapo { padding:2px 8px 3px; font-size:10px; }
    #vCapo .n { font-size:13px; }

    /* Menú de acciones colgando del botón. */
    #vMenu { position:absolute; top:calc(100% + 6px); right:8px; z-index:32;
             display:flex; flex-direction:column; align-items:stretch; gap:8px;
             min-width:190px; padding:10px; background:var(--vv-surface);
             border:1.5px solid var(--vv-border); border-radius:var(--vv-radius-lg);
             box-shadow:var(--ac-shadow-lg); }
    #vMenu[hidden] { display:none; }
    #vMenu > * { justify-content:flex-start; width:100%; }
    #vMenu #vSource { padding:9px 14px; border:1.5px solid var(--vv-border-strong);
                      border-radius:var(--vv-radius-pill); font-weight:600;
                      font-size:14px; background:var(--vv-surface-alt); }
    button.soloEstrecho { display:inline-flex; }

    /*
     * Los mandos, en un panel que sube desde abajo. En el móvil la partitura es
     * lo único que tiene que estar en pantalla; los mandos se piden, se usan y
     * se van. Va anclado abajo porque es donde llega el pulgar, y con velo para
     * que quede claro que el resto está en pausa.
     */
    #vMain { flex-direction:column; gap:10px; padding:10px; }
    /* Sin reserva de barra: en el móvil el desplazamiento va por encima y esos
       ~15 px son ancho de hoja, que es lo que escasea. */
    #vBody { padding:12px 12px 60px; scrollbar-gutter:auto; }
    #vSide { position:fixed; left:0; right:0; bottom:0; z-index:31;
             flex:0 0 auto; max-height:76vh; overflow:auto;
             padding:10px 14px calc(16px + env(safe-area-inset-bottom));
             background:var(--vv-surface); border-top:1.5px solid var(--vv-border);
             border-radius:var(--vv-radius-lg) var(--vv-radius-lg) 0 0;
             box-shadow:var(--ac-shadow-lg);
             transform:translateY(101%); transition:transform 280ms var(--ac-ease); }
    /* Asa: dice «esto se arrastra/se cierra» sin gastar una línea de texto. */
    #vSide::before { content:""; display:block; width:44px; height:4px; flex:0 0 auto;
                     border-radius:999px; background:var(--vv-border-strong);
                     margin:0 auto 10px; }
    #viewer.conMandos #vSide { transform:none; }
    /* Cerrar también con un botón: tocar fuera funciona, pero no se ve. */
    #vToolsClose { position:absolute; top:8px; right:10px; padding:6px 12px;
                   font-size:13px; color:var(--vv-accent); }
    #vSide { position:fixed; }
    /* Dentro del panel, el recuadro de mandos ya no necesita su propia caja. */
    #vCtrl { background:transparent; border:0; box-shadow:none; padding:0; }
    #vCtrl .row { flex-wrap:wrap; }
    #vVelo { position:fixed; inset:0; z-index:30; background:rgba(8,29,33,.45);
             border:0; padding:0; }

    /* El botón que lo abre: abajo, al alcance del pulgar y sobre la hoja. */
    #vTools { position:fixed; z-index:29; right:16px;
              bottom:calc(16px + env(safe-area-inset-bottom));
              padding:12px 20px; background:var(--vv-accent); color:var(--vv-on-accent);
              border-color:transparent; box-shadow:var(--ac-shadow-lg); }
    #vTools .ic { color:var(--vv-on-accent); --ac-icon-accent:var(--vv-on-accent); }
    #viewer.conMandos #vTools { display:none; }

    /* Las versiones se leen al terminar la partitura, no dentro de los mandos:
       ahí es donde se decide «pruebo esta otra». Las mueve colocarVersiones(). */
    #vBody > #vVersionPanel { margin-top:28px; max-height:none; }
  }
  @media (max-width:560px) {
    /* En el móvil la cabecera se queda con lo imprescindible: marca, tema y
       salir. El lema y el nombre de quien entra partían la barra en dos líneas. */
    header { padding:10px 12px; gap:8px; }
    header .kicker { display:none; }
    header #who { display:none; }
    header h1 { font-size:19px; }

    /* En la barra solo queda el botón de acciones, y ahí basta el icono; dentro
       del menú los nombres se leen enteros. */
    #vHead > .lado .tool span { position:absolute; width:1px; height:1px;
                                overflow:hidden; clip-path:inset(50%); white-space:nowrap; }
    #vHead > .lado .tool { padding:8px 10px; }
    #vTitle, #vArtist { font-size:14px; }
    /* Título y artista en una línea, con puntos suspensivos si no cabe. */
    #vHead .titulo > * { max-width:46vw; }
  }
  #vCtrl .row label { font-size:12px; color:var(--vv-text-muted); white-space:nowrap; }
  #vTone { color:var(--vv-accent); font-weight:600; }
  #vCtrl input[type=range] { flex:1; min-width:80px; width:auto; padding:0; border:0;
                             background:transparent; accent-color:var(--vv-accent); }
  /* Cifras (BPM, tono, velocidad, capo) siempre en la mono de la marca. */
  #vSpeedVal, #vBpmVal, #vTone {
    font-family:var(--vv-font-mono); font-variant-numeric:tabular-nums; }
  .beat { width:12px; height:12px; border-radius:50%; background:var(--vv-border-strong); display:inline-block; }
  .beat.on { background:var(--vv-state); box-shadow:var(--vv-glow); }
  /* ---- acordes ---- */
  .chordSvg { display:block; color:var(--ac-primary); }
  .chordBar { display:flex; gap:8px; overflow-x:auto; padding:8px 14px; background:var(--vv-surface);
              border-bottom:1px solid var(--vv-border); }
  .chordBar button { flex:0 0 auto; display:flex; flex-direction:column; align-items:center; gap:2px;
                     padding:6px 8px; border-color:transparent; }
  .chordBar .nm { font-family:var(--vv-font-mono); font-size:12px; font-weight:600; color:var(--vv-chord); }
  .chordBar .none { color:var(--vv-text-subtle); font-size:13px; padding:8px 4px; white-space:nowrap; }
  .modal { position:fixed; inset:0; z-index:40; background:rgba(8,29,33,.55);
           display:flex; align-items:center; justify-content:center; padding:16px; }
  .modalBox { background:var(--vv-surface); border:1.5px solid var(--vv-border); width:100%;
              box-shadow:var(--ac-shadow-lg);
              border-radius:var(--vv-radius-lg); padding:16px; max-width:780px; max-height:86vh;
              overflow:auto; display:flex; flex-direction:column; gap:12px; }
  .modalBox h3 { margin:0; font-size:20px; font-family:var(--vv-font-head); font-weight:700;
                 letter-spacing:-.02em; color:var(--vv-head);
                 min-width:0; overflow-wrap:anywhere; }
  .chordCard .nm { overflow-wrap:anywhere; text-align:center; }
  .chordGrid { display:grid; gap:10px; grid-template-columns:repeat(auto-fill,minmax(118px,1fr)); }
  .chordCard { display:flex; flex-direction:column; align-items:center; gap:4px; padding:10px;
               border:1.5px solid var(--vv-border); border-radius:var(--vv-radius-md);
               background:var(--vv-surface);
               transition:box-shadow var(--ac-dur) var(--ac-ease),
                          border-color var(--ac-dur) var(--ac-ease),
                          transform var(--ac-dur) var(--ac-ease); }
  /* Mismo trato que las tarjetas de partitura: las del diccionario también son
     <button> con superficie propia. */
  button.chordCard:hover { background:var(--vv-surface); border-color:var(--vv-active);
                           box-shadow:var(--vv-shadow-pop); transform:translateY(-2px); }
  button.chordCard:active { transform:translateY(0); box-shadow:none; }
  .chordCard .nm { font-family:var(--vv-font-mono); font-weight:600; }
  .chordCard .va { font-size:11px; color:var(--vv-text-subtle); }
  .posRow { display:flex; gap:12px; align-items:center; flex-wrap:wrap; padding:10px;
            border:1px solid var(--vv-border); border-radius:var(--vv-radius-md); }
  .posRow input { width:54px; text-align:center; font-family:var(--vv-font-mono); padding:6px 4px; }
  .posRow .lbl { font-size:11px; letter-spacing:.14em; text-transform:uppercase; color:var(--vv-text-subtle); }
  .posRow .grp { display:flex; gap:4px; align-items:center; }
  .posRow .pills button { padding:4px 8px; min-width:30px; font-size:12px; }
  /* Avisos flotantes y diálogos propios: sustituyen a alert/confirm/prompt del
     navegador, que salían con la tipografía y los colores del sistema y encima
     bloqueaban la página entera mientras estaban abiertos. */
  .toasts { position:fixed; z-index:60; left:50%; transform:translateX(-50%);
            bottom:calc(16px + env(safe-area-inset-bottom)); display:flex; gap:8px;
            flex-direction:column; align-items:stretch; pointer-events:none;
            width:min(440px,calc(100vw - 32px)); }
  .toast { pointer-events:auto; cursor:pointer; background:var(--vv-surface);
           border:1.5px solid var(--vv-border); border-left:4px solid var(--vv-accent);
           border-radius:var(--vv-radius-md); box-shadow:var(--ac-shadow-lg);
           padding:11px 14px; font-size:14px; color:var(--vv-text); overflow-wrap:anywhere;
           transition:opacity .18s ease, transform .18s ease; }
  .toast.ok { border-left-color:var(--ac-turquoise); }
  .toast.error { border-left-color:var(--ac-coral); }
  .toast.in { opacity:0; transform:translateY(10px); }
  .toast.out { opacity:0; transform:translateY(6px); }
  .dlgBox { max-width:440px; }
  .dlgText { margin:0; color:var(--vv-text-muted); white-space:pre-line; overflow-wrap:anywhere; }
  .dlgField { display:flex; flex-direction:column; gap:6px; }
  .dlgField .lbl { font-size:11px; letter-spacing:.14em; text-transform:uppercase;
                   color:var(--vv-text-subtle); }
  .dlgBtns { display:flex; gap:8px; justify-content:flex-end; flex-wrap:wrap; }
  button.danger { background:var(--vv-danger-solid); color:var(--vv-on-danger);
                  border-color:transparent; }
  button.danger:hover { background:var(--ac-coral-600); }
  /* Quien pide menos movimiento en el sistema no ve el levantamiento: se queda
     el cambio de sombra y borde, que ya dice lo mismo. */
  @media (prefers-reduced-motion: reduce) {
    #vSide { transition:none; }
    .card, .chordCard, .toast, button { transition:none; }
    .card:hover, button.chordCard:hover, .card:active, button.chordCard:active { transform:none; }
  }
  /* Botón de solo icono: cuadrado, redondo y sin el relleno de los botones de
     texto. Vale para la cabecera y para cualquier control que sea solo símbolo. */
  .iconBtn { display:inline-grid; place-items:center; width:40px; height:40px; padding:0; }
  .iconBtn svg { width:20px; height:20px; display:block; }

  /* Sol y luna comparten hueco y se turnan: el que entra llega girando desde
     -90°, el que sale se va girando y encogiendo. Se anima transform y opacity
     (las dos las resuelve el compositor), no display, que no se puede animar. */
  .iconoTema .sol, .iconoTema .luna {
    transform-origin:12px 12px;
    transition:transform 340ms var(--ac-ease), opacity 220ms var(--ac-ease);
  }
  .iconoTema .luna { opacity:0; transform:rotate(-90deg) scale(.4); }
  .iconoTema .sol { opacity:1; transform:none; }
  /* Los rayos, además, se abren al aparecer el sol. */
  .iconoTema .sol path { transition:opacity 220ms var(--ac-ease) 120ms; }
  #themeBtn:hover .iconoTema .sol { transform:rotate(20deg); }
  #themeBtn:hover .iconoTema .luna { transform:rotate(-14deg); }

  [data-theme=dark] .iconoTema .sol { opacity:0; transform:rotate(90deg) scale(.4); }
  [data-theme=dark] .iconoTema .sol path { opacity:0; transition-delay:0ms; }
  [data-theme=dark] .iconoTema .luna { opacity:1; transform:none; }
  [data-theme=dark] #themeBtn:hover .iconoTema .sol { transform:rotate(90deg) scale(.4); }
  @media (prefers-color-scheme: dark) {
    :root:not([data-theme=light]) .iconoTema .sol { opacity:0; transform:rotate(90deg) scale(.4); }
    :root:not([data-theme=light]) .iconoTema .sol path { opacity:0; transition-delay:0ms; }
    :root:not([data-theme=light]) .iconoTema .luna { opacity:1; transform:none; }
    :root:not([data-theme=light]) #themeBtn:hover .iconoTema .sol { transform:rotate(90deg) scale(.4); }
  }
  @media (prefers-reduced-motion: reduce) {
    .iconoTema .sol, .iconoTema .luna, .iconoTema .sol path { transition:none; }
    #themeBtn:hover .iconoTema .sol, #themeBtn:hover .iconoTema .luna { transform:none; }
  }
  /* Quien pide menos movimiento en el sistema no ve el levantamiento: se queda
     el cambio de sombra y borde, que ya dice lo mismo. */
  @media (prefers-reduced-motion: reduce) {
    .card, .chordCard, .toast, button { transition:none; }
    .card:hover, button.chordCard:hover, .card:active, button.chordCard:active { transform:none; }
  }
  /* ---- botones con icono ---- */
  .tool { display:inline-flex; align-items:center; gap:8px; }
  .ic { width:18px; height:18px; flex:0 0 auto; }

  /*
   * Cada herramienta del visor lleva el color de su papel, y no seis botones de
   * texto iguales: turquesa lo que corre, amarillo el pulso, coral los acordes
   * y teal el tono. El color lo hereda todo el bloque, así que añadir una
   * herramienta nueva es elegirle color, no repetir cinco reglas.
   *
   * Dos variables y no una: --tono es el RELLENO cuando la herramienta está
   * activa, y --tono-glifo el color del icono y del borde sobre la superficie
   * clara. Hacen falta las dos porque el turquesa, el amarillo y el coral de
   * marca son rellenos: como trazo sobre la tarjeta clara se quedan en 2:1 y el
   * icono desaparece. Para dibujar se usa la rampa que sí contrasta (la misma
   * que ya usan los acordes y las estrellas), y en oscuro son el color vivo.
   */
  .tool { --tono:var(--vv-accent); --tono-texto:var(--vv-on-accent);
          --tono-glifo:var(--vv-accent); --ac-icon-accent:var(--tono-glifo); }
  .tool .ic { color:var(--tono-glifo); }
  .tool:hover { border-color:var(--tono-glifo); }
  .tool[aria-pressed=true] { background:var(--tono); border-color:transparent;
                             color:var(--tono-texto); }
  .tool[aria-pressed=true] .ic { color:var(--tono-texto); --ac-icon-accent:var(--tono-texto); }
  .herr.scroll .tool  { --tono:var(--ac-active);    --tono-texto:var(--ac-primary-800);
                        --tono-glifo:var(--vv-beat); }
  .herr.metro .tool   { --tono:var(--ac-pending);   --tono-texto:var(--ac-yellow-900);
                        --tono-glifo:var(--vv-state-text); }
  .herr.acordes .tool { --tono:var(--ac-highlight); --tono-texto:var(--ac-on-highlight);
                        --tono-glifo:var(--vv-chord); }
  .herr.tono .tool    { --tono:var(--ac-action);    --tono-texto:var(--ac-on-action);
                        --tono-glifo:var(--vv-accent); }
  .tool.compartir     { --tono:var(--ac-highlight); --tono-glifo:var(--vv-chord);
                        --tono-texto:var(--ac-on-highlight); }
  .tool.imprimir      { --tono:var(--ac-action); --tono-glifo:var(--vv-accent); }
  /* «Nueva» es la acción destacada de la página: el kit reserva el coral justo
     para eso («acción destacada, nuevo»). Va rellena, no de contorno. */
  .tool.destacada { background:var(--ac-highlight); color:var(--ac-on-highlight);
                    border-color:transparent; --tono:var(--ac-highlight);
                    --ac-icon-accent:var(--ac-on-highlight); }
  .tool.destacada .ic { color:var(--ac-on-highlight); }
  .tool.destacada:hover { background:var(--ac-highlight-hover); }
  /* Favoritas: la estrella de las tarjetas ya es coral; el filtro va a juego. */
  .tool.favoritas { --tono:var(--ac-highlight); --tono-texto:var(--ac-on-highlight);
                    --tono-glifo:var(--vv-chord); }
  /* Ocultar publicadas es un filtro de estado: turquesa, y así no compite con
     el coral de «favoritas», que está al lado. */
  .tool.privadas { --tono:var(--ac-active); --tono-texto:var(--ac-primary-800);
                   --tono-glifo:var(--vv-beat); }
  /* Altas abiertas = estado activo (turquesa relleno); cerradas = contorno. */
  .tool.altas { --tono:var(--ac-active); --tono-texto:var(--ac-primary-800);
                --tono-glifo:var(--vv-beat); }
  /* La etiqueta del bloque también toma el color: es el rótulo de la sección. */
  .herr > label:first-child { font-size:11px; font-weight:600; letter-spacing:.14em;
                              text-transform:uppercase; }
  .herr.tono > label:first-child  { color:var(--vv-accent); }
  .herr.metro > label:first-child { color:var(--vv-state-text); }
  .herr.letra > label:first-child { color:var(--vv-text-muted); }
  /* Las barras deslizantes, del color de su herramienta. */
  #vCtrl #vSpeed { accent-color:var(--ac-active); }
  #vCtrl #vBpm { accent-color:var(--ac-pending); }
  #vSpeedVal { color:var(--vv-beat); }
  #vBpmVal { color:var(--vv-state-text); }

  /* «Cargar más» cierra el listado y es lo único que hay ahí abajo: va relleno
     con el color de acción y centrado, no como un contorno perdido en el fondo. */
  #moreBtn { display:block; margin:16px auto 0; padding:11px 28px;
             background:var(--vv-accent); color:var(--vv-on-accent);
             border-color:transparent; box-shadow:var(--vv-shadow-pop); }
  #moreBtn:hover:not(.card):not(.chordCard) { background:var(--vv-accent-strong);
                                              border-color:transparent; }
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
/*
 * Arranca en true: en "Mis partituras" lo que se viene a hacer es rematar lo que
 * falta, y lo ya publicado es justo lo que no necesita atención. El botón
 * "Solo privadas" apaga el filtro y devuelve el repertorio entero.
 */
var sinPublicas = true;
var chordDict = null;          // diccionario global, cacheado tras la primera carga
var chordBarOn = false;
var editingChord = null;       // nombre que se está editando, "" si es nuevo
var chordPositions = [];       // posiciones del acorde en edición
var altasAbiertas = true;      // ¿se pueden crear cuentas? lo dice el servidor

/** Cuántas partituras se piden de golpe. */
var PAGINA = 60;

/*
 * Buscar es cosa del SERVIDOR. Antes el buscador solo filtraba lo que ya estaba
 * descargado —la primera página de 60—, así que una partitura de la mitad del
 * catálogo no aparecía hasta darle a «Cargar más» las veces que hiciera falta:
 * el buscador parecía roto y en realidad estaba mirando por una rendija.
 *
 * A partir de 3 letras o cifras se le pregunta al Worker, que busca en título y
 * artista sobre TODO el listado. Y se piden de golpe (500 = el tope del Worker)
 * porque un resultado de búsqueda paginado es justo el problema de partida.
 */
var busqueda = "";             // texto que está filtrando en el servidor
var MIN_BUSQUEDA = 3;          // letras o cifras a partir de las cuales se busca
var PAGINA_BUSQUEDA = 500;
var temporizadorBusqueda = null;

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

/* ---------- avisos y diálogos con el estilo de la página ---------- */
/*
 * alert/confirm/prompt del navegador desentonaban (tipografía y colores del
 * sistema, ancho fijo, nada del tema claro/oscuro) y además congelan la página
 * mientras están abiertos: con el metrónomo sonando o una subida en marcha eso
 * se nota. Aquí van los tres en versión Vivace:
 *   notificar(texto, tipo)  aviso flotante que se va solo (no bloquea nada)
 *   confirmar(opciones)     -> Promise<boolean>
 *   pedirTexto(opciones)    -> Promise<string|null>  (null = ha cancelado)
 * Las dos últimas devuelven una promesa porque, al no bloquear, la respuesta
 * llega después: quien las use continúa dentro del .then().
 */
function notificar(texto, tipo) {
  if (!texto) return;
  var t = document.createElement("div");
  t.className = "toast in" + (tipo ? " " + tipo : "");
  // Un error se anuncia interrumpiendo; un "hecho" espera su turno.
  t.setAttribute("role", tipo === "error" ? "alert" : "status");
  t.textContent = texto;
  toastWrap.appendChild(t);
  requestAnimationFrame(function () { t.classList.remove("in"); });
  var quitar = function () {
    if (!t.parentNode) return;
    t.classList.add("out");
    setTimeout(function () { if (t.parentNode) t.parentNode.removeChild(t); }, 200);
  };
  t.onclick = quitar;                       // se puede descartar tocándolo
  setTimeout(quitar, tipo === "error" ? 6000 : 3000);
}

/** Atajo para el caso más repetido: enseñar el mensaje de error de la API. */
function avisarError(e) { notificar((e && e.message) || String(e), "error"); }

/**
 * Diálogo genérico. Con "input" pide texto y resuelve a la cadena (o null si
 * se cancela); sin él resuelve a true/false.
 */
function dialogo(opciones) {
  var o = opciones || {};
  var pideTexto = !!o.input;
  return new Promise(function (resolve) {
    dlgTitle.textContent = o.titulo || "Confirmar";
    dlgText.textContent = o.texto || "";
    dlgText.classList.toggle("hidden", !o.texto);
    dlgField.classList.toggle("hidden", !pideTexto);
    dlgLabel.textContent = pideTexto ? (o.input.etiqueta || "") : "";
    dlgInput.value = pideTexto ? (o.input.valor || "") : "";
    dlgInput.placeholder = pideTexto ? (o.input.placeholder || "") : "";
    dlgOk.textContent = o.aceptar || "Aceptar";
    dlgOk.className = o.peligro ? "danger" : "primary";
    dlgCancel.textContent = o.cancelar || "Cancelar";

    // Un diálogo puede abrirse desde dentro de otro (p. ej. borrar un acorde
    // con el editor abierto): se guarda el foco anterior para no perderlo.
    var previo = focoPrevio;
    var cerrar = function (valor) {
      dlgOk.onclick = null;
      dlgCancel.onclick = null;
      dlgModal.onkeydown = null;
      dlgModal.onmousedown = null;
      cerrarDialogo(dlgModal);
      focoPrevio = previo;
      resolve(valor);
    };
    dlgOk.onclick = function () {
      cerrar(pideTexto ? dlgInput.value : true);
    };
    dlgCancel.onclick = function () { cerrar(pideTexto ? null : false); };
    dlgModal.onkeydown = function (e) {
      if (e.key === "Escape") {
        // Sin frenar la propagación, el Escape seguiría hasta el manejador
        // global y cerraría también el visor que hay detrás del diálogo.
        e.preventDefault();
        e.stopPropagation();
        cerrar(pideTexto ? null : false);
      }
      else if (e.key === "Enter" && e.target === dlgInput) { e.preventDefault(); dlgOk.click(); }
    };
    // Clic en el velo = cancelar; dentro de la caja, no.
    dlgModal.onmousedown = function (e) { if (e.target === dlgModal) cerrar(pideTexto ? null : false); };

    abrirDialogo(dlgModal);
    // abrirDialogo enfoca el primer elemento; aquí interesa el campo si lo hay
    // y, si no, el botón que confirma.
    (pideTexto ? dlgInput : dlgOk).focus();
    if (pideTexto) dlgInput.select();
  });
}

/** Sí/no. peligro:true pinta el botón en rojo (borrados irreversibles). */
function confirmar(opciones) {
  var o = opciones || {};
  return dialogo({
    titulo: o.titulo || "¿Seguro?",
    texto: o.texto || "",
    aceptar: o.aceptar || "Continuar",
    cancelar: o.cancelar || "Cancelar",
    peligro: o.peligro
  });
}

/** Pide una cadena. Resuelve a null si se cancela (como el prompt de siempre). */
function pedirTexto(opciones) {
  var o = opciones || {};
  return dialogo({
    titulo: o.titulo || "",
    texto: o.texto || "",
    aceptar: o.aceptar || "Guardar",
    cancelar: o.cancelar || "Cancelar",
    input: {
      etiqueta: o.etiqueta || "",
      valor: o.valor || "",
      placeholder: o.placeholder || ""
    }
  });
}

/* ---------- tema ---------- */
/*
 * Los tokens de color ya contemplaban [data-theme=light], pero nadie ponía
 * nunca ese atributo: el modo claro existía en el CSS y era inalcanzable.
 * Aquí se fija y se recuerda; sin elección guardada manda el sistema.
 */
/* Color de la barra superior en cada tema (--ac-nav-bg del kit). Va repetido
   aquí porque el navegador pinta su barra con un meta, y un meta no entiende de
   variables CSS. */
var COLOR_BARRA = { light: "#1A535C", dark: "#0B1D21" };

function applyTheme(modo) {
  if (modo === "light" || modo === "dark") document.documentElement.setAttribute("data-theme", modo);
  else document.documentElement.removeAttribute("data-theme");
  recursosDeTema(temaEfectivo());
}

/**
 * Recursos con el color metido dentro del fichero: el favicon (y el color de la
 * barra del navegador). El resto del tema lo resuelven los tokens solos, pero
 * un SVG servido aparte no ve el data-theme de la página, así que hay que
 * cambiarle el href a mano. El mosaico del fondo no entra aquí: ese sí sale de
 * un token (--ac-pattern) porque lo pide el CSS.
 */
function recursosDeTema(modo) {
  var oscuro = modo === "dark";
  favicon.href = oscuro ? "/static/favicon-dark.svg" : "/static/favicon.svg";
  themeColor.content = oscuro ? COLOR_BARRA.dark : COLOR_BARRA.light;
  // El botón dice a dónde lleva, no dónde estás: el icono ya enseña el tema
  // actual, y "Cambiar tema" a secas no aclaraba nada a quien no lo ve.
  var destino = oscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro";
  themeBtn.title = destino;
  themeBtn.setAttribute("aria-label", destino);
}

/** El tema que se está viendo: la elección guardada o, sin ella, la del sistema. */
function temaEfectivo() {
  return currentTheme() ||
    (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
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

/* ---------- altas de cuenta ---------- */
/*
 * El interruptor vive en el servidor (tabla settings) y lo maneja el
 * administrador. Aquí solo se refleja: se esconde el «crear una cuenta» cuando
 * están cerradas, para no ofrecer un camino que va a acabar en error. El corte
 * de verdad lo hace la API en /auth/register; esto es cortesía, no seguridad.
 */
function loadSettings() {
  return api("GET", "/api/settings").then(function (d) {
    altasAbiertas = d.registrationOpen !== false;
    pintarAltas();
  }).catch(function () {
    // Sin respuesta se supone abierto: es como se comportaba antes, y el que
    // manda es el servidor cuando llegue el intento de alta.
    altasAbiertas = true;
    pintarAltas();
  });
}

/** Refleja el estado en la pantalla de entrada y en el panel de administración. */
function pintarAltas() {
  var cerrado = !altasAbiertas;
  // En la pantalla de entrada: sin registro posible, no se ofrece.
  authSwitch.classList.toggle("hidden", cerrado);
  authCerrado.classList.toggle("hidden", !cerrado);
  if (cerrado && registering) authSwitch.onclick();   // volver a «Entrar»

  // En el panel: solo lo ve (y lo toca) el administrador.
  var admin = !!user && user.role === "admin";
  altasTool.classList.toggle("hidden", !admin);
  altasBtn.setAttribute("aria-pressed", altasAbiertas ? "true" : "false");
  altasBtn.querySelector("span").textContent = altasAbiertas
    ? "Altas abiertas" : "Altas cerradas";
  altasBtn.title = altasAbiertas
    ? "Cerrar las altas: nadie podrá crear cuentas nuevas"
    : "Abrir las altas: cualquiera podrá crear una cuenta";
}

function alternarAltas() {
  var siguiente = !altasAbiertas;
  var pregunta = siguiente
    ? confirmar({
        titulo: "Abrir las altas",
        texto: "Cualquiera con la dirección de la web podrá crear una cuenta.",
        aceptar: "Abrir"
      })
    : confirmar({
        titulo: "Cerrar las altas",
        texto: "Nadie podrá crear cuentas nuevas. Quien ya tiene la suya sigue entrando.",
        aceptar: "Cerrar altas"
      });
  pregunta.then(function (sigue) {
    if (!sigue) return;
    altasBtn.disabled = true;
    return api("PUT", "/api/settings", { registrationOpen: siguiente }).then(function (d) {
      altasAbiertas = d.registrationOpen !== false;
      pintarAltas();
      altasMsg.textContent = "";
      notificar(altasAbiertas ? "Altas abiertas" : "Altas cerradas", "ok");
    }).catch(function (e) { altasMsg.textContent = e.message; })
      .then(function () { altasBtn.disabled = false; });
  });
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
  // El interruptor de altas solo lo ve el administrador, y la pantalla de
  // entrada tiene que reflejar el estado también al cerrar sesión.
  pintarAltas();
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
    sinPublicas = true;
    marcarPrivadas();
    marcarFavoritas();
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
  pedirTexto({
    titulo: "Nueva lista",
    etiqueta: "Nombre",
    placeholder: "Conciertos, Para aprender…",
    aceptar: "Crear"
  }).then(function (nombre) {
    if (!nombre || !nombre.trim()) return;
    return api("POST", "/api/playlists", { name: nombre.trim() })
      .then(loadPlaylists)
      .then(function () { notificar("Lista creada", "ok"); })
      .catch(function (e) { aviso(listEmpty, e.message); });
  });
}

function renombrarLista() {
  var lista = playlists.filter(function (p) { return p.id === playlistBy; })[0];
  if (!lista) return;
  pedirTexto({
    titulo: "Renombrar lista",
    etiqueta: "Nombre",
    valor: lista.name
  }).then(function (nombre) {
    if (!nombre || !nombre.trim()) return;
    return api("PUT", "/api/playlists/" + lista.id, { name: nombre.trim() })
      .then(loadPlaylists)
      .catch(function (e) { aviso(listEmpty, e.message); });
  });
}

function borrarLista() {
  var lista = playlists.filter(function (p) { return p.id === playlistBy; })[0];
  if (!lista) return;
  // Igual que en la app: borrar la carpeta NO borra las partituras.
  confirmar({
    titulo: "Borrar «" + lista.name + "»",
    texto: "Sus partituras no se borran: pasan a «Sin lista».",
    aceptar: "Borrar lista",
    peligro: true
  }).then(function (sigue) {
    if (!sigue) return;
    return api("DELETE", "/api/playlists/" + lista.id).then(function () {
      playlistBy = "";
      return loadPlaylists();
    }).then(refresh).catch(function (e) { aviso(listEmpty, e.message); });
  });
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
  privFilter.classList.toggle("hidden", !propio);
  // El catálogo puede ser enorme: género y orden los resuelve SQL. "Mis
  // partituras" son pocas y se ordenan aquí mismo, sin ida y vuelta.
  // Los listados vienen por páginas: sin tope, una cuenta grande se traía
  // el catálogo entero en cada visita.
  var pag = "limit=" + (busqueda ? PAGINA_BUSQUEDA : PAGINA) + "&offset=" + listOffset +
            (busqueda ? "&q=" + encodeURIComponent(busqueda) : "");
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
  // Se filtra otra vez aquí, y con la MISMA normalización que el servidor, para
  // que escribir la cuarta letra no espere a la respuesta: lo ya cargado se
  // recorta al instante y luego llega el listado completo.
  var q = vNormalizarBusqueda(search.value);
  var propio = tab === "mine";
  var shown = songs.filter(function (s) {
    if (propio && genreBy && (s.genre || "").toLowerCase() !== genreBy.toLowerCase()) return false;
    if (propio && favOnly && !s.favorite) return false;
    if (propio && sinPublicas && s.visibility === "public") return false;
    if (propio && playlistBy === "none" && s.playlistId) return false;
    if (propio && playlistBy && playlistBy !== "none" && s.playlistId !== playlistBy) return false;
    if (!q) return true;
    return vNormalizarBusqueda((s.title || "") + " " + (s.artist || "")).indexOf(q) >= 0;
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
    var etiquetas = document.createElement("div");
    etiquetas.className = "etiquetas";
    card.appendChild(etiquetas);
    function etiqueta(texto, clase) {
      var e = document.createElement("span");
      e.className = "badge" + (clase ? " " + clase : "");
      e.textContent = texto;
      etiquetas.appendChild(e);
    }
    if (s.genre) etiqueta(s.genre);
    if (propio) {
      etiqueta(s.visibility === "public" ? "Pública" : "Privada",
               s.visibility === "public" ? "publica" : "privada");
      var lista = playlists.filter(function (p) { return p.id === s.playlistId; })[0];
      if (lista) etiqueta(lista.name, "lista");
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
  // Con menos de 3 letras no se ha preguntado al servidor: si el recorte local
  // no encuentra nada, hay que decir que la búsqueda aún no ha salido de aquí,
  // y no que no existe la partitura.
  var cortaParaBuscar = q && vLetrasYCifras(search.value) < MIN_BUSQUEDA;
  // Si hay partituras y no se ve ninguna, lo que sobra es un filtro, no el
  // repertorio: decirlo así evita el «todavía no tienes partituras» que sonaba
  // a que se habían perdido.
  var filtrando = propio && songs.length > 0 &&
                  (favOnly || sinPublicas || playlistBy || genreBy);
  var msg = error ? error
    : shown.length ? ""
    : cortaParaBuscar ? "Escribe al menos " + MIN_BUSQUEDA +
        " letras o cifras para buscar en todo el catálogo."
    : q ? "Ninguna partitura coincide con «" + search.value.trim() + "»."
    // Caso propio y frecuente ahora que el filtro viene puesto: todo publicado.
    : (propio && sinPublicas && !favOnly && !playlistBy && !genreBy && songs.length)
        ? "Todas tus partituras están publicadas. Quita «Solo privadas» para verlas."
    : filtrando ? "Ninguna partitura pasa los filtros que tienes puestos."
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

  // Quien reciba el enlace de una privada se encontrará un 404: la API no
  // enseña lo privado a nadie más. Mejor decirlo al compartir que después.
  var permiso = privada
    ? confirmar({
        titulo: "Esta partitura es privada",
        texto: "Solo tú puedes abrirla, así que a otra persona el enlace le dará " +
               "un error. Para que sirva hay que publicarla: en el editor, " +
               "«Proponer publicación».",
        aceptar: "Ver el enlace igualmente"
      })
    : Promise.resolve(true);

  permiso.then(function (sigue) { if (sigue) ventanaDeEnlace(enlace); });
}

/*
 * Compartir en la web es ENSEÑAR el enlace, no adivinar qué quiere hacer el
 * navegador. Antes se llamaba a navigator.share o se copiaba en silencio: en el
 * escritorio no hay menú del sistema, y una copia sin ventana no se distingue de
 * un botón que no hace nada. Ahora sale el enlace, seleccionado y listo para
 * copiar, con un botón que además lo copia al portapapeles cuando se puede.
 */
function ventanaDeEnlace(enlace) {
  var titulo = current && current.song ? (current.song.title || "esta partitura") : "esta partitura";
  pedirTexto({
    titulo: "Compartir «" + titulo + "»",
    texto: "Este enlace abre la partitura directamente. Cópialo y mándalo a quien quieras.",
    etiqueta: "Enlace",
    valor: enlace,
    aceptar: "Copiar enlace"
  }).then(function (valor) {
    if (valor === null) return;                 // ha cerrado la ventana
    if (!navigator.clipboard || !navigator.clipboard.writeText) {
      // Sin portapapeles (sitio en http, permiso denegado, navegador viejo) el
      // enlace ya estaba a la vista y seleccionado: no hay nada que arreglar.
      notificar("Copia el enlace a mano: tu navegador no deja copiarlo solo.", "error");
      return;
    }
    navigator.clipboard.writeText(enlace)
      .then(function () { notificar("Enlace copiado", "ok"); })
      .catch(function () {
        notificar("No se ha podido copiar. El enlace estaba seleccionado: usa Ctrl+C.", "error");
      });
  });
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
  confirmar({
    titulo: "Borrar para siempre",
    texto: "«" + (s.title || "sin título") + "» se borrará definitivamente. " +
           "Esto no se puede deshacer.",
    aceptar: "Borrar para siempre",
    peligro: true
  }).then(function (sigue) {
    if (!sigue) return;
    return api("DELETE", "/api/songs/" + s.id + "?hard=1")
      .then(refresh)
      .catch(function (e) { aviso(listEmpty, e.message); });
  });
}

/* ---------- impresión ---------- */
/*
 * Imprime lo que se está viendo: con el tono transpuesto y la cejilla puestos,
 * no el texto original. Se abre una ventana con estilos propios de papel
 * (tinta oscura sobre blanco) en vez de imprimir la interfaz oscura.
 * Portado del panel /admin, que era el único sitio que sabía imprimir.
 */
/*
 * Hoja de estilo del papel: el paquete Accordio aplicado a un documento A4.
 *
 * La partitura ocupa la PÁGINA, no una tarjeta dentro de ella. Título y autor
 * van arriba y el número de hoja abajo, repetidos en todas: eso es lo que
 * distingue un documento de una captura de pantalla, y con una canción de tres
 * páginas es lo que permite saber qué se tiene en la mano.
 *
 * Las páginas se reparten AQUÍ, no se dejan al navegador (ver paginar()). Se
 * probaron antes las dos vías que suelen recomendarse y ninguna sirve en Chrome:
 * un elemento position:fixed no se repite —se pinta una vez y donde caiga— y
 * counter(page) fuera de un margen con nombre devuelve 0. Los márgenes con
 * nombre de @page (@bottom-center y compañía), que son los que resolverían esto
 * en CSS, no están implementados.
 *
 * Tres decisiones que no son evidentes:
 *
 *  - Va SIEMPRE en claro, aunque se esté leyendo en modo oscuro. Un PDF con
 *    fondo teal es un cartucho de tinta y una fotocopia ilegible.
 *  - Sin el mosaico de notas del fondo: en pantalla es identidad, pero debajo de
 *    una letra que hay que leer tocando es ruido.
 *  - Todo lo que lleva color pide print-color-adjust: al imprimir, el navegador
 *    descarta fondos y bordes salvo que se le diga expresamente.
 */
var ESTILO_IMPRESION =
  /* Sin margen de página: cada hoja se dibuja entera y pone el suyo. */
  '@page{size:A4;margin:0;}' +
  '*{box-sizing:border-box;-webkit-print-color-adjust:exact;print-color-adjust:exact;}' +
  'html,body{margin:0;padding:0;background:#F7EFE3;color:#12363D;' +
  "font-family:'Poppins',system-ui,-apple-system,Segoe UI,Roboto,sans-serif;}" +

  /* Una hoja de verdad: A4 exacto, con sus márgenes por dentro. */
  '.hoja{width:210mm;height:297mm;padding:16mm 15mm 12mm;background:#F7EFE3;' +
  'display:flex;flex-direction:column;overflow:hidden;' +
  'break-after:page;page-break-after:always;}' +
  '.hoja:last-child{break-after:auto;page-break-after:auto;}' +
  /* En pantalla (antes de imprimir) se ven como folios sobre la mesa. */
  '@media screen{body{background:#DCE8E5;padding:16px 0;}' +
  '.hoja{margin:0 auto 16px;box-shadow:0 2px 12px rgba(18,54,61,.18);}}' +

  /* ---- cabecera ---- */
  '.cab{flex:0 0 auto;display:flex;align-items:center;gap:10px;' +
  'border-bottom:1.5px solid #1A535C;padding-bottom:5px;margin-bottom:7mm;}' +
  /* La marca manda el alto de la cabecera; el resto se alinea con ella. */
  '.cab .logo{flex:0 0 auto;width:30px;height:21px;display:block;}' +
  ".cab .tit{font-family:'Montserrat',system-ui,sans-serif;font-weight:700;" +
  'letter-spacing:-.02em;font-size:15px;color:#12363D;' +
  'white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}' +
  '.cab .aut{font-size:12px;color:#3F5257;white-space:nowrap;}' +
  '.cab .est{margin-left:auto;display:flex;gap:6px;flex:0 0 auto;}' +
  '.cab .etq{border-radius:999px;padding:2px 9px;font-size:9.5px;font-weight:600;' +
  'letter-spacing:.1em;text-transform:uppercase;background:#FFE66D;color:#6B550A;}' +
  '.cab .etq.tono{background:#E3EEF0;color:#113941;}' +

  /* ---- la partitura, a página completa ---- */
  ".cuerpo{flex:1 1 auto;min-height:0;font-family:'JetBrains Mono',ui-monospace," +
  'SFMono-Regular,Menlo,monospace;font-size:12px;line-height:1.5;color:#12363D;}' +
  '.ln{white-space:pre;margin:0;}' +
  '.tab{white-space:pre;color:#3F5257;}' +
  '.chord{color:#C93B3C;font-weight:600;}' +
  '.fuente{margin-top:6mm;font-size:9px;color:#7B8E92;' +
  'border-top:1px solid #DCE8E5;padding-top:4px;}' +

  /* ---- pie ---- */
  '.pie{flex:0 0 auto;display:flex;align-items:baseline;' +
  'border-top:1px solid #DCE8E5;padding-top:5px;margin-top:6mm;' +
  'font-size:9.5px;color:#7B8E92;letter-spacing:.06em;}' +
  '.pie .der{margin-left:auto;}';

/*
 * Reparte las líneas en hojas A4. Se ejecuta DENTRO de la ventana de impresión,
 * con las fuentes ya cargadas: sin ellas las medidas son las de la tipografía de
 * reserva y el corte cae donde no toca.
 *
 * El método es el simple y el que no falla: se van metiendo líneas en la hoja
 * mientras quepan y, cuando una se sale, empieza otra. Medir es más lento que
 * calcular, pero calcular a ojo se rompe con la primera canción que traiga una
 * tablatura o un bloque de otro alto.
 */
var GUION_PAGINAR = [
  "function paginar() {",
  "  var molde = document.getElementById('molde');",
  "  var lineas = Array.prototype.slice.call(molde.children);",
  "  var cab = document.getElementById('cabPlantilla').innerHTML;",
  "  var pie = document.getElementById('piePlantilla').innerHTML;",
  "  var destino = document.getElementById('hojas');",
  "  molde.remove();",
  "",
  "  function nuevaHoja() {",
  "    var hoja = document.createElement('section');",
  "    hoja.className = 'hoja';",
  "    var c = document.createElement('header');",
  "    c.className = 'cab';",
  "    c.innerHTML = cab;",
  "    var cuerpo = document.createElement('div');",
  "    cuerpo.className = 'cuerpo';",
  "    var p = document.createElement('footer');",
  "    p.className = 'pie';",
  "    p.innerHTML = pie;",
  "    hoja.appendChild(c);",
  "    hoja.appendChild(cuerpo);",
  "    hoja.appendChild(p);",
  "    destino.appendChild(hoja);",
  "    return cuerpo;",
  "  }",
  "",
  "  // Una linea de acordes y la letra que va debajo son un bloque: separarlas",
  "  // en dos hojas deja los acordes sin cancion y la cancion sin acordes.",
  "  function soloAcordes(n) {",
  "    if (!n.querySelector || !n.querySelector('.chord')) return false;",
  "    var resto = n.textContent;",
  "    var acordes = n.querySelectorAll('.chord');",
  "    for (var k = 0; k < acordes.length; k++) {",
  "      resto = resto.replace(acordes[k].textContent, '');",
  "    }",
  "    return resto.trim() === '';",
  "  }",
  "",
  "  var bloques = [];",
  "  for (var b = 0; b < lineas.length; b++) {",
  "    var grupo = [lineas[b]];",
  "    if (soloAcordes(lineas[b]) && lineas[b + 1] && !soloAcordes(lineas[b + 1])) {",
  "      grupo.push(lineas[++b]);",
  "    }",
  "    bloques.push(grupo);",
  "  }",
  "",
  "  var cuerpo = nuevaHoja();",
  "  for (var i = 0; i < bloques.length; i++) {",
  "    for (var g = 0; g < bloques[i].length; g++) cuerpo.appendChild(bloques[i][g]);",
  "    // Se ha salido de la hoja: este bloque abre la siguiente.",
  "    if (cuerpo.scrollHeight > cuerpo.clientHeight &&",
  "        cuerpo.children.length > bloques[i].length) {",
  "      cuerpo = nuevaHoja();",
  "      for (var g2 = 0; g2 < bloques[i].length; g2++) cuerpo.appendChild(bloques[i][g2]);",
  "    }",
  "  }",
  "",
  "  // Numerar al final, que es cuando se sabe cuantas hay.",
  "  var hojas = destino.querySelectorAll('.hoja');",
  "  for (var j = 0; j < hojas.length; j++) {",
  "    hojas[j].querySelector('.num').textContent =",
  "      'P\\u00e1gina ' + (j + 1) + ' de ' + hojas.length;",
  "  }",
  "  return hojas.length;",
  "}"
].join("\\n");

/*
 * Marca del mástil para el papel: la versión de fondo claro del kit
 * (logo/mark-fretboard.svg), que es la que lleva el trazo en teal. Va en la
 * cabecera de cada hoja, así que se escribe una vez y se copia al paginar.
 */
var MARCA_PAPEL =
  '<svg class="logo" viewBox="0 0 132 92" aria-hidden="true">' +
  '<g fill="none" stroke="#1A535C" stroke-width="5" stroke-linecap="square">' +
  '<path d="M8 10H124M8 26H124M8 42H124M8 58H124M8 74H124"></path>' +
  '<path d="M8 10V74M37 10V74M66 10V74M95 10V74M124 10V74"></path></g>' +
  '<g><circle cx="66" cy="10" r="8.5" fill="#1A535C"></circle>' +
  '<circle cx="37" cy="26" r="8.5" fill="#1A535C"></circle>' +
  '<circle cx="95" cy="26" r="8.5" fill="#FF6B6B"></circle>' +
  '<circle cx="8" cy="42" r="8.5" fill="#1A535C"></circle>' +
  '<circle cx="80" cy="42" r="8.5" fill="#FF6B6B"></circle>' +
  '<circle cx="80" cy="58" r="8.5" fill="#FF6B6B"></circle>' +
  '<circle cx="51" cy="74" r="8.5" fill="#FF6B6B"></circle></g></svg>';

function printViewer() {
  if (!current) return;
  var w = window.open("", "_blank");
  if (!w) {
    notificar("El navegador ha bloqueado la ventana de impresión. Permite las " +
              "ventanas emergentes de este sitio y vuelve a intentarlo.", "error");
    return;
  }
  // current.body ya es lo que se está leyendo: el Original o la versión
  // elegida, y capoActual es el capo de eso mismo (ver setCapo).
  var lineas = vRenderSong(vTransposeBody(current.body || "", semis, flats));
  var capo = capoActual;
  var titulo = current.song.title || "Partitura";
  var autor = current.song.artist || "";
  var fuente = vUrlSegura(current.song.sourceUrl) ? current.song.sourceUrl : "";

  var estados =
    (capo > 0 ? '<span class="etq">Capo ' + capo + '</span>' : '') +
    (semis !== 0 ? '<span class="etq tono">Tono ' + (semis > 0 ? '+' : '') + semis + '</span>' : '');

  var doc = '<!doctype html><html lang="es"><head><meta charset="utf-8">' +
    '<title>' + vEsc(titulo) + (autor ? ' · ' + vEsc(autor) : '') + '</title>' +
    '<link rel="preconnect" href="https://fonts.googleapis.com">' +
    '<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>' +
    '<link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@700' +
    '&family=Poppins:wght@400;500;600&family=JetBrains+Mono:wght@400;600&display=swap"' +
    ' rel="stylesheet">' +
    '<style>' + ESTILO_IMPRESION + '</style></head><body>' +
    // Plantillas de cabecera y pie: se copian en cada hoja al paginar.
    '<template id="cabPlantilla">' + MARCA_PAPEL +
      '<span class="tit">' + vEsc(titulo) + '</span>' +
      (autor ? '<span class="aut">' + vEsc(autor) + '</span>' : '') +
      (estados ? '<span class="est">' + estados + '</span>' : '') +
    '</template>' +
    '<template id="piePlantilla"><span>accordio.site</span>' +
      '<span class="der num"></span></template>' +
    '<div id="hojas"></div>' +
    // Molde: todo el contenido junto, del que se van sacando las líneas.
    '<div id="molde" style="position:absolute;visibility:hidden">' + lineas +
      (fuente ? '<div class="fuente">Fuente: ' + vEsc(fuente) + '</div>' : '') +
    '</div>' +
    '<script>' + GUION_PAGINAR + '<\/script>' +
    '</body></html>';

  w.document.write(doc);
  w.document.close();
  w.focus();

  /*
   * Primero las fuentes, luego paginar, y solo entonces imprimir. Si se midiera
   * antes de que carguen, el reparto saldría con la tipografía de reserva: las
   * columnas de la monoespaciada bailan y el corte cae donde no toca. El plazo
   * es un seguro por si las fuentes no llegan (sin red, bloqueadas): mejor
   * imprimir con la de reserva que no imprimir.
   */
  var hecho = false;
  var rematar = function () {
    if (hecho) return;
    hecho = true;
    try { w.paginar(); } catch (e) { /* si falla, al menos sale el contenido */ }
    w.focus();
    w.print();
  };
  if (w.document.fonts && w.document.fonts.ready) {
    w.document.fonts.ready.then(rematar).catch(rematar);
  }
  setTimeout(rematar, 2500);
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
      a.download = "accordio-" + d2.getFullYear() + p2(d2.getMonth() + 1) + p2(d2.getDate()) +
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
      return confirmar({
        titulo: "Restaurar copia",
        texto: "Se crearán " + textos.length + " partituras a partir del ZIP.",
        aceptar: "Restaurar"
      }).then(function (sigue) {
        if (!sigue) { aviso(adminMsg, ""); return; }
        return importarTextos(textos);
      });
    })
    .catch(function (e) { aviso(adminMsg, "No se ha podido leer el ZIP: " + e.message); });
}

/** Sube una a una las partituras del ZIP informando del avance. */
function importarTextos(textos) {
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
    notificar("Restauradas " + ok + " partituras" + (fallos ? " (" + fallos + " con error)" : ""),
              fallos ? "error" : "ok");
    refresh();
  });
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
    // El ajuste se hace con el visor ya visible: en display:none, vBody no tiene
    // ancho y la cuenta saldría con el de la ventana entera.
    fontSize = letraQueQuepa();
    vBody.style.setProperty("--fs", fontSize + "px");
    renderTube();
    colocarVersiones();
    colocarAcciones();
    menuAcciones(false);
    mandos(false);
    loadVersions(d.song.id);
    loadRatings(d.song.id);
    loadComments(d.song.id);
    loadRelated(d.song.id);
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
      avisarError(e);
    }
  });
}

/** Capo de lo que se está mirando; con 0 no se enseña nada. */
/*
 * Capo de lo que se está mirando. Se guarda en una variable ADEMÁS de pintarlo:
 * quien lo necesite (la impresión) lee el dato, no el rótulo.
 *
 * Leerlo del rótulo era además una trampa: la expresión que lo extraía vivía
 * dentro del literal de plantilla que sirve este fichero, y ahí "\D" no es un
 * escape válido, así que al navegador le llegaba /D+/ —quitar las letras D— en
 * vez de /\D+/ —quitar lo que no sea dígito—. Resultado: "Capo 3" no se
 * convertía en 3 sino en NaN, y el PDF salía SIEMPRE sin capo.
 */
var capoActual = 0;

function setCapo(capo) {
  var n = Number(capo) || 0;
  capoActual = n;
  vCapo.innerHTML = n > 0 ? 'Capo <span class="n">' + n + '</span>' : "";
  vCapo.classList.toggle("hidden", n <= 0);
}

/*
 * Tamaño de letra que hace que la partitura QUEPA de ancho.
 *
 * La hoja mide 68 caracteres de la monoespaciada, que a 18 px son unos 730:
 * en un móvil de 390 eso obliga a arrastrar de lado para leer cada línea, que
 * es justo lo que no se puede hacer mientras se toca. Aquí se mide la línea más
 * larga de ESTA partitura (no las 68 columnas teóricas: casi ninguna canción
 * las gasta) y se elige el mayor tamaño que entra, entre 11 y 18 px.
 *
 * Solo se hace al abrir y solo en estrecho. A partir de ahí manda A-/A+: si
 * alguien decide leer más grande y arrastrar, es cosa suya.
 */
/*
 * Ancho de un carácter de la monoespaciada, en fracción del tamaño de letra.
 * Se MIDE en vez de darlo por supuesto: JetBrains Mono avanza 0,6 em, pero si no
 * ha cargado todavía manda la de reserva del sistema, que no tiene por qué. Con
 * el número estimado el cálculo se quedaba corto por unos píxeles y la línea se
 * salía igualmente por la derecha, que era justo lo que se quería evitar.
 * Se mide una vez y se guarda: es una lectura de layout, y no cambia.
 */
var anchoChar = 0;

/* Tamaño mínimo del ajuste automático: por debajo no se lee de un vistazo. */
var SUELO_LETRA = 13;

function anchoDeCaracter() {
  if (anchoChar) return anchoChar;
  var regla = document.createElement("span");
  regla.textContent = "0123456789";
  regla.style.cssText = "position:absolute;visibility:hidden;white-space:pre;" +
                        "font-family:var(--vv-font-mono);font-size:100px";
  document.body.appendChild(regla);
  anchoChar = regla.getBoundingClientRect().width / 1000;   // 10 caracteres a 100 px
  regla.remove();
  return anchoChar || 0.6;
}

function letraQueQuepa() {
  if (!current || pantallaAncha.matches) return fontSize;
  var lineas = String(current.body || "").split("\\n");
  var largo = 0;
  for (var i = 0; i < lineas.length; i++) {
    // Las llaves de los acordes no se pintan: {Am} ocupa dos columnas, no cuatro.
    var limpia = lineas[i].replace(/[{}]/g, "");
    if (limpia.length > largo) largo = limpia.length;
  }
  if (!largo) return fontSize;
  var hoja = vSheet.querySelector(".sheet");
  var hueco = (hoja ? hoja.clientWidth : vBody.clientWidth || window.innerWidth);
  var cabe = Math.floor(hueco / (largo * anchoDeCaracter()));
  /*
   * El suelo del ajuste automático son 13 px, y es un suelo de LEGIBILIDAD, no
   * de encaje: por debajo la letra no se lee de un vistazo con la guitarra en
   * las manos, así que una canción de columnas exageradas se queda a 13 y se
   * arrastra de lado. Encoger hasta que "quepa" cueste lo que cueste daba
   * partituras que entraban enteras y no se leían, que es peor negocio.
   *
   * Antes eran 10, y además chocaba con el mínimo de A- (11): el ajuste dejaba
   * la letra ya por debajo del mínimo manual y el botón de reducir no hacía
   * nada. Ahora A- baja hasta 8 para quien quiera apretar de verdad.
   */
  /*
   * Suelo blando: si la canción entra justo un punto por debajo (12 px), se le
   * concede. Plantarse en 13 a rajatabla dejaba hojas que se salían por cinco
   * píxeles —arrastrar la línea entera para leer una sílaba— cuando bajando uno
   * entraban enteras. Dos puntos por debajo ya no: ahí se prefiere leer y
   * arrastrar.
   */
  var suelo = cabe === SUELO_LETRA - 1 ? cabe : SUELO_LETRA;
  return Math.max(suelo, Math.min(18, cabe));
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
  vPlayIcon.setAttribute("href", "#ic-pausa");
  vPlay.setAttribute("aria-pressed", "true");
  if (navigator.wakeLock) {
    navigator.wakeLock.request("screen").then(function (w) { wakeLock = w; }).catch(function () {});
  }
  raf = requestAnimationFrame(step);
}
function stopScroll() {
  scrolling = false;
  vPlayIcon.setAttribute("href", "#ac-play-circle");
  vPlay.setAttribute("aria-pressed", "false");
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
    vMetroIcon.setAttribute("href", "#ac-metronome");
    vMetro.setAttribute("aria-pressed", "false");
  } else {
    metro.bpm = +vBpm.value;
    metro.start();
    vMetroIcon.setAttribute("href", "#ic-stop");
    vMetro.setAttribute("aria-pressed", "true");
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
  }).catch(avisarError);
}

/* ---------- comentarios ---------- */

/*
 * Recomendadas: lo decide el servidor (mismo artista y, si no hay, mismo
 * estilo) y aquí solo se pinta. Es una carga aparte y a propósito: si la
 * consulta falla o tarda, la partitura ya está en pantalla y se lee igual.
 */
function loadRelated(songId) {
  vRelated.innerHTML = "";
  api("GET", "/api/songs/" + songId + "/related").then(function (d) {
    renderRelated(d.songs || [], d.reason || "");
  }).catch(function () {
    // Sin recomendaciones no pasa nada: la sección simplemente no aparece. No
    // se avisa del error porque no es algo que el lector haya pedido.
    vRelated.innerHTML = "";
  });
}

function renderRelated(lista, motivo) {
  vRelated.innerHTML = "";
  if (!lista.length || !current) return;

  var titulo = document.createElement("h4");
  titulo.textContent = motivo === "artist"
    ? "Más de " + (current.song.artist || "este artista")
    : motivo === "genre"
    ? "Más " + (current.song.genre || "de este estilo").toLowerCase()
    : "También te puede servir";
  vRelated.appendChild(titulo);

  var rejilla = document.createElement("div");
  rejilla.className = "relGrid";
  lista.forEach(function (s) {
    var card = document.createElement("button");
    card.className = "card";
    var a = document.createElement("div");
    a.className = "a";
    a.textContent = s.artist || (s.ownerName ? "de " + s.ownerName : "");
    var t = document.createElement("div");
    t.className = "t";
    t.textContent = s.title || "(sin título)";
    card.appendChild(a);
    card.appendChild(t);
    if (s.genre && motivo === "artist") {
      var etiquetas = document.createElement("div");
      etiquetas.className = "etiquetas";
      var g = document.createElement("span");
      g.className = "badge";
      g.textContent = s.genre;
      etiquetas.appendChild(g);
      card.appendChild(etiquetas);
    }
    // Se abre EN EL MISMO visor: cambia la partitura, la URL y las propias
    // recomendaciones, sin pasar por el catálogo.
    card.onclick = function () { openSong(s.id); };
    rejilla.appendChild(card);
  });
  vRelated.appendChild(rejilla);
}

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
      .catch(avisarError)
      .then(function () { enviar.disabled = false; });
  };
  fila.appendChild(enviar);
  form.appendChild(area);
  form.appendChild(fila);
  vComments.appendChild(form);
}

function deleteComment(id) {
  confirmar({
    titulo: "Borrar comentario",
    texto: "Se quitará de la partitura y no se puede recuperar.",
    aceptar: "Borrar",
    peligro: true
  }).then(function (sigue) {
    if (!sigue) return;
    return api("DELETE", "/api/comments/" + id)
      .then(function () { loadComments(current.song.id); })
      .catch(avisarError);
  });
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
  }).catch(avisarError);
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
  }).catch(avisarError);
}

function deleteVersion(id) {
  confirmar({
    titulo: "Eliminar versión",
    texto: "La partitura original no se toca; solo se borra esta versión.",
    aceptar: "Eliminar",
    peligro: true
  }).then(function (sigue) {
    if (!sigue) return;
    return api("DELETE", "/api/versions/" + id).then(function () {
      currentVersion = null;
      showVersion(null);
      loadVersions(current.song.id);
    }).catch(avisarError);
  });
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
        confirmar({
          titulo: "Retirar propuesta",
          texto: "Dejará de estar en la cola de revisión.",
          aceptar: "Retirar"
        }).then(function (sigue) {
          if (!sigue) return;
          return api("DELETE", "/api/proposals/" + p.id)
            .then(loadProposals)
            .catch(function (e) { propMsg.textContent = e.message; });
        });
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
  var pregunta = accion === "reject"
    ? pedirTexto({
        titulo: "Rechazar propuesta",
        texto: "El motivo le llega a quien la propuso.",
        etiqueta: "Motivo",
        placeholder: "Qué habría que cambiar",
        aceptar: "Rechazar"
      })
    : confirmar(p.kind === "publish"
        ? {
            titulo: "Publicar en el catálogo",
            texto: "«" + (p.songTitle || "") + "» pasará a verse desde cualquier cuenta.",
            aceptar: "Publicar"
          }
        : { titulo: "Añadir versión", texto: "Se añadirá a la partitura.", aceptar: "Añadir" });

  pregunta.then(function (respuesta) {
    // Rechazar devuelve el texto (null = cancelado); aprobar, un booleano.
    if (respuesta === null || respuesta === false) return;
    var nota = typeof respuesta === "string" ? respuesta : "";
    return api("POST", "/api/proposals/" + p.id + "/" + accion, { note: nota }).then(function () {
      loadProposals();
      loadGenres();
    }).catch(function (e) { propMsg.textContent = e.message; });
  });
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
  // El botón conserva su icono: solo cambia la palabra y el estado, que es lo
  // que lee tanto el ojo (relleno coral) como el lector de pantalla.
  vChords.querySelector("span").textContent = chordBarOn ? "Ocultar acordes" : "Acordes";
  vChords.setAttribute("aria-pressed", chordBarOn ? "true" : "false");
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
  var nombre = editingChord;
  confirmar({
    titulo: "Quitar " + nombre,
    texto: "Sale del diccionario global: deja de verse para todo el mundo.",
    aceptar: "Quitar",
    peligro: true
  }).then(function (sigue) {
    if (!sigue) return;
    var siguiente = {};
    Object.keys(chordDict).forEach(function (k) { if (k !== nombre) siguiente[k] = chordDict[k]; });
    return saveChordDict(siguiente, "Eliminado " + nombre).then(function () {
      cerrarDialogo(chordEditor);
    }).catch(function (e) { chMsg.textContent = e.message; });
  });
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
  // El aviso va ANTES de tocar el estado del editor: si se cancela, editingId
  // no debe quedarse apuntando a una partitura que no se va a editar.
  var permiso = current.song.locked
    ? confirmar({
        titulo: "Partitura bloqueada",
        texto: "Está bloqueada para evitar cambios accidentales.",
        aceptar: "Editarla igualmente"
      })
    : Promise.resolve(true);
  permiso.then(function (sigue) { if (sigue) abrirEditorDeCancion(); });
}

/** Vuelca la partitura del visor en el editor. Sale de editCurrent. */
function abrirEditorDeCancion() {
  editorMode = "song";
  editingVersionId = null;
  editingId = current.song.id;
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
  var id = editingId;
  pedirTexto({
    titulo: "Proponer publicación",
    texto: "La revisará alguien del equipo antes de que entre en el catálogo.",
    etiqueta: "Nota para quien revise (opcional)",
    placeholder: "De dónde sale, qué has corregido…",
    aceptar: "Enviar propuesta"
  }).then(function (nota) {
    if (nota === null) return;
    return api("POST", "/api/songs/" + id + "/proposals", { kind: "publish", note: nota })
      .then(function () {
        showEdit(false);
        tab = "proposals";
        listOffset = 0;
        refresh();
        notificar("Propuesta enviada", "ok");
      }).catch(function (e) { editMsg.textContent = e.message; });
  });
}

function deleteSong() {
  if (!editingId) return;
  var id = editingId;
  confirmar({
    titulo: "Mover a la papelera",
    texto: "Se puede recuperar después desde la papelera.",
    aceptar: "Mover"
  }).then(function (sigue) {
    if (!sigue) return;
    return api("DELETE", "/api/songs/" + id).then(function () {
      showEdit(false);
      loadGenres();
      refresh();
      notificar("Partitura en la papelera", "ok");
    }).catch(function (e) { editMsg.textContent = e.message; });
  });
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

loginBtn.onclick = function () { registering = false; authTitle.textContent = "Entrar en Accordio";
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
  authTitle.textContent = registering ? "Crear cuenta en Accordio" : "Entrar en Accordio";
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
altasBtn.onclick = alternarAltas;
moreBtn.onclick = function () { listOffset += PAGINA; refresh(); };
/*
 * Al teclear: recorte inmediato de lo que ya hay y, si hay al menos 3 letras o
 * cifras, consulta al servidor un poco después. El retardo es para no lanzar
 * una consulta por tecla; con menos de 3 no se consulta, pero el recorte local
 * sigue funcionando.
 */
search.oninput = function () {
  renderList();
  var texto = search.value.trim();
  var quiereBuscar = vLetrasYCifras(texto) >= MIN_BUSQUEDA ? texto : "";
  if (quiereBuscar === busqueda) return;      // nada que cambiar en el servidor
  clearTimeout(temporizadorBusqueda);
  temporizadorBusqueda = setTimeout(function () {
    busqueda = quiereBuscar;
    listOffset = 0;
    refresh();
  }, 250);
};
tabChords.onclick = function () { irA("chords"); };

themeBtn.onclick = toggleTheme;

playlistFilter.onchange = function () {
  playlistBy = playlistFilter.value;
  renderPlaylistControls();
  renderList();
};
favFilter.onclick = function () {
  favOnly = !favOnly;
  marcarFavoritas();
  renderList();
};

privFilter.onclick = function () {
  sinPublicas = !sinPublicas;
  marcarPrivadas();
  renderList();
};

/** Estado del filtro, y qué pasa al pulsarlo. */
function marcarPrivadas() {
  privFilter.setAttribute("aria-pressed", sinPublicas ? "true" : "false");
  privFilter.title = sinPublicas
    ? "Ver también las que ya están publicadas"
    : "Ocultar las que ya están publicadas";
}

/** Estrella hueca o rellena, según el filtro esté puesto o no. */
function marcarFavoritas() {
  favFilter.setAttribute("aria-pressed", favOnly ? "true" : "false");
  favFilter.querySelector("use")
    .setAttribute("href", favOnly ? "#ac-star" : "#ac-star-outline");
}
/*
 * Panel de mandos del móvil. En pantalla ancha los mandos viven en su columna y
 * nada de esto se usa; en estrecho suben desde abajo, con velo, y se cierran
 * tocando fuera, con Escape o con el asa. El botón que los abre está anclado
 * abajo a la derecha, que es donde llega el pulgar.
 */
function mandos(abrir) {
  viewer.classList.toggle("conMandos", abrir);
  vTools.setAttribute("aria-expanded", abrir ? "true" : "false");
  vVelo.hidden = !abrir;
  if (abrir) vSide.scrollTop = 0;
}

vTools.onclick = function () { mandos(!viewer.classList.contains("conMandos")); };
vVelo.onclick = function () { mandos(false); };
vToolsClose.onclick = function () { mandos(false); };
document.addEventListener("keydown", function (e) {
  if (e.key === "Escape" && viewer.classList.contains("conMandos")) {
    e.stopPropagation();          // que no se lleve por delante el visor entero
    mandos(false);
  }
});

/*
 * Las versiones cambian de sitio según el ancho: en la columna de mandos cuando
 * hay sitio, y al final de la partitura en el móvil, que es donde se decide
 * «pruebo esta otra». Se MUEVE el mismo nodo en vez de duplicarlo: con dos
 * copias habría dos listas que mantener y dos veces los mismos ids.
 */
var pantallaAncha = window.matchMedia("(min-width: 901px)");

/*
 * Acciones de la partitura (original, compartir, PDF, editar). En pantalla ancha
 * van en la barra; en el móvil esa barra se comía un tercio de la pantalla, así
 * que se mudan a un menú que cuelga del botón «Acciones». Se mueven los MISMOS
 * nodos, como con las versiones: duplicarlos obligaría a repetir cada onclick y
 * cada .hidden (el de «Editar» depende de quién seas).
 */
var accionesDelVisor = [];

function colocarAcciones() {
  if (!accionesDelVisor.length) {
    accionesDelVisor = [vSource, vShare, vPrint, vEdit];
  }
  var destino = pantallaAncha.matches ? vLadoDer : vMenu;
  accionesDelVisor.forEach(function (n) {
    if (n.parentElement !== destino) destino.appendChild(n);
  });
  if (pantallaAncha.matches) menuAcciones(false);
}

function menuAcciones(abrir) {
  vMenu.hidden = !abrir;
  vMenuBtn.setAttribute("aria-expanded", abrir ? "true" : "false");
}

function colocarVersiones() {
  if (pantallaAncha.matches) {
    if (vVersionPanel.parentElement !== vSide) vSide.appendChild(vVersionPanel);
  } else if (vVersionPanel.parentElement !== vBody) {
    vBody.insertBefore(vVersionPanel, vComments);
  }
}
pantallaAncha.addEventListener("change", function () {
  colocarVersiones();
  colocarAcciones();
  if (pantallaAncha.matches) mandos(false);   // en ancho no hay panel que cerrar
});
colocarVersiones();
colocarAcciones();

vMenuBtn.onclick = function (e) {
  e.stopPropagation();
  menuAcciones(vMenu.hidden);
};
// Tocar en cualquier otro sitio lo cierra, y también al elegir una acción.
document.addEventListener("click", function (e) {
  if (!vMenu.hidden && !vMenu.contains(e.target)) menuAcciones(false);
});
vMenu.addEventListener("click", function () { menuAcciones(false); });
document.addEventListener("keydown", function (e) {
  if (e.key === "Escape" && !vMenu.hidden) { e.stopPropagation(); menuAcciones(false); }
});

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
/*
 * Paso de 1 px y no de 2: cerca del suelo, dos píxeles son un salto enorme
 * (de 14 a 12 la hoja cambia de aspecto entero). El mínimo es 8 para que
 * siempre quede recorrido por debajo del tamaño que elige el ajuste automático.
 */
vFontUp.onclick = function () { fontSize = Math.min(40, fontSize + 1); renderViewer(); };
vFontDown.onclick = function () { fontSize = Math.max(8, fontSize - 1); renderViewer(); };
vMetro.onclick = toggleMetro;
vBpm.oninput = function () { vBpmVal.textContent = vBpm.value; metro.bpm = +vBpm.value; };
document.addEventListener("keydown", function (e) {
  if (e.key === "Escape" && viewer.classList.contains("on")) closeViewer();
});

applyTheme(currentTheme());
window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", function () {
  if (!currentTheme()) recursosDeTema(temaEfectivo());
});
renderBeats(0);
loadGenres();
loadSettings();
restoreSession().then(function () {
  // La portada es el catálogo, se tenga sesión o no: al entrar interesa ver lo
  // que hay publicado, no la carpeta propia (que está a un clic, en su pestaña).
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
<title>Accordio</title>
<!-- Favicon y color de la barra del navegador los pone el JS del tema: con
     media queries seguirían al sistema y no a la elección manual. -->
<link id="favicon" rel="icon" href="/static/favicon.svg" type="image/svg+xml">
<meta id="themeColor" name="theme-color" content="#1A535C">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<!-- Montserrat (titulares) y Poppins (texto) son las del kit. JetBrains Mono no
     sale de él: la hoja de partitura necesita monoespaciada o los acordes dejan
     de caer sobre su sílaba. -->
<link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@600;700;800&family=Poppins:wght@400;500;600&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
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
  <a class="brand" href="/" aria-label="Accordio">
    <svg width="42" height="29" viewBox="0 0 132 92" aria-hidden="true">
      <g fill="none" stroke="currentColor" stroke-width="6" stroke-linecap="square">
        <path d="M8 10H124M8 26H124M8 42H124M8 58H124M8 74H124"></path>
        <path d="M8 10V74M37 10V74M66 10V74M95 10V74M124 10V74"></path>
      </g>
      <g>
        <circle cx="66" cy="10" r="9" fill="currentColor"></circle>
        <circle cx="37" cy="26" r="9" fill="currentColor"></circle>
        <circle cx="95" cy="26" r="9" fill="#FF6B6B"></circle>
        <circle cx="8" cy="42" r="9" fill="currentColor"></circle>
        <circle cx="80" cy="42" r="9" fill="#FF6B6B"></circle>
        <circle cx="80" cy="58" r="9" fill="#FF6B6B"></circle>
        <circle cx="51" cy="74" r="9" fill="#FF6B6B"></circle>
      </g>
    </svg>
    <span>
      <h1>Accordio</h1>
      <span class="kicker">Acordes de la comunidad</span>
    </span>
  </a>
  <span class="grow"></span>
  <span id="who" class="hidden" style="font-size:13px;color:var(--vv-text-muted)"></span>
  <button id="themeBtn" class="iconBtn" title="Cambiar tema" aria-label="Cambiar tema">
    <svg class="iconoTema" viewBox="0 0 24 24" aria-hidden="true" fill="none"
         stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
      <g class="sol">
        <circle cx="12" cy="12" r="4.1"></circle>
        <path d="M12 2.6v2.1M12 19.3v2.1M4.4 4.4l1.5 1.5M18.1 18.1l1.5 1.5M2.6 12h2.1M19.3 12h2.1M4.4 19.6l1.5-1.5M18.1 5.9l1.5-1.5"></path>
      </g>
      <path class="luna" d="M20.1 14.6A8.5 8.5 0 0 1 9.4 3.9a8.5 8.5 0 1 0 10.7 10.7z"></path>
    </svg>
  </button>
  <button id="loginBtn">Entrar</button>
  <button id="logoutBtn" class="hidden">Salir</button>
</header>

<main>
  <!-- acceso -->
  <section id="authView" class="hidden">
    <div class="stack">
      <h2 id="authTitle" style="margin:0">Entrar en Accordio</h2>
      <label>Email<input type="email" id="email" autocomplete="email"></label>
      <label id="nameWrap" class="hidden">Nombre<input type="text" id="name" autocomplete="name"></label>
      <label>Contraseña <small>(mínimo 8 caracteres)</small>
        <input type="password" id="password" minlength="8" autocomplete="current-password"></label>
      <div class="msg" id="authMsg"></div>
      <button class="primary" id="authSubmit">Entrar</button>
      <button class="ghost" id="authSwitch">Crear una cuenta</button>
      <p id="authCerrado" class="hidden aviso" style="margin:0">
        Las altas de cuenta están cerradas ahora mismo. Si ya tienes una, puedes entrar.</p>
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
      <button id="newBtn" class="tool destacada hidden">
        <svg class="ic"><use href="#ac-plus"></use></svg><span>Nueva</span></button>
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
      <button id="favFilter" class="tool favoritas hidden" aria-pressed="false" title="Ver solo las favoritas">
        <svg class="ic"><use href="#ac-star-outline"></use></svg><span>Favoritas</span></button>
      <button id="privFilter" class="tool privadas hidden" aria-pressed="true" title="Ver también las que ya están publicadas">
        <svg class="ic"><use href="#ic-candado"></use></svg><span>Solo privadas</span></button>
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
    <!-- Solo para el administrador: se enseña desde el JS, porque a la pestaña
         de Administración también llegan los editores. -->
    <div class="adminTool hidden" id="altasTool">
      <h3>Altas de cuenta</h3>
      <p>Con las altas cerradas, nadie puede crear una cuenta nueva: ni desde la
         web ni llamando a la API. Quien ya la tiene sigue entrando con
         normalidad.</p>
      <div class="acciones">
        <button id="altasBtn" class="tool altas" aria-pressed="false">
          <svg class="ic"><use href="#ac-user"></use></svg><span>Altas: …</span></button>
      </div>
      <div class="msg" id="altasMsg"></div>
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
    <!--
      Tres columnas: escribir, ver cómo queda y los ajustes. Antes los campos
      iban apilados ARRIBA y empujaban el editor y la vista previa fuera de la
      pantalla, cuando escribir la partitura es el 90 % del trabajo y el título
      o la categoría se tocan una vez. Ahora la hoja manda y los ajustes están
      al lado, en su columna, junto con los botones.
    -->
    <div class="editor">
      <div id="editSplit">
        <div class="pane">
          <div class="hd">Partitura <small>acordes entre llaves: {Am}</small>
            <span class="grow"></span>
            <button id="eDetect" class="tool" title="Marca las líneas que solo llevan acordes">
              <svg class="ic"><use href="#ac-notes"></use></svg><span>Detectar acordes</span></button>
          </div>
          <textarea id="eContent" spellcheck="false"
                    placeholder="#title: Título&#10;#artist: Autor&#10;---&#10;{Am} Primera línea"></textarea>
        </div>
        <div class="pane">
          <div class="hd">Vista previa <small>tal cual se verá</small></div>
          <div id="ePreview"></div>
        </div>

        <!-- Ajustes: lo que se rellena una vez y se mira de reojo. -->
        <aside class="pane" id="eSide">
          <div class="hd">Ficha</div>
          <!-- La caja va aparte de la cabecera, igual que en las otras dos
               columnas: así las tres empiezan y acaban a la misma altura. -->
          <div id="eSideBox">
            <div id="eSideBody">
            <label>Título<input type="text" id="eTitle"></label>
            <label>Artista<input type="text" id="eArtist"></label>
            <label>Categoría <small>estilo musical</small>
              <input type="text" id="eGenre" list="genreList" placeholder="Rock, bolero, folk…">
              <datalist id="genreList"></datalist></label>
            <label id="ePlaylistWrap">Lista
              <select id="ePlaylist">
                <option value="">Sin lista</option>
              </select></label>
            <label id="eVisibilityWrap">Visibilidad
              <select id="eVisibility">
                <option value="private">Privada (solo yo)</option>
                <option value="public">Pública (cualquiera puede verla)</option>
              </select>
            </label>
            <label id="eLockedWrap" class="row" style="gap:8px;align-items:center">
              <input type="checkbox" id="eLocked" style="width:auto">
              <span>Bloqueada <small>pide confirmación antes de editarla</small></span>
            </label>

            <div id="eVersionHead">
              <label>Nombre de la versión <small>«Acústica», «En Do», «Tablatura»…</small>
                <input type="text" id="eVersionName" placeholder="Acústica"></label>
              <label id="eNoteWrap">Mensaje para quien la revise <small>opcional</small>
                <input type="text" id="eNote" placeholder="Qué cambia y por qué"></label>
            </div>

            <div>
              <div class="vv-kicker" style="margin-bottom:6px">Capo</div>
              <div class="pills" id="eCapoPills"></div>
            </div>

            <label>URL de la partitura original <small>opcional</small>
              <input type="url" id="eSource" placeholder="https://…"></label>
            <label>Vídeo de YouTube <small>opcional; se ve junto a la partitura</small>
              <input type="text" id="eTube" placeholder="https://youtu.be/…"></label>
            <div class="row">
              <button id="eTubeSearch" title="Abre la búsqueda en otra pestaña">Buscar en YouTube</button>
              <span id="eTubeMsg" class="nota"></span>
            </div>
          </div>

            <!-- Los botones cierran la columna: se llega a ellos sin buscar. -->
            <div id="eAcciones">
              <div id="editAviso" class="aviso hidden"></div>
              <div class="msg" id="editMsg"></div>
              <button class="primary" id="saveBtn">Guardar</button>
              <button id="proposeBtn" class="hidden" title="Un editor la revisará antes de publicarla">Proponer publicación</button>
              <button id="cancelEdit">Cancelar</button>
              <button id="deleteBtn" class="hidden danger">Eliminar</button>
            </div>
          </div>
        </aside>
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
    <div class="lado der" id="vLadoDer">
      <button id="vMenuBtn" class="tool soloEstrecho" aria-expanded="false" aria-controls="vMenu"
              aria-label="Acciones de la partitura" title="Acciones de la partitura">
        <svg class="ic"><use href="#ac-menu"></use></svg><span>Acciones</span></button>
      <a id="vSource" class="hidden" href="#" target="_blank" rel="noopener noreferrer"
         title="Abrir la partitura original en otra pestaña">Original ↗</a>
      <button id="vShare" class="tool compartir" title="Copiar el enlace de esta partitura">
        <svg class="ic"><use href="#ac-submit"></use></svg><span>Compartir</span></button>
      <button id="vPrint" class="tool imprimir" title="Abre el diálogo de impresión; ahí puedes elegir «Guardar como PDF»">
        <svg class="ic"><use href="#ac-sheet-music"></use></svg><span>PDF</span></button>
      <button id="vEdit" class="hidden">Editar</button>
    </div>
    <!-- En estrecho, las acciones se mudan aquí dentro (ver colocarAcciones). -->
    <div id="vMenu" hidden></div>
  </div>
  <div id="vChordBar" class="chordBar hidden"></div>
  <div id="vMain">
    <!-- Solo en móvil: velo y botón inferior del panel deslizante. -->
    <div id="vVelo" hidden></div>
    <button id="vTools" class="tool soloEstrecho" aria-expanded="false" aria-controls="vSide">
      <svg class="ic"><use href="#ac-settings"></use></svg><span>Mandos</span></button>
    <div id="vSide">
      <button id="vToolsClose" class="ghost soloEstrecho" title="Cerrar los mandos">Cerrar</button>
      <div id="vCtrl">
      <!-- Cada herramienta tiene su color de la paleta: turquesa lo que corre
           (scroll), amarillo lo que marca el pulso (metrónomo), coral los
           acordes y teal el tono. Antes eran seis botones de texto idénticos y
           no se distinguía uno de otro de un vistazo. -->
      <div class="row herr scroll">
        <button id="vPlay" class="tool" aria-pressed="false" title="Desplazamiento automático">
          <svg class="ic"><use id="vPlayIcon" href="#ac-play-circle"></use></svg><span>Scroll</span>
        </button>
        <!-- Hasta 100 px/s: por encima la hoja pasa más rápido de lo que se
             toca, y el rango de 300 dejaba lo útil (10-40) apelotonado en el
             primer centímetro de la barra. -->
        <input type="range" id="vSpeed" min="2" max="100" value="20" style="min-width:60px">
        <label id="vSpeedVal">20 px/s</label>
      </div>
      <div class="row herr tono">
        <label>Tono</label>
        <button id="vDown" class="tool" title="Bajar medio tono">–</button>
        <label id="vTone" style="min-width:2.5em;text-align:center">±0</label>
        <button id="vUp" class="tool" title="Subir medio tono">+</button>
        <button id="vFlat" class="tool" title="Escribir con bemoles">♭</button>
      </div>
      <div class="row herr letra">
        <label>Letra</label>
        <button id="vFontDown" class="tool" title="Letra más pequeña">A-</button>
        <button id="vFontUp" class="tool" title="Letra más grande">A+</button>
        <span class="grow"></span>
      </div>
      <div class="row herr acordes">
        <button id="vChords" class="tool" aria-pressed="false" title="Diagramas de los acordes de esta partitura">
          <svg class="ic"><use href="#ac-chord"></use></svg><span>Acordes</span>
        </button>
      </div>
      <div class="row herr metro">
        <button id="vMetro" class="tool" aria-pressed="false" title="Metrónomo">
          <svg class="ic"><use id="vMetroIcon" href="#ac-metronome"></use></svg><span>Metrónomo</span>
        </button>
        <span id="vBeats" class="row" style="gap:4px"></span>
      </div>
      <div class="row herr metro">
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
    <div id="vBody"><div id="vSheet"></div><div id="vComments"></div><div id="vRelated"></div></div>
    <div id="vTube"><div class="marco"></div></div>
  </div>
</div>

<!-- Iconos del kit (icons/sprite.svg), solo los que se usan. Van como <symbol>
     porque el trazo lee currentColor y el detalle --ac-icon-accent: así cada
     herramienta pinta su icono con el color de su papel sin duplicar ficheros.
     Las dos últimas no salen del kit: le faltan pausa y stop. -->
<svg xmlns="http://www.w3.org/2000/svg" style="display:none" aria-hidden="true">
  <symbol id="ac-play-circle" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.6"></circle><path d="m10.2 8.6 5.4 3.4-5.4 3.4z" fill="var(--ac-icon-accent, #FF6B6B)" stroke="var(--ac-icon-accent, #FF6B6B)"></path></symbol>
  <symbol id="ac-metronome" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M9.2 4h5.6l4.4 16.4H4.8z"></path><path d="M6.2 14.6h11.6"></path><path d="M12 14.6 19.6 5.4" stroke="var(--ac-icon-accent, #FF6B6B)"></path></symbol>
  <symbol id="ac-chord" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="4.5" width="16" height="15" rx="1"></rect><path d="M9.3 4.5v15M14.7 4.5v15M4 9.5h16M4 14.5h16"></path><circle cx="9.3" cy="7" r="1.7" fill="var(--ac-icon-accent, #FF6B6B)" stroke="none"></circle><circle cx="14.7" cy="12" r="1.7" fill="var(--ac-icon-accent, #FF6B6B)" stroke="none"></circle></symbol>
  <symbol id="ac-sheet-music" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M5.4 3.6h9l4.6 4.6v12.2H5.4z"></path><path d="M14.2 3.6v4.8H19"></path><circle cx="9.4" cy="16.4" r="1.8" fill="var(--ac-icon-accent, #FF6B6B)" stroke="var(--ac-icon-accent, #FF6B6B)"></circle><path d="M11.2 16.4v-5l3.4-1"></path></symbol>
  <symbol id="ac-submit" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M20.4 3.6 3.6 10.4l6.6 2.8 2.8 6.6z"></path><path d="M20.4 3.6 10.2 13.2" stroke="var(--ac-icon-accent, #FF6B6B)"></path></symbol>
  <symbol id="ac-star" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3.6l2.6 5.5 6 .8-4.4 4.2 1.1 6-5.3-2.9-5.3 2.9 1.1-6L3.4 9.9l6-.8z" fill="currentColor" stroke="currentColor"></path></symbol>
  <symbol id="ac-star-outline" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3.6l2.6 5.5 6 .8-4.4 4.2 1.1 6-5.3-2.9-5.3 2.9 1.1-6L3.4 9.9l6-.8z"></path></symbol>
  <symbol id="ac-plus" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5.2v13.6M5.2 12h13.6"></path></symbol>
  <symbol id="ac-search" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="10.8" cy="10.8" r="6.4"></circle><path d="m15.6 15.6 4.4 4.4" stroke="var(--ac-icon-accent, #FF6B6B)"></path></symbol>
  <symbol id="ac-settings" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M12 3.4v2.6M12 18v2.6M4.9 7.6l2.3 1.3M16.8 15.1l2.3 1.3M4.9 16.4l2.3-1.3M16.8 8.9l2.3-1.3" stroke="var(--ac-icon-accent, #FF6B6B)"></path></symbol>
  <symbol id="ac-review" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M7.4 20.6V10l4.3-6.7c1.7.2 2.4 1.3 2.2 3l-.4 3.7h5a2 2 0 0 1 2 2.4l-1.3 6a2 2 0 0 1-2 1.6z"></path><rect x="2.4" y="10" width="4.6" height="10.6" rx="1.2" fill="var(--ac-icon-accent, #FF6B6B)" stroke="var(--ac-icon-accent, #FF6B6B)"></rect></symbol>
  <symbol id="ac-user" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="3.7"></circle><path d="M4.9 20.5a7.1 7.1 0 0 1 14.2 0"></path></symbol>
  <symbol id="ac-menu" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M3.6 7h16.8M3.6 12h16.8M3.6 17h16.8"></path></symbol>
  <symbol id="ac-notes" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="7.6" cy="17" r="2.7"></circle><circle cx="17" cy="15" r="2.7"></circle><path d="M10.3 17V7.2l9.4-2.4V15"></path><path d="M10.3 9.6l9.4-2.4" stroke="var(--ac-icon-accent, #FF6B6B)"></path></symbol>
  <symbol id="ic-candado" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="4.6" y="10.4" width="14.8" height="9.6" rx="2.4"></rect><path d="M8.2 10.4V7.6a3.8 3.8 0 0 1 7.6 0v2.8"></path><circle cx="12" cy="15.2" r="1.6" fill="var(--ac-icon-accent, #FF6B6B)" stroke="none"></circle></symbol>
  <symbol id="ic-pausa" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.6"></circle><path d="M10.2 9v6M13.8 9v6" stroke="var(--ac-icon-accent, #FF6B6B)"></path></symbol>
  <symbol id="ic-stop" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M9.2 4h5.6l4.4 16.4H4.8z"></path><path d="M6.2 14.6h11.6"></path><rect x="9.6" y="8.4" width="4.8" height="4.8" rx="1" fill="var(--ac-icon-accent, #FF6B6B)" stroke="var(--ac-icon-accent, #FF6B6B)"></rect></symbol>
</svg>

<!-- Avisos flotantes (aria-live: un lector de pantalla los canta sin robar el
     foco) y diálogo genérico de confirmar/pedir texto, con el estilo de la web. -->
<div id="toastWrap" class="toasts" aria-live="polite" aria-atomic="false"></div>

<div id="dlgModal" class="modal hidden" role="dialog" aria-modal="true" aria-labelledby="dlgTitle">
  <div class="modalBox dlgBox">
    <h3 id="dlgTitle">Confirmar</h3>
    <p id="dlgText" class="dlgText"></p>
    <label id="dlgField" class="dlgField hidden">
      <span id="dlgLabel" class="lbl"></span>
      <input id="dlgInput" type="text" autocomplete="off" spellcheck="false">
    </label>
    <div class="dlgBtns">
      <button id="dlgCancel" type="button">Cancelar</button>
      <button id="dlgOk" type="button" class="primary">Aceptar</button>
    </div>
  </div>
</div>

<script src="/static/vivace.js" defer></script>
<script src="/static/vivace-app.js" defer></script>

</body>
</html>`;

/** Favicon del kit: marca G sobre teal. Bajo 32 px el mástil no se lee, y esta
    es la versión que el propio kit manda usar a ese tamaño. */
export const FAVICON_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64">
  <rect width="64" height="64" rx="14" fill="#1A535C"></rect>
  <rect x="26" y="12" width="12" height="20" rx="3" fill="#FFE66D"></rect>
  <rect x="27" y="33" width="10" height="7" fill="#FF6B6B"></rect>
  <text x="32" y="56" text-anchor="middle" font-family="Montserrat, system-ui, sans-serif" font-weight="700" font-size="30" fill="#FFE66D">G</text>
</svg>`;

/** La misma marca en night: el kit trae par para cada recurso con color fijo,
    y el conmutador de tema le cambia el href al <link rel=icon>. */
export const FAVICON_DARK_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64">
  <rect width="64" height="64" rx="14" fill="#0B1D21"></rect>
  <rect x="26" y="12" width="12" height="20" rx="3" fill="#94E4DD"></rect>
  <rect x="27" y="33" width="10" height="7" fill="#FF8A8A"></rect>
  <text x="32" y="56" text-anchor="middle" font-family="Montserrat, system-ui, sans-serif" font-weight="700" font-size="30" fill="#94E4DD">G</text>
</svg>`;

/** Mosaico de notas del fondo (400x400, se repite), en sus dos versiones. */
export const PATTERN_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400" width="400" height="400">
  <rect width="400" height="400" fill="#F7EFE3"></rect>
  <g transform="translate(40 50) rotate(-18) scale(0.9)" fill="none" stroke="#E8D4B8" stroke-width="4"><path d="M0 0h84M0 18h84M0 36h84M0 54h84"></path><path d="M0 0v54M28 0v54M56 0v54M84 0v54"></path><circle cx="28" cy="0" r="6" fill="#E8D4B8" stroke="none"></circle><circle cx="56" cy="18" r="6" fill="#E8D4B8" stroke="none"></circle><circle cx="14" cy="36" r="6" fill="#E8D4B8" stroke="none"></circle></g>
  <g transform="translate(250 60) rotate(8)" fill="#E8D4B8"><ellipse cx="0" cy="16" rx="6.4" ry="5"></ellipse><ellipse cx="19" cy="12" rx="6.4" ry="5"></ellipse><path d="M5.4 16V-6l19-4v22h-2.6V-7.4L8 -3.6V16z"></path></g>
  <circle cx="350" cy="120" r="16" fill="none" stroke="#E8D4B8" stroke-width="6"></circle>
  <g transform="translate(160 190) rotate(-8) scale(1.4)" fill="#E8D4B8"><ellipse cx="0" cy="14" rx="7" ry="5.4" transform="rotate(-18)"></ellipse><path d="M5.6 12.8V-14c4 1.6 8 3.4 8 8.2 0-6.4-4-8.6-8-10.4z"></path></g>
  <g transform="translate(230 250) rotate(12) scale(0.7)" fill="none" stroke="#E8D4B8" stroke-width="4"><path d="M0 0h84M0 18h84M0 36h84M0 54h84"></path><path d="M0 0v54M28 0v54M56 0v54M84 0v54"></path><circle cx="28" cy="0" r="6" fill="#E8D4B8" stroke="none"></circle><circle cx="56" cy="18" r="6" fill="#E8D4B8" stroke="none"></circle><circle cx="14" cy="36" r="6" fill="#E8D4B8" stroke="none"></circle></g>
  <g transform="translate(60 300) rotate(-12)" fill="#E8D4B8"><ellipse cx="0" cy="16" rx="6.4" ry="5"></ellipse><ellipse cx="19" cy="12" rx="6.4" ry="5"></ellipse><path d="M5.4 16V-6l19-4v22h-2.6V-7.4L8 -3.6V16z"></path></g>
  <g transform="translate(340 300) rotate(14) scale(1.2)" fill="#E8D4B8"><ellipse cx="0" cy="14" rx="7" ry="5.4" transform="rotate(-18)"></ellipse><path d="M5.6 12.8V-14c4 1.6 8 3.4 8 8.2 0-6.4-4-8.6-8-10.4z"></path></g>
  <circle cx="150" cy="350" r="12" fill="none" stroke="#E8D4B8" stroke-width="6"></circle>
</svg>`;

export const PATTERN_DARK_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400" width="400" height="400">
  <rect width="400" height="400" fill="#0F2429"></rect>
  <g transform="translate(40 50) rotate(-18) scale(0.9)" fill="none" stroke="#2E5A60" stroke-width="4"><path d="M0 0h84M0 18h84M0 36h84M0 54h84"></path><path d="M0 0v54M28 0v54M56 0v54M84 0v54"></path><circle cx="28" cy="0" r="6" fill="#2E5A60" stroke="none"></circle><circle cx="56" cy="18" r="6" fill="#2E5A60" stroke="none"></circle><circle cx="14" cy="36" r="6" fill="#2E5A60" stroke="none"></circle></g>
  <g transform="translate(250 60) rotate(8)" fill="#2E5A60"><ellipse cx="0" cy="16" rx="6.4" ry="5"></ellipse><ellipse cx="19" cy="12" rx="6.4" ry="5"></ellipse><path d="M5.4 16V-6l19-4v22h-2.6V-7.4L8 -3.6V16z"></path></g>
  <circle cx="350" cy="120" r="16" fill="none" stroke="#2E5A60" stroke-width="6"></circle>
  <g transform="translate(160 190) rotate(-8) scale(1.4)" fill="#2E5A60"><ellipse cx="0" cy="14" rx="7" ry="5.4" transform="rotate(-18)"></ellipse><path d="M5.6 12.8V-14c4 1.6 8 3.4 8 8.2 0-6.4-4-8.6-8-10.4z"></path></g>
  <g transform="translate(230 250) rotate(12) scale(0.7)" fill="none" stroke="#2E5A60" stroke-width="4"><path d="M0 0h84M0 18h84M0 36h84M0 54h84"></path><path d="M0 0v54M28 0v54M56 0v54M84 0v54"></path><circle cx="28" cy="0" r="6" fill="#2E5A60" stroke="none"></circle><circle cx="56" cy="18" r="6" fill="#2E5A60" stroke="none"></circle><circle cx="14" cy="36" r="6" fill="#2E5A60" stroke="none"></circle></g>
  <g transform="translate(60 300) rotate(-12)" fill="#2E5A60"><ellipse cx="0" cy="16" rx="6.4" ry="5"></ellipse><ellipse cx="19" cy="12" rx="6.4" ry="5"></ellipse><path d="M5.4 16V-6l19-4v22h-2.6V-7.4L8 -3.6V16z"></path></g>
  <g transform="translate(340 300) rotate(14) scale(1.2)" fill="#2E5A60"><ellipse cx="0" cy="14" rx="7" ry="5.4" transform="rotate(-18)"></ellipse><path d="M5.6 12.8V-14c4 1.6 8 3.4 8 8.2 0-6.4-4-8.6-8-10.4z"></path></g>
  <circle cx="150" cy="350" r="12" fill="none" stroke="#2E5A60" stroke-width="6"></circle>
</svg>`;
