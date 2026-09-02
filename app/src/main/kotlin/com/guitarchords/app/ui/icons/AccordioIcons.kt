package com.guitarchords.app.ui.icons

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.guitarchords.app.ui.theme.extendedColors

/*
 * Iconos del paquete Accordio (accordio-web-kit/icons), los mismos que la web.
 *
 * No son ficheros: los trazos están aquí y el ImageVector se construye con los
 * colores del tema. Es a propósito. Los iconos del kit tienen DOS tonos —el
 * trazo en el color del texto y un detalle en coral— y ese coral cambia entre
 * claro y oscuro (#FF6B6B / #FF8A8A). Un vector drawable con el color dentro
 * necesitaría dos copias de cada icono y un juego de recursos por tema; y con
 * `tint` de Compose se pierde el detalle, porque tiñe el icono entero.
 *
 * La rejilla es la del kit: 24 dp, trazo 1,7 y remates redondeados.
 */


/*
 * Marca del mástil (accordio-web-kit/logo/mark-fretboard). No es un icono de
 * interfaz: lleva sus propios colores, los mismos que la cabecera de la web, así
 * que se pinta con `tint = Color.Unspecified`.
 */
private const val REJILLA_MARCA_W = 132f
private const val REJILLA_MARCA_H = 92f

private val MarcaLineas = listOf(
    "M8 10H124M8 26H124M8 42H124M8 58H124M8 74H124",
    "M8 10V74M37 10V74M66 10V74M95 10V74M124 10V74"
)

/** Digitación de la marca: posición y papel del color. */
private val MarcaPuntos = listOf(
    Triple(66f, 10f, 0), Triple(37f, 26f, 0), Triple(95f, 26f, 1),
    Triple(8f, 42f, 0), Triple(80f, 42f, 1), Triple(80f, 58f, 2), Triple(51f, 74f, 1)
)

private fun circulo(cx: Float, cy: Float, r: Float) =
    "M$cx $cy m-$r 0 a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 ${-2 * r} 0z"

/** Trazos de un icono, separados por el papel que hacen. */
private class IconoKit(
    val trazos: List<String>,
    val trazosAcento: List<String> = emptyList(),
    val rellenosAcento: List<String> = emptyList(),
    /** Relleno en el color principal: la estrella llena del kit es el único caso. */
    val rellenos: List<String> = emptyList()
)

private const val REJILLA = 24f
private const val GROSOR = 1.7f

