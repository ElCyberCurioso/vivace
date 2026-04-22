package com.guitarchords.app.ui.finder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.guitarchords.app.chords.ChordLibrary
import com.guitarchords.app.chords.ChordRecognizer
import com.guitarchords.app.ui.components.ChordModal
import kotlin.math.floor
import kotlin.math.roundToInt

private const val VISIBLE_FRETS = 5
private const val STRINGS = 6
private val OPEN_MIDI = intArrayOf(40, 45, 50, 55, 59, 64)

private fun noteNameFor(stringIdx: Int, fret: Int): String =
    ChordLibrary.ROOTS[(OPEN_MIDI[stringIdx] + fret) % 12]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordFinderScreen(onBack: () -> Unit) {
    val frets = remember { mutableStateListOf(0, 0, 0, 0, 0, 0) }
    var baseFret by remember { mutableIntStateOf(1) }
    var modalChord by remember { mutableStateOf<String?>(null) }

    val matches = remember(frets.toList()) { ChordRecognizer.identify(frets.toList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscador de acordes") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") }
                },
                actions = {
                    IconButton(onClick = {
                        for (i in 0 until STRINGS) frets[i] = 0
                        baseFret = 1
                    }) { Icon(Icons.Default.Refresh, "Limpiar") }
                }
            )
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StringStateRow(frets) { i, v -> frets[i] = v }
            Spacer(Modifier.height(8.dp))
            Fretboard(
                frets = frets.toList(),
                baseFret = baseFret,
                onTapFret = { stringIdx, fret ->
                    frets[stringIdx] = if (frets[stringIdx] == fret) 0 else fret
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(6f / 7f)
            )
            Spacer(Modifier.height(8.dp))
            BaseFretControls(baseFret) { baseFret = it }
            Spacer(Modifier.height(12.dp))
            Text(
                "Acordes coincidentes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            if (matches.isEmpty()) {
                Text(
                    "Marca trastes para identificar un acorde",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                matches.forEach { m ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    m.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (!m.bassIsRoot) {
                                    Text(
                                        "bajo: ${m.slashBass}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            OutlinedButton(onClick = { modalChord = m.root + m.quality }) {
                                Text("Ver")
                            }
                        }
                    }
                }
            }
        }
    }

    modalChord?.let {
        ChordModal(chordName = it, onDismiss = { modalChord = null })
    }
}

@Composable
private fun StringStateRow(
    frets: List<Int>,
    onSet: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0 until STRINGS) {
            val v = frets[i]
            val label = when {
                v == -1 -> "X"
                v == 0 -> "O"
                else -> v.toString()
            }
            val muted = v == -1
            OutlinedButton(
                onClick = {
                    onSet(
                        i,
                        when (v) {
                            0 -> -1
                            -1 -> 0
                            else -> 0
                        }
                    )
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (muted) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
            ) {
                Text(
                    label,
                    color = if (muted) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BaseFretControls(baseFret: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { if (baseFret > 1) onChange(baseFret - 1) },
            enabled = baseFret > 1
        ) { Icon(Icons.Default.Remove, "Menos traste") }
        Text(
            "Traste base: $baseFret",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        IconButton(
            onClick = { if (baseFret < 12) onChange(baseFret + 1) },
            enabled = baseFret < 12
        ) { Icon(Icons.Default.Add, "Más traste") }
    }
}

@Composable
private fun Fretboard(
    frets: List<Int>,
    baseFret: Int,
    onTapFret: (stringIdx: Int, fret: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.onSurface
    val dotColor = MaterialTheme.colorScheme.primary
    val dotTextColor = MaterialTheme.colorScheme.onPrimary
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onSizeChanged { boxSize = it }
            .pointerInput(baseFret, boxSize) {
                detectTapGestures { off ->
                    if (boxSize.width == 0 || boxSize.height == 0) return@detectTapGestures
                    val w = boxSize.width.toFloat()
                    val h = boxSize.height.toFloat()
                    val topMargin = h * 0.10f
                    val sideMargin = w * 0.10f
                    val usableW = w - 2 * sideMargin
                    val usableH = h - topMargin - h * 0.06f
                    val colStep = usableW / (STRINGS - 1)
                    val rowStep = usableH / VISIBLE_FRETS
                    if (off.y < topMargin) return@detectTapGestures
                    val row = floor((off.y - topMargin) / rowStep).toInt()
                    if (row !in 0 until VISIBLE_FRETS) return@detectTapGestures
                    val stringIdx = ((off.x - sideMargin) / colStep).roundToInt()
                    if (stringIdx !in 0 until STRINGS) return@detectTapGestures
                    onTapFret(stringIdx, baseFret + row)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val w = size.width
            val h = size.height
            val topMargin = h * 0.10f
            val sideMargin = w * 0.10f
            val usableW = w - 2 * sideMargin
            val usableH = h - topMargin - h * 0.06f
            val colStep = usableW / (STRINGS - 1)
            val rowStep = usableH / VISIBLE_FRETS
            val dotRadius = colStep * 0.30f
            val nutStroke = if (baseFret == 1) 8f else 3f

            for (i in 0 until STRINGS) {
                val x = sideMargin + i * colStep
                drawLine(color, Offset(x, topMargin), Offset(x, topMargin + usableH), 2f)
            }
            for (r in 0..VISIBLE_FRETS) {
                val y = topMargin + r * rowStep
                val s = if (r == 0) nutStroke else 2f
                drawLine(color, Offset(sideMargin, y), Offset(sideMargin + usableW, y), s)
            }

            if (baseFret > 1) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = color.toArgb()
                        textSize = rowStep * 0.5f
                        isAntiAlias = true
                    }
                    drawText("${baseFret}fr", 4f, topMargin + rowStep * 0.6f, paint)
                }
            }

            val markerY = topMargin - 14f
            for (s in 0 until STRINGS) {
                val x = sideMargin + s * colStep
                when (frets[s]) {
                    -1 -> {
                        drawLine(color, Offset(x - 10f, markerY - 10f), Offset(x + 10f, markerY + 10f), 3f)
                        drawLine(color, Offset(x - 10f, markerY + 10f), Offset(x + 10f, markerY - 10f), 3f)
                    }
                    0 -> drawCircle(color, 8f, Offset(x, markerY), style = Stroke(width = 3f))
                }
            }

            for (s in 0 until STRINGS) {
                val fret = frets[s]
                if (fret <= 0) continue
                val rel = fret - baseFret + 1
                if (rel !in 1..VISIBLE_FRETS) continue
                val x = sideMargin + s * colStep
                val y = topMargin + (rel - 0.5f) * rowStep
                drawCircle(dotColor, dotRadius, Offset(x, y))
                val note = noteNameFor(s, fret)
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = dotTextColor.toArgb()
                        textSize = dotRadius * if (note.length > 1) 0.85f else 1.05f
                        isAntiAlias = true
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val metrics = paint.fontMetrics
                    val yText = y - (metrics.ascent + metrics.descent) / 2f
                    drawText(note, x, yText, paint)
                }
            }
        }
    }
}
