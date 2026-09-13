package com.guitarchords.app.print

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.core.content.res.ResourcesCompat
import com.guitarchords.app.R
import com.guitarchords.app.chords.ChordParser
import com.guitarchords.app.chords.ContentBlock
import com.guitarchords.app.chords.RenderedLine
import com.guitarchords.app.data.Song
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Impresión de partituras.
 *
 * La hoja es la MISMA que imprime la web (ESTILO_IMPRESION en
 * worker/src/web-html.js): papel crema, cabecera con la marca del mástil, el
 * cuerpo en monoespaciada con los acordes en coral dentro de la línea, marca de
 * agua en diagonal y pie con el sitio y la paginación. Quien imprima desde el
 * móvil y desde el navegador tiene que acabar con el mismo folio en la mano, así
 * que aquí no hay licencias de estilo: cada medida es la de la hoja web pasada a
 * puntos PostScript, que es la unidad del lienzo de PdfDocument (72 dpi).
 *
 * Por eso tampoco se dibujan ya los diagramas de acordes: la hoja web no los
 * lleva, y con ellos las dos impresiones dejaban de ser la misma.
 */
object PrintAdapter {
    /**
     * @param semitones tono aplicado en el visor; sale como etiqueta en la
     *        cabecera igual que en la web. La transposición del contenido ya
     *        viene hecha en [song].
     */
    fun print(context: Context, song: Song, semitones: Int = 0) {
        val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        pm.print(
            "Accordio: ${song.title}",
            SongPrintAdapter(context, song, semitones),
            null
        )
    }
}

/* ---------- paleta y medidas ---------- */

/** Los colores de la hoja impresa, los mismos hex que la web. */
private object Tinta {
    val PAPEL = Color.parseColor("#F7EFE3")
    val TEXTO = Color.parseColor("#12363D")
    val TEAL = Color.parseColor("#1A535C")
    val CORAL = Color.parseColor("#FF6B6B")
    val ACORDE = Color.parseColor("#C93B3C")
    val APAGADO = Color.parseColor("#3F5257")
    val TENUE = Color.parseColor("#7B8E92")
    val LINEA = Color.parseColor("#DCE8E5")
    val CAPO_FONDO = Color.parseColor("#FFE66D")
    val CAPO_TEXTO = Color.parseColor("#6B550A")
    val TONO_FONDO = Color.parseColor("#E3EEF0")
    val TONO_TEXTO = Color.parseColor("#113941")
}

/**
 * Las medidas de la hoja web en puntos. `MM` y `PX` son las dos conversiones que
 * hacen falta: los márgenes del folio van en milímetros y la tipografía en
 * píxeles CSS (96 dpi), y el lienzo del PDF trabaja a 72.
 */
private object Medida {
    const val MM = 2.834646f
    const val PX = 0.75f

    val padArriba = 16f * MM
    val padLado = 15f * MM
    val padAbajo = 12f * MM

    val logoAncho = 30f * PX
    val logoAlto = 21f * PX
    val cabHueco = 10f * PX
    val cabPad = 5f * PX
    val cabBorde = 1.5f * PX
    val cabMargen = 7f * MM

    val etqHueco = 6f * PX
    val etqPadX = 9f * PX
    val etqPadY = 2f * PX

    val cuerpoTam = 12f * PX
    val cuerpoLinea = cuerpoTam * 1.5f

    val fuenteMargen = 6f * MM
    val fuentePad = 4f * PX

    val pieMargen = 6f * MM
    val pieBorde = 1f * PX
    val piePad = 5f * PX

    val marcaLogoAncho = 104f * PX
    val marcaLogoAlto = 73f * PX
    val marcaHueco = 22f * PX

    /** opacity:.07 */
    const val MARCA_ALFA = 18
}

/**
 * Los pinceles de la hoja. Montserrat y JetBrains Mono son ficheros
 * **variables** —el de Montserrat nace en Thin—, así que el peso se pide por el
 * eje `wght`; sin eso la cabecera saldría en pelo.
 */
private class Tipos(ctx: Context) {
    private val mono = ResourcesCompat.getFont(ctx, R.font.jetbrains_mono)
    private val titular = ResourcesCompat.getFont(ctx, R.font.montserrat)
    private val texto = ResourcesCompat.getFont(ctx, R.font.poppins_regular)
    private val textoFuerte = ResourcesCompat.getFont(ctx, R.font.poppins_semibold)

    val letra = pincel(mono, Medida.cuerpoTam, Tinta.TEXTO, 400)
    val acorde = pincel(mono, Medida.cuerpoTam, Tinta.ACORDE, 600)
    val tab = pincel(mono, Medida.cuerpoTam, Tinta.APAGADO, 400)

