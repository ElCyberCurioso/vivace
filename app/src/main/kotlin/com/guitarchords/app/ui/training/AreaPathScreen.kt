package com.guitarchords.app.ui.training

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.training.Curriculum
import com.guitarchords.app.training.TrainingArea
import com.guitarchords.app.ui.theme.extendedColors
import com.guitarchords.app.ui.training.components.LevelBadge
import com.guitarchords.app.ui.training.components.titleRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaPathScreen(
    area: TrainingArea,
    onOpenExercise: (String) -> Unit,
    onBack: () -> Unit,
    vm: TrainingDashboardViewModel = viewModel()
) {
    val areas by vm.areas.collectAsStateWithLifecycle()
    val bests by vm.bestResults.collectAsStateWithLifecycle()

    val unlockedLevel = areas.firstOrNull { it.area == area.name }?.unlockedLevel ?: 1
    val bestByExercise = bests.associateBy { it.exerciseId }
    val passedIds = TrainingDashboardViewModel.passedIds(bests)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(area.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { pv ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pv),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            for (level in 1..Curriculum.MAX_LEVEL) {
                val specs = Curriculum.levelExercises(area, level)
                if (specs.isEmpty()) continue
                val locked = level > unlockedLevel
                item(key = "header_$level") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = if (level == 1) 0.dp else 16.dp, bottom = 8.dp)
                    ) {
                        Text(
                            stringResource(R.string.training_level, level),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (locked) {
                            Spacer(Modifier.size(8.dp))
                            Icon(
                                Icons.Default.Lock, null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                stringResource(R.string.training_locked, level - 1),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(specs, key = { it.id }) { spec ->
                    val best = bestByExercise[spec.id]
                    val passed = spec.id in passedIds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !locked) { onOpenExercise(spec.id) },
                        colors = if (locked) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) else CardDefaults.cardColors()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LevelBadge(spec.level)
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(spec.titleRes, *spec.titleArgs.toTypedArray()),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                if (best != null) {
                                    Text(
                                        stringResource(R.string.training_best_score, best.best),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            when {
                                locked -> Icon(
                                    Icons.Default.Lock, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                passed -> Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = MaterialTheme.extendedColors.success
                                )
                                else -> Icon(
                                    Icons.Default.PlayArrow, null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
