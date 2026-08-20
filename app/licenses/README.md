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
