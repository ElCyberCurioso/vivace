package com.guitarchords.app.ui.song

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.chords.ChordParser
import com.guitarchords.app.chords.ChordTransposer
import com.guitarchords.app.chords.ContentBlock
import com.guitarchords.app.chords.RenderedLine
import com.guitarchords.app.print.PrintAdapter
import com.guitarchords.app.ui.components.ChordModal
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongViewScreen(
    songId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    vm: SongViewModel = viewModel()
) {
    LaunchedEffect(songId) { vm.load(songId) }
    val song by vm.song.collectAsStateWithLifecycle()

    var fontSize by remember { mutableIntStateOf(18) }
    var scrolling by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(30f) } // pixels per second
    var selectedChord by remember { mutableStateOf<String?>(null) }
    var semitones by remember { mutableIntStateOf(0) }
    var controlsExpanded by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val ctx = LocalContext.current

    LaunchedEffect(scrolling, speed) {
        if (!scrolling) return@LaunchedEffect
        val stepMs = 16L
        while (scrolling) {
            val delta = (speed * stepMs / 1000f)
            val max = scrollState.maxValue
            if (scrollState.value >= max) { scrolling = false; break }
            scrollState.scrollBy(delta)
            delay(stepMs)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(song?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") }
                },
                actions = {
                    IconButton(onClick = {
                        song?.let {
                            val shifted = if (semitones == 0) it
                            else it.copy(content = ChordTransposer.transposeContent(it.content, semitones))
                            PrintAdapter.print(ctx, shifted)
                        }
                    }) { Icon(Icons.Default.Print, "Imprimir") }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar") }
                }
            )
        },
        bottomBar = {
            BottomControls(
                expanded = controlsExpanded,
                onToggleExpanded = { controlsExpanded = !controlsExpanded },
                fontSize = fontSize,
                onFontSize = { fontSize = it.coerceIn(10, 48) },
                scrolling = scrolling,
                onToggleScroll = { scrolling = !scrolling },
                speed = speed,
                onSpeed = { speed = it },
                semitones = semitones,
                onSemitones = { semitones = it.coerceIn(-11, 11) }
            )
        }
    ) { pv ->
        song?.let { s ->
            val blocks = remember(s.content) { ChordParser.parseBlocks(s.content) }
            val rendered = remember(blocks, semitones) {
                if (semitones == 0) blocks
                else blocks.map { b ->
                    when (b) {
                        is ContentBlock.Lyric -> b.copy(
                            line = b.line.copy(
                                chords = b.line.chords.map { ct ->
                                    ct.copy(chord = ChordTransposer.transposeChord(ct.chord, semitones))
                                }
                            )
                        )
                        is ContentBlock.Tab -> b
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(scrollState)
            ) {
                if (s.artist.isNotBlank()) {
                    Text(s.artist, style = MaterialTheme.typography.bodyMedium)
                }
                if (s.capo > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            "Capo en traste ${s.capo}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                if (s.artist.isNotBlank() || s.capo > 0) {
                    Spacer(Modifier.height(8.dp))
                }
                rendered.forEach { block ->
                    when (block) {
                        is ContentBlock.Lyric -> SongLine(
                            line = block.line,
                            fontSize = fontSize,
                            chordColor = MaterialTheme.colorScheme.primary,
                            onChordClick = { selectedChord = it }
                        )
                        is ContentBlock.Tab -> TabBlock(
                            rows = block.rows,
                            fontSize = fontSize
                        )
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        } ?: Box(Modifier.fillMaxSize().padding(pv), contentAlignment = Alignment.Center) {
            Text("Cargando…")
        }
    }

    selectedChord?.let {
        ChordModal(chordName = it, onDismiss = { selectedChord = null })
    }
}

private suspend fun ScrollState.scrollBy(delta: Float) {
    val next = (value + delta).toInt().coerceIn(0, maxValue)
    scrollTo(next)
}

@Composable
private fun SongLine(
    line: RenderedLine,
    fontSize: Int,
    chordColor: Color,
    onChordClick: (String) -> Unit
) {
    if (line.isEmpty) {
        Spacer(Modifier.height((fontSize * 0.8).dp))
        return
    }
    val baseStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp
    )
    if (line.chords.isNotEmpty()) {
        val chordAnnotated = buildChordLine(line, chordColor)
        ClickableText(
            text = chordAnnotated,
            style = baseStyle.copy(fontWeight = FontWeight.Bold, color = chordColor),
            onClick = { offset ->
                chordAnnotated.getStringAnnotations("chord", offset, offset)
                    .firstOrNull()?.let { onChordClick(it.item) }
            }
        )
    }
    if (line.lyric.isNotBlank()) {
        Text(text = line.lyric, style = baseStyle)
    } else if (line.chords.isNotEmpty()) {
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun TabBlock(rows: List<String>, fontSize: Int) {
    val scroll = rememberScrollState()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(scroll)
                .padding(8.dp)
        ) {
            rows.forEach { row ->
                Text(
                    text = row,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (fontSize - 2).coerceAtLeast(10).sp
                    )
                )
            }
        }
    }
}

private fun buildChordLine(line: RenderedLine, color: Color): AnnotatedString = buildAnnotatedString {
    var col = 0
    for (ct in line.chords) {
        val pad = (ct.position - col).coerceAtLeast(if (col == 0) 0 else 1)
        append(" ".repeat(pad))
        val start = length
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
            append(ct.chord)
        }
        addStringAnnotation("chord", ct.chord, start, start + ct.chord.length)
        col = ct.position + ct.chord.length
    }
}

private fun semitoneLabel(n: Int): String {
    val sign = when {
        n > 0 -> "+"
        n < 0 -> ""
        else -> "±"
    }
    return "$sign$n"
}

@Composable
private fun BottomControls(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    fontSize: Int,
    onFontSize: (Int) -> Unit,
    scrolling: Boolean,
    onToggleScroll: () -> Unit,
    speed: Float,
    onSpeed: (Float) -> Unit,
    semitones: Int,
    onSemitones: (Int) -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onToggleScroll) {
                    Icon(
                        if (scrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (scrolling) "Pausa" else "Reproducir"
                    )
                }
                if (!expanded) {
                    Text("${speed.toInt()} px/s", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.size(12.dp))
                    Text("Tono ${semitoneLabel(semitones)}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        if (expanded) "Ocultar controles" else "Mostrar controles"
                    )
                }
            }
            if (expanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = fontSize.toFloat(),
                        onValueChange = { onFontSize(it.toInt()) },
                        valueRange = 10f..48f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("A", style = MaterialTheme.typography.titleLarge)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Vel", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = speed,
                        onValueChange = onSpeed,
                        valueRange = 5f..800f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("${speed.toInt()} px/s", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text("Tono", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { onSemitones(semitones - 1) }) {
                        Icon(Icons.Default.Remove, "Bajar semitono")
                    }
                    Text(
                        semitoneLabel(semitones),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { onSemitones(semitones + 1) }) {
                        Icon(Icons.Default.Add, "Subir semitono")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { onSemitones(0) },
                        enabled = semitones != 0
                    ) { Text("Reset") }
                }
            }
        }
    }
}
