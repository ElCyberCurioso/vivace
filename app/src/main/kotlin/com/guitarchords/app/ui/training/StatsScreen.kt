package com.guitarchords.app.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HighlightOff
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.training.Achievements
import com.guitarchords.app.training.Curriculum
import com.guitarchords.app.training.TrainingArea
import com.guitarchords.app.ui.theme.extendedColors
import com.guitarchords.app.ui.training.components.StreakChip
import com.guitarchords.app.ui.training.components.XpBar
import com.guitarchords.app.ui.training.components.achievementDescRes
import com.guitarchords.app.ui.training.components.achievementTitleRes
import com.guitarchords.app.ui.training.components.icon
import com.guitarchords.app.ui.training.components.titleRes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onRecalibrate: () -> Unit,
    onBack: () -> Unit,
    vm: TrainingDashboardViewModel = viewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val areaStats by vm.areaStats.collectAsStateWithLifecycle()
    val achievements by vm.achievements.collectAsStateWithLifecycle()
    val recent by vm.recentResults.collectAsStateWithLifecycle()

    val unlockedIds = achievements.mapTo(mutableSetOf()) { it.achievementId }
    val statsByArea = areaStats.associateBy { it.area }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.training_stats)) },
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        profile?.let { p ->
                            XpBar(p.xpTotal)
                            Spacer(Modifier.height(8.dp))
                            StreakChip(p.streakCurrent, p.streakBest)
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.stats_xp_by_area),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(TrainingArea.entries.filter { Curriculum.byArea(it).isNotEmpty() }) { area ->
                val stat = statsByArea[area.name]
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            area.icon, null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(area.titleRes),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                stringResource(
                                    R.string.stats_attempts,
                                    stat?.attempts ?: 0,
                                    stat?.passed ?: 0
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            stringResource(R.string.training_xp, stat?.xp ?: 0L),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.stats_achievements),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(Achievements.all, key = { it.id }) { def ->
                val unlocked = def.id in unlockedIds
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents, null,
                            modifier = Modifier.size(28.dp),
                            tint = if (unlocked) MaterialTheme.extendedColors.warning
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(
                                stringResource(achievementTitleRes(def.id)),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (unlocked) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (unlocked) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(achievementDescRes(def.id)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.stats_history),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(recent, key = { it.id }) { result ->
                val spec = Curriculum.byId(result.exerciseId)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (result.passed) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = if (result.passed) MaterialTheme.extendedColors.success
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            spec?.let { stringResource(it.titleRes, *it.titleArgs.toTypedArray()) }
                                ?: result.exerciseId,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            formatDate(result.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${result.score}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onRecalibrate,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.stats_recalibrate))
                }
            }
        }
    }
}

private fun formatDate(ms: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ms))