    val cabTitulo = pincel(titular, 15f * Medida.PX, Tinta.TEXTO, 700)
        .apply { letterSpacing = -0.02f }
    val cabAutor = pincel(texto, 12f * Medida.PX, Tinta.APAGADO)
    val etiqueta = pincel(textoFuerte, 9.5f * Medida.PX, Tinta.CAPO_TEXTO)
        .apply { letterSpacing = 0.1f }
    val fuente = pincel(texto, 9f * Medida.PX, Tinta.TENUE)
    val pie = pincel(texto, 9.5f * Medida.PX, Tinta.TENUE)
        .apply { letterSpacing = 0.06f }
    val marca = pincel(titular, 68f * Medida.PX, Tinta.TEXTO, 700)
        .apply { letterSpacing = 0.05f }

    /**
     * La línea del cuerpo mide 1,5 veces el tamaño de letra (el line-height de
     * la web), no lo que pida la fuente: así el corte de página cae en el mismo
     * sitio que en el navegador. La base se coloca centrando la caja.
     */
    val altoLinea = Medida.cuerpoLinea
    val baseLinea = run {
        val fm = letra.fontMetrics
        (Medida.cuerpoLinea - (-fm.ascent + fm.descent)) / 2f - fm.ascent
    }

    val altoEtiqueta = etiqueta.fontSpacing + 2 * Medida.etqPadY
    val altoCabecera = max(
        Medida.logoAlto,
        max(cabTitulo.fontSpacing, max(cabAutor.fontSpacing, altoEtiqueta))
    )
    val altoFuente = Medida.fuenteMargen + Medida.pieBorde + Medida.fuentePad + fuente.fontSpacing

    private fun pincel(tf: Typeface?, tam: Float, color: Int, peso: Int = 0): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = tam
            this.color = color
            // Solo las variables llevan eje; a las Poppins sueltas les sobra.
            if (peso > 0 && !setFontVariationSettings("'wght' $peso")) {
                isFakeBoldText = peso >= 600
            }
        }
}

/* ---------- contenido ---------- */

/** Un tramo de línea: letra o acorde. La web los pinta seguidos, en su sitio. */
private class Tramo(val texto: String, val acorde: Boolean)

private sealed class Trazo {
    abstract fun alto(t: Tipos): Float

    class Linea(val tramos: List<Tramo>, val tab: Boolean) : Trazo() {
        override fun alto(t: Tipos) = t.altoLinea

        /** Una línea que solo lleva acordes: la letra va en la de debajo. */
        val soloAcordes: Boolean
            get() = tramos.any { it.acorde } && tramos.none { !it.acorde && it.texto.isNotBlank() }
    }

    class Fuente(val texto: String) : Trazo() {
        override fun alto(t: Tipos) = t.altoFuente
    }
}

/* ---------- el adaptador ---------- */