private fun IconoKit.construir(color: Color, acento: Color, nombre: String): ImageVector {
    val b = ImageVector.Builder(
        name = nombre,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = REJILLA, viewportHeight = REJILLA
    )
    fun trazar(datos: String, tinta: Color) {
        b.addPath(
            pathData = PathParser().parsePathString(datos).toNodes(),
            stroke = SolidColor(tinta),
            strokeLineWidth = GROSOR,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    rellenos.forEach {
        b.addPath(pathData = PathParser().parsePathString(it).toNodes(), fill = SolidColor(color))
    }
    trazos.forEach { trazar(it, color) }
    trazosAcento.forEach { trazar(it, acento) }
    rellenosAcento.forEach {
        b.addPath(pathData = PathParser().parsePathString(it).toNodes(), fill = SolidColor(acento))
    }
    return b.build()
}

private val ChordDatos = IconoKit(
    trazos = listOf(
        "M5.0 4.5h14.0a1.0 1.0 0 0 1 1.0 1.0v13.0a1.0 1.0 0 0 1 -1.0 1.0h-14.0a1.0 1.0 0 0 1 -1.0 -1.0v-13.0a1.0 1.0 0 0 1 1.0 -1.0z",
        "M9.3 4.5v15M14.7 4.5v15M4 9.5h16M4 14.5h16"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = listOf(
        "M7.6000000000000005 7.0a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0z",
        "M13.0 12.0a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0z"
    )
)

private val ChordTypesDatos = IconoKit(
    trazos = listOf(
        "M4 8.5h16",
        "M5.0 8.5h14.0a1.0 1.0 0 0 1 1.0 1.0v9.0a1.0 1.0 0 0 1 -1.0 1.0h-14.0a1.0 1.0 0 0 1 -1.0 -1.0v-9.0a1.0 1.0 0 0 1 1.0 -1.0z",
        "M9.3 8.5v11M14.7 8.5v11M4 14h16",
        "M5.6 5.4a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z",
        "M10.6 5.4a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = listOf(
        "M15.6 5.4a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z"
    )
)

private val MetronomeDatos = IconoKit(
    trazos = listOf(
        "M9.2 4h5.6l4.4 16.4H4.8z",
        "M6.2 14.6h11.6"
    ),
    trazosAcento = listOf(
        "M12 14.6 19.6 5.4"
    ),
    rellenosAcento = emptyList()
)

private val SheetMusicDatos = IconoKit(
    trazos = listOf(
        "M5.4 3.6h9l4.6 4.6v12.2H5.4z",
        "M14.2 3.6v4.8H19",
        "M11.2 16.4v-5l3.4-1"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = listOf(
        "M7.6000000000000005 16.4a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0z"
    )
)

private val PlayCircleDatos = IconoKit(
    trazos = listOf(
        "M3.4000000000000004 12.0a8.6 8.6 0 1 0 17.2 0a8.6 8.6 0 1 0 -17.2 0z"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = listOf(
        "m10.2 8.6 5.4 3.4-5.4 3.4z"
    )
)

private val NotesDatos = IconoKit(
    trazos = listOf(
        "M4.8999999999999995 17.0a2.7 2.7 0 1 0 5.4 0a2.7 2.7 0 1 0 -5.4 0z",
        "M14.3 15.0a2.7 2.7 0 1 0 5.4 0a2.7 2.7 0 1 0 -5.4 0z",
        "M10.3 17V7.2l9.4-2.4V15"
    ),
    trazosAcento = listOf(
        "M10.3 9.6l9.4-2.4"
    ),
    rellenosAcento = emptyList()
)

private val StarDatos = IconoKit(
    trazos = listOf(
        "M12 3.6l2.6 5.5 6 .8-4.4 4.2 1.1 6-5.3-2.9-5.3 2.9 1.1-6L3.4 9.9l6-.8z"
    ),
    rellenos = listOf(
        "M12 3.6l2.6 5.5 6 .8-4.4 4.2 1.1 6-5.3-2.9-5.3 2.9 1.1-6L3.4 9.9l6-.8z"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = emptyList()
)

private val StarOutlineDatos = IconoKit(
    trazos = listOf(
        "M12 3.6l2.6 5.5 6 .8-4.4 4.2 1.1 6-5.3-2.9-5.3 2.9 1.1-6L3.4 9.9l6-.8z"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = emptyList()
)

private val PlusDatos = IconoKit(
    trazos = listOf(
        "M12 5.2v13.6M5.2 12h13.6"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = emptyList()
)

private val PlusCircleDatos = IconoKit(
    trazos = listOf(
        "M3.4000000000000004 12.0a8.6 8.6 0 1 0 17.2 0a8.6 8.6 0 1 0 -17.2 0z"
    ),
    trazosAcento = listOf(
        "M12 8v8M8 12h8"
    ),
    rellenosAcento = emptyList()
)

private val SearchDatos = IconoKit(
    trazos = listOf(
        "M4.4 10.8a6.4 6.4 0 1 0 12.8 0a6.4 6.4 0 1 0 -12.8 0z"
    ),
    trazosAcento = listOf(
        "m15.6 15.6 4.4 4.4"
    ),
    rellenosAcento = emptyList()
)

private val SettingsDatos = IconoKit(
    trazos = listOf(
        "M9.0 12.0a3.0 3.0 0 1 0 6.0 0a3.0 3.0 0 1 0 -6.0 0z"
    ),
    trazosAcento = listOf(
        "M12 3.4v2.6M12 18v2.6M4.9 7.6l2.3 1.3M16.8 15.1l2.3 1.3M4.9 16.4l2.3-1.3M16.8 8.9l2.3-1.3"
    ),
    rellenosAcento = emptyList()
)

private val SubmitDatos = IconoKit(
    trazos = listOf(
        "M20.4 3.6 3.6 10.4l6.6 2.8 2.8 6.6z"
    ),
    trazosAcento = listOf(
        "M20.4 3.6 10.2 13.2"
    ),
    rellenosAcento = emptyList()
)

private val UserDatos = IconoKit(
    trazos = listOf(
        "M8.3 8.0a3.7 3.7 0 1 0 7.4 0a3.7 3.7 0 1 0 -7.4 0z",
        "M4.9 20.5a7.1 7.1 0 0 1 14.2 0"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = emptyList()
)

private val ProfileDatos = IconoKit(
    trazos = listOf(
        "M5.9 8.2a3.5 3.5 0 1 0 7.0 0a3.5 3.5 0 1 0 -7.0 0z",
        "M3 20.4a6.5 6.5 0 0 1 12.8 0"
    ),
    trazosAcento = listOf(
        "M16.2 5.3a3.5 3.5 0 0 1 0 5.9",
        "M17.8 14.7A6.6 6.6 0 0 1 21 20.4"
    ),
    rellenosAcento = emptyList()
)

private val ReviewDatos = IconoKit(
    trazos = listOf(
        "M7.4 20.6V10l4.3-6.7c1.7.2 2.4 1.3 2.2 3l-.4 3.7h5a2 2 0 0 1 2 2.4l-1.3 6a2 2 0 0 1-2 1.6z"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = listOf(
        "M3.5999999999999996 10.0h2.1999999999999997a1.2 1.2 0 0 1 1.2 1.2v8.2a1.2 1.2 0 0 1 -1.2 1.2h-2.1999999999999997a1.2 1.2 0 0 1 -1.2 -1.2v-8.2a1.2 1.2 0 0 1 1.2 -1.2z"
    )
)

private val UpvoteDatos = IconoKit(
    trazos = listOf(
        "M12 20.5V4.6"
    ),
    trazosAcento = listOf(
        "M5.2 11.4 12 4.6l6.8 6.8"
    ),
    rellenosAcento = emptyList()
)

private val TabDatos = IconoKit(
    trazos = listOf(
        "M3.4 6.6h17.2M3.4 10.2h17.2M3.4 13.8h17.2M3.4 17.4h17.2"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = listOf(
        "M6.800000000000001 6.6a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0z",
        "M12.0 13.8a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0z"
    )
)

private val HomeDatos = IconoKit(
    trazos = listOf(
        "M3.8 10.6 12 4l8.2 6.6V20a1 1 0 0 1-1 1H4.8a1 1 0 0 1-1-1z"
    ),
    trazosAcento = listOf(
        "M9.4 21v-6.2h5.2V21"
    ),
    rellenosAcento = emptyList()
)

private val BellDatos = IconoKit(
    trazos = listOf(
        "M6.2 9.4a5.8 5.8 0 1 1 11.6 0c0 4.1 1.1 5.9 1.9 6.9H4.3c.8-1 1.9-2.8 1.9-6.9z"
    ),
    trazosAcento = listOf(
        "M9.9 19.6a2.2 2.2 0 0 0 4.2 0"
    ),
    rellenosAcento = emptyList()
)

private val MenuDatos = IconoKit(
    trazos = listOf(
        "M3.6 7h16.8M3.6 12h16.8M3.6 17h16.8"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = emptyList()
)

private val ChevronRightDatos = IconoKit(
    trazos = listOf(
        "m9.5 5.5 7 6.5-7 6.5"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = emptyList()
)

private val GuitarCustomDatos = IconoKit(
    trazos = listOf(
        "M14.6 9.6c-1.1-1-2.6-1.5-4.1-1.2-2 .4-2.9 1.9-4.4 3.4a4.6 4.6 0 0 0 .1 6.5 4.4 4.4 0 0 0 6.4 0c1.5-1.5 3-2.5 3.4-4.4.3-1.6-.2-3.2-1.4-4.3z",
        "M15.6 8.6 19.4 4.8"
    ),
    trazosAcento = emptyList(),
    rellenosAcento = listOf(
        "M9.1 15.0a1.9 1.9 0 1 0 3.8 0a1.9 1.9 0 1 0 -3.8 0z",
        "m18.6 4 1.6-1.6L21.8 4l-1.6 1.6z"
    )
)

/**
 * Los iconos, ya con los colores del tema.
 *
 * `color` es el trazo (por defecto, el del texto que haya alrededor) y `acento`
 * el detalle, que sale del rol `chord` del tema: el coral del kit en la rampa
 * que se ve en cada modo.
 */
object AccordioIcons {
    @Composable private fun icono(datos: IconoKit, nombre: String, color: Color?, acento: Color?): ImageVector {
        val trazo = color ?: MaterialTheme.colorScheme.onSurface
        val detalle = acento ?: MaterialTheme.extendedColors.chord
        return remember(datos, trazo, detalle) { datos.construir(trazo, detalle, nombre) }
    }

    @Composable fun acorde(color: Color? = null, acento: Color? = null) = icono(ChordDatos, "Acorde", color, acento)
    @Composable fun tiposDeAcorde(color: Color? = null, acento: Color? = null) = icono(ChordTypesDatos, "TiposDeAcorde", color, acento)
    @Composable fun metronomo(color: Color? = null, acento: Color? = null) = icono(MetronomeDatos, "Metronomo", color, acento)
    @Composable fun partitura(color: Color? = null, acento: Color? = null) = icono(SheetMusicDatos, "Partitura", color, acento)
    @Composable fun reproducir(color: Color? = null, acento: Color? = null) = icono(PlayCircleDatos, "Reproducir", color, acento)
    @Composable fun notas(color: Color? = null, acento: Color? = null) = icono(NotesDatos, "Notas", color, acento)
    @Composable fun estrella(color: Color? = null, acento: Color? = null) = icono(StarDatos, "Estrella", color, acento)
    @Composable fun estrellaHueca(color: Color? = null, acento: Color? = null) = icono(StarOutlineDatos, "EstrellaHueca", color, acento)
    @Composable fun mas(color: Color? = null, acento: Color? = null) = icono(PlusDatos, "Mas", color, acento)
    @Composable fun masCirculo(color: Color? = null, acento: Color? = null) = icono(PlusCircleDatos, "MasCirculo", color, acento)
    @Composable fun buscar(color: Color? = null, acento: Color? = null) = icono(SearchDatos, "Buscar", color, acento)
    @Composable fun ajustes(color: Color? = null, acento: Color? = null) = icono(SettingsDatos, "Ajustes", color, acento)
    @Composable fun compartir(color: Color? = null, acento: Color? = null) = icono(SubmitDatos, "Compartir", color, acento)
    @Composable fun usuario(color: Color? = null, acento: Color? = null) = icono(UserDatos, "Usuario", color, acento)
    @Composable fun perfil(color: Color? = null, acento: Color? = null) = icono(ProfileDatos, "Perfil", color, acento)
    @Composable fun revision(color: Color? = null, acento: Color? = null) = icono(ReviewDatos, "Revision", color, acento)
    @Composable fun voto(color: Color? = null, acento: Color? = null) = icono(UpvoteDatos, "Voto", color, acento)
    @Composable fun tablatura(color: Color? = null, acento: Color? = null) = icono(TabDatos, "Tablatura", color, acento)
    @Composable fun inicio(color: Color? = null, acento: Color? = null) = icono(HomeDatos, "Inicio", color, acento)
    @Composable fun aviso(color: Color? = null, acento: Color? = null) = icono(BellDatos, "Aviso", color, acento)
    @Composable fun menu(color: Color? = null, acento: Color? = null) = icono(MenuDatos, "Menu", color, acento)
    @Composable fun siguiente(color: Color? = null, acento: Color? = null) = icono(ChevronRightDatos, "Siguiente", color, acento)
    @Composable fun guitarra(color: Color? = null, acento: Color? = null) = icono(GuitarCustomDatos, "Guitarra", color, acento)

    /**
     * La marca de Accordio, con sus colores propios. Píntala con
     * `tint = Color.Unspecified` o Compose la aplanará a un solo tono.
     */
    @Composable
    fun marca(): ImageVector {
        val ext = MaterialTheme.extendedColors
        val trazo = ext.onNav
        val acentos = listOf(trazo, ext.pending, MaterialTheme.colorScheme.primary)
        return remember(trazo, acentos) {
            val b = ImageVector.Builder(
                name = "Accordio",
                defaultWidth = 33.dp, defaultHeight = 23.dp,
                viewportWidth = REJILLA_MARCA_W, viewportHeight = REJILLA_MARCA_H
            )
            MarcaLineas.forEach {
                b.addPath(
                    pathData = PathParser().parsePathString(it).toNodes(),
                    stroke = SolidColor(trazo),
                    strokeLineWidth = 4f,
                    strokeLineCap = StrokeCap.Square
                )
            }
            MarcaPuntos.forEach { (cx, cy, papel) ->
                b.addPath(
                    pathData = PathParser().parsePathString(circulo(cx, cy, 8f)).toNodes(),
                    fill = SolidColor(acentos[papel])
                )
            }
            b.build()
        }
    }
}
