package com.guitarchords.app.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.training.TrainingArea
import com.guitarchords.app.ui.training.components.AreaCard
import com.guitarchords.app.ui.training.components.StreakChip
import com.guitarchords.app.ui.training.components.XpBar
import com.guitarchords.app.ui.theme.accordioTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingDashboardScreen(
    onOpenExercise: (String) -> Unit,
    onOpenArea: (TrainingArea) -> Unit,
    onOpenPlacement: () -> Unit,
    onOpenStats: () -> Unit,
    onBack: () -> Unit,
    vm: TrainingDashboardViewModel = viewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val areas by vm.areas.collectAsStateWithLifecycle()
    val bests by vm.bestResults.collectAsStateWithLifecycle()
    val recommended by vm.recommended.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = accordioTopBarColors(),
                title = { Text(stringResource(R.string.training_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (profile?.placementDone == true) {
                        IconButton(onClick = onOpenStats) {
                            Icon(Icons.Default.BarChart, stringResource(R.string.training_stats))
                        }
                    }
                }
            )
        }
    ) { pv ->
        val p = profile
        when {
            p == null -> {}   // perfil sembrado en la migración; estado transitorio
            !p.placementDone -> PlacementInvite(
                onStart = onOpenPlacement,
                onSkip = { vm.skipPlacement() },
                modifier = Modifier.fillMaxSize().padding(pv)
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(pv)
            ) {
                item(span = { GridItemSpan(2) }) {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            XpBar(p.xpTotal)
                            Spacer(Modifier.height(8.dp))
                            StreakChip(p.streakCurrent, p.streakBest)
                        }
                    }
                }
                item(span = { GridItemSpan(2) }) {
                    val rec = recommended
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.training_recommended),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            if (rec != null) {
                                Text(
                                    stringResource(rec.titleRes, *rec.titleArgs.toTypedArray()),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { onOpenExercise(rec.id) }) {
                                    Icon(Icons.Default.PlayArrow, null)
                                    Spacer(Modifier.size(6.dp))
                                    Text(stringResource(R.string.training_start))
                                }
                            } else {
                                Text(
                                    stringResource(R.string.training_all_done),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                items(TrainingArea.entries.toList()) { area ->
                    val (unlocked, passed, total) = vm.areaCardData(area, areas, bests)
                    AreaCard(
                        area = area,
                        unlockedLevel = unlocked,
                        passedInLevel = passed,
                        totalInLevel = total,
                        enabled = area.hasContent(),
                        onClick = { onOpenArea(area) }
                    )
                }
            }
        }
    }
}

/** Invitación a pantalla completa: el test de nivel es la puerta de entrada. */
@Composable
private fun PlacementInvite(
    onStart: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Quiz,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.training_placement_invite_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.training_placement_invite_msg),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.training_placement_start))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkip) {
            Text(stringResource(R.string.training_placement_skip))
        }
    }
}
