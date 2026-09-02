package com.guitarchords.app.ui.tuner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarchords.app.R
import com.guitarchords.app.tuner.TunerEngine
import com.guitarchords.app.ui.theme.VivaceMono
import com.guitarchords.app.ui.theme.extendedColors
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import com.guitarchords.app.ui.theme.accordioTopBarColors

private data class StringTarget(val name: String, val freq: Float)

private val STANDARD = listOf(
    StringTarget("E2", 82.4069f),
    StringTarget("A2", 110.000f),
    StringTarget("D3", 146.832f),
    StringTarget("G3", 195.998f),
    StringTarget("B3", 246.942f),
    StringTarget("E4", 329.628f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { TunerEngine() }

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

    LaunchedEffect(hasPerm) {
        if (hasPerm) engine.start(scope)
    }
    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    val freq by engine.frequency.collectAsStateWithLifecycle()
    val level by engine.level.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = accordioTopBarColors(),
                title = { Text(stringResource(R.string.tuner_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                }
            )
        }
    ) { pv ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!hasPerm) {
                PermissionPrompt(onRequest = {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                })
            } else {
                TunerContent(freq = freq, level = level)
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Mic, null, modifier = Modifier.height(64.dp))
        Text(
            stringResource(R.string.mic_permission_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            stringResource(R.string.mic_permission_msg),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onRequest) { Text(stringResource(R.string.grant_permission)) }
    }
}

@Composable
private fun TunerContent(freq: Float, level: Float) {
    val target = remember(freq) { nearest(freq) }
    val cents = remember(freq, target) {
        if (freq <= 0f || target == null) 0f
        else 1200f * (ln(freq / target.freq) / ln(2f))
    }
    val active = freq > 0f && level > 0.01f && target != null
    val inTune = active && abs(cents) < 5f

    // Pulso háptico al entrar en tono.
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(inTune) {
        if (inTune) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            if (active) target!!.name else "—",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = if (inTune) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.onSurface
        )
        Text(
            if (active) "%.1f Hz".format(freq) else stringResource(R.string.play_a_string),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = VivaceMono)
        )

        Needle(
            cents = if (active) cents.coerceIn(-50f, 50f) else 0f,
            active = active,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Text(
            if (active) "${if (cents >= 0) "+" else ""}${cents.roundToInt()} cents" else " ",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = VivaceMono),
            color = when {
                !active -> MaterialTheme.colorScheme.onSurfaceVariant
                inTune -> MaterialTheme.extendedColors.success
                abs(cents) < 15 -> MaterialTheme.extendedColors.warning
                else -> MaterialTheme.colorScheme.error
            }
        )

        Spacer(Modifier.height(12.dp))
        StringsRow(selected = target?.name)
    }
}

@Composable
private fun Needle(
    cents: Float,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val surface = MaterialTheme.colorScheme.onSurface
    val good = MaterialTheme.extendedColors.success
    val warn = MaterialTheme.extendedColors.warning
    val bad = MaterialTheme.colorScheme.error
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val trackY = h * 0.65f
        val trackH = 10f
        drawRoundRect(
            color = surface.copy(alpha = 0.15f),
            topLeft = Offset(0f, trackY - trackH / 2),
            size = androidx.compose.ui.geometry.Size(w, trackH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH, trackH)
        )
        for (i in -5..5) {
            val x = w / 2 + (i / 5f) * (w / 2 * 0.95f)
            val tall = i == 0 || i % 5 == 0
            drawLine(
                color = surface.copy(alpha = if (tall) 0.7f else 0.35f),
                start = Offset(x, trackY - (if (tall) 26f else 14f)),
                end = Offset(x, trackY + (if (tall) 26f else 14f)),
                strokeWidth = if (i == 0) 4f else 2f
            )
        }
        val needleColor = when {
            !active -> inactive
            abs(cents) < 5 -> good
            abs(cents) < 15 -> warn
            else -> bad
        }
        val nx = w / 2 + (cents / 50f) * (w / 2 * 0.95f)
        drawLine(
            color = needleColor,
            start = Offset(nx, trackY - 44f),
            end = Offset(nx, trackY + 44f),
            strokeWidth = 6f
        )
        drawCircle(needleColor, 12f, Offset(nx, trackY))
    }
}

@Composable
private fun StringsRow(selected: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        STANDARD.forEach { t ->
            val isSel = t.name == selected
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSel)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    t.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

private fun nearest(freq: Float): StringTarget? {
    if (freq <= 0f) return null
    return STANDARD.minByOrNull { abs(ln(freq / it.freq)) }
}