private class SongPrintAdapter(
    private val context: Context,
    private val song: Song,
    private val semitones: Int
) : PrintDocumentAdapter() {

    private val tipos = Tipos(context)
    private val grupos: List<List<Trazo>> = agrupar(trazos())
    private var pdf: PrintedPdfDocument? = null
    private var hojas: List<List<Trazo>> = emptyList()

    override fun onLayout(
        old: PrintAttributes?,
        new: PrintAttributes,
        cancel: CancellationSignal,
        cb: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancel.isCanceled) { cb.onLayoutCancelled(); return }
        pdf = PrintedPdfDocument(context, new)
        hojas = paginar(Hoja(medidaPagina(new)))
        val info = PrintDocumentInfo.Builder("${song.title}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(hojas.size)
            .build()
        cb.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pageRanges: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancel: CancellationSignal,
        cb: WriteResultCallback
    ) {
        val doc = pdf ?: run { cb.onWriteFailed("pdf null"); return }
        try {
            for (i in hojas.indices) {
                if (cancel.isCanceled) { cb.onWriteCancelled(); return }
                val page = doc.startPage(i)
                val medida = page.info.pageWidth.toFloat() to page.info.pageHeight.toFloat()
                dibujarHoja(page.canvas, Hoja(medida), hojas[i], i + 1, hojas.size)
                doc.finishPage(page)
            }
            FileOutputStream(destination.fileDescriptor).use { out -> doc.writeTo(out) }
            cb.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            cb.onWriteFailed(e.message)
        } finally {
            doc.close()
            pdf = null
        }
    }

    override fun onFinish() {
        pdf?.close()
        pdf = null
    }

    /** A4 en puntos: los mils del papel a 72 dpi, que es como pagina PdfDocument. */
    private fun medidaPagina(a: PrintAttributes): Pair<Float, Float> {
        val m = a.mediaSize ?: PrintAttributes.MediaSize.ISO_A4
        val w = m.widthMils * 72f / 1000f
        val h = m.heightMils * 72f / 1000f
        return if (m.isPortrait) w to h else h to w
    }

    /** El reparto de la hoja: dónde empieza el cuerpo y cuánto cabe en él. */
    private inner class Hoja(medida: Pair<Float, Float>) {
        val ancho = medida.first
        val alto = medida.second
        val izq = Medida.padLado
        val der = ancho - Medida.padLado
        val cabArriba = Medida.padArriba
        val cabRaya = cabArriba + tipos.altoCabecera + Medida.cabPad
        val cuerpoArriba = cabRaya + Medida.cabBorde + Medida.cabMargen
        val pieTexto = alto - Medida.padAbajo - tipos.pie.fontSpacing
        val pieRaya = pieTexto - Medida.piePad
        val cuerpoAbajo = pieRaya - Medida.pieMargen
        val cuerpoAlto = cuerpoAbajo - cuerpoArriba
    }

    /* ---------- contenido ---------- */

    private fun trazos(): List<Trazo> {
        val out = mutableListOf<Trazo>()
        ChordParser.parseBlocks(song.content).forEach { bloque ->
            when (bloque) {
                is ContentBlock.Lyric -> out += Trazo.Linea(tramos(bloque.line), tab = false)
                is ContentBlock.Tab ->
                    bloque.rows.forEach { out += Trazo.Linea(listOf(Tramo(it, false)), tab = true) }
            }
        }
        val fuente = song.sourceUrl.trim()
        if (fuente.isNotBlank()) out += Trazo.Fuente("Fuente: $fuente")
        return out
    }

    /**
     * La línea, tal como la pinta la web: el `{X}` se sustituye EN SU SITIO por
     * el acorde, no se sube a una línea aparte.
     */
    private fun tramos(linea: RenderedLine): List<Tramo> {
        val out = mutableListOf<Tramo>()
        var pos = 0
        linea.chords.forEach { ct ->
            val corte = ct.position.coerceIn(pos, linea.lyric.length)
            if (corte > pos) out += Tramo(linea.lyric.substring(pos, corte), false)
            out += Tramo(ct.chord, true)
            pos = corte
        }
        if (pos < linea.lyric.length) out += Tramo(linea.lyric.substring(pos), false)
        return out
    }

    /**
     * Una línea de solo acordes y la letra que va debajo son un bloque:
     * separarlas en dos hojas deja los acordes sin canción y la canción sin
     * acordes. Es la regla de `soloAcordes` del paginador de la web.
     */
    private fun agrupar(lista: List<Trazo>): List<List<Trazo>> {
        val out = mutableListOf<List<Trazo>>()
        var i = 0
        while (i < lista.size) {
            val actual = lista[i]
            val siguiente = lista.getOrNull(i + 1)
            val pegar = actual is Trazo.Linea && actual.soloAcordes &&
                siguiente is Trazo.Linea && !siguiente.soloAcordes
            if (pegar) {
                out += listOf(actual, siguiente!!)
                i += 2
            } else {
                out += listOf(actual)
                i++
            }
        }
        return out
    }

    /** Se van metiendo bloques mientras quepan; el que se sale abre hoja nueva. */
    private fun paginar(hoja: Hoja): List<List<Trazo>> {
        val out = mutableListOf<List<Trazo>>()
        var actual = mutableListOf<Trazo>()
        var alto = 0f
        grupos.forEach { grupo ->
            val h = grupo.fold(0f) { acc, t -> acc + t.alto(tipos) }
            if (actual.isNotEmpty() && alto + h > hoja.cuerpoAlto + 0.01f) {
                out += actual
                actual = mutableListOf()
                alto = 0f
            }
            actual += grupo
            alto += h
        }
        if (actual.isNotEmpty() || out.isEmpty()) out += actual
        return out
    }

    /* ---------- dibujo ---------- */

    private fun dibujarHoja(c: Canvas, hoja: Hoja, trazos: List<Trazo>, num: Int, total: Int) {
        c.drawColor(Tinta.PAPEL)
        dibujarAgua(c, hoja)
        dibujarCabecera(c, hoja)
        dibujarCuerpo(c, hoja, trazos)
        dibujarPie(c, hoja, num, total)
    }

    /**
     * La marca cruzando la hoja de la esquina inferior izquierda a la superior
     * derecha, a opacidad de filigrana. Se pinta la primera: todo lo demás va
     * encima.
     */
    private fun dibujarAgua(c: Canvas, hoja: Hoja) {
        val rotulo = "Accordio"
        val anchoTexto = tipos.marca.measureText(rotulo)
        val ancho = Medida.marcaLogoAncho + Medida.marcaHueco + anchoTexto
        val capa = c.saveLayerAlpha(0f, 0f, hoja.ancho, hoja.alto, Medida.MARCA_ALFA)
        c.translate(hoja.ancho / 2f, hoja.alto / 2f)
        c.rotate(-45f)
        val x = -ancho / 2f
        dibujarMastil(c, x, -Medida.marcaLogoAlto / 2f, Medida.marcaLogoAncho, Medida.marcaLogoAlto)
        val fm = tipos.marca.fontMetrics
        c.drawText(
            rotulo,
            x + Medida.marcaLogoAncho + Medida.marcaHueco,
            -(fm.ascent + fm.descent) / 2f,
            tipos.marca
        )
        c.restoreToCount(capa)
    }

    private fun dibujarCabecera(c: Canvas, hoja: Hoja) {
        val centro = hoja.cabArriba + tipos.altoCabecera / 2f
        dibujarMastil(c, hoja.izq, centro - Medida.logoAlto / 2f, Medida.logoAncho, Medida.logoAlto)

        val etiquetas = mutableListOf<Pair<String, Boolean>>()
        if (song.capo > 0) etiquetas += "Capo ${song.capo}" to false
        if (semitones != 0) etiquetas += "Tono ${if (semitones > 0) "+" else ""}$semitones" to true

        var anchoEtqs = 0f
        etiquetas.forEachIndexed { i, (texto, _) ->
            anchoEtqs += tipos.etiqueta.measureText(texto.uppercase()) + 2 * Medida.etqPadX
            if (i > 0) anchoEtqs += Medida.etqHueco
        }

        val autor = song.artist.trim()
        val anchoAutor =
            if (autor.isEmpty()) 0f else tipos.cabAutor.measureText(autor) + Medida.cabHueco
        var x = hoja.izq + Medida.logoAncho + Medida.cabHueco
        val libre = hoja.der - x - anchoAutor -
            (if (anchoEtqs > 0f) anchoEtqs + Medida.cabHueco else 0f)

        val titulo = recortar(tipos.cabTitulo, song.title, libre)
        c.drawText(titulo, x, base(centro, tipos.cabTitulo), tipos.cabTitulo)
        if (autor.isNotEmpty()) {
            x += tipos.cabTitulo.measureText(titulo) + Medida.cabHueco
            c.drawText(autor, x, base(centro, tipos.cabAutor), tipos.cabAutor)
        }

        var ex = hoja.der - anchoEtqs
        etiquetas.forEach { (texto, tono) ->
            ex += dibujarEtiqueta(c, texto.uppercase(), ex, centro, tono) + Medida.etqHueco
        }

        val raya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Tinta.TEAL
            strokeWidth = Medida.cabBorde
        }
        val y = hoja.cabRaya + Medida.cabBorde / 2f
        c.drawLine(hoja.izq, y, hoja.der, y, raya)
    }

    /** Devuelve el ancho de la píldora dibujada. */
    private fun dibujarEtiqueta(
        c: Canvas,
        texto: String,
        x: Float,
        centro: Float,
        tono: Boolean
    ): Float {
        val ancho = tipos.etiqueta.measureText(texto) + 2 * Medida.etqPadX
        val alto = tipos.altoEtiqueta
        val caja = RectF(x, centro - alto / 2f, x + ancho, centro + alto / 2f)
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (tono) Tinta.TONO_FONDO else Tinta.CAPO_FONDO
        }
        c.drawRoundRect(caja, alto / 2f, alto / 2f, fondo)
        tipos.etiqueta.color = if (tono) Tinta.TONO_TEXTO else Tinta.CAPO_TEXTO
        c.drawText(texto, x + Medida.etqPadX, base(centro, tipos.etiqueta), tipos.etiqueta)
        return ancho
    }

    private fun dibujarCuerpo(c: Canvas, hoja: Hoja, trazos: List<Trazo>) {
        // Como en la hoja web, lo que se sale del folio se recorta.
        c.save()
        c.clipRect(hoja.izq, hoja.cuerpoArriba, hoja.der, hoja.cuerpoAbajo)
        var y = hoja.cuerpoArriba
        trazos.forEach { trazo ->
            when (trazo) {
                is Trazo.Linea -> dibujarLinea(c, hoja, trazo, y)
                is Trazo.Fuente -> dibujarFuente(c, hoja, trazo, y)
            }
            y += trazo.alto(tipos)
        }
        c.restore()
    }

    private fun dibujarLinea(c: Canvas, hoja: Hoja, linea: Trazo.Linea, y: Float) {
        var x = hoja.izq
        val base = y + tipos.baseLinea
        linea.tramos.forEach { tramo ->
            if (tramo.texto.isEmpty()) return@forEach
            val p = when {
                tramo.acorde -> tipos.acorde
                linea.tab -> tipos.tab
                else -> tipos.letra
            }
            c.drawText(tramo.texto, x, base, p)
            x += p.measureText(tramo.texto)
        }
    }

    private fun dibujarFuente(c: Canvas, hoja: Hoja, trazo: Trazo.Fuente, y: Float) {
        val raya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Tinta.LINEA
            strokeWidth = Medida.pieBorde
        }
        val ry = y + Medida.fuenteMargen + Medida.pieBorde / 2f
        c.drawLine(hoja.izq, ry, hoja.der, ry, raya)
        val base = ry + Medida.pieBorde / 2f + Medida.fuentePad - tipos.fuente.fontMetrics.ascent
        c.drawText(
            recortar(tipos.fuente, trazo.texto, hoja.der - hoja.izq),
            hoja.izq,
            base,
            tipos.fuente
        )
    }

    private fun dibujarPie(c: Canvas, hoja: Hoja, num: Int, total: Int) {
        val raya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Tinta.LINEA
            strokeWidth = Medida.pieBorde
        }
        val ry = hoja.pieRaya + Medida.pieBorde / 2f
        c.drawLine(hoja.izq, ry, hoja.der, ry, raya)
        val base = hoja.pieTexto - tipos.pie.fontMetrics.ascent
        c.drawText("accordio.site", hoja.izq, base, tipos.pie)
        val paginas = "Página $num de $total"
        c.drawText(paginas, hoja.der - tipos.pie.measureText(paginas), base, tipos.pie)
    }

    /**
     * La marca del mástil del kit (logo/mark-fretboard.svg), la misma que va en
     * la cabecera de la hoja web: cinco cuerdas, cinco trastes y los puntos. El
     * SVG tiene viewBox 132×92 y se escala sin deformarse, como haría el
     * navegador.
     */
    private fun dibujarMastil(c: Canvas, x: Float, y: Float, ancho: Float, alto: Float) {
        val escala = min(ancho / 132f, alto / 92f)
        c.save()
        c.translate(x + (ancho - 132f * escala) / 2f, y + (alto - 92f * escala) / 2f)
        c.scale(escala, escala)
        c.clipRect(0f, 0f, 132f, 92f)
        val cuerda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.SQUARE
            color = Tinta.TEAL
        }
        for (fy in floatArrayOf(10f, 26f, 42f, 58f, 74f)) c.drawLine(8f, fy, 124f, fy, cuerda)
        for (fx in floatArrayOf(8f, 37f, 66f, 95f, 124f)) c.drawLine(fx, 10f, fx, 74f, cuerda)
        val punto = Paint(Paint.ANTI_ALIAS_FLAG)
        val puntos = arrayOf(
            Triple(66f, 10f, Tinta.TEAL),
            Triple(37f, 26f, Tinta.TEAL),
            Triple(95f, 26f, Tinta.CORAL),
            Triple(8f, 42f, Tinta.TEAL),
            Triple(80f, 42f, Tinta.CORAL),
            Triple(80f, 58f, Tinta.CORAL),
            Triple(51f, 74f, Tinta.CORAL)
        )
        puntos.forEach { (cx, cy, color) ->
            punto.color = color
            c.drawCircle(cx, cy, 8.5f, punto)
        }
        c.restore()
    }

    /** La base del texto para que la caja quede centrada en [centro]. */
    private fun base(centro: Float, p: Paint): Float {
        val fm = p.fontMetrics
        return centro - (fm.ascent + fm.descent) / 2f
    }

    /** Lo que no cabe se corta con puntos suspensivos, como el `text-overflow`. */
    private fun recortar(p: Paint, texto: String, ancho: Float): String {
        if (ancho <= 0f) return ""
        if (p.measureText(texto) <= ancho) return texto
        var corte = texto.length
        while (corte > 0 && p.measureText(texto.substring(0, corte) + "…") > ancho) corte--
        return texto.substring(0, corte) + "…"
    }
}
