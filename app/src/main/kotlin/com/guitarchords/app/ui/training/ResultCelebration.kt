package com.guitarchords.app.ui.training

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.training.ResultSummary
import com.guitarchords.app.training.TrainingArea
import com.guitarchords.app.ui.theme.extendedColors
import com.guitarchords.app.ui.training.components.achievementTitleRes
import com.guitarchords.app.ui.training.components.titleRes

/** Pantalla de resultado tras un ejercicio: score, XP animada, logros. */
@Composable
fun ResultCelebration(
    summary: ResultSummary,
    area: TrainingArea,
    hasNext: Boolean,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedXp by animateIntAsState(
        targetValue = summary.xpEarned,
        animationSpec = tween(900),
        label = "xp"
    )
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (summary.passed) Icons.Default.CheckCircle else Icons.Default.SentimentNeutral,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (summary.passed) MaterialTheme.extendedColors.success
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(if (summary.passed) R.string.tr_result_passed else R.string.tr_result_failed),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.tr_result_score, summary.score),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.tr_xp_earned, animatedXp),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        if (summary.streakExtended && summary.streakCurrent > 1) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalFireDepartment, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    stringResource(R.string.training_streak, summary.streakCurrent),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        summary.newUserLevel?.let { level ->
            Spacer(Modifier.height(12.dp))
            Card {
                Text(
                    stringResource(R.string.tr_level_up, level),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        summary.unlockedAreaLevel?.let { level ->
            Spacer(Modifier.height(8.dp))
            Card {
                Text(
                    stringResource(R.string.tr_area_unlocked, level, stringResource(area.titleRes)),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (summary.newAchievements.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            summary.newAchievements.forEach { id ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents, null,
                        tint = MaterialTheme.extendedColors.warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        stringResource(
                            R.string.tr_achievement_unlocked,
                            stringResource(achievementTitleRes(id))
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        if (hasNext) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tr_next))
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tr_retry))
        }
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back))
        }
    }
}
