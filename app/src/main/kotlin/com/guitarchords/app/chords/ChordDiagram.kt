package com.guitarchords.app.chords

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChordDiagram(
    shape: ChordShape,
    name: String? = null,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    showFingers: Boolean = true
) {
    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!name.isNullOrBlank()) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Spacer(Modifier.height(4.dp))
        }
        Canvas(modifier = Modifier.size(width = 120.dp, height = 140.dp)) {
            drawDiagram(shape, color, showFingers)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiagram(
    shape: ChordShape,
    color: Color,
    showFingers: Boolean
) {
    val w = size.width
    val h = size.height
    val topMargin = h * 0.14f
    val sideMargin = w * 0.12f
    val usableW = w - 2 * sideMargin
    val usableH = h - topMargin - h * 0.08f

    val strings = 6
    val frets = 5
    val colStep = usableW / (strings - 1)
    val rowStep = usableH / frets
    val dotRadius = colStep * 0.32f
    val baseFret = shape.baseFret
    val nutStroke = if (baseFret == 1) 6f else 2f

    for (i in 0 until strings) {
        val x = sideMargin + i * colStep
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(x, topMargin),
            end = androidx.compose.ui.geometry.Offset(x, topMargin + usableH),
            strokeWidth = 2f
        )
    }
    for (r in 0..frets) {
        val y = topMargin + r * rowStep
        val stroke = if (r == 0) nutStroke else 2f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(sideMargin, y),
            end = androidx.compose.ui.geometry.Offset(sideMargin + usableW, y),
            strokeWidth = stroke
        )
    }

    if (baseFret > 1) {
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                this.color = color.toArgb()
                textSize = 24f
                isAntiAlias = true
            }
            drawText("${baseFret}fr", 2f, topMargin + rowStep * 0.6f, paint)
        }
    }

    val markerY = topMargin - 12f
    for (s in 0 until strings) {
        val x = sideMargin + s * colStep
        val fret = shape.frets[s]
        if (fret == -1) {
            drawLine(color, androidx.compose.ui.geometry.Offset(x - 6f, markerY - 6f),
                androidx.compose.ui.geometry.Offset(x + 6f, markerY + 6f), strokeWidth = 2f)
            drawLine(color, androidx.compose.ui.geometry.Offset(x - 6f, markerY + 6f),
                androidx.compose.ui.geometry.Offset(x + 6f, markerY - 6f), strokeWidth = 2f)
        } else if (fret == 0) {
            drawCircle(
                color = color,
                radius = 6f,
                center = androidx.compose.ui.geometry.Offset(x, markerY),
                style = Stroke(width = 2f)
            )
        }
    }

    shape.barres.forEach { barre ->
        val from = barre.fromString.coerceIn(1, 6)
        val to = barre.toString.coerceIn(1, 6)
        val s1 = strings - from
        val s2 = strings - to
        val sLo = minOf(s1, s2)
        val sHi = maxOf(s1, s2)
        val relFret = barre.fret - baseFret + 1
        if (relFret in 1..frets) {
            val y = topMargin + (relFret - 0.5f) * rowStep
            val xStart = sideMargin + sLo * colStep
            val xEnd = sideMargin + sHi * colStep
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(xStart - dotRadius * 0.9f, y - dotRadius * 0.7f),
                size = androidx.compose.ui.geometry.Size(xEnd - xStart + dotRadius * 1.8f, dotRadius * 1.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(dotRadius, dotRadius)
            )
        }
    }

    for (s in 0 until strings) {
        val fret = shape.frets[s]
        if (fret <= 0) continue
        val relFret = fret - baseFret + 1
        if (relFret !in 1..frets) continue
        val x = sideMargin + s * colStep
        val y = topMargin + (relFret - 0.5f) * rowStep
        val skipDot = shape.barres.any { b ->
            b.fret == fret && (strings - s) in minOf(b.fromString, b.toString)..maxOf(b.fromString, b.toString)
        }
        if (!skipDot) {
            drawCircle(
                color = color,
                radius = dotRadius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
        if (showFingers) {
            val finger = shape.fingers.getOrNull(s) ?: 0
            if (finger in 1..4) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize = dotRadius * 1.4f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText(finger.toString(), x, y + dotRadius * 0.5f, paint)
                }
            }
        }
    }
}

