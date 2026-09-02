# Fuentes empaquetadas

La app incluye las dos tipografías del paquete de marca Vivace, en su versión
**variable** (un solo fichero cubre todos los pesos), tal como las publica
Google Fonts:

| Fuente | Fichero | Licencia |
|---|---|---|
| Space Grotesk (interfaz) | `app/src/main/res/font/space_grotesk.ttf` | [OFL-SpaceGrotesk.txt](OFL-SpaceGrotesk.txt) |
| JetBrains Mono (cifras) | `app/src/main/res/font/jetbrains_mono.ttf` | [OFL-JetBrainsMono.txt](OFL-JetBrainsMono.txt) |

Ambas están bajo **SIL Open Font License 1.1**, que permite empaquetarlas con
la aplicación. Se cargan en `ui/theme/Type.kt` con `FontVariation` para pedir
cada peso al eje `wght`.

## Montserrat y Poppins

Tipografías del paquete de marca Accordio: **Montserrat** para titulares y
**Poppins** para el texto. Ambas se distribuyen bajo la SIL Open Font License
1.1, la misma que JetBrains Mono y la que llevaba Space Grotesk.

- Montserrat — Copyright 2011 The Montserrat Project Authors
  (https://github.com/JulietaUla/Montserrat). Va el fichero **variable**, que da
  todos los pesos en uno.
- Poppins — Copyright 2020 The Poppins Project Authors
  (https://github.com/itfoundry/Poppins). No hay variable: van los tres pesos
  que gasta la interfaz (Regular, Medium, SemiBold).

Space Grotesk, la tipografía del paquete anterior (Vivace · Nocturno), se
retiró al adoptar Accordio: ya no la usaba nadie y eran 136 KB en cada APK.
