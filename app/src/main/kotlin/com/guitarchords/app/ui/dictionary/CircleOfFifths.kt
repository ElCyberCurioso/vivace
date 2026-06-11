package com.guitarchords.app.ui.dictionary

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import kotlin.math.atan2
import kotlin.math.hypot

private val MAJORS = listOf("C","G","D","A","E","B","F#","Db","Ab","Eb","Bb","F")
private val MINORS = listOf("Am","Em","Bm","F#m","C#m","G#m","Ebm","Bbm","Fm","Cm","Gm","Dm")
// Diminished chord (vii° in major / ii° in minor) for tonic at each index:
private val DIM_BY_TONIC = listOf("Bdim","F#dim","C#dim","G#dim","D#dim","A#dim","Fdim","Cdim","Gdim","Ddim","Adim","Edim")

private data class Family(
    val tonic: String,
    val pairs: List<Pair<String, String>> // (roman, chord)
)

private fun family(idx: Int, major: Boolean): Family {
    val left = (idx + 11) % 12
    val right = (idx + 1) % 12
    val dim = DIM_BY_TONIC[idx]
    return if (major) Family(
        tonic = MAJORS[idx],
        pairs = listOf(
            "I" to MAJORS[idx],
            "ii" to MINORS[left],
            "iii" to MINORS[right],
            "IV" to MAJORS[left],
            "V" to MAJORS[right],
            "vi" to MINORS[idx],
            "vii°" to dim
        )
    ) else Family(
        tonic = MINORS[idx],
        pairs = listOf(
            "i" to MINORS[idx],
            "ii°" to dim,
            "III" to MAJORS[idx],
            "iv" to MINORS[left],
            "v" to MINORS[right],
            "VI" to MAJORS[left],
            "VII" to MAJORS[right]
        )
    )
}

@Composable
fun CircleOfFifthsView(
    modifier: Modifier = Modifier,
    onChordClick: (String) -> Unit
) {
    var tonicIdx by remember { mutableIntStateOf(0) }
    var tonicMajor by remember { mutableStateOf(true) }

    val fam = remember(tonicIdx, tonicMajor) { family(tonicIdx, tonicMajor) }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiaryContainer
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVar = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val outline = MaterialTheme.colorScheme.outline

    val leftIdx = (tonicIdx + 11) % 12
    val rightIdx = (tonicIdx + 1) % 12

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            FilterChip(
                selected = tonicMajor,
                onClick = { tonicMajor = true },
                label = { Text(stringResource(R.string.major_label)) }
            )
            FilterChip(
                selected = !tonicMajor,
                onClick = { tonicMajor = false },
                label = { Text(stringResource(R.string.minor_label)) }
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        val r = hypot(dx, dy)
                        val rOuter = (minOf(size.width, size.height) / 2f) - 4f
                        val rMid = rOuter * 0.62f
                        val rInner = rOuter * 0.30f
                        if (r > rOuter || r < rInner) return@detectTapGestures
                        // angle from top, clockwise, in degrees
                        val rawDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        val fromTop = ((rawDeg + 90f + 360f) % 360f)
                        val sector = ((fromTop + 15f) / 30f).toInt() % 12
                        tonicIdx = sector
                        tonicMajor = r >= rMid
                    }
                }
        ) {
            val w = size.minDimension
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rOuter = w / 2f - 4f
            val rMid = rOuter * 0.62f
            val rInner = rOuter * 0.30f

            val outerMid = (rOuter + rMid) / 2f
            val innerMid = (rMid + rInner) / 2f
            val outerStroke = rOuter - rMid
            val innerStroke = rMid - rInner

            val hl = setOf(tonicIdx, leftIdx, rightIdx)
            val tonicColor = primary
            val neighborColorOuter = secondary
            val neighborColorInner = tertiary

            for (i in 0 until 12) {
                val startAngle = -90f - 15f + 30f * i
                val isTonicOuter = tonicMajor && i == tonicIdx
                val isTonicInner = !tonicMajor && i == tonicIdx
                val outerFill = when {
                    isTonicOuter -> tonicColor
                    i in hl -> neighborColorOuter
                    else -> surfaceVar
                }
                val innerFill = when {
                    isTonicInner -> tonicColor
                    i in hl -> neighborColorInner
                    else -> surface
                }
                drawArc(
                    color = outerFill,
                    startAngle = startAngle,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(cx - outerMid, cy - outerMid),
                    size = Size(outerMid * 2, outerMid * 2),
                    style = Stroke(width = outerStroke)
                )
                drawArc(
                    color = innerFill,
                    startAngle = startAngle,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(cx - innerMid, cy - innerMid),
                    size = Size(innerMid * 2, innerMid * 2),
                    style = Stroke(width = innerStroke)
                )
            }

            // Dividers + ring outlines
            for (i in 0 until 12) {
                val angDeg = -90f + 30f * i - 15f
                val angRad = Math.toRadians(angDeg.toDouble())
                val x1 = cx + rInner * kotlin.math.cos(angRad).toFloat()
                val y1 = cy + rInner * kotlin.math.sin(angRad).toFloat()
                val x2 = cx + rOuter * kotlin.math.cos(angRad).toFloat()
                val y2 = cy + rOuter * kotlin.math.sin(angRad).toFloat()
                drawLine(outline, Offset(x1, y1), Offset(x2, y2), strokeWidth = 1.5f)
            }
            drawCircle(outline, rOuter, Offset(cx, cy), style = Stroke(width = 1.5f))
            drawCircle(outline, rMid, Offset(cx, cy), style = Stroke(width = 1.5f))
            drawCircle(outline, rInner, Offset(cx, cy), style = Stroke(width = 1.5f))

            // Labels
            drawContext.canvas.nativeCanvas.apply {
                val outerPaint = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    textSize = outerStroke * 0.35f
                }
                val innerPaint = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT
                    textSize = innerStroke * 0.32f
                }
                for (i in 0 until 12) {
                    val angDeg = -90f + 30f * i
                    val angRad = Math.toRadians(angDeg.toDouble())
                    val ox = cx + outerMid * kotlin.math.cos(angRad).toFloat()
                    val oy = cy + outerMid * kotlin.math.sin(angRad).toFloat()
                    val ix = cx + innerMid * kotlin.math.cos(angRad).toFloat()
                    val iy = cy + innerMid * kotlin.math.sin(angRad).toFloat()
                    val outerTonic = tonicMajor && i == tonicIdx
                    val innerTonic = !tonicMajor && i == tonicIdx
                    outerPaint.color = if (outerTonic) onPrimary.toArgb() else onSurface.toArgb()
                    innerPaint.color = if (innerTonic) onPrimary.toArgb() else onSurface.toArgb()
                    val outerBaseline = oy - (outerPaint.ascent() + outerPaint.descent()) / 2f
                    val innerBaseline = iy - (innerPaint.ascent() + innerPaint.descent()) / 2f
                    drawText(MAJORS[i], ox, outerBaseline, outerPaint)
                    drawText(MINORS[i], ix, innerBaseline, innerPaint)
                }

                // Center label
                val centerPaint = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    textSize = rInner * 0.55f
                    color = onSurface.toArgb()
                }
                val centerBaseline = cy - (centerPaint.ascent() + centerPaint.descent()) / 2f
                drawText(fam.tonic, cx, centerBaseline, centerPaint)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Tónica: ${fam.tonic} — toca un acorde",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Toca el círculo para cambiar de tono. Anillo exterior = mayores, interior = menores.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            items(fam.pairs) { (roman, chord) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChordClick(chord) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(roman, style = MaterialTheme.typography.labelMedium)
                        Text(
                            chord,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
