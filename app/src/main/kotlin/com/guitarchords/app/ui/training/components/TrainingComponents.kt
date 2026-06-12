package com.guitarchords.app.ui.training.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.training.Gamification
import com.guitarchords.app.training.TrainingArea

/** Recurso de string del nombre visible de cada área. */
val TrainingArea.titleRes: Int
    get() = when (this) {
        TrainingArea.CHORDS -> R.string.training_area_chords
        TrainingArea.CHANGES -> R.string.training_area_changes
        TrainingArea.RHYTHM -> R.string.training_area_rhythm
        TrainingArea.SCALES -> R.string.training_area_scales
        TrainingArea.TECHNIQUE -> R.string.training_area_technique
        TrainingArea.THEORY -> R.string.training_area_theory
        TrainingArea.EAR -> R.string.training_area_ear
    }

/** Icono de cada área. */
val TrainingArea.icon: ImageVector
    get() = when (this) {
        TrainingArea.CHORDS -> Icons.Default.Piano
        TrainingArea.CHANGES -> Icons.Default.SwapHoriz
        TrainingArea.RHYTHM -> Icons.Default.Timer
        TrainingArea.SCALES -> Icons.Default.MusicNote
        TrainingArea.TECHNIQUE -> Icons.Default.School
        TrainingArea.THEORY -> Icons.Default.LibraryMusic
        TrainingArea.EAR -> Icons.Default.Hearing
    }

/** Cabecera de nivel de usuario: nivel actual, barra de progreso y XP. */
@Composable
fun XpBar(xpTotal: Long, modifier: Modifier = Modifier) {
    val level = Gamification.levelForXp(xpTotal)
    val progress = Gamification.progressInLevel(xpTotal)
    val toNext = Gamification.xpToReachLevel(level + 1) - xpTotal
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.training_level, level),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.training_xp, xpTotal),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.training_xp_to_next, toNext, level + 1),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Chip de racha diaria con la llama. */
@Composable
fun StreakChip(current: Int, best: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = if (current > 0) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(4.dp))
        Text(
            stringResource(R.string.training_streak, current),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.size(8.dp))
        Text(
            stringResource(R.string.training_streak_best, best),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Tarjeta de un área en el dashboard. */
@Composable
fun AreaCard(
    area: TrainingArea,
    unlockedLevel: Int,
    passedInLevel: Int,
    totalInLevel: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = if (enabled) CardDefaults.cardColors()
        else CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    area.icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(area.titleRes),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(Modifier.height(8.dp))
            if (enabled) {
                Text(
                    stringResource(R.string.training_level, unlockedLevel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.training_progress_of, passedInLevel, totalInLevel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (totalInLevel > 0) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { passedInLevel.toFloat() / totalInLevel },
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                    )
                }
            } else {
                Text(
                    stringResource(R.string.training_coming_soon),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Strings de cada logro (el dominio solo maneja ids estables). */
fun achievementTitleRes(id: String): Int = when (id) {
    "first_steps" -> R.string.ach_first_steps_title
    "placement_done" -> R.string.ach_placement_done_title
    "streak_3" -> R.string.ach_streak_3_title
    "streak_7" -> R.string.ach_streak_7_title
    "ten_exercises" -> R.string.ach_ten_exercises_title
    "chord_apprentice" -> R.string.ach_chord_apprentice_title
    "perfect_score" -> R.string.ach_perfect_score_title
    "xp_1000" -> R.string.ach_xp_1000_title
    "first_mic" -> R.string.ach_first_mic_title
    else -> R.string.ach_all_areas_title
}

fun achievementDescRes(id: String): Int = when (id) {
    "first_steps" -> R.string.ach_first_steps_desc
    "placement_done" -> R.string.ach_placement_done_desc
    "streak_3" -> R.string.ach_streak_3_desc
    "streak_7" -> R.string.ach_streak_7_desc
    "ten_exercises" -> R.string.ach_ten_exercises_desc
    "chord_apprentice" -> R.string.ach_chord_apprentice_desc
    "perfect_score" -> R.string.ach_perfect_score_desc
    "xp_1000" -> R.string.ach_xp_1000_desc
    "first_mic" -> R.string.ach_first_mic_desc
    else -> R.string.ach_all_areas_desc
}

/** Distintivo "Nivel N" pequeño (listas de ejercicios, resultados). */
@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
    Text(
        "N$level",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}
