package com.guitarchords.app.ui.song

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.chords.ChordParser
import com.guitarchords.app.chords.ChordTransposer
import com.guitarchords.app.chords.ContentBlock
import com.guitarchords.app.chords.RenderedLine
import com.guitarchords.app.data.SongVersion
import com.guitarchords.app.metronome.MetronomeEngine
import com.guitarchords.app.print.PrintAdapter
import com.guitarchords.app.sync.SongTextFormat
import com.guitarchords.app.ui.components.ChordModal
import com.guitarchords.app.ui.components.EmptyState
import com.guitarchords.app.ui.playlists.TextDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
import com.guitarchords.app.ui.theme.accordioTopBarColors
import com.guitarchords.app.ui.icons.AccordioIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongViewScreen(
    songId: Long,
    onEdit: () -> Unit,
    onEditVersion: (Long) -> Unit,
    onPractice: () -> Unit,
    onBack: () -> Unit,
    playlistId: Long = 0L,
    vm: SongViewModel = viewModel()
) {
    LaunchedEffect(songId) { vm.load(songId) }
    LaunchedEffect(playlistId) { vm.loadSiblings(playlistId) }
    val song by vm.song.collectAsStateWithLifecycle()
    val notFound by vm.notFound.collectAsStateWithLifecycle()
    val versions by vm.versions.collectAsStateWithLifecycle()
    // Modo concierto: encadenar las canciones de la carpeta sin volver al listado.
    val siblings by vm.siblings.collectAsStateWithLifecycle()
    val songIndex = siblings.indexOf(song?.id ?: -1L)
    val hasPrev = songIndex > 0
    val hasNext = songIndex >= 0 && songIndex < siblings.lastIndex

    var fontSize by remember { mutableIntStateOf(18) }
    var scrolling by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(30f) } // pixels per second
    var selectedChord by remember { mutableStateOf<String?>(null) }
    var semitones by remember { mutableIntStateOf(0) }
    var useFlats by remember { mutableStateOf(false) }
    var controlsExpanded by remember { mutableStateOf(true) }
    var selectedVersionId by remember { mutableStateOf<Long?>(null) }
    var showAddVersion by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val ctx = LocalContext.current
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val shareTitle = stringResource(R.string.share_song)

    // Metrónomo accesible desde el visor (lo más útil al tocar la canción).
    // El motor vive mientras se ve la canción; el panel se abre en una hoja.
    val metroEngine = remember { MetronomeEngine() }
    val metroScope = rememberCoroutineScope()
    var showMetronome by remember { mutableStateOf(false) }
    var metroBpm by remember { mutableIntStateOf(100) }
    var metroBeats by remember { mutableIntStateOf(4) }
    val metroRunning by metroEngine.running.collectAsStateWithLifecycle()
    val metroBeat by metroEngine.beat.collectAsStateWithLifecycle()
    val metroSheet = rememberModalBottomSheetState()
    DisposableEffect(Unit) { onDispose { metroEngine.release() } }

    // Modo escenario: con el auto-scroll activo la pantalla no se apaga.
    DisposableEffect(scrolling) {
        view.keepScreenOn = scrolling
        onDispose { view.keepScreenOn = false }
    }

    // Active version: null = the song's "Original" content/capo.
    val activeVersion = versions.firstOrNull { it.id == selectedVersionId }
    val activeContent = activeVersion?.content ?: song?.content ?: ""
    val activeCapo = activeVersion?.capo ?: song?.capo ?: 0
    // URL de origen: la de la versión activa, o la de la canción como fallback.
    val activeUrl = activeVersion?.sourceUrl?.takeIf { it.isNotBlank() }
        ?: song?.sourceUrl.orEmpty()

    // If the selected version was deleted, fall back to Original.
    LaunchedEffect(versions) {
        if (selectedVersionId != null && versions.none { it.id == selectedVersionId }) {
            selectedVersionId = null
        }
    }

    // Al saltar a otra canción del concierto: volver arriba, parar el
    // auto-scroll y quitar la versión seleccionada (era de la canción anterior).
    // El tamaño de letra, la velocidad y el tono se conservan a propósito.
    LaunchedEffect(song?.id) {
        selectedVersionId = null
        scrolling = false
        scrollState.scrollTo(0)
    }

    LaunchedEffect(scrolling, speed) {
        if (!scrolling) return@LaunchedEffect
        val stepMs = 16L
        var remainder = 0f   // carry sub-pixel movement so slow speeds still advance
        while (scrolling && isActive) {
            val max = scrollState.maxValue
            if (scrollState.value >= max) { scrolling = false; break }
            remainder += speed * stepMs / 1000f
            val step = remainder.toInt()
            if (step > 0) {
                try {
                    scrollState.scrollTo((scrollState.value + step).coerceIn(0, max))
                    remainder -= step
                } catch (e: CancellationException) {
                    // User grabbed the scroll and preempted us. If the effect itself
                    // is still active, just resume auto-scroll from wherever they left it.
                    if (!isActive) throw e
                    remainder = 0f
                }
            }
            delay(stepMs)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = accordioTopBarColors(),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = onPractice) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = stringResource(R.string.practice_changes_title)
                        )
                    }
                    IconButton(onClick = { showMetronome = true }) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = stringResource(R.string.metronome_title),
                            tint = if (metroRunning) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (song?.locked == true) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = stringResource(R.string.locked_song_banner),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    if (activeUrl.isNotBlank()) {
                        TextButton(onClick = {
                            val raw = activeUrl.trim()
                            val url = if (raw.startsWith("http://") || raw.startsWith("https://")) raw
                            else "https://$raw"
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }) {
                            Icon(Icons.Default.Link, null)
                            Spacer(Modifier.size(4.dp))
                            Text(stringResource(R.string.link))
                        }
                    }
                    IconButton(onClick = {
                        song?.let { sng ->
                            val body = SongTextFormat.encode(
                                sng.copy(
                                    content = if (semitones == 0 && !useFlats) activeContent
                                    else ChordTransposer.transposeContent(activeContent, semitones, useFlats),
                                    capo = activeCapo
                                )
                            )
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, sng.title)
                                putExtra(Intent.EXTRA_TEXT, body)
                            }
                            ctx.startActivity(Intent.createChooser(send, shareTitle))
                        }
                    }) { Icon(AccordioIcons.compartir(), stringResource(R.string.share_song)) }
                    IconButton(onClick = {
                        song?.let { sng ->
                            val base = sng.copy(content = activeContent, capo = activeCapo)
                            val shifted = if (semitones == 0 && !useFlats) base
                            else base.copy(
                                content = ChordTransposer.transposeContent(activeContent, semitones, useFlats)
                            )
                            PrintAdapter.print(ctx, shifted)
                        }
                    }) { Icon(Icons.Default.Print, stringResource(R.string.print)) }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
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
                onToggleScroll = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scrolling = !scrolling
                },
                speed = speed,
                onSpeed = { speed = it },
                semitones = semitones,
                onSemitones = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    semitones = it.coerceIn(-11, 11)
                },
                useFlats = useFlats,
                onUseFlats = { useFlats = it },
                hasPrev = hasPrev,
                hasNext = hasNext,
                onPrev = { vm.step(-1) },
                onNext = { vm.step(1) }
            )
        }
    ) { pv ->
        song?.let { s ->
            val blocks = remember(activeContent) { ChordParser.parseBlocks(activeContent) }
            val rendered = remember(blocks, semitones, useFlats) {
                if (semitones == 0 && !useFlats) blocks
                else blocks.map { b ->
                    when (b) {
                        is ContentBlock.Lyric -> b.copy(
                            line = b.line.copy(
                                chords = b.line.chords.map { ct ->
                                    ct.copy(chord = ChordTransposer.transposeChord(ct.chord, semitones, useFlats))
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
                // Cabecera de la partitura: título y artista viven aquí, no en la barra.
                Text(
                    s.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                if (s.artist.isNotBlank()) {
                    Text(
                        s.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                VersionBar(
                    versions = versions,
                    selectedId = selectedVersionId,
                    onSelect = { selectedVersionId = it },
                    onAdd = { showAddVersion = true },
                    onEditSelected = { selectedVersionId?.let(onEditVersion) }
                )
                if (activeCapo > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.capo_fret, activeCapo),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
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
            // Sin canción: puede estar cargando o no existir (id inválido o
            // borrada definitivamente desde la papelera).
            if (notFound) {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = stringResource(R.string.song_not_found_title),
                    subtitle = stringResource(R.string.song_not_found_subtitle),
                    actionLabel = stringResource(R.string.back),
                    onAction = onBack
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }

    selectedChord?.let {
        ChordModal(chordName = it, onDismiss = { selectedChord = null })
    }

    if (showAddVersion) {
        TextDialog(
            title = stringResource(R.string.new_version),
            initial = "",
            onDismiss = { showAddVersion = false },
            onConfirm = { name ->
                showAddVersion = false
                vm.addVersion(name) { newId ->
                    selectedVersionId = newId
                    onEditVersion(newId)
                }
            }
        )
    }

    if (showMetronome) {
        ModalBottomSheet(
            onDismissRequest = { showMetronome = false },
            sheetState = metroSheet
        ) {
            MetronomePanel(
                bpm = metroBpm,
                onBpm = { metroBpm = it; metroEngine.bpm = it },
                beatsPerBar = metroBeats,
                onBeats = { metroBeats = it; metroEngine.beatsPerBar = it },
                running = metroRunning,
                beat = metroBeat,
                onToggle = {
                    if (metroRunning) {
                        metroEngine.stop()
                    } else {
                        metroEngine.bpm = metroBpm
                        metroEngine.beatsPerBar = metroBeats
                        metroEngine.start(metroScope)
                    }
                }
            )
        }
    }
}

@Composable
private fun MetronomePanel(
    bpm: Int,
    onBpm: (Int) -> Unit,
    beatsPerBar: Int,
    onBeats: (Int) -> Unit,
    running: Boolean,
    beat: Int,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.metronome_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.bpm, bpm),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
        )
        Slider(
            value = bpm.toFloat(),
            onValueChange = { onBpm(it.toInt()) },
            valueRange = 30f..240f,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 1..beatsPerBar) {
                val active = running && beat == i
                val c = when {
                    active && i == 1 -> MaterialTheme.colorScheme.primary
                    active -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                    modifier = Modifier
                        .size(if (i == 1) 22.dp else 18.dp)
                        .background(c, CircleShape)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 3, 4, 6).forEach { n ->
                FilterChip(
                    selected = beatsPerBar == n,
                    onClick = { onBeats(n) },
                    label = { Text("$n") }
                )
            }
        }
        FloatingActionButton(onClick = onToggle) {
            Icon(
                if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (running) stringResource(R.string.stop) else stringResource(R.string.start),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionBar(
    versions: List<SongVersion>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onAdd: () -> Unit,
    onEditSelected: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.original)) }
        )
        versions.forEach { v ->
            FilterChip(
                selected = selectedId == v.id,
                onClick = { onSelect(v.id) },
                label = { Text(v.name) }
            )
        }
        AssistChip(
            onClick = onAdd,
            label = { Text(stringResource(R.string.version)) },
            leadingIcon = { Icon(AccordioIcons.mas(), null) }
        )
        if (selectedId != null && versions.any { it.id == selectedId }) {
            IconButton(onClick = onEditSelected) {
                Icon(Icons.Default.Edit, stringResource(R.string.edit_version))
            }
        }
    }
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
        Text(
            text = buildChordLine(line, chordColor, onChordClick),
            style = baseStyle.copy(fontWeight = FontWeight.Bold, color = chordColor)
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

/**
 * Línea de acordes: cada acorde es un enlace pulsable que abre su diagrama,
 * manteniendo la columna exacta sobre la letra.
 */
private fun buildChordLine(
    line: RenderedLine,
    color: Color,
    onChordClick: (String) -> Unit
): AnnotatedString = buildAnnotatedString {
    var col = 0
    for (ct in line.chords) {
        val pad = (ct.position - col).coerceAtLeast(if (col == 0) 0 else 1)
        append(" ".repeat(pad))
        withLink(
            LinkAnnotation.Clickable(
                tag = ct.chord,
                linkInteractionListener = { onChordClick(ct.chord) }
            )
        ) {
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                append(ct.chord)
            }
        }
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
    onSemitones: (Int) -> Unit,
    useFlats: Boolean,
    onUseFlats: (Boolean) -> Unit,
    hasPrev: Boolean = false,
    hasNext: Boolean = false,
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {}
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
                // Modo concierto: saltar entre las canciones de la carpeta.
                if (hasPrev || hasNext) {
                    IconButton(onClick = onPrev, enabled = hasPrev) {
                        Icon(
                            Icons.AutoMirrored.Filled.NavigateBefore,
                            stringResource(R.string.previous_song)
                        )
                    }
                }
                IconButton(onClick = onToggleScroll) {
                    Icon(
                        if (scrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (scrolling) stringResource(R.string.pause) else stringResource(R.string.play)
                    )
                }
                if (hasPrev || hasNext) {
                    IconButton(onClick = onNext, enabled = hasNext) {
                        Icon(
                            Icons.AutoMirrored.Filled.NavigateNext,
                            stringResource(R.string.next_song)
                        )
                    }
                }
                if (!expanded) {
                    Text("${speed.toInt()} px/s", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        stringResource(R.string.tone_with_value, semitoneLabel(semitones)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        if (expanded) stringResource(R.string.hide_controls) else stringResource(R.string.show_controls)
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
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
                    Text(stringResource(R.string.speed_label), style = MaterialTheme.typography.bodySmall)
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
                    Text(stringResource(R.string.tone), style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { onSemitones(semitones - 1) }) {
                        Icon(Icons.Default.Remove, stringResource(R.string.semitone_down))
                    }
                    Text(
                        semitoneLabel(semitones),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { onSemitones(semitones + 1) }) {
                        Icon(AccordioIcons.mas(), stringResource(R.string.semitone_up))
                    }
                    FilterChip(
                        selected = useFlats,
                        onClick = { onUseFlats(!useFlats) },
                        label = { Text("♭") }
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { onSemitones(0) },
                        enabled = semitones != 0
                    ) { Text(stringResource(R.string.reset)) }
                }
                }
            }
        }
    }
}
