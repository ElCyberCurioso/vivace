package com.guitarchords.app.ui.training.exercises

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarchords.app.R
import com.guitarchords.app.chords.MusicTheory
import com.guitarchords.app.training.NoteMatcher
import com.guitarchords.app.training.ScaleNotesSpec
import com.guitarchords.app.tuner.TunerEngine
import com.guitarchords.app.ui.components.FretboardInput
import com.guitarchords.app.ui.theme.extendedColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ScaleNotesExercise(
    spec: ScaleNotesSpec,
    onFinish: (score: Int, passed: Boolean, micValidated: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { TunerEngine() }
    val matcher = remember { NoteMatcher(matchOctave = spec.matchOctave) }

    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPerm = granted }

    var index by remember { mutableIntStateOf(0) }
    var matched by remember { mutableIntStateOf(0) }
    var justMatched by remember { mutableStateOf(false) }

    fun finishNow(currentMatched: Int) {
        val score = (currentMatched * 100f / spec.notes.size).roundToInt()
        onFinish(score, score >= spec.passPct, true)
    }

    fun advance(wasMatched: Boolean) {
        if (wasMatched) matched++
        if (index + 1 < spec.notes.size) {
            index++
        } else {
            finishNow(matched)   // ya incluye la nota actual
        }
    }

    // Bucle de detección: sondea el detector (~una ventana YIN) en vez de
    // coleccionar el StateFlow — lecturas idénticas consecutivas se conflan.
    LaunchedEffect(hasPerm) {
        if (!hasPerm) return@LaunchedEffect
        engine.start(scope)
        while (isActive) {
            val target = spec.notes.getOrNull(index) ?: break
            val ok = matcher.process(
                engine.frequency.value, engine.level.value, target.midi
            )
            if (ok) {
                justMatched = true
                delay(350)                      // feedback visual breve
                justMatched = false
                advance(wasMatched = true)
            }
            delay(90)
        }
    }
    DisposableEffect(Unit) { onDispose { engine.stop() } }

    if (!hasPerm) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Mic, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.tr_scale_mic_needed),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text(stringResource(R.string.tr_scale_mic_grant))
            }
        }
        return
    }

    val level by engine.level.collectAsStateWithLifecycle()
    val note = spec.notes[index]
    val noteName = MusicTheory.NOTES[note.midi % 12]
    val stringNumber = 6 - note.stringIdx          // idx 0 = 6ª cuerda (Mi grave)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.tr_scale_note_progress, index + 1, spec.notes.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { index.toFloat() / spec.notes.size },
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            noteName,
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = if (justMatched) MaterialTheme.extendedColors.success
            else MaterialTheme.colorScheme.onSurface
        )
        Text(
            stringResource(R.string.tr_scale_play_at, stringNumber, note.fret),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        // Diagrama con solo la nota actual marcada (no interactivo).
        val frets = List(6) { i -> if (i == note.stringIdx) note.fret else -1 }
        FretboardInput(
            frets = frets,
            baseFret = if (note.fret > 5) max(1, note.fret - 2) else 1,
            onTapFret = { _, _ -> },
            modifier = Modifier
                .widthIn(max = 300.dp)
                .fillMaxWidth(0.85f)
                .aspectRatio(6f / 7f)
        )
        Spacer(Modifier.height(8.dp))

        // Nivel de señal del micrófono.
        LinearProgressIndicator(
            progress = { (level * 8f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.6f).height(4.dp)
        )
        Spacer(Modifier.height(16.dp))

        TextButton(onClick = {
            matcher.reset()
            advance(wasMatched = false)
        }) {
            Text(stringResource(R.string.tr_scale_not_detected))
        }
    }
}
