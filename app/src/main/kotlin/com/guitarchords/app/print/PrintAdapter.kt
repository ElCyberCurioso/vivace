package com.guitarchords.app.print

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.guitarchords.app.chords.Chord
import com.guitarchords.app.chords.ChordLibrary
import com.guitarchords.app.chords.ChordParser
import com.guitarchords.app.chords.ChordShape
import com.guitarchords.app.chords.RenderedLine
import com.guitarchords.app.data.Song
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object PrintAdapter {
    fun print(context: Context, song: Song) {
        val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        pm.print(
            "GuitarChords: ${song.title}",
            SongPrintAdapter(context, song),
            null
        )
    }
}

private class SongPrintAdapter(
    private val context: Context,
    private val song: Song
) : PrintDocumentAdapter() {

    private var pdf: PrintedPdfDocument? = null
    private var pages: Int = 1
    private var attrs: PrintAttributes? = null
    private val lines: List<RenderedLine> = ChordParser.parse(song.content)
    private val uniqueChords: List<Chord> = ChordParser.uniqueChords(song.content)
        .mapNotNull { ChordLibrary.find(it) }

    override fun onLayout(
        old: PrintAttributes?,
        new: PrintAttributes,
        cancel: CancellationSignal,
        cb: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancel.isCanceled) { cb.onLayoutCancelled(); return }
        attrs = new
        pdf = PrintedPdfDocument(context, new)
        pages = estimatePages()
        val info = PrintDocumentInfo.Builder("${song.title}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pages)
            .build()
        cb.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pageRanges: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancel: CancellationSignal,
        cb: WriteResultCallback
    ) {
        val doc = pdf ?: run {
            cb.onWriteFailed("pdf null"); return
        }
        try {
            renderAll(doc)
            FileOutputStream(destination.fileDescriptor).use { out ->
                doc.writeTo(out)
            }
            cb.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            cb.onWriteFailed(e.message)
        } finally {
            doc.close()
            pdf = null
        }
    }

    private fun pageGeometry(page: PdfDocument.Page): Geom {
        val pageInfo = page.info
        val width = pageInfo.pageWidth.toFloat()
        val height = pageInfo.pageHeight.toFloat()
        val margin = 36f
        return Geom(width, height, margin)
    }

    private data class Geom(val width: Float, val height: Float, val margin: Float) {
        val contentW = width - 2 * margin
        val contentH = height - 2 * margin
    }

    private fun diagramsBlockHeight(geom: Geom): Float {
        if (uniqueChords.isEmpty()) return 0f
        val perRow = max(3, (geom.contentW / 90f).toInt())
        val rows = (uniqueChords.size + perRow - 1) / perRow
        return rows * 130f + 30f
    }

    private fun lineStep(paint: Paint) = paint.fontSpacing * 1.1f

    private fun estimatePages(): Int {
        val geom = estGeom()
        val bodyPaint = Paint().apply { textSize = 11f; typeface = Typeface.MONOSPACE }
        val step = lineStep(bodyPaint)
        val firstTop = geom.margin + 30f + diagramsBlockHeight(geom) + 30f
        val firstAvail = geom.height - geom.margin - firstTop
        val restAvail = geom.contentH
        val perLine = 2 * step + 6f
        var remainingLines = lines.size
        val firstFits = (firstAvail / perLine).toInt()
        remainingLines -= firstFits
        if (remainingLines <= 0) return 1
        val perPage = max(1, (restAvail / perLine).toInt())
        return 1 + ((remainingLines + perPage - 1) / perPage)
    }

    private fun estGeom(): Geom {
        val a = attrs ?: return Geom(595f, 842f, 36f)
        val ws = a.mediaSize ?: PrintAttributes.MediaSize.ISO_A4
        val res = a.resolution ?: return Geom(595f, 842f, 36f)
        val widthPt = ws.widthMils * res.horizontalDpi / 1000f
        val heightPt = ws.heightMils * res.verticalDpi / 1000f
        return Geom(widthPt, heightPt, 36f)
    }

    private fun renderAll(doc: PrintedPdfDocument) {
        val headerPaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subPaint = Paint().apply { textSize = 12f; isAntiAlias = true }
        val bodyPaint = Paint().apply {
            textSize = 11f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        val chordPaint = Paint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            color = android.graphics.Color.rgb(120, 40, 20)
            isAntiAlias = true
        }

        var page = doc.startPage(0)
        var geom = pageGeometry(page)
        var canvas = page.canvas
        var y = geom.margin + 20f

        canvas.drawText(song.title, geom.margin, y, headerPaint)
        y += 6f
        if (song.artist.isNotBlank()) {
            y += subPaint.fontSpacing
            canvas.drawText(song.artist, geom.margin, y, subPaint)
        }
        if (song.capo > 0) {
            y += subPaint.fontSpacing
            canvas.drawText("Capo: traste ${song.capo}", geom.margin, y, subPaint)
        }
        y += 18f

        if (uniqueChords.isNotEmpty()) {
            y = drawDiagrams(canvas, geom, y)
            y += 16f
        }

        val step = lineStep(bodyPaint)
        val perLine = 2 * step + 6f
        var pageIndex = 0
        for (line in lines) {
            if (y + perLine > geom.height - geom.margin) {
                doc.finishPage(page)
                pageIndex++
                page = doc.startPage(pageIndex)
                geom = pageGeometry(page)
                canvas = page.canvas
                y = geom.margin + step
            }
            if (line.chords.isNotEmpty()) {
                for (ct in line.chords) {
                    val x = geom.margin + ct.position * charWidth(chordPaint)
                    canvas.drawText(ct.chord, x, y, chordPaint)
                }
                y += step
            }
            if (line.lyric.isNotBlank()) {
                canvas.drawText(line.lyric, geom.margin, y, bodyPaint)
                y += step
            } else if (line.chords.isEmpty()) {
                y += step
            }
            y += 4f
        }
        doc.finishPage(page)
    }

    private fun charWidth(paint: Paint): Float = paint.measureText("M")

    private fun drawDiagrams(canvas: Canvas, geom: Geom, startY: Float): Float {
        val perRow = max(3, (geom.contentW / 90f).toInt())
        val colW = geom.contentW / perRow
        val diagH = 120f
        var row = 0
        var col = 0
        uniqueChords.forEach { chord ->
            val shape = chord.variations.firstOrNull() ?: return@forEach
            val x = geom.margin + col * colW
            val y = startY + row * (diagH + 10f)
            drawChordMini(canvas, chord.name, shape, x, y, colW - 8f, diagH)
            col++
            if (col >= perRow) { col = 0; row++ }
        }
        val rows = row + if (col > 0) 1 else 0
        return startY + rows * (diagH + 10f)
    }

    private fun drawChordMini(
        canvas: Canvas,
        name: String,
        shape: ChordShape,
        x: Float, y: Float, w: Float, h: Float
    ) {
        val namePaint = Paint().apply { textSize = 12f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText(name, x + w / 2f - namePaint.measureText(name) / 2f, y + 12f, namePaint)

        val dotPaint = Paint().apply { color = android.graphics.Color.BLACK; isAntiAlias = true }
        val line = Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }
        val boxTop = y + 24f
        val boxLeft = x + 8f
        val boxRight = x + w - 8f
        val boxW = boxRight - boxLeft
        val strings = 6
        val fretsCount = 5
        val colStep = boxW / (strings - 1)
        val rowStep = (h - 32f) / fretsCount
        val baseFret = shape.baseFret
        val nutStroke = if (baseFret == 1) 3f else 1.2f

        for (i in 0 until strings) {
            val sx = boxLeft + i * colStep
            canvas.drawLine(sx, boxTop, sx, boxTop + fretsCount * rowStep, line)
        }
        for (r in 0..fretsCount) {
            val ry = boxTop + r * rowStep
            val p = Paint(line).apply { strokeWidth = if (r == 0) nutStroke else 1.2f }
            canvas.drawLine(boxLeft, ry, boxRight, ry, p)
        }
        if (baseFret > 1) {
            val fretText = "${baseFret}fr"
            val p = Paint().apply { textSize = 9f; isAntiAlias = true }
            canvas.drawText(fretText, boxLeft - 18f, boxTop + rowStep * 0.65f, p)
        }

        val mark = Paint().apply { textSize = 9f; isAntiAlias = true }
        for (s in 0 until strings) {
            val sx = boxLeft + s * colStep
            val fret = shape.frets[s]
            when {
                fret == -1 -> canvas.drawText("×", sx - 2.5f, boxTop - 3f, mark)
                fret == 0 -> canvas.drawText("○", sx - 2.5f, boxTop - 3f, mark)
            }
        }

        shape.barres.forEach { b ->
            val relFret = b.fret - baseFret + 1
            if (relFret !in 1..fretsCount) return@forEach
            val sLo = strings - max(b.fromString, b.toString)
            val sHi = strings - min(b.fromString, b.toString)
            val by = boxTop + (relFret - 0.5f) * rowStep
            val bx1 = boxLeft + sLo * colStep - 4f
            val bx2 = boxLeft + sHi * colStep + 4f
            val rect = RectF(bx1, by - 4f, bx2, by + 4f)
            canvas.drawRoundRect(rect, 4f, 4f, dotPaint)
        }

        for (s in 0 until strings) {
            val fret = shape.frets[s]
            if (fret <= 0) continue
            val relFret = fret - baseFret + 1
            if (relFret !in 1..fretsCount) continue
            val sx = boxLeft + s * colStep
            val sy = boxTop + (relFret - 0.5f) * rowStep
            val onBarre = shape.barres.any { b ->
                b.fret == fret && (strings - s) in min(b.fromString, b.toString)..max(b.fromString, b.toString)
            }
            if (!onBarre) {
                canvas.drawCircle(sx, sy, 4f, dotPaint)
            }
        }
    }

    override fun onFinish() {
        pdf?.close()
        pdf = null
    }
}
