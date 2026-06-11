package com.guitarchords.app.ui.finder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.chords.ChordRecognizer
import com.guitarchords.app.ui.components.BaseFretControls
import com.guitarchords.app.ui.components.ChordModal
import com.guitarchords.app.ui.components.FretboardInput
import com.guitarchords.app.ui.components.StringStateRow

private const val STRINGS = 6

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
                title = { Text(stringResource(R.string.finder_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = {
                        for (i in 0 until STRINGS) frets[i] = 0
                        baseFret = 1
                    }) { Icon(Icons.Default.Refresh, stringResource(R.string.clear)) }
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
            FretboardInput(
                frets = frets.toList(),
                baseFret = baseFret,
                onTapFret = { stringIdx, fret ->
                    frets[stringIdx] = if (frets[stringIdx] == fret) 0 else fret
                },
                // En tablets el 90 % del ancho dispara la altura (ratio 6:7) y el
                // diagrama no cabe en pantalla: se limita el ancho máximo.
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(0.9f)
                    .aspectRatio(6f / 7f)
            )
            Spacer(Modifier.height(8.dp))
            BaseFretControls(baseFret) { baseFret = it }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.matching_chords),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            if (matches.isEmpty()) {
                Text(
                    stringResource(R.string.mark_frets_hint),
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
                                Text(stringResource(R.string.view))
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
